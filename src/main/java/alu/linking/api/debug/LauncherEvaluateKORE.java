package alu.linking.api.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.api.GERBILAPIAnnotator;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.pagerank.PageRankLoader;
import alu.linking.structure.Loggable;

public class LauncherEvaluateKORE implements Loggable {

	public static void main(String[] args) {
		new LauncherEvaluateKORE().run();
	}

	public void run() {
		final String inPath = //
				"./evaluation/" + //
						"kore50-nif.ttl"
		// "dbpedia-spotlight-nif.ttl"
		//
		;
		final File fileKORE50 = new File(inPath);
		// final String inputDir = "./evaluation/min_example.ttl";
		// final String outputDir = "./evaluation/annotated/";
		final GERBILAPIAnnotator annotator = new GERBILAPIAnnotator(EnumModelType.DBPEDIA_FULL);
		annotator.init();
		final TurtleNIFParser parser = new TurtleNIFParser();
		List<Document> documents = null;
		List<Document> copyDocuments = null;
		try {
			documents = parser.parseNIF(new FileInputStream(fileKORE50));
			copyDocuments = parser.parseNIF(new FileInputStream(fileKORE50));
		} catch (FileNotFoundException e) {
			documents = null;
			e.printStackTrace();
		}

		final List<EvaluationResult> evaluationResults = Lists.newArrayList();

		// Process one document after the other
		for (int i = 0; i < documents.size(); ++i) {
			final Document inputDoc = documents.get(i);
			getLogger().info("Evaluating:" + GERBILAPIAnnotator.smallText(inputDoc.getText()));
			try {
				final String results = annotator.annotateDocument(inputDoc);
				// Read the output results and compare to the one I was handed
//				getLogger().info("########################## Results: ##########################");
//				getLogger().info(results);
//				getLogger().info("##############################################################");
				final Document resultDoc = new TurtleNIFDocumentParser().getDocumentFromNIFString(results);
//			Document resultDoc = null;
//			switch (resultDocs.size()) {
//			case 0:
//				getLogger().error("No document returned...");
//				throw new RuntimeException("No document returned...");
//			case 1:
//				// Correct
//				getLogger().info("Document retrieved!");
//				resultDoc = resultDocs.get(0);
//				break;
//			default:
//				getLogger().error("Too many documents returned");
//				throw new RuntimeException("Too many documents returned...");
//			}

				// Compare "result" doc's markings and the "input" doc's
				final EvaluationResult evaluationResult = evaluateMarkings(copyDocuments.get(i).getMarkings(),
						resultDoc.getMarkings(), inputDoc.getText());
				evaluationResults.add(evaluationResult);
			} catch (IOException ioe) {
				getLogger().error("IOE Exception happened... ", ioe);
				break;
			} catch (Exception e) {
				getLogger().error("(Parser?) Exception happened... ", e);
				break;
			}
		}

		// Display the overall metrics now
		getLogger().info("########################################");
		getLogger().info("########### LOCAL (Macro) ##############");
		getLogger().info("########################################");
		displayEvaluation(evaluationResults);

		// Display the overall metrics now
		getLogger().info("##############################################################");
		getLogger().info("########### LOCAL (Filtered 100% INCORRECT finds) ############");
		getLogger().info("########### This answers to the question:         ############");
		getLogger().info("##### If we get one correctly, how good is it overall? #######");
		getLogger().info("##############################################################");
		final List<EvaluationResult> filteredEvaluationResults = Lists.newArrayList();
		for (EvaluationResult evaluation : evaluationResults) {
			// Filter out the cases in which...
			// Precision and recall are 0
			if (((int) (evaluation.precision * 100.0)) != 0 || ((int) (evaluation.recall * 100.0)) != 0) {
				// This way we see "if we get something, how well do we do"
				filteredEvaluationResults.add(evaluation);
			}
		}
		displayEvaluation(filteredEvaluationResults);

		getLogger().info("#######################################");
		getLogger().info("########### GLOBAL (Micro) ############");
		getLogger().info("#######################################");
		displayTruthValues(EvaluationResult.globalTP, EvaluationResult.globalTN, EvaluationResult.globalFP,
				EvaluationResult.globalFN);
		getLogger().info("Finished successfully!");
		final StringBuilder sbMissingPR = new StringBuilder();
		int missingPRCounter = 0;
		final int displayPR = 10;
		for (String s : PageRankLoader.setPRNotFound) {
			sbMissingPR.append(s);
			sbMissingPR.append("; ");
			missingPRCounter++;
			if (missingPRCounter >= displayPR) {
				break;
			}
		}
		getLogger().info(
				"Missing PR values(" + PageRankLoader.setPRNotFound.size() + "): [" + sbMissingPR.toString() + "]");
	}

	private void displayEvaluation(final List<EvaluationResult> evaluationResults) {
		final EvaluationResult firstEval = evaluationResults.get(0);
		final StringBuilder sbPrecisionSum = new StringBuilder(String.valueOf(firstEval.precision));
		final StringBuilder sbRecallSum = new StringBuilder(String.valueOf(firstEval.recall));
		final StringBuilder sbF1Sum = new StringBuilder(String.valueOf(firstEval.f1));
		double precisionSum = firstEval.precision;
		double recallSum = firstEval.recall;
		double f1Sum = firstEval.f1;
		for (int i = 1; i < evaluationResults.size(); ++i) {
			final EvaluationResult evaluation = evaluationResults.get(i);
			double precision = evaluation.precision;
			double recall = evaluation.recall;
			double f1 = evaluation.f1;
			precisionSum += precision;
			recallSum += recall;
			f1Sum += f1;
			final String plus = " + ";
			sbPrecisionSum.append(plus + precision);
			sbRecallSum.append(plus + recall);
			sbF1Sum.append(plus + f1);

		}

		final double evalAmt = ((double) evaluationResults.size());
		final double avgPrecision = (precisionSum / evalAmt);
		final double avgRecall = (recallSum / evalAmt);
		final double avgF1 = (f1Sum / evalAmt);
		getLogger().info("Precision Sum: " + precisionSum + " = " + sbPrecisionSum.toString());
		getLogger().info("Recall Sum: " + recallSum + " = " + sbRecallSum.toString());
		getLogger().info("F1 Sum: " + f1Sum + " = " + sbF1Sum.toString());
		getLogger().info("Precision Avg.: " + avgPrecision);
		getLogger().info("Recall Avg.: " + avgRecall);
		getLogger().info("F1 Avg.: " + avgF1);
	}

	/**
	 * Displays Precision, Recall and F1 measure along with the TP, TN, FP and FN
	 * values
	 * 
	 * @param TP
	 * @param TN
	 * @param FP
	 * @param FN
	 */
	private void displayTruthValues(int TP, int TN, int FP, int FN) {
		final double globalPrecision = precision(TP, TN, FP, FN);
		final double globalRecall = recall(TP, TN, FP, FN);
		final double globalF1 = f1(TP, TN, FP, FN);
		getLogger().info("TP(" + TP + "), TN(" + TN + "), FP(" + FP + "), FN(" + FN + ")");
		getLogger().info("Precision:" + globalPrecision);
		getLogger().info("Recall:" + globalRecall);
		getLogger().info("F1:" + globalF1);
	}

	private EvaluationResult evaluateMarkings(final List<Marking> inputMarkings, final List<Marking> resultMarkings,
			final String inputText) {
		final EvaluationResult evaluationResult = new EvaluationResult();
		final Set<TestMarking> setInputMarkings = transformToTestMarkings(inputMarkings, inputText);
		final Set<TestMarking> setResultMarkings = transformToTestMarkings(resultMarkings, inputText);
		getLogger().info("Input: List(" + inputMarkings.size() + "), Set(" + setInputMarkings.size() + ")");
		getLogger().info("Result: List(" + resultMarkings.size() + "), Set(" + setResultMarkings.size() + ")");
		getLogger().info("Input Set:" + setInputMarkings);
		getLogger().info("Result Set:" + setResultMarkings);
		double tp = 0, fp = 0, tn = 0, fn = 0;
		for (TestMarking m : setInputMarkings) {
			// Iterate through set, check if each is also in the result one
			if (setResultMarkings.contains(m)) {
				// It's in both -> true positive
				tp++;
			} else {
				// It's not in our results, but it is in the input
				fn++;
			}
		}

		for (TestMarking m : setResultMarkings) {
			// Iterate through set, check if each is also in the result one
			if (setInputMarkings.contains(m)) {
				// tp++;
			} else {
				// It's in the found markings but not in the ground truth
				fp++;
			}
		}
		final String outStr = "Markings TRUE(" + setInputMarkings.size() + ") / Found(" + setResultMarkings.size()
				+ "): TP(" + tp + "), FN(" + fn + "), FP(" + fp + "), TN(" + tn + ")";
		final double precision = precision(tp, tn, fp, fn);// tp / (tp + fp);
		final double recall = recall(tp, tn, fp, fn);// tp / (tp + fn);
		final double f1 = f1(tp, tn, fp, fn);// 2 * precision * recall / (precision + recall);
		getLogger().info(GERBILAPIAnnotator.smallText(inputText));
		getLogger().info(outStr);
		getLogger().info("Precision: " + precision);
		getLogger().info("Recall: " + recall);
		getLogger().info("F1: " + f1);
		getLogger().info("################################################################");
		getLogger().info("###################### END OF DOCUMENT #########################");
		getLogger().info("################################################################");

		evaluationResult.tp = (int) (Double.isNaN(tp) ? 0 : tp);
		evaluationResult.tn = (int) (Double.isNaN(tn) ? 0 : tn);
		evaluationResult.fp = (int) (Double.isNaN(fp) ? 0 : fp);
		evaluationResult.fn = (int) (Double.isNaN(fn) ? 0 : fn);
		// Increment the global counters
		evaluationResult.globalFN += (int) (Double.isNaN(fn) ? 0 : fn);
		evaluationResult.globalFP += (int) (Double.isNaN(fp) ? 0 : fp);
		evaluationResult.globalTN += (int) (Double.isNaN(tn) ? 0 : tn);
		evaluationResult.globalTP += (int) (Double.isNaN(tp) ? 0 : tp);
		evaluationResult.f1 = Double.isNaN(f1) ? 0 : f1;
		evaluationResult.precision = Double.isNaN(precision) ? 0 : precision;
		evaluationResult.recall = Double.isNaN(recall) ? 0 : recall;
		evaluationResult.inputText = inputText;
		evaluationResult.inputFile = null;
		evaluationResult.goldStandard = setInputMarkings;
		evaluationResult.foundMarkings = setResultMarkings;
		return evaluationResult;
	}

	private double precision(Number tp, Number tn, Number fp, Number fn) {
		final double precision = tp.doubleValue() / (tp.doubleValue() + fp.doubleValue());
		return precision;
	}

	private double recall(Number tp, Number tn, Number fp, Number fn) {
		final double recall = tp.doubleValue() / (tp.doubleValue() + fn.doubleValue());
		return recall;
	}

	private double f1(Number tp, Number tn, Number fp, Number fn) {
		final double precision = precision(tp, tn, fp, fn);
		final double recall = recall(tp, tn, fp, fn);
		final double f1 = 2 * precision * recall / (precision + recall);
		return f1;
	}

	private Set<TestMarking> transformToTestMarkings(Collection<Marking> markings, final String inputText) {
		final Set<TestMarking> ret = new HashSet<>();
		for (Marking mark : markings) {
			final Span ne = (Span) mark;
			final String text = inputText.substring(ne.getStartPosition(), ne.getStartPosition() + ne.getLength());
			final Set<String> uris;
			if (mark instanceof Meaning) {
				uris = ((Meaning) mark).getUris();
			} else {
				uris = new HashSet<>();
			}
			if (uris.size() == 0) {
				getLogger().warn("No URI passed...");
			} else if (uris.size() > 1) {
				getLogger().warn(uris.size() + " URIs passed...");
			}
			for (String uri : uris) {
				ret.add(new TestMarking(text, uri));
			}
		}
		return ret;
	}
}