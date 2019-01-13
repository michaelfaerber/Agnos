package alu.linking.preprocessing.webcrawler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import alu.linking.structure.Loggable;

public class CrawlerConnectionOutput extends CrawlerConnection implements Loggable {

	private final String outPath;

	public CrawlerConnectionOutput(final String url, final String outPath) {
		super(url);
		this.outPath = outPath;
	}

	@Override
	public CrawlerConnection call() throws IOException {
		super.call();
		final String content = getText();
		if (content != null && content.length() > 0) {
			// Write the content to the appropriate file once crawling is completed
			try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(this.outPath), StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
				// WRITE OUT CONTENT OF CAUGHT WEBSITE
				bwOut.write(content);
			}
		} else {
			getLogger().warn("No content output to: " + this.outPath);
		}

		return this;
	}
}
