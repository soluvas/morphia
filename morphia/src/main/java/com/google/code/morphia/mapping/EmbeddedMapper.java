/**
 * 
 */
package com.google.code.morphia.mapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.utils.IterHelper;
import com.google.code.morphia.utils.IterHelper.MapIterCallback;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@SuppressWarnings({"unchecked","rawtypes"})
class EmbeddedMapper implements CustomMapper{
	private static final Logr log = MorphiaLoggerFactory.get(EmbeddedMapper.class);
	
	public void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, Map<Object, DBObject> involvedObjects, Mapper mapr) {
		String name = mf.getNameToStore();
		
		Object fieldValue = mf.getFieldValue(entity);

		if (mf.isMap()) {
			writeMap(mf, dbObject, involvedObjects, name, fieldValue, mapr);
		} else if (mf.isMultipleValues()) {
			writeCollection(mf, dbObject, involvedObjects, name, fieldValue, mapr);
		} else {
			//run converters
			if (mapr.converters.hasDbObjectConverter(mf) || mapr.converters.hasDbObjectConverter(entity.getClass())) {
				mapr.converters.toDBObject(entity, mf, dbObject, mapr.getOptions());
				return;
			}
			
			final DBObject dbObj;
			if (fieldValue == null) {
				dbObj = null;
			} else {
				log.trace("Mapping " + mf.getDeclaringClass().getName() + "#" + mf.getFullName() + " from " + fieldValue);
				dbObj = mapr.toDBObject(fieldValue, involvedObjects);
			}
			if (dbObj != null) {
				if (!shouldSaveClassName(fieldValue, dbObj, mf))
					dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
				
				if (dbObj.keySet().size() > 0 || mapr.getOptions().storeEmpties) {
					dbObject.put(name, dbObj);
				}
			}
		}
	}

	private void writeCollection(final MappedField mf, final DBObject dbObject, Map<Object, DBObject> involvedObjects, String name, Object fieldValue, Mapper mapr) {
		Iterable coll = null;
		
		if (fieldValue != null)
			if (mf.isArray)
				coll =  Arrays.asList((Object[])fieldValue);
			else
				coll = (Iterable) fieldValue;
		
		if (coll != null) {
			List values = new ArrayList();
			for (Object o : coll) {
				if (null == o)
					values.add(null);
				else if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(o.getClass()))
					values.add(mapr.converters.encode(o));
				else {
					Object val;
					if (Collection.class.isAssignableFrom(o.getClass()) || Map.class.isAssignableFrom(o.getClass()))
						val = mapr.toMongoObject(o, true);
					else
						val = mapr.toDBObject(o, involvedObjects);

					if (!shouldSaveClassName(o, val, mf))
						((DBObject) val).removeField(Mapper.CLASS_NAME_FIELDNAME);
					
					values.add(val);
				}
			}
			if (values.size() > 0 || mapr.getOptions().storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}

	private void writeMap(final MappedField mf, final DBObject dbObject, Map<Object, DBObject> involvedObjects, String name, Object fieldValue, Mapper mapr) {
		if (fieldValue == null)
			return;
		
		// HACK: support EMF EMap
		final Map<String, Object> map;
		if (fieldValue instanceof Map) {
			map = (Map<String, Object>) fieldValue;
		} else {
			try {
				final Method method = fieldValue.getClass().getMethod("map");
				map = (Map<String, Object>) method.invoke(fieldValue);
				if (map == null)
					return;
			} catch (Exception e) {
				throw new MappingException("To treat " + mf + " as map, it must either typed as java.util.Map or have a map() method which returns java.util.Map.", e);
			}
		}
		
		BasicDBObject values = new BasicDBObject();
		
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object entryVal = entry.getValue();
			Object val;
			
			if (entryVal == null)
				val = null;
			else if(mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(entryVal.getClass()))
				val = mapr.converters.encode(entryVal);
			else {
				if (Map.class.isAssignableFrom(entryVal.getClass()) || Collection.class.isAssignableFrom(entryVal.getClass()))
					val = mapr.toMongoObject(entryVal, true);
				else
					val = mapr.toDBObject(entryVal, involvedObjects);
			
				if (!shouldSaveClassName(entryVal, val, mf))
					((DBObject) val).removeField(Mapper.CLASS_NAME_FIELDNAME);
			}
			
			String strKey = mapr.converters.encode(entry.getKey()).toString();
			values.put(strKey, val);
		}
		
		if (values.size() > 0 || mapr.getOptions().storeEmpties)
			dbObject.put(name, values);
	}
	
	public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		if (mf == null)
			throw new IllegalArgumentException("mappedField is null, cannot deserialize " + dbObject);
		try {
			if (mf.isMap()) {
				readMap(dbObject, mf, entity, cache, mapr);
			} else if (mf.isMultipleValues()) {
				readCollection(dbObject, mf, entity, cache, mapr);
			} else {
				// single element
				
				Object dbVal = mf.getDbObjectValue(dbObject);
				if (dbVal != null) {
					boolean isDBObject = dbVal instanceof DBObject && !(dbVal instanceof BasicDBList);
					
					//run converters						
					if (isDBObject && (mapr.converters.hasDbObjectConverter(mf) || mapr.converters.hasDbObjectConverter(mf.getType()))) {
						mapr.converters.fromDBObject(((DBObject)dbVal), mf, entity);
						return;
					} else {
						Object refObj = null;
						if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(mf.getType()))
							refObj = mapr.converters.decode(mf.getType(), dbVal, mf);
						else {
							refObj = mapr.getOptions().objectFactory.createInstance(mapr, mf, ((DBObject)dbVal));
							refObj = mapr.fromDb(((DBObject)dbVal), refObj, cache);
						}
						if (refObj != null) {
							mf.setFieldValue(entity, refObj);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Cannot deserialize field " + mf.getFullName() + " from " + dbObject, e);
		}
	}

	private void readCollection(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		// multiple documents in a List
		Collection values = mf.isSet() ? mapr.getOptions().objectFactory.createSet(mf) : mapr.getOptions().objectFactory.createList(mf);
		
		Object dbVal = mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			
			List dbVals = null;
			if (dbVal instanceof List)
				dbVals = (List) dbVal;
			else {
				dbVals = new BasicDBList();
				dbVals.add(dbVal);
			}
			
			for (Object o : dbVals) {
				
				DBObject dbObj = (DBObject) o;
				Object newEntity = null;
				
				if (dbObj != null) {
					//run converters
					if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(mf.getSubClass()))
						newEntity = mapr.converters.decode(mf.getSubClass(), dbObj, mf);
					else {
						newEntity = readMapOrCollectionOrEntity(dbObj, mf, cache, mapr);
					}
				}
				
				values.add(newEntity);
			}
		}
		if (values.size() > 0) {
			if (mf.getType().isArray()) {
				mf.setFieldValue(entity, ReflectionUtils.convertToArray(mf.getSubClass(), ReflectionUtils.iterToList(values)));
			} else {
				// Make it work with EMF EList
				if (mf.getType().isAssignableFrom( values.getClass() )) {
					// if the field type is standard Collection/List, then we can directly assign
					mf.setFieldValue(entity, values);
				} else {
					// it's not a standard JDK list, so we use List#addAll(),
					// but we need existing instance either from field or from getter
					Collection currentValue = (Collection) mf.getFieldValue(entity);
					if (currentValue == null) {
						final String getterName = "get" + Character.toUpperCase(mf.getJavaFieldName().charAt(0)) + mf.getJavaFieldName().substring(1);
						try {
							final Method getter = entity.getClass().getMethod(getterName);
							currentValue = (Collection) getter.invoke(entity);
							if (currentValue == null)
								throw new MappingException("Getter " + getter + " returns null");
						} catch (Exception e) {
							// no existing Collection instance? you're giving no choice!
							throw new MappingException("If field " + mf + " is not a JDK Collection, you must provide an instance via field/getter so Morphia can call addAll()", e);
						}
					}
					currentValue.addAll(values);
				}
			}
		}
	}
	
	private void readMap(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache, final Mapper mapr) {
		final Map<Object, Object> map = mapr.getOptions().objectFactory.createMap(mf);
		
		DBObject dbObj = (DBObject) mf.getDbObjectValue(dbObject);
		new IterHelper<Object, Object>().loopMap(dbObj, new MapIterCallback<Object, Object>() {
			@Override
			public void eval(Object key, Object val) {
				Object newEntity = null;
				
				//run converters
				if (val != null) {
					if (	mapr.converters.hasSimpleValueConverter(mf) || 
							mapr.converters.hasSimpleValueConverter(mf.getSubClass()))
						newEntity = mapr.converters.decode(mf.getSubClass(), val, mf);
					else {
						if(val instanceof DBObject)
							newEntity = readMapOrCollectionOrEntity((DBObject) val, mf, cache, mapr);
						else
							throw new MappingException("Embedded element isn't a DBObject! How can it be that is a " + val.getClass());

					}
				}

				Object objKey = mapr.converters.decode(mf.getMapKeyClass(), key);
				map.put(objKey, newEntity);			}
		});
		
		if (map.size() > 0) {
			mf.setFieldValue(entity, map);
		}
	}

	private Object readMapOrCollectionOrEntity(DBObject dbObj, MappedField mf, EntityCache cache, Mapper mapr) {
		if(Map.class.isAssignableFrom(mf.getSubClass()) || Iterable.class.isAssignableFrom(mf.getSubClass())) {
			MapOrCollectionMF mocMF = new MapOrCollectionMF((ParameterizedType)mf.getSubType());
			mapr.fromDb(dbObj, mocMF, cache);
			return mocMF.getValue();
		} else {
			Object newEntity = mapr.getOptions().objectFactory.createInstance(mapr, mf, dbObj);
			return mapr.fromDb(dbObj, newEntity, cache);
		}
	}
	
	public static boolean shouldSaveClassName(Object rawVal, Object convertedVal, MappedField mf) {
		if (rawVal == null || mf == null)
			return true;
		if (mf.isSingleValue())
			return !(mf.getType().equals(rawVal.getClass()) && !(convertedVal instanceof BasicDBList));
		else
			if ( convertedVal != null && 
				 convertedVal instanceof DBObject && 
				 !mf.getSubClass().isInterface() && 
				 !Modifier.isAbstract(mf.getSubClass().getModifiers()) && 
				 mf.getSubClass().equals(rawVal.getClass()))
				return false;
			else 
				return true;
	}

}
