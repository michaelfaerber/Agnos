package alu.linking.executable.preprocessing.setup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.externalsorting.ExternalSort;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.preprocessing.embeddings.sentenceformatter.RDF2VecEmbeddingSentenceFormatter;
import alu.linking.structure.Executable;
import alu.linking.utils.WalkUtils;
import de.dwslab.petar.walks.StringDelims;
import de.dwslab.petar.walks.WalkGenerator;

public class RDF2VecWalkGenerator implements Executable {
	private final EnumModelType kg;

	public RDF2VecWalkGenerator(EnumModelType KG) {
		this.kg = KG;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);

		final Map<String, Set<String>> map;
		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(kg);
		map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(kg)));
		final Set<String> entitiesSet = new HashSet<>();
		for (Map.Entry<String, Set<String>> e : map.entrySet()) {
			entitiesSet.addAll(e.getValue());
		}
		final List<String> entities = new ArrayList<String>(entitiesSet);

		try (final BufferedWriter wrtWalkOutput = new BufferedWriter(new FileWriter(walkOutput))) {
			// Generate walks into wanted output file
			int threadCount = 40;
			for (int depth = 1; depth < 2; ++depth) {
				new WalkGenerator(FilePaths.DATASET.getPath(kg), WalkUtils.getBlacklist(kg), entities)
						.generateWalks(wrtWalkOutput, 0, depth, threadCount, 0, 9_000_000);
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
		new RDF2VecEmbeddingSentenceFormatter().groupSortedFile(walkOutput, sentencesOut, DELIM,
				StringDelims.WALK_DELIM, 0);
		return null;
	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

}
