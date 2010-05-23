/**
 * 
 */
package com.google.code.morphia.mapping.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class ByteConverter implements ValueConverter<Byte> {
	
	@Override
	public Byte objectFromValue(Object val) {
		Object dbValue = val;
		if (dbValue instanceof Double) {
			return ((Double) dbValue).byteValue();
		} else if (dbValue instanceof Integer) {
			return ((Integer) dbValue).byteValue();
		}
		String sVal = val.toString();
		return Byte.parseByte(sVal);
	}
	
	@Override
	public Object valueFromObject(Byte t) {
		return t;
	}
	
}
