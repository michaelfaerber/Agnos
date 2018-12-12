package alu.linking.disambiguation.hops.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.input.NxParserGenericWrapper;

public class GraphBuilder {
	private String inputFilepathN3 = null;
	private final EnumModelType KG;
	
	public GraphBuilder(final EnumModelType KG) {
		this(KG, alu.linking.config.constants.FilePaths.FILE_EXTENDED_GRAPH.getPath(KG));
	}

	public GraphBuilder(final EnumModelType KG, final String inputFileN3) {
		inputFilepathN3 = inputFileN3;
		this.KG = KG;
	}

	public synchronized void computeGraph() throws IOException {
		long startTime = System.currentTimeMillis();
		alu.linking.utils.Stopwatch.start(this.getClass().getName());

		try (final BufferedReader brIn = new BufferedReader(new FileReader(inputFilepathN3));
				NxParserGenericWrapper parser = new NxParserGenericWrapper()) {
			FileInParser<org.semanticweb.yars.nx.Node> fip = parser.create(brIn, new Object[] { null });
			List<org.semanticweb.yars.nx.Node> line = null;

			while ((line = fip.getNext()) != null) {
				Graph.getInstance().addNode(line.get(0), line.get(1), line.get(2));
			}
		}
		// Flagging appropriate nodes as ENTITIES...
		final HashSet<String> entities = new HashSet<String>();
		final Map<Integer, GraphNode<Integer>> allNodes = Graph.getInstance().getNodes();
		final DualHashBidiMap<Integer, String> idMapping = Graph.getInstance().getIDMapping();

		try (BufferedReader brIn = new BufferedReader(new FileReader(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)))) {
			entities.addAll(new MentionPossibilityLoader(KG)
					.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG))).keySet());
//			String line = null;
//			while ((line = brIn.readLine()) != null) {
//				final String[] entitySF = line.split(Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val);
//				final String entity = entitySF[0];
//				entities.add(entity);
//			}
			for (String entity : entities) {
				allNodes.get(idMapping.getKey(entity)).flagEntity();
			}
		}
		System.out.println("[BUILD GRAPH] Parsing Ended");
		System.out.println("[BUILD GRAPH] No. of nodes: " + Graph.getInstance().getNodes().size());
		System.out.println("[BUILD GRAPH] Execution time: "
				+ alu.linking.utils.Stopwatch.endDiff(this.getClass().getName()) + "ms");
	}

}
