package alu.linking.executable.preprocessing.deprecated;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.UnexpectedException;
import java.util.List;

import org.apache.log4j.Logger;

import alu.linking.preprocessing.fileparser.EnumFileType;
import alu.linking.preprocessing.fileparser.input.FileInParser;
import alu.linking.preprocessing.fileparser.output.OutParser;
import alu.linking.structure.Executable;

public class SFManagerFile implements Executable {
	private Logger logger = Logger.getLogger(getClass());

	/**
	 * 
	 * @param inFile
	 *            input file
	 * @param outFile
	 *            output file
	 * @param inType
	 *            type of input file
	 * @param outType
	 *            type of output file
	 * @throws IOException
	 */
	public void transform(final File inFile, final File outFile, final EnumFileType inType, final EnumFileType outType,
			final boolean append, final boolean headLine) throws IOException {
		// Transforms from one file type to another basically...
		// It's important to do everything step-by-step, hence the existence of this
		// class even though it feels kinda superfluous doing it this way
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()));
				BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(outFile.getPath()), StandardOpenOption.WRITE,
						append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE)) {
			final FileInParser parser = inType.parserInClass.newInstance().create(brIn);
			if (headLine) {
				// Just remove first line
				parser.withHeader(headLine);
			}
			List<Object> line = null;
			final OutParser outParser = outType.parserOutClass.newInstance();
			while ((line = parser.getNext()) != null) {
				if (line.size() < 3) {
					throw new UnexpectedException(
							"There should be 3 tokens per line... Either line parsing failed or the file contains two tokens per line... (File: "
									+ inFile.getAbsolutePath() + ")");
				}

				final String subj = parser.toFormatString(line.get(0));
				final String pred;
				if (line.size() > 3) {
					Object[] preds = new Object[line.size() - 2];
					for (int i = 1; i < line.size() - 1; i++) {
						preds[i - 1] = line.get(i);
					}
					pred = parser.toFormatString(preds);
				} else {
					pred = parser.toFormatString(line.get(1));
				}
				final Object lineObj = line.get(line.size() - 1);
				final String obj = parser.toFormatString(lineObj);
				if (subj == null || subj.length() == 0 || pred == null || pred.length() == 0 || obj == null
						|| obj.length() == 0) {
					// Ignore tuples with empty values for subject, predicate or object...
					continue;
				}
				final String outString = outParser.format(subj, pred, obj);
				try {
					bwOut.write(outString);
				} catch (NullPointerException npe) {
					npe.printStackTrace();
					logger.error("Outparser: " + outParser.getClass());
					logger.error("SUBJ: " + subj);
					logger.error("PRED: " + pred);
					logger.error("OBJ: " + obj);
					logger.error("OBJ ITEM: " + lineObj);
					logger.error("OBJ ITEM (FORMATTED): " + parser.toFormatString(lineObj));
					logger.error("OutString: " + outString);
					logger.error("LINE: " + line);
					throw npe;
				}
				bwOut.newLine();
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void init() {

	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {

		if (o != null && o.length == 1) {
			// What to do with one argument
		} else if (o != null && (o.length >= 2 && o.length <= 6)) {
			if (o[0] instanceof File && o[1] instanceof File) {
				final File inFile = (File) (o[0]);
				final File outFile = (File) (o[1]);
				try {
					if (o[2] != null) {
						int argC = 3;
						// 3
						final EnumFileType inType = o.length >= argC ? (EnumFileType) o[argC - 1] : EnumFileType.CSV;
						argC++;
						// 4
						final EnumFileType outType = o.length >= argC ? (EnumFileType) o[argC - 1] : EnumFileType.N3;
						argC++;
						// 5
						final boolean append = o.length >= argC ? (boolean) o[argC - 1] : false;
						argC++;
						// 6
						final boolean hasHeadLine = o.length >= argC ? (boolean) o[argC - 1] : false;
						transform(inFile, outFile, inType, outType, append, hasHeadLine);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getExecMethod() {
		return "transform";
	}

}
