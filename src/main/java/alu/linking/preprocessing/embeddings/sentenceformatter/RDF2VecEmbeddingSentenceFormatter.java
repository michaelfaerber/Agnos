package alu.linking.preprocessing.embeddings.sentenceformatter;

public class RDF2VecEmbeddingSentenceFormatter extends EmbeddingSentenceFormatter {

	@Override
	public String formatOutput(String line, String entity, String lineDelim, int entityPos) {
		//Outputs full line - including entity etc
		String ret = line;
		//line.replace(entity, entity.trim()).replace(" ", "");
		//System.out.println("LINE:"+ret);
		return ret;
	}

}
