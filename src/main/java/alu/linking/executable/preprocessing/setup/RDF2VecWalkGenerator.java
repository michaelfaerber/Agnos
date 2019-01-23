package alu.linking.executable.preprocessing.setup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
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
import alu.linking.utils.IDMappingGenerator;
import alu.linking.utils.WalkUtils;
import de.dwslab.petar.walks.StringDelims;
import de.dwslab.petar.walks.WalkGenerator;

public class RDF2VecWalkGenerator implements Executable {
	private final EnumModelType kg;
	private final int threadCount;
	private final int walkDepth;
	private List<String> predicateBlacklist;

	public RDF2VecWalkGenerator(EnumModelType KG) {
		this(KG, 4);
	}

	public RDF2VecWalkGenerator(EnumModelType KG, final int walkDepth) {
		this(KG, walkDepth, 20);
	}

	public RDF2VecWalkGenerator(EnumModelType KG, int walkDepth, final int threadCount) {
		this(KG, walkDepth, threadCount,
				Arrays.asList(new String[] { "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" }));
	}

	public RDF2VecWalkGenerator(EnumModelType KG, int walkDepth, final int threadCount,
			final List<String> predicateBlacklist) {
		this.kg = KG;
		this.walkDepth = walkDepth;
		this.threadCount = threadCount;
		this.predicateBlacklist = predicateBlacklist;
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

		try (final BufferedWriter wrtWalkOutput = new BufferedWriter(new FileWriter(walkOutput));
				final IDMappingGenerator<String> predicateMapper = new IDMappingGenerator<>(
						new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_HUMAN.getPath(kg)),
						true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_MACHINE.getPath(kg)), true);
				final IDMappingGenerator<String> entityMapper = new IDMappingGenerator<>(
						new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(kg)),
						true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_MACHINE.getPath(kg)), true, "e")) {
			// Generate walks into wanted output file
			final int offset = -1, limit = -1;

			final HashSet<String> uniqueBlacklist = new HashSet<>(WalkUtils.getBlacklist(kg));
			if (this.predicateBlacklist != null)
			{
				uniqueBlacklist.addAll(this.predicateBlacklist);
			}

			final WalkGenerator wg = new WalkGenerator(FilePaths.DATASET.getPath(kg), uniqueBlacklist,
					entitiesSet, kg.query.query, predicateMapper, entityMapper, FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg));
			for (int depth = 1; depth < this.walkDepth; ++depth) {
				// Load the entities from the walk generator - assuming there is enough space
				// for all entities in RAM
				if (entitiesSet.size() == 0) {
					entitiesSet.addAll(wg.selectAllEntities(wg.dataset, wg.model, offset, limit));
				}
				// Generate the walks
				wg.generateWalks(wrtWalkOutput, 0, depth, this.threadCount, offset, limit);
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
