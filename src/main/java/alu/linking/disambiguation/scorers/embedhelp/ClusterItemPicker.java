package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public static final int DEFAULT_PR_TOP_K = 20;// 50;// 30;// 0;// 100;
	public static final double DEFAULT_PR_MIN_THRESHOLD = 1d;// 0.16d;// 0.16d;// 1d;// 0.1d;
	public static final int DEFAULT_REPEAT = 2000;// was 200 before, but due to long texts...
	public static final double DEFAULT_PRUNE_MIN_SCORE_RATIO = 0.1;
	public static final boolean allowSelfConnection = false;
	// Whether to remove assignments when there is only one possibility (due to high
	// likelihood of distortion)
	public static final boolean REMOVE_SINGLE_ASSIGNMENTS = false;
	// 0.16d due to MANY rarely-referenced 0.15d endpoints existing
	public static final int MIN_REPEAT = 1;

	public List<String> combine();

	public double getPickerWeight();

	public void printExperimentSetup();

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

	default Map<String, List<String>> limitTopPRClusters(final PageRankLoader prLoader,
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

	/**
	 * Method executing the wanted operation for grouping of entities for the
	 * specified surface forms<br>
	 * Note: Pretty much fulfills the role of a reward function which in the end
	 * determines which entity is disambiguated to
	 * 
	 * @param previousValue     previous value within map
	 * @param pairSimilaritySum the cosine similarity that might want to be summed
	 * @return value resulting of the operation
	 */
	default Double applyOperation(Double previousValue, Double pairSimilaritySum) {
		// Either sum them or just add +1
		// occurrence
		// return previousValue + 1;
		// summed similarity
		// return previousValue + pairSimilaritySum;
		// square it to make a bigger impact, the better it is
		return previousValue + Math.pow(pairSimilaritySum, 2f);
		// Highest of both - Result: terrible
		// return Math.max(previousValue, pairSimilaritySum);
	}

}
