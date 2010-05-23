/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class FloatConverter implements ValueConverter<Float> {
	
	@Override
	public Float objectFromValue(Object val) {
		Object dbValue = val;
		if (dbValue instanceof Double) {
			return ((Double) dbValue).floatValue();
		}
		String sVal = val.toString();
		return Float.parseFloat(sVal);
	}
	
	@Override
	public Object valueFromObject(Float t) {
		return t;
	}
	
}
