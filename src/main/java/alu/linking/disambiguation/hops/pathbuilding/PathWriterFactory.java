package alu.linking.disambiguation.hops.pathbuilding;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

public class PathWriterFactory {
	private static Logger logger = Logger.getLogger(PathWriterFactory.class);

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
