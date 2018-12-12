package alu.linking.preprocessing.embeddings.sentenceformatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import alu.linking.structure.Loggable;

/**
 * Takes the combined output of the queries assuming that they are sorted and
 * produces sentences based on the data
 * 
 * @author kris
 *
 */
public abstract class EmbeddingSentenceFormatter implements Loggable {

	/**
	 * Takes a sorted (by key - as defined by keyPos) input file and groups the
	 * lines by it
	 * 
	 * @param sortedInputFile sorted input file (most of the time sorted by the
	 *                        first token, meaning that the default keyPos should be
	 *                        equal to 0 for most cases)
	 * @param outputFile      where to output the aggregated result
	 * @param outDelim        delimiter to use for the output
	 * @param inDelim         delimiter used in the input file
	 * @param keyPos          position of the key to aggregate on (very important
	 *                        for the line by line logic)
	 */
	public void groupSortedFile(String sortedInputFile, String outputFile, String outDelim, String inDelim,
			final int keyPos) {
		try (BufferedReader br = new BufferedReader(new FileReader(sortedInputFile));
				final BufferedWriter bw = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
			// Assumption: there is at least one line
			// Note: Assumption is no longer necessary
			String line = null;// br.readLine();// null
			String currGroup = null;// = (line = br.readLine()).split(inDelim)[keyPos];
			String prevGroup = null;
			// prev/curr group logic only works due to
			// (1) the output being in groups
			// (2) once a 'group' ends, it doesn't return
			int lineCounter = 0;
			while ((line = br.readLine()) != null) {
				currGroup = extractKey(line, keyPos, inDelim);
				if (!currGroup.startsWith("http://")) {
					System.out.println("Weird-looking group(" + lineCounter + "): " + currGroup);
				}
				lineCounter++;
				if (currGroup != null) {
					// These add to the front, not the back
					if (prevGroup == null || !prevGroup.equals(currGroup)) {
						// Goes to next line once the group changed
						if (prevGroup != null) {
							bw.newLine();
						}
						// New 'group' -> output entity
						bw.write(currGroup);
						bw.write(outDelim);
					} else {
						// Adds a delimiter to add the next sentence of this same group
						bw.write(outDelim);
					}
					// Output data as wanted (generally removes the 'currGroup' from it as well)
					bw.write(formatOutput(line, currGroup, inDelim, keyPos));
				}
				prevGroup = currGroup;
			}
			getLogger().info("Output sentences into:");
			getLogger().info(new File(outputFile).getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Extracts key from the given line with help of the key's position delimited by
	 * a specified delimiter
	 * 
	 * @param line    line to extract from
	 * @param keyPos  index of key
	 * @param inDelim delimiter by which to separate line
	 * @return the key
	 */
	public String extractKey(String line, int keyPos, String inDelim) {
		return line.split(inDelim)[keyPos];
	}

	public abstract String formatOutput(String line, String entity, String lineDelim, int entityPos);

}
