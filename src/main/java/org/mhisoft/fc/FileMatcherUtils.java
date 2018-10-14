package org.mhisoft.fc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Jul, 2018
 */
public class FileMatcherUtils {


	private static ConcurrentHashMap<String, Pattern> convertedRegExPatterns = new ConcurrentHashMap<>();



	//new String[]{"_*.repositories", "*.pom", "*-b1605.0.1*", "*-b1605.0.1", "mobile*", "*"};

	/**
	 * Convert user friendly match pattern to regular expressions.
	 */
	public static Pattern getConvertToRegularExpression(final String targetPattern) {
		if (convertedRegExPatterns.get(targetPattern)!=null) {
			return convertedRegExPatterns.get(targetPattern);
		}
		else {
			String regex = targetPattern.replace(".", "\\.");
			regex = regex.replace("?", ".?").replace("*", ".*");

			Pattern p= Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

			convertedRegExPatterns.put(targetPattern, p);
			return p;
		}
	}



	public static boolean isFileMatchTargetFilePattern(final File f,final String targetPattern) {
		if (targetPattern==null)
			return true; //nothing to match
		// f.getName().matches(getConvertToRegularExpression(targetPattern));
		Matcher m = getConvertToRegularExpression(targetPattern).matcher(f.getName());
		/*
		//matches() return true if the whole string matches the given pattern.
		// find() tries to find a substring that matches the pattern.
		*/
		return m.matches();

	}

	/**
	 * Return true as long as one file pattern matches.
	 * it checks nulls on targetPatterns. If nothing matches, return true.
	 * @param f
	 * @param targetPatterns
	 * @return
	 */
	public static boolean isFileMatchTargetFilePatterns(final File f, final String[] targetPatterns) {
		if (targetPatterns==null)
			return true; //nothing to match
		for (String targetPattern : targetPatterns) {
			boolean b = isFileMatchTargetFilePattern(f, targetPattern );
			if (b)
				return true;
		}

		return false;
	}

}
