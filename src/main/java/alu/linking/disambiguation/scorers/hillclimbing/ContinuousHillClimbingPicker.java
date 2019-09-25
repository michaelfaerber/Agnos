package alu.linking.disambiguation.scorers.hillclimbing;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.Comparators;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.executable.preprocessing.loader.PageRankLoader;
import alu.linking.mentiondetection.Mention;
import alu.linking.utils.MentionUtils;

/**
 * This cluster item picker serves to continuously call HillClimbingPicker,
 * while removing the weakest identified link in each iteration, until only 2
 * (including) remain
 * 
 * @author Kris
 *
 */
public class ContinuousHillClimbingPicker extends HillClimbingPicker {

	public ContinuousHillClimbingPicker(final BiFunction<Double, Double, Double> operation,
			final EntitySimilarityService similarityService, final PageRankLoader pagerankLoader) {
		super(operation, similarityService, pagerankLoader);
	}

	public ContinuousHillClimbingPicker(final EntitySimilarityService similarityService,
			final PageRankLoader pagerankLoader) {
		super(similarityService, pagerankLoader);
	}

	@Override
	public List<String> combine() {
		super.prune = false;

		final List<Mention> copyContext = Lists.newArrayList(this.context);
		// Sorts them for the sake of initialisation picking based on word order
		Collections.sort(copyContext, Comparators.mentionOffsetComparator);
		// Computing clusters outside, so we don't have to redo it every time
		final Map<String, List<String>> clusters = computeClusters(copyContext);
		// Remove entities that do not have an associated embedding
		// & cluster if they are left w/o entity as a result of it
		removeInvalidEmbeddings(clusters);

		final Map<String, List<MutablePair<String, Double>>> continuousChoices = new HashMap<>();
		while (copyContext.size() > 1) {
			// Do the picking logic
			final Map<String, Pair<String, Double>> iterationChoices = super.pickItems(clusters);

			for (Map.Entry<String, Pair<String, Double>> iterationChoice : iterationChoices.entrySet()) {
				final String key = iterationChoice.getKey();
				List<MutablePair<String, Double>> continuousPairs = continuousChoices.get(key);
				if (continuousPairs == null) {
					continuousPairs = Lists.newArrayList();
					continuousChoices.put(key, continuousPairs);
				}

				boolean found = false;
				final Pair<String, Double> iterationChoicePair = iterationChoice.getValue();
				for (MutablePair<String, Double> continuousPair : continuousPairs) {
					if (continuousPair.getKey().equals(iterationChoicePair.getKey())) {
						// Same entity = 'Collision' - so modify/update score accordingly
						found = true;
						// It's the same pair, so let's combine them!
						final Double currentValue = continuousPair.getValue();
						final Double newValue = computeNewValue(this.context.size() - clusters.size(), currentValue,
								iterationChoicePair.getValue());
						continuousPair.setValue(newValue);
					}
				}

				if (!found) {
					// Not a collision, so just add it
					continuousPairs.add(new MutablePair<String, Double>(iterationChoicePair.getLeft(),
							initVal(iterationChoicePair.getRight())));
				}
			}

			Double minValue = Double.MAX_VALUE;
			Pair<String, Double> minPair = null;
			String minKey = null;
			// Find the entity-score pair for the worst surface form
			for (Map.Entry<String, Pair<String, Double>> e : iterationChoices.entrySet()) {
				final Pair<String, Double> currentPair = e.getValue();
				final Double currentValue = currentPair.getRight();
				if (currentValue <= minValue) {
					minKey = e.getKey();
					minPair = currentPair;
					minValue = currentValue;
				}
			}

			// Remove surface form with worst result (as it likely is noise)
			clusters.remove(minKey);
			try {
				MentionUtils.removeStringMention(minKey, copyContext);
			} catch (IllegalArgumentException iae) {

			}
		}

		// Now just get the best one for each surface form
		final List<String> retList = Lists.newArrayList();
		for (Entry<String, List<MutablePair<String, Double>>> entrySurfaceForm : continuousChoices.entrySet()) {
			Double maxValue = Double.MIN_VALUE;
			Pair<String, Double> maxPair = null;
			String maxKey = null;

			for (MutablePair<String, Double> pair : entrySurfaceForm.getValue()) {
				if (pair.getValue() > maxValue) {
					maxPair = pair;
					maxValue = pair.getValue();
					maxKey = pair.getKey();
				}
			}
			if (maxKey != null) {
				retList.add(maxKey);
			}
		}

		getLogger().info("FINAL CHOICES[" + retList.size() + "]: " + retList);
		return retList;
	}

	private Double initVal(Double right) {
		// return right;
		return 1D;
	}

	/**
	 * Computes the new value based on the iteration that we are part of, as well as
	 * the previously existing value and the new value
	 * 
	 * @param iterationNumber
	 * @param previousValue
	 * @param currentValue
	 * @return
	 */
	private Double computeNewValue(int iterationNumber, Double previousValue, Double currentValue) {
		return previousValue + iterationNumber * currentValue;
		// return previousValue + currentValue;
		// return previousValue + 1;
	}

}
