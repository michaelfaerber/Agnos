package alu.linking.disambiguation.pagerank;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import alu.linking.structure.Loggable;

public class PageRankLoader implements Loggable {
	/**
	 * Reads pagerank from a proper pagerank RDF file where the source is the node
	 * for which the object is the pagerank value of e.g. <a> <:PRValue> "50.23"
	 * 
	 * @param inFile
	 * @return
	 */
	public Map<String, Number> readIn(final File inFile) {
		final Map<String, Number> map = new HashMap<String, Number>();
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			final NxParser nxparser = new NxParser(brIn);
			while (nxparser.hasNext()) {
				final Node[] nodes = nxparser.next();
				try {
					map.put(nodes[0].toString(), Float.valueOf(nodes[2].toString()));
				} catch (ArrayIndexOutOfBoundsException aiooe) {
					getLogger().error("Error appeared with: " + Arrays.toString(nodes));
					throw aiooe;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
}
