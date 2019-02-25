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
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

public interface ClusterItemPicker extends ContextBase<Mention>, Loggable {
	public List<String> combine();

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
		getLogger().warn("Multiple SF occurrences - Collisions(" + collisionCounter + ") for SF("
				+ multipleOccurrences.size() + "): " + multipleOccurrences);
		return clusterMap;

	}
}
