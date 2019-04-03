package alu.linking.api.debug;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class LauncherDecoderTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final String entityURL = "http://dbpedia.org/resource/RAF_G%C3%BCtersloh";
		try {
			final String decoded = URLDecoder.decode(entityURL, "UTF8");
			System.out.println("Decoded = " + decoded);
		} catch (UnsupportedEncodingException uee) {
			// #WeTried
			uee.printStackTrace();
		}

	}

}
