package alu.linking.executable.preprocessing.util;

import java.io.File;
import java.io.IOException;

import alu.linking.structure.Executable;

public class FileCombiner implements Executable {

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T exec(Object... o) throws IOException {

		if (o != null) {
			// final File inFile, final File outFile, final boolean append
			if (o.length >= 1 && o.length <= 3) {
				int argC = 0;
				final File inFile = o.length > argC ? (File) o[argC] : null;
				argC++;
				final File outFile = o.length > argC ? (File) o[argC] : new File(inFile.getAbsolutePath() + "_out");
				argC++;
				final boolean append = o.length > argC ? (boolean) o[argC] : false;
				alu.linking.utils.FileUtils.transferFileContentsFromTo(inFile, outFile, append);
			}

		}
		return null;
	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getExecMethod() {
		// TODO Auto-generated method stub
		return null;
	}

}
