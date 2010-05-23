/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

import java.util.Locale;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class LocaleConverter implements ValueConverter<Locale> {
	
	@Override
	public Locale objectFromValue(Object val) {
		return SimpleValueConverter.parseLocale(val.toString());
	}
	
	@Override
	public Object valueFromObject(Locale t) {
		return t.toString();
	}
	
}
