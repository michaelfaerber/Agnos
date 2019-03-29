package alu.linking.disambiguation.scorers.pairwise;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import alu.linking.disambiguation.pagerank.PageRankLoader;
import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.mentiondetection.Mention;

public class PairwisePicker implements ClusterItemPicker {

	private Collection<Mention> context = null;
	private final EntitySimilarityService similarityService;
	private final PageRankLoader pagerankLoader;
	public final int pagerankTopK;
	public final double pagerankMinThreshold;

	public PairwisePicker(final EntitySimilarityService similarityService, final PageRankLoader pagerankLoader) {
		this(similarityService, pagerankLoader, DEFAULT_PR_TOP_K, DEFAULT_PR_MIN_THRESHOLD);
	}

	public PairwisePicker(final EntitySimilarityService similarityService, final PageRankLoader pagerankLoader,
			final int pagerankTopK, final double pagerankMinThreshold) {
		this.similarityService = similarityService;
		this.pagerankLoader = pagerankLoader;
		this.pagerankTopK = pagerankTopK;
		this.pagerankMinThreshold = pagerankMinThreshold;
	}

	@Override
	public void linkContext(Collection<Mention> context) {
		this.context = context;
	}

	@Override
	public void updateContext() {
		// None?
	}

	@Override
	public List<String> combine() {
		// All the logic!
		// This way we can keep track of surface forms, their respective entities and
		// the entities' scores
		final Map<String, Map<String, Number>> mapScore = computeInitScoreMap(this.context, 0f);
		final Map<String, List<String>> clusters = computeClusters(this.context);

		// Limit clusters to defined top K by PR score (reduces complexity)
		final Map<String, List<String>> mapLimitedClusters = limitTopPRClusters(this.pagerankLoader, clusters,
				this.pagerankTopK, this.pagerankMinThreshold);

		final boolean allowSelfConnection = true;
		for (Map.Entry<String, List<String>> eOuter : mapLimitedClusters.entrySet()) {
			final String toSF = eOuter.getKey();
			final Map<String, Number> toSFEntityScoreMap = mapScore.get(toSF);
			for (Map.Entry<String, List<String>> eInner : mapLimitedClusters.entrySet()) {
				try {
					if (eOuter.getKey().equals(eInner.getKey())) {
						// Skip so we don't do similarities with ourselves...
						continue;
					}
					final Collection<String> targets = eOuter.getValue();
					final String fromSF = eInner.getKey();
					final Map<String, Number> fromSFEntityScoreMap = mapScore.get(fromSF);
					for (String fromEntity : eInner.getValue()) {
						// Get the best similarity from this entity for the other entities
						final Pair<String, Double> bestTarget = this.similarityService.topSimilarity(fromEntity,
								targets, allowSelfConnection);
						if (bestTarget == null) {
							continue;
						}
						// combine this value somehow with the pre-existing value within the map
						// Similar ideology as the initial in-memory graph idea with the paths
						// Note the assumption that: sim(A,B) = sim(B,A)
						final Number similarityScore = bestTarget.getRight();

						// Update target value
						final String targetEntity = bestTarget.getLeft();
						final Number targetCurrentScore = toSFEntityScoreMap.get(targetEntity);
						final Double newTargetScore = applyOperation(targetCurrentScore.doubleValue(),
								similarityScore.doubleValue());
						toSFEntityScoreMap.put(targetEntity, newTargetScore);

						// Update source value (this is doing it in both directions, so if we want to
						// add a kind of "anonymous admirer" type of logic, that would mean ignoring
						// updating the source)
						final Number fromCurrentScore = fromSFEntityScoreMap.get(fromEntity);
						final Double newFromScore = applyOperation(fromCurrentScore.doubleValue(),
								similarityScore.doubleValue());
						fromSFEntityScoreMap.put(fromEntity, newFromScore);
					}
				} catch (NullPointerException npe) {
					System.out.println("MapScore Keys: " + mapScore.keySet());
					System.out.println("toSF: " + toSF);
					throw npe;
				}
			}

		}

		// Now that things have been chosen, let's just get the best ones!
		final List<Pair<String, Number>> bestScores = Lists.newArrayList();
		for (Map.Entry<String, Map<String, Number>> eSF : mapScore.entrySet()) {
			// Reuse the same object rather than re-instantiating for performance
			// Initialising w/ the assumption that the minimal score is 0
			final MutablePair<String, Number> bestEntity = new MutablePair<String, Number>(null, 0d);
			for (Map.Entry<String, Number> eEntity : eSF.getValue().entrySet()) {
				if (bestEntity.getRight().doubleValue() < eEntity.getValue().doubleValue()) {
					bestEntity.setLeft(eEntity.getKey());
					bestEntity.setValue(eEntity.getValue());
				}
			}
			bestScores.add(bestEntity);
		}

		final List<String> retList = Lists.newArrayList();
		for (Pair<String, Number> bestScore : bestScores) {
			retList.add(bestScore.getLeft());
		}
		return retList;
	}

	@Override
	public double getPickerWeight() {
		// TODO Auto-generated method stub
		return 20;
	}

	@Override
	public void printExperimentSetup() {
		// TODO Auto-generated method stub

	}

}
