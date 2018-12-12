package alu.linking.preprocessing.dbpedia.spotlight;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import alu.linking.utils.JSONDecoder;

public class SpotlightDecoder extends JSONDecoder {

	public static ArrayList decodeResources(final Reader reader) throws IOException {
		return (java.util.ArrayList) decode(reader).get("Resources");
	}

	public static Map convertToSurfaceFormMap(java.util.ArrayList list) {
		final HashMap<String, Object> annotatedResourceMap = new HashMap<>();
		for (Object obj : list) {
			if (obj instanceof HashMap) {
				final HashMap m = ((HashMap) obj);
				final String surface_form = m.get("@surfaceForm").toString();
				final String offset = m.get("@offset").toString();
				// final String key = DecoderUtils.combineSFormOffset(surface_form, offset);
				final String key = m.get("@URI").toString();
				annotatedResourceMap.put(key, obj);
			} else {
				throw new IllegalArgumentException("Should be a HashMap, instead was given: " + obj.getClass());
			}
		}
		return annotatedResourceMap;
	}

}
