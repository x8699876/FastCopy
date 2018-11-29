package org.mhisoft.fc.utils;

import javax.xml.bind.DatatypeConverter;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2018
 */
public class StrUtils {

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s);
	}

}
