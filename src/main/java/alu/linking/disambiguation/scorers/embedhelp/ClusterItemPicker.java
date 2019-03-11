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
	public List<String> combine();

	public double getPickerWeight();
	
	public void printExperimentSetup();

	default Map<String, List<String>> computeClusters(final Collection<Mention> context) {
		final Map<String, List<String>> clusterMap = new HashMap<>();

		List<String> putList = Lists.newArrayList();
		final Set<String> multipleOccurrences = new HashSet<>();
		int collisionCounter = 0;
		for (Mention m : context) {
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
}
