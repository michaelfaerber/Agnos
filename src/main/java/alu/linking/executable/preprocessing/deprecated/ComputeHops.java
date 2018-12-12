package alu.linking.executable.preprocessing.deprecated;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import org.apache.log4j.Logger;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.pathbuilding.ConcurrentPathBuilderBatch;
import alu.linking.structure.Executable;

public class ComputeHops implements Executable {
	private Logger logger = Logger.getLogger(getClass());

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
		// Load graph
		Graph.getInstance().readIn(FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG), FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG),
				FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));
		// Compute the paths

		final BufferedWriter wrtNotFoundTraversalNodes = new BufferedWriter(
				new FileWriter(alu.linking.config.constants.FilePaths.FILE_HOPS_SOURCE_NODES_NOT_FOUND.getPath(KG), false));
		final BufferedReader readerInAllTraversalNodes = new BufferedReader(
				new FileReader(alu.linking.config.constants.FilePaths.FILE_HOPS_SOURCE_NODES.getPath(KG)));

		final boolean outputPaths = true;
		final boolean outputEdges = false;
		final boolean outputDirections = false;
		final boolean outputToList = true;
		final List<String> paths = new ConcurrentPathBuilderBatch().fromFile(KG, readerInAllTraversalNodes, wrtNotFoundTraversalNodes, outputPaths,
				outputEdges, outputDirections, outputToList);
		logger.debug("All found Paths\n"+paths);
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
