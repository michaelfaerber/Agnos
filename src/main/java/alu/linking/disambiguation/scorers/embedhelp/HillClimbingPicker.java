package alu.linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.mentiondetection.Mention;
import alu.linking.utils.Stopwatch;

public class HillClimbingPicker implements ClusterItemPicker {
	private final Random r = new Random(System.currentTimeMillis());
	final Comparator<Mention> offsetComparator = new Comparator<Mention>() {
		@Override
		public int compare(Mention o1, Mention o2) {
			return o1.getOffset() - o2.getOffset();
		}
	};
	private Collection<Mention> context;
	private final EntitySimilarityService similarityService;
	private final boolean RANDOM_FIRST_CHOICE = true;
	private final int REPEAT;
	private static final int DEFAULT_REPEAT = 20;// was 200 before, but due to long texts...
	private static final int MIN_REPEAT = 1;
	private static final double MIN_SCORE_RATIO = 0.5;

	public HillClimbingPicker(final Map<String, List<Number>> entityEmbeddingsMap) {
		this(new EntitySimilarityService(entityEmbeddingsMap), DEFAULT_REPEAT);
	}

	public HillClimbingPicker(final EntitySimilarityService similarityService) {
		this(similarityService, DEFAULT_REPEAT);
	}

	public HillClimbingPicker(final EntitySimilarityService similarityService, final int repeat) {
		this.similarityService = similarityService;
		// How many times to repeat hillclimbing
		this.REPEAT = Math.max(MIN_REPEAT, repeat);
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
		// Choose an initial combination and improve on it at every step until it can no
		// longer be improved
		// Try making it with a chain-like minimal distance logic
		// "Get smallest for one of current chains"

		// Map to keep the final choices in our eye
		// Map<SurfaceForm, List<Pair<Entity, SimilaritySum>>>
		final Map<String, List<Pair<String, Double>>> disambiguationResultsMap = new HashMap<>();

		// Order list by natural occurring order of words (enforces intuition of words
		// close to each other being about the same stuff)
		final List<Mention> contextList = Lists.newArrayList(this.context);
		Collections.sort(contextList, offsetComparator);

		// Compute clusters with the strings for simplicity of calls
		final Map<String, List<String>> clusters = computeClusters(contextList);

		// Execute hillclimbing multiple times
		for (int hillClimbExec = 0; hillClimbExec < REPEAT; ++hillClimbExec) {
			hillClimb(disambiguationResultsMap, contextList, clusters);
		}

		// -------------------------------------------
		// HillClimbing Disambigation - END
		// -------------------------------------------

		// ---------------------
		// Grouping - Start
		// ---------------------

		// Now that it has been executed as many times as we want it to, we should group
		// the similar entities and possibly do something with the similaritySum values
		// (e.g. disambiguate over them?), OTHERWISE: just take the number of times they
		// each appeared and take the most-appearing one

		// <Key,Value>: <SurfaceForm, ChosenEntity>
		final Map<String, Pair<String, Double>> finalChoiceMap = new HashMap<>();
		// <Key,Value>: <EntityIRI, CosineSimSum>
		final Map<String, Double> groupMap = new HashMap<>();
		for (Map.Entry<String, List<Pair<String, Double>>> e : disambiguationResultsMap.entrySet()) {
			groupMap.clear();
			final String surfaceForm = e.getKey();
			// Group all pairs for this specific surface form
			for (Pair<String, Double> pair : e.getValue()) {
				Double existingPairValue = groupMap.get(pair.getLeft());
				if (existingPairValue == null) {
					existingPairValue = 0D;
				}
				// Sums up over the stuffz
				// key = entity; value = summed score
				groupMap.put(pair.getLeft(), applyOperation(existingPairValue, pair.getRight()));
			}
			// displayAllResultsMap(allResultsMap);
			// displayScoredChoices(surfaceForm, groupMap);

			// Pairs have been summed up together, so let's rank them
			Double prevSim = Double.MIN_VALUE;
			for (Entry<String, Double> pairEntry : groupMap.entrySet()) {
				// For this surface form, choose the best candidate
				if (pairEntry.getValue() > prevSim) {
					finalChoiceMap.put(surfaceForm, new ImmutablePair<>(pairEntry.getKey(), pairEntry.getValue()));
					prevSim = pairEntry.getValue();
				}
			}
		}

		// ---------------
		// Pruning
		// ---------------
		prune(finalChoiceMap);

		final List<String> retList = Lists.newArrayList();
		for (Pair<String, Double> pair : finalChoiceMap.values()) {
			retList.add(pair.getKey());
		}
		getLogger().info("FINAL CHOICES: " + retList.size());
		return retList;
	}

	/**
	 * Prunes entries from given map for which the Pair's Double (right) element is
	 * less than the minimum score threshold
	 * 
	 * @param choiceMap
	 */
	private void prune(final Map<String, Pair<String, Double>> choiceMap) {
		final Iterator<Entry<String, Pair<String, Double>>> finalChoicesIterator = choiceMap.entrySet().iterator();
		final Double MIN_SCORE = computeMinScore();
		getLogger().info("Min score THRESHOLD:" + MIN_SCORE);
		getLogger().info("PRUNING/ABSTAINING PROCEDURE - START");
		while (finalChoicesIterator.hasNext()) {
			final Entry<String, Pair<String, Double>> entry = finalChoicesIterator.next();
			// getLogger().info("Entry:" + entry.getKey() + " - " + entry.getValue());
			if (entry.getValue().getRight() < MIN_SCORE) {
				getLogger().info("->Pruning:" + entry.getKey() + " - " + entry.getValue());
				finalChoicesIterator.remove();
			}
		}
		getLogger().info("PRUNING/ABSTAINING PROCEDURE - END");

	}

	private Map<String, List<Pair<String, Double>>> hillClimb(
			final Map<String, List<Pair<String, Double>>> disambiguationResultsMap, final List<Mention> contextList,
			final Map<String, List<String>> clusters) {
		// Use clusterNames as the shuffling mechanism
		final List<String> clusterNames = Lists.newArrayList(clusters.keySet());
		// Adds to clusterNames with the initial order
		// contextList.stream().forEach(mention -> clusterNames.add(mention.getMention()));

		// Start with the written ordering (for initial bias), then shuffle

		// ##############################
		// Initial path choice - START
		// ##############################
		Stopwatch.start(getClass().getName());
		// First initialise by getting the optimal similarity
		final Map<String, Pair<String, Double>> chosenClusterEntityMap = new HashMap<>(clusters.size());
		Double similaritySum = 0D;
		if (clusterNames.size() > 1) {
			final String finalTarget;

			if (RANDOM_FIRST_CHOICE) {
				Collections.shuffle(clusterNames);
				final String fromClusterName = clusterNames.get(0);
				final String toClusterName = clusterNames.get(1);
				final List<String> fromEntities = clusters.get(fromClusterName);
				final List<String> toEntities = clusters.get(toClusterName);
				final int firstItemIndex = r.nextInt(fromEntities.size());
				final int secondItemIndex = r.nextInt(toEntities.size());
				final String sourceEntity = fromEntities.get(firstItemIndex);
				final String targetEntity = toEntities.get(secondItemIndex);
				chosenClusterEntityMap.put(fromClusterName,
						new ImmutablePair<String, Double>(sourceEntity, Double.MIN_VALUE));
				chosenClusterEntityMap.put(toClusterName, new ImmutablePair<String, Double>(targetEntity,
						this.similarityService.similarity(sourceEntity, targetEntity).doubleValue()));

			} else {
				// First CHECK ALL THE BEST connections between the first two, then just check
				// best
				// target
				chooseOptimalStart(clusterNames.get(0), clusterNames.get(1), chosenClusterEntityMap, clusters);
			}

			similaritySum += chooseClosestToFirst(clusters, clusterNames, chosenClusterEntityMap);

		}
		// Logger.getLogger(getClass().getName()).info("Initial path choice done in " +
		// Stopwatch.endDiffStart(getClass().getName()) + " ms!");
		// ##############################
		// Initial path choice - END
		// ##############################

		// ##############################
		// Randomized best-choice - START
		// ##############################
		// final int iterations = 20;
		final int iterations = Math.min(20, (int) (Math.sqrt(clusterNames.size())));
		Set<String> previousChoices = null;
		Set<String> currentChoices = null;
		final int maxIterations = 200;
		int iterCounter = 0;
		do {
			previousChoices = currentChoices;
			for (int i = 0; i < iterations; ++i) {
				// Shuffle the cluster names randomly
				Collections.shuffle(clusterNames);
				// This is the hillclimbing part of it which takes the best local choice in
				// order to maximize the reward of the overall similarity sum
				iterativelyPickLocalBest(clusters, chosenClusterEntityMap, clusterNames);
			}
			currentChoices = mapToValueSet(chosenClusterEntityMap);
			iterCounter += iterations;
			if (iterCounter > maxIterations) {
				getLogger().warn("Been iterating(" + iterCounter + ") for a while now...");
				getLogger().warn("Previous choices:" + previousChoices);
				getLogger().warn("Current choices:" + currentChoices);
				break;
			}
		} while (!equals(previousChoices, currentChoices));
		// ##############################
		// Randomized best-choice - END
		// ##############################

		// Add this execution's results to the all-encompassing results map for further
		// analysis
		for (Map.Entry<String, Pair<String, Double>> e : chosenClusterEntityMap.entrySet()) {
			// Entities aggregated over executions for this particular SF
			List<Pair<String, Double>> sfEntities;
			if ((sfEntities = disambiguationResultsMap.get(e.getKey())) == null) {
				sfEntities = Lists.newArrayList();
				disambiguationResultsMap.put(e.getKey(), sfEntities);
			}
			sfEntities.add(e.getValue());
		}

		return disambiguationResultsMap;
	}

	/**
	 * Made a method out of it since the minimal score should be dependent on the
	 * operation applied (on whether it is based on summed SIMILARITIES or on
	 * OCCURRENCE)
	 * 
	 * @return minimum score threshold
	 */
	private Double computeMinScore() {
		return ((double) REPEAT) * MIN_SCORE_RATIO;
	}

	/**
	 * Method executing the wanted operation for grouping of entities for the
	 * specified surface forms
	 * 
	 * @param previousValue     previous value within map
	 * @param pairSimilaritySum the cosine similarity that might want to be summed
	 * @return value resulting of the operation
	 */
	private Double applyOperation(Double previousValue, Double pairSimilaritySum) {
		// Either sum them or just add +1
		// occurrence
		return previousValue + 1;
		// summed similarity
		// return previousValue + pairSimilaritySum;
	}

	private void displayAllResultsMap(Map<String, List<Pair<String, Double>>> allResultsMap) {
		getLogger().info("ALL RESULTS MAP - START");
		for (Entry<String, List<Pair<String, Double>>> e : allResultsMap.entrySet()) {
			getLogger().info("[" + e.getKey() + "] " + e.getValue());
		}
		getLogger().info("ALL RESULTS MAP - END");
	}

	private void displayScoredChoices(final String sf, final Map groupMap) {
		final Set<Entry> entrySet = groupMap.entrySet();
		getLogger().info("displayScoredChoices - START");
		for (Map.Entry e : entrySet) {
			getLogger().info("GROUP[" + sf + "] " + e.getKey() + " -> " + e.getValue());
		}
		getLogger().info("displayScoredChoices - END");
	}

	/**
	 * Takes the left part of the value's pair and adds it to a "value set" which it
	 * then returns
	 * 
	 * @param chosenClusterEntityMap map containing the pairs to process
	 * @return values (aka. left part of pair) combined as described
	 */
	private Set<String> mapToValueSet(Map<String, Pair<String, Double>> chosenClusterEntityMap) {
		final Set<String> valueSet = new HashSet<String>();
		// for (Map.Entry<String, Pair<String, Double>> e :
		// chosenClusterEntityMap.entrySet()) {
		// valueSet.add(e.getValue().getLeft());
		// }

		chosenClusterEntityMap.values().stream().forEach(pair -> valueSet.add(pair.getLeft()));
		return valueSet;
	}

	/**
	 * Whether since the previous result
	 * 
	 * @param previousChoices
	 * @param chosenClusterEntityMap
	 * @return
	 */
	private boolean equals(Set<String> previousChoices, Set<String> currentChoices) {
		if (previousChoices == null || currentChoices == null) {
			return (previousChoices == null) && (currentChoices == null);
		}
		if (previousChoices.size() != currentChoices.size()) {
			return false;
		}
		return previousChoices.containsAll(currentChoices);
	}

	/**
	 * Hill-Climbing part. Maximizes over summed similarity towards a specified item
	 * (and whether it increases or decreases thanks to it)
	 * 
	 * @param clusters
	 * @param chosenClusterEntityMap
	 * @param clusterNames
	 */
	private void iterativelyPickLocalBest(Map<String, List<String>> clusters,
			Map<String, Pair<String, Double>> chosenClusterEntityMap, List<String> clusterNames) {
		// Now pick the best one between current and next
		for (int clusterNameIndex = 1; clusterNameIndex < clusterNames.size(); ++clusterNameIndex) {
			final String currClusterName = clusterNames.get(clusterNameIndex - 1);
			final String nextClusterName = clusterNames.get(clusterNameIndex);

			final Pair<String, Double> bestNext = this.similarityService.topSimilarity(
					chosenClusterEntityMap.get(currClusterName).getLeft(), clusters.get(nextClusterName));
			// If this increases the similarity sum, take it
			// Otherwise just go to the next one
			final Pair<String, Double> previousChoice = chosenClusterEntityMap.remove(nextClusterName);
			final Double newSimilaritySum = getSimilaritySumToEntity(chosenClusterEntityMap, bestNext.getLeft());
			final Double oldSimilaritySum = getSimilaritySumToEntity(chosenClusterEntityMap, previousChoice.getLeft());

			// Intuition: If I like my choice more than anyone dislikes it, it is worth it!
			// So if the new similarity sum is higher, we'll take it
			if (newSimilaritySum > oldSimilaritySum) {
				chosenClusterEntityMap.put(nextClusterName, bestNext);
			} else {
				chosenClusterEntityMap.put(nextClusterName, previousChoice);
			}
		}

	}

	/**
	 * Chooses optimal starting position AND optimal 2nd item (updating the second's
	 * similarity) to maximize similarity between first and second item
	 * 
	 * @param fromCluster
	 * @param toCluster
	 * @param chosenClusterEntityMap
	 * @param clusters
	 * @return
	 */
	private String chooseOptimalStart(final String fromCluster, final String toCluster,
			final Map<String, Pair<String, Double>> chosenClusterEntityMap, final Map<String, List<String>> clusters) {
		Double similaritySum = 0D;
		String fromClusterName = fromCluster;
		String toClusterName = toCluster;
		final List<String> fromEntities = clusters.get(fromClusterName);
		final List<String> toEntities = clusters.get(toClusterName);
		Pair<String, Double> bestTarget = null;
		String bestFromEntity = null;
		double currBestSim = Double.MIN_VALUE;

		Stopwatch.start("topSimilarity");
		for (String fromEntity : fromEntities) {
			final Pair<String, Double> entitySimPair = this.similarityService.topSimilarity(fromEntity, toEntities);
			final Double entitySim = entitySimPair.getRight();
			if (entitySim > currBestSim) {
				currBestSim = entitySim;
				bestFromEntity = fromEntity;
				bestTarget = entitySimPair;
//				Logger.getLogger(getClass().getName()).info("[" + Stopwatch.endDiffStart("topSimilarity") + " ms] "
//						+ "FROM(" + bestFromEntity + ") - BEST TARGET(" + bestTarget + ") ");
			}
		}
		// The target of the last cluster's entity will be the first one
		final String finalTarget = bestFromEntity;

//		Logger.getLogger(getClass().getName())
//				.info("Finished (hardcore topsimilarity - FROM(" + fromEntities.size() + ")xTO(" + toEntities.size()
//						+ ")) doing the two first entities in " + Stopwatch.endDiff(getClass().getName()) + " ms!");

		// Put choice into the map for tracking
		// Note: the 'target' of a similarity KEEPS THE SIMILARITY
		// Hence: Putting null/smallest value for SOURCE's similarity
		chosenClusterEntityMap.put(fromClusterName,
				new ImmutablePair<String, Double>(bestFromEntity, Double.MIN_VALUE));

		// Target entity gets the computed similarity
		chosenClusterEntityMap.put(toClusterName, bestTarget);
		similaritySum += bestTarget.getRight();

		// Now iterate through the rest to choose one each
		Stopwatch.endDiffStart("topSimilarity");

		return finalTarget;
	}

	/**
	 * Once the first one has been chosen, continues by taking the most-similar
	 * next-neighbour (does NOT check if the overall similarity is maximized)
	 * 
	 * @param clusters
	 * @param clusterNames
	 * @param chosenClusterEntityMap
	 * @return
	 */
	private Double chooseClosestToFirst(final Map<String, List<String>> clusters, final List<String> clusterNames,
			final Map<String, Pair<String, Double>> chosenClusterEntityMap) {
		Double similaritySum = 0D;
		String fromClusterName, toClusterName;
		for (int i = 1; i < clusterNames.size() - 1; ++i) {
			fromClusterName = clusterNames.get(i);
			toClusterName = clusterNames.get(i + 1);
			final Pair<String, Double> entitySimPair = this.similarityService
					.topSimilarity(chosenClusterEntityMap.get(fromClusterName).getLeft(), clusters.get(toClusterName));
			chosenClusterEntityMap.put(toClusterName, entitySimPair);
			similaritySum += entitySimPair.getRight();
//			Logger.getLogger(getClass().getName())
//					.info("Cluster[" + toClusterName + "]: Found a top-similar item(" + entitySimPair.getLeft() + ", "
//							+ entitySimPair.getRight() + ") in " + Stopwatch.endDiffStart("topSimilarity") + " ms!");
		}

		// # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
		// Then just get the similarity from the last one to the first one
		// # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

		// SOURCE of the "last-to-first" connection
		final String lastClusterName = clusterNames.get(clusterNames.size() - 1);
		// TARGET of the "last-to-first" connection
		final String firstClusterName = clusterNames.get(0);
		final String firstEntity = chosenClusterEntityMap.get(firstClusterName).getLeft();
		// The entities for the last and first were already chosen, but we need to see
		// how similar they actually are
		final Double lastFirstSimilarity = this.similarityService.similarity(
				// last entity
				chosenClusterEntityMap.get(lastClusterName).getLeft(),
				// first entity
				firstEntity).doubleValue();
		final Pair<String, Double> finalConnection = new ImmutablePair<String, Double>(firstEntity,
				lastFirstSimilarity);
		similaritySum += lastFirstSimilarity;
		chosenClusterEntityMap.put(firstClusterName, finalConnection);

		return similaritySum;
	}

	private Double getSimilaritySumToEntity(Map<String, Pair<String, Double>> chosenClusterEntityMap,
			final String sourceEntity) {
		Double sum = 0D;
		for (Map.Entry<String, Pair<String, Double>> e : chosenClusterEntityMap.entrySet()) {
			sum += this.similarityService.similarity(sourceEntity, e.getValue().getKey()).doubleValue();
		}
		return sum;
	}

	@Override
	public double getPickerWeight() {
		// TODO Auto-generated method stub
		return 10;
	}

}
