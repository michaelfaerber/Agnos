package alu.linking.executable.preprocessing.deprecated;

import java.io.File;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.crunchbase.CrunchbaseKGManager;
import alu.linking.structure.Executable;

public class CrunchbaseExtendedGraphLoader implements Executable {

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
	public <T> T exec(Object... o) throws Exception {
		final EnumModelType KG = EnumModelType.DEFAULT;
		new CrunchbaseKGManager(FilePaths.DATASET_CRUNCHBASE.getPath(KG))
				.addStatements(new File(FilePaths.FILE_EXTENDED_GRAPH.getPath(KG)), "N3");
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
