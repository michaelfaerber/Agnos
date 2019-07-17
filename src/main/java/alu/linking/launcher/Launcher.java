package alu.linking.launcher;

import java.io.File;
import java.util.List;

import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;
import alu.linking.config.kg.deprecated.KGManager;
import alu.linking.executable.Pipeline;
import alu.linking.executable.debug.Tester;
import alu.linking.executable.preprocessing.deprecated.ComputeHops;
import alu.linking.executable.preprocessing.deprecated.CrunchbaseExtendedGraphLoader;
import alu.linking.executable.preprocessing.deprecated.HSFURLManagerContentSaver;
import alu.linking.executable.preprocessing.deprecated.SFManagerFile;
import alu.linking.executable.preprocessing.setup.BuildGraph;
import alu.linking.executable.preprocessing.setup.surfaceform.processing.NP_HSFManager;
import alu.linking.executable.preprocessing.setup.surfaceform.processing.url.NP_HSFURLManager;
import alu.linking.executable.preprocessing.util.FileCombiner;
import alu.linking.executable.preprocessing.util.VirtuosoCommunicator;
import alu.linking.preprocessing.crunchbase.CrunchbaseKGManager;
import alu.linking.preprocessing.fileparser.EnumFileType;

public class Launcher {
	private static Logger logger = Logger.getLogger(Launcher.class);

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.DEFAULT;
		final boolean t = true;
		final boolean f = false;
		// Execute queries for SF, HSF, HSF_NP and HSF_NP_URL on server
		final boolean UPDATE_QUERY_OUTPUT_SF = f;
		final boolean UPDATE_QUERY_OUTPUT_HSF = f;
		final boolean UPDATE_QUERY_OUTPUT_HSF_NP = f;
		final boolean UPDATE_QUERY_OUTPUT_HSF_NP_URL = f;
		// Note that querying hops will be done via topic modeling in the hopefully near
		// future
		final boolean UPDATE_QUERY_HOPS = f;
		// Translate from the query output into an applicable Extended Graph format
		final boolean PART_A = f;
		final boolean PART_B = f;
		final boolean PART_C = f;
		// Crawls data from the websites and applies NPcomplete
		// based on the URLs it found from UPDATE_QUERY_OUTPUT_HSF_NP_URL
		final boolean PART_D = f;
		// Whether to crawl data from the extracted URLs and then save it
		final boolean CRAWL_AND_SAVE = f;
		// Where to only apply part D for first file, remove after short debugging...
		final boolean firstfile = f;
		final boolean COMBINE_OUTPUTS_TO_EXTENDED = f;
		final boolean LOAD_EXTENDED_GRAPH = f;
		final boolean DISPLAY_STATEMENTS = f;
		final boolean TAGTOG_BMW = t;
		final boolean UPDATE_BUILD_N_DUMP_GRAPH_FROM_EXTENDED_GRAPH = f;
		final boolean DO_HOPS = f;
		final boolean DO_TESTS = f;
		try {
			final Pipeline pipeline = new Pipeline();
			final File crunchbaseDumpFile = new File(FilePaths.FILE_DUMP_CRUNCHBASE.getPath(KG));
			final File crunchbaseEntitiesFile = new File(FilePaths.FILE_CRUNCHBASE_ENTITIES.getPath(KG));
			final File crunchbase_noun_phrases = new File(FilePaths.FILE_CRUNCHBASE_ENTITIES_NOUN_PHRASES.getPath(KG));
			final File crunchbase_linked_noune_phrases = new File(
					FilePaths.FILE_CRUNCHBASE_ENTITIES_NOUN_PHRASES_LINKED.getPath(KG));
			// pipeline.queue(new EntityExtractor(), crunchbaseDumpFile,
			// crunchbaseEntitiesFile);
			// pipeline.queue(new SurfaceFormManager(), crunchbaseEntitiesFile,
			// crunchbase_noun_phrases);

			// pipeline.queue(new StringReplacer(), new File(FilePaths.FILE_PAGERANK.getPath(KG)),
			// new File(FilePaths.FILE_PAGERANK_ADAPTED.getPath(KG)), "\t", "");

			if (UPDATE_QUERY_OUTPUT_SF) {
				// Execute all queries from the surface form directory
				final File[] surfaceFormQueryFiles = new File(FilePaths.DIR_QUERY_IN_SURFACEFORM.getPath(KG)).listFiles();
				queueUpdateQueryOutput(pipeline, surfaceFormQueryFiles);
			}

			if (UPDATE_QUERY_OUTPUT_HSF) {
				// Execute all queries from the helping surface form directory
				final File[] helpingSurfaceFormQueryFiles = new File(FilePaths.DIR_QUERY_IN_HELPING_SURFACEFORM.getPath(KG))
						.listFiles();
				// Execute queries and get the data for (helping) surface forms
				// Note: HSF = Helping Surface Form
				// b) Updates query output for the directly to-attach helping surface form (HSF)
				queueUpdateQueryOutput(pipeline, helpingSurfaceFormQueryFiles);
			}
			if (UPDATE_QUERY_OUTPUT_HSF_NP) {
				// Execute all queries from the helping surface form NP directory
				final File[] helpingSurfaceFormNPQueryFiles = new File(
						FilePaths.DIR_QUERY_IN_NP_HELPING_SURFACEFORM.getPath(KG)).listFiles();
				// c) Updates query output for the HSFs that need to be analyzed
				// for noun-phrases (aka. long HSF)
				queueUpdateQueryOutput(pipeline, helpingSurfaceFormNPQueryFiles);
			}
			if (UPDATE_QUERY_OUTPUT_HSF_NP_URL) {
				// Execute all queries from the helping surface form NP URL directory
				final File[] helpingSurfaceFormNPURLQueryFiles = new File(
						FilePaths.DIR_QUERY_IN_NP_URL_HELPING_SURFACEFORM.getPath(KG)).listFiles();
				// d) Updates query output for the HSF consisting of URLs which will need to be
				// crawled and analyzed for noun-phrases afterwards
				queueUpdateQueryOutput(pipeline, helpingSurfaceFormNPURLQueryFiles);
			}

			if (UPDATE_QUERY_HOPS) {
				// Execute the hopping queries and update the output from them
				logger.debug("[Hops Querying] Added to queue");
				// Execute all queries from the helping surface form NP URL directory
				final File[] hopsQueryFiles = new File(FilePaths.DIR_QUERY_IN_EXTENDED_GRAPH_HOPS.getPath(KG)).listFiles();
				queueUpdateQueryOutput(pipeline, hopsQueryFiles);
			}

			// Now that we have the queried data in CSV form
			// -> We need to process (some of) it for the extendedgraph

			if (PART_A) {
				alu.linking.utils.Stopwatch.start("PART_A");
				// Entities to be annotated and their surface forms
				// -> no further processing needed other than placing them in the correct
				// place(s) (should probably keep the predicates for these, actually)
				logger.debug("Part a) Added to queue");
				final File[] surfaceFormFiles = new File(FilePaths.DIR_QUERY_OUT_SURFACEFORM.getPath(KG)).listFiles();
				final EnumFileType inType = EnumFileType.CSV;
				final EnumFileType outType = EnumFileType.N3;
				final boolean append = false;
				final boolean headLine = true;
				for (File inFile : surfaceFormFiles) {
					if (inFile.isFile()) {
						final File outFile = new File(FilePaths.DIR_EXTENDED_GRAPH.getPath(KG) + inFile.getName());
						pipeline.queue(new SFManagerFile(), inFile, outFile, inType, outType, append, headLine);
					}
				}
				alu.linking.utils.Stopwatch.endOutput("PART_A");
			}

			if (PART_B) {
				alu.linking.utils.Stopwatch.start("PART_B");
				// Helping surface forms are extracted that will just be added to the graph
				// with the :helpingSurfaceForm predicate
				// aka. minor modification of query's output required
				final File[] hSF_Files = new File(FilePaths.DIR_QUERY_OUT_HELPING_SURFACEFORM.getPath(KG)).listFiles();
				logger.debug("Part b) Added to queue");
				for (File hSFIn : hSF_Files) {
					if (hSFIn.isFile()) {
						// Where the final helping surface forms will be stored
						final String hSFOut = FilePaths.DIR_EXTENDED_GRAPH.getPath(KG) + hSFIn.getName();
						// Grabs the URLs from input file of type CSV, grabs the content from the web
						// and finally outputs it in N3 format to the specified output file
						pipeline.queue(new NP_HSFManager(), hSFIn, new File(hSFOut), EnumFileType.CSV, EnumFileType.N3,
								false, true, false);
					}
				}
				alu.linking.utils.Stopwatch.endOutput("PART_B");
			}

			if (PART_C) {
				alu.linking.utils.Stopwatch.start("PART_C");
				// For Part c): Noun-phrase detection/extraction on longish text
				// Take all files in designated folder as input (aka. all files with the output
				// from the executed queries) and process them to get their noun-phrases
				final File[] hSF_NPFiles = new File(FilePaths.DIR_QUERY_OUT_NP_HELPING_SURFACEFORM.getPath(KG)).listFiles();
				logger.debug("Part c) Added to queue");
				for (File hSFNPIn : hSF_NPFiles) {
					if (hSFNPIn.isFile()) {
						// Where the final helping surface forms will be stored
						final String hSFNPOut = FilePaths.DIR_EXTENDED_GRAPH.getPath(KG) + hSFNPIn.getName();
						// Grabs the URLs from input file of type CSV, grabs the content from the web
						// and finally outputs it in N3 format to the specified output file
						pipeline.queue(new NP_HSFManager(), hSFNPIn, new File(hSFNPOut), EnumFileType.CSV, EnumFileType.N3,
								false, true, true);
					}
				}
				alu.linking.utils.Stopwatch.endOutput("PART_C");
			}

			if (CRAWL_AND_SAVE) {
				// Whether to crawl the URLs and save the contents on the machine for faster
				// future extraction
				final File[] hSF_NPURLFiles = new File(FilePaths.DIR_QUERY_OUT_NP_URL_HELPING_SURFACEFORM.getPath(KG))
						.listFiles();
				logger.debug("Added content saving to queue");
				for (File urlFileIn : hSF_NPURLFiles) {
					if (urlFileIn.isFile()) {
						pipeline.queue(new HSFURLManagerContentSaver(KG), urlFileIn, EnumFileType.CSV, true);
					}
				}
			}

			if (PART_D) {
				alu.linking.utils.Stopwatch.start("PART_D");
				// For Part d): Noun-Phrase detection/extraction on web content (either online
				// or pre-downloaded offline)
				// All files within the query output's designated folder are taken as input
				final File[] hSF_NPURLFiles = new File(FilePaths.DIR_QUERY_OUT_NP_URL_HELPING_SURFACEFORM.getPath(KG))
						.listFiles();
				logger.debug("Part d) Added to queue");
				for (File urlFileIn : hSF_NPURLFiles) {
					if (urlFileIn.isFile()) {
						// Where the final helping surface forms will be stored
						final String urlNPOut = FilePaths.DIR_EXTENDED_GRAPH.getPath(KG) + urlFileIn.getName();
						// logger.debug("URL File: " + urlNPOut);
						// Grabs the URLs from input file of type CSV, grabs the content from the web
						// and finally outputs it in N3 format to the specified output file
						pipeline.queue(new NP_HSFURLManager(KG), urlFileIn, new File(urlNPOut), EnumFileType.CSV,
								EnumFileType.N3, false, true);
					}
					if (firstfile) {
						break;
					}
				}
				alu.linking.utils.Stopwatch.endOutput("PART_D");
			}

			if (COMBINE_OUTPUTS_TO_EXTENDED) {
				final File[] parts = new File(FilePaths.DIR_EXTENDED_GRAPH.getPath(KG)).listFiles();
				final File combinedFile = new File(FilePaths.FILE_EXTENDED_GRAPH.getPath(KG));
				boolean firstFile = true;
				// Extended graph currently is just the entities/surface forms/helping surface
				// forms (without all the rest contained within the initial one)
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].isFile()) {
						// First part overwrites previously-existing file, other one(s)
						logger.debug("Combining: " + parts[i].getCanonicalPath());
						pipeline.queue(new FileCombiner(), parts[i], combinedFile, !firstFile);
						firstFile = false;
					}
				}
			}

			if (UPDATE_BUILD_N_DUMP_GRAPH_FROM_EXTENDED_GRAPH) {
				logger.debug("Building Graph object based on extended graph...");
				pipeline.queue(new BuildGraph(), null);
			}

			if (DO_HOPS) {
				logger.debug("Computing hops...");
				pipeline.queue(new ComputeHops(), null);
			}

			if (LOAD_EXTENDED_GRAPH) {
				// Loads extended graph from the combined extended graph file into a local Jena
				// Model
				pipeline.queue(new CrunchbaseExtendedGraphLoader(), null);
			}
			if (DISPLAY_STATEMENTS) {
				// Query contents of extended graph just for the fun of it / debugging
				List<Statement> stmts = new CrunchbaseKGManager(FilePaths.DATASET_CRUNCHBASE.getPath(KG)).getStatements(null,
						"http://own.org/helpingSurfaceFormExtractedNP", null);
				logger.debug("Number of statements: " + stmts.size());
				final int nbShown = 10;
				for (int i = 0; i < Math.min(nbShown, stmts.size()); ++i) {
					logger.debug(stmts.get(i));
				}
			}

			if (DO_TESTS) {
				pipeline.queue(new Tester(), null);
			}

			// See for mention extraction:
			// DeterministicCorefAnnotator -> MentionExtractor;

			pipeline.setOutput(true);
			logger.info("Starting pipeline");
			pipeline.exec();
			logger.info("Pipeline finished!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shorthand method for looping through the File[] and queueing up new instances
	 * of VirtuosoCommunicator() to the pipeline as <br>
	 * pipeline.queue(new VirtuosoCommunicator(), File[:].getPath(), TRUE)
	 * 
	 * @param pipeline
	 *            pipeline that will take care of executing its parts
	 * @param queryInputFiles
	 *            files containing queries to be executed on the SPARQL endpoint
	 * 
	 */
	private static void queueUpdateQueryOutput(Pipeline pipeline, File[] queryInputFiles) {
		for (File queryFile : queryInputFiles) {
			pipeline.queue(new VirtuosoCommunicator(), queryFile.getPath(), Boolean.TRUE);
		}
	}

	public static void queryGraph(final EnumModelType KG) {
		// Takes ages to load huge dataset
		final String dsPath = FilePaths.DATASET_SAMPLE.getPath(KG);
		CrunchbaseKGManager kgm = new CrunchbaseKGManager(dsPath);
		logger.debug("READING DUMP");
		kgm.read(FilePaths.FILE_DUMP_CRUNCHBASE.getPath(KG));
		KGManager.getAllStatements(dsPath, EnumModelType.CRUNCHBASE);
		logger.debug("FINISHED READING DUMP");
	}

}
