/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class DoubleConverter extends TypeConverter {
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, double.class, Double.class);
	}
	
	@Override
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val instanceof Double) {
			return (Double) val;
		}
		Object dbValue = val;
		if (dbValue instanceof Number) {
			return ((Number) dbValue).doubleValue();
		}
		String sVal = val.toString();
		return Double.parseDouble(sVal);
	}
	
}
