package alu.linking.launcher.eval.cb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.beust.jcommander.internal.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.webcrawler.CrawlerConnection;

public class LauncherDownloadCBNews {

	static BufferedWriter bwError;
	static BufferedWriter bwProgress;
	static BufferedWriter bwIgnore;
	static final EnumModelType KG = EnumModelType.CRUNCHBASE2;

	public static void main(String[] args) throws FileNotFoundException, IOException {
		final int numThreads = 2000;
		final ExecutorService es = Executors.newFixedThreadPool(numThreads);
		bwError = new BufferedWriter(new FileWriter(FilePaths.LOG_FILE_CRAWL_CB_ERROR.getPath(KG), false));
		bwProgress = new BufferedWriter(new FileWriter(FilePaths.LOG_FILE_CRAWL_CB_PROGRESS.getPath(KG), false));
		bwIgnore = new BufferedWriter(new FileWriter(FilePaths.LOG_FILE_CRAWL_CB_IGNORE.getPath(KG), false));
		final String locNewsDump = FilePaths.FILE_CB_NEWS_URL.getPath(KG);
		// Offset in case some were already completed
		final int startAt = 0;
		try (BufferedReader brIn = new BufferedReader(new FileReader(new File(locNewsDump)))) {
			final String sep = Strings.NEWS_URL_SEP.val;
			String line = null;
			int currIndex = 0;
			while ((line = brIn.readLine()) != null) {
				if (currIndex >= startAt) {
					// NEWS<sep>URL
					final String[] tokens = line.split(sep);
					final String url = tokens[1];
					final int index = currIndex;
					es.submit(new Runnable() {
						@Override
						public void run() {
							try {
								outputBodyLinks(crawl(url, index), index);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				}
				currIndex++;
			}
		}
	}

	private static void outputBodyLinks(CrawlerConnection crawl, final int number) throws IOException {
		// Get body
		String body = null;
		final List<String> links = Lists.newArrayList();
		body = crawl.getText();
		// Get links from body
		links.addAll(crawl.getLinks());
		if (links.size() == 0 || body == null || body.length() == 0) {
			bwIgnore.append(number + "T:" + (body == null ? 0 : body.length()) + "/ L:" + links.size());
			bwIgnore.newLine();
			return;
		}
		final String dirNewsBody = FilePaths.DIR_NEWS_BODY.getPath(KG);
		final String dirNewsLinks = FilePaths.DIR_NEWS_LINKS.getPath(KG);
		// Output body
		try (final BufferedWriter outBody = new BufferedWriter(new FileWriter(dirNewsBody + "/" + number + ".txt"))) {
			outBody.append(body);
			outBody.newLine();
		}
		// Output links
		try (final BufferedWriter outLinks = new BufferedWriter(new FileWriter(dirNewsLinks + "/" + number + ".txt"))) {
			for (String link : links) {
				outLinks.append(link);
				outLinks.newLine();
			}
		}
	}

	/**
	 * Crawls and outputs the URL
	 * 
	 * @param url
	 */
	private static CrawlerConnection crawl(final String url, final int number) {
		final CrawlerConnection crawlerConn = new CrawlerConnection(url, true);
		try {
			// Crawl
			crawlerConn.call();
		} catch (IOException ioe) {
			// Failure! :(
			try {
				bwError.append(String.valueOf(number) + " - " + ioe.getLocalizedMessage());
				bwError.newLine();
				bwError.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Success! :)
		try {
			bwProgress.append(
					number + " (T:" + crawlerConn.getText().length() + " / URL:" + crawlerConn.getLinks().size() + ")");
			bwProgress.newLine();
			bwProgress.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return crawlerConn;
	}
}
