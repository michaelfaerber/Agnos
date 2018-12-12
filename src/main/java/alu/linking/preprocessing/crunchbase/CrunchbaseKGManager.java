package alu.linking.preprocessing.crunchbase;

import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Statement;

import alu.linking.config.kg.EnumModelType;
import alu.linking.config.kg.KGManager;

public class CrunchbaseKGManager extends KGManager {

	public CrunchbaseKGManager(String defaultDSPath) {
		super(defaultDSPath, EnumModelType.CRUNCHBASE);
	}

	@Override
	public void convertToKG(String filepath, Map<String, Object> jsonMap) {
		// Read from file and add to model
	}

	@Override
	public List<Statement> getStatements(String dsPath, String subject, String property, String object) {
		return getStatements(dsPath, EnumModelType.CRUNCHBASE, subject, property, object);
	}

	@Override
	public List<Statement> getAllStatements() {
		return getStatements(null, null, null);
	}

	@Override
	public List<Statement> getStatements(String subject, String predicate, String object) {
		return getStatements(default_set_dsPath, EnumModelType.CRUNCHBASE, subject, predicate, object);
	}

}
