package alu.linking.disambiguation.scorers;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Lists;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.PostScorer;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.EmbeddingsUtils;

public class GraphWalkEmbeddingScorer<N> implements PostScorer<PossibleAssignment<N>, Mention<N>>, Loggable {
	private final EnumModelType KG;
	private final Map<String, List<Number>> entityEmbeddingsMap;
	private boolean hasChanged = true;
	private Collection<Mention<N>> context;
	private final Set<String> bestCombination = new HashSet<>();

	public GraphWalkEmbeddingScorer(final EnumModelType KG)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		this.KG = KG;
		final boolean RAW_LOAD = false;
		if (RAW_LOAD) {
			Map<String, List<Number>> helperMap = null;
			try {
				try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
						new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG))))) {
					helperMap = (Map<String, List<Number>>) ois.readObject();
				}
			} catch (EOFException eof) {
				System.out.println("Exception!");
			} finally {
				System.out.println("HelperMap: " + helperMap);
				this.entityEmbeddingsMap = helperMap;
			}
		} else {
			this.entityEmbeddingsMap = EmbeddingsUtils
					.readEmbeddings(new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG)));

		}
	}

	@Override
	public Number computeScore(PossibleAssignment<N> assignment) {
		synchronized ("hasChanged") {
			if (hasChanged) {
				recomputeOptimum();
				hasChanged = false;
			}
		}
		if (bestCombination.contains(assignment.toString())) {
			// This assignment is similar to the final wanted combination!
			return 1;
		} else {
			// This assignment is NOT similar to the final wanted combination.
			return 0;
		}
	}

	/**
	 * Recomputes the best combination of entities to minimize distance
	 */
	private synchronized void recomputeOptimum() {
		int c = 0;
		try {
			getLogger().info(c++);
			bestCombination.clear();
			// Compute clusters based on assignments
			final List<List<String>> clusters = Lists.newArrayList();
			final Map<String, List<String>> clusterMap = new HashMap<>();
			getLogger().info(c++);
			for (Mention<N> m : context) {
				clusterMap.putIfAbsent(m.getMention(), Lists.newArrayList());
				final List<String> cluster = clusterMap.get(m.getMention());
				for (PossibleAssignment<N> ass : m.getPossibleAssignments()) {
					cluster.add(ass.getAssignment().toString());
				}
			}
			getLogger().info(c++);

			// Add the detected clusters to an encompassing list
			for (Map.Entry<String, List<String>> e : clusterMap.entrySet()) {
				clusters.add(e.getValue());
			}
			getLogger().info(c++);

			// Compute all possible clusters
			final List<List<String>> clusterPermutations = EmbeddingsUtils.findPermutations(clusters);
			getLogger().info(c++);

			// Compute optimal combination (via similarity/distances)
			final Map<String, Number> optimalSimilarityMap = new HashMap<>();
			Number optSimilaritySum = null;
			int optPermutationIndex = -1;
			getLogger().info(c++);
			for (int clusterIndex = 0; clusterIndex < clusterPermutations.size(); ++clusterIndex) {
				final List<String> permutation = clusterPermutations.get(clusterIndex);
				Number similaritySum = 0d;
				// Summing up distances/similarity to determine optimal entity combination (aka.
				// 'cluster')
				for (int i = 0; i < permutation.size(); i++) {
					for (int j = i+1; j < permutation.size(); j++) {
						if (i != j) {
							final List<Number> leftEmbedding = this.entityEmbeddingsMap.get(permutation.get(i));
							final List<Number> rightEmbedding = this.entityEmbeddingsMap.get(permutation.get(j));
							//getLogger().info("l:" + permutation.get(i) + " " + (leftEmbedding == null));
							//getLogger().info("r:" + permutation.get(j) + " " + (rightEmbedding == null));
							//getLogger().info("Permutation:" + permutation);
							// TODO: FIX LOGIC SO WE CAN REMOVE THIS PART OF THE LOGIC
							if (leftEmbedding == null || rightEmbedding == null) {
								continue;
							}
							similaritySum = similaritySum.doubleValue()
									+ EmbeddingsUtils.cosineSimilarity(leftEmbedding, rightEmbedding).doubleValue();
						}
					}
				}
				if (optSimilaritySum == null || optSimilaritySum.doubleValue() < similaritySum.doubleValue()) {
					optSimilaritySum = similaritySum;
					optPermutationIndex = clusterIndex;
				}
			}
			bestCombination.addAll(clusterPermutations.get(optPermutationIndex));
			getLogger().info(c++);
			getLogger().info("Recomputeoptimum combo: ");
			getLogger().info(bestCombination);
		} catch (Exception exc) {
			getLogger().error(exc);
			getLogger().error(exc.getMessage());
			getLogger().error(exc.getCause());
			getLogger().error(exc.getLocalizedMessage());
			getLogger().error(exc.getMessage(), exc);
		}
		hasChanged = false;
	}

	@Override
	public Number getWeight() {
		return 20f;
	}

	@Override
	public void linkContext(Collection<Mention<N>> context) {
		this.context = context;
	}

	@Override
	public void updateContext() {
		synchronized ("hasChanged") {
			hasChanged = true;
		}
	}

}
