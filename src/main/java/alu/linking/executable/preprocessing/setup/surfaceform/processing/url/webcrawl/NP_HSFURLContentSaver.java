package alu.linking.executable.preprocessing.setup.surfaceform.processing.url.webcrawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Numbers;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.webcrawler.Crawler;
import alu.linking.structure.Executable;
import alu.linking.structure.TextProcessor;
import edu.stanford.nlp.ling.TaggedWord;

public class NP_HSFURLContentSaver implements Executable {

	final EnumModelType KG;
	final ExecutorService executorService;
	final LinkedList<ImmutablePair<Future<TextProcessor>, String>> futureQueue = new LinkedList<ImmutablePair<Future<TextProcessor>, String>>();
	private static long fileCounter = 0l;

	public NP_HSFURLContentSaver(final EnumModelType KG) {
		this.executorService = Executors.newFixedThreadPool(Numbers.WEBCRAWLER_CONNECTIONS.val.intValue());
		this.KG = KG;
	}

	@Override
	public void init() {
	}

	public void loopThroughAndProcess(final Crawler crawler, final File[] inFiles, final boolean headLine)
			throws IOException {
		final File outFolder = new File(FilePaths.DIR_OUT_HSFURL.getPath(KG));
		if (!outFolder.exists()) {
			outFolder.getParentFile().mkdirs();
		}
		String lineStr = null;
		final String delim = Strings.QUERY_RESULT_DELIMITER.val;
		// Outputs /which/ URL is saved /where/ to a log file in a CSV-Format, so we can
		// later on look
		// up (with a CSVReader) where each website's content is stored
		try (BufferedWriter bwLog = Files.newBufferedWriter(Paths.get(FilePaths.FILE_OUT_HSFURL_MAPPING.getPath(KG)),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (File inFile : inFiles) {
				try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
//					final FileInParser parser = enumFileTypeIn.parserInClass.newInstance().create(brIn,
//							Strings.QUERY_RESULT_DELIMITER.val);
					if (headLine) {
						brIn.readLine();
//						parser.withHeader(headLine);
					}
//					while ((line = parser.getNext()) != null) {
					while ((lineStr = brIn.readLine()) != null) {
						final List<Object> line = Arrays.asList(lineStr.split(delim));
						final String url = line.get(line.size() - 1).toString();
						final String outPath = FilePaths.DIR_OUT_HSFURL.getPath(KG) + (fileCounter++) + ".txt";

						// Store which file contains which URL's content
						bwLog.write(outPath + Strings.CSV_DELIM.val + url);
						bwLog.newLine();
						// Crawls websites asynchronously
						final Future<TextProcessor> future = executorService
								.submit(crawler.createConnection(url, outPath));
						// futureQueue.add(new ImmutablePair<Future<TextProcessor>, String>(future,
						// outPath));
					}
				}
			}
			executorService.shutdown();

			// Crawling on multiple threads being executed

//			while (futureQueue.size() > 0) {
//				synchronized (futureQueue) {
//					final ImmutablePair<Future<TextProcessor>, String> futurePair = futureQueue.poll();
//					if (!futurePair.getLeft().isDone() && !futurePair.getLeft().isCancelled()) {
//						// It's not done, so put it back into queue
//						futureQueue.add(futurePair);
//						continue;
//					}
//					// .get() and .isDone() ensure that the thread is done grabbing the website's
//					// content
//					final String content = futurePair.getLeft().get().getText();
//					System.out.println("Writing... website");
//					if (content != null && content.length() > 0) {
//						// Write the content to the appropriate file once crawling is completed
//						try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(futurePair.getRight()),
//								StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
//								StandardOpenOption.CREATE)) {
//							// WRITE OUT CONTENT OF CAUGHT WEBSITE
//							bwOut.write(content);
//						}
//					} else {
//						getLogger().warn("No content output to: " + futurePair.getRight());
//					}
//				}
//			}

			// Waits for two weeks... max
			this.executorService.awaitTermination(14L, TimeUnit.DAYS);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	@Override
	public List<TaggedWord> exec(Object... o) {
		if (o != null && o.length == 1) {
			// do nothing at length 1...
			getLogger().error("More than one input parameter required");
		} else if (o != null && (o.length >= 2 && o.length <= 6)) {
			if (o[0] instanceof File) {
				final File inFile = (File) (o[0]);
				try {
					int argC = 2;
					// 2
					final boolean hasHeadLine = o.length >= argC ? (boolean) o[argC - 1] : false;
					argC++;
					// 3 - if needed
					File[] todoFiles = null;
					if (inFile.isFile()) {
						todoFiles = new File[] { inFile };
					} else {
						todoFiles = inFile.listFiles();
					}
					loopThroughAndProcess(new Crawler(), todoFiles, hasHeadLine);
				} catch (IOException e) {
					System.err.println("Could not properly loop through file & process it");
					e.printStackTrace();
				}
			} else {
				System.err.println("Not a file passed.");
			}
		}
		return null;

	}

	@Override
	public boolean destroy() {
		executorService.shutdown();
		return false;
	}
}
