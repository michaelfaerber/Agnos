package de.dwslab.petar.walks;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

public class WalkGenerator {
	public static final Logger log = LoggerFactory.getLogger(WalkGenerator.class);

	public static String directPropsQuery = "SELECT ?b ?c WHERE {$ENTITY$ ?b ?c}";

	/**
	 * defines the depth of the walk (only nodes are considered as a step)
	 */
	public int depthWalk = 7;
	/**
	 * defines the number of walks from each node
	 */
	public int numberWalks = 200;

	/**
	 * the query for extracting paths
	 */
	public String walkQuery = "";

	public int processedEntities = 0;
	public static int processedWalks = 0;
	public static int fileProcessedLines = 0;

	public static long startTime = System.currentTimeMillis();

	/**
	 * the rdf model
	 */
	public final Model model;

	public final Dataset dataset;

	public String fileName = "walks.txt";

	public WalkGenerator(final String repoLocation) {
		this.dataset = TDBFactory.createDataset(repoLocation);
		this.model = dataset.getDefaultModel();
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
	 */
	public void generateWalks(BufferedWriter wrt, int nmWalks, int dpWalks, int nmThreads, int offset, int limit) {

		// set the parameters
		this.numberWalks = nmWalks;
		this.depthWalk = dpWalks;
		final boolean computeDirectConnections = false;

		// generate the query
		this.walkQuery = generateQuery(depthWalk, numberWalks);
		System.out.println("SELECTING all entities from repo");
		final List<String> entities = selectAllEntities(dataset, model, offset, limit);

		System.out.println("Total number of entities to process: " + entities.size());
		ThreadPoolExecutor pool = new ThreadPoolExecutor(nmThreads, nmThreads, 0, TimeUnit.SECONDS,
				new java.util.concurrent.ArrayBlockingQueue<Runnable>(entities.size()));

		for (String entity : entities) {
			// Thread which will compute the hops for this particular entity
			EntityThread th = new EntityThread(entity, walkQuery, wrt, computeDirectConnections);
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

		final String queryString = EntityDefinitions.MAGEntityDefinition + " OFFSET " + offset + " LIMIT " + limit;

		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		dataset.begin(ReadWrite.READ);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		dataset.end();

		while (results.hasNext()) {
			QuerySolution result = results.next();
			allEntities.add(result.get("s").toString());
		}
		qe.close();
		return allEntities;
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
	public static String generateQuery(int depth, int numberWalks) {
		String selectPart = "SELECT ?p0 ?o1";
		String mainPart = "{ $ENTITY$ ?p0 ?o1  ";
		String query = "";
		for (int i = 1; i < depth; i++) {
			mainPart += ". ?o" + i + " ?p" + i + "?o" + (i + 1) + " ";
			selectPart += " ?p" + i + "?o" + (i + 1) + " ";
		}
		query = selectPart + " WHERE " + mainPart + "}";
		// + " BIND(RAND() AS ?sortKey) } ORDER BY ?sortKey LIMIT "
		// + numberWalks;
		return query;
	}

	private class EntityThread implements Runnable {
		private final String entity;
		private final List<String> finalList = new ArrayList<String>();
		private final boolean computeDirectConnections;
		private final String walkQuery;
		private final BufferedWriter writer;

		public EntityThread(final String entity, final String walkQuery, final BufferedWriter writer,
				final boolean computeDirectConnections) {
			this.entity = entity;
			this.computeDirectConnections = computeDirectConnections;
			this.walkQuery = walkQuery;
			this.writer = writer;
		}

		@Override
		public void run() {
			processEntity(this.computeDirectConnections);
			writeToFile(finalList, writer);

		}

		private void processEntity(final boolean directConnections) {
			// get all the walks
			List<String> tmpList = new ArrayList<String>();
			String queryStr = walkQuery.replace("$ENTITY$", "<" + entity + ">");
			// TMPLIST - START
			// This part does the long path query computation
			executeQuery(queryStr, tmpList);
			final Random rand = new Random();
			if (numberWalks > 0) {
				for (int i = 0; i < numberWalks; i++) {
					if (tmpList.size() < 1)
						break;
					int randomNum = rand.nextInt(tmpList.size());
					if (randomNum > tmpList.size() - 1)
						randomNum = tmpList.size() - 1;
					finalList.add(tmpList.get(randomNum));
					tmpList.remove(randomNum);
				}
			} else {
				finalList.addAll(tmpList);
			}
			// TMPLIST - END

			// Whether to add direct connections as well
			if (directConnections) {
				// This adds all the direct connections additionally to the longer ones
				queryStr = directPropsQuery.replace("$ENTITY$", "<" + entity + ">");
				executeQuery(queryStr, finalList);
			}
		}

		public void executeQuery(String queryStr, List<String> walkList) {
			Query query = QueryFactory.create(queryStr);
			dataset.begin(ReadWrite.READ);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			ResultSet resultsTmp = qe.execSelect();
			String entityShort = entity.replace("http://dbpedia.org/resource/", "dbr:");
			ResultSet results = ResultSetFactory.copyResults(resultsTmp);
			qe.close();
			dataset.end();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				String singleWalk = entityShort;
				// construct the walk from each node or property on the path
				// List<String> vars = results.getResultVars();
				final int varSize = results.getResultVars().size();
				// Need to make sure that the iteration order is preserved...
				final QuerySolutionIterator it = new QuerySolutionIterator(result);
				// for (int i = 0; i < vars.size(); ++i) {
				int i = 0;
				while (it.hasNext()) {
					// final String var = vars.get(i);
					final String var = it.next();
					try {
						// clean it if it is a literal
						singleWalk += StringDelims.WALK_DELIM + result.get(var).toString();

//						if (result.get(var) != null && result.get(var).isLiteral()) {
//							String val = result.getLiteral(var).toString();
//							val = val.replace("\n", " ").replace("\t", " ").replace(StringDelims.WALK_DELIM, "");
//							singleWalk += val + " " + StringDelims.WALK_DELIM;
//						} else if (result.get(var) != null) {
//							singleWalk += result.get(var).toString().replace("http://dbpedia.org/resource/", "dbr:")
//									.replace("http://dbpedia.org/ontology/", "dbo:")
//									.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:")
//									.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:")
//									.replace(StringDelims.WALK_DELIM, "") + " " + StringDelims.WALK_DELIM;
//						}

					} catch (Exception e) {
						e.printStackTrace();
					}
					i++;
				}
				walkList.add(singleWalk);
			}

		}

	}

	public static void main(String[] args) {
		System.out.println("USAGE:  repoLocation outputFile nmWalks dpWalks nmThreads");
		WalkGenerator generator = new WalkGenerator(args[0]);
		generator.generateWalks(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
				Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]));
	}
}
