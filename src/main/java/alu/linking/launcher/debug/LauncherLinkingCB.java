package alu.linking.launcher.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.launcher.LauncherInputLinking;
import alu.linking.mentiondetection.Mention;
import alu.linking.postprocessing.ThresholdPruner;
import alu.linking.utils.CBURLTransformer;

public class LauncherLinkingCB {
	public static void main(String[] argv) {
		final EnumModelType KG = EnumModelType.CRUNCHBASE2;
		final LauncherInputLinking linking = new LauncherInputLinking(KG);
		String inputFilename = "1004291";// number
		final ThresholdPruner pruner = new ThresholdPruner(1.0d);

		final String dirIn = FilePaths.DIR_NEWS_FILTERED_BODY.getPath(KG);
		final StringBuilder input = new StringBuilder();
		try (final Scanner sc = new Scanner(System.in)) {
			do {
				final File inFileBody = new File(dirIn + "/" + inputFilename + ".txt");
				final File inFileLinks = new File(
						FilePaths.DIR_NEWS_FILTERED_LINKS.getPath(KG) + "/" + inputFilename + ".txt");
				try {
					try (BufferedReader brIn = new BufferedReader(new FileReader(inFileBody))) {
						String line = null;
						// Ignore first line as it's a title line
						brIn.readLine();
						// Rest is input
						while ((line = brIn.readLine()) != null) {
							input.append(line + " ");
						}
					}
					List<Mention> mentions = linking.run(input.toString());
					mentions = pruner.prune(mentions);
					displayMentions(mentions);
					evaluate(mentions, inFileLinks);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				inputFilename = sc.nextLine();
			} while (inputFilename != null && inputFilename.length() > 0);
		}
	}

	private static void evaluate(List<Mention> mentions, File inFile) {
		// Get the appropriate evaluation URLs to compare with
		final Set<String> urls = new HashSet<>();
		try {
			try (final BufferedReader brIn = new BufferedReader(new FileReader(inFile))) {
				// Read each link (official KG), line by line
				String line = null;
				while ((line = brIn.readLine()) != null) {
					if (CBURLTransformer.isCrunchbaseURL(line)) {
						// Same line -> not a crunchbase line
						// Aka. different line -> it's a crunchbase line, so add it
						urls.add(CBURLTransformer.toCustomKG(line));
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		// Grab all URLs from the mentions
		final Set<String> mentionURLs = new HashSet<>();
		for (Mention m : mentions) {
			// No need to check if they make sense since they do as they are directly taken
			// from the KG
			mentionURLs.add(m.getAssignment().getAssignment());
		}
		final int foundMentionsCount = mentionURLs.size();
		final int expectedMentionsCount = urls.size();
		final Set<String> copyExpectedURLs = new HashSet<>(urls);
		copyExpectedURLs.retainAll(mentionURLs);
		final int correctMentionsCount = copyExpectedURLs.size();
		final int foundButWrong = foundMentionsCount - correctMentionsCount;
		final int missingMentionsCount = expectedMentionsCount - correctMentionsCount;
		final double precision = ((double) correctMentionsCount) / ((double) foundMentionsCount);
		final double recall = ((double) correctMentionsCount) / ((double) expectedMentionsCount);
		final double f1 = 2 * precision * recall / (precision + recall);
		System.out.println("# of found, but wrong:" + foundButWrong);
		System.out.println("# of missing:" + missingMentionsCount);
		System.out.println("# of correct:" + correctMentionsCount);
		System.out.println("Expected:" + expectedMentionsCount + " - " + pickItems(urls, 10));
		System.out.println("Found:" + foundMentionsCount + " - " + pickItems(mentionURLs, 5));
		System.out.println("Precision: " + precision);
		System.out.println("Recall: " + recall);
		System.out.println("F1: " + f1);
	}

	private static String pickItems(Set<String> items, int max) {
		final StringBuilder sb = new StringBuilder();
		final int max_lim = Math.min(max, items.size());
		int counter = 0;
		for (String s : items) {
			sb.append(s);
			if (counter < max_lim) {
				sb.append(", ");
			} else {
				if (items.size() > counter) {
					sb.append(" (...)");
				}
				break;
			}
			counter++;
		}
		return sb.toString();
	}

	private static void displayMentions(List<Mention> mentions) {
		System.out.println("#######################################################");
		System.out.println("Mention Details(" + mentions.size() + "):");
		final TreeMap<String, Mention> alphabeticalSortedMentions = new TreeMap<String, Mention>();
		final boolean detailed = true;
		for (Mention m : mentions) {
			alphabeticalSortedMentions.put(m.getMention() + "_" + m.getOriginalMention(), m);
		}
		// Display them
		final File outFile = new File("." + "/" + "cb_linked_output.txt");
		try {
			try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(outFile))) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, Mention> e : alphabeticalSortedMentions.entrySet()) {
					sb.setLength(0);
					final Mention m = e.getValue();
					if (detailed) {
						sb.append("Mention[" + m.getMention() + "; " + m.getDetectionConfidence() + "] ");
						sb.append(Strings.NEWLINE.val);
						sb.append("Original Text:" + m.getOriginalMention());
						sb.append(Strings.NEWLINE.val);
						sb.append("Possible assignments: "
								+ (m.getPossibleAssignments() != null ? m.getPossibleAssignments().size() : "None"));
						sb.append(Strings.NEWLINE.val);
						sb.append("Found assignment: " + m.getAssignment());
						sb.append(Strings.NEWLINE.val);
						sb.append("Found Assignment's Score: " + m.getAssignment().getScore());
						sb.append(Strings.NEWLINE.val);
						sb.append("--------------------------------------------------");
						sb.append(Strings.NEWLINE.val);
					} else {
						sb.append(m.getOriginalMention() + "(" + m.getMention() + "; " + m.getDetectionConfidence()
								+ ")\t\t-> " + m.getAssignment().getScore() + ":"
								+ m.getAssignment().getAssignment().toString());
						sb.append(Strings.NEWLINE.val);
					}
					bwOut.write(sb.toString());
					System.out.println(sb.toString());
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
