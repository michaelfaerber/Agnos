package alu.linking.disambiguation.hops.pathbuilding;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

public class PathWriterFactory {
	private static Logger logger = Logger.getLogger(PathWriterFactory.class);

	/**
	 * Creates a write instance to write results to - no or a null argument being
	 * passed results in a StringListWriter type, in case of 1 argument being
	 * passed, attempts to create a FileWriter instance
	 * 
	 * @param strings strings to base decision on
	 * @return write instance to output results to
	 * @throws IOException if the wrong number of input arguments are provided (>1)
	 */
	public static Writer create(final String... strings) throws IOException {
		if (strings == null || strings.length == 0 || strings[0] == null) {
			// Create a list writer
			return new StringListWriter();
		} else if (strings.length == 1) {
			return new BufferedWriter(new FileWriter(strings[0]));
		}
		throw new IllegalArgumentException("Illegal argument passed!");
	}
}
