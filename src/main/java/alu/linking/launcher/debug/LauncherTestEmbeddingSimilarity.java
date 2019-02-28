package alu.linking.launcher.debug;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.candidategeneration.PossibleAssignment;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.launcher.LauncherContinuousMentionDetector;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.utils.DetectionUtils;
import alu.linking.utils.EmbeddingsUtils;
import alu.linking.utils.IDMappingLoader;
import alu.linking.utils.Stopwatch;

public class LauncherTestEmbeddingSimilarity {

	public static Logger getLogger() {
		return Logger.getLogger(LauncherTestEmbeddingSimilarity.class);
	}

	public static void main(String[] args) {
		try {
			getLogger().info("Testing embedding similarity");
			final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
			System.out.println("Loading mention possibilities...");
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(KG, null);
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			final Set<String> stopwords = stopwordsLoader.getStopwords();
			final InputProcessor inputProcessor = new InputProcessor(stopwords);
			// ########################################################
			// Mention Detection
			// ########################################################
			final MentionDetector md = DetectionUtils.setupMentionDetection(KG, map, inputProcessor);

			// ########################################################
			// Candidate Generator
			// ########################################################
			final CandidateGenerator candidateGenerator = new CandidateGeneratorMap(map);

			final IDMappingLoader<String> entityMapping = new IDMappingLoader<String>()
					.loadHumanFile(new File(FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG)));
			final File embedFile = new File(FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG));
			getLogger().info("Loading embeddings from: " + embedFile.getAbsolutePath());
			Stopwatch.start(LauncherContinuousMentionDetector.class.getName());
			Map<String, List<Number>> entityEmbeddingsMap = EmbeddingsUtils.readEmbeddings(embedFile, entityMapping,
					true);
			getLogger().info("Finished(" + Stopwatch.endOutput(LauncherContinuousMentionDetector.class.getName())
					+ " ms.) loading embeddings from: " + embedFile.getAbsolutePath());

			List<Mention> mentionsLeft = null;
			List<Mention> mentionsRight = null;

			String inputLine = null;
			try (final Scanner sc = new Scanner(System.in)) {
				do {
					// ########################################################
					// Awaiting input
					// ########################################################
					System.out.println("\\\\033[1mAwaiting user input\\\\033[0m...");
					// inputLine = sc.nextLine();
					inputLine = "Victoria Beckham";
					mentionsLeft = md.detect(InputProcessor.combineProcessedInput(InputProcessor.process(inputLine)));
					System.out.println("Detected [" + mentionsLeft.size() + "] mentions.");
					for (Mention mention : mentionsLeft) {
						mention.updatePossibleAssignments(candidateGenerator.generate(mention));
					}

					// inputLine = sc.nextLine();
					inputLine = "David Beckham";
					mentionsRight = md.detect(InputProcessor.combineProcessedInput(InputProcessor.process(inputLine)));
					System.out.println("Detected [" + mentionsRight.size() + "] mentions.");
					for (Mention mention : mentionsRight) {
						mention.updatePossibleAssignments(candidateGenerator.generate(mention));
					}

					for (int i = 0; i < mentionsLeft.size(); ++i) {
						System.out.print("i(" + i + ") ");
						for (int j = i + 1; j < mentionsRight.size(); ++j) {
							System.out.print("j(" + j + ") ");
							final Collection<PossibleAssignment> assignmentsLeft = mentionsLeft.get(i)
									.getPossibleAssignments();
							final Collection<PossibleAssignment> assignmentsRight = mentionsRight.get(j)
									.getPossibleAssignments();
							for (PossibleAssignment possAssLeft : assignmentsLeft) {
								for (PossibleAssignment possAssRight : assignmentsRight) {
									final String leftEntity = possAssLeft.getAssignment().toString();
									final String rightEntity = possAssRight.getAssignment().toString();
									final List<Number> left = entityEmbeddingsMap.get(leftEntity);
									final List<Number> right = entityEmbeddingsMap.get(rightEntity);
									if (left != null && right != null) {
										final Number sim = EmbeddingsUtils.cosineSimilarity(left, right, true);
										System.out.println("Sim(" + sim + "): " + leftEntity + " - " + rightEntity);
									} else {
										System.out.println("NULL:" + leftEntity + "("
												+ (left == null ? "NULL" : "NOT NULL") + ") - " + rightEntity + "("
												+ (right == null ? "NULL" : "NOT NULL") + ")");
									}
								}
							}
						}
					}
					inputLine = null;
				} while (inputLine != null && inputLine.length() != 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
