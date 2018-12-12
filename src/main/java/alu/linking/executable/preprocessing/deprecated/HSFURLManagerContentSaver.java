package alu.linking.executable.preprocessing.deprecated;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.ImmutablePair;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Numbers;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.fileparser.EnumFileType;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.output.NxOutputParser;
import alu.linking.preprocessing.webcrawler.Crawler;
import alu.linking.structure.Executable;
import alu.linking.structure.RDFFormatter;
import alu.linking.structure.TextProcessor;
import edu.stanford.nlp.ling.TaggedWord;

public class HSFURLManagerContentSaver implements Executable, RDFFormatter<java.lang.String> {

	final ExecutorService executorService;
	final Queue<ImmutablePair<Future<TextProcessor>, String>> futureQueue = new LinkedList<ImmutablePair<Future<TextProcessor>, String>>();
	private static long fileCounter = 0l;
	private final EnumModelType KG;
	public HSFURLManagerContentSaver(final EnumModelType KG) {
		this.executorService = Executors.newFixedThreadPool(Numbers.WEBCRAWLER_CONNECTIONS.val.intValue());
		this.KG = KG;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @throws IOException
	 */
	public void loopThroughAndProcess(final Crawler crawler, final File inFile, final EnumFileType enumFileTypeIn,
			final boolean headLine) throws IOException {
		final File outFolder = new File(FilePaths.DIR_OUT_HSFURL.getPath(KG));
		if (!outFolder.exists()) {
			outFolder.getParentFile().mkdirs();
		}
		List<Object> line = null;
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			final FileInParser parser = enumFileTypeIn.parserInClass.newInstance().create(brIn);
			if (headLine) {
				parser.withHeader(headLine);
			}
			try (BufferedWriter bwLog = Files.newBufferedWriter(Paths.get(FilePaths.FILE_OUT_HSFURL_MAPPING.getPath(KG)),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				while ((line = parser.getNext()) != null) {
					final String url = line.get(line.size() - 1).toString();
					final String outPath = FilePaths.DIR_OUT_HSFURL.getPath(KG) + (fileCounter++) + ".txt";

					// Store which file contains which URL's content
					// System.out.println(outPath + ";" + url);
					bwLog.write(outPath + Strings.CSV_DELIM.val + url);
					bwLog.newLine();
					// Crawls websites asynchronously
					final Future<TextProcessor> future = executorService.submit(crawler.createConnection(url, false));
					futureQueue.add(new ImmutablePair<Future<TextProcessor>, String>(future, outPath));

				}
			}
			executorService.shutdown();

			// Crawling on multiple threads being executed

			while (futureQueue.size() > 0) {
				synchronized (futureQueue) {
					final ImmutablePair<Future<TextProcessor>, String> futurePair = futureQueue.poll();
					if (!futurePair.getLeft().isDone() && !futurePair.getLeft().isCancelled()) {
						// It's not done, so put it back into queue
						futureQueue.add(futurePair);
						continue;
					}
					final String content = futurePair.getLeft().get().getText();
					if (content != null && content.length() > 0) {
						try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(futurePair.getRight()),
								StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
								StandardOpenOption.CREATE)) {
							// WRITE OUT CONTENT OF CAUGHT WEBSITE
							bwOut.write(content);
						}
					} else {
						// System.err.println("Invalid/Empty content: " + line);
					}
				}
			}

			//

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	@Override
	public List<TaggedWord> exec(Object... o) {
		if (o != null && o.length == 1) {
		} else if (o != null && (o.length >= 2 && o.length <= 6)) {
			if (o[0] instanceof File) {
				final File inFile = (File) (o[0]);
				try {
					int argC = 2;
					// 2
					final EnumFileType enumFileTypeIn = o.length >= argC ? (EnumFileType) o[argC - 1]
							: EnumFileType.CSV;
					argC++;
					// 3
					final boolean hasHeadLine = o.length >= argC ? (boolean) o[argC - 1] : false;

					loopThroughAndProcess(new Crawler(), inFile, enumFileTypeIn, hasHeadLine);
				} catch (IOException e) {
					System.err.println("Could not properly loop through file & process it");
					e.printStackTrace();
				}
			}
		}
		return null;

	}

	@Override
	public boolean destroy() {
		executorService.shutdown();
		return false;
	}

	@Override
	public java.lang.String getExecMethod() {
		return "splitSFtoNounPhrases";
	}

	@Override
	public String format(String subject, String predicate, String object) {
		/*
		 * return new org.semanticweb.yars.nx.Resource(subject).toN3() + " " + new
		 * org.semanticweb.yars.nx.Resource(predicate).toN3() + " " + new
		 * org.semanticweb.yars.nx.Literal(object).toN3() + " .";
		 */
		return new NxOutputParser().format(subject, predicate, object);
	}

}
