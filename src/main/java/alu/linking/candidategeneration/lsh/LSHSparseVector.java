package alu.linking.candidategeneration.lsh;

import java.util.HashSet;
import java.util.Set;

public class LSHSparseVector<V> {
	private Set<Integer> entries = new HashSet<>();
	private int length;
	private V missValue;
	private V hitValue;

	public static LSHSparseVector<Boolean> create(int length) {
		return new LSHSparseVector<Boolean>(length, false, true);
	}

	LSHSparseVector(int length, V missValue, V hitValue) {
		this.length = length;
		this.missValue = missValue;
		this.hitValue = hitValue;
	}

	public V get(int index) {
		if (index < 0 || index > length || !entries.contains(index)) {
			return missValue;
		} else {
			return hitValue;
		}
	}

	public Set<Integer> getEntries() {
		return this.entries;
	}

	public void set(int index) {
		this.entries.add(index);
	}

	public boolean[] toBooleanArray() {
		boolean[] arr = new boolean[length];
		final double loadRatioThreshold = 0.6;
		if (loadRatioThreshold * ((double) length) <= entries.size()) {
			// There are a lot of entries, so input entries alongside the miss values
			// Threshold at 0.3 -> if the entries fill up 30% of the vector or more
			for (int i = 0; i < length; ++i) {
				arr[i] = entries.contains(i) ? true : false;
			}
		} else {
			// There are few entries -> fill it out with default value, then with entries
			for (int i = 0; i < arr.length; ++i) {
				arr[i] = false;
			}
			for (Integer hitIndex : entries) {
				arr[hitIndex] = true;
			}
		}
		return arr;
	}
}
