package alu.linking.launcher;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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

public class LauncherSetupTDB implements Loggable {
	public static void main(String[] args) {
		new LauncherSetupTDB().exec();
	}

	private void exec() {
		// Choose for which KG to load it into the TDB
		// final EnumModelType KG = EnumModelType.MAG;
		for (EnumModelType KG : EnumModelType.values()) {
			final String datasetPath = FilePaths.DATASET.getPath(KG);
			final String KGpath = FilePaths.FILE_EXTENDED_GRAPH.getPath(KG);
			try (BufferedReader br = new BufferedReader(new FileReader(KGpath))) {
				if (br.readLine() == null) {
					getLogger().info("Skipping " + KG.name() + " due to empty file.");
					continue;
				}
				else
				{
					getLogger().info("Loading " + KG.name());
				}
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
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
