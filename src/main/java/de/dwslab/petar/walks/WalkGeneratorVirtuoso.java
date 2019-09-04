package de.dwslab.petar.walks;


import java.util.Collection;

import org.apache.jena.query.QueryExecution;

//import virtuoso.jena.driver.VirtGraph;
//import virtuoso.jena.driver.VirtuosoQueryExecution;
//import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class WalkGeneratorVirtuoso extends WalkGenerator {

	public WalkGeneratorVirtuoso(Collection<String> predicateBlacklist, String entityQueryStr, String logEntities,
			WalkResultProcessor resultProcessor, String entitiesOutputPath) {
		super(predicateBlacklist, entityQueryStr, logEntities, resultProcessor, entitiesOutputPath);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected QueryExecution queryCreate(String query) {
		// TODO Auto-generated method stub
		return null;
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
	protected boolean isLineByLineOutput() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}
/*
	private final VirtGraph virtGraph;

	public WalkGeneratorVirtuoso(VirtGraph virtGraph, Collection<String> predicateBlacklist, String entityQueryStr,
			String logEntities, WalkResultProcessor resultProcessor, final String entitiesOutputPath) {
		super(predicateBlacklist, entityQueryStr, logEntities, resultProcessor, entitiesOutputPath);
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

*/
}
