package alu.linking.launcher;

import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.RDF2VecWalkGenerator;
import alu.linking.structure.Loggable;

public class LauncherWalkGenerator implements Loggable {
	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.
		// DBPEDIA_FULL
		// MINI_MAG
				CRUNCHBASE2;
		try {
			new RDF2VecWalkGenerator(KG, 1, 6, 50, null).exec();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
