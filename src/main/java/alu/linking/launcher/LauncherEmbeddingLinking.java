package alu.linking.launcher;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.mentiondetection.fuzzy.MentionDetectorLSH;
import alu.linking.utils.Stopwatch;

public class LauncherEmbeddingLinking {
	public static boolean openBrowser;

	public static void main(String[] args) {
		openBrowser = false;
		new LauncherEmbeddingLinking(EnumModelType.CRUNCHBASE).run();
	}

	private final EnumModelType KG;

	LauncherEmbeddingLinking(EnumModelType KG) {
		this.KG = KG;
	}

	public void run() {
		try {
			Stopwatch.start(getClass().getName());
			final Map<String, Collection<String>> map;
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG, stopwordsLoader);
			map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
			// FILE_EXTENDED_GRAPH
			Stopwatch.endOutputStart(getClass().getName());
			System.out.println("Number of entries: " + map.size());
			final InputProcessor inputProcessor = new InputProcessor(stopwordsLoader.getStopwords());
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
			final AssignmentChooser chooser = new AssignmentChooser(this.KG);
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
					System.out.println("Mentions: "+mentions);
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
}
