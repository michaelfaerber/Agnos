package alu.linking.utils;

import java.util.Map;

public class DebugUtils {
	public static void print(Map<Object, Object> m)
	{
		for (Map.Entry<Object, Object> e : m.entrySet())
		{
			System.out.println("Key("+e.getKey().getClass()+"):"+e.getKey()+" -- Val("+e.getValue().getClass()+"):"+e.getValue());
		}
	}
}
