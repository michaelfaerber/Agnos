package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.beust.jcommander.internal.Lists;

import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.EmbeddingsUtils;

public class GreedyOptimalPicker extends AbstractClusterItemPicker implements Loggable {

	private Map<String, List<Number>> entityEmbeddingsMap;
	private Collection<Mention> context;
	private static final BiFunction<Double, Double, Double> DEFAULT_OPERATION = ClusterItemPicker::occurrenceOperation;

	public GreedyOptimalPicker(final BiFunction<Double, Double, Double> operation,
			Map<String, List<Number>> entityEmbeddingMap) {
		super(operation);
		this.entityEmbeddingsMap = entityEmbeddingMap;
	}

	@Override
	public List<String> combine() {
		// Compute clusters based on assignments
		List<List<String>> clusters = Lists.newArrayList();
		final Map<String, List<String>> clusterMap = computeClusters(context);

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
			Number similaritySum = 0d;
			// Summing up distances/similarity to determine optimal entity combination (aka.
			// 'cluster')
			for (int i = 0; i < permutation.size(); i++) {
				for (int j = i + 1; j < permutation.size(); j++) {
					if (i != j) {
						final List<Number> leftEmbedding = this.entityEmbeddingsMap.get(permutation.get(i));
						final List<Number> rightEmbedding = this.entityEmbeddingsMap.get(permutation.get(j));
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
		return clusterPermutations.get(optPermutationIndex);
	}

	@Override
	public void updateContext(Collection<Mention> context) {
		// In case something else has to be updated
		this.context = context;
	}

	@Override
	public double getPickerWeight() {
		return 5;
	}

	@Override
	public void printExperimentSetup() {
		getLogger().info("GreedyOptimal");
	}
}
