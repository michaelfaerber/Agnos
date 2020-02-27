package alu.linking.launcher.eval.wikidata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.mentiondetection.exact.HashMapCaseInsensitive;
import alu.linking.mentiondetection.exact.MentionDetectorMap;
import alu.linking.structure.PossibleAssignment;

public class LauncherOutputAmbiguity {
	private static boolean init = false;

	private MentionDetector md;
	private CandidateGenerator candidateGenerator;
	private Map<String, Collection<String>> map;
	private final EnumModelType KG;

	public static void main(String[] argv) {
		final LauncherOutputAmbiguity executable = new LauncherOutputAmbiguity(EnumModelType.WIKIDATA);
		final String inFile = "./infile.txt";
		final String outFile = "./outfile.txt";
		final String outMentionSeparator = "###### NEW MENTION #####";
		final String outSentenceSeparator = "##### NEW SENTENCE #####";
		try (final BufferedReader br = new BufferedReader(new FileReader(new File(inFile)));
				final BufferedWriter bwOut = new BufferedWriter(new FileWriter(new File(outFile)))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				bwOut.write(outSentenceSeparator);
				bwOut.newLine();
				final List<Mention> mentions = executable.run(line);
				bwOut.write("##### Number of mentions: " + mentions.size() + " #####");
				bwOut.newLine();
				bwOut.write("####################################");
				bwOut.newLine();
				for (final Mention m : mentions) {
					bwOut.write("Mention[" + m.getMention() + "]");
					bwOut.write(":");
					bwOut.newLine();
					//Output all the possible assignments aka. candidates for this mention
					for (PossibleAssignment pa : m.getPossibleAssignments()) {
						bwOut.write(pa.getAssignment());
						bwOut.newLine();
					}
					bwOut.write(outMentionSeparator);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public LauncherOutputAmbiguity(final EnumModelType KG) {
		this.KG = KG;
	}

	private void init() throws Exception {
		// KG = EnumModelType.DEFAULT;
		if (init)
			return;

		final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG, stopwordsLoader);
		final Map<String, Collection<String>> tmpMap = mpl
				.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		map = new HashMapCaseInsensitive<Collection<String>>();
		// Case-insensitive map implementation
		for (Map.Entry<String, Collection<String>> e : tmpMap.entrySet()) {
			map.put(e.getKey(), e.getValue());
		}

		// ########################################################
		// Mention Detection
		// ########################################################
		final InputProcessor inputProcessor = new InputProcessor(null);
		this.md = new MentionDetectorMap(map, inputProcessor);
		// LauncherInputLinking.md = new MentionDetectorLSH(map, 0.8);
		// ########################################################
		// Candidate Generation
		// ########################################################
		candidateGenerator = new CandidateGeneratorMap(map);
		init = true;
	}

	public List<Mention> run(final String inputLine) {
		try {
			init();
			final List<Mention> mentions = md.detect(inputLine);
			// ########################################################
			// Candidate Generation (update for mentions)
			// ########################################################
			Collections.sort(mentions, new Comparator<Mention>() {
				@Override
				public int compare(Mention o1, Mention o2) {
					// Made so it accepts the smallest match as the used one
					final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
					return (o1.getOffset() == o2.getOffset()) ? diffLength
							: ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
				}
			});
			for (Mention m : mentions) {
				// Update possible assignments
				m.updatePossibleAssignments(candidateGenerator.generate(m));
			}
			return mentions;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
