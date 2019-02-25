package alu.linking.api.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.apache.log4j.Logger;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

public class LauncherMarkingTester {

	public static void main(String[] args) {
		try {
			new LauncherMarkingTester().annotate(new FileInputStream(new File("./evaluation/split_log/45.log")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public String annotate(final InputStream inputStream) {
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
		final Set<String> diffMarkings = new HashSet<>();
		int markingCounter = 0;
		final String text = document.getText();
		final StringBuilder sbInText = new StringBuilder();
		// Copy the markings so we can sort them
		List<Marking> markings = Lists.newArrayList(document.getMarkings());
		Collections.sort(markings, new Comparator<Marking>() {

			@Override
			public int compare(Marking o1, Marking o2) {
				Span spanLeft = null, spanRight = null;
				if (o1 instanceof Span) {
					spanLeft = (Span) o1;
				}

				if (o2 instanceof Span) {
					spanRight = (Span) o2;
				}

				if (spanLeft == null || spanRight == null) {
					return 0;
				}

				return spanLeft.getStartPosition() - spanRight.getStartPosition();
			}
		});
		for (Marking mark : markings) {
			if (mark instanceof Span) {
				final Span span = ((Span) mark);
				sbInText.append(text.substring(span.getStartPosition(), span.getStartPosition() + span.getLength())
						+ "(" + span.getStartPosition() + ")");
				sbInText.append(" ");
			}
			diffMarkings.add(mark.getClass().getName());
			markingCounter++;
		}
		System.out.println("Unique markings(" + diffMarkings.size() + "): " + diffMarkings);
		System.out.println("Total markings:" + markingCounter);
		System.out.println("Chopped input: " + sbInText.toString());
		System.out.println("Actual input: "+text);
		return "";
	}

	private static Logger getLogger() {
		return Logger.getLogger(LauncherMarkingTester.class);
	}
}
