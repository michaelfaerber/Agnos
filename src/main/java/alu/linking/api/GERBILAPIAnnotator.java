package alu.linking.api;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;

import com.beust.jcommander.internal.Lists;

import alu.linking.structure.Executable;

public class GERBILAPIAnnotator implements Executable {

	@Override
	public void init() {
		// Load all the necessary stuff
		// such as embeddings, LSH sparse vectors and hashes
	}

	public String annotate(final Reader inputReader) {
		// 1. Generate a Reader, an InputStream or a simple String that contains the NIF
		// sent by GERBIL
		// 2. Parse the NIF using a Parser (currently, we use only Turtle)
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			document = parser.getDocumentFromNIFReader(inputReader);
		} catch (Exception e) {
			getLogger().error("Exception while reading request.", e);
			return "";
		}
		// 3. use the text and maybe some Markings sent by GERBIL to generate your
		// Markings
		// (a.k.a annotations) depending on the task you want to solve
		// 4. Add your generated Markings to the document

		document.setMarkings(new ArrayList<Marking>(annotateSafely(document)));
		// 5. Generate a String containing the NIF and send it back to GERBIL
		final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
		final String nifDocument = creator.getDocumentAsNIFString(document);
		return nifDocument;
	}

	private Collection<? extends Marking> annotateSafely(Document document) {
		final List<Marking> retList = Lists.newArrayList();
		// new ScoredNamedEntity(startPosition, length, uris, confidence);
		// new Mention()... transform mention into a scored named entity

		return null;
	}

	@Override
	public String exec(Object... o) throws Exception {
		if (o != null && o.length > 0) {
			Reader inputReader = null;
			for (Object obj : o) {
				if (obj instanceof Reader) {
					inputReader = (Reader) obj;
					break;
				}
			}
			if (inputReader != null) {
				return annotate(inputReader);
			}
		}
		return null;
	}

	@Override
	public boolean destroy() {
		// Tear down all the loaded data structures
		return false;
	}
}
