package alu.linking.launcher.debug;

import java.io.File;
import java.io.IOException;

import com.google.code.externalsorting.ExternalSort;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Loggable;

public class LauncherGroupByKey implements Loggable {
	public static void main(String[] args) {
		new LauncherGroupByKey().exec();
	}

	public void exec() {
		try {
			final EnumModelType kg = EnumModelType.DEFAULT;
			final String walkOutput = FilePaths.FILE_GRAPH_WALK_OUTPUT.getPath(kg);
			getLogger().info("External Sorting/Grouping: " + walkOutput);
			//new ExternalSortByKey(" ->", 0).process(walkOutput);
			final String sortedOutput = walkOutput+"_extSort.txt";
			ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(walkOutput)), new File(sortedOutput));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
