package alu.linking.launcher.debug;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.ExtSortGrouper;

public class LauncherExtSortGroup {

	public static void main(String[] args) {
		final EnumModelType kg = EnumModelType.DBPEDIA_FULL;
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);
		try {
			new ExtSortGrouper().exec(walkOutput, sentencesOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
