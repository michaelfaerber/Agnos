package alu.linking.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.loader.MentionPossibilityLoader;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.mentiondetection.exact.MentionDetectorMap;
import alu.linking.mentiondetection.fuzzy.MentionDetectorLSH;

public class DetectionUtils {

	/**
	 * Loads the surface forms for a specified KG along with a StopwordsLoader
	 * instance (which loads the stopwords for the surface forms)
	 * 
	 * @param KG              Knowledge Graph for which to load its associated
	 *                        surface forms
	 * @param stopwordsLoader loads the stopwords which filter out unwanted surface
	 *                        forms
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Collection<String>> loadSurfaceForms(final EnumModelType KG,
			final StopwordsLoader stopwordsLoader) throws IOException {
		final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG, stopwordsLoader);
		Map<String, Collection<String>> map = mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		return map;
	}

	public static MentionDetector setupMentionDetection(final EnumModelType KG, final Map<String, Collection<String>> map,
			final InputProcessor inputProcessor) throws Exception {
		return setupMentionDetection(KG, map, inputProcessor, false);
		//return setupMentionDetection(KG, map, inputProcessor, true);
	}

	public static MentionDetector setupMentionDetection(final EnumModelType KG, final Map<String, Collection<String>> map,
			final InputProcessor inputProcessor, final boolean LSH_OR_MAP) throws Exception {
		Logger.getLogger(DetectionUtils.class).info("Number of entries (aka. different surface forms): " + map.size());
		// return new MentionDetectorMap(map);//
		final MentionDetector md;
		if (LSH_OR_MAP) {
			md = new MentionDetectorLSH(KG, 0.9, inputProcessor);
		} else {
			md = new MentionDetectorMap(map, inputProcessor);
		}
		md.init();
		return md;
	}

	public static void displayMentions(final Logger logger, Collection<Mention> mentions, final boolean detailed) {
		logger.info("Mention Details(" + mentions.size() + "):");
		final TreeMap<String, Mention> alphabeticalSortedMentions = new TreeMap<String, Mention>();
		// Sort them by key for visibility
		for (Mention m : mentions) {
			alphabeticalSortedMentions.put(m.getMention() + "_" + m.getOriginalMention(), m);
		}

		for (Map.Entry<String, Mention> e : alphabeticalSortedMentions.entrySet()) {
			final Mention m = e.getValue();
			if (detailed) {
				logger.info("Mention[" + m.getMention() + "; " + m.getDetectionConfidence() + "] ");
				logger.info("Original Text:" + m.getOriginalMention());
				logger.info("Offset: " + m.getOffset());
				logger.info("Possible assignments: "
						+ (m.getPossibleAssignments() != null ? m.getPossibleAssignments().size() : "None"));
				logger.info("Found assignment: " + m.getAssignment());
				logger.info("Found Assignment's Score: " + m.getAssignment().getScore());
				logger.info("--------------------------------------------------");
			} else {
				logger.info(
						m.getOriginalMention() + "(" + m.getMention() + "; " + m.getDetectionConfidence() + ")\t\t-> "
								+ m.getAssignment().getScore() + ":" + m.getAssignment().getAssignment().toString());
			}
		}
		// Displaying them as ordered...
		// for (Mention m : mentions) {
		// logger.info("Mention(" + m.getOffset() + "): " +
		// m.getOriginalMention());
		// }

	}

	public static String makeURL(final Mention m, final AtomicInteger currIndex, final String resultLine) {
		final StringBuilder hyperlinkMention = new StringBuilder(" <a href=");
		hyperlinkMention.append(m.getAssignment().getAssignment().toString());
		hyperlinkMention.append(">");
		hyperlinkMention.append(m.getMention());
		hyperlinkMention.append("</a> ");

		final StringBuilder hyperlinkMentionOriginal = new StringBuilder(" <a href=");
		hyperlinkMentionOriginal.append(m.getAssignment().getAssignment().toString());
		hyperlinkMentionOriginal.append(">");
		hyperlinkMentionOriginal.append(m.getOriginalMention());
		hyperlinkMentionOriginal.append("</a> ");

		final String search = m.getOriginalMention();
		int foundIndex = resultLine.indexOf(search, currIndex.get());
		String retLine = null;
		try {
			retLine = resultLine.substring(0, foundIndex) + hyperlinkMentionOriginal
					+ resultLine.substring(foundIndex + search.length());
		} catch (StringIndexOutOfBoundsException siooe) {
			Logger.getLogger(DetectionUtils.class).error(currIndex + " - Out of bounds for: " + search);
			Logger.getLogger(DetectionUtils.class).error("Mention:" + m.getMention() + " - " + m.getOffset());
		}
		currIndex.set(foundIndex + search.length());

		return retLine;
	}

}
