package alu.linking.disambiguation.scorers.embedhelp;

import java.util.function.BiFunction;

public abstract class AbstractClusterItemPicker implements ClusterItemPicker {
	public final BiFunction<Double, Double, Double> combinationOperation;

	protected AbstractClusterItemPicker(final BiFunction<Double, Double, Double> combinationOperation) {
		this.combinationOperation = combinationOperation;
	}

	protected Double applyOperation(Double existingPairValue, Double right) {
		return getCombinationOperation().apply(existingPairValue, right);
	}

	@Override
	public BiFunction<Double, Double, Double> getCombinationOperation() {
		return this.combinationOperation;
	}
}
