package alu.linking.disambiguation.scorers;

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
import alu.linking.utils.EmbeddingsUtils;

public class SSPEmbeddingScorer implements PostScorer<PossibleAssignment, Mention> {
	private final EnumModelType KG;
	private final Map<String, List<Number>> entityEmbeddingsMap;
	private boolean hasChanged = true;
	private Collection<Mention> context;
	private final Set<String> bestCombination = new HashSet<>();

	public SSPEmbeddingScorer(final EnumModelType KG)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		this.KG = KG;
		final ObjectInputStream ois = new ObjectInputStream(
				new FileInputStream(new File(FilePaths.FILE_EMBEDDINGS_SSP_ENTITY_EMBEDDINGS_RAWMAP.getPath(KG))));
		this.entityEmbeddingsMap = (Map<String, List<Number>>) ois.readObject();
	}

	@Override
	public Number computeScore(PossibleAssignment assignment) {
		if (hasChanged) {
			recomputeOptimum();
			hasChanged = false;
		}
		if (bestCombination.contains(assignment.toString())) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Recomputes the best combination of entities to minimize distance
	 */
	private synchronized void recomputeOptimum() {
		bestCombination.clear();
		// Compute clusters based on assignments
		final List<List<String>> clusters = Lists.newArrayList();
		final Map<String, List<String>> clusterMap = new HashMap<>();
		for (Mention m : context) {
			clusterMap.putIfAbsent(m.getMention(), Lists.newArrayList());
			List<String> cluster = clusterMap.get(m.getMention());
			cluster.add(m.getAssignment().getAssignment().toString());
		}

		// Add the detected clusters to an encompassing list
		for (Map.Entry<String, List<String>> e : clusterMap.entrySet()) {
			clusters.add(e.getValue());
		}

		// Compute all possible clusters
		final List<List<String>> clusterPermutations = EmbeddingsUtils.findPermutations(clusters);

		// Compute optimal combination (via similarity/distances)
		final Map<String, Number> optimalSimilarityMap = new HashMap<>();
		Number optSimilaritySum = null;
		int optPermutationIndex = -1;
		for (int clusterIndex = 0; clusterIndex < clusterPermutations.size(); ++clusterIndex) {
			final List<String> permutation = clusterPermutations.get(clusterIndex);
			Number similaritySum = 0;
			// Summing up distances/similarity to determine optimal entity combination (aka.
			// 'cluster')
			for (int i = 0; i < permutation.size(); i++) {
				for (int j = 0; j < permutation.size(); j++) {
					if (i != j) {
						final List<Number> leftEmbedding = this.entityEmbeddingsMap.get(permutation.get(i));
						final List<Number> rightEmbedding = this.entityEmbeddingsMap.get(permutation.get(j));
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
		//hasChanged = false;
	}

	@Override
	public Number getWeight() {
		return 20f;
	}

	@Override
	public void updateContext(Collection<Mention> context) {
		this.context = context;
		hasChanged = true;
	}

}
