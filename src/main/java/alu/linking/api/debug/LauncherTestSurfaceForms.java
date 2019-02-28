package alu.linking.api.debug;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import alu.linking.config.kg.EnumModelType;
import alu.linking.mentiondetection.StopwordsLoader;
import alu.linking.utils.DetectionUtils;

public class LauncherTestSurfaceForms {

	private static EnumModelType KG;

	public static void main(String[] args) {
		KG = EnumModelType.DBPEDIA_FULL;
		final StopwordsLoader stopwordsLoader = new StopwordsLoader(KG);
		try {
			final Map<String, Set<String>> map = DetectionUtils.loadSurfaceForms(KG, stopwordsLoader);
			for (Map.Entry<String, Set<String>> e : map.entrySet())
			{
				System.out.println(e.getKey()+" - "+e.getValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
