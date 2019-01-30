package de.dwslab.petar.walks;

import java.io.BufferedWriter;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import alu.linking.utils.IDMappingGenerator;

public class WalkResultProcessorAll extends WalkResultProcessor {
	private final IDMappingGenerator<String> predicateMapper;
	private final IDMappingGenerator<String> entityMapper;

	public WalkResultProcessorAll(IDMappingGenerator<String> entityMapper, IDMappingGenerator<String> predicateMapper) {
		this.entityMapper = entityMapper;
		this.predicateMapper = predicateMapper;
	}

	@Override
	public void processResultLines(ResultSet results, String entity, final BufferedWriter wrt,
			final boolean lineByLineOutput) {
		while (results.hasNext()) {
			final QuerySolution result = results.next();
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
		processedEntities++;

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
			// final String var = vars.get(i);
			final String var = it.next();
			try {
				// clean it if it is a literal
				final String nodeVal;
				if (var.startsWith(prefixPredicate) && predicateMapper != null) {
					// Check if mapping exists
					// If not -> create
					// Else -> grab it
					/*
					 * Note that nodeVal will now be a string-Integer (in order to minimize storage
					 * space usage for frequently occurring things, in thise case: predicates).
					 * 
					 * Reasoning on predicates: Storing all nodes in-memory for storage space
					 * reduction would potentially be REALLY heavy RAM-wise, so cutting out a large
					 * chunk of space required by very repetitive/repeatedly-used predicates makes a
					 * lot more sense since it will 'barely' impact RAM while helping out greatly
					 * storage-wise
					 */
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
