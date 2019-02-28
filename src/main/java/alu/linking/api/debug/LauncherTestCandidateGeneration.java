package alu.linking.api.debug;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.utils.DetectionUtils;

public class LauncherTestCandidateGeneration {

	public static void main(String[] args) {
		try {
			final EnumModelType KG = EnumModelType.DBPEDIA_FULL;
			final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
			stopwordsLoader.getStopwords();
			final Map<String, Collection<String>> map = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
			System.out.println("angelina items: " + map.get("angelina"));
			System.out.println("Angelina items: " + map.get("Angelina"));
			/*
			for (Map.Entry<String, Set<String>> e : map.entrySet())
			{
				System.out.println(e.getKey()+" - "+e.getValue());
			}
			*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
