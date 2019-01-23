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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alu.linking.utils.IDMappingGenerator;

public class WalkGenerator {
	public static final Logger log = LoggerFactory.getLogger(WalkGenerator.class);

	public String directPropsQuery = "SELECT ?b ?c WHERE {$ENTITY$ ?b ?c}";

	/**
	 * defines the DEFAULT depth of the walk (only nodes are considered as a step)
	 */
	public int depthWalk = 7;
	/**
	 * defines the number of walks from each node
	 */
	public int numberWalks = 200;

	/**
	 * the query for extracting paths
	 */

	public long processedEntities = 0;
	public long processedWalks = 0;
	public long fileProcessedLines = 0;

	public static final String newline = System.getProperty("line.separator");

	public long startTime = System.currentTimeMillis();

	/**
	 * the rdf model
	 */
	public final Model model;

	public final Dataset dataset;

	public String fileName = "walks.txt";

	private final Collection<String> predicateBlacklist;

	private final Collection<String> entities;

	private final String entityQueryStr;

	private final String prefixSubject = "s";
	private final String prefixPredicate = "p";
	private final String prefixObject = "o";
	private final IDMappingGenerator<String> predicateMapper;
	private final IDMappingGenerator<String> entityMapper;

	private long entityAmt = -1;

	private final String logEntities;

	public WalkGenerator(final String repoLocation, List<String> predicateBlacklist, List<String> entities,
			final String entityQueryStr) {
		this(repoLocation, predicateBlacklist, entities, entityQueryStr, null, null, null);
	}

	public WalkGenerator(final String repoLocation, Collection<String> predicateBlacklist, Collection<String> entities,
			final String entityQueryStr, final IDMappingGenerator<String> predicateMapper,
			final IDMappingGenerator<String> entityMapper, final String logEntities) {
		this.dataset = TDBFactory.createDataset(repoLocation);
		this.model = dataset.getDefaultModel();
		this.predicateBlacklist = predicateBlacklist;
		this.entities = entities;
		if (entityQueryStr != null && entityQueryStr.length() > 0) {
			this.entityQueryStr = entityQueryStr;
		} else {
			this.entityQueryStr = EntityDefinitions.MAGEntityDefinition;
		}
		this.predicateMapper = predicateMapper;
		this.entityMapper = entityMapper;

		this.logEntities = logEntities;
	}

	public void generateWalks(String outputFile, int nmWalks, int dpWalks, int nmThreads, int offset, int limit) {
		try (BufferedWriter wrt = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputFile, false), "utf-8"), 32 * 1024)) {
			generateWalks(wrt, nmWalks, dpWalks, nmThreads, offset, limit);
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
	public void generateWalks(BufferedWriter wrt, int nmWalks, int dpWalks, int nmThreads, int offset, int limit)
			throws IOException {

		// set the parameters
		this.numberWalks = nmWalks;

		this.depthWalk = dpWalks;

		// generate the query
		final String walkQuery = generateQuery(depthWalk);
		System.out.println("Walk query(" + depthWalk + "):");
		System.out.println(walkQuery);
		final Collection<String> entities;
		if (this.entities == null || this.entities.size() == 0) {
			System.out.println("SELECTING all entities from repo");
			entities = selectAllEntities(dataset, model, offset, limit);
		} else {
			System.out.println("Using passed entities (" + this.entities.size() + ")");
			entities = this.entities;
		}
		this.entityAmt = entities.size();
		System.out.println("Total number of entities to process: " + entityAmt);
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
			EntityThread th = new EntityThread(entity, walkQuery, wrt, bwLogEntities);
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
	public List<String> selectAllEntities(Dataset dataset, Model model, int offset, int limit) {
		List<String> allEntities = new ArrayList<String>();
		final String queryString = this.entityQueryStr + (offset >= 0 ? " OFFSET " + offset : "")
				+ (limit >= 0 ? " LIMIT " + limit : "");
		System.out.println("Entity query: " + queryString);
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		dataset.begin(ReadWrite.READ);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		dataset.end();

		while (results.hasNext()) {
			QuerySolution result = results.next();
			allEntities.add(result.get(prefixSubject).toString());
		}
		qe.close();
		return allEntities;
	}

	/**
	 * Adds new walks to the list; If the list is filled it is written to the file
	 * 
	 * @param tmpList
	 */
	public synchronized void writeToFile(String str, final BufferedWriter wrt) {
		processedWalks += 1;
		fileProcessedLines += 1;
		try {
			wrt.write(str.replace(newline, " ").replace("\n", " ").replace("\r", " ").replace("  ", " ") + newline);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (processedWalks % 1_000_000 == 0) {
			System.out.println("TOTAL NUMBER OF PATHS(" + this.depthWalk + " =?= "
					+ ((this.processedEntities / this.entityAmt)+1) + " ; " + (this.processedEntities % this.entityAmt)
					+ " / " + this.entityAmt + ") : " + processedWalks);
			System.out.println("TOTAL TIME:" + ((System.currentTimeMillis() - startTime) / 1000));
			// flush the file
			if (fileProcessedLines > 3_000_000) {
				fileProcessedLines = 0;
				try {
					wrt.flush();
					// writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Adds new walks to the list; If the list is filled it is written to the file
	 * 
	 * @param tmpList
	 */
	public synchronized void writeToFile(List<String> tmpList, final BufferedWriter wrt) {
		processedEntities++;
		processedWalks += tmpList.size();
		fileProcessedLines += tmpList.size();
		for (String str : tmpList)
			try {
				wrt.write(str + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (processedEntities % 1_000 == 0) {
			System.out.println("TOTAL PROCESSED ENTITIES: " + processedEntities);
			System.out.println("TOTAL NUMBER OF PATHS : " + processedWalks);
			System.out.println("TOTAL TIME:" + ((System.currentTimeMillis() - startTime) / 1000));
		}
		// flush the file
		if (fileProcessedLines > 3_000_000) {
			fileProcessedLines = 0;
			try {
				wrt.flush();
				// writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			int tmpNM = (processedWalks / 3000000);
//			String tmpFilename = fileName.replace(".txt", tmpNM + ".txt");
//			try {
//				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFilename, false), "utf-8"),
//						32 * 1024);
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
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
		System.out.println("DEBUG - S");
		System.out.println(this.predicateBlacklist);
		System.out.println(blacklistedPredicatesStr);
		System.out.println(predicateBlacklistNotIn);
		System.out.println(blacklistPart);
		System.out.println("DEBUG - E");
		// + " BIND(RAND() AS ?sortKey) } ORDER BY ?sortKey LIMIT "
		// + numberWalks;
		return query;
	}

	private class EntityThread implements Runnable {
		private final String entity;
		private final String walkQuery;
		private final BufferedWriter writer;
		private final BufferedWriter entityLog;

		public EntityThread(final String entity, final String walkQuery, final BufferedWriter writer,
				final BufferedWriter entityLog) {
			this.entity = entity;
			this.walkQuery = walkQuery;
			this.writer = writer;
			this.entityLog = entityLog;
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
			final Query query = QueryFactory.create(queryStr);
			dataset.begin(ReadWrite.READ);
			final QueryExecution qe = QueryExecutionFactory.create(query, model);
			final ResultSet resultsTmp = qe.execSelect();
			final ResultSet results = ResultSetFactory.copyResults(resultsTmp);
			qe.close();
			dataset.end();
			while (results.hasNext()) {
				final QuerySolution result = results.next();
				String singleWalk;
				if (entityMapper != null) {
					try {
						singleWalk = entityMapper.generateMapping(entity);
					} catch (Exception e) {
						// Do the same as 'else' clause to be consistent
						System.err.println("Mapping Error for entity (" + entity + ")");
						singleWalk = entity;
					}
				} else {
					singleWalk = entity;
				}
				// construct the walk from each node or property on the path
				// Need to make sure that the iteration order is preserved...
				// (QuerySolutionIterator sorts variables alphabetically)
				final QuerySolutionIterator it = new QuerySolutionIterator(result);
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
							// Note that nodeVal will now be a string-Integer (in order to minimize storage
							// space usage for frequently occurring things, in thise case: predicates)
							// Reasoning on predicates: Storing all nodes in-memory for storage space
							// reduction would potentially be REALLY heavy RAM-wise, so cutting out a large
							// chunk of space required by very repetitive/repeatedly-used predicates makes a
							// lot more sense since it will 'barely' impact RAM while helping out greatly
							// storage-wise
							nodeVal = predicateMapper.generateMapping(result.get(var).toString()).toString();
						} else {
							nodeVal = result.get(var).toString();
						}
						singleWalk += StringDelims.WALK_DELIM + nodeVal;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// writeToFile is synchronized, but watch out not to use this.writer anywhere
				// else
				// Outputs every line one by one rather than accumulating
				writeToFile(singleWalk, this.writer);
			}
			processedEntities++;
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

	public static void main(String[] args) {
		System.out.println("USAGE:  repoLocation outputFile nmWalks dpWalks nmThreads");
		WalkGenerator generator = new WalkGenerator(args[0], null, null, EntityDefinitions.originalEntityDefinition);
		generator.generateWalks(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
				Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]));
	}
}
