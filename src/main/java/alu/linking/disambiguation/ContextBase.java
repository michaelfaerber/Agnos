package alu.linking.disambiguation;

import java.util.Collection;

public interface ContextBase<M> {
	/**
	 * Generally meant for some stuff that should be executed upon context changing,
	 * e.g. when something should be computed only once and not every time an item
	 * is to be scored
	 */
	public void updateContext(Collection<M> context);

}
