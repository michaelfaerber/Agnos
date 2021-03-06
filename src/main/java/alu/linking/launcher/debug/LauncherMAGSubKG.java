package alu.linking.launcher.debug;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.disambiguation.hops.graph.EdgeBlacklisting;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.NodeBlacklisting;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.mentiondetection.fuzzy.MentionDetectorLSH;
import alu.linking.utils.Stopwatch;

public class LauncherMAGSubKG {
	public static boolean openBrowser;

	private final EnumModelType KG;

	public LauncherMAGSubKG(EnumModelType KG) {
		this.KG = KG;
	}

	public static void main(String[] args) {
		openBrowser = false;
		new LauncherMAGSubKG(EnumModelType.DEFAULT).run();
	}

	public void run() {
		try {
			Stopwatch.start(getClass().getName());
			final Map<String, Collection<String>> map;
			final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG);
			map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
			// FILE_EXTENDED_GRAPH
			Stopwatch.endOutputStart(getClass().getName());
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			final Set<String> stopwords = stopwordsLoader.getStopwords();
			final InputProcessor inputProcessor = new InputProcessor(stopwords);
			System.out.println("Number of entries: " + map.size());
			final MentionDetector md = new MentionDetectorLSH(KG, 0.8, inputProcessor);
			Stopwatch.endOutputStart(getClass().getName());
			// ########################################################
			// Mention Detection
			// ########################################################
			List<Mention> mentions = null;
			System.out.println("Started detection!");
			// In order to check if exec duration depends on one-time loading
			// or whether it will 'always' be this approx. speed for this case
			final String detectionWatchName = "mDetection";
			final String chooserWatch = "chooser - init (loads graph)";
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);
			final AssignmentChooser chooser = new AssignmentChooser(KG);
			Stopwatch.endOutput(chooserWatch);
			// Blacklisting stuff from graph
			Stopwatch.start("Blacklist");
			NodeBlacklisting nBlacklisting = new NodeBlacklisting(Graph.getInstance());
			EdgeBlacklisting eBlacklisting = new EdgeBlacklisting(Graph.getInstance());
			for (String key : blacklistMapNodes()) {
				nBlacklisting.blacklist(key);
			}
			for (String key : blacklistMapEdges()) {
				eBlacklisting.blacklist(key);
			}
			final int amtBlacklisted = nBlacklisting.blacklistConnectionsOver(0.05);
			System.out.println("Blacklisted items: " + amtBlacklisted);
			System.out.println("Nodes Blacklisting - Enforcing...");
			nBlacklisting.enforce();
			System.out.println("Edges Blacklisting - Enforcing...");
			eBlacklisting.enforce();
			Stopwatch.endOutput("Blacklist");

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
					// final CandidateGenerator candidateGenerator = new
					// CandidateGeneratorMap(map);
					final CandidateGenerator candidateGenerator = new CandidateGeneratorMap(map);
					Stopwatch.start("mentions");
					Collections.sort(mentions, new Comparator<Mention>() {
						@Override
						public int compare(Mention o1, Mention o2) {
							// Made so it accepts the smallest match as the used one
							final int diffLength = (o1.getOriginalMention().length()
									- o2.getOriginalMention().length());
							return (o1.getOffset() == o2.getOffset()) ? diffLength
									: ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
						}
					});
					for (Mention m : mentions) {
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
					final TreeMap<String, Mention> alphabeticalSortedMentions = new TreeMap<String, Mention>();
					final boolean detailed = false;
					// Sort them by key for visibility
					for (Mention m : mentions) {
						alphabeticalSortedMentions.put(m.getMention() + "_" + m.getOriginalMention(), m);
					}
					// Display them
					for (Map.Entry<String, Mention> e : alphabeticalSortedMentions.entrySet()) {
						final Mention m = e.getValue();
						if (detailed) {
							System.out.println("Mention[" + m.getMention() + "; " + m.getDetectionConfidence() + "] ");
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
						// for (Map.Entry<String, Mention> e : sortedMentions.entrySet()) {
						int currIndex = -1;
						for (Mention m : mentions) {
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

							// Mention m = e.getValue();
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
					// for (Mention m : mentions) {
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

	private static Set<String> blacklistMapNodes() {
		final HashSet<String> ret = new HashSet<String>();
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

		// ret.addAll(entityIDMapping.keySet());
		// ret.addAll(entityIDMapping.values());
		return ret;
	}

	private static Set<String> blacklistMapEdges() {
		final HashSet<String> ret = new HashSet<String>();
		ret.add("http://ma-graph.org/property/rank");
		ret.add("http://purl.org/dc/terms/created");
		ret.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		return ret;
	}
}
