package alu.linking.api.debug;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import alu.linking.api.GERBILAPIAnnotator;
import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.InputProcessor;
import alu.linking.mentiondetection.Mention;
import alu.linking.mentiondetection.MentionDetector;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.utils.DetectionUtils;

public class LauncherTestCandidateGeneration {

	public static void main(String[] args) {
		test2();
	}

	public static void test1()
	{
		try {
			final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			stopwordsLoader.getStopwords();
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
			System.out.println("angelina items: " + map.get("angelina"));
			System.out.println("Angelina items: " + map.get("Angelina"));
			final InputProcessor inputProcessor = new InputProcessor(null);
			MentionDetector md = DetectionUtils.setupMentionDetection(KG, map, inputProcessor);

			List<Mention> mentions = md.detect("angelina or not angelina?");
			System.out.println(mentions);
			// DetectionUtils.displayMentions(Logger.getLogger(LauncherTestCandidateGeneration.class),
			// mentions, false);
			for (Map.Entry<String, Collection<String>> e : map.entrySet()) {
				final List<Mention> detected = md.detect(e.getKey());
				System.out.println("["+(detected.size() > 0) + "]("+detected.size()+") - " + e.getKey() + " - " + e.getValue());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void test2()
	{
		final GERBILAPIAnnotator annotator = new GERBILAPIAnnotator(EnumModelType.DBPEDIA_FULL);
		final String nifString = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" + 
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + 
				"@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n" + 
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n" + 
				"@prefix nif: <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> .\r\n" + 
				"@prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> .\r\n" + 
				"\r\n" + 
				"<http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv#char=0,>\r\n" + 
				"	a nif:String , nif:Context , nif:RFC5147String ;\r\n" + 
				"	nif:sourceUrl <http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv> .\r\n" + 
				"\r\n" + 
				"<http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=0,>\r\n" + 
				"	a nif:String , nif:Context , nif:RFC5147String ;\r\n" + 
				"	nif:broaderContext <http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv> ;\r\n" + 
				"	nif:isString \"Angelina, her father Jon, and her partner Brad never played together in the same movie.\"^^xsd:string .\r\n" + 
				"\r\n" + 
				"<http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=0,8>\r\n" + 
				"	a nif:String , nif:RFC5147String ;\r\n" + 
				"	nif:referenceContext <http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=0,> ;\r\n" + 
				"	nif:anchorOf \"Angelina\"^^xsd:string ;\r\n" + 
				"	nif:beginIndex \"0\"^^xsd:long ;\r\n" + 
				"	nif:endIndex \"8\"^^xsd:long ;\r\n" + 
				"	a nif:Phrase ;\r\n" + 
				"	itsrdf:taIdentRef  <http://dbpedia.org/resource/Angelina_Jolie> .\r\n" + 
				"\r\n" + 
				"<http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=21,24>\r\n" + 
				"	a nif:String , nif:RFC5147String ;\r\n" + 
				"	nif:referenceContext <http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=0,> ;\r\n" + 
				"	nif:anchorOf \"Jon\"^^xsd:string ;\r\n" + 
				"	nif:beginIndex \"21\"^^xsd:long ;\r\n" + 
				"	nif:endIndex \"24\"^^xsd:long ;\r\n" + 
				"	a nif:Phrase ;\r\n" + 
				"	itsrdf:taIdentRef  <http://dbpedia.org/resource/Jon_Voight> .\r\n" + 
				"\r\n" + 
				"<http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=42,46>\r\n" + 
				"	a nif:String , nif:RFC5147String ;\r\n" + 
				"	nif:referenceContext <http://www.mpi-inf.mpg.de/yago-naga/aida/download/KORE50.tar.gz/AIDA.tsv/CEL07#char=0,> ;\r\n" + 
				"	nif:anchorOf \"Brad\"^^xsd:string ;\r\n" + 
				"	nif:beginIndex \"42\"^^xsd:long ;\r\n" + 
				"	nif:endIndex \"46\"^^xsd:long ;\r\n" + 
				"	a nif:Phrase ;\r\n" + 
				"	itsrdf:taIdentRef  <http://dbpedia.org/resource/Brad_Pitt> .\r\n" + 
				"";
		final String nifString2 = 
				"@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" + 
				"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\r\n" + 
				"@prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> .\r\n" + 
				"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + 
				"@prefix nif:   <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> .\r\n" + 
				"\r\n" + 
				"<http://www.aksw.org/gerbil/NifWebService/request_38#char=107,114>\r\n" + 
				"        a                     nif:RFC5147String , nif:String , nif:Phrase ;\r\n" + 
				"        nif:anchorOf          \"Chicago\" ;\r\n" + 
				"        nif:beginIndex        \"107\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:endIndex          \"114\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:referenceContext  <http://www.aksw.org/gerbil/NifWebService/request_38#char=0,129> .\r\n" + 
				"\r\n" + 
				"<http://www.aksw.org/gerbil/NifWebService/request_38#char=75,84>\r\n" + 
				"        a                     nif:RFC5147String , nif:String , nif:Phrase ;\r\n" + 
				"        nif:anchorOf          \"Woodstock\" ;\r\n" + 
				"        nif:beginIndex        \"75\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:endIndex          \"84\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:referenceContext  <http://www.aksw.org/gerbil/NifWebService/request_38#char=0,129> .\r\n" + 
				"\r\n" + 
				"<http://www.aksw.org/gerbil/NifWebService/request_38#char=100,105>\r\n" + 
				"        a                     nif:RFC5147String , nif:String , nif:Phrase ;\r\n" + 
				"        nif:anchorOf          \"Davis\" ;\r\n" + 
				"        nif:beginIndex        \"100\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:endIndex          \"105\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:referenceContext  <http://www.aksw.org/gerbil/NifWebService/request_38#char=0,129> .\r\n" + 
				"\r\n" + 
				"<http://www.aksw.org/gerbil/NifWebService/request_38#char=120,128>\r\n" + 
				"        a                     nif:RFC5147String , nif:String , nif:Phrase ;\r\n" + 
				"        nif:anchorOf          \"Mitchell\" ;\r\n" + 
				"        nif:beginIndex        \"120\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:endIndex          \"128\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:referenceContext  <http://www.aksw.org/gerbil/NifWebService/request_38#char=0,129> .\r\n" + 
				"\r\n" + 
				"<http://www.aksw.org/gerbil/NifWebService/request_38#char=0,129>\r\n" + 
				"        a               nif:RFC5147String , nif:String , nif:Context ;\r\n" + 
				"        nif:beginIndex  \"0\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:endIndex    \"129\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:isString    \"The Isle of Wight festival in 1970 was the biggest at its time, surpassing Woodstock with acts like Davis, Chicago, and Mitchell.\" .\r\n" + 
				"\r\n" + 
				"<http://www.aksw.org/gerbil/NifWebService/request_38#char=4,26>\r\n" + 
				"        a                     nif:RFC5147String , nif:String , nif:Phrase ;\r\n" + 
				"        nif:anchorOf          \"Isle of Wight festival\" ;\r\n" + 
				"        nif:beginIndex        \"4\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:endIndex          \"26\"^^xsd:nonNegativeInteger ;\r\n" + 
				"        nif:referenceContext  <http://www.aksw.org/gerbil/NifWebService/request_38#char=0,129> .\r\n" + 
				"";
		annotator.init();
		annotator.annotate(nifString2);
	}
		
}
