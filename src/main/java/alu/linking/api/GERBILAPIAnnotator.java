package alu.linking.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;
import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.internal.Lists;

import alu.linking.candidategeneration.CandidateGenerator;
import alu.linking.candidategeneration.CandidateGeneratorMap;
import alu.linking.config.constants.Comparators;
import alu.linking.config.kg.EnumModelType;
import alu.linking.disambiguation.AssignmentChooser;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.postprocessing.MentionPruner;
import alu.linking.postprocessing.ThresholdPruner;
import alu.linking.structure.Executable;
import alu.linking.utils.DetectionUtils;
import alu.linking.utils.Stopwatch;

public class GERBILAPIAnnotator implements Executable {

	private final EnumModelType KG;

	private final String chooserWatch = "Scorer (Watch)";
	private final String detectionWatch = MentionDetector.class.getName();
	private final String linking = "Linking (Watch)";
	private final boolean REMOVE_OVERLAP = false;
	private final boolean PROCESS_JUST_MARKINGS = false;
	private static final boolean detailed = false;
	// No touchy
	private Boolean init = false;
	private final Comparator<Mention> offsetComparator = Comparators.mentionOffsetComp;

	private Set<String> stopwords = null;
	private MentionDetector md = null;
	private CandidateGenerator<String> candidateGenerator = null;
	private AssignmentChooser chooser = null;
	private MentionPruner pruner = null;

	public GERBILAPIAnnotator(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	@SuppressWarnings("unused")
	public void init() {
		if (false) {
			return;
		}
		synchronized (this.init) {
			if (this.init)
				return;
		}
		// Load all the necessary stuff
		// such as embeddings, LSH sparse vectors and hashes
		try {
			getLogger().info("Initializing Framework Structures");
			getLogger().info("Loading mention possibilities...");
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			this.stopwords = stopwordsLoader.getStopwords();
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(this.KG, stopwordsLoader);
			final InputProcessor inputProcessor = new InputProcessor(stopwords);
			// ########################################################
			// Mention Detection
			// ########################################################
			this.md = DetectionUtils.setupMentionDetection(KG, map, inputProcessor);

			// ########################################################
			// Candidate Generator
			// ########################################################
			this.candidateGenerator = new CandidateGeneratorMap(map);
			Stopwatch.endOutputStart(getClass().getName());
			// Initialise AssignmentChooser
			Stopwatch.start(chooserWatch);
			this.chooser = new AssignmentChooser(this.KG);
			Stopwatch.endOutput(chooserWatch);
			this.pruner = new ThresholdPruner(1.0d);
			init = true;
		} catch (Exception exc) {
			getLogger().error("Exception during init", exc);
		}
	}

	@SuppressWarnings("unused")
	public String annotate(final InputStream inputStream) {
		if (false) {
			try (final BufferedReader brIn = new BufferedReader(new InputStreamReader(inputStream))) {
				String line = null;
				getLogger().info("Input from GERBIL - START:");
				while ((line = brIn.readLine()) != null) {
					getLogger().error(line);
				}
				getLogger().info("Input from GERBIL - END");
			} catch (IOException e) {
				getLogger().error("IOException", e);
			}
			return "";
		}

		// 1. Generate a Reader, an InputStream or a simple String that contains the NIF
		// sent by GERBIL
		// 2. Parse the NIF using a Parser (currently, we use only Turtle)
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			document = parser.getDocumentFromNIFStream(inputStream);
		} catch (Exception e) {
			getLogger().error("Exception while reading request.", e);
			return "";
		}

		return annotate(document);
	}

	public String annotate(final String input) {
		// Parse the NIF using a Parser (currently, we use only Turtle)
		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			document = parser.getDocumentFromNIFString(input);
		} catch (Exception e) {
			getLogger().error("Exception while reading request.", e);
			return "";
		}
		return annotate(document);
	}

	private String annotate(final Document document) {
		// In case it hasn't been initialised yet
		init();
		final int MIN_MARKINGS = 1;

		final String text;

		// Ordered markings required to reconciliate the markings' detected mentions'
		// offsets
		List<Marking> orderedMarkings = getSortedMarkings(document);
		// Whether to process just for the markings or all the entire document
		if (PROCESS_JUST_MARKINGS) {
			// Gets the markings - if there are any, it will annotate with them (making it a
			// lot faster),
			// otherwise it will fall back to processing the plain text (e.g. in the case
			// GERBIL does not actually pass markings text)

			if (orderedMarkings != null && orderedMarkings.size() > 0 // && orderedMarkings.size() > MIN_MARKINGS
			) {
				text = markingsToText(document, orderedMarkings);
				getLogger().info("Using [Markings]:" + smallText(text));
			} else {
				text = document.getText();
				orderedMarkings = null;
				getLogger().info("No markings; Using [plain text]:" + smallText(text));
			}
		} else {
			// Processes the entire input text
			text = document.getText();
			getLogger().info("Using [plain text]:" + smallText(text));
			orderedMarkings = null;
		}

		// 3. use the text and maybe some Markings sent by GERBIL to generate your
		// Markings
		// (a.k.a annotations) depending on the task you want to solve
		// 4. Add your generated Markings to the document
		try {
			document.setMarkings(new ArrayList<Marking>(annotateSafely(text, orderedMarkings, document.getText())));
		} catch (InterruptedException ie) {
			getLogger().error("Exception while annotating.", ie);
			return "";
		}
		// 5. Generate a String containing the NIF and send it back to GERBIL
		final TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
		final String nifDocument = creator.getDocumentAsNIFString(document);
		return nifDocument;
	}

	private String smallText(String text) {
		final int length = 50;
		final StringBuilder sb = new StringBuilder(text.substring(0, Math.min(text.length(), length)));
		if (text.length() > length) {
			sb.append("[...] (" + text.length() + ")");
		}
		return sb.toString();
	}

	private String markingsToText(final Document document, final List<Marking> markings) {
		final String text = document.getText();
		final StringBuilder sbInText = new StringBuilder();
		for (Marking mark : markings) {
			if (mark instanceof Span) {
				final Span span = ((Span) mark);
				sbInText.append(text.substring(span.getStartPosition(), span.getStartPosition() + span.getLength()));
				// Separate each by a space character
				sbInText.append(" ");
			}
		}
		return sbInText.toString();
	}

	private List<Marking> getSortedMarkings(Document document) {
		// Copy the markings so we can sort them
		List<Marking> markings = Lists.newArrayList(document.getMarkings());
		Collections.sort(markings, Comparators.markingsOffsetComp);
		return markings;
	}

	private Collection<? extends Marking> annotateSafely(final String text, final List<Marking> markings,
			final String origText) throws InterruptedException {
		final List<Marking> retList = Lists.newArrayList();
		// new ScoredNamedEntity(startPosition, length, uris, confidence);
		// new Mention()... transform mention into a scored named entity

		final List<Mention> mentions = linking(text);

		if (markings != null) {
			// If we're just using markings, we need to readjust the offsets for the output
			fixMentionMarkingOffsets(mentions, markings, origText);
		}

		// Transform mentions into GERBIL's wanted markings
		for (Mention mention : mentions) {
			retList.add(new ScoredNamedEntity(mention.getOffset(), mention.getOriginalMention().length(),
					mention.getAssignment().toString(), mention.getAssignment().getScore().doubleValue()));
		}
		return retList;
	}

	private void fixMentionMarkingOffsets(final List<Mention> mentions, final List<Marking> markings,
			final String origText) {
		final Map<String, List<Mention>> mentionsMap = new HashMap<>();
		// Adds mentions to a map by the original text surface form
		for (Mention mention : mentions) {
			List<Mention> mentionsSameSF;
			if ((mentionsSameSF = mentionsMap.get(StringUtils.stripEnd(mention.getOriginalMention(), null))) == null) {
				mentionsSameSF = Lists.newArrayList();
				mentionsMap.put(StringUtils.stripEnd(mention.getOriginalMention(), null), mentionsSameSF);
			}
			mentionsSameSF.add(mention);
		}

		for (Map.Entry<String, List<Mention>> e : mentionsMap.entrySet()) {
			int textIndex = 0;
			int counter = 0;
			while (// (textIndex < (origText.length() - 1)) &&
			(textIndex = origText.indexOf(e.getKey(), textIndex)) != -1) {
				if (e.getValue().size() <= counter) {
					// this can be the case when only one mention contains a marking (within a
					// string containing another such mention)

					// Incrementing counter for the ensuing error message logic
					counter++;
					break;
				}
				e.getValue().get(counter).setOffset(textIndex);
				// Advance it by one so it's not forever stuck on the same one
				textIndex++;
				counter++;
			}
			if (counter < e.getValue().size()) {
				// Didn't work out, possibly due to the combined detected mention not existing
				// in the original text, e.g. when only markings are used, it concatenates the
				// markings into a text... which can be meh
				getLogger().error("Missed mentions... Counter(" + counter + "): Value(" + e.getValue()
						+ "): Orig. Mention(" + e.getKey() + "): Text:" + origText);
			}
		}
	}

	private List<Mention> linking(String text) throws InterruptedException {
		List<Mention> mentions = null;

		Stopwatch.start(linking);
		Stopwatch.start(detectionWatch);
		// ########################################################
		// Mention Detection
		// ########################################################
		mentions = md.detect(text);

		// ------------------------------------------------------------------------------
		// Change the offsets due to stopword-removal applied through InputProcessor
		// modifying the actual input, therewith distorting it greatly
		// ------------------------------------------------------------------------------
		// fixMentionOffsets(text, mentions);

		// getLogger().info("Detected [" + mentions.size() + "] mentions.");
		// getLogger().info("Detection duration: " +
		// Stopwatch.endDiffStart(detectionWatch) + " ms.");

		// ########################################################
		// Candidate Generation (update for mentions)
		// ########################################################
		Collections.sort(mentions, offsetComparator);
		for (Mention m : mentions) {
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
		// Display them
		// DetectionUtils.displayMentions(getLogger(), mentions, true);
		Stopwatch.endOutput(linking);

		// ########################################################
		// Pruning
		// ########################################################
		getLogger().info("Before Pruning:" + mentions);
		mentions = this.pruner.prune(mentions);
		getLogger().info("After Pruning:" + mentions);

		return mentions;
	}

	/**
	 * Fixes the mentions' offsets
	 * 
	 * @param text     input text
	 * @param mentions detected mentions
	 */
	private void fixMentionOffsets(final String text, final List<Mention> mentions) {
		Map<String, List<Integer>> multipleMentions = new HashMap<>();
		final String textLowercase = text.toLowerCase();
		for (Mention mention : mentions) {
			// If there's multiple, let the earlier offset be the earlier indexOf
			final String surfaceForm = mention.getOriginalMention().toLowerCase();
			final int index = textLowercase.indexOf(surfaceForm);
			if (textLowercase.indexOf(surfaceForm, index + 1) == -1) {
				// There is no other such surface form within the input, so just update with the
				// found index
				mention.updateOffset(index);
			} else {
				List<Integer> indices;
				if ((indices = multipleMentions.get(surfaceForm)) == null) {
					indices = Lists.newArrayList();
					multipleMentions.put(surfaceForm, indices);
				}
				indices.add(mention.getOffset());
			}
		}
		// Now go through the map for the multiple mentions
		for (Mention mention : mentions) {
			final String surfaceForm = mention.getOriginalMention().toLowerCase();
			final List<Integer> indices;
			if (surfaceForm.length() == 0) {
				continue;
			}

			if ((indices = multipleMentions.get(surfaceForm)) == null) {
				continue;
			}
			// Sorts it redundantly quite a few times... but myah, honestly, it's just a few
			// values
			Collections.sort(indices);

			final int rank = indices.indexOf(mention.getOffset());
			if (rank < 0) {
				getLogger().error("Failed logic on the offset rank[" + rank + "] for indices[" + indices + "]: mention["
						+ mention.getOriginalMention() + "]");
			}
			int currIndex = -1;
			for (int i = 0; i < rank;) {
				currIndex = textLowercase.indexOf(surfaceForm, currIndex + 1);
			}
			if (currIndex < 0) {
				getLogger().error("Failed logic on the mentions' offset updating");
			}
			mention.updateOffset(currIndex);
		}
	}

	/**
	 * Removes smallest overlapping mentions from the list of mentions
	 * 
	 * @param mentions list of mentions
	 */
	private void removeOverlapping(List<Mention> mentions) {
		// #####################################################################
		// Remove smallest conflicting mentions keeping just the longest ones
		// #####################################################################
		Set<Mention> toRemoveMentions = new HashSet<>();
		for (int i = 0; i < mentions.size(); ++i) {
			for (int j = i + 1; j < mentions.size(); ++j) {
				final Mention leftMention = mentions.get(i);
				final Mention rightMention = mentions.get(j);
				// If they conflict, add the shorter one to a list to be removed
				if (leftMention.overlaps(rightMention)) {
					// Remove smaller one
					final int mentionLenDiff = leftMention.getMention().length() - rightMention.getMention().length();
					final boolean sameFinalMention = rightMention.getMention().equals(leftMention.getMention());
					if (mentionLenDiff > 0) {
						// If they have the same mention, remove the longer one, otherwise the shorter
						// one
						if (sameFinalMention) {
							toRemoveMentions.add(leftMention);
						} else {
							// Left is bigger, so remove right
							toRemoveMentions.add(rightMention);
						}
					} else {
						// If they have the same mention, remove the longer one, otherwise the shorter
						// one -> idea is to remove noise/stopwords as much as possible
						if (sameFinalMention) {
							toRemoveMentions.add(rightMention);
						} else {
							// Right is bigger or EQUAL to left; if equal, it doesn't matter which...
							// -> remove left
							toRemoveMentions.add(leftMention);
						}
					}
				}
			}
		}
		final Set<String> removed = new HashSet<>();
		int counter = 0;
		for (Mention toRemove : toRemoveMentions) {
			mentions.remove(toRemove);
			removed.add(toRemove.getMention() + " - " + toRemove.getOriginalMention());
			counter++;
		}
		getLogger().info("Removed [" + counter + "/" + mentions.size() + "] mentions:'" + removed + "'");

	}

	@Override
	public String exec(Object... o) throws Exception {
		if (o != null && o.length > 0) {
			InputStream inputReader = null;
			for (Object obj : o) {
				if (obj instanceof InputStream) {
					inputReader = (InputStream) obj;
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
