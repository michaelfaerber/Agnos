package alu.linking.executable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.structure.Executable;
import alu.linking.utils.DetectionUtils;

public class SSCCECreator implements Executable {

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.MAG;
		// For xLISA: NGram, NER, POS Tagging
		final String sample_text = "This is a sample text to be tested";
		// We process it with the usual mention detection
		// Then we go and check all the related candidates
		// Then we check for all the related embeddings

		// Mention detection needs: hashes (LSH) + sparse vectors (LSH) +
		// links/SurfaceForms
		// Candidate Generation: map with the appropriate keys taken out of the full
		// thing
		// Disambiguation: PageRank files + all related embeddings
		// Pruning: Nothing required as it is simply based on a threshold on the passed
		// score
		// LSH Hashes - contains the words in the first column, so start with it
		final String out_hashes = "", out_sparse_vectors = "", out_surface_form_links = "";
		final List<String> toExtractSF = null;
		final List<Long> lines = grabData(KG, FilePaths.FILE_LSH_HASHES, out_hashes, toExtractSF,
				Strings.LSH_HASH_DELIMITER.val, 0);
		// LSH Sparse vectors - only has the array outputs
		grabData(KG, FilePaths.FILE_LSH_DOCUMENT_VECTORS_SPARSE, out_sparse_vectors, lines);
		// LinksSurfaceForms.txt
		grabData(KG, FilePaths.FILE_ENTITY_SURFACEFORM_LINKING, out_surface_form_links, toExtractSF,
				Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val, 1);
	}

	private static void grabData(final EnumModelType KG, final FilePaths inPath, final String outPath,
			final Collection<Long> lineNumberCol) {
		final List<Long> lineNumbers = new ArrayList<>(lineNumberCol);
		Collections.sort(lineNumbers);
		long lineCounter = 0l;
		int listPos = 0;
		long listVal = lineNumbers.get(0);

		try (final BufferedReader brIn = new BufferedReader(new FileReader(new File(inPath.getPath(KG))));
				final BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outPath)))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				if (lineCounter == listVal) {
					// Output line + move the stuff
					bwOut.write(line);
					bwOut.newLine();
					// Advance counter
					listPos++;
					if (listPos >= lineNumbers.size()) {
						break;
					}
					listVal = lineNumbers.get(listPos);
				} else {
					// Continue on to next line...
				}
				lineCounter++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Line-by-line logic to grab something if a specific position in the line by
	 * delimiter should be output to a specified file
	 * 
	 * @param KG             for which KG we want it to be done (affects the final
	 *                       path)
	 * @param inPath         where to get the data from
	 * @param outPath        where to output
	 * @param toExtractItems list of items we are looking for
	 * @param delim          delimiter
	 * @param position       position by delimiter
	 */
	private static List<Long> grabData(final EnumModelType KG, final FilePaths inPath, final String outPath,
			final Collection<String> toExtractItems, final String delim, final int position) {
		final List<Long> lineNumbers = Lists.newArrayList();
		long lineCounter = 0;
		try (final BufferedReader brIn = new BufferedReader(new FileReader(new File(inPath.getPath(KG))));
				final BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outPath)))) {

			String line = null;
			while ((line = brIn.readLine()) != null) {
				final String[] tokens = line.split(delim);
				if (tokens.length <= position) {
					throw new RuntimeException("Searched item supposedly at index[" + position + "], tokens["
							+ Arrays.toString(tokens) + "] only have length[" + tokens.length + "]");
				}
				// If current line is among the wanted mentions
				if (toExtractItems.contains(tokens[position])) {
					// Output line as-is
					bwOut.write(line);
					// And add a newline character
					bwOut.newLine();
					// Keep track of which line was added
					lineNumbers.add(lineCounter);
				}
				lineCounter++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lineNumbers;
	}

	/**
	 * Grabs candidates for candidate generation
	 * 
	 * @param KG
	 * @param outPath
	 * @param toExtractCandidates
	 * @throws IOException
	 */
	private static void grabCandidates(final EnumModelType KG, final String outPath,
			final Collection<String> toExtractCandidates) throws IOException {
		// Extract it somehow
		// Load map from inPath into inMap
		final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
		final Map<String, Collection<String>> inMap = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
		final Map<String, Collection<String>> outMap = new HashMap<>();
		for (String candidate : toExtractCandidates) {
			Collection<String> entities = inMap.get(candidate);
			if (entities == null) {
				throw new IllegalArgumentException("No Candidate(" + candidate + ") found.");
			}
			outMap.put(candidate, entities);
		}
		outputMap(outPath, outMap);
	}

	private static <S, T> void outputMap(final String path, Map<S, T> outMap) {
		for (Map.Entry<S, T> e : outMap.entrySet()) {

		}
	}

	@Override
	public void init() {
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		return null;
	}

	@Override
	public boolean destroy() {
		return false;
	}
}
