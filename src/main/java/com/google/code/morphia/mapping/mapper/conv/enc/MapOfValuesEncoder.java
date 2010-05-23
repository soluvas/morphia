/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv.enc;

import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.mapper.conv.EncodingContext;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class MapOfValuesEncoder implements TypeEncoder {
	
	@Override
	public boolean canHandle(MappedField f) {
		if (f.getAnnotation(Reference.class) != null)
			return false;
		if (f.getAnnotation(Serialized.class) != null)
			return false;
		// TDOO !? isn´t a map always embedded?
		// if (f.getAnnotation(Embedded.class) != null)
		// return false;
		//		
		return f.isMap();
	}
	
	@Override
	public Object decode(EncodingContext ctx, MappedField f, Object fromDBObject) throws MappingException {
			Map<Object, Object> map = (Map<Object, Object>) fromDBObject;
			Map values = (Map) ctx.getMapper().tryConstructor(HashMap.class, f
					.getCTor());//FIXME
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				Object objKey = ctx.getMapper().objectFromValue(f.getMapKeyType(),
						entry.getKey());
				values.put(objKey, ctx.getMapper().objectFromValue(f.getSubType(),
						entry.getValue()));
			}
		return values;
	}
	
	@Override
	public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException {
		Map<Object, Object> map = (Map<Object, Object>) value;
		if ((map != null) && (map.size() > 0)) {
			Map mapForDb = new HashMap();
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				String strKey = ctx.getMapper().objectToValue(entry.getKey()).toString();
				mapForDb.put(strKey, ctx.getMapper().objectToValue(entry.getValue()));
			}
			return mapForDb;
		}
		return null;
	}
	
}
