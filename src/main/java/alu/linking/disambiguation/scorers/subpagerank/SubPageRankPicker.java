package alu.linking.disambiguation.scorers.subpagerank;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.Lists;

import alu.linking.disambiguation.pagerank.AssignmentScore;
import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.mentiondetection.Mention;
import alu.linking.structure.Loggable;
import alu.linking.utils.Stopwatch;

public class SubPageRankPicker implements ClusterItemPicker, Loggable {

	enum PageRankAlgorithm {
		SCORER_GRAPH_MAP, // Probably best
		SCORER_GRAPH_OPTIMIZED, // Making use of problem's properties to do it
		// Warning: DO NOT use the OPTIMIZED one for a general PR problem (it exploits
		// the nxn connection idea)
		SCORER_GRAPH // Naive approach
	}

	private Collection<Mention> context;
	private final EntitySimilarityService similarityService;
	private final double minSimilarityThreshold;
	private final PageRankAlgorithm algorithm;

	public SubPageRankPicker(final Map<String, List<Number>> entityEmbeddings) {
		this(new EntitySimilarityService(entityEmbeddings));
	}

	public SubPageRankPicker(final EntitySimilarityService similarityService) {
		this(similarityService, 0.5d);
	}

	public SubPageRankPicker(final EntitySimilarityService similarityService, final double minEdgeThreshold) {
		this(similarityService, minEdgeThreshold, PageRankAlgorithm.SCORER_GRAPH_MAP);
	}

	public SubPageRankPicker(final EntitySimilarityService similarityService, final double minEdgeThreshold,
			final PageRankAlgorithm algorithm) {
		this.similarityService = similarityService;
		this.algorithm = algorithm;
		this.minSimilarityThreshold = minEdgeThreshold;
	}

	@Override
	public void linkContext(Collection<Mention> context) {
		this.context = context;
	}

	@Override
	public void updateContext() {
		// Nothing?
	}

	@Override
	public List<String> combine() {
		final boolean OLD = true;
		final List<String> retList = Lists.newArrayList();
		Stopwatch.start(getClass().getName());
		getLogger().info("Computing cluster...");
		final Map<String, List<String>> clusters = computeClusters(context);
		getLogger().info("Finished Computing cluster in " + Stopwatch.endDiffStart(getClass().getName()) + " ms");
		final Set<String> notFoundIRIs;
		final int iterations = 5;
		final double startVal = 0.1d, dampingFactor = 0.85;
		switch (this.algorithm) {
		case SCORER_GRAPH:
			if (true) {
				final ScorerGraphNaive scorerGraph = new ScorerGraphNaive(this.similarityService,
						minSimilarityThreshold).uniqueNeighbours(true);
				scorerGraph.dampingFactor(dampingFactor).iterations(iterations).startValue(startVal);
				scorerGraph.populate(clusters);
				getLogger().info("Finished instantiating scorer graph in "
						+ Stopwatch.endDiffStart(getClass().getName()) + " ms");
				scorerGraph.pagerank();
				getLogger().info("Finished pagerank in " + Stopwatch.endDiffStart(getClass().getName()) + " ms");
				Map<Integer, ImmutablePair<String, Double>> groupedMap = scorerGraph.topByGroup();
				for (Map.Entry<Integer, ImmutablePair<String, Double>> e : groupedMap.entrySet()) {
					System.out.println("Value / Score: " + e.getValue().left + " - " + e.getValue().right);
					retList.add(e.getValue().left);
				}
				notFoundIRIs = this.similarityService.notFoundIRIs;
			}
			break;
		case SCORER_GRAPH_OPTIMIZED:
			if (true) {
				final ScorerGraphOptimized sgo = new ScorerGraphOptimized(this.similarityService);
				sgo.dampingFactor(dampingFactor).iterations(iterations).startValue(startVal);
				sgo.clusters(clusters).pagerank();
				retList.addAll(sgo.getTop());
				System.out.println("Top values found: ");
				System.out.println(retList);
				notFoundIRIs = sgo.getNotFoundIRIs();
			}
			break;
		case SCORER_GRAPH_MAP:
			if (true) {
				final ScorerGraphMap scorerGraph = new ScorerGraphMap(this.similarityService, minSimilarityThreshold);
				scorerGraph.dampingFactor(dampingFactor).iterations(iterations).startValue(startVal).pagerank(clusters);
				for (Map.Entry<String, List<String>> eClusters : clusters.entrySet()) {
					final AssignmentScore bestScore = scorerGraph.grabBest(eClusters.getValue());
					getLogger().info("Cluster[" + eClusters.getKey() + "]: " + bestScore);
					retList.add(bestScore.assignment);
				}
				notFoundIRIs = this.similarityService.notFoundIRIs;
			}
			break;
		default:
			notFoundIRIs = null;
			break;
		}

		// Display problems we had during computations
		getLogger().info("Could not find (" + notFoundIRIs.size() + "): ");
		int notFoundCounter = 0;
		final int MAX_DISPLAY = 50;
		for (String s : notFoundIRIs) {
			if (notFoundCounter > MAX_DISPLAY) {
				break;
			}
			getLogger().info(notFoundCounter++ + " - " + s);
		}
		return retList;
	}

	@Override
	public double getPickerWeight() {
		return 20;
	}

}
