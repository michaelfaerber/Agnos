package alu.linking.disambiguation;

import java.util.function.BiFunction;

import alu.linking.disambiguation.scorers.embedhelp.ClusterItemPicker;

public enum CombineOperation {
	OCCURRENCE(ClusterItemPicker::occurrenceOperation), //
	MAX_SIM(ClusterItemPicker::maxedOperation), //
	SIM_ADD(ClusterItemPicker::similarityOperation), //
	SIM_SQUARE_ADD(ClusterItemPicker::similaritySquaredOperation),//

	;
	public final BiFunction<Double, Double, Double> combineOperation;

	CombineOperation(BiFunction<Double, Double, Double> combineOperation) {
		this.combineOperation = combineOperation;
	}
}
