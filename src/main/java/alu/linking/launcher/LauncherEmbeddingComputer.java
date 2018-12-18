package alu.linking.launcher;

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
		new GraphWalkHandler(EnumModelType.MAG).computeRequiredEntityEmbeddings();
	}
}