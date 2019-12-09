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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.structure.Executable;
import alu.linking.utils.DetectionUtils;
import alu.linking.utils.TextUtils;

public class SSCCECreator implements Executable {

	public static void main(String[] args) {
		try {
			new SSCCECreator().exec();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		return grabData(KG, inPath, outPath, toExtractItems, delim, position, "", "");
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
	 * @param prefix         prefix on the line that our toExtractItems need to add
	 *                       in order to match
	 * @param suffix         suffix on the line that our toExtractItems need to add
	 *                       in order to match
	 */
	private static List<Long> grabData(final EnumModelType KG, final FilePaths inPath, final String outPath,
			final Collection<String> toExtractItems, final String delim, final int position, final String prefix,
			final String suffix) {
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
				String token = tokens[position];
				// Remove prefix from the string to be compared
				if (prefix != null && prefix.length() > 0 && token.startsWith(prefix)) {
					token = token.substring(prefix.length());
				}
				// Remove suffix from the string to be compared
				if (suffix != null && suffix.length() > 0 && token.endsWith(suffix)) {
					token = token.substring(0, token.length() - suffix.length());
				}

				if (toExtractItems.contains(token)) {
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
	private static List<String> grabCandidates(final EnumModelType KG, final String outPath,
			final Collection<String> toExtractCandidates) throws IOException {
		// Extract it somehow
		// Load map from inPath into inMap
		final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
		final List<String> combinedValues = Lists.newArrayList();
		final Map<String, Collection<String>> inMap = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
		final Map<String, Collection<String>> outMap = new HashMap<>();
		for (String candidate : toExtractCandidates) {
			Collection<String> entities = inMap.get(candidate);
			if (entities == null) {
				throw new IllegalArgumentException("No Candidate(" + candidate + ") found! Nay!");
			}
			outMap.put(candidate, entities);
		}
		Collection<Collection<String>> values = outMap.values();
		for (Collection<String> coll : values) {
			for (String candidateEntity : coll) {
				combinedValues.add(TextUtils.stripArrowSigns(candidateEntity));
			}
		}
		final String delim = Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val;
		outputMap(outPath, outMap, delim);
		return combinedValues;
	}

	private static <S, T> void outputMap(final String path, Map<S, T> outMap, final String delim) {
		final StringBuilder sbOut = new StringBuilder();
		for (Map.Entry<S, T> e : outMap.entrySet()) {
			if (e.getValue() instanceof Collection) {
				for (Object o : ((Collection) (e.getValue()))) {
					sbOut.append(e.getKey());
					sbOut.append(delim);
					sbOut.append(o);
					sbOut.append(Strings.NEWLINE.val);
				}
			} else {
				sbOut.append(e.getKey());
				sbOut.append(delim);
				sbOut.append(e.getValue());
				sbOut.append(Strings.NEWLINE.val);
			}
		}
	}

	@Override
	public void init() {
		// None needed?
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		final EnumModelType KG = EnumModelType.//
				CRUNCHBASE//
		// MAG//
		;
		// For xLISA: NGram, NER, POS Tagging
		final String inText = "This is a sample text to be tested frank sinatra";
		// Whether mention detection should be LSH or exact map matching
		final boolean LSH_OR_EXACTMAP = false;

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
		final String out_hashes = "./sscce/ssccecreator_hashes_" + KG.name() + ".txt", //
				out_sparse_vectors = "./sscce/ssccecreator_sparse_vectors_" + KG.name() + ".txt", //
				out_surface_form_links = "./sscce/ssccecreator_surface_forms_" + KG.name() + ".txt", //
				out_candidates = "./sscce/ssccecreator_candidates_" + KG.name() + ".txt", //
				out_pagerank = "./sscce/ssccecreator_pagerank_" + KG.name() + ".txt", //
				out_embeddings = "./sscce/ssccecreator_embeddings_" + KG.name() + ".txt" //
		;
		final List<String> toExtractSF;
		final List<Long> lines;

		// ----------------------------------------------
		// Detect the words appearing in the sentence
		// ----------------------------------------------
		System.out.println("Starting mention detection");
		final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
		final Set<String> stopwords = stopwordsLoader.getStopwords();
		final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
		System.out.println("Surface forms: " + map.size());
		final InputProcessor inputProcessor = new InputProcessor(stopwords);
		// ########################################################
		// Mention Detection
		// ########################################################
		MentionDetector md = DetectionUtils.setupMentionDetection(KG, map, inputProcessor, LSH_OR_EXACTMAP);
		System.out.println("Detecting!");
		final List<Mention> mentions = md.detect(inText);
		final Set<String> stringMentions = new HashSet<>();
		for (Mention m : mentions) {
			stringMentions.add(m.getMention());
		}
		md = null;
		toExtractSF = new ArrayList<>(stringMentions);
		System.out.println("Found [" + toExtractSF.size() + "] mentions.");
		System.out.println(toExtractSF.subList(0, Math.min(toExtractSF.size(), 20)));
		// --------------------------- End of Mention Detection

		if (LSH_OR_EXACTMAP) {
			System.out.println("LSH Data grabbing");
			// Relies on the existing LSH signatures to see what entities we want
			// lines = which lines (by line number) we want to be copied over
			lines = grabData(KG, FilePaths.FILE_LSH_HASHES, out_hashes, toExtractSF, Strings.LSH_HASH_DELIMITER.val, 0);
			// LSH Sparse vectors - only has the array outputs
			grabData(KG, FilePaths.FILE_LSH_DOCUMENT_VECTORS_SPARSE, out_sparse_vectors, lines);
			// LinksSurfaceForms.txt
			grabData(KG, FilePaths.FILE_ENTITY_SURFACEFORM_LINKING, out_surface_form_links, toExtractSF,
					Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val, 1);
		} else {
			// grab
			// new MentionDetectorMap();
		}

		// Candidate Generation - Grab all entities
		System.out.println("Grabbing candidates");
		final List<String> toExtractEntities = grabCandidates(KG, out_candidates, toExtractSF);
		System.out.println("Grabbed entity candidates (" + toExtractEntities.size() + ") to extract");
		System.out.println("Some candidates: " + toExtractEntities.subList(0, Math.min(50, toExtractEntities.size())));

		// PageRank
		// <http://dbpedia.org/resource/Hypogaea> <http://purl.org/voc/vrank#pagerank>
		// "0.2353255484"^^<http://www.w3.org/2001/XMLSchema#float> .
		System.out.println("Grabbing pagerank");
		grabData(KG, FilePaths.FILE_PAGERANK, out_pagerank, toExtractEntities, " ", 0, "<", ">");

		// Embeddings
		// http://dbpedia.org/resource/Tuřany_(Kladno_District)\t...\t...\t ...
		System.out.println("Grabbing embeddings");
		grabData(KG, FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS, out_embeddings, toExtractEntities,
				Strings.EMBEDDINGS_SENTENCES_DELIM.val, 0);
		return null;

	}

	@Override
	public boolean destroy() {
		return false;
	}
}
