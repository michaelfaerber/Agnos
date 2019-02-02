package alu.linking.executable.preprocessing.setup;

import java.io.File;
import java.io.IOException;

import com.google.code.externalsorting.ExternalSort;

import alu.linking.structure.Executable;

public class ExtSortGrouper implements Executable {

	@Override
	public void init() {

	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		if (o == null || o.length < 1) {
			throw new IllegalArgumentException("No arguments");
		}

		if (o[0] == null || o[1] == null || !(o[0] instanceof String) || !(o[1] instanceof String)) {
			throw new IllegalArgumentException("First argument should be walkOutput(" + o[0]
					+ "). Second should be sentenceOutput(" + o[1] + ")...");
		}
		final String walkOutput = o[0].toString();
		final String sentencesOut = o[1].toString();

		final String sortOutput = walkOutput + "_sorted";
		try {
			getLogger().info("External Sorting/Grouping of " + walkOutput);
			getLogger().info("Into: " + sortOutput);
			// new ExternalSortByKey(" ->", 0).process(walkOutput);
			ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(walkOutput)), new File(sortOutput));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		getLogger().info("Output walks into:");
		getLogger().info(new File(walkOutput).getAbsolutePath());

		new SortedGrouper().exec(sortOutput, sentencesOut);
		return null;
	}

	@Override
	public boolean destroy() {
		return false;
	}

}
