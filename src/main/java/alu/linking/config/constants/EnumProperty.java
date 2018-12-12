package alu.linking.config.constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import alu.linking.config.kg.EnumModelType;

public enum EnumProperty {

	// ##################################
	// # User Data (i.e. basic client authentication)
	// ##################################
	AUTHENTICATE_TAGTOG(FilePaths.AUTHENTICATION_TAGTOG), //
	AUTHENTICATE_TAGTOG_TESTING(FilePaths.AUTHENTICATION_TAGTOG), //
	AUTHENTICATE_BABELFY(FilePaths.AUTHENTICATION_BABELFY),//
	;
	private final FilePaths path;

	EnumProperty(final FilePaths path) {
		this.path = path;
	}

	/**
	 * Queries this property file for the specified keyword
	 * 
	 * @param keyword
	 * @return
	 */
	public char[] get(final EnumModelType KG, final String keyword) {
		try (InputStream is = new FileInputStream(new File(this.path.getPath(KG)))) {
			final Properties prop = new Properties();
			prop.load(is);
			// Long call-chain for fail-fast behaviour
			char[] ret = prop.getProperty(keyword).toCharArray();
			// A bit overkill, but as prop.clear just sets the Entry<?,?> values to null, it
			// is still up to the GC, so rather explicitly overwrite it
			prop.setProperty(keyword, "");
			prop.clear();
			return ret;
		} catch (IOException e) {
			// Likely a FNFE from a sub-optimally set-up project environment
			e.printStackTrace();
		} catch (NullPointerException npe) {
			throw new IllegalArgumentException("Invalid value requested. (" + keyword + ") for " + this.path.getPath(KG));
		}
		// Shouldn't be reached as it either defaults to the try-block or should throw a
		// NPE if no valid value is found
		return null;
	}
}
