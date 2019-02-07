package alu.linking.preprocessing.surfaceform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.util.NxUtil;

import alu.linking.config.constants.Strings;
import alu.linking.structure.MentionPossibilityProcessor;
import alu.linking.structure.RDFLineProcessor;

/**
 * This class extracts literals and maps them as Map<Literal, Set<Source>> The
 * point is to be able to give the MentionDetector an input map to match against
 * text occurrences.
 * 
 * @author Kwizzer
 *
 */
public class MentionPossibilityExtractor implements MentionPossibilityProcessor {
	private final int DEFAULT_MIN_LENGTH_THRESHOLD = 0;// 0 pretty much means 'no threshold'
	private final int DEFAULT_MAX_LENGTH_THRESHOLD = 50;// 0 pretty much means 'no threshold'
	private int lengthMinThreshold = DEFAULT_MIN_LENGTH_THRESHOLD;
	private int lengthMaxThreshold = DEFAULT_MAX_LENGTH_THRESHOLD;
	private final HashSet<String> blackList = new HashSet<String>();
	private final HashMap<String, Set<String>> mentionPossibilities = new HashMap<String, Set<String>>();
	private final String delim = Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val;

	public void populateBlacklist(final File inFile) throws IOException {
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				blackList.add(line);
			}
		}
	}

	public void dumpBlacklist(final File outFile) throws IOException {
		try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(outFile.getPath()), StandardOpenOption.WRITE)) {
			for (String word : blackList) {
				bwOut.write(word);
				bwOut.newLine();
			}
		}

	}

	public void blacklist(final String word) {
		this.blackList.add(word);
	}

	/**
	 * Let's look at the entity file and get everything that we can link back to
	 * them
	 * 
	 * @param inFile
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, Set<String>> addPossibilities(final File inFile) throws IOException {
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			processFile(brIn);
		}
		return mentionPossibilities;
	}

	/**
	 * General processing for a file, choose whether it's a specific file for
	 * linking or simply a N3
	 * 
	 * @param brIn
	 * @throws IOException
	 */
	private void processFile(BufferedReader brIn) throws IOException {
		processFileEntitySurfaceFormLinking(brIn);
	}

	/**
	 * Specific file having entities and their respective surface forms (usually
	 * output by querying a KG)
	 * 
	 * @param brIn
	 * @throws IOException
	 */
	private void processFileEntitySurfaceFormLinking(final BufferedReader brIn) throws IOException {
		String line = null;
		boolean nxparsing = true;
		int counter = 0;
		if (!nxparsing) {
			while ((line = brIn.readLine()) != null) {
				final String[] tokens = line.split(delim);
				if (tokens.length == 2) {
					mentionPossibility(tokens[0], "<link>", tokens[1]);
					// addPossibility(mentionPossibilities, tokens[1], tokens[0]);
				} else if (tokens.length == 3) {
					// addPossibility(mentionPossibilities, tokens[2], tokens[0]);
					mentionPossibility(tokens[0], tokens[1], tokens[2]);
				} else if (tokens.length != 0) {
					System.err.println("Invalid line...: " + line);
				}
			}
		} else {

			final Iterator<String> bufferIterator = new Iterator<String>() {
				final StringBuilder ret = new StringBuilder();
				final StringBuilder tokenBuilder = new StringBuilder();
				String[] currLine = new String[] { "" };
				final String dummyPredicate = "<link>";
				// predicate position reosurce is missing
				final int missingResourceIndex = 1;

				@Override
				public boolean hasNext() {
					return currLine != null;
				}

				@Override
				public String next() {
					if (hasNext()) {
						ret.setLength(0);
						tokenBuilder.setLength(0);

						String line;
						try {
							line = brIn.readLine();
						} catch (IOException e) {
							line = null;
						}
						if (line != null) {
							currLine = line.split(delim);
						} else {
							currLine = null;
							return null;
						}

						for (int i = 0; i < currLine.length; ++i) {
							if (i != currLine.length - 1) {
								// For any token that's not the last
								final String token = currLine[i];
								final boolean isLiteral = token.contains("^^http://") || token.contains("^^<http://");
								if (!token.startsWith("<") && !isLiteral) {
									// Does not start w/ < and is not a literal
									tokenBuilder.append("<");
								}
								// add the actual token
								tokenBuilder.append(token);

								if (!token.endsWith(">") && !isLiteral) {
									tokenBuilder.append(">");
								}
								tokenBuilder.append(" ");
							} else {
								// For the last one (should be a literal)
								final String token = currLine[i];
								final int literalTypeIndex = token.indexOf("^^http://");
								final int literalTypeLtIndex = token.indexOf("^^<http://");
								final int atEnglishLanguage = token.indexOf("@en");
								// String is missing starting quotes
								if (!token.startsWith("\"")) {
									tokenBuilder.append("\"");
								}

								if (!token.endsWith("\"")) {
									// String is missing ending quote
									if (atEnglishLanguage != -1) {
										// Put the quote before the @en
										tokenBuilder.append(token.substring(0, atEnglishLanguage));
										tokenBuilder.append("\"");
										tokenBuilder.append(token.substring(atEnglishLanguage, token.length()));
									} else if (literalTypeIndex != -1) {
										// Put the quote before the string definition of ^^http:// etc
										// but add < and > around the string definition
										tokenBuilder.append(token.substring(0, literalTypeIndex));
										tokenBuilder.append("\"^^<");
										tokenBuilder.append(token.substring(literalTypeIndex + 2, token.length()));
										tokenBuilder.append(">");
									} else if (literalTypeLtIndex != -1) {
										// Put the quote before the string definition of ^^<http:// etc
										tokenBuilder.append(token.substring(0, literalTypeLtIndex));
										tokenBuilder.append("\"");
										tokenBuilder.append(token.substring(literalTypeLtIndex, token.length()));
									} else {
										// Put the quote at the end of line simply
										tokenBuilder.append(token);
										tokenBuilder.append("\"");
									}
								} else {
									// String has an ending quote already, so no need to add one
									tokenBuilder.append(token);
								}
								tokenBuilder.append(" ");
							}
							if (i == missingResourceIndex) {
								// You're at the position you need to introduce a new resource into, so add it
								// to the actual return value already
								ret.append(dummyPredicate);
								ret.append(" ");
							}
							ret.append(tokenBuilder.toString());
							tokenBuilder.setLength(0);
						}
						ret.append(".");
						return ret.toString();
					} else {
						// Should be handled by the above logic and hasNext() is resolved as currLine !=
						// null anyway, but just in case logic somehow changes it up
						currLine = null;
						return null;
					}
				}
			};
			// Making use of NxParser to remove String definition etc
			NxParser parser = new NxParser(new ArrayList<String>() {
				@Override
				public Iterator<String> iterator() {
					return bufferIterator;
				}
			});

			while (parser.hasNext()) {
				Node[] triple = parser.next();
				mentionPossibility(triple[0], triple[1], triple[2]);
			}
		}

	}

	/**
	 * NTriples type of input file
	 * 
	 * @param brIn
	 * @throws IOException
	 */
	private void processFileNTriples(Reader brIn) throws IOException {
		final NxParser parser = new NxParser(brIn);
		int lineCounter = 0;
		final PrintStream syserr = System.err;
		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// Ignore
			}
		}));
		try {
			while (parser.hasNext()) {
				// Process the entity line
				final Node[] triple;
				try {
					triple = parser.next();
				} catch (StringIndexOutOfBoundsException sioobe) {
					continue;
				}
				mentionPossibility(triple[0], triple[1], triple[2]);
				lineCounter++;
				if (lineCounter % 10_000 == 0) {
					System.out.println("Processed lines: " + lineCounter);
				}
			}
		} catch (Exception e) {
		} finally {
			System.setErr(syserr);
		}
	}

	private void processFile(BufferedReader brIn, RDFLineProcessor processor) throws IOException {
		String line = null;
		while ((line = brIn.readLine()) != null) {
			// Process the entity line
			final List<String> triple = processor.parse(line);
			mentionPossibility(triple.get(0), triple.get(1), triple.get(2));
		}
	}

	/**
	 * Populates the map one entry at a time
	 * 
	 * @param map    map to be populated
	 * @param word   word to be added to the map
	 * @param source what the word belongs to
	 */
	private void addPossibility(final HashMap<String, Set<String>> map, String word, String source) {
		word = word.toLowerCase();
		source = source.toLowerCase();
		if (!passesRequirements(word))
			return;
		Set<String> s;
		if ((s = map.get(word)) == null) {
			s = new HashSet<String>();
			map.put(word, s);
		}
		s.add(source);
	}

	/**
	 * Checks whether it passes threshold and blacklist requirements
	 * 
	 * @param word the word that should be added as a possibly found mention
	 * @return whether it passes threshold and blacklist requirements
	 */
	private boolean passesRequirements(String word) {
		boolean ret = (word != null && word.length() > lengthMinThreshold && !blackList.contains(word));
		if (word.length() > lengthMaxThreshold) {
//			System.out.println("Too long(" + word.length() + "): " + word.substring(0, 40));
			ret = false;
		}
		return ret;
	}

	public void setMinLenThreshold(final int minLen) {
		this.lengthMinThreshold = minLen;
	}

	@Override
	/**
	 * Add the possibility if it fits what we want
	 */
	public void mentionPossibility(String s, String p, String o) {
		try {
			int endIndex = o.indexOf("^^http://");
			final String cleanedO;
			if (endIndex == -1) {
				cleanedO = NxUtil.escapeForMarkup(o);
			} else {
				cleanedO = NxUtil.escapeForMarkup(o.substring(0, endIndex));
			}
			mentionPossibility(new Resource(s, true), new Resource(p, true), new Literal(cleanedO));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("s:" + s);
			System.out.println("p:" + p);
			System.out.println("o:" + o);
		}
		// if (RDFNodeUtils.isTypedLiteral(o)) {
		// addPossibility(mentionPossibilities,
		// RDFNodeUtils.stripLiteralQuotesAndType(o), s);
		// }
	}

	@Override
	public void mentionPossibility(Node s, Node p, Node o) {
		// <http://www.w3.org/1999/02/22-rdf-syntax-ns#label>
		if (o instanceof org.semanticweb.yars.nx.Literal) {
//			final boolean addPoss;
//			if (p instanceof org.semanticweb.yars.nx.Resource
//			// && ((org.semanticweb.yars.nx.Resource) p).toString().equals(labelURI)
//			) {
			addPossibility(mentionPossibilities, o.toString(), s.toString());
//			}
		}
	}
}
