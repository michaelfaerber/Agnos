package alu.linking.preprocessing.dbpedia.spotlight;

import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Statement;

import alu.linking.config.kg.EnumModelType;
import alu.linking.config.kg.KGManager;

public class SpotlightKGManager extends KGManager {
	public SpotlightKGManager(final String dsPath) {
		super(dsPath, EnumModelType.DBPEDIA);
	}

	@Override
	public void convertToKG(final String filepath, final Map<String, Object> jsonMap) {
		for (Map.Entry<String, Object> e : jsonMap.entrySet()) {
			final String entityURL = e.getKey();
			final String surface_form = ((Map) e.getValue()).get("@surfaceForm").toString();
			final String offset = ((Map) e.getValue()).get("@offset").toString();
			addEntity(this.default_set_dsPath, EnumModelType.DBPEDIA, filepath, entityURL, surface_form);
		}
	}

	@Override
	public List<Statement> getStatements(final String dsPath, final String subject, final String property,
			final String object) {
		return getStatements(dsPath, EnumModelType.DBPEDIA, subject, property, object);
	}

	@Override
	public List<Statement> getAllStatements() {
		return getStatements(null, null, null);
	}

	@Override
	public List<Statement> getStatements(String subject, String predicate, String object) {
		return getStatements(default_set_dsPath, EnumModelType.DBPEDIA, subject, predicate, object);
	}

}
