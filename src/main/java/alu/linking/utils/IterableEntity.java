package alu.linking.utils;

import java.util.Iterator;

public abstract class IterableEntity implements Iterable<String>, AutoCloseable {
	@Override
	public abstract Iterator<String> iterator();

	@Override
	public abstract void close() throws Exception;
}
