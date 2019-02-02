package alu.linking.launcher.debug;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.SortedGrouper;

public class LauncherSortedGrouper {

	public static void main(String[] args) {
		final EnumModelType kg = EnumModelType.DBPEDIA_FULL;
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);
		try {
			new SortedGrouper().exec(walkOutput, sentencesOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
