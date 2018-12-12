package alu.linking.disambiguation;

public interface Scorer<T> {
	public Number computeScore(T assignment);

	public Number getWeight();
}
