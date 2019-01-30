package de.dwslab.petar.walks;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WalkGenerator implements AutoCloseable {
	public static final Logger log = LoggerFactory.getLogger(WalkGenerator.class);

	public String directPropsQuery = "SELECT ?b ?c WHERE {$ENTITY$ ?b ?c}";

	/**
	 * defines the number of walks from each node
	 */
//	public int numberWalks = 200;

	/**
	 * the query for extracting paths
	 */

	public String fileName = "walks.txt";

	private final Collection<String> predicateBlacklist;

	private final String entityQueryStr;

	private final String prefixSubject = "s";
	private final String prefixPredicate = "p";
	private final String prefixObject = "o";
	private final WalkResultProcessor resultProcessor;

	private final String logEntities;

	public WalkGenerator(Collection<String> predicateBlacklist, final String entityQueryStr, final String logEntities,
			final WalkResultProcessor resultProcessor) {
		this.predicateBlacklist = predicateBlacklist;
		if (entityQueryStr != null && entityQueryStr.length() > 0) {
			this.entityQueryStr = entityQueryStr;
		} else {
			this.entityQueryStr = EntityDefinitions.MAGEntityDefinition;
		}

		this.logEntities = logEntities;
		this.resultProcessor = resultProcessor;
	}

	protected abstract QueryExecution queryCreate(final String query);

	protected abstract void beginREAD();

	protected abstract void beginWRITE();

	protected abstract void endTransaction();

	protected abstract boolean isLineByLineOutput();

	@Override
	public abstract void close() throws Exception;

	public void generateWalks(String outputFile, Collection<String> entities, int nmWalks, int dpWalks, int nmThreads,
			int offset, int limit) {
		try (BufferedWriter wrt = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputFile, false), "utf-8"), 32 * 1024)) {
			generateWalks(wrt, entities, nmWalks, dpWalks, nmThreads, offset, limit);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialises threadpool and generates walks, outputting them to wrt in an
	 * asynchronous way. BufferedWriter is explicitly enforced due to the need of
	 * thread-safety.
	 * 
	 * @param wrt       BufferedWriter output writer
	 * @param nmWalks   number of walks to do
	 * @param dpWalks   depth of the walks to do
	 * @param nmThreads number of threads to set up for the threadpool
	 * @param offset    whether we need an offset in the query (useful for large
	 *                  quantities)
	 * @param limit     how many should be returned (useful for large quantities)
	 * @throws IOException
	 */
	public void generateWalks(BufferedWriter wrt, Collection<String> entitiesCollection, int nmWalks, int dpWalks,
			int nmThreads, int offset, int limit) throws IOException {

		// set the parameters
		// this.numberWalks = nmWalks;

		// Updates depth for display and probability purposes
		this.resultProcessor.updateDepth(dpWalks);
		// generate the query
		final String walkQuery = generateQuery(dpWalks);
		System.out.println("Walk query(" + dpWalks + "):");
		System.out.println(walkQuery);
		Collection<String> entities;
		if (entitiesCollection == null || entitiesCollection.size() == 0) {
			System.out.println("SELECTING all entities from repo");
			entities = selectAllEntities(offset, limit);
		} else {
			System.out.println("Using passed entities (" + entitiesCollection.size() + ")");
			entities = entitiesCollection;
		}
		this.resultProcessor.updateEntityAmt(entitiesCollection.size());
		System.out.println("Total number of entities to process: " + this.resultProcessor.getEntityAmt());
		ThreadPoolExecutor pool = new ThreadPoolExecutor(nmThreads, nmThreads, 0, TimeUnit.SECONDS,
				new java.util.concurrent.ArrayBlockingQueue<Runnable>(entities.size()));

		final BufferedWriter bwLogEntities;
		if (this.logEntities != null) {
			bwLogEntities = new BufferedWriter(new FileWriter(logEntities));
			// Output which depth we are at to keep track of iteration
			// All previous ones were executed anyway, so no need to keep track of all
			// entities, so just overwrite previous file
			bwLogEntities.write("" + dpWalks);
			bwLogEntities.newLine();

		} else {
			bwLogEntities = null;
		}

		for (String entity : entities) {
			// Thread which will compute the hops for this particular entity
			EntityThread th = new EntityThread(entity, walkQuery, wrt, bwLogEntities, isLineByLineOutput());
			pool.execute(th);
		}

		pool.shutdown();
		try {
			pool.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Selects all entities from the repo
	 * 
	 * @return
	 */
	public List<String> selectAllEntities(int offset, int limit) {
		List<String> allEntities = new ArrayList<String>();
		final String queryString = this.entityQueryStr + (offset > 0 ? " OFFSET " + offset : "")
				+ (limit >= 0 ? " LIMIT " + limit : "");
		System.out.println("Entity query: " + queryString);

		// Execute the query and obtain results
		ResultSet results;
		beginREAD();
		try (final QueryExecution qe = queryCreate(queryString)) {
			results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				allEntities.add(result.get(prefixSubject).toString());
			}
			endTransaction();
		} catch (Exception e) {
			throw e;
		}
		return allEntities;
	}

	/**
	 * generates the query with the given depth
	 * 
	 * @param depth
	 * @return
	 */
	public String generateQuery(int depth) {
		String selectPart = "SELECT ?p0 ?o0";
		String hopPart = "{ $ENTITY$ ?p0 ?o0  ";
		String query = "";
		// Relies on collection outputting correctly with commas between string items
		String blacklistedPredicatesStr = this.predicateBlacklist.toString();
		blacklistedPredicatesStr = blacklistedPredicatesStr.substring(1, blacklistedPredicatesStr.length() - 1);
		String predicateBlacklistNotIn = ((this.predicateBlacklist != null && this.predicateBlacklist.size() > 0)
				? (" NOT IN (" + blacklistedPredicatesStr + " )")
				: "");

		final String filterStart = " . FILTER(";
		final String filterEnd = ") ";
		String blacklistPart = filterStart + " ?p0 " + predicateBlacklistNotIn + filterEnd;
		for (int i = 1; i < depth; i++) {
			hopPart += ". ?o" + (i - 1) + " ?p" + i + " ?o" + i + " ";
			selectPart += " ?p" + i + " ?o" + i + " ";
			blacklistPart += filterStart + " ?p" + i + " " + predicateBlacklistNotIn + filterEnd;
		}
		final String notLiteral = "!isLiteral(?o" + (depth - 1) + ")";
		query = selectPart + " WHERE " + hopPart + filterStart + notLiteral + filterEnd
				+ ((predicateBlacklistNotIn.length() > 0) ? blacklistPart : "") + " }";
		// + " BIND(RAND() AS ?sortKey) } ORDER BY ?sortKey LIMIT "
		// + numberWalks;
		return query;
	}

	private class EntityThread implements Runnable {
		private final String entity;
		private final String walkQuery;
		private final BufferedWriter writer;
		private final BufferedWriter entityLog;
		private final boolean lineByLineOutput;

		public EntityThread(final String entity, final String walkQuery, final BufferedWriter writer,
				final BufferedWriter entityLog, final boolean lineByLineOutput) {
			this.entity = entity;
			this.walkQuery = walkQuery;
			this.writer = writer;
			this.entityLog = entityLog;
			this.lineByLineOutput = lineByLineOutput;
		}

		@Override
		public void run() {
			processEntity();
			// writeToFile(finalList, writer);
		}

		private void processEntity() {
			String queryStr = walkQuery.replace("$ENTITY$", "<" + entity + ">");
			// This part does the 'long' path query computation
			executeQuery(queryStr);
			// Note: Removed 'direct query' computation, as it can also be done with this at
			// length 1...
		}

		/**
		 * Executes specified query on the wanted model
		 * 
		 * @param queryStr
		 */
		public void executeQuery(String queryStr) {
			beginREAD();
			try (final QueryExecution qe = queryCreate(queryStr)) {
				final ResultSet resultsTmp = qe.execSelect();
				final ResultSet results = ResultSetFactory.copyResults(resultsTmp);
				endTransaction();
				resultProcessor.processResultLines(results, entity, this.writer, this.lineByLineOutput);
			}
			try {
				synchronized (this.entityLog) {
					this.entityLog.write(entity);
					this.entityLog.newLine();
				}
				this.entityLog.flush();
			} catch (IOException ioe) {
				System.err.println("Error outputting to log file.");
			}
		}
	}

}
