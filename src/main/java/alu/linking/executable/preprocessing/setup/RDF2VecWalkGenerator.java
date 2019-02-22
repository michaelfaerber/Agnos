package alu.linking.executable.preprocessing.setup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import alu.linking.config.constants.EnumConnection;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.structure.Executable;
import alu.linking.utils.IDMappingGenerator;
import alu.linking.utils.WalkUtils;
import de.dwslab.petar.walks.WalkGenerator;
import de.dwslab.petar.walks.WalkGeneratorJena;
import de.dwslab.petar.walks.WalkGeneratorVirtuoso;
import de.dwslab.petar.walks.WalkResultProcessor;
import de.dwslab.petar.walks.WalkResultProcessorAll;
import de.dwslab.petar.walks.WalkResultprocessorRandomDecreasingDepth;
import virtuoso.jena.driver.VirtGraph;

public class RDF2VecWalkGenerator implements Executable {
	private final EnumModelType kg;
	private final int threadCount;
	private final int maxWalkDepth;
	private final int minWalkDepth;
	private List<String> predicateBlacklist;
	private static final int DEFAULT_MIN_WALK_DEPTH = 1;

	public RDF2VecWalkGenerator(EnumModelType KG) {
		this(KG, 4);
	}

	public RDF2VecWalkGenerator(EnumModelType KG, final int walkDepth) {
		this(KG, walkDepth, DEFAULT_MIN_WALK_DEPTH, 20);
	}

	public RDF2VecWalkGenerator(EnumModelType KG, int walkDepthMin, int walkDepthMax, final int threadCount) {
		this(KG, walkDepthMin, walkDepthMax, threadCount,
				Arrays.asList(new String[] { "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" }));
	}

	public RDF2VecWalkGenerator(final EnumModelType KG, final int walkDepthMin, final int walkDepthMax,
			final int threadCount, final List<String> predicateBlacklist) {
		this.kg = KG;
		this.maxWalkDepth = walkDepthMax;
		this.minWalkDepth = walkDepthMin;
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
		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(kg, new StopwordsLoader(kg));
		map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(kg)));
		final Set<String> allEntities = new HashSet<>();
		for (Map.Entry<String, Set<String>> e : map.entrySet()) {
			allEntities.addAll(e.getValue());
		}

		final int offset = -1, limit = -1;
		final HashSet<String> uniqueBlacklist = new HashSet<>(WalkUtils.getBlacklist(kg));
		if (this.predicateBlacklist != null) {
			uniqueBlacklist.addAll(this.predicateBlacklist);
		}
		System.out.println("Blacklist(" + uniqueBlacklist.size() + "): " + uniqueBlacklist);

		try (final IDMappingGenerator<String> predicateMapper = new IDMappingGenerator<>(
				new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_HUMAN.getPath(kg)),
				true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_MACHINE.getPath(kg)), true);
				final IDMappingGenerator<String> entityMapper = new IDMappingGenerator<>(//
						new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(kg)), //
						true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_MACHINE.getPath(kg)), true, //
						"e")//
		) {
			// Generate walks into wanted output file
			final WalkGenerator wg;
			final boolean ALL_VS_RANDOM = false;
			final WalkResultProcessor resultProcessor;
			if (ALL_VS_RANDOM) {
				resultProcessor = new WalkResultProcessorAll(entityMapper, predicateMapper);
			} else {
				resultProcessor = new WalkResultprocessorRandomDecreasingDepth(new float[] { 1.0f, 0.1f, 0.05f, 0.01f })
						.entityMapper(entityMapper).predicateMapper(predicateMapper).minWalks(20).maxWalks(1_000);
			}

			// Whether to use Jena or Virtuoso is defined in the EnumModelType
			if (kg.useVirtuoso()) {
				getLogger().info("Executing graph walks through [VIRTUOSO]");
				// Use virtuoso for graph walks
				final EnumConnection connVirtuoso = kg.virtuosoConn;
				final VirtGraph virtGraph = new VirtGraph(kg.virtuosoGraphname, connVirtuoso.baseURL,
						new String(connVirtuoso.userAcc.getBytesUsername()),
						new String(connVirtuoso.userAcc.getBytesPassword()));

				wg = new WalkGeneratorVirtuoso(virtGraph, uniqueBlacklist, kg.query.query,
						FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg), resultProcessor);
			} else {
				// Use Jena for graph walks
				getLogger().info("Executing graph walks through [JENA]");
				wg = new WalkGeneratorJena(FilePaths.DATASET.getPath(kg), uniqueBlacklist, kg.query.query,
						FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg), resultProcessor);
			}
			// Load the entities from the walk generator - assuming there is enough space
			// for all entities in RAM
			if (allEntities.size() == 0) {
				System.out.println("Selecting all entities prior to (outside of) random walk generation.");
				allEntities.addAll(wg.selectAllEntities(offset, limit));
				System.out.println("Number of returned entities: " + allEntities.size());
			}

			final List<String> uniqueEntities = new ArrayList<>(allEntities);
			allEntities.clear();
			// Execute entities in parts of 100 entities at a time

			executeWalks(wg, walkOutput, uniqueEntities, offset, limit);
			// Uncomment if you want to use chunking of output files for the walks
			// executeWalks_chunking(wg, walkOutput, uniqueEntities, limit, offset);

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// new ExtSortGrouper().exec(walkOutput, sentencesOut);
		return null;
	}

	private void executeWalks(WalkGenerator wg, final String walkOutput, final Collection<String> uniqueEntities,
			final int offset, final int limit) throws IOException {
		try (final BufferedWriter wrtWalkOutput = new BufferedWriter(new FileWriter(walkOutput), 8192 * 20)) {
			for (int depth = Math.max(minWalkDepth, 1); depth <= this.maxWalkDepth; ++depth) {
				getLogger().info("Doing depth(" + depth + ")");
				// Chunk it into separate files
				// Generate the walks
				wg.generateWalks(wrtWalkOutput, uniqueEntities, 0, depth, this.threadCount, offset, limit);
			}
		}
	}

	/**
	 * WARNING: This method is NOT finished. Use at own risk.<br>
	 * Chunks the entities we want to compute for into smaller bits (in case of low
	 * storage space interrupt, we then would more easily be able to see how
	 * many/which entities were done and which weren't)
	 * 
	 * @param wg
	 * @param chunking
	 * @param walkOutput
	 * @param uniqueEntities
	 * @param offset
	 * @param limit
	 * @throws IOException
	 */
	private void executeWalks_chunking(final WalkGenerator wg, final String walkOutput,
			final List<String> uniqueEntities, final int offset, final int limit) throws IOException {
		final int[] chunking = new int[] { 100, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 10_000, 100_000, 100_000//
		};
		int entityCounter = 0;
		// Walks in chunks of entities
		System.out.println("Computing chunks...");
		for (int chunkIndex = 0; (chunkIndex < chunking.length)
				&& (entityCounter < uniqueEntities.size()); chunkIndex++) {
			// Chunk is either the defined size or less (if we hit the end)
			// Compute the wanted chunk of entities!
			final List<String> entitiesChunk = uniqueEntities.subList(entityCounter,
					Math.min(entityCounter + chunking[chunkIndex], uniqueEntities.size()));
			// Change the file name based on which chunk it is
			try (final BufferedWriter wrtWalkOutput = new BufferedWriter(
					new FileWriter(walkOutput + "_" + chunkIndex + "_" + entitiesChunk.size() + ".txt"))) {
				System.out.println("Starting chunk #" + chunkIndex + " with entities (" + entitiesChunk.size() + ")");
				for (int depth = 1; depth <= this.maxWalkDepth; ++depth) {
					// Chunk it into separate files
					// Generate the walks
					wg.generateWalks(wrtWalkOutput, entitiesChunk, 0, depth, this.threadCount, offset, limit);
				}
			}
			entityCounter += entitiesChunk.size();
		}
		// Do for the rest if there is anything left
		final int missingEntitiesCount = uniqueEntities.size() - entityCounter;
		if (missingEntitiesCount > 0) {
			Set<String> missingEntities = new HashSet<>();
			// Missing some more entities, so let's compute them in the end
			for (int missingIndex = entityCounter; missingIndex < uniqueEntities.size(); ++missingIndex) {
				missingEntities.add(uniqueEntities.get(missingIndex));
			}
			final String missingWalkOutput = walkOutput + "_rest_" + missingEntitiesCount + ".txt";
			System.out.println("Missing " + missingEntitiesCount + " entities.");
			System.out.println("Outputting missing entities into:");
			System.out.println(missingWalkOutput);
			try (final BufferedWriter wrtWalkOutput = new BufferedWriter(new FileWriter(missingWalkOutput))) {
				for (int depth = 1; depth <= this.maxWalkDepth; ++depth) {
					// Generate walks for the entities that weren't computed yet
					wg.generateWalks(wrtWalkOutput, missingEntities, 0, depth, this.threadCount, offset, limit);
				}
			}
		}

	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

}
