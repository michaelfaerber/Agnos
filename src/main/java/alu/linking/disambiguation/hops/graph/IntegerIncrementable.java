package alu.linking.disambiguation.hops.graph;

import java.util.concurrent.atomic.AtomicLong;

/**
 * An integer-based (atomic long, actually) incrementable object
 * @author Kristian Noullet
 *
 */
public class IntegerIncrementable implements Incrementable<Integer> {
	public AtomicLong val;

	public IntegerIncrementable(final long init) {
		this.val = new AtomicLong(init);
	}

	@Override
	public IntegerIncrementable increase() {
		this.val.incrementAndGet();
		return this;
	}

	@Override
	public IntegerIncrementable decrease() {
		this.val.decrementAndGet();
		return this;
	}

	@Override
	public Number getNumberVal() {
		return this.val.get();
	}

	@Override
	public Integer getVal() {
		return getNumberVal().intValue();
	}

}
