package alu.linking.disambiguation.hops.graph;

public interface Incrementable<T> {
	public Incrementable<T> increase();
	public Incrementable<T> decrease();
	public Number getNumberVal();
	public T getVal();
	
}
