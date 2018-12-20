package alu.linking.launcher;

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
			System.out.println("Transforming python entity embeddings into a Java map");
			new GraphWalkHandler(EnumModelType.CRUNCHBASE).readPythonEntityEmbeddingsOutputHashMap();
			System.out.println("Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}