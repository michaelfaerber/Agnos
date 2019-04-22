package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.beust.jcommander.internal.Lists;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.disambiguation.ContextBase;
import alu.linking.disambiguation.pagerank.AssignmentScore;
import alu.linking.disambiguation.pagerank.PageRankLoader;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

public interface ClusterItemPicker extends ContextBase<Mention>, Loggable {
	public enum PICK_SELECTION {
		TOP_PAGERANK, RANDOM, OPTIMAL_CALC
	}

	public static final int DEFAULT_PR_TOP_K = 30;// 50;// 30;// 0;// 100;
	public static final double DEFAULT_PR_MIN_THRESHOLD = 1d;// 0.16d;// 0.16d;// 1d;// 0.1d;
	public static final int DEFAULT_REPEAT = 2_000;// was 200 before, but due to long texts...
	public static final double DEFAULT_PRUNE_MIN_SCORE_RATIO = 0.40;
	public static final boolean allowSelfConnection = false;
	// Whether to remove assignments when there is only one possibility (due to high
	// likelihood of distortion)
	public static final boolean REMOVE_SINGLE_ASSIGNMENTS = false;
	// 0.16d due to MANY rarely-referenced 0.15d endpoints existing
	public static final int MIN_REPEAT = 1;

	public List<String> combine();

	public double getPickerWeight();

	public void printExperimentSetup();

	public BiFunction<Double, Double, Double> getCombinationOperation();

	default Map<String, Map<String, Number>> computeInitScoreMap(final Collection<Mention> context,
			final Number initValue) {
		final Map<String, Map<String, Number>> retMap = new HashMap<>();
		for (Mention mention : context) {
			if (mention == null || mention.getMention() == null) {
				continue;
			}

			final String surfaceForm = mention.getMention();
			Map<String, Number> sfMap = null;
			if ((sfMap = retMap.get(surfaceForm)) == null) {
				retMap.put(surfaceForm, sfMap);
				sfMap = new HashMap<>();
			}
			for (PossibleAssignment assignment : mention.getPossibleAssignments()) {
				sfMap.put(assignment.getAssignment(), initValue);
			}
		}
		return retMap;
	}

	default Map<String, List<String>> computeClusters(final Collection<Mention> context) {
		final Map<String, List<String>> clusterMap = new HashMap<>();

		List<String> putList = Lists.newArrayList();
		final Set<String> multipleOccurrences = new HashSet<>();
		int collisionCounter = 0;
		for (Mention m : context) {
			if (m == null || m.getMention() == null) {
				continue;
			}
			final List<String> absent = clusterMap.putIfAbsent(m.getMention(), putList);
			if (absent == null) {
				// Everything OK
				final List<String> cluster = clusterMap.get(m.getMention());
				for (PossibleAssignment ass : m.getPossibleAssignments()) {
					cluster.add(ass.getAssignment().toString());
				}
				// Prepare the putList for the next one
				putList = Lists.newArrayList();
			} else {
				multipleOccurrences.add(m.getMention());
				collisionCounter++;
				// getLogger().warn("Cluster already contained wanted mention (doubled word in
				// input?)");
			}
		}
		// getLogger().warn("Multiple SF occurrences - Collisions(" + collisionCounter +
		// ") for SF("+ multipleOccurrences.size() + "): " + multipleOccurrences);
		return clusterMap;
	}

	default Map<String, List<String>> computePRLimitedClusters(final PageRankLoader prLoader,
			final Map<String, List<String>> clusters, final int PR_TOP_K, final double PR_MIN_THRESHOLD) {
		final Map<String, List<String>> copyClusters = new HashMap<>();
		for (final String clusterName : clusters.keySet()) {
			final List<String> entities = clusters.get(clusterName);
			// log.info("SF[" + clusterName + "] - Entities[" + entities + "]");

			List<AssignmentScore> rankedScores = prLoader.makeOrPopulateList(entities);
			if (PR_TOP_K > 0) {
				rankedScores = prLoader.getTopK(entities, PR_TOP_K);
			}

			if (PR_MIN_THRESHOLD > 0) {
				prLoader.cutOff(entities, PR_MIN_THRESHOLD);
			}

			// final List<AssignmentScore> rankedScores = prLoader.cutOff(entities,
			// PR_MIN_THRESHOLD);

			final List<String> limitedEntities = Lists.newArrayList();
			// Compute the list to disambiguate from
			// rankedScores.stream().forEach(item -> limitedEntities.add(item.assignment));
			for (AssignmentScore item : rankedScores) {
				limitedEntities.add(item.assignment);
			}

			// Overwrite clusters, so disambiguation is only done on top PR scores
			copyClusters.put(clusterName, limitedEntities);
		}
		return copyClusters;
	}

	default Map<String, Map<String, Number>> computePRLimitedScoreClusters(final PageRankLoader prLoader,
			final Map<String, List<String>> clusters, final int PR_TOP_K, final double PR_MIN_THRESHOLD,
			final Number initVal) {
		final Map<String, Map<String, Number>> copyClusters = new HashMap<>();
		for (final String clusterName : clusters.keySet()) {
			final List<String> entities = clusters.get(clusterName);
			// log.info("SF[" + clusterName + "] - Entities[" + entities + "]");

			List<AssignmentScore> rankedScores = prLoader.makeOrPopulateList(entities);
			if (PR_TOP_K > 0) {
				rankedScores = prLoader.getTopK(entities, PR_TOP_K);
			}

			if (PR_MIN_THRESHOLD > 0) {
				prLoader.cutOff(entities, PR_MIN_THRESHOLD);
			}

			// final List<AssignmentScore> rankedScores = prLoader.cutOff(entities,
			// PR_MIN_THRESHOLD);

			final Map<String, Number> limitedEntities = new HashMap<>();
			// Compute the list to disambiguate from
			// rankedScores.stream().forEach(item -> limitedEntities.add(item.assignment));
			for (AssignmentScore item : rankedScores) {
				limitedEntities.put(item.assignment, initVal);
			}

			// Overwrite clusters, so disambiguation is only done on top PR scores
			copyClusters.put(clusterName, limitedEntities);
		}
		return copyClusters;
	}


	public static Double occurrenceOperation(Double previousValue, Double pairSimilaritySum) {
		return previousValue + 1;
	}

	public static Double similarityOperation(Double previousValue, Double pairSimilaritySum) {
		return previousValue + pairSimilaritySum;
	}

	public static Double similaritySquaredOperation(Double previousValue, Double pairSimilaritySum) {
		return previousValue + Math.pow(pairSimilaritySum, 2f);
	}

	public static Double maxedOperation(Double previousValue, Double pairSimilaritySum) {
		return Math.max(previousValue, pairSimilaritySum);
	}

}
