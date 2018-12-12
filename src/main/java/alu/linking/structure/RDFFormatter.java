package alu.linking.structure;

public interface RDFFormatter<T> {
	public String format(final T subject, final T predicate, final T object);
}
