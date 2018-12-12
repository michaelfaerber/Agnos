package alu.linking.executable.preprocessing.deprecated;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Executable;

public class EntitySFLinkingFromFile implements Executable {

	@Override
	public void init() {

	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		final EnumModelType KG = EnumModelType.DEFAULT;
		final String SF_STRING = "http://xmlns.com/foaf/0.1/name";
		try (BufferedReader br = new BufferedReader(new FileReader(FilePaths.FILE_EXTENDED_GRAPH.getPath(KG)));
				BufferedWriter bw = new BufferedWriter(
						new FileWriter(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)))) {
			final NxParser parser = new NxParser(br);
			while (parser.hasNext()) {
				final Node[] triple = parser.next();
				if (triple[1].toString().equals(SF_STRING)) {
					bw.write(triple[0].toString() + Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val
							+ triple[2].toString());
					bw.newLine();
				}
			}
		}
		getLogger().debug("Finished compiling " + FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG));
		return null;
	}

	@Override
	public boolean destroy() {
		return false;
	}

	@Override
	public String getExecMethod() {
		return "exec";
	}

}
