package alu.linking.launcher;

import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.RDF2VecWalkGenerator;
import alu.linking.structure.Loggable;

public class LauncherWalkGenerator implements Loggable {
	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.
		// DBPEDIA_FULL//
		// MINI_MAG//
		// CRUNCHBASE2//
				MAG//
		;
		try {
			final int startLength = 4, endLength = 4, threads = 30;
			final boolean loadPredicateMapper = true;
			new RDF2VecWalkGenerator(KG, startLength, endLength, threads, null, loadPredicateMapper).exec();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
