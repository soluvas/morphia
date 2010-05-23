/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class ShortConverter implements ValueConverter<Short> {
	
	@Override
	public Short objectFromValue(Object val) {
		Object dbValue = val;
		if (dbValue instanceof Double) {
			return ((Double) dbValue).shortValue();
		} else if (dbValue instanceof Integer) {
			return ((Integer) dbValue).shortValue();
		}
		String sVal = val.toString();
		return Short.parseShort(sVal);
	}
	
	@Override
	public Object valueFromObject(Short t) {
		return t;
	}
	
}
