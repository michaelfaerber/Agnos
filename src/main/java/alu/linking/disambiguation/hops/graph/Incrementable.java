package alu.linking.disambiguation.hops.graph;

/**
 * Incrementable interface allowing for object to be increased, decreased,
 * having its value returned as well as having it returned in a numerical form
 * 
 * @author Kristian Noullet
 *
 * @param <T>
 */
public interface Incrementable<T> {
	public Incrementable<T> increase();

	public Incrementable<T> decrease();

	public Number getNumberVal();

	public T getVal();

}
