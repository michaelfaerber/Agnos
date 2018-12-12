package alu.linking.preprocessing.embeddings.sentenceformatter;

public class SSPEmbeddingSentenceFormatter extends EmbeddingSentenceFormatter {

	@Override
	public String formatOutput(String line, String entity, String lineDelim, int entityPos) {
		// Outputs line without the entity
		if (entityPos == 0) {
			return line.replaceFirst(entity + lineDelim, "").trim();
		} else {
			throw new RuntimeException("Not yet implemented for other key position for SSP embeddings");
		}
	}

}
