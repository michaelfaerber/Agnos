package alu.linking.api;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;
import org.semanticweb.yars.nx.Node;

import com.beust.jcommander.internal.Lists;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.structure.Executable;
import alu.linking.utils.DetectionUtils;
import alu.linking.utils.Stopwatch;

public class GERBILAPIAnnotator implements Executable {

	private final EnumModelType KG;
	private final String chooserWatch = "chooser - init (loads graph)";
	private final String detectionWatch = MentionDetector.class.getName();
	private final String iterationWatch = "iteration";
	private final boolean REMOVE_OVERLAP = true;
	private static final boolean detailed = false;
	private final Comparator<Mention<Node>> offsetComparator = new Comparator<Mention<Node>>() {
		@Override
		public int compare(Mention<Node> o1, Mention<Node> o2) {
			// Made so it accepts the smallest match as the used one
			final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
			return (o1.getOffset() == o2.getOffset()) ? diffLength : ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
		}
	};

	private AssignmentChooser<Node> chooser = null;
	private Set<String> stopwords = null;
	private MentionDetector md = null;
	private CandidateGenerator<Node> candidateGenerator;

	public GERBILAPIAnnotator(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public void init() {
		// Load all the necessary stuff
		// such as embeddings, LSH sparse vectors and hashes
		try {
			getLogger().info("Initializing entity linking framework");
			getLogger().info("Loading mention possibilities...");
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			stopwords = stopwordsLoader.getStopwords();
			final Map<String, Set<String>> map = DetectionUtils.loadSurfaceForms(this.KG, stopwordsLoader);
			// ########################################################
			// Mention Detection
			// ########################################################
			md = DetectionUtils.setupMentionDetection(KG, map);

			// ########################################################
			// Candidate Generator
			// ########################################################
			candidateGenerator = new CandidateGeneratorMap(map);
			Stopwatch.endOutputStart(getClass().getName());
			System.out.println("Started detection!");
			// In order to check if exec duration depends on one-time loading
			// or whether it will 'always' be this approx. speed for this case
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);
			chooser = new AssignmentChooser<Node>(this.KG);
			Stopwatch.endOutput(chooserWatch);
			String inputLine = null;
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	public String annotate(final Reader inputReader) {
		// 1. Generate a Reader, an InputStream or a simple String that contains the NIF
		// sent by GERBIL
		// 2. Parse the NIF using a Parser (currently, we use only Turtle)
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			document = parser.getDocumentFromNIFReader(inputReader);
		} catch (Exception e) {
			getLogger().error("Exception while reading request.", e);
			return "";
		}
		// 3. use the text and maybe some Markings sent by GERBIL to generate your
		// Markings
		// (a.k.a annotations) depending on the task you want to solve
		// 4. Add your generated Markings to the document
		try {
			document.setMarkings(new ArrayList<Marking>(annotateSafely(document)));
		} catch (InterruptedException ie) {
			getLogger().error("Exception while annotating.", ie);
			return "";
		}
		// 5. Generate a String containing the NIF and send it back to GERBIL
		final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
		final String nifDocument = creator.getDocumentAsNIFString(document);
		return nifDocument;
	}

	private Collection<? extends Marking> annotateSafely(Document document) throws InterruptedException {
		final List<Marking> retList = Lists.newArrayList();
		final String text = document.getText();
		// new ScoredNamedEntity(startPosition, length, uris, confidence);
		// new Mention()... transform mention into a scored named entity

		final List<Mention<Node>> mentions = linking(text);

		// Transform mentions into GERBIL's wanted markings
		for (Mention<Node> mention : mentions) {
			retList.add(new ScoredNamedEntity(mention.getOffset(), mention.getOriginalMention().length(),
					mention.getAssignment().toString(), mention.getAssignment().getScore().doubleValue()));
		}
		return retList;
	}

	private List<Mention<Node>> linking(String text) throws InterruptedException {
		List<Mention<Node>> mentions = null;

		Stopwatch.start(iterationWatch);
		Stopwatch.start(detectionWatch);
		mentions = md.detect(InputProcessor.combineProcessedInput(InputProcessor.process(text, stopwords)));
		System.out.println("Detected [" + mentions.size() + "] mentions.");
		System.out.println("Detection duration: " + Stopwatch.endDiffStart(detectionWatch) + " ms.");

		// ########################################################
		// Candidate Generation (update for mentions)
		// ########################################################
		Collections.sort(mentions, offsetComparator);
		for (Mention<Node> m : mentions) {
			// Update possible assignments w/ possible candidates
			m.updatePossibleAssignments(candidateGenerator.generate(m));
		}

		if (REMOVE_OVERLAP) {
			removeOverlapping(mentions);
		}

		// ########################################################
		// Disambiguation
		// ########################################################
		Stopwatch.start(chooserWatch);
		chooser.choose(mentions);
		getLogger().info("Disambiguation duration: " + Stopwatch.endDiff(chooserWatch) + " ms.");
		Stopwatch.endOutput(getClass().getName());
		getLogger().info("#######################################################");
		getLogger().info("Mention Details(" + mentions.size() + "):");
		// Display them
		DetectionUtils.displayMentions(getLogger(), mentions, true);
		Stopwatch.endOutput(iterationWatch);
		return mentions;
	}

	/**
	 * Removes smallest overlapping mentions from the list of mentions
	 * 
	 * @param mentions list of mentions
	 */
	private void removeOverlapping(List<Mention<Node>> mentions) {
		// #####################################################################
		// Remove smallest conflicting mentions keeping just the longest ones
		// #####################################################################
		List<Mention<Node>> toRemoveMentions = Lists.newArrayList();
		for (int i = 0; i < mentions.size(); ++i) {
			for (int j = i + 1; j < mentions.size(); ++j) {
				final Mention<Node> leftMention = mentions.get(i);
				final Mention<Node> rightMention = mentions.get(j);
				// If they conflict, add the shorter one to a list to be removed
				if (leftMention.getMention().contains(rightMention.getMention())) {
					toRemoveMentions.add(rightMention);
				} else if (rightMention.getMention().contains(leftMention.getMention())) {
					toRemoveMentions.add(leftMention);
				}
			}
		}
		for (Mention<Node> toRemove : toRemoveMentions) {
			getLogger().info("Removing mention for:'" + toRemove.getMention() + "'");
			mentions.remove(toRemove);
		}
	}

	@Override
	public String exec(Object... o) throws Exception {
		if (o != null && o.length > 0) {
			Reader inputReader = null;
			for (Object obj : o) {
				if (obj instanceof Reader) {
					inputReader = (Reader) obj;
					break;
				}
			}
			if (inputReader != null) {
				return annotate(inputReader);
			}
		}
		return null;
	}

	@Override
	public boolean destroy() {
		// Tear down all the loaded data structures
		return false;
	}
}
