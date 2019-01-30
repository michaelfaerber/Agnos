package alu.linking.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

import com.beust.jcommander.internal.Lists;

public class LauncherNormalizer {

	public static void main(String[] args) {
		final String inPath = "./dbpedia/resources/data/datasets/extracted/";// "/vol1/dblp/dumps/dblp_2018-11-02_unique.nt";//
																				// "/vol1/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06.nt";//non-normalized
																				// KG
		final File inFile = new File(inPath);
		final List<File> inFiles = Lists.newArrayList();
		if (inFile.isDirectory()) {
			for (File f : inFile.listFiles()) {
				if (f.isFile()) {
					inFiles.add(f);
				}
			}
		} else {
			inFiles.add(inFile);
		}

		// "./dblp_2018-11-02_unique_normalized.nt";// normalized KG output

		final Normalizer.Form normalizationType = Normalizer.Form.NFKD;
		final long startTime = System.currentTimeMillis();
		int normCounter = 0;
		for (File file : inFiles) {
			final String outPath = "./input_dbpedia/" + file.getName();
			System.out.println("Normalizing(" + normCounter++ + "): " + file.getAbsolutePath());
			System.out.println("Output: " + outPath);
			try (final BufferedReader brIn = new BufferedReader(new FileReader(file));
					final BufferedWriter bwOut = new BufferedWriter(new FileWriter(outPath))) {
				String line = null;
				while ((line = brIn.readLine()) != null) {
					String normalizedString;
					if (true) {
						normalizedString = Normalizer.normalize(line, normalizationType).replace("\\\\", "")
								.replace("|", "").replace("{", "").replace("}", "").replaceAll("\\p{Space}", "").replace(">", ">\t");
						bwOut.write(normalizedString);
						bwOut.newLine();
					} else {
						if (!line.contains("http://dblp.uni-trier.de/db/conf/eacl/eacl2006.html#BilhautW06")) {
							normalizedString = Normalizer.normalize(line, normalizationType).replace("\\\\", "")
									.replace("|", "").replace("{", "").replace("}", "");
							if (line.contains(
									"http://csdl.computer.org/comp/proceedings/iv/2003/1988/00/19880485abs.htm")) {
								normalizedString = normalizedString.replaceAll("\\p{Space}", "").replace(">", ">\t");
							}
							bwOut.write(normalizedString);
							bwOut.newLine();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				System.out.println("Finished after " + (System.currentTimeMillis() - startTime) / 1_000 + " seconds.");
			}
		}
	}

}
