package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;

import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.disambiguation.ContextBase;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;

public interface ClusterItemPicker<S> extends ContextBase<Mention<S>>, Loggable {
	public List<S> combine();
	
	default Map<String, List<String>> computeCluster(final Collection<Mention<S>> context)
	{
		final Map<String, List<String>> clusterMap = new HashMap<>();

		for (Mention<S> m : context) {
			final List<String> absent = clusterMap.putIfAbsent(m.getMention(), Lists.newArrayList());
			if (absent == null) {
				// Everything OK
				final List<String> cluster = clusterMap.get(m.getMention());
				for (PossibleAssignment<S> ass : m.getPossibleAssignments()) {
					cluster.add(ass.getAssignment().toString());
				}
			} else {
				getLogger().warn("Cluster already contained wanted mention (doubled word in input?)");
			}
		}
		return clusterMap;

	}
}
