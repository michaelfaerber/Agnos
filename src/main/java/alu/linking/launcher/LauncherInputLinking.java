package alu.linking.launcher;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.yars.nx.Node;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.disambiguation.hops.graph.EdgeBlacklisting;
import alu.linking.disambiguation.hops.graph.Graph;
import alu.linking.disambiguation.hops.graph.NodeBlacklisting;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.MentionDetectorLSH;
import alu.linking.utils.Stopwatch;

public class LauncherInputLinking {
	private static boolean init = false;

	private static MentionDetector md;
	private static AssignmentChooser<Node> chooser;
	private static CandidateGenerator<Node> candidateGenerator;
	private static Map<String, Set<String>> map;
	private static EnumModelType KG;

	private static void init() throws Exception {
		KG = EnumModelType.DEFAULT;
		if (init)
			return;

		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG);
		map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		candidateGenerator = new CandidateGeneratorMap(map);

		md = new MentionDetectorLSH(map, 0.85);
		// ########################################################
		// Mention Detection
		// ########################################################
		chooser = new AssignmentChooser<Node>(KG, new File(FilePaths.FILE_PAGERANK.getPath(KG)));
		// Blacklisting stuff from graph
		Stopwatch.start("Blacklist");
		NodeBlacklisting nBlacklisting = new NodeBlacklisting(Graph.getInstance());
		EdgeBlacklisting eBlacklisting = new EdgeBlacklisting(Graph.getInstance());
		for (Map.Entry<String, String> e : blacklistMap().entrySet()) {
			nBlacklisting.blacklist(e.getKey());
		}
		for (String key : blacklistMapEdges()) {
			eBlacklisting.blacklist(key);
		}

		nBlacklisting.blacklistConnectionsOver(0.05);
		nBlacklisting.enforce();
		Stopwatch.endOutput("Blacklist");
		init = true;
	}

	public static List<Mention<Node>> run(final String inputLine) {
		try {
			init();
			final List<Mention<Node>> mentions = md.detect(inputLine);
			// ########################################################
			// Candidate Generation (update for mentions)
			// ########################################################
			Collections.sort(mentions, new Comparator<Mention<Node>>() {
				@Override
				public int compare(Mention<Node> o1, Mention<Node> o2) {
					// Made so it accepts the smallest match as the used one
					final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
					return (o1.getOffset() == o2.getOffset()) ? diffLength
							: ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
				}
			});
			for (Mention<Node> m : mentions) {
				// Update possible assignments
				m.updatePossibleAssignments(candidateGenerator.generate(m));
			}

			// ########################################################
			// Disambiguation
			// ########################################################
			chooser.choose(mentions);
			return mentions;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Map<String, String> blacklistMap() {
		Map<String, String> entityIDMapping = new HashMap<String, String>();
		return entityIDMapping;
	}

	private static Set<String> blacklistMapEdges() {
		final HashSet<String> ret = new HashSet<String>();
		ret.add("http://ma-graph.org/property/rank");
		ret.add("http://purl.org/dc/terms/created");
		ret.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		return ret;
	}

}
