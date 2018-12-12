package alu.linking.executable.preprocessing.setup.surfaceform.processing.url;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.ImmutableTriple;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Numbers;
import alu.linking.config.constants.Objects;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.preprocessing.setup.surfaceform.processing.NP_HSFManager;
import alu.linking.preprocessing.fileparser.EnumFileType;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.output.NxOutputParser;
import alu.linking.preprocessing.fileparser.output.OutParser;
import alu.linking.preprocessing.webcrawler.Crawler;
import alu.linking.structure.Executable;
import alu.linking.structure.RDFFormatter;
import alu.linking.structure.TextProcessor;
import edu.stanford.nlp.ling.TaggedWord;

public class NP_HSFURLManager implements Executable, RDFFormatter<java.lang.String> {

	final ExecutorService executorService;
	final Queue<ImmutableTriple<String, Future<TextProcessor>, Long>> futureQueue = new LinkedList<ImmutableTriple<String, Future<TextProcessor>, Long>>();
	final EnumModelType KG;

	public NP_HSFURLManager(final EnumModelType KG) {
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

	public void loopThroughAndProcess(final File inFile, final File outFile) throws IOException {
		loopThroughAndProcess(inFile, outFile, EnumFileType.CSV, EnumFileType.CSV, false, false);
	}

	/**
	 * Takes an input file and parses it with given EnumFileType's associated input
	 * parser (set through enumFileTypeIn), goes through each entry and applies
	 * NPComplete on it for tag extraction. Once that is done, all tags are output
	 * via set EnumFileType's outputparser with enumFileTypeOut
	 * 
	 * @param inFile
	 * @param outFile
	 * @throws IOException
	 */
	public void loopThroughAndProcess(final File inFile, final File outFile, final EnumFileType enumFileTypeIn,
			final EnumFileType enumFileTypeOut, final boolean append, final boolean headLine) throws IOException {
		if (!outFile.exists()) {
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdirs();
			}
			outFile.createNewFile();
		}
		List<Object> line = null;
		// Output logging
		final PrintStream defOut = System.out;
		final PrintStream logFileOut = new PrintStream(
				new BufferedOutputStream(new FileOutputStream(new File(FilePaths.LOG_FILE_WEB_CRAWLING.getPath(KG)))));
		final PrintStream ignoreStream = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {// Ignore
			}
		}) {

			@Override
			public void println(String x) {// Ignore
			}

			@Override
			public void print(String s) {// Ignore
			}
		};
		System.setOut(logFileOut);

		// Error Logging
		final PrintStream defErr = System.err;
		final PrintStream logFileError = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(new File(FilePaths.LOG_FILE_ERROR_WEB_CRAWLING.getPath(KG)))));
		System.setErr(logFileError);

		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			final FileInParser parser = enumFileTypeIn.parserInClass.newInstance().create(brIn);
			try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(outFile.getPath()), StandardOpenOption.WRITE,
					append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE)) {
				parser.withHeader(headLine);
				while ((line = parser.getNext()) != null) {
					final String subject = parser.toFormatString(line.get(0));
					final String url = line.get(line.size() - 1).toString();

					// Crawls websites asynchronously
					final Future<TextProcessor> future = executorService
							.submit(Objects.CONNECTION_OR_OFFLINE.createTextProcess(url));
					futureQueue.add(new ImmutableTriple<String, Future<TextProcessor>, Long>(subject, future,
							System.currentTimeMillis()));
				}
				executorService.shutdown();
				// Crawling on multiple threads being executed
				final OutParser outParser = enumFileTypeOut.parserOutClass.newInstance();
				outParser.setHTML(true);
				synchronized (futureQueue) {
					while (futureQueue.size() > 0) {
						final ImmutableTriple<String, Future<TextProcessor>, Long> futurePair = futureQueue.poll();
						if (!futurePair.getMiddle().isDone()) {
							// It's not done, so put it back into queue
							futureQueue.add(futurePair);
							continue;
						}
						// Waits until thread is done, grabs the resulting TextProcessor and takes the
						// text from it
						final String toNounPhrase = futurePair.getMiddle().get().getText();
						final String subject = futurePair.getLeft();
						if (toNounPhrase != null && toNounPhrase.length() > 0) {
							final List<TaggedWord> tags = NP_HSFManager.splitTextToNounPhrases(toNounPhrase);
							if (tags != null) {
								// One tag per line
								for (TaggedWord tag : tags) {
									if (tag.value() != null && tag.value().length() > 0) {
										// Output each tag appropriately
										final String outString = outParser.format(subject,
												Strings.PRED_HELPING_SURFACE_FORM.val, tag.value());
										if (outString != null && outString.length() > 0) {
											bwOut.write(outString);
											bwOut.newLine();
										}
									}
								}

							}
						} else {
							System.err.println("Invalid/Empty content: " + line);
						}
					}
				}

				//

			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			logFileOut.close();
			logFileError.close();
			System.setOut(defOut);
			System.setErr(defErr);
		}
	}

	@Override
	public List<TaggedWord> exec(Object... o) {
		if (o != null && o.length == 1) {
			return NP_HSFManager.splitTextToNounPhrases(new Crawler().crawlSimple(o[0].toString()));
			// return tripleToHelpingSurfaceForm(o[0].toString());
		} else if (o != null && (o.length >= 2 && o.length <= 6)) {
			if (o[0] instanceof File && o[1] instanceof File) {
				final File inFile = (File) (o[0]);
				final File outFile = (File) (o[1]);
				try {
					int argC = 3;
					// 3
					final EnumFileType enumFileTypeIn = o.length >= argC ? (EnumFileType) o[argC - 1]
							: EnumFileType.CSV;
					argC++;
					// 4
					final EnumFileType enumFileTypeOut = o.length >= argC ? (EnumFileType) o[argC - 1]
							: EnumFileType.N3;
					argC++;
					// 5
					final boolean append = o.length >= argC ? (boolean) o[argC - 1] : false;
					argC++;
					// 6
					final boolean hasHeadLine = o.length >= argC ? (boolean) o[argC - 1] : false;

					loopThroughAndProcess(inFile, outFile, enumFileTypeIn, enumFileTypeOut, append, hasHeadLine);
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
