/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv.enc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.mapper.conv.EncodingContext;
import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class CollectionOfValuesEncoder implements TypeEncoder{

	@Override
	public boolean canHandle(MappedField f) {
		return f.isMultipleValues();//relies on order
	}

	@Override
	public Object decode(EncodingContext ctx, MappedField mf, Object fromDBObject) throws MappingException {
		List list = (List) fromDBObject; // TODO what if that fails? migration
											// issue?
		Class subtype = mf.getSubType();
		if (subtype != null) {
			// map back to the java datatype
			// (List/Set/Array[])
			Collection values = createCollection(ctx, mf);

			// FIXME move
			if (subtype == Locale.class) {
				for (Object o : list) {
					values.add(ctx.getMapper().parseLocale((String) o));
				}
			} else if (subtype == Key.class) {
				for (Object o : list) {
					values.add(new Key((DBRef) o));
				}
			} else if (subtype.isEnum()) {
				for (Object o : list) {
					values.add(Enum.valueOf(subtype, (String) o));
				}
			} else {
				for (Object o : list) {
					values.add(o);
				}
			}
			
			if (mf.getType().isArray()) {
				return ctx.getMapper().convertToArray(subtype, values);
			} else {
				return values;
			}
		} else {
			// for types converted by driver?
			return list;
		}
	}

	private Collection createCollection(EncodingContext ctx, MappedField mf) {
		Collection values;
		
		if (!mf.isSet()) {
			values = (List) ctx.getMapper().tryConstructor(ArrayList.class, mf.getCTor());
		} else {
			values = (Set) ctx.getMapper().tryConstructor(HashSet.class, mf.getCTor());
		}
		return values;
	}

	@Override
	public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException {
		if (value != null) {
			Iterable iterableValues = null;
			
			if (f.getType().isArray()) {
				Object[] objects = null;
				try {
					objects = (Object[]) value;
					// TODO us see if possible without catching exception
				} catch (ClassCastException e) {
					// store the primitive array without making
					// it into a list.
					if (Array.getLength(value) == 0) {
						return null;
					}
					return value;
				}
				// convert array into arraylist
				iterableValues = new ArrayList(objects.length);
				for (Object obj : objects) {
					((ArrayList) iterableValues).add(obj);
				}
			} else {
				// cast value to a common interface
				iterableValues = (Iterable) value;
			}
			
			List values = new ArrayList();
			
			if (f.getSubType() != null) {
				for (Object o : iterableValues) {
					values.add(ctx.getMapper().objectToValue(f.getSubType(), o));
				}
			} else {
				for (Object o : iterableValues) {
					values.add(ctx.getMapper().objectToValue(o));
				}
			}
			if (values.size() > 0) {
				return values;
			}
		}
		return null;
	}
	
}