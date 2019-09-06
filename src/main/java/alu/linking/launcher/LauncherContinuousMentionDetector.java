package alu.linking.launcher;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.utils.DetectionUtils;
import alu.linking.utils.Stopwatch;

/**
 * Entry point for continuous entity linking from stdin (outputting to stdout)
 * once precomputation structures have been properly established.
 * 
 * @author Kristian Noullet
 *
 */
public class LauncherContinuousMentionDetector {
	// Which knowledge graph to make use of
	private static EnumModelType KG = EnumModelType.DBPEDIA_FULL;
	// Whether to open results as a HTML document (in the browser)
	public static boolean openBrowser = false;
	// Whether the output should be detailed
	public static final boolean detailed = false;
	final String chooserWatch = "chooser - init (loads graph)";
	final String detectionWatch = MentionDetector.class.getName();
	final String iterationWatch = "iteration";
	final boolean OVERLAP = true;
	final Comparator<Mention> offsetComparator = new Comparator<Mention>() {
		@Override
		public int compare(Mention o1, Mention o2) {
			// Made so it accepts the smallest match as the used one
			final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
			return (o1.getOffset() == o2.getOffset()) ? diffLength : ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
		}
	};

	public static void main(String[] args) {
		new LauncherContinuousMentionDetector(KG).run();
	}

	LauncherContinuousMentionDetector(EnumModelType kG) {
		KG = kG;
	}

	public void run() {
		try {
			Stopwatch.start(getClass().getName());
			System.out.println("Loading mention possibilities...");
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			final Set<String> stopwords = stopwordsLoader.getStopwords();
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
			final InputProcessor inputProcessor = new InputProcessor(stopwords);
			// ########################################################
			// Mention Detection
			// ########################################################
			final MentionDetector md = DetectionUtils.setupMentionDetection(KG, map, inputProcessor);

			// ########################################################
			// Candidate Generator
			// ########################################################
			final CandidateGenerator candidateGenerator = new CandidateGeneratorMap(map);
			Stopwatch.endOutputStart(getClass().getName());
			List<Mention> mentions = null;
			System.out.println("Started detection!");
			// In order to check if exec duration depends on one-time loading
			// or whether it will 'always' be this approx. speed for this case
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);
			final AssignmentChooser chooser = new AssignmentChooser(KG);
			Stopwatch.endOutput(chooserWatch);
			String inputLine = null;
			try (final Scanner sc = new Scanner(System.in)) {
				do {
					// ########################################################
					// Awaiting input
					// ########################################################
					System.out.println("\t\t[Awaiting user input]\t\t");
					System.out.print(">");
					inputLine = sc.nextLine();
					Stopwatch.start(iterationWatch);
					Stopwatch.start(detectionWatch);
					mentions = md.detect(InputProcessor
							.combineProcessedInput(InputProcessor.processAndRemoveStopwords(inputLine, stopwords)));
					System.out.println("Detected [" + mentions.size() + "] mentions.");
					System.out.println("Detection duration: " + Stopwatch.endDiffStart(detectionWatch) + " ms.");

					// ########################################################
					// Candidate Generation (update for mentions)
					// ########################################################
					Collections.sort(mentions, offsetComparator);
					for (Mention m : mentions) {
						// Update possible assignments w/ possible candidates
						m.updatePossibleAssignments(candidateGenerator.generate(m));
					}

					if (!OVERLAP) {
						// #####################################################################
						// Remove smallest conflicting mentions keeping just the longest ones
						// #####################################################################
						List<Mention> toRemoveMentions = Lists.newArrayList();
						for (int i = 0; i < mentions.size(); ++i) {
							for (int j = i + 1; j < mentions.size(); ++j) {
								final Mention leftMention = mentions.get(i);
								final Mention rightMention = mentions.get(j);
								// If they conflict, add the shorter one to a list to be removed
								if (leftMention.getMention().contains(rightMention.getMention())) {
									toRemoveMentions.add(rightMention);
								} else if (rightMention.getMention().contains(leftMention.getMention())) {
									toRemoveMentions.add(leftMention);
								}
							}
						}
						for (Mention toRemove : toRemoveMentions) {
							getLogger().info("Removing mention for:'" + toRemove.getMention() + "'");
							mentions.remove(toRemove);
						}
					}

					// ########################################################
					// Disambiguation
					// ########################################################
					Stopwatch.start(chooserWatch);
					chooser.choose(mentions);
					System.out.println("Disambiguation duration: " + Stopwatch.endDiff(chooserWatch) + " ms.");
					Stopwatch.endOutput(getClass().getName());
					System.out.println("#######################################################");
					System.out.println("Mention Details(" + mentions.size() + "):");
					// Display them
					DetectionUtils.displayMentions(getLogger(), mentions, detailed);
					Stopwatch.endOutput(iterationWatch);
					if (openBrowser) {
						// Flush result to file and open in browser
						output(mentions, inputLine);
					}

				} while (inputLine != null && inputLine.length() != 0);
				System.out.println("Total uptime: " + Stopwatch.endDiffStart(getClass().getName()) + " ms!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param mentions
	 * @param inputLine
	 * @throws IOException
	 */
	private void output(List<Mention> mentions, final String inputLine) throws IOException {
		final File resultsFile = new File("./output.html").getCanonicalFile();
		String resultLine = inputLine;
		final AtomicInteger currIndex = new AtomicInteger(-1);
		for (Mention m : mentions) {
			resultLine = DetectionUtils.makeURL(m, currIndex, resultLine);
		}
		try (final BufferedWriter bwResults = new BufferedWriter(new FileWriter(resultsFile))) {
			bwResults.write("<META HTTP-EQUIV=\"content-type\" CONTENT=\"text/html; charset=utf-8\"><br>" + resultLine);
			bwResults.flush();
		}
		// Open file in browser
		if (openBrowser && Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(resultsFile.toURI());
		}
	}

	private static Logger getLogger() {
		return Logger.getLogger(LauncherContinuousMentionDetector.class);
	}
}
