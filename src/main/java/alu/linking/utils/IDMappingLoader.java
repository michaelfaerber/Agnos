package alu.linking.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.log4j.Logger;

import alu.linking.config.constants.Strings;

public class IDMappingLoader<V> {
	private static Logger logger = Logger.getLogger(IDMappingLoader.class);

	private DualHashBidiMap<String, V> mappingRaw = null;
	private final DualHashBidiMap<String, String> mappingHuman = new DualHashBidiMap<String, String>();
	private final String tokenSeparator;

	public void translate(final IDMappingLoader<V> to, final File inputFile, final File outputFile) throws FileNotFoundException, IOException {

		if (mappingRaw != null && mappingRaw.size() > 0) {
			try (final BufferedReader br = new BufferedReader(new FileReader(inputFile)))
			{
				String line = null;
				while ((line=br.readLine())!=null)
				{
					line.split(tokenSeparator);
				}
			}
			for (Map.Entry<String, V> e : this.mappingRaw.entrySet()) {
				final String key = e.getKey();
				final V uri = e.getValue();
				final String otherKey = to.getKey(uri);
			}
		} else if (mappingHuman != null && mappingHuman.size() > 0) {

		}

	}

	public IDMappingLoader() {
		this(Strings.ID_MAPPING_SEPARATOR.val);
	}

	public IDMappingLoader(final String separator) {
		this.tokenSeparator = separator;
	}

	/**
	 * Loads a human-readable mapping from the specified files
	 * 
	 * @param humanMappingFile file containing the mapping
	 * @return this for chaining
	 * @throws IOException
	 */
	public IDMappingLoader<V> loadHumanFile(final File humanMappingFile) throws IOException {
		mappingRaw = null;
		mappingHuman.clear();
		logger.info("Loading human-readable ID Mapping from: " + humanMappingFile.getAbsolutePath());
		try (final BufferedReader brIn = new BufferedReader(new FileReader(humanMappingFile))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				final String[] tokens = line.split(this.tokenSeparator);
				if (tokens.length != 2) {
					throw new RuntimeException("Invalid number of tokens(" + tokens.length + ") for line:" + line);
				}
				this.mappingHuman.put(tokens[0], tokens[1]);
			}
		}
		logger.info("Finished loading human-readable ID Mapping(" + this.mappingHuman.size() + ")!");
		return this;
	}

	/**
	 * Loads a raw file from the specified location, translating it into the
	 * appropriate type of mapping map
	 * 
	 * @param rawMappingFile
	 * @return this for chaining
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public IDMappingLoader<V> loadRawFile(final File rawMappingFile) throws ClassNotFoundException, IOException {
		mappingRaw = null;
		mappingHuman.clear();
		logger.info("Loading RAW ID Mapping from: " + rawMappingFile.getAbsolutePath());
		try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(rawMappingFile))) {
			this.mappingRaw = (DualHashBidiMap<String, V>) ois.readObject();
		}
		logger.info("Finished loading raw ID Mapping(" + this.mappingRaw.size() + ")!");

		return this;
	}

	/**
	 * Gets the object for the given mapping-key from the RAW map<br>
	 * Note: Requires loading of the raw mapping.
	 * 
	 * @param key what the wanted object is mapped with
	 * @return wanted object for the given key
	 */
	public V getMappingRaw(final String key) {
		return this.mappingRaw.get(key);
	}

	/**
	 * Gets the object for the given mapping-key from the human-readable mapping<br>
	 * Note: Requires loading of the human readable mapping.
	 * 
	 * @param key what the wanted object is mapped with
	 * @return wanted object for the given key
	 */
	public String getMappingHuman(final String key) {
		return this.mappingHuman.get(key);
	}

	/**
	 * Gets the mapping for given key.<br>
	 * Prioritises raw mapping if existing and returns the .toString()
	 * representation for it<br>
	 * If no raw mapping is defined, it returns the value found in the
	 * human-readable mapping (if loaded).<br>
	 * If neither is loaded, will always return NULL
	 * 
	 * @param key that has a value associated to it
	 * @return value associated to key mapping
	 */
	public String getMapping(final String key) {
		String ret = null;
		if (this.mappingRaw != null && this.mappingRaw.size() > 0) {
			final V tempRet = getMappingRaw(key);
			if (tempRet != null) {
				ret = tempRet.toString();
			}
		}
		if (ret != null) {
			return ret;
		}
		if (this.mappingHuman != null && this.mappingHuman.size() > 0) {
			return getMappingHuman(key);
		}
		return ret;
	}

	public String getKey(final V uri) {
		String ret = null;
		if (this.mappingRaw != null && this.mappingRaw.size() > 0) {
			final String tempRet = this.mappingRaw.getKey(uri);
			if (tempRet != null) {
				ret = tempRet;
			}
		}
		if (ret != null) {
			return ret;
		}
		if (this.mappingHuman != null && this.mappingHuman.size() > 0) {
			final String tempRet = this.mappingHuman.getKey(uri);
			if (tempRet != null) {
				ret = tempRet;
			}
		}
		return ret;
	}

}
