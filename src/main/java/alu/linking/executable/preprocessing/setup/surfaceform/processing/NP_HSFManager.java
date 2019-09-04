package alu.linking.executable.preprocessing.setup.surfaceform.processing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import alu.linking.config.constants.Strings;
import alu.linking.executable.preprocessing.nounphrases.NPExtractionManager;
import alu.linking.preprocessing.fileparser.EnumFileType;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.output.NxOutputParser;
import alu.linking.preprocessing.fileparser.output.OutParser;
import alu.linking.structure.Executable;
import alu.linking.structure.RDFFormatter;
import edu.stanford.nlp.ling.TaggedWord;

public class NP_HSFManager implements Executable, RDFFormatter<java.lang.String> {
	private final NxOutputParser outparser = new NxOutputParser();

	public NP_HSFManager() {

	}

	/**
	 * This method will strip the literal type definition and use NPComplete to
	 * check for noun phrases and return the ones that were set to be used.
	 * 
	 * @param literal text on which to apply NPComplete
	 * @return
	 */
	public static List<TaggedWord> splitTextToNounPhrases(final String literal) {
		if (literal != null && literal.length() != 0) {
			final List<TaggedWord> foundTags = NPExtractionManager.INSTANCE.exec(literal);
			return foundTags;
		} else {
			System.err.println("Not a valid literal: " + literal);
			// Nothing be extracted considering it's not a literal aka. not a surface form
			// so just return null
			return null;
		}
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

	public void loopThroughAndProcess(final File inFile, final File outFile, final boolean applyNPanalysis)
			throws IOException {
		loopThroughAndProcess(inFile, outFile, EnumFileType.CSV, EnumFileType.CSV, false, false, applyNPanalysis);
	}

	/**
	 * Takes an input file and loops through it, processing it and outputting
	 * results to outFile. Requires to be told which type of file the input is and
	 * the output will be. <br>
	 * Additionally, can be defined whether to append results to the output file or
	 * to just overwrite a possibly previously-existing file. <br>
	 * Also needs to be told whether the first line is a header (to know whether to
	 * ignore it or not)
	 * 
	 * @param inFile          input file
	 * @param outFile         output file to write results to
	 * @param enumFileTypeIn  type of input file
	 * @param enumFileTypeOut type of output file
	 * @param append          whether to append to or to overwrite file
	 * @param headLine        whether the first line is just a header (generally the
	 *                        case for CSV files)
	 * @throws IOException
	 */
	public void loopThroughAndProcess(final File inFile, final File outFile, final EnumFileType enumFileTypeIn,
			final EnumFileType enumFileTypeOut, final boolean append, final boolean headLine,
			final boolean applyNPanalysis) throws IOException {
		if (!outFile.exists()) {
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdirs();
			}
			outFile.createNewFile();
		}
		List<Object> line = null;
		final PrintStream defOut = System.out;
		final PrintStream errOut = System.err;

		if (applyNPanalysis) {
			System.setOut(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					// Ignore
				}
			}) {
				@Override
				public void println(String x) {
					// Ignore
				}

				@Override
				public void print(String s) {
					// Ignore
				}
			});

			System.setErr(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					// Ignore
				}
			}) {
				@Override
				public void println(String x) {
					// Ignore
				}

				@Override
				public void print(String s) {
					// Ignore
				}
			});

		}
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			final FileInParser parser = enumFileTypeIn.parserInClass.newInstance().create(brIn);
			try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(outFile.getPath()), StandardOpenOption.WRITE,
					append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE)) {
				if (headLine) {
					parser.withHeader(headLine);
				}
				final OutParser outParser = enumFileTypeOut.parserOutClass.newInstance();
				while ((line = parser.getNext()) != null) {
					// parser.format(line);
					final String subject = parser.toFormatString(line.get(0));
					if (applyNPanalysis) {
						final String toNounPhrase = line.get(line.size() - 1).toString();
						if (toNounPhrase != null && toNounPhrase.length() > 0) {
							final List<TaggedWord> tags = splitTextToNounPhrases(toNounPhrase);
							if (tags != null) {
								for (TaggedWord tag : tags) {
									final String outString = outParser.format(subject,
											Strings.PRED_HELPING_SURFACE_FORM.val, tag.value());
									bwOut.write(outString);
									bwOut.newLine();
								}

							}
						} else {
							System.err.println(line);
						}
					} else {
						// No noun-phrase analysis, just add the predicate
						final Object lineObj = line.get(line.size() - 1);
						if (lineObj == null || lineObj.toString().length() == 0) {
							continue;
						}
						final String outString = outParser.format(subject, Strings.PRED_HELPING_SURFACE_FORM.val,
								lineObj.toString());
						bwOut.write(outString);
						bwOut.newLine();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (applyNPanalysis) {
			System.setOut(defOut);
			System.setErr(errOut);
		}
	}

	@Override
	public List<TaggedWord> exec(Object... o) {
		if (o != null && o.length == 1) {
			return splitTextToNounPhrases(o[0].toString());
			// return tripleToHelpingSurfaceForm(o[0].toString());
		} else if (o != null && (o.length >= 3 && o.length <= 7)) {
			if (o[0] instanceof File && o[1] instanceof File) {
				final File inFile = (File) (o[0]);
				final File outFile = (File) (o[1]);
				try {
					if (o[2] != null) {
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
						argC++;
						final boolean applyNPanalysis = o.length >= argC ? (boolean) o[argC - 1] : true;
						loopThroughAndProcess(inFile, outFile, enumFileTypeIn, enumFileTypeOut, append, hasHeadLine,
								applyNPanalysis);
					}
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
		return outparser.format(subject, predicate, object);
	}

}
