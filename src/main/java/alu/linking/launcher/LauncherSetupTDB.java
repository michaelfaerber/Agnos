package alu.linking.launcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Loggable;

/**
 * Load a specified knowledge base into a Jena dataset that we can query
 * henceforth
 * 
 * @author Kris
 *
 */
public class LauncherSetupTDB implements Loggable {
	public static void main(String[] args) {
		// new LauncherSetupTDB().exec();
//			final String[] MAG_load_locations = new String[] {
//					"/home/faerberm/inRDF/Affiliations.nt",
//					"/home/faerberm/inRDF/Authors.nt",
//					"/home/faerberm/inRDF/ConferenceInstances.nt",
//					"/home/faerberm/inRDF/ConferenceSeries.nt",
//					"/home/faerberm/inRDF/FieldsOfStudy.nt",
//					"/home/faerberm/inRDF/Journals.nt",
//					"/home/faerberm/inRDF/PaperAuthorAffiliations.nt",
//					"/home/faerberm/inRDF/PaperCitationContexts.nt",//???
//					"/home/faerberm/inRDF/PaperFieldsOfStudy.nt",
//					"/home/faerberm/inRDF/PaperLanguages.nt",//???
//					"/home/faerberm/inRDF/Papers.nt",
//					"/home/faerberm/inRDF/PaperUrls.nt",
//					"/home/faerberm/inRDF/RelatedFieldOfStudy.nt",
//			};
		// Load the crunchbase graph into the crunchbase dataset
		final EnumModelType KG = null;//EnumModelType.CRUNCHBASE;
		final String KGpath = "/vol1/cb/crunchbase-201510/dumps/crunchbase-dump-201510.nt";
		new LauncherSetupTDB().exec(KG, KGpath);
	}

	/**
	 * Loads all KGs into their respective datasets
	 * 
	 */
	private void exec() {
		// Choose for which KG to load it into the TDB
		// final EnumModelType KG = EnumModelType.MAG;
		for (EnumModelType KG : EnumModelType.values()) {
			final String KGpath = FilePaths.FILE_EXTENDED_GRAPH.getPath(KG);
			// Read a line to make sure it is not an empty file we are trying to load
			try (BufferedReader br = new BufferedReader(new FileReader(KGpath))) {
				if (br.readLine() == null) {
					// Skip this file if it's empty
					getLogger().info("Skipping " + KG.name() + " due to empty file.");
					continue;
				} else {
					// Process file if it's not empty
					getLogger().info("Loading " + KG.name());
					exec(KG, KGpath);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads a single KG into the appropriate dataset
	 * 
	 * @param KG     which graph it corresponds to
	 * @param KGpath where to load it from
	 */
	private void exec(EnumModelType KG, final String KGpath) {
		final String datasetPath = FilePaths.DATASET.getPath(KG);
		// Non-empty file
		final Dataset dataset = TDBFactory.createDataset(datasetPath);
		dataset.begin(ReadWrite.READ);
		// Get model inside the transaction
		Model model = dataset.getDefaultModel();
		dataset.end();

		// Now load it all into the Model
		dataset.begin(ReadWrite.WRITE);
		model = dataset.getDefaultModel();
		TDBLoader.loadModel(model, KGpath, true);
		model.commit();
		dataset.end();

	}
}
