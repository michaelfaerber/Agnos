package alu.linking.preprocessing.embeddings.posttraining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.embeddings.sentenceformatter.SSPEmbeddingSentenceFormatter;
import alu.linking.utils.EmbeddingsUtils;

public class GraphWalkHandler {
	final EnumModelType KG;

	GraphWalkHandler(final EnumModelType KG) {
		this.KG = KG;
	}

	/**
	 * Reads computed word embeddings and computes required entity embeddings based
	 * on them. <br>
	 * Entity Embeddings are combined through addition.<br>
	 * <b>Note</b>: If you want to define a custom combination function, please use
	 * {@link #computeRequiredEntityEmbeddings(BiFunction)} instead
	 */
	public void computeRequiredEntityEmbeddings() {
		computeRequiredEntityEmbeddings(EmbeddingsUtils::add);
	}

	/**
	 * Reads computed word embeddings and computes required entity embeddings based
	 * on them. <br>
	 * Entity Embeddings are combined through use of the passed BiFunction
	 */
	public void computeRequiredEntityEmbeddings(
			BiFunction<List<Number>, List<Number>, List<Number>> embeddingCombineFunction) {
		try {
			// Reads the generated word embeddings
			final Map<String, List<Number>> word_embeddings = EmbeddingsUtils
					.readEmbeddings(new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG)));
			// Now rebuild all of the word embeddings into entity embeddings
			final String ssp_embedding_rep = FilePaths.FILE_EMBEDDINGS_GAPH_WALK_TRAINED_EMBEDDINGS REALLY?.getPath(KG);
			final SSPEmbeddingSentenceFormatter ssp_formatter = new SSPEmbeddingSentenceFormatter();
			final int ssp_key_index = 0;
			final String ssp_textdata_sorted_delim = Strings.QUERY_RESULT_DELIMITER.val;
			final Map<String, List<Number>> entityEmbeddingMap = new HashMap<>();
			try (final BufferedReader brIn = new BufferedReader(new FileReader(new File(ssp_embedding_rep)))) {
				String line = null;
				while ((line = brIn.readLine()) != null) {
					final String[] words = line.split(ssp_textdata_sorted_delim);
					final String key = ssp_formatter.extractKey(line, ssp_key_index, ssp_textdata_sorted_delim);
					// Embeddings for this line's sentence or sentence part are computed and added
					// to what we already have
					// Note: Sentences may and likely do span multiple lines, hence our multi-line
					// sorted logic
					final List<Number> embedding = embeddingCombineFunction.apply(entityEmbeddingMap.get(key),
							EmbeddingsUtils.rebuildSentenceEmbedding(word_embeddings, Arrays.asList(words),
									embeddingCombineFunction));
					// Overwrite the value we have as an embedding currently
					entityEmbeddingMap.put(key, embedding);
				}
			}
			// Now we should back up the proper entity embeddings for easier access later on
			// Raw dump
			final ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(FilePaths.FILE_EMBEDDINGS_SSP_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG)));
			oos.writeObject(entityEmbeddingMap);
			oos.close();
			// Human-readable dump
			try (final BufferedWriter bwOut = new BufferedWriter(
					new FileWriter(new File(FilePaths.FILE_EMBEDDINGS_SSP_ENTITY_EMBEDDINGS.getPath(KG))))) {
				final String outDelim = Strings.EMBEDDINGS_ENTITY_EMBEDDINGS_DUMP_DELIMITER.val;
				for (Map.Entry<String, List<Number>> e : entityEmbeddingMap.entrySet()) {
					bwOut.write(e.getKey());
					for (Number n : e.getValue()) {
						bwOut.write(outDelim);
						bwOut.write(n.toString());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
