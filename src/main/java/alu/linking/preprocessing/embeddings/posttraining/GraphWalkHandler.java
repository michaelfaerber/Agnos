package alu.linking.preprocessing.embeddings.posttraining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
import alu.linking.preprocessing.embeddings.sentenceformatter.EmbeddingSentenceFormatter;
import alu.linking.preprocessing.embeddings.sentenceformatter.RDF2VecEmbeddingSentenceFormatter;
import alu.linking.utils.EmbeddingsUtils;

/**
 * Class handling embeddings computed through graph walks (combining and
 * transforming existing embeddings)
 * 
 * @author Kristian Noullet
 *
 */
public class GraphWalkHandler {
	final EnumModelType KG;

	public GraphWalkHandler(final EnumModelType KG) {
		this.KG = KG;
	}

	/**
	 * Reads computed word embeddings and computes required entity embeddings based
	 * on them. <br>
	 * Entity Embeddings are combined through addition.<br>
	 * <b>Note</b>: If you want to define a custom combination function, please use
	 * {@link #computeRequiredEntityEmbeddings(BiFunction)} instead
	 * 
	 * @Deprecated Done in python now (way more efficient RAM-wise)
	 */
	@Deprecated
	public void computeRequiredEntityEmbeddings() {
		computeRequiredEntityEmbeddings(EmbeddingsUtils::add);
	}

	/**
	 * Reads computed word embeddings and computes required entity embeddings based
	 * on them. <br>
	 * Entity Embeddings are combined through use of the passed BiFunction
	 * 
	 * @Deprecated Done in python now (way more efficient RAM-wise)
	 */
	@Deprecated
	public void computeRequiredEntityEmbeddings(
			BiFunction<List<Number>, List<Number>, List<Number>> embeddingCombineFunction) {
		try {
			// Reads the generated word embeddings
			final Map<String, List<Number>> word_embeddings = EmbeddingsUtils.readEmbeddings(new File(""));// FilePaths.FILE_EMBEDDINGS_GAPH_WALK_TRAINED_EMBEDDINGS.getPath(KG)));
			// Now rebuild all of the word embeddings into entity embeddings
			// Take the sentences and combine them as wanted for each entity (one line = one
			// entity)
			final String rdf2vec_embedding_rep = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(KG);
			final EmbeddingSentenceFormatter rdf2vec_formatter = new RDF2VecEmbeddingSentenceFormatter();
			final int rdf2vec_key_index = 0;
			final String rdf2vec_sorted_delim = Strings.EMBEDDINGS_RDF2VEC_SPLIT_DELIM.val;
			final Map<String, List<Number>> entityEmbeddingMap = new HashMap<>();
			int workedCounter = 0;
			try (final BufferedReader brIn = new BufferedReader(new FileReader(new File(rdf2vec_embedding_rep)))) {
				String line = null;
				while ((line = brIn.readLine()) != null) {
					final String[] words = line.split(rdf2vec_sorted_delim);
					final String key = rdf2vec_formatter.extractKey(line, rdf2vec_key_index, rdf2vec_sorted_delim);
					// Embeddings for this line's sentence or sentence part are computed and added
					// to what we already have
					// Note: Sentences may and likely do span multiple lines, hence our multi-line
					// sorted logic
					// EDIT: Can't remember why the above holds. Likely copied from sorting logic?
					final List<Number> left = entityEmbeddingMap.get(key);// (((left = entityEmbeddingMap.get(key)) ==
																			// null) ? Lists.newArrayList() : left);
					final List<Number> rebuiltSentence = EmbeddingsUtils.rebuildSentenceEmbedding(word_embeddings,
							Arrays.asList(words), embeddingCombineFunction);
					if (rebuiltSentence == null) {
						System.err.println("Words: " + Arrays.asList(words));
						int foundEmbedding = 0;
						for (String word : words) {
							foundEmbedding += (word_embeddings.get(word) != null ? 1 : 0);
						}
						System.out.println("Found embeddings for " + foundEmbedding + " / " + words.length);
						throw new RuntimeException("A rebuilt sentence embedding should not be null...");
					}

					final List<Number> embedding = embeddingCombineFunction.apply(left, rebuiltSentence);
					if (embedding != null && embedding.size() > 0) {
						workedCounter++;
					}
					// Overwrite the value we have as an embedding currently
					entityEmbeddingMap.put(key, embedding);
				}
			}
			// ------------------------------------------
			// Now we should back up the proper entity
			// embeddings for easier access later on
			// ------------------------------------------
			// Raw dump
			final ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG)));
			oos.writeObject(entityEmbeddingMap);
			oos.close();
			// Human-readable dump
			try (final BufferedWriter bwOut = new BufferedWriter(
					new FileWriter(new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG))))) {
				final String outDelim = Strings.EMBEDDINGS_ENTITY_EMBEDDINGS_DUMP_DELIMITER.val;
				for (Map.Entry<String, List<Number>> e : entityEmbeddingMap.entrySet()) {
					bwOut.write(e.getKey());
					for (Number n : e.getValue()) {
						bwOut.write(outDelim);
						bwOut.write(n.toString());
					}
					bwOut.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Newer version of {@link #computeRequiredEntityEmbeddings()} Reads computed
	 * entity embeddings (done via python) and constructs a HashMap based on it
	 * which is dumped in its raw byte format for faster loading for disambiguation
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void readPythonEntityEmbeddingsOutputHashMap() throws FileNotFoundException, IOException {
		final Map<String, List<Number>> entityEmbeddingMap = EmbeddingsUtils
				.readEmbeddings(new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG)));
		// ------------------------------------------
		// Now we should back up the proper entity
		// embeddings for easier access later on
		// ------------------------------------------
		// Raw dump
		try (FileOutputStream fos = new FileOutputStream(
				FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG))) {
			try (final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				oos.writeObject(entityEmbeddingMap);
			}
		}

	}
}
