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
import alu.linking.utils.IDMappingLoader;
import alu.linking.utils.IterableEntity;
import alu.linking.utils.IterableFileEntity;
import alu.linking.utils.WalkUtils;
import de.dwslab.petar.walks.WalkGenerator;
import de.dwslab.petar.walks.WalkGeneratorJena;
import de.dwslab.petar.walks.WalkGeneratorVirtuoso;
import de.dwslab.petar.walks.WalkResultProcessor;
import de.dwslab.petar.walks.WalkResultProcessorAll;
import de.dwslab.petar.walks.WalkResultprocessorRandomDecreasingDepth;
import virtuoso.jena.driver.VirtGraph;

/**
 * Class handling graph walk generation in an RDF2Vec-fashion with some caveats
 * (extensions), outputting graph walks to the appropriate place for it to be
 * picked up by the python code for embedding computations
 * 
 * @author Kristian Noullet
 *
 */
public class RDF2VecWalkGenerator implements Executable {
	private final EnumModelType kg;
	private final int threadCount;
	private final int maxWalkDepth;
	private final int minWalkDepth;
	private List<String> predicateBlacklist;
	private static final int DEFAULT_MIN_WALK_DEPTH = 1;
	private final boolean loadPredicateMapper;

	public RDF2VecWalkGenerator(EnumModelType KG) {
		this(KG, 4);
	}

	public RDF2VecWalkGenerator(EnumModelType KG, final int walkDepth) {
		this(KG, walkDepth, DEFAULT_MIN_WALK_DEPTH, 20);
	}

	public RDF2VecWalkGenerator(EnumModelType KG, int walkDepthMin, int walkDepthMax, final int threadCount) {
		this(KG, walkDepthMin, walkDepthMax, threadCount,
				Arrays.asList(new String[] { "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" }), false);
	}

	public RDF2VecWalkGenerator(final EnumModelType KG, final int walkDepthMin, final int walkDepthMax,
			final int threadCount, final List<String> predicateBlacklist, final boolean loadPredicateMapper) {
		this.kg = KG;
		this.maxWalkDepth = walkDepthMax;
		this.minWalkDepth = walkDepthMin;
		this.threadCount = threadCount;
		this.predicateBlacklist = predicateBlacklist;
		this.loadPredicateMapper = loadPredicateMapper;
	}

	enum LIMITING_FACTOR {
		RAM, HDD
	};

	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		final LIMITING_FACTOR limit = LIMITING_FACTOR.RAM;
		switch (limit) {
		case HDD:
			hdd_friendly_walks();
			break;
		case RAM:
			ram_friendly_walks();
			break;
		default:
			break;
		}

		// new ExtSortGrouper().exec(walkOutput, sentencesOut);
		return null;
	}

	private void ram_friendly_walks() throws Exception {
		final float[] distribution = new float[] { 1.0f, 0.1f, 0.05f, 0.02f// , 0.01f
		};
		// Just output the walks to a different spot than the rest
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(this.kg
		// EnumModelType.NONE
		);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);

		final boolean entitiesqueryVsFile = true;

		try (final IterableEntity iterableEntities = entitiesqueryVsFile ?
		// null, so it has to query it
				null :
				// else load from entities.nt file with NxParser
				new IterableFileEntity(new File(FilePaths.FILE_NT_ENTITIES.getPath(kg)), true)) {

			// Load the blacklist for predicates (e.g. rdf:Type or such)
			final int offset = -1, limit = -1;
			final HashSet<String> uniqueBlacklist = new HashSet<>(WalkUtils.getBlacklist(kg));
			if (this.predicateBlacklist != null) {
				uniqueBlacklist.addAll(this.predicateBlacklist);
			}
			getLogger().info("Blacklist(" + uniqueBlacklist.size() + "): " + uniqueBlacklist);

			// Generate walks into wanted output file
			final WalkGenerator wg;
			// Take 'random' ones
			final boolean ALL_VS_RANDOM = false;
			final WalkResultProcessor resultProcessor;
			final String prefix = "";
			final File idMappingFileHuman = new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_HUMAN.getPath(kg));
			final File idMappingFileRaw = new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_MACHINE.getPath(kg));
			try (final IDMappingGenerator<String> predicateMapper = this.loadPredicateMapper
					? new IDMappingLoader<String>().loadHumanFile(idMappingFileHuman)
							.createGenerator(idMappingFileHuman, true ? null : idMappingFileRaw, prefix)
					: new IDMappingGenerator<>(idMappingFileHuman, true ? null : idMappingFileRaw, true)) {

				if (ALL_VS_RANDOM) {
					resultProcessor = new WalkResultProcessorAll(null, predicateMapper);
				} else {
					resultProcessor = new WalkResultprocessorRandomDecreasingDepth(distribution).entityMapping(null)
							.predicateMapper(predicateMapper).minWalks(50).maxWalks(1_000);
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
							FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg), resultProcessor,
							FilePaths.FILE_TXT_ENTITIES.getPath(kg));
				} else {
					// Use Jena for graph walks
					getLogger().info("Executing graph walks through [JENA]");
					wg = new WalkGeneratorJena(FilePaths.DATASET.getPath(kg), uniqueBlacklist, kg.query.query,
							FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg), resultProcessor,
							FilePaths.FILE_TXT_ENTITIES.getPath(kg));
				}

				executeWalks(wg, walkOutput, iterableEntities, offset, limit);
				// Uncomment if you want to use chunking of output files for the walks
				// executeWalks_chunking(wg, walkOutput, uniqueEntities, limit, offset);
				wg.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void hdd_friendly_walks() throws Exception {
		final float[] distribution = new float[] { 1.0f, 0.1f, 0.05f, 0.01f };
		final boolean ITERABLE_VS_SFLINKS = true;
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);

		// Load the entities to do graph walks for (via mention possibilities)
		final Set<String> allEntities = new HashSet<>();
		final File fileMappingOutputHuman = new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(kg));
		try (final IDMappingGenerator<String> entityMapper = new IDMappingGenerator<>(//
				fileMappingOutputHuman, //
				true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_MACHINE.getPath(kg)), true, //
				"e")) {
			if (ITERABLE_VS_SFLINKS) {
				try (final IterableEntity iterableEntities = new IterableFileEntity(
						new File(FilePaths.FILE_NT_ENTITIES.getPath(kg)), true)) {
					for (String entity : iterableEntities) {
						// Generate mapping for this given entity (if it doesn't exist yet)
						entityMapper.generateMapping(entity);
						// And add the entity to a collection of entities
						allEntities.add(entity);
					}
				}
			} else {
				final Map<String, Collection<String>> map;
				final MentionPossibilityLoader mpl = new MentionPossibilityLoader(kg, new StopwordsLoader(kg));
				map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(kg)));
				for (Map.Entry<String, Collection<String>> e : map.entrySet()) {
					for (String entity : e.getValue()) {
						// Generate mapping for this given entity (if it doesn't exist yet)
						entityMapper.generateMapping(entity);
						// And add the entity to a collection of entities
						allEntities.add(entity);
					}
				}

			}
		}
		// Now that the mappings have been written to a file, we can load them from the
		// same one (to make sure they make sense / work properly)
		final IDMappingLoader<String> entityMapping = new IDMappingLoader<String>();
		entityMapping.loadHumanFile(fileMappingOutputHuman);

		// Load the blacklist for predicates (e.g. rdf:Type or such)
		final int offset = -1, limit = -1;
		final HashSet<String> uniqueBlacklist = new HashSet<>(WalkUtils.getBlacklist(kg));
		if (this.predicateBlacklist != null) {
			uniqueBlacklist.addAll(this.predicateBlacklist);
		}
		getLogger().info("Blacklist(" + uniqueBlacklist.size() + "): " + uniqueBlacklist);

		try (final IDMappingGenerator<String> predicateMapper = new IDMappingGenerator<>(
				new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_HUMAN.getPath(kg)),
				true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_MACHINE.getPath(kg)), true)) {
			// Generate walks into wanted output file
			final WalkGenerator wg;
			// Take 'random' ones
			final boolean ALL_VS_RANDOM = false;
			final WalkResultProcessor resultProcessor;
			if (ALL_VS_RANDOM) {
				resultProcessor = new WalkResultProcessorAll(entityMapping, predicateMapper);
			} else {
				resultProcessor = new WalkResultprocessorRandomDecreasingDepth(distribution)
						.entityMapping(entityMapping).predicateMapper(predicateMapper).minWalks(20).maxWalks(1_000);
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
						FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg), resultProcessor,
						FilePaths.FILE_TXT_ENTITIES.getPath(kg));
			} else {
				// Use Jena for graph walks
				getLogger().info("Executing graph walks through [JENA]");
				wg = new WalkGeneratorJena(FilePaths.DATASET.getPath(kg), uniqueBlacklist, kg.query.query,
						FilePaths.FILE_GRAPH_WALK_LOG_ENTITY.getPath(kg), resultProcessor,
						FilePaths.FILE_TXT_ENTITIES.getPath(kg));
			}
			// Load the entities from the walk generator - assuming there is enough space
			// for all entities in RAM
			if (allEntities.size() == 0) {
				System.out.println("Selecting all entities prior to (outside of) random walk generation.");
				allEntities.addAll(wg.selectAllEntities(offset, limit));
				// Now creating appropriate mappings for them into the files
				try (final IDMappingGenerator<String> entityMapper = new IDMappingGenerator<>(//
						fileMappingOutputHuman, //
						true ? null : new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_MACHINE.getPath(kg)), true, //
						"e")) {
					for (String entity : allEntities) {
						// Generate mapping for this given entity (if it doesn't exist yet)
						entityMapper.generateMapping(entity);
					}
				}
				// Then loading it into the already-fed mapping
				entityMapping.loadHumanFile(fileMappingOutputHuman);

				// For testing purposes just adding one entity
				// allEntities.add("http://ma-graph.org/entity/2798428352");
				System.out.println("Number of returned entities: " + allEntities.size());
			}

			final List<String> uniqueEntities = new ArrayList<>(allEntities);
			allEntities.clear();
			// Execute entities in parts of 100 entities at a time

			executeWalks(wg, walkOutput, uniqueEntities, offset, limit);
			// Uncomment if you want to use chunking of output files for the walks
			// executeWalks_chunking(wg, walkOutput, uniqueEntities, limit, offset);
			wg.close();

		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void executeWalks(WalkGenerator wg, final String walkOutput, final Iterable<String> uniqueEntities,
			final int offset, final int limit) throws IOException, InterruptedException {
		for (int depth = Math.max(minWalkDepth, 1); depth <= this.maxWalkDepth; ++depth) {
			try (final BufferedWriter wrtWalkOutput = new BufferedWriter(new FileWriter(walkOutput + "_" + depth),
					1_024 * 1_024 * 1_000)) {
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
	 * @throws InterruptedException
	 */
	private void executeWalks_chunking(final WalkGenerator wg, final String walkOutput,
			final List<String> uniqueEntities, final int offset, final int limit)
			throws IOException, InterruptedException {
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
