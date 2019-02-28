package alu.linking.preprocessing.embeddings;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.google.code.externalsorting.ExternalSort;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.executable.preprocessing.util.FileCombiner;
import alu.linking.preprocessing.embeddings.sentenceformatter.SSPEmbeddingSentenceFormatter;
import alu.linking.structure.Executable;

public class SSPEmbeddingGenerator implements Executable {
	private final EnumModelType KG;

	public SSPEmbeddingGenerator(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		/**
		 * Prerequisites: -The Query preprocessing completed prior to this execution
		 * 
		 * Go through all entities<br>
		 * For each: get <br>
		 * (1) the surface form, <br>
		 * (2) the helping surface forms, <br>
		 * (3) the NP helping surface forms' nounphrases, and <br>
		 * (4) the NP URL helping surface forms' nounphrases <br>
		 * -Potentially remove stopwords <br>
		 * -Concatenate them all together on one line<br>
		 * -Concatenate the produced files to have 1 file containing all sentences
		 * delimited by a specified delimiter <br>
		 */
		try {
			// Should contain entity surface form linking
			final Map<String, Collection<String>> mapEntitySF = new MentionPossibilityLoader(KG)
					.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
			// Concat wanted contents to single file
			final File[] sfInputFiles = new File(FilePaths.DIR_QUERY_OUT_SURFACEFORM.getPath(KG)).listFiles();
			final File[] hsfInputFiles = new File(FilePaths.DIR_QUERY_OUT_HELPING_SURFACEFORM.getPath(KG)).listFiles();
			final File[] np_hsfInputFiles = new File(FilePaths.DIR_QUERY_OUT_NP_HELPING_SURFACEFORM.getPath(KG))
					.listFiles();
			final File[] np_url_hsfInputFiles = new File(FilePaths.DIR_QUERY_OUT_NP_URL_HELPING_SURFACEFORM.getPath(KG))
					.listFiles();
			final FileCombiner fc = new FileCombiner();
			final File fileCombinedQueryOutput = new File(FilePaths.FILE_SSP_QUERY_OUT_COMBINED_OUTPUT.getPath(KG));
			boolean notFirst = false;
			// Surface Forms
			for (File f : sfInputFiles) {
				if (!f.isFile() || f.isHidden())
					continue;
				fc.exec(f, fileCombinedQueryOutput, notFirst);
				notFirst = true;
			}
			// Helping Surface Forms
			for (File f : hsfInputFiles) {
				if (!f.isFile() || f.isHidden())
					continue;
				fc.exec(f, fileCombinedQueryOutput, notFirst);
				notFirst = true;
			}
			// NP Helping Surface Forms
			for (File f : np_hsfInputFiles) {
				if (!f.isFile() || f.isHidden())
					continue;
				fc.exec(f, fileCombinedQueryOutput, notFirst);
				notFirst = true;
			}
			// NP URL Helping Surface Forms
			for (File f : np_url_hsfInputFiles) {
				if (!f.isFile() || f.isHidden())
					continue;
				fc.exec(f, fileCombinedQueryOutput, notFirst);
				notFirst = true;
			}
			final String embeddingRepOutput = FilePaths.FILE_EMBEDDINGS_SSP_TEXTDATA_SORTED.getPath(KG);
			final String sentencesOutput = FilePaths.FILE_EMBEDDINGS_SSP_SENTENCES.getPath(KG);
			// Apply external sorting on the combined file
			ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(fileCombinedQueryOutput),
					new File(embeddingRepOutput));
			// Once they're sorted, apply 'grouping' logic
			new SSPEmbeddingSentenceFormatter().groupSortedFile(embeddingRepOutput, sentencesOutput,
					Strings.EMBEDDINGS_SENTENCES_DELIM.val, Strings.QUERY_RESULT_DELIMITER.val, 0);
			// Now all sentences are grouped by key, so it should be ready for training
			System.out.println("embeddingRepOutput: " + embeddingRepOutput);
			System.out.println("Output sentences to: " + sentencesOutput);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

}
