package alu.linking.launcher;

import org.apache.log4j.Logger;

import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.Pipeline;
import alu.linking.executable.preprocessing.deprecated.PageRankComputer;
import alu.linking.executable.preprocessing.setup.MentionDetectionSetup;

public class LauncherSetup {
	public static void main(String[] args) {
		final boolean t = true;
		final boolean f = false;
		final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
		try {
			final Pipeline pipeline = new Pipeline();
			Logger.getLogger(LauncherSetup.class).info("Setting up precomputation structures");
			// pipeline.setOutput(true);
			// Download all the tagtog files
			// pipeline.queue(new TagtogDataDownloader(), null);
			// Transform all the tagtog files from JSON to RDF
			// pipeline.queue(new TagtogRDFTransformer(), null);
			// Combine transformed RDF files into a single graph
			// pipeline.queue(new BMWGraphCombiner(), null);
			// Computed Pagerank for the given RDF graph
			pipeline.queue(new PageRankComputer(KG), null);
			// Have to replace tab characters by space character for N3 compliance

			// pipeline.queue(new EntitySFLinkingFromFile(), null);
			// Compute a logical graph (for disambiguation) based on the RDF graph
			// And output it...
			// pipeline.queue(new BuildGraph(), null);

			pipeline.queue(new MentionDetectionSetup(KG), null);
			pipeline.exec();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
