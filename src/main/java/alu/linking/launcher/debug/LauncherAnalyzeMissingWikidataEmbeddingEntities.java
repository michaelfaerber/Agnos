package alu.linking.launcher.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.io.nif.DocumentListParser;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.NIFTransferPrefixMapping;
import org.aksw.gerbil.transfer.nif.Span;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;

import alu.linking.eval.TestMarking;

public class LauncherAnalyzeMissingWikidataEmbeddingEntities {

	public static void main(String[] args) {
		try {
			new LauncherAnalyzeMissingWikidataEmbeddingEntities().run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() throws FileNotFoundException {
		final String outPath = "./evaluation_entities.tsv";
		final String inPath = //
				"./evaluation/" + //
				// "kore50-nif_steve_jobs.ttl"
				// "kore50-nif.ttl"
				// "kore50_yago.ttl"
				// "kore50_crunchbase.ttl"
						"kore50_wikidata.ttl"

		// "dbpedia-spotlight-nif.ttl"
		//
		;
		final File outFile = new File(outPath);
		final File fileKORE50 = new File(inPath);
		System.out.println("Input Data: " + fileKORE50.getAbsolutePath());
		if (!fileKORE50.exists()) {
			throw new FileNotFoundException(
					"Could not find the evaluation input file at: " + fileKORE50.getAbsolutePath());
		}

		List<Document> documents = null;
		List<Document> copyDocuments = null;
		try {
			documents = parseDocuments(fileKORE50);
			copyDocuments = parseDocuments(fileKORE50);
		} catch (FileNotFoundException e) {
			documents = null;
			e.printStackTrace();
			throw e;
		}
		// ################# Init read evaluation documents - END ######################

		// Now iterate through the documents

		// Process one document after the other
		try (final BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
			for (int i = 0; i < documents.size(); ++i) {
				final Document inputDoc = documents.get(i);
				// Compare "result" doc's markings and the "input" doc's
				final List<Marking> markings = copyDocuments.get(i).getMarkings();
				final String inputText = inputDoc.getText();
				// Doing this for sentence-based comparison, but not necessary if separate files
				// are used for instance
				final Set<TestMarking> testMarkings = transformToTestMarkings(markings, inputText);
				final int size = testMarkings.size();
				int counter = 0;
				for (TestMarking mark : testMarkings) {
					bw.write(mark.toString());
					if (++counter < size) {
						bw.write("\t");
					}
				}
				bw.write("\n");
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
				System.err.println("No URI passed...");
			} else if (uris.size() > 1) {
				System.err.println(uris.size() + " URIs passed...");
			}
			for (String uri : uris) {
				ret.add(new TestMarking(text, uri));
			}
		}
		return ret;
	}

	private List<Document> parseDocuments(File fileKORE50) throws FileNotFoundException {
		return parseDocuments(new BufferedReader(new FileReader(fileKORE50)));
	}

	private List<Document> parseDocuments(Reader reader) throws FileNotFoundException {
		final Model nifModel = parseNIFModel(reader, getDefaultModel());
		final DocumentListParser docListParser = new DocumentListParser();
		return docListParser.parseDocuments(nifModel);
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

}