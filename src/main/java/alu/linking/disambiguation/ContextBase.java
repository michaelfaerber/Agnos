package alu.linking.disambiguation;

import java.util.Collection;

public interface ContextBase<M> {
	public void linkContext(Collection<M> context);

	public void updateContext();

}
