package alu.linking.mentiondetection.fuzzy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.semanticweb.yars.nx.Node;

import com.google.common.collect.Lists;

import alu.linking.candidategeneration.lsh.LSHSparseVector;
import alu.linking.config.constants.Numbers;
import alu.linking.mentiondetection.EnumDetectionType;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.structure.Loggable;
import alu.linking.utils.FuzzyUtils;
import alu.linking.utils.Stopwatch;
import info.debatty.java.lsh.LSHMinHash;

public class MentionDetectorLSH implements MentionDetector<Node>, Loggable {
	private boolean setup = false;
	private final AtomicInteger collisionCounter = new AtomicInteger(0);
	// Trigrams of all possible surface forms
	private final TreeMap<String, Integer> ngrams = new TreeMap<>();
	// Keyset of the linking map - all possible surface forms
	private final TreeSet<String> surface_forms = new TreeSet<String>();
	private final String tokenSeparator = " ";// space
	// Attention: to get relevant results, the number of elements per bucket
	// should be at least 100
	// Note that changing the seed, bands and/or buckets means all signatures must
	// be recomputed!

	// Constant / Default values
	private final int seed = 1337;
	private final static int bandsDefaultValue = Numbers.LSH_BANDS.val.intValue();
	private final static int bucketsDefaultValue = Numbers.LSH_BUCKETS.val.intValue();
	// LSH Variables
	private int bands = bandsDefaultValue;
	private int buckets = bucketsDefaultValue;
	private LSHMinHash lsh = null;
	private int[][] hashes = null;
	private final Map<String, Set<String>> linking;
	private LSHSparseVector<Boolean>[] document_vectors_sparse;
	// Text processing variables
	private final EnumDetectionType detectionType;
	private final int n_gram_length = 3;
	// Precomputed data variables
	final String outDocVectorsEntries = "./document_vectors_sparse_entries.txt";
	final String outHashes = "./hashes.txt";
	final String docArraySplitStr = "\t;\t";

	// JS similarity min. threshold
	private final double threshold;
	// Whether or not data was loaded
	private boolean loaded = false;
	private final String mentionLock = "mentionsList";

	// #################################
	// ########## CONSTRUCTOR ##########
	// #################################
	public MentionDetectorLSH(final Map<String, Set<String>> map) {
		this(map, 0.7);
	}

	/**
	 * Calls {@link #MentionDetectorLSH(Map, double, int, int)} with the given map,
	 * threshold and the bands and buckets default values
	 * 
	 */
	public MentionDetectorLSH(final Map<String, Set<String>> map, final double threshold) {
		this(map, threshold, bandsDefaultValue, bucketsDefaultValue);
	}

	/**
	 * 
	 * @param map
	 *            Map where keys represent the text occurrences that are possible to
	 *            be detected from input text
	 * @param threshold
	 *            similarity threshold for fuzzy matching
	 * @param value
	 *            either buckets or bands (other one will get its default value)
	 * @param bucketsVsBands
	 *            TRUE means that passed VALUE parameter represents the number of
	 *            buckets, FALSE means that it represents the number of bands
	 */
	public MentionDetectorLSH(final Map<String, Set<String>> map, final double threshold, final int value,
			final boolean bucketsVsBands) {
		this(map, threshold, bucketsVsBands ? bandsDefaultValue : value, bucketsVsBands ? value : bandsDefaultValue);
	}

	/**
	 * 
	 * @param map
	 *            Contains all possible String occurrences along with the associated
	 *            resources. Only its keyset will be used in practice (as this is
	 *            simple mention detection)
	 * @param threshold
	 *            Similarity threshold that should be passed for fuzzy matches
	 * @param bands
	 *            how many bands LSH should be computed with
	 * @param buckets
	 *            how many buckets LSH should be computed with
	 */
	public MentionDetectorLSH(Map<String, Set<String>> map, final double threshold, final int bands,
			final int buckets) {
		this(map, threshold, bands, buckets, EnumDetectionType.BOUND_DYNAMIC_WINDOW);
	}

	/**
	 * 
	 * @param map
	 *            Contains all possible String occurrences along with the associated
	 *            resources. Only its keyset will be used in practice (as this is
	 *            simple mention detection)
	 * @param threshold
	 *            Similarity threshold that should be passed for fuzzy matches
	 * @param bands
	 *            how many bands LSH should be computed with
	 * @param buckets
	 *            how many buckets LSH should be computed with
	 * @param detectionType
	 *            which type of tokenization should be applied to the input text
	 */
	public MentionDetectorLSH(Map<String, Set<String>> map, final double threshold, final int bands, final int buckets,
			EnumDetectionType detectionType) {
		this.linking = map;
		this.detectionType = detectionType;
		this.threshold = threshold;
		this.bands = bands;
		this.buckets = buckets;
	}

	/**
	 * Persists all necessary data structures to the filesystem so that they can be
	 * loaded back with load()
	 * 
	 * @throws IOException
	 */
	public void backup() throws IOException {
		// Output all sparse vector entries
		try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outDocVectorsEntries)))) {
			for (LSHSparseVector<Boolean> vec : document_vectors_sparse) {
				bwOut.write(vec.getEntries().toString());
				bwOut.newLine();
			}
		}
		// Output all the hashes
		try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outHashes)))) {
			Iterator<String> docIterator = surface_forms.iterator();
			for (int[] hash : hashes) {
				bwOut.write(docIterator.next());
				bwOut.write(docArraySplitStr);
				bwOut.write(Arrays.toString(hash));
				bwOut.newLine();
			}
		}
	}

	/**
	 * Loads all required data structures from backed up files.<br>
	 * Note that if any of the following has been done, setup() must be called
	 * appropriately (followed by backup()) before this method can be called:<br>
	 * 1) changing buckets<br>
	 * 2) bands(bins) <br>
	 * 3) underlying knowledge base<br>
	 * 
	 * @throws Exception
	 */
	public void load() throws Exception {
		if (loaded)
			return;
		Stopwatch.start(getClass().getName());
		getLogger().debug("Loading...");
		// Read all the document names & hashes appropriately
		surface_forms.clear();
		int lineCounter = 0;
		List<int[]> hashList = Lists.newArrayList();
		try (BufferedReader bwIn = new BufferedReader(new FileReader(new File(outHashes)))) {
			String line = null;
			while ((line = bwIn.readLine()) != null) {
				String[] tokens = line.split(docArraySplitStr);
				// aka. surface form
				final String docName = tokens[0];
				surface_forms.add(docName);
				final String[] arrayTokens = tokens[1].replace("[", "").replace("]", "").split(",");

				int[] array = new int[arrayTokens.length];
				hashList.add(array);
				int arrCounter = 0;
				for (String arrayToken : arrayTokens) {
					array[arrCounter++] = Integer.valueOf(arrayToken.trim());
				}
				lineCounter++;
			}
		}
		hashes = hashList.toArray(new int[lineCounter][]);

		// Based on the documents, fill up the bag of words
		ngrams.clear();
		final TreeSet<String> sortedNGrams = new TreeSet<>();
		for (String s : surface_forms) {
			for (String ngram : FuzzyUtils.generateNgrams(s, n_gram_length)) {
				sortedNGrams.add(ngram);
			}
		}
		int nGramPosCounter = 0;
		for (String ngram : sortedNGrams) {
			ngrams.put(ngram, nGramPosCounter++);
		}
		updateBuckets();
		// Recreate the sparse vectors
		try (BufferedReader bwIn = new BufferedReader(new FileReader(new File(outDocVectorsEntries)))) {
			String line = null;
			final List<LSHSparseVector<Boolean>> doc_vectors_list = Lists.newArrayList();
			while ((line = bwIn.readLine()) != null) {
				final String entries = line;
				final String[] entryTokens = entries.replace("[", "").replace("]", "").split(",");
				LSHSparseVector<Boolean> sparse_vec = LSHSparseVector.create(ngrams.size());
				for (String index : entryTokens) {
					sparse_vec.set(Integer.valueOf(index.trim()));
				}
				doc_vectors_list.add(sparse_vec);
			}
			this.document_vectors_sparse = doc_vectors_list.toArray(new LSHSparseVector[surface_forms.size()]);
		}

		// Set LSHMinHash
		this.lsh = new LSHMinHash(bands, buckets, this.ngrams.size(), this.seed);
		getLogger().info("Loading completed in " + Stopwatch.endDiff(getClass().getName()) + " ms.");
		loaded = true;
	}

	/**
	 * Short-hand call to {@link #detect(String, String)} with {@param source=null}
	 * 
	 * @param input
	 *            input text/corpus to detect mentions from
	 */
	@Override
	public List<Mention<Node>> detect(String input) {
		return detect(input, null);
	}

	/**
	 * @param input
	 *            input text/corpus to detect mentions from
	 * @param source
	 *            where this text comes from or what it is linked to
	 */
	@Override
	public List<Mention<Node>> detect(final String input, final String source) {
		try {
			getCollisionCounter().set(0);
			// this.hashes = setup();
			// backup all data
			// backup();
			load();
			final String[] words = input.replaceAll("\\p{Punct}", "").split("\\p{Space}");// POSIX class
			// Synchronized list
			final List<Mention<Node>> mentions = Lists.newArrayList();

			final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(Numbers.MENTION_DETECTION_THREAD_AMT.val.intValue());
			AtomicInteger doneCounter = new AtomicInteger(0);

			final StringBuilder combinedWords = new StringBuilder();
			int stringPos = 0;
			switch (detectionType) {
			case BOUND_DYNAMIC_WINDOW:
				final int windowSize = Numbers.MENTION_DETECTION_WINDOW_SIZE.val.intValue();
				combinedWords.setLength(0);
				stringPos = 0;
				for (int i = 0; i < words.length; ++i) {
					int subPos = stringPos;// + i*tokenSeparator.length();//+i due to spaces;
					for (int j = 0; j < windowSize; ++j) {
						if (i + j > words.length - 1) {
							break;
						}
						// final int index = Math.min(words.length - 1, i + j);
						final int index = i + j;
						if (j != 0) {
							combinedWords.append(tokenSeparator);
							subPos += tokenSeparator.length();
						}
						combinedWords.append(words[index]);
						execFind(executor, mentions, doneCounter, combinedWords.toString(), source, threshold, subPos);
						subPos += words[index].length();
					}
					combinedWords.setLength(0);
					stringPos += words[i].length() + tokenSeparator.length();
				}
				break;
			case SINGLE_WORD:
				// Just Single words
				stringPos = 0;
				for (String token : words) {
					execFind(executor, mentions, doneCounter, token, source, threshold, stringPos);
					stringPos += token.length();
				}
				break;
			case UNBOUND_DYNAMIC_WINDOW:
				/*
				 * Example: Input: I have a cat Processed as: I I have I have a I have a cat
				 * have have a have a cat ...
				 */
				// Just Single words
				stringPos = 0;
				for (String token : words) {
					execFind(executor, mentions, doneCounter, token, source, threshold, stringPos);
					stringPos += token.length();
				}
				// Multi-words (excluding single words)
				combinedWords.setLength(0);
				stringPos = 0;
				for (int i = 0; i < words.length; ++i) {
					combinedWords.append(words[i]);
					int subPos = stringPos;
					for (int j = i + 1; j < words.length; ++j) {
						combinedWords.append(tokenSeparator + words[j]);
						execFind(executor, mentions, doneCounter, combinedWords.toString(), source, threshold,
								stringPos);
						subPos += tokenSeparator.length() + words[j].length();
					}
					stringPos += words[i].length();
					combinedWords.setLength(0);
				}
				break;
			case UNBOUND_DYNAMIC_WINDOW_STRICT_MULTI_WORD:
				// Multi-words (excluding single words)
				combinedWords.setLength(0);
				stringPos = 0;
				for (int i = 0; i < words.length; ++i) {
					combinedWords.append(words[i]);
					int subPos = stringPos;
					for (int j = i + 1; j < words.length; ++j) {
						combinedWords.append(tokenSeparator + words[j]);
						execFind(executor, mentions, doneCounter, combinedWords.toString(), source, threshold,
								stringPos);
						subPos = tokenSeparator.length() + words[j].length();
					}
					stringPos += words[i].length();
					combinedWords.setLength(0);
				}
				break;
			default:
				break;
			}

			// No more tasks will be added
			executor.shutdown();
			do {
				// No need for await termination as this is pretty much it already...
				Thread.sleep(50);
				// getLogger().debug("Finished executing: " + doneCounter.get() + "
				// processes.");
			} while (!executor.isTerminated());
			getLogger().info("Collisions: " + collisionCounter.get());
			// Shouldn't wait at all generally, but in order to avoid unexpected behaviour -
			// especially relating to logic changes on the above busy-waiting loop
			final boolean terminated = executor.awaitTermination(10L, TimeUnit.MINUTES);
			if (!terminated) {
				getLogger().error("Executor has not finished terminating");
			}
			return mentions;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Submits a find task to the executor, adding the result to the mentions list,
	 * incrementing the 'done' counter<br>
	 * <b>Note</b>: Mentions list MUST be synchronized (e.g. through
	 * Collections.synchronizedList(List))
	 * 
	 * @param executor
	 * @param mentions
	 * @param doneCounter
	 * @param string
	 * @param source
	 * @param threshold
	 */
	private void execFind(final ThreadPoolExecutor executor, final List<Mention<Node>> mentions,
			final AtomicInteger doneCounter, final String string, final String source, final double threshold,
			final int indexPos) {
		executor.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				final Mention<Node> mention = find(string, source, threshold, indexPos);
				if (mention != null) {
					synchronized (mentionLock) {
						mentions.add(mention);
					}
				}
				return doneCounter.incrementAndGet();
			}
		});
	}

	/**
	 * Finds a mention for a given input token
	 * 
	 * @param input
	 *            token or word
	 * @param source
	 *            what entity this word is linked to
	 * 
	 * @param threshold
	 *            minimum similarity threshold for it to be accepted
	 * @return mention with the closest possible mate
	 * 
	 */
	public Mention<Node> find(final String input, final String source, final double threshold, final int offset) {
		final Object[] resMinHash = minhash(input, threshold);
		if (resMinHash == null) {
			return null;
		}
		final String word = (String) resMinHash[0];
		final double findConfidence = (double) resMinHash[1];
		if (word == null) {
			// No mention found within our knowledge base... which would match threshold
			// criterion, at least
			return null;
		}
		// getLogger().debug("Best Found word(" + input + "): " + word + " [" +
		// findConfidence + "] " + "; Duration: "
		// + Stopwatch.endDiff(watchName) + "ms.");

		// Create a mention with the best-found word
		final Mention<Node> mention = new Mention<Node>(word, source,
				null, offset, findConfidence, input);
		return mention;
	}

	/**
	 * Setup() done for pre-computing the LSH signatures etc.
	 * 
	 * @return
	 * @throws Exception
	 */
	public void setup() throws Exception {
		if (setup)
			return;// this.hashes;
		ngrams.clear();
		this.surface_forms.clear();
		this.surface_forms.addAll(this.linking.keySet());
		final TreeSet<String> allNGrams = new TreeSet<>();
		for (String word : this.linking.keySet()) {
			// This is a word we want to ngram and add
			for (String ngram : FuzzyUtils.generateNgrams(word, n_gram_length)) {
				allNGrams.add(ngram);
			}
		}
		int ngramPosCounter = 0;
		for (String ngram : allNGrams) {
			ngrams.put(ngram, ngramPosCounter++);
		}
		updateBuckets();
		// Precompute the vectors + hashes

		// Required as this.ngrams is not final
		final SortedMap<String, Integer> dictionary = this.ngrams;
		final TreeSet<String> documents = this.surface_forms;
		// Number of sets ('documents', I assume?)
		int documentSize = documents.size();
		// Size of dictionary
		this.document_vectors_sparse = new LSHSparseVector[documentSize];
		this.lsh = new LSHMinHash(bands, buckets, dictionary.size(), this.seed);

		// Create the vectors to execute hashes on afterwards
		Iterator<String> documentsIterator = documents.iterator();
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
		AtomicInteger doneCounter = new AtomicInteger(0);
		int[][] hashes = new int[documentSize][];
		for (int i = 0; i < documentSize; ++i) {
			final String document = documentsIterator.next();
			final int vectorIndex = i;
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					document_vectors_sparse[vectorIndex] = LSHSparseVector.create(dictionary.size());
					final LSHSparseVector<Boolean> vector = document_vectors_sparse[vectorIndex];
					// Then fill out the right values for this particular document
					final List<String> doc_ngrams = FuzzyUtils.generateNgrams(document, n_gram_length);
					for (final String ngram : doc_ngrams) {
						int index = dictionary.get(ngram);// getIndex(dictionary, ngram);
						vector.set(index);
					}
					// Now we can proceed to LSH binning
					// Compute the LSH hash of each vector
					hashes[vectorIndex] = lsh.hash(vector.toBooleanArray());
					// Returning the ID helps us potentially identify which ones have not completed
					// yet, if wanted in the future
					return doneCounter.incrementAndGet();
				}
			});
		}
		// No more tasks will be added
		executor.shutdown();
		do {
			// No need to await termination as this is pretty much it already...
			Thread.sleep(5_000);
			getLogger()
					.debug("Vector setup - In progress [" + doneCounter.get() + " / " + documentSize + "] documents.");
		} while (!executor.isTerminated());
		// Shouldn't wait at all generally, but in order to avoid unexpected behaviour -
		// especially relating to logic changes on the above busy-waiting loop
		executor.awaitTermination(10L, TimeUnit.MINUTES);
		//
		getLogger()
				.debug("Finished computing signatures for " + doneCounter.get() + " / " + documentSize + " documents!");

		this.hashes = hashes;
		setup = true;
	}

	private void updateBuckets() {
		// Update buckets based on dataset size
		// n = bands * rows per band
		// this.buckets = (int) (Math.ceil(Math.sqrt(((double) this.ngrams.size()))));
		// this.buckets = 200;
		if (buckets == bucketsDefaultValue) {
			this.buckets = Numbers.LSH_BUCKETS.val.intValue();
		}
		if (bands == bandsDefaultValue) {
			this.bands = Numbers.LSH_BANDS.val.intValue();
		}
		System.out.println("Bands: " + this.bands + "; Buckets: " + buckets);
	}

	/**
	 * Returns at which position within a given sorted set the passed 'word' is
	 * located, returns -1 if none found
	 * 
	 * @param bagOfWords
	 * @param word
	 * @return
	 */
	private int getIndex(SortedSet<String> bagOfWords, String word) {
		return bagOfWords.contains(word) ? bagOfWords.headSet(word).size() : -1;
	}

	/**
	 * Generates signature for input query and retrieves with given minimum
	 * threshold the best word for it!
	 * 
	 * @param query
	 *            input query which we want to fuzzily match with
	 * @param threshold
	 *            minimum threshold for Jaccard similarity to accept a word
	 * @return most similar word to passed one or null if threshold is not met
	 */
	private Object[] minhash(final String query, final double threshold) {
		try {
			final TreeMap<String, Integer> dictionary = this.ngrams;
			// Size of dictionary
			int n = dictionary.size();

			final LSHSparseVector<Boolean> query_vector_sparse = LSHSparseVector.create(n);

			final List<String> query_ngrams = FuzzyUtils.generateNgrams(query, n_gram_length);
			for (String ngram : query_ngrams) {
				int index = dictionary.getOrDefault(ngram, -1);// getIndex(dictionary, ngram);
				// index == -1 means that we are introducing a new ngram which didn't exist
				// before with our query aka. -> no hit possible with it, so we ignore it
				if (index != -1) {
					query_vector_sparse.set(index);
				}
			}
			final boolean[] query_vector_dense = query_vector_sparse.toBooleanArray();
			final int[] query_hash = lsh.hash(query_vector_dense);
			final Map<Integer, Double> similarDocs = findSimilarEntries(query_vector_sparse, query_hash, threshold,
					document_vectors_sparse, hashes);
			if (similarDocs == null || similarDocs.size() == 0) {
				return null;
			}
			final List<String> retrievalList = Lists.newArrayList(surface_forms);
			double maxVal = 0d;
			int maxValIndex = -1;
			for (Map.Entry<Integer, Double> e : similarDocs.entrySet()) {
				if (e.getValue() > maxVal) {
					maxValIndex = e.getKey();
					maxVal = e.getValue();
				}
			}
			// final Map<Integer, Double> sortedMap =
			// AnalysisUtils.sortByValue(similarDocs);
			// for (Map.Entry<Integer, Double> e : sortedMap.entrySet()) {
			// System.out.println(retrievalList.get(e.getKey()) + " - " + e.getValue());
			// }

			// for (Map.Entry<Integer, Double> e : sortedMap.entrySet()) {
			// Just return the best word
			// return new Object[] { retrievalList.get(e.getKey()), e.getValue() };
			// }
			String retrievedWord = retrievalList.get(maxValIndex);
			return new Object[] { retrievedWord, maxVal };
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Map<Integer, Double> findSimilarEntries(LSHSparseVector<Boolean> queryDocument, int[] query_hash,
			double threshold, LSHSparseVector<Boolean>[] document_vectors_sparse, int[][] hashes) {
		final Map<Integer, Double> ret = new TreeMap<Integer, Double>();
		// getLogger().debug("Hash - Query: " + Arrays.toString(query_hash));
		for (int i = 0; i < document_vectors_sparse.length; i++) {
			LSHSparseVector<Boolean> doc2 = document_vectors_sparse[i];
			// We compute the similarity between each pair of sets
			double similarity = 0d;
			// System.out.println("Query dim: " + query_hash.length + "; Compare Dim.:" +
			// hash2.length);
			// System.out.println("Query hash: " + query_hash.length);
			// System.out.println("hash2: " + hash2.length);
			final int[] hash2 = hashes[i];
			final boolean oneOrMoreSimilar = FuzzyUtils.sameHashOnSameIndex(query_hash, hash2);
			//Just for understanding purposes
			final boolean possiblyFits = oneOrMoreSimilar;
			if (possiblyFits) {
				try {
					similarity = FuzzyUtils.jaccardSimilarity(queryDocument, doc2);
					// MinHash.jaccardIndex(doc1, doc2);
					if (similarity >= threshold) {
					//if (FuzzyUtils.reachesThreshold(queryDocument.getEntries(), doc2.getEntries(), threshold)) {
						// Add index of the document to ret list
						ret.put(i, similarity);
						//ret.put(i, threshold);
					} else {
						collisionCounter.incrementAndGet();
					}
				} catch (NullPointerException npe) {
					getLogger().error("NPE during JS computation for [" + queryDocument + "], doc2[" + doc2 + "]");
					throw npe;
					// continue;
				}
			} else {
			}
		}
		return ret;
	}

	public AtomicInteger getCollisionCounter() {
		return this.collisionCounter;
	}

	@Override
	public void init() throws Exception {
		load();
	}
}
