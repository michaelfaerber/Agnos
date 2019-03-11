package alu.linking.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.semanticweb.yars.nx.parser.NxParser;

public class IteratorFileEntity implements Iterator<String>, AutoCloseable {
	final NxParser parser;
	// Have to keep a reference to the reader due to parser not having a close()
	// method...
	final BufferedReader reader;

	IteratorFileEntity(final File ntFile) throws FileNotFoundException {
		this.reader = new BufferedReader(new FileReader(ntFile));
		this.parser = new NxParser(reader);
	}

	@Override
	public boolean hasNext() {
		return parser.hasNext();
	}

	@Override
	public String next() {
		// Takes the subject
		return parser.next()[0].toString();
	}

	@Override
	public void close() throws IOException {
		this.reader.close();
	}

}
