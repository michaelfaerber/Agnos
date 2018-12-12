package alu.linking.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.semanticweb.yars.nx.Node;

import alu.linking.mentiondetection.Mention;

public class MentionUtils {
	/**
	 * Formats passed mentions in a specific way
	 * 
	 * @param mentions  list of scored mentions
	 * @param inputLine input line that was linked
	 * @return formatted string output
	 * @throws IOException
	 */
	public static String formatMentionsXML(List<Mention<Node>> mentions, final String inputLine) throws IOException {
		// mention\tstartoffset\tendoffset\tentityURI\tconfscore\n
		final StringWriter sw = new StringWriter();
		try (BufferedWriter bwResults = new BufferedWriter(sw)) {
			bwResults.write("<mentions>");
			// for (Map.Entry<String, Mention<Node>> e : sortedMentions.entrySet()) {
			int currIndex = -1;
			for (Mention<Node> m : mentions) {
				final String search = m.getOriginalMention();
				int foundIndex = inputLine.indexOf(search, currIndex);
				final String mention_text = "<mention><source>" + m.getMention() + "</source><original>"
						+ m.getOriginalMention() + "</original><assignment>" + m.getAssignment()
						+ "</assignment><offset>" + foundIndex + "</offset>" + "</mention>";
				currIndex = foundIndex + search.length();
				bwResults.write(mention_text);
				bwResults.newLine();
			}
			bwResults.write("</mentions>");
			bwResults.flush();
		}
		return sw.toString();
	}

	/**
	 * Formats passed mentions in a specific way
	 * 
	 * @param mentions  list of scored mentions
	 * @param inputLine input line that was linked
	 * @return formatted string output
	 * @throws IOException
	 */
	public static String formatMentionsTabbedLines(List<Mention<Node>> mentions, final String inputLine)
			throws IOException {
		// mention\tstartoffset\tendoffset\tentityURI\tconfscore\n
		final String delim = "\t";
		final String lineSep = "\n";
		final StringWriter sw = new StringWriter();
		try (BufferedWriter bwResults = new BufferedWriter(sw)) {
			int currIndex = -1;
			for (Mention<Node> m : mentions) {
				final String search = m.getOriginalMention();
				int foundIndex = inputLine.indexOf(search, currIndex);
				final String mention_text = m.getMention() + delim + m.getOriginalMention() + delim + foundIndex + delim + (foundIndex + (m.getOriginalMention().length())) + delim
						+ m.getAssignment() + delim + m.getAssignment().getScore() + lineSep;
				currIndex = foundIndex + search.length();
				bwResults.write(mention_text);
			}
			bwResults.flush();
		}
		return sw.toString();
	}
}
