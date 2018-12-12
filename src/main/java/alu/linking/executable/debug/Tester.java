package alu.linking.executable.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.input.NxParserGenericWrapper;
import alu.linking.structure.Executable;

public class Tester implements Executable {

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
		testB(KG);
		return null;
	}

	/**
	 * Check if everything that appears in the RDF input file (as triples) is also
	 * actually a node/predicate in the graph
	 * 
	 * @throws IOException
	 */
	private void testA(final EnumModelType KG) throws IOException {
		// Load graph
		Graph.getInstance().readIn(FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG),
				FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG),
				FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));
		// Now read from the input RDF grah file and compare with in-memory graph
		try (NxParserGenericWrapper parser = new NxParserGenericWrapper()) {
			final BufferedReader brIn = new BufferedReader(new FileReader(FilePaths.FILE_EXTENDED_GRAPH.getPath(KG)));
			FileInParser<org.semanticweb.yars.nx.Node> fip = parser.create(brIn, new Object[] { null });
			List<org.semanticweb.yars.nx.Node> line = null;
			DualHashBidiMap<Integer, String> m = Graph.getInstance().getIDMapping();
			DualHashBidiMap<Integer, String> predMap = Graph.getInstance().getPredicateIDMapping();
			int found = 0;
			int notFound = 0;
			while ((line = fip.getNext()) != null) {
				if (m.getKey(line.get(0).toN3()) != null && m.getKey(line.get(2).toN3()) != null
						&& predMap.getKey(line.get(1).toN3()) != null) {
					found++;
				} else {
					notFound++;
				}
			}
			brIn.close();
			System.out.println("Found Counter: " + found);
			System.out.println("Not found counter: " + notFound);
		}

	}

	/**
	 * Check that paths were computed for every node in the graph
	 */
	private void testB(final EnumModelType KG) throws IOException {
		// Load graph
		Graph.getInstance().readIn(FilePaths.FILE_HOPS_GRAPH_DUMP.getPath(KG),
				FilePaths.FILE_HOPS_GRAPH_DUMP_PATH_IDS.getPath(KG),
				FilePaths.FILE_HOPS_GRAPH_DUMP_EDGE_IDS.getPath(KG));

		File[] files = new File(FilePaths.DIR_HOPS_OUTPUT.getPath(KG)).listFiles();
		DualHashBidiMap<Integer, String> nodeMap = Graph.getInstance().getIDMapping();
		DualHashBidiMap<Integer, String> predMap = Graph.getInstance().getPredicateIDMapping();
		HashSet<String> notInMapSet = new HashSet<String>();
		HashSet<Integer> allNodesInPathOutput = new HashSet<Integer>();
		for (File f : files) {
			BufferedReader brIn = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = brIn.readLine()) != null) {
				final StringTokenizer st = new StringTokenizer(line, " ");
				// First token in line is the source node
				final String firstTokenInLine = st.nextToken();
				System.out.println("First token: " + firstTokenInLine);
				if (nodeMap.get(Integer.getInteger(firstTokenInLine)) == null) {
					notInMapSet.add(firstTokenInLine);
				}
				allNodesInPathOutput.add(Integer.getInteger(firstTokenInLine));
			}
			brIn.close();
		}
		System.out.println("Could not find following values FROM PATHS in the MAP:");
		System.out.println("(AKA. ID, UPDATE OR CONSISTENCY PROBLEM)");
		System.out.println(notInMapSet.size() + " out of " + allNodesInPathOutput.size());

		// Now check which values are in the map but not in the paths
		HashSet<Integer> notInPathsSet = new HashSet<Integer>();
		for (Map.Entry<Integer, String> e : nodeMap.entrySet()) {
			if (!allNodesInPathOutput.contains(e.getKey())) {
				notInPathsSet.add(e.getKey());
			}
		}
		System.out.println("Could not find following values FROM MAP in the PATHS:");
		System.out.println("(AKA. MISSING PATHS)");
		System.out.println(notInPathsSet.size() + " out of " + nodeMap.size());
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
