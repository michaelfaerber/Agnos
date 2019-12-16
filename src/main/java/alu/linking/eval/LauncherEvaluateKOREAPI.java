package alu.linking.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.rmi.UnexpectedException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.io.nif.DocumentListParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.NIFTransferPrefixMapping;
import org.aksw.gerbil.transfer.nif.Span;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;

import alu.linking.api.GERBILAPIAnnotator;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.PageRankLoader;
import alu.linking.structure.Loggable;
import alu.linking.utils.TextUtils;

public class LauncherEvaluateKOREAPI implements Loggable {

	static FileWriter outputWrt;
	static boolean saveToFile = true;

	public static void main(String[] args) {
		try {
			final String outPath = "./evaluation_out.txt";
			outputWrt = new FileWriter(new File(outPath));
			new LauncherEvaluateKOREAPI().run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() throws FileNotFoundException {
		final EnumModelType KG = EnumModelType.//
				//WIKIDATA
		CRUNCHBASE
		// DBPEDIA_FULL
		;

		final String inPath = //
				"./evaluation/" + //
				// "kore50-nif_steve_jobs.ttl"
				// "kore50-nif.ttl"
				// "kore50_yago.ttl"
				//"kore50_crunchbase.ttl"
				"kore50_crunchbase_tiger_woods.ttl"
						//"kore50_wikidata.ttl"

		// "dbpedia-spotlight-nif.ttl"
		//
		;
		final File fileKORE50 = new File(inPath);
		System.out.println("Input Data: " + fileKORE50.getAbsolutePath());
		if (!fileKORE50.exists()) {
			throw new FileNotFoundException(
					"Could not find the evaluation input file at: " + fileKORE50.getAbsolutePath());
		}

		// ################# Init read evaluation documents - START
		// ######################
		// (To make sure they work, since otherwise we lose a lot of time with
		// initialization)
		final TurtleNIFParser parser = new TurtleNIFParser();
		List<Document> documents = null;
		List<Document> copyDocuments = null;
		try {
			documents = parseDocuments(fileKORE50);
			copyDocuments = parseDocuments(fileKORE50);
			// documents = parser.parseNIF(new BufferedReader(new
			// FileReader(fileKORE50)));// new FileInputStream(fileKORE50));
			// copyDocuments = parser.parseNIF(new FileInputStream(fileKORE50));
		} catch (FileNotFoundException e) {
			documents = null;
			e.printStackTrace();
			throw e;
		}
		// ################# Init read evaluation documents - END ######################

		// ################# Init linking - START ######################
		// final String inputDir = "./evaluation/min_example.ttl";
		// final String outputDir = "./evaluation/annotated/";
		final GERBILAPIAnnotator annotator = new GERBILAPIAnnotator(KG);
		annotator.init();
		// ################# Init linking - END ######################

		// Now iterate through the documents

		final List<EvaluationResult> evaluationResults = Lists.newArrayList();

		// Process one document after the other
		for (int i = 0; i < documents.size(); ++i) {
			final Document inputDoc = documents.get(i);
			output("Evaluating:" + TextUtils.smallText(inputDoc.getText()));
			try {
				final String results = annotator.annotateDocument(inputDoc);
				// Read the output results and compare to the one I was handed
//				output("########################## Results: ##########################");
//				output(results);
//				output("##############################################################");
				System.out.println("Results = " + results);
				final Document resultDoc = parseDocument(new StringReader(results));// new
																					// TurtleNIFDocumentParser().getDocumentFromNIFString(results);
//			Document resultDoc = null;
//			switch (resultDocs.size()) {
//			case 0:
//				getLogger().error("No document returned...");
//				throw new RuntimeException("No document returned...");
//			case 1:
//				// Correct
//				output("Document retrieved!");
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
				getLogger().error("IO Exception happened... ", ioe);
				break;
			} catch (Exception e) {
				getLogger().error("(Parser?) Exception happened... ", e);
				break;
			}
		}

		// Display the overall metrics now
		output("########################################");
		output("########### LOCAL (Macro) ##############");
		output("########################################");
		displayEvaluation(evaluationResults);

		// Display the overall metrics now
		output("##############################################################");
		output("########### LOCAL (Filtered 100% INCORRECT finds) ############");
		output("########### This answers to the question:         ############");
		output("##### If we get one correctly, how good is it overall? #######");
		output("##############################################################");
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

		output("#######################################");
		output("########### GLOBAL (Micro) ############");
		output("#######################################");
		displayTruthValues(EvaluationResult.globalTP, EvaluationResult.globalTN, EvaluationResult.globalFP,
				EvaluationResult.globalFN);
		output("Finished successfully!");
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
		output("Missing PR values(" + PageRankLoader.setPRNotFound.size() + "): [" + sbMissingPR.toString() + "]");
	}

	private void displayEvaluation(final List<EvaluationResult> evaluationResults) {
		if (evaluationResults == null || evaluationResults.size() == 0) {
			System.err.println("No evaluation results passed to evaluate...");
		}
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
		output("Precision Sum: " + precisionSum + " = " + sbPrecisionSum.toString());
		output("Recall Sum: " + recallSum + " = " + sbRecallSum.toString());
		output("F1 Sum: " + f1Sum + " = " + sbF1Sum.toString());
		output("Precision Avg.: " + avgPrecision);
		output("Recall Avg.: " + avgRecall);
		output("F1 Avg.: " + avgF1);
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
		output("TP(" + TP + "), TN(" + TN + "), FP(" + FP + "), FN(" + FN + ")");
		output("Precision:" + globalPrecision);
		output("Recall:" + globalRecall);
		output("F1:" + globalF1);
	}

	private EvaluationResult evaluateMarkings(final List<Marking> inputMarkings, final List<Marking> resultMarkings,
			final String inputText) {
		final EvaluationResult evaluationResult = new EvaluationResult();
		final Set<TestMarking> setInputMarkings = transformToTestMarkings(inputMarkings, inputText);
		final Set<TestMarking> setResultMarkings = transformToTestMarkings(resultMarkings, inputText);
		output("Input: List(" + inputMarkings.size() + "), Set(" + setInputMarkings.size() + ")");
		output("Result: List(" + resultMarkings.size() + "), Set(" + setResultMarkings.size() + ")");
		output("Input Set:" + setInputMarkings);
		output("Result Set:" + setResultMarkings);
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
		output(TextUtils.smallText(inputText));
		output(outStr);
		output("Precision: " + precision);
		output("Recall: " + recall);
		output("F1: " + f1);
		output("################################################################");
		output("###################### END OF DOCUMENT #########################");
		output("################################################################");

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

	protected Model getDefaultModel() {
		Model nifModel = ModelFactory.createDefaultModel();
		nifModel.setNsPrefixes(NIFTransferPrefixMapping.getInstance());
		return nifModel;
	}

	protected Model parseNIFModel(Reader reader, Model nifModel) {
		// RDFReaderRIOT rdfReader = new RDFReaderRIOT_TTL();
		RDFReaderRIOT rdfReader = new RDFReaderRIOT("TTL");
		rdfReader.read(nifModel, reader, "");
		return nifModel;
	}

	private Document parseDocument(File fileKORE50) throws FileNotFoundException, UnexpectedException {
		return parseDocument(new BufferedReader(new FileReader(fileKORE50)));
	}

	private List<Document> parseDocuments(File fileKORE50) throws FileNotFoundException {
		return parseDocuments(new BufferedReader(new FileReader(fileKORE50)));
	}

	private Document parseDocument(Reader reader) throws FileNotFoundException, UnexpectedException {
		final List<Document> documents = parseDocuments(reader);
		if (documents.size() == 1) {
			return documents.get(0);
		} else {
			throw new UnexpectedException("Expected to receive 1 document, instead received "
					+ (documents == null ? "null" : documents.size()));
		}
	}

	private List<Document> parseDocuments(Reader reader) throws FileNotFoundException {
		final Model nifModel = parseNIFModel(reader, getDefaultModel());
		final DocumentListParser docListParser = new DocumentListParser();
		return docListParser.parseDocuments(nifModel);
	}

	private void output(final String msg) {
		if (saveToFile) {
			try {
				outputWrt.write(msg + Strings.NEWLINE.val);
				outputWrt.flush();
			} catch (IOException e) {
				System.out.println("[IOError] " + msg);
			}
		} else {
			getLogger().info(msg);
		}
	}

}