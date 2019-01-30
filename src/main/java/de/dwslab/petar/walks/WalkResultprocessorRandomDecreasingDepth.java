package de.dwslab.petar.walks;

public class WalkResultprocessorRandomDecreasingDepth extends WalkResultProcessorRandomUniform {

	private float[] depthDistribution = null;

	public WalkResultprocessorRandomDecreasingDepth(long seed) {
		super(seed);
		// Take all
		depthDistribution(new float[] { 1.0f });
	}

	public WalkResultprocessorRandomDecreasingDepth(final float[] depthDistribution) {
		this(System.currentTimeMillis());
		depthDistribution(depthDistribution);
	}

	public WalkResultprocessorRandomDecreasingDepth depthDistribution(float[] depthDistribution) {
		this.depthDistribution = depthDistribution;
		return this;
	}

	@Override
	public WalkResultProcessor updateDepth(int currDepth) {
		super.updateDepth(currDepth);
		this.probability(this.depthDistribution[Math.min(currDepth - 1, this.depthDistribution.length - 1)]);
		return this;
	}

	/**
	 * Normalizes all entries within the distribution and updates current
	 * probability (through calling the depth update function with the current
	 * depth) <br>
	 * Complexity: 2*n + O(updateDepth(currDepth)) where n =
	 * length(depthDistribution)
	 * 
	 * @return this
	 */
	public WalkResultprocessorRandomDecreasingDepth normalizeDistribution() {
		float sum = 0f;
		for (float f : this.depthDistribution) {
			sum += f;
		}

		for (int i = 0; i < this.depthDistribution.length; ++i) {
			this.depthDistribution[i] /= sum;
		}
		updateDepth(this.depth);

		return this;
	}
}
