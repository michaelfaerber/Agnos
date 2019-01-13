package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.Lists;

import alu.linking.mentiondetection.Mention;

public class SubPageRankPicker<S> implements ClusterItemPicker<S> {

	private Collection<Mention<S>> context;
	private Map<String, List<Number>> entityEmbeddingsMap;

	public SubPageRankPicker(Map<String, List<Number>> entityEmbeddingMap) {
		this.entityEmbeddingsMap = entityEmbeddingMap;
	}

	@Override
	public void linkContext(Collection<Mention<S>> context) {
		this.context = context;
	}

	@Override
	public void updateContext() {
		// Nothing?
	}

	@Override
	public List<S> combine() {
		final List<S> retList = Lists.newArrayList();
		final Map<String, List<String>> clusters = computeCluster(context);
		final ScorerGraph scorerGraph = new ScorerGraph(this.entityEmbeddingsMap).dampingFactor(0.85).iterations(5)
				.startValue(1d).uniqueNeighbours(true).populate(clusters);
		scorerGraph.pagerank();
		Map<Integer, ImmutablePair<String, Double>> groupedMap = scorerGraph.topByGroup();
		for (Map.Entry<Integer, ImmutablePair<String, Double>> e : groupedMap.entrySet()) {
			System.out.println("Value / Score: " + e.getValue().left + " - " + e.getValue().right);
			retList.add((S) e.getValue().left);
		}
		for (String s : scorerGraph.notFoundIRIs) {
			System.out.println("Could not find (" + scorerGraph.notFoundIRIs.size() + "): " + s);
		}
		return retList;
	}

}
