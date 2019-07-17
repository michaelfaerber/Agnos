package alu.linking.disambiguation.scorers.subpagerank;

import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;

/**
 * Abstract graph mostly utilised for sub-pagerank scoring algorithm( alternative)s
 * @author Kristian Noullet
 *
 */
public abstract class AbstractScorerGraph {
	// How many iterations
	protected int iter = 5;
	// Likelihood of following a link
	protected double damping = 0.85d;
	// Minimum similarity threshold required to set an "edge" between two entities
	protected final double minEdgeSimilarityThreshold;
	protected final EntitySimilarityService similarityService;
	protected double startValue = 0.1d;

	public AbstractScorerGraph(final EntitySimilarityService similarityService) {
		this(similarityService, 0.5);
	}

	public AbstractScorerGraph(final EntitySimilarityService similarityService,
			final double minEdgeSimilarityThreshold) {
		this.similarityService = similarityService;
		this.minEdgeSimilarityThreshold = minEdgeSimilarityThreshold;
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
