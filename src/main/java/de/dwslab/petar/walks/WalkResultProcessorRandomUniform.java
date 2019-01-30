package de.dwslab.petar.walks;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import alu.linking.utils.IDMappingGenerator;

public class WalkResultProcessorRandomUniform extends WalkResultProcessor {
	protected IDMappingGenerator<String> entityMapper = null;
	protected IDMappingGenerator<String> predicateMapper = null;
	protected final Random rand;
	protected double probabilityRatio = 1.0;
	// Maximum upper bound for random value, if it is above, it is rejected/ignored
	// maxThreshold == max means everything goes through
	protected int maxThreshold = 100;
	// Max value of input probability, at 100 by default as we assume probabilities
	// to be ratios
	protected final int max = 100;
	protected int minWalks;

	public WalkResultProcessorRandomUniform(long seed) {
		this.rand = new Random(seed);
	}

	public WalkResultProcessorRandomUniform() {
		this.rand = new Random(System.currentTimeMillis());
	}

	public WalkResultProcessorRandomUniform(final double probabilityAdded, IDMappingGenerator<String> entityMapper,
			IDMappingGenerator<String> predicateMapper, final int minWalks, final long seed) {
		this(seed);
		entityMapper(entityMapper);
		predicateMapper(predicateMapper);
		minWalks(minWalks);
	}

	public WalkResultProcessorRandomUniform probability(final float probability) {
		this.probabilityRatio = probability;
		this.maxThreshold = (int) (probabilityRatio * ((double) max));
		return this;
	}

	public WalkResultProcessorRandomUniform entityMapper(final IDMappingGenerator<String> entityMapper) {
		this.entityMapper = entityMapper;
		return this;
	}

	public WalkResultProcessorRandomUniform predicateMapper(final IDMappingGenerator<String> predicateMapper) {
		this.predicateMapper = predicateMapper;
		return this;
	}

	public WalkResultProcessorRandomUniform minWalks(final int minWalks) {
		this.minWalks = minWalks;
		return this;
	}

	@Override
	public void processResultLines(ResultSet results, String entity, BufferedWriter wrt, boolean lineByLineOutput) {
		final List<QuerySolution> minKeeperList = new ArrayList<QuerySolution>(this.minWalks);
		long added = 0;
		while (results.hasNext()) {
			final QuerySolution result = results.next();
			// Check if Fortuna / randomness allows us to get this triple
			if (rand.nextInt(max) > maxThreshold) {
				// Keep backup of this walk just in case there aren't enough in total to
				// accommodate the min factor
				if (added < this.minWalks) {
					minKeeperList.add(result);
				} else if (minKeeperList.size() > 0) {
					// Clear it since it has at least one item and is no longer necessary
					minKeeperList.clear();
				}
				// Ignore if not within given %
				continue;
			}
			// One more walk is output
			added++;
			makeStringWalk(result, entity, wrt, lineByLineOutput);
		}
		final Random randomizer = new Random(System.currentTimeMillis());

		// Compute the 'missing' ones
		for (long counter = added; counter < this.minWalks && minKeeperList.size() > 0; counter++) {
			final int index = randomizer.nextInt(minKeeperList.size());
			final QuerySolution sol = minKeeperList.get(index);
			minKeeperList.remove(index);
			makeStringWalk(sol, entity, wrt, lineByLineOutput);
		}
		processedEntities++;
	}

	/**
	 * Helper function for outputting, so it is easy to add 'missing' lines to the
	 * output
	 * 
	 * @param result
	 * @param entity
	 * @param wrt
	 * @param lineByLineOutput
	 */
	private void makeStringWalk(QuerySolution result, String entity, BufferedWriter wrt, boolean lineByLineOutput) {
		StringBuilder singleWalk;
		if (entityMapper != null) {
			try {
				singleWalk = new StringBuilder(entityMapper.generateMapping(entity));
			} catch (Exception e) {
				// Do the same as 'else' clause to be consistent
				System.err.println("Mapping Error for entity (" + entity + ")");
				singleWalk = new StringBuilder(entity);
			}
		} else {
			singleWalk = new StringBuilder(entity);
		}
		// construct the walk from each node or property on the path
		// Need to make sure that the iteration order is preserved...
		// (QuerySolutionIterator sorts variables alphabetically)
		processColumns(singleWalk, result);
		// writeToFile is synchronized, but watch out not to use this.writer anywhere
		// else
		// Outputs every line one by one rather than accumulating multiple
		writeToFile(singleWalk.toString(), wrt, lineByLineOutput);

	}

	/**
	 * Processes the 'columns' of returned results, aka. processes a query solution
	 * (single line)
	 * 
	 * @param singleWalk
	 * @param solution
	 * @return
	 */
	@Override
	public StringBuilder processColumns(StringBuilder walk, QuerySolution solution) {
		final QuerySolutionIterator it = new QuerySolutionIterator(solution);
		while (it.hasNext()) {
			final String var = it.next();
			try {
				final String nodeVal;
				if (var.startsWith(prefixPredicate) && predicateMapper != null) {
					nodeVal = predicateMapper.generateMapping(solution.get(var).toString()).toString();
				} else {
					nodeVal = solution.get(var).toString();
				}
				walk.append(StringDelims.WALK_DELIM + nodeVal);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return walk;
	}

}
