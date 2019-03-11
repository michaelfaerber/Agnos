package de.dwslab.petar.walks;

import java.util.Collection;

import org.apache.jena.query.QueryExecution;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class WalkGeneratorVirtuoso extends WalkGenerator {
	private final VirtGraph virtGraph;

	public WalkGeneratorVirtuoso(VirtGraph virtGraph, Collection<String> predicateBlacklist, String entityQueryStr,
			String logEntities, WalkResultProcessor resultProcessor) {
		super(predicateBlacklist, entityQueryStr, logEntities, resultProcessor);
		this.virtGraph = virtGraph;
	}

	@Override
	protected void beginREAD() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void beginWRITE() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void endTransaction() {
		// TODO Auto-generated method stub

	}

	@Override
	protected QueryExecution queryCreate(String queryStr) {
		final VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(queryStr, virtGraph);
		return vqe;
	}

	@Override
	public void close() throws Exception {
		if (!this.virtGraph.isClosed()) {
			this.virtGraph.close();
		}
	}

	@Override
	protected boolean isLineByLineOutput() {
		return true;
	}

}
