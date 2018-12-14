package alu.linking.mentiondetection.exact;

import java.util.HashMap;

public class HashMapCaseInsensitive<V> extends HashMap<String, V> {
	private static final long serialVersionUID = -8430533644019709511L;

	@Override
	public V put(String key, V value) {
			return super.put(key.toString().toLowerCase(), value);
	}

	@Override
	public V get(Object key) {
		if (key instanceof String)
		{
			return super.get(((String)key).toLowerCase());
		}
		return super.get(key);
	}
}
