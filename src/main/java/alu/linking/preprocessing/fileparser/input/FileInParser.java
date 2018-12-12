package alu.linking.preprocessing.fileparser.input;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public interface FileInParser<V> extends AutoCloseable {
	public List<V> getNext();

	public void close() throws IOException;

	public abstract FileInParser<V> create(final Reader reader, Object... params) throws IOException;

	// public V getLast(T line);

	// public V getConnector(T line);

	// public V getFirst(T line);

	public String toFormatString(V word);

	public String toFormatString(Object... words);

	public List<V> withHeader(final boolean header);

}
