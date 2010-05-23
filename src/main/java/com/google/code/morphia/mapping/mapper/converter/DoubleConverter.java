/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class DoubleConverter implements ValueConverter<Double> {
	
	@Override
	public Double objectFromValue(Object val) {
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
	
	@Override
	public Object valueFromObject(Double t) {
		return t;
	}
	
}
