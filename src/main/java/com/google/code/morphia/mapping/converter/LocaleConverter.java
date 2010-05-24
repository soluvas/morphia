/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import java.util.Locale;
import java.util.StringTokenizer;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class LocaleConverter extends TypeConverter {
	// FIXME us needs testing
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, Locale.class);
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		return parseLocale(fromDBObject.toString());
	}
	
	@Override
	Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null)
			return null;
		return value.toString(); // TODO is that safe?
	}
	
	public static Locale parseLocale(final String localeString) {
		if ((localeString != null) && (localeString.length() > 0)) {
			StringTokenizer st = new StringTokenizer(localeString, "_");
			String language = st.hasMoreElements() ? st.nextToken() : Locale.getDefault().getLanguage();
			String country = st.hasMoreElements() ? st.nextToken() : "";
			String variant = st.hasMoreElements() ? st.nextToken() : "";
			return new Locale(language, country, variant);
		}
		return null;
	}
}
