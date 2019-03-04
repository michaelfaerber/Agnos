package alu.linking.disambiguation.scorers.subpagerank;

import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;

public abstract class AbstractScorerGraph {
	// How many iterations
	protected int iter = 5;
	// Likelihood of following a link
	protected double damping = 0.85d;
	// Minimum similarity threshold required to set an "edge" between two entities
	protected double minEdgeSimilarityThreshold;
	protected final EntitySimilarityService similarityService;
	protected final double MIN_SIMILARITY = 0.5d;
	protected double startValue = 0.1d;

	public AbstractScorerGraph(final EntitySimilarityService similarityService) {
		this.similarityService = similarityService;
	}

	public AbstractScorerGraph startValue(Number num) {
		this.startValue = num.doubleValue();
		return this;
	}

	public AbstractScorerGraph iterations(final int iterations) {
		this.iter = iterations;
		return this;
	}

	public AbstractScorerGraph dampingFactor(final Number damping) {
		this.damping = damping.doubleValue();
		return this;
	}

}
