package alu.linking.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

public abstract class JSONDecoder {
	public static Map<String, Object> decode(final Reader reader) throws IOException {
		final JSONTokener tokener = new JSONTokener(reader);
		Object obj = null;
		Map<String, Object> jsonMap = null;
		final JSONObject jsonObj = ((JSONObject) tokener.nextValue());

		jsonMap = jsonObj.toMap();

		/*
		 * while ((obj = tokener.nextValue()) != null) { JSONObject jsonObj =
		 * ((JSONObject) obj); System.out.println(jsonObj.toMap()); jsonMap =
		 * jsonObj.toMap(); for (Map.Entry e : jsonMap.entrySet()) {
		 * System.out.println("K:" + e.getKey()); System.out.println("V(" +
		 * e.getValue().getClass() + "):" + e.getValue()); if (e.getValue() instanceof
		 * java.util.ArrayList) { for (Object o : ((java.util.ArrayList) e.getValue()))
		 * { System.out.println("O:" + o); } } } }
		 */
		reader.close();
		return jsonMap;
	}

	/**
	 * Tokenizes a JSON file and takes the first object it detects and returns it
	 * 
	 * @param reader
	 *            input reader
	 * @return first found JSONObject instance
	 * @throws IOException
	 *             if something goes wrong while reading
	 */
	public static JSONObject grabFirst(final Reader reader) throws IOException {
		final JSONTokener tokener = new JSONTokener(reader);
		final JSONObject jsonObj = ((JSONObject) tokener.nextValue());
		reader.close();
		return jsonObj;
	}

}
