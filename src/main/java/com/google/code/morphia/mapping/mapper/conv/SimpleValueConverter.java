/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.google.code.morphia.Key;
import com.mongodb.BasicDBObject;
import com.mongodb.DBRef;
import com.mongodb.ObjectId;

public class SimpleValueConverter {

	private SimpleValueConverter() {
	}

	private static final Logger logger = Logger.getLogger(SimpleValueConverter.class.getName());

	/** Converts known types from mongodb -> java. */
	public static Object objectFromValue(final Class javaType,
			final Object val) {
	
		if (val == null) {
			return null;
		}
		if (javaType == null) {
			return val;
		}
	
		Class valType = val.getClass();
	
		if (SimpleValueConverter.compatibleTypes(javaType, valType)) {
			return val;
		}
	
		if (javaType == String.class) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			return val.toString();
		} else if ((javaType == Character.class) || (javaType == char.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			return val.toString().charAt(0);
		} else if ((javaType == Integer.class) || (javaType == int.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			if (val instanceof String) {
				return Integer.parseInt((String) val);
			} else {
				return ((Number) val).intValue();
			}
		} else if ((javaType == Long.class) || (javaType == long.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			if (val instanceof String) {
				return Long.parseLong((String) val);
			} else {
				return ((Number) val).longValue();
			}
		} else if ((javaType == Byte.class) || (javaType == byte.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			Object dbValue = val;
			if (dbValue instanceof Double) {
				return ((Double) dbValue).byteValue();
			} else if (dbValue instanceof Integer) {
				return ((Integer) dbValue).byteValue();
			}
			String sVal = val.toString();
			return Byte.parseByte(sVal);
		} else if ((javaType == Short.class) || (javaType == short.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			Object dbValue = val;
			if (dbValue instanceof Double) {
				return ((Double) dbValue).shortValue();
			} else if (dbValue instanceof Integer) {
				return ((Integer) dbValue).shortValue();
			}
			String sVal = val.toString();
			return Short.parseShort(sVal);
		} else if ((javaType == Float.class) || (javaType == float.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			Object dbValue = val;
			if (dbValue instanceof Double) {
				return ((Double) dbValue).floatValue();
			}
			String sVal = val.toString();
			return Float.parseFloat(sVal);
		} else if ((javaType == Double.class) || (javaType == double.class)) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			if (val instanceof Double) {
				return val;
			}
			Object dbValue = val;
			if (dbValue instanceof Number) {
				return ((Number) dbValue).doubleValue();
			}
			String sVal = val.toString();
			return Double.parseDouble(sVal);
		} else if (javaType == Locale.class) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			return SimpleValueConverter.parseLocale(val.toString());
		} else if (javaType.isEnum()) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			return Enum.valueOf(javaType, val.toString());
		} else if (javaType == Key.class) {
			logger.finer("Converting from " + val.getClass().getSimpleName()
					+ " to " + javaType.getSimpleName());
			return new Key((DBRef) val);
		} else {
			// Not a known convertible
			// type.
			return val;
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
	
		if (javaType.isEnum()) {
			logger.finer("Converting from " + javaType.getSimpleName()
					+ " to String");
			return ((Enum) obj).name();
		} else if (javaType == Locale.class) {
			logger.finer("Converting from " + javaType.getSimpleName()
					+ " to String");
			return ((Locale) obj).toString();
		} else if ((javaType == char.class) || (javaType == Character.class)) {
			logger.finer("Converting from " + javaType.getSimpleName()
					+ " to String");
			return ((Character) obj).toString();
		} else if (javaType == Key.class) {
			logger.finer("Converting from " + javaType.getSimpleName()
					+ " to DBRef");
			
			return ((Key) obj).toRef();
			// TODO scott: can we get away with this?
			// was: return ((Key) obj).toRef(this);
		} else {
			return obj;
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
	
}
