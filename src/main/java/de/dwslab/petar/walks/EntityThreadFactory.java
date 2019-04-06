package de.dwslab.petar.walks;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;

class EntityThreadFactory {
	private final String queryStart;
	private final String queryEnd;
	private final BufferedWriter writer;
	private final BufferedWriter entityLog;
	private final boolean lineByLineOutput;
	private final WalkResultProcessor resultProcessor;
	private final WalkGenerator wg;

	public EntityThreadFactory(final WalkGenerator wg, final String walkQuery, final BufferedWriter writer,
			final WalkResultProcessor resultProcessor, final BufferedWriter entityLog, final boolean lineByLineOutput) {
		this.writer = writer;
		this.entityLog = entityLog;
		this.lineByLineOutput = lineByLineOutput;
		this.resultProcessor = resultProcessor;
		this.wg = wg;
		// String queryStr = walkQuery.replace("$ENTITY$", "<" + entity + ">");
		final String entityKeyword = "$ENTITY$";
		final int entityIndex = walkQuery.indexOf(entityKeyword);
		this.queryStart = walkQuery.substring(0, entityIndex);
		this.queryEnd = walkQuery.substring(entityIndex + entityKeyword.length());
	}

	public Runnable createNew(final String entity) {
		return new Runnable() {
			@Override
			public void run() {
				processEntity(entity);
				// writeToFile(finalList, writer);
			}
		};
	}

	private void processEntity(final String entity) {
		// This part does the 'long' path query computation
		executeQuery(entity);
		// Note: Removed 'direct query' computation, as it can also be done with this at
		// length 1...
	}

	/**
	 * Executes specified query on the wanted model (defined through the
	 * WalkGenerator instance and queryCreate)
	 * 
	 * @param queryStr
	 */
	public void executeQuery(String entity) {
		if (entity == null)
			return;
		wg.beginREAD();
		final StringBuilder sbQuery = new StringBuilder(queryStart);
		sbQuery.append("<");
		sbQuery.append(entity);
		sbQuery.append(">");
		sbQuery.append(queryEnd);
		try (final QueryExecution qe = wg.queryCreate(sbQuery.toString())) {
			final ResultSet results = qe.execSelect();
			// final ResultSet resultsTmp = qe.execSelect();
			// final ResultSet results = ResultSetFactory.copyResults(resultsTmp);
			wg.endTransaction();
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
