package alu.linking.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.surfaceform.query.SFQuery;

public class RDFUtils {
	final static Logger logger = LogManager.getLogger(RDFUtils.class);

	public static void getEntitiesFromNTFile(final EnumModelType kg, final File inputFile, final String queryStr) {
		final boolean smallKG = false;
		final Model model;

		if (smallKG) {
			model = getDefaultModel();
			try (final FileInputStream fis = new FileInputStream(inputFile)) {
				// logger.debug
				System.out.println("Reading in the model from:" + inputFile.getAbsolutePath());
				parseModel(fis, model);
				// logger.debug
				System.out.println("Finished reading!");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			final String datasetPath = "./tmpDataset";
			new File(datasetPath).mkdirs();
			// Non-empty file
			final Dataset dataset = TDBFactory.createDataset(datasetPath);
			dataset.begin(ReadWrite.READ);
			// Get model inside the transaction
			model = dataset.getDefaultModel();
			dataset.end();

			// Now load it all into the Model
			dataset.begin(ReadWrite.WRITE);
			try {
				TDBLoader.loadModel(model, inputFile.getAbsolutePath(), true);
				dataset.commit();
			} catch (Exception e) {
				System.out.println("Aborted: " + inputFile.getAbsolutePath());
				// model.abort();
				dataset.abort();
				throw e;
			} finally {
				dataset.end();
			}
		}
		// Now the data is loaded into the model
		// Query the entity data and output appropriately
		try (final BufferedWriter bwOut = new BufferedWriter(
				new FileWriter(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(kg))))) {
			List<BufferedWriter> writers = Lists.newArrayList();
			writers.add(bwOut);
			// logger.debug
			System.out.println("Executing query:");
			new SFQuery(kg).execSelectQuery(queryStr, model, writers);
			// logger.debug
			System.out.println("Finished query execution!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Model parseModel(final InputStream is, final Model model) {
		final RDFReaderRIOT rdfReader = new RDFReaderRIOT("NT");
		rdfReader.read(model, is, "");
		return model;
	}

	private static Model getDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		// model.setNsPrefixes(NIFTransferPrefixMapping.getInstance());
		return model;
	}

	public static ResultSet execQuery(Model model, String queryStr) {
		System.out.println("Executing");
		System.out.println(queryStr);
		final Query query = QueryFactory.create(queryStr);
		// Execute the query and obtain results
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		final ResultSet results = qe.execSelect();
		return results;
	}

}
