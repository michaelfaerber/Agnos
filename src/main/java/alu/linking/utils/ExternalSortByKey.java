package alu.linking.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

/**
 * Sorting algorithm making use of the file system and as such avoiding having
 * to load everything into memory
 */
public class ExternalSortByKey {

	private final String keyDelimiter;
	private final int keyPosition;
	private final static String defaultTmpFolder = "./tmpGroupByKey/";
	private final File tmpFolder;
	private final DualHashBidiMap<String, File> mappingKeyToFilePath = new DualHashBidiMap<>();

	/**
	 * See {@link #ExternalSortByKey(String, int, File}
	 */
	public ExternalSortByKey(final String keyDelimiter, final int keyPosition) {
		this(keyDelimiter, keyPosition, new File(defaultTmpFolder));
	}

	/**
	 * 
	 * @param keyDelimiter delimiter allowing us to determine a key
	 * @param keyPosition  key's position post splitting
	 * @param tmpFolder    temporary folder for external sort processing
	 * 
	 */
	public ExternalSortByKey(final String keyDelimiter, final int keyPosition, final File tmpFolder) {
		this.keyDelimiter = keyDelimiter;
		this.keyPosition = keyPosition;
		if (keyPosition < 0) {
			throw new IllegalArgumentException("Key position has to be greater than 0");
		}
		this.tmpFolder = tmpFolder;
		if (!tmpFolder.exists() && !tmpFolder.mkdirs()) {
			throw new RuntimeException("Could not create tmp folder: " + tmpFolder.getAbsolutePath());
		}
	}

	/**
	 * Sorts file by defined key and overwrites the passed file with the found
	 * results
	 * 
	 * @param inFile input and output file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void process(final String inFile) throws FileNotFoundException, IOException {
		process(inFile, inFile);
	}

	/**
	 * Calls {@link #process(File, File)} with process(new File(inFile), new
	 * File(outFile));
	 * 
	 * @param inFile  input file
	 * @param outFile output file
	 * @throws IOException
	 */
	public void process(String inFile, String outFile) throws IOException {
		process(new File(inFile), new File(outFile));
	}

	/**
	 * Takes contents from inFile and sort-groups them based only on defined key
	 * into outFile
	 * 
	 * @param inFile  file to read from
	 * @param outFile file to output to
	 * @throws IOException
	 */
	private void process(File inFile, File outFile) throws IOException {
		// First read from input and partition into subfiles based on 'key' into the tmp
		// folder
		String line = null;
		try (final BufferedReader brIn = new BufferedReader(new FileReader(inFile))) {
			while ((line = brIn.readLine()) != null) {
				final String key = line.split(keyDelimiter)[keyPosition];
				final String filename = key.replaceAll("[^\\p{Alnum}]", "_");
				final File tmpKeyFile = new File(tmpFolder, filename);
				mappingKeyToFilePath.put(key, tmpKeyFile);
				if (!tmpKeyFile.exists()) {
					tmpKeyFile.createNewFile();
				}
				try (final BufferedWriter wrtKeyLine = new BufferedWriter(new FileWriter(tmpKeyFile, true))) {
					wrtKeyLine.write(line);
					wrtKeyLine.newLine();
				}
			}
		}
		line = null;
		// Done grouping lines into files according to keys, so it's time to merge it
		// all back together
		final File[] outFiles = this.tmpFolder.listFiles();
		Arrays.sort(outFiles);
		// Now output the sorted files with the grouped content together into outFile
		line = null;
		try (BufferedWriter bwOut = new BufferedWriter(new FileWriter(outFile))) {
			for (File f : outFiles) {
				if (!f.isFile()) {
					continue;
				}
				try (BufferedReader brIn = new BufferedReader(new FileReader(f))) {
					while ((line = brIn.readLine()) != null) {
						bwOut.write(line);
						bwOut.newLine();
					}
				}
			}
		}
	}

}
