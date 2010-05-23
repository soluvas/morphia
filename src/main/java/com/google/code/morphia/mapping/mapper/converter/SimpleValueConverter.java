/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.google.code.morphia.Key;
import com.mongodb.BasicDBObject;
import com.mongodb.ObjectId;

public class SimpleValueConverter {

	private static Map<Class<?>, ValueConverter<?>> supportedTypes = new HashMap();

	static {
		// this should not be static, as the user might want to extend this
		// list.

		supportedTypes.put(byte.class, new ByteConverter());
		supportedTypes.put(Byte.class, new ByteConverter());
		supportedTypes.put(char.class, new CharacterConverter());
		supportedTypes.put(Character.class, new CharacterConverter());
		supportedTypes.put(int.class, new IntegerConverter());
		supportedTypes.put(Integer.class, new IntegerConverter());
		supportedTypes.put(short.class, new ShortConverter());
		supportedTypes.put(Short.class, new ShortConverter());
		supportedTypes.put(long.class, new LongConverter());
		supportedTypes.put(Long.class, new LongConverter());
		supportedTypes.put(float.class, new FloatConverter());
		supportedTypes.put(Float.class, new FloatConverter());
		supportedTypes.put(double.class, new DoubleConverter());
		supportedTypes.put(Double.class, new DoubleConverter());
		//
		supportedTypes.put(String.class, new StringConverter());
		supportedTypes.put(Locale.class, new LocaleConverter());
		supportedTypes.put(Key.class, new KeyConverter());

	}

	private static final Logger logger = Logger.getLogger(SimpleValueConverter.class.getName());

	/** Converts known types from mongodb -> java. */
	public static Object objectFromValue(final Class javaType,
			final Object val) {
	
		if (val == null) {
			return null;
		}
		
		if (javaType == null) {
			// no conversion. used by untyped maps
			return val;
		}
	
		Class valType = val.getClass();
		if (SimpleValueConverter.compatibleTypes(javaType, valType)) {
			return val;
		}
	
		ValueConverter<?> c = supportedTypes.get(javaType);
		if (c != null)
			return c.objectFromValue(val);

		if (javaType.isEnum()) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			return Enum.valueOf(javaType, val.toString());
		} else {
			// Not a known convertible type.
			
			// TODO scott: please review: should we not yell here?
			throw new IllegalArgumentException("Conversion from " + val.getClass().getName() + " to "
					+ javaType.getName() + " cannot be achieved.");
			// was: return val;
		}
	}

	/**
	 * Converts known types from java -> mongodb. Really it just converts enums
	 * and locales to strings
	 */
	public static Object objectToValue(Class javaType, final Object obj) {
		if (obj == null) {
			return null;
		}
		if (javaType == null) {
			javaType = obj.getClass();
		}
		

		ValueConverter c = supportedTypes.get(javaType);
		if (c != null)
			return c.valueFromObject(obj);
		//
		// if (javaType.isEnum()) {
		// logger.finer("Converting from " + javaType.getSimpleName()
		// + " to String");
		// return ((Enum) obj).name();
		// }else
		// return obj;
		// // throw new IllegalArgumentException("Conversion from " +
		// // obj.getClass().getName() + " to "
		// // + javaType.getName() + " cannot be achieved.");

		if (javaType.isEnum()) {
			logger.finer("Converting from " + javaType.getSimpleName()
					+ " to String");
			return ((Enum) obj).name();
		} else if (javaType == Locale.class) {
			logger.finer("Converting from " + javaType.getSimpleName() + " to String");
			return ((Locale) obj).toString();
		} else if ((javaType == char.class) || (javaType == Character.class)) {
			logger.finer("Converting from " + javaType.getSimpleName() + " to String");
			return ((Character) obj).toString();
		} else if (javaType == Key.class) {
			logger.finer("Converting from " + javaType.getSimpleName() + " to DBRef");
			
			return ((Key) obj).toRef();
			// TODO scott: can we get away with this?
			// was: return ((Key) obj).toRef(this);
		} else {
			if (SimpleValueConverter.compatibleTypes(javaType, obj.getClass())) {
				return obj;
			}
 else {
				throw new IllegalArgumentException("Conversion from " + obj.getClass().getName() + " to "
						+ javaType.getName() + " cannot be achieved.");
			}
			// return obj;
		}
	}

	public static boolean compatibleTypes(final Class type1, final Class type2) {
		if (type1.equals(type2)) {
			return true;
		}
		// TODO scott: please review, looks broken.
		return (type1.isAssignableFrom(type2) || ((type1.isPrimitive() || type2
				.isPrimitive()) && type2.getSimpleName().toLowerCase().equals(
						type1.getSimpleName().toLowerCase())));// &&
		// valType.getName().startsWith("java.lang") &&
		// javaType.getName().startsWith("java.lang") ));
	
	}

	/** Converts known types from mongodb -> java. */
	public static Object objectFromValue(final Class javaType,
			final BasicDBObject dbObject, final String name) {
		return objectFromValue(javaType, dbObject.get(name));
	}

	/**
	 * Converts known types from java -> mongodb. Really it just converts enums
	 * and locales to strings
	 */
	public static Object objectToValue(final Object obj) {
		if (obj == null) {
			return null;
		}
		return objectToValue(obj.getClass(), obj);
	}

	public static Locale parseLocale(final String localeString) {
		if ((localeString != null) && (localeString.length() > 0)) {
			StringTokenizer st = new StringTokenizer(localeString, "_");
			String language = st.hasMoreElements() ? st.nextToken() : Locale
					.getDefault().getLanguage();
			String country = st.hasMoreElements() ? st.nextToken() : "";
			String variant = st.hasMoreElements() ? st.nextToken() : "";
			return new Locale(language, country, variant);
		}
		return null;
	}

	/** turns the object into an ObjectId if it is/should-be one */
	public static Object asObjectIdMaybe(final Object id) {
		try {
			if ((id instanceof String) && ObjectId.isValid((String) id)) {
				return new ObjectId((String) id);
			}
		} catch (Exception e) {
			// sometimes isValid throws exceptions... bad!
		}
		return id;
	}

	public static Object[] convertToArray(final Class type, final Collection values) {
		Object exampleArray = Array.newInstance(type, 1);
		Object[] array = ((ArrayList) values).toArray((Object[]) exampleArray);
		return array;
	}
	
	public static boolean isSupportedType(Class type) {
		return supportedTypes.containsKey(type);
	}
	
}
