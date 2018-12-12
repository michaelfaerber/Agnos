package alu.linking.structure;

import java.io.IOException;
import java.io.StringWriter;

public abstract class BufferedSeparableWriter extends StringWriter implements SeparableEntry {

	public BufferedSeparableWriter() {
		super();
	}

	public BufferedSeparableWriter(int i) {
		super(i);
	}

	public abstract void newEntry() throws IOException;

	public abstract void newLine() throws IOException;

}
