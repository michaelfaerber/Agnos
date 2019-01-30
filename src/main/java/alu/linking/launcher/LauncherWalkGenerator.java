package alu.linking.launcher;

import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.RDF2VecWalkGenerator;
import alu.linking.structure.Loggable;

public class LauncherWalkGenerator implements Loggable {
	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
		try {
			new RDF2VecWalkGenerator(KG, 2, 40, null).exec();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
