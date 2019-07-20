package alu.linking.launcher.debug;

import java.io.IOException;

import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.embeddings.posttraining.GraphWalkHandler;

/**
 * Computes the entity embeddings based on trained word embeddings
 * 
 * @author Kris
 *
 */
public class LauncherEmbeddingComputer {
	public static void main(String[] args) {
		try {
			final EnumModelType KG = EnumModelType.CRUNCHBASE;
			System.out.println("Transforming python entity embeddings into a Java map");
			new GraphWalkHandler(KG).readPythonEntityEmbeddingsOutputHashMap();
			System.out.println("Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}