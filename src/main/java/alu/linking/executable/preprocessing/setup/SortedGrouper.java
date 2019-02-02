package alu.linking.executable.preprocessing.setup;

import alu.linking.config.constants.Strings;
import alu.linking.preprocessing.embeddings.sentenceformatter.RDF2VecEmbeddingSentenceFormatter;
import alu.linking.structure.Executable;
import de.dwslab.petar.walks.StringDelims;

public class SortedGrouper implements Executable {

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		if (o == null || o.length < 1) {
			throw new IllegalArgumentException("No arguments");
		}

		if (o[0] == null || o[1] == null || !(o[0] instanceof String) || !(o[1] instanceof String)) {
			throw new IllegalArgumentException("First argument should be input(" + o[0]
					+ "). Second should be output(" + o[1] + ")...");
		}
		final String sortOutput = o[0].toString();
		final String sentencesOut = o[1].toString();
		getLogger().info("Grouping into: "+sentencesOut);
		final String DELIM = Strings.EMBEDDINGS_SENTENCES_DELIM.val;
		// Now group the walked paths into the appropriate sentences
		new RDF2VecEmbeddingSentenceFormatter().groupSortedFile(sortOutput, sentencesOut, DELIM,
				StringDelims.WALK_DELIM, 0);
		
		return null;
	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

}
