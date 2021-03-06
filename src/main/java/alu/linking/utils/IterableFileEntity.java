package alu.linking.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import alu.linking.structure.Loggable;

/**
 * Uses NXParser to iterate through a file, returning the subject (aka. item at
 * index 0) at each point. </br>
 * If the skipDuplicates flag is set to TRUE, it will keep track of
 * already-visited elements and return unique entries only
 * 
 * @author Kristian Noullet
 *
 */
public class IterableFileEntity extends IterableEntity implements Loggable {
	private File ntFile = null;
	private IteratorFileEntity iterator = null;
	private boolean skipDuplicates;

	public IterableFileEntity(final File ntFile, final boolean skipDuplicates)
			throws FileNotFoundException, IOException {
		// Pass a .NT file
		this.ntFile = ntFile;
		this.skipDuplicates = skipDuplicates;
	}

	@Override
	public void close() throws Exception {
		if (iterator != null) {
			iterator.close();
		}
	}

	@Override
	public Iterator<String> iterator() {
		try {
			if (this.iterator != null) {
				// Added closing of previous iterator into a separate try-catch as we just want
				// to close it and still be able to open a new one (would be annoying if closing
				// an old one would impede creating a new one)
				try {
					this.iterator.close();
				} catch (IOException e) {
					getLogger().error("IOException while closing file-based entity iterator ", e);
				}
			}
			this.iterator = new IteratorFileEntity(ntFile, skipDuplicates);
			return this.iterator;
		} catch (FileNotFoundException e) {
			getLogger().error("FNFE while initialising file-based entity iterator ", e);
			this.iterator = null;
		}
		// Returns null rather than current iterator due to issues with the iterator
		// instance being the only way to get here, meaning fail-fast is the best
		// behaviour
		return null;
	}

}
