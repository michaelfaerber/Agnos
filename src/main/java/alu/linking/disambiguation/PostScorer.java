package alu.linking.disambiguation;

import java.util.Collection;

public interface PostScorer<T, M> extends Scorer<T> {
	public void linkContext(Collection<M> context);

	public void updateContext();

}
