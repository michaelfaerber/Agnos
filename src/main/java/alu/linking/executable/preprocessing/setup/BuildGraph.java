package alu.linking.executable.preprocessing.setup;

import java.io.BufferedWriter;
import java.io.FileWriter;

import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.GraphBuilder;
import alu.linking.structure.Executable;

public class BuildGraph implements Executable {

	@Override
	public void init() {
	}

	@Override
	public boolean reset() {

		return false;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		getLogger().debug("Computing graph...");
		final EnumModelType KG = EnumModelType.DEFAULT;
		new GraphBuilder(KG).computeGraph();
		getLogger().debug("Graph computed!");
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(alu.linking.config.constants.FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG)));
		getLogger().debug("Dumping graph...");
		Graph.getInstance().dump(bw);
		Graph.getInstance().outputNodeIDs(alu.linking.config.constants.FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG));
		Graph.getInstance()
				.outputPredicateIDs(alu.linking.config.constants.FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));
		Graph.getInstance().reset();
		getLogger().debug("Graph dumped!");
		return null;
	}

	@Override
	public boolean destroy() {

		return false;
	}

	@Override
	public String getExecMethod() {
		// TODO Auto-generated method stub
		return null;
	}

}
