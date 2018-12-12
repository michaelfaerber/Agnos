package alu.linking.config.kg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;

import alu.linking.config.constants.Strings;
import alu.linking.utils.Stopwatch;

public abstract class KGManager {
	final static Map<String, Dataset> dsMap = new HashMap<String, Dataset>();
	protected final String default_set_dsPath;
	private final EnumModelType type;

	public KGManager(final String defaultDSPath, final EnumModelType type) {
		this.default_set_dsPath = defaultDSPath;
		this.type = type;
		createDataset(defaultDSPath);
	}

	public abstract void convertToKG(final String filepath, final Map<String, Object> jsonMap);

	public void addEntity(String dsPath, EnumModelType modelName, String filepath, String entity, String surface_form) {
		/*
		 * //if you want to use a URL with a filepath... String url_filepath = ""; if
		 * (filepath != null) { url_filepath = Strings.RDF_FILE_PREFIX.val +
		 * filepath.replaceAll("\\\\", "/"); } final String s = url_filepath + "#" +
		 * surface_form; System.out.println("Adding entity with prefix: " +
		 * url_filepath);
		 */
		final String s = entity;
		final String p = Strings.PRED_SURFACE_FORM_CRUMMYMATCH.val;
		// final String o = entity;
		final String o = surface_form;
		System.out.println("Adding: " + s + " --- " + p + " ---> " + o);
		addStatement(dsMap.get(dsPath), modelName.name(), s, p, o);
	}

	/**
	 * Creates dataset for specified dsPath if it doesn't exist yet, otherwise
	 * simply returns the existing one
	 * 
	 * @param dsPath
	 * @return
	 */
	public Dataset createDataset(final String dsPath) {
		Dataset ret;
		if ((ret = dsMap.get(dsPath)) == null) {
			ret = TDBFactory.createDataset(dsPath);
			dsMap.put(dsPath, ret);
		}
		return ret;
	}

	/**
	 * Full (outer) merge (aka. union) of two models which is stored into the given
	 * dataset with the modelTypeMerged.<br/>
	 * <b>Note</b>: Replaces any old models with the given modelTypeMerged-named
	 * model
	 * 
	 * @param dsPath
	 *            Path of dataset for all 3 considered models
	 * @param modelTypeLeft
	 *            Model to execute action on
	 * @param modelTypeRight
	 *            Model to execute action on
	 * @param modelTypeMerged
	 *            Resulting merged model
	 */
	public static void mergeSurfaceForms(String dsPath, EnumModelType modelTypeLeft, EnumModelType modelTypeRight,
			EnumModelType modelTypeMerged) {
		// Create new KG with merged data
		Dataset ds = dsMap.get(dsPath);
		ds.begin(ReadWrite.WRITE);
		final Model modelLeft = ds.getNamedModel(modelTypeLeft.name());
		final Model modelRight = ds.getNamedModel(modelTypeRight.name());
		ds.replaceNamedModel(modelTypeMerged.name(), ModelFactory.createUnion(modelLeft, modelRight));
		ds.commit();
		ds.end();
	}

	/**
	 * Left join of models
	 * 
	 * @param dsPath
	 * @param modelType
	 */
	public void mergeSurfaceFormsLeft(String dsPath, EnumModelType modelTypeLeft, EnumModelType modelTypeRight) {
		final List leftStatements = getAllStatements(dsPath, modelTypeLeft);
		final List rightStatements = getAllStatements(dsPath, modelTypeRight);
		// Create new KG with merged data

	}

	/**
	 * Right join of models
	 * 
	 * @param dsPath
	 * @param modelType
	 */
	public void mergeSurfaceFormsRight(String dsPath, EnumModelType modelTypeLeft, EnumModelType modelTypeRight) {
		mergeSurfaceFormsLeft(dsPath, modelTypeRight, modelTypeLeft);
	}

	/**
	 * Inner join of models (aka. intersection)
	 * 
	 * @param dsPath
	 * @param modelType
	 */
	public void mergeSurfaceFormsInner(String dsPath, EnumModelType modelTypeLeft, EnumModelType modelTypeRight) {
		final List leftStatements = getAllStatements(dsPath, modelTypeLeft);
		final List rightStatements = getAllStatements(dsPath, modelTypeRight);
		// Create new KG with merged data

	}

	public void loadModel(final String dsPath, final String modelName, final String path) {
		final Dataset ds = dsMap.get(dsPath);
		loadModel(ds, modelName, path);
	}

	public void loadModel(final Dataset ds, final String modelName, final String path) {
		Model model = null;

		ds.begin(ReadWrite.WRITE);
		try {
			model = ds.getNamedModel(modelName);
			FileManager.get().readModel(model, path);
			ds.commit();
		} finally {
			ds.end();
		}
	}

	public void addStatement(String dsPath, String modelName, String subject, String property, String object) {
		final Dataset ds = dsMap.get(dsPath);
		addStatement(ds, modelName, subject, property, object);
	}

	/**
	 * Add statements from a file to specified model in its appropriate dataset
	 * 
	 * @param ds
	 * @param inputFile
	 * @param fileType
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void addStatements(final File inputFile, final String fileType) throws FileNotFoundException, IOException {
		Model model = null;
		final Dataset ds = dsMap.get(default_set_dsPath);
		try {
			model = ds.getNamedModel(this.type.name());
			model.begin();
			try (InputStream is = new FileInputStream(inputFile)) {
				Stopwatch.start(getClass().getName());
				System.out.println("Reading the model...");
				model.read(is, null, fileType);
				Stopwatch.endOutput(getClass().getName());
				System.out.println("Finished reading the model");
				model.commit();
				model.close();
			}
		} finally {
		}
	}

	public void addStatement(Dataset ds, String modelName, String subject, String property, String object) {
		Model model = null;

		ds.begin(ReadWrite.WRITE);
		try {
			model = ds.getNamedModel(modelName);

			Statement stmt = model.createStatement(model.createResource(subject), model.createProperty(property),
					model.createResource(object));

			model.add(stmt);
			ds.commit();
		} finally {
			// if (model != null)
			// model.close();
			ds.end();
		}
	}

	public abstract List<Statement> getStatements(String dsPath, String subject, String property, String object);

	/**
	 * Query the given model within specified dataset for any statements
	 * 
	 * @param dsPath
	 *            Dataset path
	 * @param modelType
	 *            Which model to query
	 * @return all statements for the given model within the specified dataset
	 */
	public static List<Statement> getAllStatements(String dsPath, EnumModelType modelType) {
		return getStatements(dsPath, modelType, null, null, null);
	}

	public abstract List<Statement> getAllStatements();

	public abstract List<Statement> getStatements(final String subject, final String predicate, final String object);

	/**
	 * Returns all statements from the specified dataset for the particular model
	 * with optional subject/predicate/object selection criteria.
	 * 
	 * 
	 * @param dsPath
	 *            Which dataset to query the data from
	 * @param modelType
	 *            Which model within the dataset should be queried
	 * @param subject
	 *            if null -> can be any
	 * @param property
	 *            if null -> can be any
	 * @param object
	 *            if null -> can be any
	 * @return all statements fitting the input criteria
	 */
	public static List<Statement> getStatements(final String dsPath, final EnumModelType modelType,
			final String subject, final String property, final String object) {
		List<Statement> results = new ArrayList<Statement>();
		final Dataset ds = dsMap.get(dsPath);

		Model model = null;
		final String modelName = modelType.name();
		ds.begin(ReadWrite.READ);
		try {
			model = ds.getNamedModel(modelName);

			Selector selector = new SimpleSelector((subject != null) ? model.createResource(subject) : (Resource) null,
					(property != null) ? model.createProperty(property) : (Property) null,
					(object != null) ? model.createResource(object) : (RDFNode) null);

			StmtIterator it = model.listStatements(selector);
			{
				while (it.hasNext()) {
					Statement stmt = it.next();
					results.add(stmt);
				}
			}
			ds.commit();
		} finally {
			ds.end();
		}

		return results;
	}

	public void removeStatement(String dsPath, String modelName, String subject, String property, String object) {
		Model model = null;
		final Dataset ds = dsMap.get(dsPath);

		ds.begin(ReadWrite.WRITE);
		try {
			model = ds.getNamedModel(modelName);

			Statement stmt = model.createStatement(model.createResource(subject), model.createProperty(property),
					model.createResource(object));

			model.remove(stmt);
			ds.commit();
		} finally {
			if (model != null)
				model.close();
			ds.end();
		}
	}

	public void closeAll() {
		for (Map.Entry<String, Dataset> e : dsMap.entrySet()) {
			e.getValue().close();
		}
	}

	public void close(final String dsPath) {
		dsMap.get(dsPath).close();
	}

	public void read(final String filepath) {
		final Dataset ds = dsMap.get(default_set_dsPath);
		ds.getNamedModel(this.type.name()).read(filepath);
	}

}
