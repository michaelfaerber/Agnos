package alu.linking.api.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.Strings;

public class LauncherGERBILTableToLatex {

	public enum ADD_MODE {
		ONLY_FIRST, BEST;
	}

	public static void main(String[] args) {
		final ADD_MODE add_mode = ADD_MODE.BEST;
		final File inFile = new File(
				// "C:\\Users\\Kris\\Desktop\\jar_out\\evaluation\\evaluation_everything_in_progress.txt")//
				"C:\\Users\\Kris\\Desktop\\jar_out\\evaluation\\merged_evaluations.txt")//
		;
		final String separator = "\t";
		DecimalFormat df = new DecimalFormat("#.##");
		final String[] columns = "Annotator	Dataset	Micro F1 score	Micro Precision	Micro Recall	Macro F1 score	Macro Precision	Macro Recall	InKB Macro F1 score	InKB Macro Precision	InKB Macro Recall	InKB Micro F1 score	InKB Micro Precision	InKB Micro Recall	EE Macro F1 score	EE Macro Precision	EE Macro Recall	EE Micro F1 score	EE Micro Precision	EE Micro Recall	avg millis/doc	confidence threshold	GSInKB Macro F1 score	GSInKB Macro Precision	GSInKB Macro Recall	GSInKB Micro F1 score	GSInKB Micro Precision	Error Count	Timestamp	GERBIL version"
				.split(separator);
		final String[] columnNames = new String[] { "Annotator", "Dataset", "Micro F1 score", "Micro Precision",
				"Micro Recall", "Macro F1 score", "Macro Precision", "Macro Recall", "InKB Macro F1 score",
				"InKB Macro Precision", "InKB Macro Recall", "InKB Micro F1 score", "InKB Micro Precision",
				"InKB Micro Recall", "EE Macro F1 score", "EE Macro Precision", "EE Macro Recall", "EE Micro F1 score",
				"EE Micro Precision", "EE Micro Recall", "avg millis/doc", "confidence threshold",
				"GSInKB Macro F1 score", "GSInKB Macro Precision", "GSInKB Macro Recall", "GSInKB Micro F1 score",
				"GSInKB Micro Precision", "Error Count" };
		final String[] errorTokenNames = new String[] { "DBpedia Spotlight ", "Derczynski ",
				"The annotator caused too many single errors. ", "2019-04-24 03:47:57 ", "1.2.7" };
		final String[] errorTokens = "Agnos (NIF WS) 	ERD2014 	The annotator caused too many single errors. 	2019-04-24 04:18:49 	1.2.7"
				.split(separator);
		final String[] stillRunningNames = new String[] { "Babelfy ", "OKE 2018 Task 4 training dataset ",
				"The experiment is still running. ", "2019-04-23 20:13:27 ", "1.2.7" };
		final String[] stillRunning = "Agnos (NIF WS) 	Microposts2016-Test 	The experiment is still running. 	2019-04-23 20:13:26 	1.2.7"
				.split(separator);
		final int stateMessageIndex = 2;
		final List<String> whitelist = Lists.newArrayList();
		whitelist.addAll(Arrays.asList(new String[] { "Agnos", "mag" }));
		final boolean DEBUG = false;
		final List<String> runningLists = Lists.newArrayList();
		final List<String> errorsLists = Lists.newArrayList();
		final List<String> missingStatesList = Lists.newArrayList();
		// Key: Dataset_System, List<>(F1, Precision, Recall)
		final TreeMap<String, Triple<String, String, String>> mapDatasetSystemTOMetrics = new TreeMap<>();
		try (final BufferedReader brIn = new BufferedReader(new FileReader(inFile))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				final String[] tokens = line.split(separator);
				if (tokens.length != columns.length) {
					// Probably an error or still computing...
					if (tokens.length == errorTokens.length) {
						if (tokens[stateMessageIndex].contains("error")) {
							errorsLists.add(tokens[0] + " " + tokens[1]);
						} else if (tokens[stateMessageIndex].contains("running")) {
							errorsLists.add(tokens[0] + " " + tokens[1]);
						} else {
							missingStatesList.add(line);
						}
					}
					continue;
				}
				// NAME DATASET
				final String systemName = tokens[0];
				final String datasetName = tokens[1];
				final String key = makeKey(datasetName, systemName);
				final Triple<String, String, String> f1PrecisionRecall = new ImmutableTriple(
						df.format(Double.valueOf(tokens[2])), df.format(Double.valueOf(tokens[3])),
						df.format(Double.valueOf(tokens[4])));
				if (whitelist == null || whitelist.size() == 0 || isIn(key, whitelist)) {
					if (mapDatasetSystemTOMetrics.get(key) == null) {
						mapDatasetSystemTOMetrics.put(key, f1PrecisionRecall);
					} else if (add_mode != ADD_MODE.ONLY_FIRST) {
						final Triple<String, String, String> oldTriple = mapDatasetSystemTOMetrics.get(key);
						final Double oldF1 = Double.parseDouble(oldTriple.getLeft());
						final Double newF1 = Double.parseDouble(f1PrecisionRecall.getLeft());
						// Put only the bigger one in...
						if (newF1 > oldF1) {
							mapDatasetSystemTOMetrics.put(key, f1PrecisionRecall);
						}
						if (DEBUG) {
							System.out.println(
									"Inserted [" + key + "]: OLD[" + oldTriple + "], NEW[" + f1PrecisionRecall + "]");
						}
//					throw new RuntimeException(
//							"Error: Key[" + key + "] already in map...:" + mapDatasetSystemTOMetrics);
					}
				}
			}

			// Get number of datasets
			final TreeSet<String> systems = new TreeSet<>();
			for (Entry<String, Triple<String, String, String>> e : mapDatasetSystemTOMetrics.entrySet()) {
				final String[] datasetSystem = splitKey(e.getKey());
				final String systemName = datasetSystem[1];
				systems.add(systemName);
			}
			final List<String> systemNames = Lists.newArrayList(systems);

			// Now that the map is populated as (DATASET_SYSTEM -> <metrics values>), get
			// them all out
			final StringBuilder sb = new StringBuilder();
			// sb.append("\\begin{table}[]");
			sb.append(Strings.NEWLINE.val);
			sb.append("	    \\centering");
			sb.append(Strings.NEWLINE.val);

			// sb.append(" \\begin{tabular}{| *{" + (systemNames.size() * 3 + 1) + "}{c|}
			// }");
			sb.append("	    \\begin{longtable}{| *{" + (systemNames.size() * 3 + 1) + "}{c|} }");
			sb.append(Strings.NEWLINE.val);
			sb.append("	    \\hline");
			sb.append(Strings.NEWLINE.val);

			// Header
			boolean first = true;
			for (String system : systemNames) {
				if (first) {
					sb.append("		");
					first = false;
				}

				sb.append("& \\multicolumn{3}{c|}{" + system.trim() + "}");
			}
			sb.append("\\\\ \\hline");
			sb.append(Strings.NEWLINE.val);

			// Dataset + actual values
			boolean firstCol = true;
			String prevDataset = null;
			int column = 0;
			String system = null;
			for (Entry<String, Triple<String, String, String>> e : mapDatasetSystemTOMetrics.entrySet()) {
				final String[] datasetSystem = splitKey(e.getKey());
				final String dataset = datasetSystem[0];
				system = datasetSystem[1];
				while (column < systemNames.size() && !systemNames.get(column).equals(system)) {
					// Wrong column, so there must be one or more missing systems for this dataset
					// 3 x & due to length(F1, Precision, Recall) = 3
					sb.append(" & ");
					sb.append(" & ");
					sb.append(" & ");
					column++;
				}

				boolean nextLine = prevDataset != null && !prevDataset.equals(dataset);
				if (nextLine) {
					sb.append("\\\\ \\hline");
					sb.append(Strings.NEWLINE.val);
					firstCol = true;
					column = 0;
				}

				if (firstCol) {
					sb.append("		" + dataset.trim());
					firstCol = false;
				}
				final Triple<String, String, String> f1PrecisionRecall = e.getValue();
				sb.append(" & ");
				sb.append(f1PrecisionRecall.getLeft());
				sb.append(" & ");
				sb.append(f1PrecisionRecall.getMiddle());
				sb.append(" & ");
				sb.append(f1PrecisionRecall.getRight());
				prevDataset = dataset;
				column++;
			}
			// Take care of last line
			while (system != null && column < systemNames.size() && !systemNames.get(column).equals(system)) {
				// Wrong column, so there must be one or more missing systems for this dataset
				// 3 x & due to length(F1, Precision, Recall) = 3
				sb.append(" & ");
				sb.append(" & ");
				sb.append(" & ");
				column++;
			}

			sb.append("\\\\ \\hline");
			sb.append(Strings.NEWLINE.val);

			// sb.append(" \\end{tabular}\r\n");
			sb.append(" \\caption{GERBIL Evaluation}\r\n");
			sb.append(" \\label{tab:evaluation}\r\n");
			sb.append("    \\end{longtable}\r\n");
			// sb.append("\\end{table}\r\n");

			System.out.println("%No idea(" + missingStatesList.size() + ")");
			System.out.println("%ERRORS(" + errorsLists.size() + ")");
			System.out.println("%IN PROGRESS(" + runningLists.size() + ")");
			System.out.println(
					"%Entries(" + mapDatasetSystemTOMetrics.size() + "): " + mapDatasetSystemTOMetrics.keySet());
			System.out.println(sb.toString());

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static boolean isIn(String key, List<String> whitelist) {
		for (String white : whitelist) {
			if (key.toLowerCase().contains(white.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private static String makeKey(String dataset, String system) {
		return dataset + "_" + system;
	}

	private static String[] splitKey(String key) {
		return key.split("_");
	}

}
