package alu.linking.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.yars.nx.parser.NxParser;

public class IteratorFileEntity implements Iterator<String>, AutoCloseable {
	final NxParser parser;
	// Have to keep a reference to the reader due to parser not having a close()
	// method...
	final BufferedReader reader;
	final boolean skipDuplicates;
	final Set<String> duplicateChecker = new HashSet<>();

	IteratorFileEntity(final File ntFile, final boolean skipDuplicates) throws FileNotFoundException {
		this.reader = new BufferedReader(new FileReader(ntFile));
		this.parser = new NxParser(reader);
		this.skipDuplicates = skipDuplicates;
	}

	@Override
	public boolean hasNext() {
		return parser.hasNext();
	}

	@Override
	public String next() {
		// Takes the subject
		String ret;
		if (parser.hasNext()) {
			ret = parser.next()[0].toString();
		} else {
			ret = null;
		}
		// Removes starting and ending "<" resp. ">"
		if (ret.startsWith("<") && ret.endsWith(">")) {
			ret = ret.substring(1, ret.length() - 1);
		}

		if (skipDuplicates) {
			if (duplicateChecker.contains(ret)) {
				// It's a duplicate, so go to the next one
				if (hasNext()) {
					return next();
				} else {
					return null;
				}
			} else {
				// It didn't exist yet, so just add it so we know for the future and return w/e
				// needs to be returned
				duplicateChecker.add(ret);
			}
		}
		return ret;
	}

	@Override
	public void close() throws IOException {
		this.reader.close();
	}

}
