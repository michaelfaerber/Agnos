package alu.linking.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.internal.Lists;

public class AnalysisUtils {
	public static Collection<? extends Object> extractList(Object jsonObj) {
		final Collection<Object> ret = Lists.newArrayList();
		if (jsonObj instanceof List) {
			List<Object> list = ((List) jsonObj);
			for (Object o : list) {
				if (o instanceof Collection) {
					ret.addAll(extractList(o));
				} else {
					ret.add(o);
				}
			}
		}
		return ret;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		list.sort(Entry.comparingByValue(Comparator.reverseOrder()));
		Map<K, V> result = new LinkedHashMap<>();
		for (Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

}
