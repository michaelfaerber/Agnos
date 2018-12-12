package alu.linking.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.code.externalsorting.ExternalSort;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.embeddings.sentenceformatter.RDF2VecEmbeddingSentenceFormatter;
import alu.linking.structure.Loggable;
import de.dwslab.petar.walks.StringDelims;
import de.dwslab.petar.walks.WalkGenerator;

public class LauncherWalkGenerator implements Loggable {
	public static void main(String[] args) {
		new LauncherWalkGenerator().exec();
	}

	private void exec() {
		final EnumModelType kg = EnumModelType.DEFAULT;
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);
		try (final BufferedWriter wrtWalkOutput = new BufferedWriter(new FileWriter(walkOutput))) {
			// Generate walks into wanted output file
			int threadCount = 60;
			for (int depth = 1; depth < 8; ++depth) {
				new WalkGenerator(FilePaths.DATASET.getPath(kg)).generateWalks(wrtWalkOutput, 0, depth, threadCount, 0,
						9_000_000);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			getLogger().info("External Sorting/Grouping of " + walkOutput);
			// new ExternalSortByKey(" ->", 0).process(walkOutput);
			ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(walkOutput)), new File(walkOutput));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		getLogger().info("Output walks into:");
		getLogger().info(new File(walkOutput).getAbsolutePath());
		final String DELIM = Strings.EMBEDDINGS_SENTENCES_DELIM.val;
		// Now group the walked paths into the appropriate sentences
		new RDF2VecEmbeddingSentenceFormatter().groupSortedFile(walkOutput, sentencesOut, DELIM, StringDelims.WALK_DELIM, 0);

	}
}
