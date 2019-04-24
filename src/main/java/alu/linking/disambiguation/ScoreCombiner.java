package alu.linking.disambiguation;

import alu.linking.config.constants.Numbers;
import alu.linking.disambiguation.scorers.PageRankScorer;
import alu.linking.disambiguation.scorers.VicinityScorer;
import alu.linking.structure.Loggable;

public class ScoreCombiner<T> implements Loggable {
	public Number combine(final Number currScore, final Scorer<T> scorer, final T scorerParam) {
		// Add all types of scorers here with the appropriate weights


		if (scorer instanceof PageRankScorer) {
			// Pretty much just sets the weight
			final Double prScore = Numbers.PAGERANK_WEIGHT.val.doubleValue()
					// Due to PR values varying highly, doing a square root of it to slightly
					// smoothen it out
					* Math.sqrt(scorer.computeScore(scorerParam).doubleValue());
			return add(currScore, prScore);
		} else if (scorer instanceof VicinityScorer) {
			final Double vicScore = Numbers.VICINITY_WEIGHT.val.doubleValue()
					* scorer.computeScore(scorerParam).doubleValue();
			return add(currScore, vicScore);

		} else {
			final Number weight = scorer.getWeight();
			final Number score = scorer.computeScore(scorerParam);

			return add(currScore, weight.doubleValue() * score.doubleValue());
		}
	}

	/**
	 * Transforms both numbers to double and adds them together.<br>
	 * <b>If currScore is NULL, it is treated as 0.</b>
	 * 
	 * @param currScore
	 * @param score
	 * @return
	 */
	private Number add(Number currScore, Number score) {
		return currScore == null ? score.doubleValue() : currScore.doubleValue() + score.doubleValue();
	}
}
