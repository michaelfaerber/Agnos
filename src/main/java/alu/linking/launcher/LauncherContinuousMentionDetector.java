package alu.linking.launcher;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.semanticweb.yars.nx.Node;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.fuzzy.MentionDetectorLSH;
import alu.linking.utils.Stopwatch;

public class LauncherContinuousMentionDetector {
	public static boolean openBrowser;

	public static void main(String[] args) {
		openBrowser = false;
		new LauncherContinuousMentionDetector(EnumModelType.DBPEDIA_FULL).run();
	}

	private final EnumModelType KG;

	LauncherContinuousMentionDetector(EnumModelType KG) {
		this.KG = KG;
	}

	public void run() {
		try {
			Stopwatch.start(getClass().getName());
			final Map<String, Set<String>> map;
			System.out.println("Loading mention possibilities...");
			final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG);
			map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
			// FILE_EXTENDED_GRAPH
			Stopwatch.endOutputStart(getClass().getName());
			System.out.println("Number of entries (aka. different surface forms): " + map.size());
			final MentionDetector md = new MentionDetectorLSH(KG, 0.8);
			Stopwatch.endOutputStart(getClass().getName());
			// ########################################################
			// Mention Detection
			// ########################################################
			List<Mention<Node>> mentions = null;
			System.out.println("Started detection!");
			// In order to check if exec duration depends on one-time loading
			// or whether it will 'always' be this approx. speed for this case
			final String detectionWatchName = "mDetection";
			final String chooserWatch = "chooser - init (loads graph)";
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);
			final AssignmentChooser<Node> chooser = new AssignmentChooser<Node>(this.KG);
			Stopwatch.endOutput(chooserWatch);

			String inputLine = null;
			try (final Scanner sc = new Scanner(System.in)) {
				final String iterationWatchname = "Entity Linking";
				do {
					System.out.println("Awaiting user input:");
					// System.out.println("\\033[1mInsert your String here\\033[0m");
					inputLine = sc.nextLine();
					Stopwatch.start(iterationWatchname);
					Stopwatch.start(detectionWatchName);
					mentions = md.detect(inputLine);
					System.out.println("Detection duration: " + Stopwatch.endDiffStart(detectionWatchName) + " ms.");
					System.out.println("Detected [" + mentions.size() + "] mentions.");
					// Once we know where something was mentioned, we need to generate candidates
					// for them
					// ########################################################
					// Candidate Generation (update for mentions)
					// ########################################################
					// final CandidateGenerator<Node> candidateGenerator = new
					// CandidateGeneratorMap(map);
					final CandidateGenerator<Node> candidateGenerator = new CandidateGeneratorMap(map);
					Stopwatch.start("mentions");
					Collections.sort(mentions, new Comparator<Mention<Node>>() {
						@Override
						public int compare(Mention<Node> o1, Mention<Node> o2) {
							// Made so it accepts the smallest match as the used one
							final int diffLength = (o1.getOriginalMention().length()
									- o2.getOriginalMention().length());
							return (o1.getOffset() == o2.getOffset()) ? diffLength
									: ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
						}
					});
					for (Mention<Node> m : mentions) {
						// Update possible assignments
						m.updatePossibleAssignments(candidateGenerator.generate(m));
					}

					// ########################################################
					// Disambiguation
					// ########################################################

					// disambiguation through scoring/assignment choosing
					Stopwatch.start(chooserWatch);
					chooser.choose(mentions);
					System.out.println("Choosing/scoring duration: " + Stopwatch.endDiff(chooserWatch) + " ms.");
					Stopwatch.endOutput(getClass().getName());
					System.out.println("#######################################################");
					System.out.println("Mention Details(" + mentions.size() + "):");
					final TreeMap<String, Mention<Node>> alphabeticalSortedMentions = new TreeMap<String, Mention<Node>>();
					final boolean detailed = false;
					// Sort them by key for visibility
					for (Mention<Node> m : mentions) {
						alphabeticalSortedMentions.put(m.getMention() + "_" + m.getOriginalMention(), m);
					}
					// Display them
					for (Map.Entry<String, Mention<Node>> e : alphabeticalSortedMentions.entrySet()) {
						final Mention<Node> m = e.getValue();
						if (detailed) {
							System.out.println("Mention[" + m.getMention() + "; " + m.getDetectionConfidence() + "] "
									+ m.getSource());
							System.out.println("Original Text:" + m.getOriginalMention());
							System.out.println("Possible assignments: "
									+ (m.getPossibleAssignments() != null ? m.getPossibleAssignments().size()
											: "None"));
							System.out.println("Found assignment: " + m.getAssignment());
							System.out.println("Found Assignment's Score: " + m.getAssignment().getScore());
							System.out.println("--------------------------------------------------");
						} else {
							System.out.println(m.getOriginalMention() + "(" + m.getMention() + "; "
									+ m.getDetectionConfidence() + ")\t\t-> " + m.getAssignment().getScore() + ":"
									+ m.getAssignment().getAssignment().toString());
						}
					}
					Stopwatch.endOutput(iterationWatchname);
					// Flush result to file and open in browser
					final File resultsFile = new File("./output.html").getCanonicalFile();
					try (BufferedWriter bwResults = new BufferedWriter(new FileWriter(resultsFile))) {
						String resultLine = inputLine;
						// for (Map.Entry<String, Mention<Node>> e : sortedMentions.entrySet()) {
						int currIndex = -1;
						for (Mention<Node> m : mentions) {
							final String hyperlinkMention = " <a href=" + m.getAssignment().getAssignment().toString()
									+ ">" + m.getMention() + "</a> ";
							final String hyperlinkMentionOriginal = " <a href="
									+ m.getAssignment().getAssignment().toString() + ">" + m.getOriginalMention()
									+ "</a> ";

							final String search = m.getOriginalMention();
							int foundIndex = resultLine.indexOf(search, currIndex);
							try {
								resultLine = resultLine.substring(0, foundIndex) + hyperlinkMentionOriginal
										+ resultLine.substring(foundIndex + search.length());
							} catch (StringIndexOutOfBoundsException siooe) {
								System.out.println(currIndex + " - Out of bounds for: " + search);
								System.out.println("Mention:" + m.getMention() + " - " + m.getOffset());
							}
							currIndex = foundIndex + search.length();

							// Mention<Node> m = e.getValue();
							// resultLine = resultLine.replace(" " + m.getMention() + " ",
							// hyperlinkMention);
							// resultLine = resultLine.replace(" " + m.getOriginalMention() + " ",
							// hyperlinkMentionOriginal);
						}
						bwResults.write("<META HTTP-EQUIV=\"content-type\" CONTENT=\"text/html; charset=utf-8\"><br>"
								+ resultLine);
						bwResults.flush();
					}
					// Open file in browser
					if (openBrowser && inputLine != null && inputLine.length() != 0 && Desktop.isDesktopSupported()) {
						Desktop.getDesktop().browse(resultsFile.toURI());
					}
					// Displaying them as ordered...
					// for (Mention<Node> m : mentions) {
					// System.out.println("Mention(" + m.getOffset() + "): " +
					// m.getOriginalMention());
					// }
				} while (inputLine != null && inputLine.length() != 0);
				System.out.println("Total duration: " + Stopwatch.endDiffStart(getClass().getName()) + " ms!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Map<String, String> blacklistMap() {
		Map<String, String> entityIDMapping = new HashMap<String, String>();
		entityIDMapping.put("e_1", "car");// confirmed to be 'car', prior set to 'car_part'
		entityIDMapping.put("e_3", "car_part");// confirmed, prior set to 'action_car_part'
		entityIDMapping.put("e_5", "action_car_part");// confirmed
		entityIDMapping.put("e_6", "SENTECE_LABEL");// confirmed
		entityIDMapping.put("e_19", "termin_wartezeit");// confirmed
		entityIDMapping.put("e_22", "holbring_mobil");// confirmed
		entityIDMapping.put("e_25", "location");// confirmed
		entityIDMapping.put("e_2", "car_property");// confirmed
		entityIDMapping.put("e_4", "damage");// confirmed
		return entityIDMapping;
	}
}
