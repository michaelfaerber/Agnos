package alu.linking.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import com.beust.jcommander.internal.Lists;

import alu.linking.config.constants.Strings;

public class LauncherNormalizer {

	private static final Normalizer.Form normalizationType = Normalizer.Form.NFKD;

	public static void main(String[] args) {
		final String inPath =
				// "./dbpedia/resources/data/datasets/extracted/";
				//"/vol2/dblp/dumps/dblp_2018-11-02_unique.nt";//
				"/vol2/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06.nt";//non-normalized
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

		final long startTime = System.currentTimeMillis();
		int normCounter = 0;
		final int bufferSize = 100 * 8192;
		final String outDir = "./cb2018-06/";
		for (File file : inFiles) {
			final String outPath = outDir + file.getName();
			System.out.println("Normalizing(" + normCounter++ + "): " + file.getAbsolutePath());
			System.out.println("Output: " + outPath);
			try (final BufferedReader brIn = new BufferedReader(new FileReader(file), bufferSize);
					final BufferedWriter bwOut = new BufferedWriter(new FileWriter(outPath), bufferSize);
					final BufferedWriter bwSubjectMapping = new BufferedWriter(
							new FileWriter(outDir + "normalized_subjects.txt"));
					final BufferedWriter bwPredicateMapping = new BufferedWriter(
							new FileWriter(outDir + "normalized_predicates.txt"));
					final BufferedWriter bwObjectMapping = new BufferedWriter(
							new FileWriter(outDir + "normalized_objects.txt"))) {
				final NxParser parser = new NxParser(brIn);
				final StringBuilder normalizedString = new StringBuilder();
				while (parser.hasNext()) {
					normalizedString.setLength(0);
					final Node[] triple = parser.next();
					final Node s = triple[0], p = triple[1], o = triple[2];
					final String subject = normalizeSubject(s);
					final String predicate = normalizePredicate(p);
					final String object = normalizeObject(o);
					// Problem: if there's a literal, it ain't good to normalize it as a line
					// normalizeLine(subject);
					if (!s.toN3().equals(subject)) {
						addMapping(bwSubjectMapping, s.toN3(), subject);
					}
					if (!p.toN3().equals(predicate)) {
						addMapping(bwPredicateMapping, p.toN3(), predicate);
					}
					if (!o.toN3().equals(object)) {
						addMapping(bwObjectMapping, o.toN3(), object);
					}

					normalizedString.append(subject);
					normalizedString.append("\t");
					normalizedString.append(predicate);
					normalizedString.append("\t");
					normalizedString.append(object);
					normalizedString.append("\t.");
					bwOut.write(normalizedString.toString());
					bwOut.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				System.out.println("Finished after " + (System.currentTimeMillis() - startTime) / 1_000 + " seconds.");
			}
		}
	}

	private static String normalizeLine(String line) {
		return Normalizer.normalize(line, normalizationType).replace("\\\\", "").replace("|", "").replace("{", "")
				.replace("}", "").replaceAll("\\p{Space}", "").replace(">", ">\t").replace("\"", "");
	}

	private static String normalizeSubject(Node subject) {
		return Normalizer.normalize(subject.toN3(), normalizationType).replace("\\\\", "").replace("|", "")
				.replace("{", "").replace("}", "").replaceAll("\\p{Space}", "").replace("\"", "");
	}

	private static String normalizePredicate(Node predicate) {
		return Normalizer.normalize(predicate.toN3(), normalizationType).replace("\\\\", "").replace("|", "")
				.replace("{", "").replace("}", "").replaceAll("\\p{Space}", "").replace("\"", "");
	}

	private static String normalizeObject(Node object) {
		// Not doing more since it might be a literal
		// Could of course check with the Node type whether it is a literal or not, but
		// myah
		final String objStr = object.toN3();
		String ret = Normalizer.normalize(objStr, normalizationType);
		if (!(object instanceof Literal)) {
			// Replace stuff here too
			ret = ret.replace("\\\\", "").replace("|", "").replace("{", "").replace("}", "")
					.replaceAll("\\p{Space}", "").replace("\"", "");
		}
		return ret;
	}

	private static void addMapping(final BufferedWriter bw, final String original, final String normalized)
			throws IOException {
		// Key = original
		// Value = normalized
		// Note - DANGER: 2 normalized versions may conflict...
		bw.write(original);
		bw.write(Strings.IRI_NORMALIZATION_MAPPING_SEPARATOR.val);
		bw.write(normalized);
		bw.newLine();
	}

}
