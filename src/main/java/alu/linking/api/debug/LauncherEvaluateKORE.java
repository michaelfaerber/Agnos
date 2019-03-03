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
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.apache.log4j.Logger;

import alu.linking.api.GERBILAPIAnnotator;
import alu.linking.config.kg.EnumModelType;

public class LauncherEvaluateKORE {

	public static void main(String[] args) {
		final File fileKORE50 = new File("./evaluation/kore50-nif.ttl");
		// final String inputDir = "./evaluation/min_example.ttl";
		// final String outputDir = "./evaluation/annotated/";
		final GERBILAPIAnnotator annotator = new GERBILAPIAnnotator(EnumModelType.DBPEDIA_FULL);
		annotator.init();
		final TurtleNIFParser parser = new TurtleNIFParser();
		List<Document> documents = null;
		try {
			documents = parser.parseNIF(new FileInputStream(fileKORE50));
		} catch (FileNotFoundException e) {
			documents = null;
			e.printStackTrace();
		}

		// Process one document after the other
		for (Document inputDoc : documents) {

			getLogger().info("Evaluating:" + GERBILAPIAnnotator.smallText(inputDoc.getText()));
			try {
				final String results = annotator.annotate(inputDoc.getText());
				// Read the output results and compare to the one I was handed
				getLogger().info("########################## Results: ##########################");
				getLogger().info(results);
				getLogger().info("##############################################################");
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
				compareMarkings(inputDoc.getMarkings(), resultDoc.getMarkings(), inputDoc.getText());
			} catch (IOException ioe) {
				getLogger().error("IOE Exception happened... ", ioe);
				break;
			} catch (Exception e) {
				getLogger().error("(Parser?) Exception happened... ", e);
				break;
			}
		}
		getLogger().info("Finished successfully!");
	}

	private static void compareMarkings(final List<Marking> inputMarkings, final List<Marking> resultMarkings,
			final String inputText) {
		final Set<TestMarking> setInputMarkings = transformToTestMarkings(inputMarkings, inputText);
		final Set<TestMarking> setResultMarkings = transformToTestMarkings(resultMarkings, inputText);
		double tp = 0, fp = 0, tn = 0, fn = 0;
		for (TestMarking m : setInputMarkings) {
			// Iterate through set, check if each is also in the result one
			if (setResultMarkings.contains(m)) {
				tp++;
			} else {
				fn++;
			}
		}

		for (TestMarking m : setResultMarkings) {
			// Iterate through set, check if each is also in the result one
			if (setInputMarkings.contains(m)) {
				// tp++;
			} else {
				fp++;
			}
		}
		final String outStr = "Markings TRUE(" + setInputMarkings.size() + ") / Found(" + setInputMarkings.size()
				+ "): TP(" + tp + "), FN(" + fn + "), FP(" + fp + "), TN(" + tn + ")";
		final double precision = tp / (tp + fp);
		final double recall = tp / (tp + fn);
		final double f1 = 2 * precision * recall / (precision + recall);
		getLogger().info(GERBILAPIAnnotator.smallText(inputText));
		getLogger().info(outStr);
		getLogger().info("Precision: " + precision);
		getLogger().info("Recall: " + recall);
		getLogger().info("F1: " + f1);
		getLogger().info("################################################################");
		getLogger().info("###################### END OF DOCUMENT #########################");
		getLogger().info("################################################################");

	}

	private static Set<TestMarking> transformToTestMarkings(Collection<Marking> markings, final String inputText) {
		final Set<TestMarking> ret = new HashSet<>();
		for (Marking mark : markings) {
			final NamedEntity ne = (NamedEntity) mark;
			final String text = inputText.substring(ne.getStartPosition(), ne.getLength());
			final Set<String> uris = ne.getUris();
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

	private static Logger getLogger() {
		return Logger.getLogger(LauncherEvaluateKORE.class);
	}
}