package alu.linking.executable.preprocessing.deprecated;

import java.io.File;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.util.VirtuosoCommunicator;
import alu.linking.structure.Executable;

public class VicinityComputer implements Executable {

	final EnumModelType KG;

	public VicinityComputer(final EnumModelType KG) {
		this.KG = KG;
	}

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
		final VirtuosoCommunicator virtCom = new VirtuosoCommunicator();
		if (o == null) {
			final String[] files = new File(FilePaths.DIR_QUERY_IN_EXTENDED_GRAPH_HOPS.getPath(KG)).list();
			for (String file : files) {
				if (new File(file).exists()) {
					virtCom.lookup(KG, file, true);
				}
			}
		} else {
			int argC = 0;
			final File inputFile = o.length > argC && o[argC] instanceof File ? (File) o[argC] : null;
			argC++;
			final File outputFile = o.length > argC && o[argC] instanceof File ? (File) o[argC] : null;
			virtCom.lookup(inputFile, outputFile);
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
		return "computeHop";
	}

}
