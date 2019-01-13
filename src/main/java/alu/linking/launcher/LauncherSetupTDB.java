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

		// Load the crunchbase graph into the crunchbase dataset
		final EnumModelType KG = EnumModelType.
		// MINI_MAG;
				DBLP;
		// CRUNCHBASE2;
		// CRUNCHBASE;
		System.out.println("Setting up TDB for: " + KG.name());
		final String KGpath =
				// "/vol1/cb/crunchbase-201510/dumps/crunchbase-dump-201510.nt";//CB1
				// "/vol1/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06.nt";//CB2
				// "/vol1/dblp/dumps/dblp_2018-11-02_unique.nt";//DBLP
				// "/vol1/mag/data/2018-07-19/MAGFieldsOfStudyKG/MAGFieldsOfStudyKG.nt";//MAG
				// "./crunchbase-dump-2018-06_normalized.nt";// normalized CB2
				"./dblp_2018-11-02_unique_normalized.nt";// normalized DBLP
		System.out.println("Source: " + KGpath);
		new LauncherSetupTDB().exec(KG, KGpath);

		// Set up for other

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
