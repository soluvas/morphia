/**
 * 
 */
package com.google.code.morphia.mapping.converter;

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
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CollectionOfValuesConverter extends TypeConverter {
	private final ConverterChain chain;
	
	public CollectionOfValuesConverter(ConverterChain chain) {
		this.chain = chain;
	}
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return c.isArray() || ReflectionUtils.isCollection(c);
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField mf) throws MappingException {
		List list = (List) fromDBObject; // TODO what if that fails? migration
		// issue?
		Class subtype = mf.getSubType();
		if (subtype != null) {
			// map back to the java datatype
			// (List/Set/Array[])
			Collection values = createCollection(mf);
			
			// FIXME move
			if (subtype == Locale.class) {
				for (Object o : list) {
					// FIXME code dup.
					values.add(LocaleConverter.parseLocale((String) o));
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
				return ReflectionUtils.convertToArray(subtype, values);
			} else {
				return values;
			}
		} else {
			// for types converted by driver?
			return list;
		}
	}
	
	private Collection createCollection(final MappedField mf) {
		Collection values;
		
		if (!mf.isSet()) {
			values = (List) ReflectionUtils.newInstance(mf.getCTor(), ArrayList.class);
		} else {
			values = (Set) ReflectionUtils.newInstance(mf.getCTor(), HashSet.class);
		}
		return values;
	}
	
	@Override
	Object encode(Object value, MappedField f) {
		
		if (value == null)
			return null;
		
		Iterable iterableValues = null;
		
		if (value.getClass().isArray()) {
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
		
		if (f != null && f.getSubType() != null) {
			for (Object o : iterableValues) {
				values.add(chain.encode(f.getSubType(), o));
			}
		} else {
			for (Object o : iterableValues) {
				values.add(chain.encode(o));
			}
		}
		if (values.size() > 0) {
			return values;
		} else
			return null;
		
	}
	
}
