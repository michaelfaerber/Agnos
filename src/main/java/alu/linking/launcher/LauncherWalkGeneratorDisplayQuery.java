package alu.linking.launcher;

import java.util.Arrays;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Loggable;
import de.dwslab.petar.walks.WalkGenerator;

public class LauncherWalkGeneratorDisplayQuery implements Loggable {
	public static void main(String[] args) {
		new LauncherWalkGeneratorDisplayQuery().exec();
	}

	private void exec() {
		// Get a look at what the generated query is like
		final EnumModelType kg = EnumModelType.DEFAULT;
		final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
		final String sentencesOut = FilePaths.FILE_GRAPH_WALK_OUTPUT_SENTENCES.getPath(kg);
		final String s = new WalkGenerator("",
				Arrays.asList(new String[] { "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" }), null).generateQuery(7,
						40);
		System.out.println(s);
	}
}
