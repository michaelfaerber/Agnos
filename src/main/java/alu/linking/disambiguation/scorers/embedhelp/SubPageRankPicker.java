package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.Lists;

import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.Stopwatch;

public class SubPageRankPicker<S> implements ClusterItemPicker<S>, Loggable {

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
		final boolean OLD = false;
		final List<S> retList = Lists.newArrayList();
		Stopwatch.start(getClass().getName());
		getLogger().info("Computing cluster...");
		final Map<String, List<String>> clusters = computeClusters(context);
		getLogger().info("Finished Computing cluster in " + Stopwatch.endDiffStart(getClass().getName()) + " ms");
		final Set<String> notFoundIRIs;
		if (OLD) {
			final ScorerGraph scorerGraph = new ScorerGraph(this.entityEmbeddingsMap).dampingFactor(0.85).iterations(5)
					.startValue(1d).uniqueNeighbours(true).populate(clusters);
			getLogger().info(
					"Finished instantiating scorer graph in " + Stopwatch.endDiffStart(getClass().getName()) + " ms");
			scorerGraph.pagerank();
			getLogger().info("Finished pagerank in " + Stopwatch.endDiffStart(getClass().getName()) + " ms");
			Map<Integer, ImmutablePair<String, Double>> groupedMap = scorerGraph.topByGroup();
			for (Map.Entry<Integer, ImmutablePair<String, Double>> e : groupedMap.entrySet()) {
				System.out.println("Value / Score: " + e.getValue().left + " - " + e.getValue().right);
				retList.add((S) e.getValue().left);
			}
			notFoundIRIs = scorerGraph.notFoundIRIs;
		} else {
			final ScorerGraphOptimized sgo = new ScorerGraphOptimized(this.entityEmbeddingsMap).dampingFactor(0.85)
					.iterations(5).startValue(1d).clusters(clusters);
			sgo.pagerank();
			retList.addAll((Collection<S>) sgo.getTop());
			System.out.println("Top values found: ");
			System.out.println(retList);
			notFoundIRIs = sgo.getNotFoundIRIs();

		}
		// Display problems
		for (String s : notFoundIRIs) {
			getLogger().info("Could not find (" + notFoundIRIs.size() + "): " + s);
		}
		return retList;
	}

}
