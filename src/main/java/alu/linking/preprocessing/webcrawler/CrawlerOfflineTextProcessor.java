package alu.linking.preprocessing.webcrawler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.fileparser.EnumFileType;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.structure.TextProcessor;

public class CrawlerOfflineTextProcessor implements TextProcessor {

	private static final DualHashBidiMap<String, String> mapping = new DualHashBidiMap<String, String>();
	private final String urlId;
	private String text = null;
	private boolean callStarted = false;
	private boolean callFinish = false;

	public CrawlerOfflineTextProcessor(final EnumModelType KG, final String url) {
		// Load the URL-File Mapping if not yet loaded
		synchronized (mapping) {
			if (mapping.size() == 0) {
				System.out.println("Populating map!");
				// Load the mapping from the appropriate file
				try (final FileInParser in = EnumFileType.CSV.parserInClass.newInstance()
						.create(new FileReader(FilePaths.FILE_OUT_HSFURL_MAPPING.getPath(KG)), ' ')) {
					List<String> tokens = null;
					while ((tokens = in.getNext()) != null) {
						if (tokens.size() == 2) {
							// path;url
							mapping.put(tokens.get(0), tokens.get(1));
						} else {
							System.err.println("[File-URL Mapping] Invalid tokens: " + tokens);
						}
					}
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Set the URL of this text processor
		this.urlId = url;
	}

	@Override
	public String getText() throws IOException {
		if (!callFinish && !callStarted) {
			call();
		} else if (!callFinish && callStarted) {
			System.err.println("Called at an inopportune time...");
		}
		return text;
	}

	@Override
	public CrawlerOfflineTextProcessor call() throws IOException {
		callStarted = true;
		// Go through the file to grab the information we need
		final String filepath = mapping.getKey(urlId);
		if (filepath != null) {
			final File urlFile = new File(filepath);
			StringBuilder sb = new StringBuilder("");
			if (urlFile.exists()) {
				// Read from the file
				Files.lines(Paths.get(filepath)).forEach(s -> sb.append(s + " "));
				this.text = sb.toString();
			} else {
				this.text = null;
			}
		} else {
			System.err.println("Mapping was not found for URL: " + urlId);
			this.text = null;
		}
		callFinish = true;
		return this;
	}

}
