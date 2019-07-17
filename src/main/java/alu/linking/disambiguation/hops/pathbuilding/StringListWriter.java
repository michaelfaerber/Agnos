package alu.linking.disambiguation.hops.pathbuilding;

import java.io.IOException;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.structure.BufferedSeparableWriter;

/**
 * A write instance to keep lists of strings
 * @author Kristian Noullet
 *
 */
public class StringListWriter extends BufferedSeparableWriter {
	private final StringBuilder entry = new StringBuilder();
	private final List<String> entries = Lists.newArrayList();
	private final char CR = 0x0D;
	private final char LF = 0x0A;

	public StringListWriter() {
		super();
	}

	@Override
	public void write(char[] cbuf) {
		write(cbuf, 0, cbuf.length);
	}

	@Override
	public void write(int c) {
		write(String.valueOf(c));
	}

	@Override
	public void write(String str) {
		write(str.toCharArray());
	}

	@Override
	public void write(String str, int off, int len) {
		write(str.toCharArray(), off, len);
	}

	@Override
	public void write(char[] cbuf, int off, int len) {
		// Only need to implement this one, as all other write operations refer to it
		// Write it out normally
		// super.write(cbuf, off, len);
		// Update our 'entry'
		try {
			char[] newStuff = new char[len];
			for (int i = off; i < Math.min(off + len, cbuf.length); ++i) {
				newStuff[i - off] = cbuf[i];
			}
			entry.append(newStuff);
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		// TODO: Newline detection based on input characters
		// System.out.println("Added: " + Arrays.toString(newStuff));
	}

	@Override
	public void flush() {
	}

	@Override
	public void newEntry() throws IOException {
		// Means it's a new entry to be added to our list!
		entries.add(this.entry.toString());
		this.entry.setLength(0);
	}

	@Override
	public void newLine() throws IOException {
		newEntry();
	}

	@Override
	public void close() throws IOException {
		super.close();
		// Add currently-found entry to it
		if (this.entry.length() > 0) {
			newEntry();
		}
	}

	/**
	 * Returns all entries that were found by the used PathCrawler
	 * 
	 * @return
	 */
	public List<String> getEntries() {
		return entries;
	}
}
