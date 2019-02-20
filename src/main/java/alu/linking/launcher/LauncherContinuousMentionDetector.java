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
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.semanticweb.yars.nx.Node;

import alu.linking.api.GERBILAPIAnnotator;
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
import alu.linking.utils.DetectionUtils;
import alu.linking.utils.Stopwatch;

public class LauncherContinuousMentionDetector {
	public static boolean openBrowser;
	// Whether the output should be detailed
	public static final boolean detailed = false;

	final String chooserWatch = "chooser - init (loads graph)";
	final String detectionWatch = MentionDetector.class.getName();
	final String iterationWatch = "iteration";
	final boolean OVERLAP = true;
	final Comparator<Mention<Node>> offsetComparator = new Comparator<Mention<Node>>() {
		@Override
		public int compare(Mention<Node> o1, Mention<Node> o2) {
			// Made so it accepts the smallest match as the used one
			final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
			return (o1.getOffset() == o2.getOffset()) ? diffLength : ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
		}
	};

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
			System.out.println("Loading mention possibilities...");
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			final Set<String> stopwords = stopwordsLoader.getStopwords();
			final Map<String, Set<String>> map = DetectionUtils.loadSurfaceForms(this.KG, stopwordsLoader);
			// ########################################################
			// Mention Detection
			// ########################################################
			final MentionDetector md = DetectionUtils.setupMentionDetection(KG, map);

			// ########################################################
			// Candidate Generator
			// ########################################################
			final CandidateGenerator<Node> candidateGenerator = new CandidateGeneratorMap(map);
			Stopwatch.endOutputStart(getClass().getName());
			List<Mention<Node>> mentions = null;
			System.out.println("Started detection!");
			// In order to check if exec duration depends on one-time loading
			// or whether it will 'always' be this approx. speed for this case
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);
			final AssignmentChooser<Node> chooser = new AssignmentChooser<Node>(this.KG);
			Stopwatch.endOutput(chooserWatch);
			String inputLine = null;
			try (final Scanner sc = new Scanner(System.in)) {
				do {
					// ########################################################
					// Awaiting input
					// ########################################################
					System.out.println("\\\\033[1mAwaiting user input\\\\033[0m...");
					inputLine = sc.nextLine();
					Stopwatch.start(iterationWatch);
					Stopwatch.start(detectionWatch);
					mentions = md
							.detect(InputProcessor.combineProcessedInput(InputProcessor.process(inputLine, stopwords)));
					System.out.println("Detected [" + mentions.size() + "] mentions.");
					System.out.println("Detection duration: " + Stopwatch.endDiffStart(detectionWatch) + " ms.");

					// ########################################################
					// Candidate Generation (update for mentions)
					// ########################################################
					Collections.sort(mentions, offsetComparator);
					for (Mention<Node> m : mentions) {
						// Update possible assignments w/ possible candidates
						m.updatePossibleAssignments(candidateGenerator.generate(m));
					}

					if (!OVERLAP) {
						// #####################################################################
						// Remove smallest conflicting mentions keeping just the longest ones
						// #####################################################################
						List<Mention<Node>> toRemoveMentions = Lists.newArrayList();
						for (int i = 0; i < mentions.size(); ++i) {
							for (int j = i + 1; j < mentions.size(); ++j) {
								final Mention<Node> leftMention = mentions.get(i);
								final Mention<Node> rightMention = mentions.get(j);
								// If they conflict, add the shorter one to a list to be removed
								if (leftMention.getMention().contains(rightMention.getMention())) {
									toRemoveMentions.add(rightMention);
								} else if (rightMention.getMention().contains(leftMention.getMention())) {
									toRemoveMentions.add(leftMention);
								}
							}
						}
						for (Mention<Node> toRemove : toRemoveMentions) {
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
	private void output(List<Mention<Node>> mentions, final String inputLine) throws IOException {
		final File resultsFile = new File("./output.html").getCanonicalFile();
		String resultLine = inputLine;
		final AtomicInteger currIndex = new AtomicInteger(-1);
		for (Mention<Node> m : mentions) {
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
