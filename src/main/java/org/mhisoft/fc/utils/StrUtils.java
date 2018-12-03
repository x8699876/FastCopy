package org.mhisoft.fc.utils;

import java.text.DecimalFormat;

import javax.xml.bind.DatatypeConverter;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2018
 */
public class StrUtils {

	static DecimalFormat df_time = new DecimalFormat("###,###.##");

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s);
	}

	public static String getDisplayTime(final long millis) {
		double _d= millis;
		if (millis<1000) {
			return  millis + " (ms)";
		}
		else {

			_d  = millis/1000; //sec
			if (_d>60) {
				_d = _d/60; //min
				return  df_time.format(_d) + " (min)";
			}
			else
				return  df_time.format(_d) + " (s)";


		}
	}
}
