package alu.linking.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;

public class WalkUtils {
	public static List<String> getBlacklist(final EnumModelType KG) throws IOException {
		final List<String> blacklist = Lists.newArrayList();
		try (BufferedReader brIn = new BufferedReader(
				new FileReader(FilePaths.FILE_GRAPH_WALK_BLACKLIST_PREDICATE.getPath(KG)))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				blacklist.add(line);
			}
		}
		return blacklist;
	}
}
