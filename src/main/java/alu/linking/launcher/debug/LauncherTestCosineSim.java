package alu.linking.launcher.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.scorers.GraphWalkEmbeddingScorer;
import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import alu.linking.utils.EmbeddingsUtils;

public class LauncherTestCosineSim {

	public static void main(String[] args) {
		final boolean TEST_RESOURCES = true;
		final boolean TEST_NUMERICAL = false;
		if (TEST_NUMERICAL) {
			System.out.println(EmbeddingsUtils.cosineSimilarity(Arrays.asList(new Double[] { 5.0, 10.0 }),
					Arrays.asList(new Double[] { 2.0, 5.0 })));
		}

		if (TEST_RESOURCES) {
			try {
				final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
				Map<String, List<Number>> entityEmbeddingsMap;
				entityEmbeddingsMap = GraphWalkEmbeddingScorer.humanload(
						FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG),
						FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG));
				final EntitySimilarityService similarityService = new EntitySimilarityService(entityEmbeddingsMap);
				final Collection<String> resources = new ArrayList<>(new HashSet<>(Arrays.asList(new String[] {
						"http://dbpedia.org/resource/Pope", "http://dbpedia.org/resource/Freiburg_im_Breisgau",
						"http://dbpedia.org/resource/University_of_Freiburg", "http://dbpedia.org/resource/Karlsruhe",
						"http://dbpedia.org/resource/Berlin", "http://dbpedia.org/resource/IBM",
						"http://dbpedia.org/resource/Penguin", "http://dbpedia.org/resource/Kyoto"

				})));
				final StringBuilder sbResult = new StringBuilder();
				// Create header line
				sbResult.append("FROM\\TO");
				sbResult.append(";");
				for (String resource : resources) {
					sbResult.append(resource.replace("http://dbpedia.org/resource", ""));
					sbResult.append(";");
				}
				sbResult.append("\n");

				// Now do all the lines
				for (String from : resources) {
					sbResult.append(from.replace("http://dbpedia.org/resource", ""));
					sbResult.append(";");
					for (String to : resources) {
						sbResult.append(similarityService.similarity(from, to));
						sbResult.append(";");
					}
					sbResult.append("\n");
				}

				System.out.println("Result:");
				System.out.println(sbResult.toString());
				System.out.println("Not found IRIs:" + similarityService.notFoundIRIs);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
