package de.dwslab.petar.walks;

import java.util.Collection;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

public class WalkGeneratorJena extends WalkGenerator {

	public WalkGeneratorJena(final String repoLocation, List<String> predicateBlacklist, String entityQueryStr, WalkResultProcessor resultProcessor) {
		this(repoLocation, predicateBlacklist, entityQueryStr, null, resultProcessor);
	}

	public WalkGeneratorJena(final String repoLocation, Collection<String> predicateBlacklist, String entityQueryStr,
			String logEntities, final WalkResultProcessor resultProcessor) {
		super(predicateBlacklist, entityQueryStr, logEntities, resultProcessor);
		this.dataset = TDBFactory.createDataset(repoLocation);
		this.model = dataset.getDefaultModel();
		// dataset, model,
	}

	/**
	 * the rdf model
	 */
	public final Model model;
	public final Dataset dataset;

	@Override
	protected QueryExecution queryCreate(String queryString) {
		// TODO Auto-generated method stub
		dataset.begin(ReadWrite.READ);
		final Query query = QueryFactory.create(queryString);
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		return qe;
	}

	@Override
	protected void beginREAD() {
		dataset.begin(ReadWrite.READ);
	}

	@Override
	protected void beginWRITE() {
		dataset.begin(ReadWrite.WRITE);
	}

	@Override
	protected void endTransaction() {
		dataset.end();
	}

	@Override
	public void close() throws Exception {
		this.dataset.close();
	}

	@Override
	protected boolean isLineByLineOutput() {
		return true;
	}

}
