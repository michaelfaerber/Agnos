package alu.linking.launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import com.beust.jcommander.internal.Lists;

import alu.linking.config.constants.Strings;

public class LauncherNormalizer {

	private static final Normalizer.Form normalizationType = Normalizer.Form.NFKD;

	static int collisionCounter = 0;
	public static void main(String[] args) {
		final String inPath =
				// "./dbpedia/resources/data/datasets/extracted/";
				// "/vol2/dblp/dumps/dblp_2018-11-02_unique.nt";//
				// "/vol2/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06.nt";//non-normalized
				// KG
				"./dbpedia_ttl";
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
		final Map<String, String> subjectMapping = new HashMap<>();
		final Map<String, String> predicateMapping = new HashMap<>();
		final Map<String, String> objectMapping = new HashMap<>();
		try (final BufferedWriter bwSubjectMapping = new BufferedWriter(
				new FileWriter(outDir + "normalized_subjects.txt"));
				final BufferedWriter bwPredicateMapping = new BufferedWriter(
						new FileWriter(outDir + "normalized_predicates.txt"));
				final BufferedWriter bwObjectMapping = new BufferedWriter(
						new FileWriter(outDir + "normalized_objects.txt"))) {
			for (File file : inFiles) {
				final String outPath = outDir + file.getName();
				System.out.println("Normalizing(" + normCounter++ + "): " + file.getAbsolutePath());
				System.out.println("Output: " + outPath);
				try (final BufferedReader brIn = new BufferedReader(new FileReader(file), bufferSize);
						final BufferedWriter bwOut = new BufferedWriter(new FileWriter(outPath), bufferSize)) {
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
							addMapping(bwSubjectMapping, subjectMapping, s.toN3(), subject);
						}
						if (!p.toN3().equals(predicate)) {
							addMapping(bwPredicateMapping, predicateMapping, p.toN3(), predicate);
						}
						if (!o.toN3().equals(object)) {
							addMapping(bwObjectMapping, objectMapping, o.toN3(), object);
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
					System.out.println(
							"Finished after " + (System.currentTimeMillis() - startTime) / 1_000 + " seconds.");
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Number of collisions: " + collisionCounter);
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

	private static void addMapping(final BufferedWriter bw, final Map<String, String> mapping, final String original,
			final String normalized) throws IOException {
		// Key = original
		// Value = normalized
		// Note - DANGER: 2 normalized versions may conflict...
		final String mapVal = mapping.get(normalized);
		if (mapVal == null) {
			mapping.put(normalized, original);
			// Output it to file
			bw.write(original);
			bw.write(Strings.IRI_NORMALIZATION_MAPPING_SEPARATOR.val);
			bw.write(normalized);
			bw.newLine();
		} else {
			if (mapVal.equals(original)) {
				// If they're equal, it's k since mapping exists
			} else {
				// If they're NOT equal, then we have a collision ... which is bad
				// System.err.println("Collision for normalized[" + normalized + "] -
				// orig1/mapVal[" + mapVal + "], orig["+ original + "]");
				// In case of conflict, output to file
				collisionCounter++;
				bw.write(original);
				bw.write(Strings.IRI_NORMALIZATION_MAPPING_SEPARATOR.val);
				bw.write(normalized);
				bw.newLine();
			}
		}
	}
}
