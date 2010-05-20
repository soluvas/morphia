/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.google.code.morphia.mapping;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.EntityInterceptor;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.lazy.CGLibLazyProxyFactory;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;
import com.google.code.morphia.mapping.lazy.DefaultDatastoreProvider;
import com.google.code.morphia.mapping.lazy.LazyProxyFactory;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityMap;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBBinary;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.ObjectId;

/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class Mapper {
	private static final Logger logger = Logger.getLogger(Mapper.class
			.getName());

	public static final String ID_KEY = "_id";
	public static final String IGNORED_FIELDNAME = ".";
	public static final String CLASS_NAME_FIELDNAME = "className";

	/** Set of classes that have been validated for mapping by this mapper */
	private final ConcurrentHashMap<String, MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
	private final ThreadLocal<Map<String, Object>> entityCache = new ThreadLocal<Map<String, Object>>();
	private final ThreadLocal<Map<String, Object>> proxyCache = new ThreadLocal<Map<String, Object>>();
	private final ConcurrentLinkedQueue<EntityInterceptor> interceptors = new ConcurrentLinkedQueue<EntityInterceptor>();

	public Mapper() {
	}

	public void addInterceptor(final EntityInterceptor ei) {
		interceptors.add(ei);
	}

	public Collection<EntityInterceptor> getInterceptors() {
		return interceptors;
	}

	public boolean isMapped(final Class c) {
		return mappedClasses.containsKey(c.getCanonicalName());
	}

	public void addMappedClass(final Class c) {
		MappedClass mc = new MappedClass(c, this);
		mc.validate();
		mappedClasses.put(c.getCanonicalName(), mc);
	}

	public MappedClass addMappedClass(final MappedClass mc) {
		mc.validate();
		mappedClasses.put(mc.getClazz().getCanonicalName(), mc);
		return mc;
	}

	public Map<String, MappedClass> getMappedClasses() {
		return mappedClasses;
	}

	/**
	 * Gets the mapped class for the object (type). If it isn't mapped, create a
	 * new class and cache it (without validating).
	 */
	public MappedClass getMappedClass(final Object obj) {
		if (obj == null) {
			return null;
		}
		Class type = (obj instanceof Class) ? (Class) obj : obj.getClass();
		MappedClass mc = mappedClasses.get(type.getCanonicalName());
		if (mc == null) {
			// no validation
			mc = new MappedClass(type, this);
			mappedClasses.put(mc.getClazz().getCanonicalName(), mc);
		}
		return mc;
	}

	public void clearHistory() {
		entityCache.remove();
	}

	public String getCollectionName(final Object object) {
		MappedClass mc = getMappedClass(object);
		return mc.getCollectionName();
	}

	private String getId(final Object entity) {
		try {
			if (entity instanceof ProxiedEntityReference) {
				ProxiedEntityReference proxy = (ProxiedEntityReference) entity;
				return proxy.__getEntityId();
			}
			return (String) getMappedClass(entity).getIdField().get(entity);
		} catch (IllegalAccessException iae) {
			throw new RuntimeException(iae);
		}
	}

	/**
	 * Updates the {@code @Id} and {@code @CollectionName} fields.
	 * 
	 * @param entity
	 *            The object to update
	 * @param dbId
	 *            Value to update with; null means skip
	 * @param dbNs
	 *            Value to update with; null or empty means skip
	 */
	public void updateKeyInfo(final Object entity, final Object dbId,
			final String dbNs) {
		MappedClass mc = getMappedClass(entity);
		// update id field, if there.
		if ((mc.getIdField() != null) && (dbId != null)) {
			try {
				Object dbIdValue = objectFromValue(mc.getIdField().getType(),
						dbId);
				Object idValue = mc.getIdField().get(entity);
				if (idValue != null) {
					// The entity already had an id set. Check to make sure it
					// hasn't changed. That would be unexpected, and could
					// indicate a bad state.
					if (!dbIdValue.equals(idValue)) {
						throw new RuntimeException("id mismatch: " + idValue
								+ " != " + dbIdValue + " for "
								+ entity.getClass().getName());
					}
				} else {
					mc.getIdField().set(entity, dbIdValue);
				}
			} catch (Exception e) {
				if (e.getClass().equals(RuntimeException.class)) {
					throw (RuntimeException) e;
				}

				throw new RuntimeException(e);
			}
		}
	}

	Class getClassForName(final String className, final Class defaultClass) {
		if (mappedClasses.containsKey(className)) {
			return mappedClasses.get(className).getClazz();
		}
		try {
			Class c = Class.forName(className, true, Thread.currentThread()
					.getContextClassLoader());
			return c;
		} catch (ClassNotFoundException ex) {
			return defaultClass;
		}
	}

	protected Object createInstance(final Class entityClass,
			final BasicDBObject dbObject) {
		// see if there is a className value
		String className = (String) dbObject.get(CLASS_NAME_FIELDNAME);
		Class c = entityClass;
		if (className != null) {
			// try to Class.forName(className) as defined in the dbObject first,
			// otherwise return the entityClass
			c = getClassForName(className, entityClass);
		}
		return createInstance(c);
	}

	/** Gets a no-arg constructor and calls it via reflection. */
	protected Object createInstance(final Class type) {
		try {
			// allows private/protected constructors
			Constructor constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * creates an instance of testType (if it isn't Object.class or null) or
	 * fallbackType
	 */
	protected Object tryConstructor(final Class fallbackType,
			final Constructor tryMe) {
		if (tryMe != null) {
			tryMe.setAccessible(true);
			try {
				return tryMe.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return createInstance(fallbackType);
	}

	/** coverts a DBObject back to a type-safe java object */
	public Object fromDBObject(final Class entityClass,
			final BasicDBObject dbObject) {
		if (dbObject == null) {
			Throwable t = new Throwable();
			logger.log(Level.SEVERE,
					"Somebody passed in a null dbObject; bad client!", t);
			return null;
		}

		entityCache.set(new HashMap<String, Object>());
		proxyCache.set(new HashMap<String, Object>());
		Object entity = null;
		try {
			entity = createInstance(entityClass, dbObject);
			mapDBObjectToEntity(dbObject, entity);
		} finally {
			entityCache.remove();
			proxyCache.remove();
		}
		return entity;
	}

	/**
	 * converts a java object to a mongo object (possibly a DBObject for complex
	 * mappings)
	 */
	public Object toMongoObject(final Object javaObj) {
		if (javaObj == null) {
			return null;
		}
		Class origClass = javaObj.getClass();
		Object newObj = objectToValue(origClass, javaObj);
		Class type = newObj.getClass();
		boolean bSameType = origClass.equals(type);
		boolean bSingleValue = true;
		Class subType = null;

		if (type.isArray()
				|| ReflectionUtils.implementsAnyInterface(type, Iterable.class,
						Collection.class, List.class, Set.class, Map.class)) {
			bSingleValue = false;
			// subtype of Long[], List<Long> is Long
			subType = (type.isArray()) ? type.getComponentType()
					: ReflectionUtils.getParameterizedClass(type);
		}

		if (bSameType && bSingleValue && !ReflectionUtils.isPropertyType(type)) {
			DBObject dbObj = toDBObject(javaObj);
			dbObj.removeField(CLASS_NAME_FIELDNAME);
			return dbObj;
		} else if (bSameType && !bSingleValue
				&& !ReflectionUtils.isPropertyType(subType)) {
			ArrayList<Object> vals = new ArrayList<Object>();
			if (type.isArray()) {
				for (Object obj : (Object[]) newObj) {
					vals.add(toMongoObject(obj));
				}
			} else {
				for (Object obj : (Iterable) newObj) {
					vals.add(toMongoObject(obj));
				}
			}
			return vals;
		} else {
			return newObj;
		}
	}

	public DBObject toDBObject(final Object entity) {
		return toDBObject(entity, null);
	}

	/**
	 * converts an entity to a DBObject
	 */
	public DBObject toDBObject(final Object entity,
			final LinkedHashMap<Object, DBObject> involvedObjects) {
		BasicDBObject dbObject = new BasicDBObject();

		MappedClass mc = getMappedClass(entity);

		dbObject
		.put(CLASS_NAME_FIELDNAME, entity.getClass().getCanonicalName());

		// if ( mc.getPolymorphicAnnotation() != null ) {
		// dbObject.put(CLASS_NAME_FIELDNAME,
		// entity.getClass().getCanonicalName());
		// }

		dbObject = (BasicDBObject) mc.callLifecycleMethods(PrePersist.class,
				entity, dbObject, this);
		for (MappedField mf : mc.getPersistenceFields()) {
			try {
				if (mf.hasAnnotation(Id.class)) {
					Object dbVal = mf.getFieldValue(entity);
					if (dbVal != null) {
						dbObject.put(ID_KEY,
								objectToValue(asObjectIdMaybe(dbVal)));
					}
				} else if (mf.hasAnnotation(Reference.class)) {
					mapReferencesToDBObject(entity, mf, dbObject);
				} else if (mf.hasAnnotation(Embedded.class)
						&& !mf.isTypeMongoCompatible()) {
					mapEmbeddedToDBObject(entity, mf, dbObject, involvedObjects);
				} else if (mf.hasAnnotation(Property.class)
						|| mf.hasAnnotation(Serialized.class)
						|| mf.isTypeMongoCompatible()) {
					mapValuesToDBObject(entity, mf, dbObject);
				} else {
					logger.warning("Ignoring field: " + mf.getFullName()
							+ " [type:" + mf.getType().getSimpleName() + "]");
				}
			} catch (Exception e) {
				throw new MappingException("Error mapping field:"
						+ mf.getFullName(), e);
			}
		}
		if (involvedObjects != null) {
			involvedObjects.put(entity, dbObject);
		}
		mc.callLifecycleMethods(PreSave.class, entity, dbObject, this);
		return dbObject;
	}

	void mapReferencesToDBObject(final Object entity, final MappedField mf,
			final BasicDBObject dbObject) {
		try {
			String name = mf.getName();

			Object fieldValue = mf.getFieldValue(entity);

			if (mf.isMap()) {
				Map<Object, Object> map = (Map<Object, Object>) fieldValue;
				if ((map != null) && (map.size() > 0)) {
					Map values = (Map) tryConstructor(HashMap.class, mf
							.getCTor());

					for (Map.Entry<Object, Object> entry : map.entrySet()) {
						String strKey = objectToValue(entry.getKey())
						.toString();
						values.put(strKey, new DBRef(null,
								getCollectionName(entry.getValue()),
								asObjectIdMaybe(getId(entry.getValue()))));
					}
					if (values.size() > 0) {
						dbObject.put(name, values);
					}
				}
			} else if (mf.isMultipleValues()) {
				if (fieldValue != null) {
					List values = new ArrayList();

					if (fieldValue instanceof ProxiedEntityReferenceList) {
						ProxiedEntityReferenceList p = (ProxiedEntityReferenceList) fieldValue;
						List<Key<?>> getKeysAsList = p.__getKeysAsList();
						Class c = p.__getReferenceObjClass();
						String collectionName = getCollectionName(c);
						for (Key<?> key : getKeysAsList) {
							values.add(new DBRef(null, collectionName,
									asObjectIdMaybe(key.getId())));
						}
					} else {

						if (mf.getType().isArray()) {
							for (Object o : (Object[]) fieldValue) {
								values.add(new DBRef(null,
										getCollectionName(o),
										asObjectIdMaybe(getId(o))));
							}
						} else {
							for (Object o : (Iterable) fieldValue) {
								values.add(new DBRef(null,
										getCollectionName(o),
										asObjectIdMaybe(getId(o))));
							}
						}
					}
					if (values.size() > 0) {
						dbObject.put(name, values);
					}
				}
			} else {
				if (fieldValue != null) {
					dbObject.put(name, new DBRef(null,
							getCollectionName(fieldValue),
							asObjectIdMaybe(getId(fieldValue))));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void mapEmbeddedToDBObject(final Object entity, final MappedField mf,
			final BasicDBObject dbObject,
			final LinkedHashMap<Object, DBObject> involvedObjects) {
		String name = mf.getName();

		Object fieldValue = null;
		try {
			fieldValue = mf.getFieldValue(entity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (mf.isMap()) {
			Map<String, Object> map = (Map<String, Object>) fieldValue;
			if (map != null) {
				BasicDBObject values = new BasicDBObject();
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					Object entryVal = entry.getValue();
					DBObject convertedVal = toDBObject(entryVal,
							involvedObjects);

					if (mf.getSubType().equals(entryVal.getClass())) {
						convertedVal.removeField(Mapper.CLASS_NAME_FIELDNAME);
					}

					String strKey = objectToValue(entry.getKey()).toString();
					values.put(strKey, convertedVal);
				}
				if (values.size() > 0) {
					dbObject.put(name, values);
				}
			}

		} else if (mf.isMultipleValues()) {
			Iterable coll = (Iterable) fieldValue;
			if (coll != null) {
				List values = new ArrayList();
				for (Object o : coll) {
					DBObject dbObj = toDBObject(o, involvedObjects);
					if (mf.getSubType().equals(o.getClass())) {
						dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
					}
					values.add(dbObj);
				}
				if (values.size() > 0) {
					dbObject.put(name, values);
				}
			}
		} else {
			DBObject dbObj = fieldValue == null ? null : toDBObject(fieldValue,
					involvedObjects);
			if (dbObj != null) {

				if (mf.getType().equals(fieldValue.getClass())) {
					dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
				}

				if (dbObj.keySet().size() > 0) {
					dbObject.put(name, dbObj);
				}
			}
		}
	}

	/** serializes object to byte[] */
	public byte[] serialize(final Object o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		return baos.toByteArray();
	}

	/** deserializes DBBinary/byte[] to object */
	public Object deserialize(final Object data) throws IOException,
	ClassNotFoundException {
		ByteArrayInputStream bais;
		if (data instanceof DBBinary) {
			bais = new ByteArrayInputStream(((DBBinary) data).getData());
		} else {
			bais = new ByteArrayInputStream((byte[]) data);
		}

		ObjectInputStream ois = new ObjectInputStream(bais);

		return ois.readObject();
	}

	void mapValuesToDBObject(final Object entity, final MappedField mf,
			final BasicDBObject dbObject) {
		try {
			String name = mf.getName();
			Class fieldType = mf.getType();
			Object fieldValue = mf.getFieldValue(entity);

			if (mf.hasAnnotation(Serialized.class)) {
				dbObject.put(name, serialize(fieldValue));
			}

			// sets and list are stored in mongodb as ArrayLists
			else if (mf.isMap()) {
				Map<Object, Object> map = (Map<Object, Object>) mf
				.getFieldValue(entity);
				if ((map != null) && (map.size() > 0)) {
					Map mapForDb = new HashMap();
					for (Map.Entry<Object, Object> entry : map.entrySet()) {
						String strKey = objectToValue(entry.getKey())
						.toString();
						mapForDb.put(strKey, objectToValue(entry.getValue()));
					}
					dbObject.put(name, mapForDb);
				}
			} else if (mf.isMultipleValues()) {
				if (fieldValue != null) {
					Iterable iterableValues = null;

					if (fieldType.isArray()) {
						Object[] objects = null;
						try {
							objects = (Object[]) fieldValue;
						} catch (ClassCastException e) {
							// store the primitive array without making
							// it into a list.
							if (Array.getLength(fieldValue) == 0) {
								return;
							}
							dbObject.put(name, fieldValue);
							return;
						}
						// convert array into arraylist
						iterableValues = new ArrayList(objects.length);
						for (Object obj : objects) {
							((ArrayList) iterableValues).add(obj);
						}
					} else {
						// cast value to a common interface
						iterableValues = (Iterable) fieldValue;
					}

					List values = new ArrayList();

					if (mf.getSubType() != null) {
						for (Object o : iterableValues) {
							values.add(objectToValue(mf.getSubType(), o));
						}
					} else {
						for (Object o : iterableValues) {
							values.add(objectToValue(o));
						}
					}
					if (values.size() > 0) {
						dbObject.put(name, values);
					}
				}
			} else {
				Object val = objectToValue(fieldValue);
				if (val != null) {
					dbObject.put(name, val);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	Object mapDBObjectToEntity(BasicDBObject dbObject, final Object entity) {
		// check the history key (a key is the namespace + id)
		String cacheKey = (!dbObject.containsField(ID_KEY)) ? null : "["
			+ dbObject.getString(ID_KEY) + "]";
		if (entityCache.get() == null) {
			entityCache.set(new HashMap<String, Object>());
		}
		if (cacheKey != null) {
			if (entityCache.get().containsKey(cacheKey)) {
				return entityCache.get().get(cacheKey);
			} else {
				entityCache.get().put(cacheKey, entity);
			}
		}

		MappedClass mc = getMappedClass(entity);

		dbObject = (BasicDBObject) mc.callLifecycleMethods(PreLoad.class,
				entity, dbObject, this);
		try {
			for (MappedField mf : mc.getPersistenceFields()) {
				if (mf.hasAnnotation(Id.class)) {
					if (dbObject.get(ID_KEY) != null) {
						mf.setFieldValue(entity, objectFromValue(mf.getType(),
								dbObject, ID_KEY));
					}

				} else if (mf.hasAnnotation(Reference.class)) {
					mapReferencesFromDBObject(dbObject, mf, entity);

				} else if (mf.hasAnnotation(Embedded.class)
						&& !mf.isTypeMongoCompatible()) {
					mapEmbeddedFromDBObject(dbObject, mf, entity);

				} else if (mf.hasAnnotation(Property.class)
						|| mf.hasAnnotation(Serialized.class)
						|| mf.isTypeMongoCompatible()) {
					mapValuesFromDBObject(dbObject, mf, entity);
				} else {
					logger.warning("Ignoring field: " + mf.getFullName()
							+ " [type:" + mf.getType().getName() + "]");
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		mc.callLifecycleMethods(PostLoad.class, entity, dbObject, this);
		return entity;
	}

	void mapValuesFromDBObject(final BasicDBObject dbObject,
			final MappedField mf, final Object entity) {
		String name = mf.getName();
		try {
			Class fieldType = mf.getType();

			if (mf.hasAnnotation(Serialized.class)) {
				Object data = dbObject.get(name);
				if (!((data instanceof DBBinary) || (data instanceof byte[]))) {
					throw new MappingException(
							"The stored data is not a DBBinary or byte[] instance for "
							+ mf.getFullName() + " ; it is a "
							+ data.getClass().getName());
				}

				try {
					mf.setFieldValue(entity, deserialize(data));
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				} catch (ClassNotFoundException ex) {
					throw new IllegalStateException("Unable to deserialize "
							+ data + " on field " + mf.getFullName(), ex);
				}
			} else if (mf.isMap()) {
				if (dbObject.containsField(name)) {
					Map<Object, Object> map = (Map<Object, Object>) dbObject
					.get(name);
					Map values = (Map) tryConstructor(HashMap.class, mf
							.getCTor());
					for (Map.Entry<Object, Object> entry : map.entrySet()) {
						Object objKey = objectFromValue(mf.getMapKeyType(),
								entry.getKey());
						values.put(objKey, objectFromValue(mf.getSubType(),
								entry.getValue()));
					}
					mf.setFieldValue(entity, values);
				}
			} else if (mf.isMultipleValues()) {
				if (dbObject.containsField(name)) {
					Class subtype = mf.getSubType();

					// for byte[] don't treat it as a multiple values.
					if ((subtype == byte.class) && fieldType.isArray()) {
						mf.setFieldValue(entity, dbObject.get(name));
						return;
					}
					// List and Sets are stored as List in mongodb
					List list = (List) dbObject.get(name);

					if (subtype != null) {
						// map back to the java datatype
						// (List/Set/Array[])
						Collection values;

						if (!mf.isSet()) {
							values = (List) tryConstructor(ArrayList.class, mf
									.getCTor());
						} else {
							values = (Set) tryConstructor(HashSet.class, mf
									.getCTor());
						}

						if (subtype == Locale.class) {
							for (Object o : list) {
								values.add(parseLocale((String) o));
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
						if (fieldType.isArray()) {
							Object[] array = convertToArray(subtype, values);
							mf.setFieldValue(entity, array);
						} else {
							mf.setFieldValue(entity, values);
						}
					} else {
						mf.setFieldValue(entity, list);
					}
				}
			} else {
				if (dbObject.containsField(name)) {
					mf.setFieldValue(entity, objectFromValue(fieldType,
							dbObject, name));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Object[] convertToArray(final Class type, final Collection values) {
		Object exampleArray = Array.newInstance(type, 1);
		Object[] array = ((ArrayList) values).toArray((Object[]) exampleArray);
		return array;
	}

	void mapEmbeddedFromDBObject(final BasicDBObject dbObject,
			final MappedField mf, final Object entity) {
		String name = mf.getName();

		Class fieldType = mf.getType();
		try {

			if (mf.isMap()) {
				Map map = (Map) tryConstructor(HashMap.class, mf.getCTor());

				if (dbObject.containsField(name)) {
					BasicDBObject dbVal = (BasicDBObject) dbObject.get(name);
					for (Map.Entry entry : dbVal.entrySet()) {
						Object newEntity = createInstance(mf.getSubType(),
								(BasicDBObject) entry.getValue());

						newEntity = mapDBObjectToEntity((BasicDBObject) entry
								.getValue(), newEntity);
						// TODO Add Lifecycle call for newEntity
						Object objKey = objectFromValue(mf.getMapKeyType(),
								entry.getKey());
						map.put(objKey, newEntity);
					}
				}

				if (map.size() > 0) {
					mf.setFieldValue(entity, map);
				}
			} else if (mf.isMultipleValues()) {
				// multiple documents in a List
				Class newEntityType = mf.getSubType();
				Collection values = (Collection) tryConstructor(
						(!mf.isSet()) ? ArrayList.class : HashSet.class, mf
								.getCTor());

				if (dbObject.containsField(name)) {
					Object dbVal = dbObject.get(name);

					List<BasicDBObject> dbVals = (dbVal instanceof List) ? (List<BasicDBObject>) dbVal
							: Collections.singletonList((BasicDBObject) dbVal);

					for (BasicDBObject dbObj : dbVals) {
						Object newEntity = createInstance(newEntityType, dbObj);
						newEntity = mapDBObjectToEntity(dbObj, newEntity);
						values.add(newEntity);
					}
				}
				if (values.size() > 0) {
					if (mf.getType().isArray()) {
						Object[] array = convertToArray(mf.getSubType(), values);
						mf.setFieldValue(entity, array);
					} else {
						mf.setFieldValue(entity, values);
					}
				}
			} else {
				// single document
				if (dbObject.containsField(name)) {
					BasicDBObject dbVal = (BasicDBObject) dbObject.get(name);
					Object refObj = createInstance(fieldType, dbVal);
					refObj = mapDBObjectToEntity(dbVal, refObj);
					if (refObj != null) {
						mf.setFieldValue(entity, refObj);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void mapReferencesFromDBObject(final BasicDBObject dbObject,
			final MappedField mf, final Object entity) {
		String name = mf.getName();

		Class fieldType = mf.getType();

		try {

			Reference refAnn = mf.getAnnotation(Reference.class);
			if (mf.isMap()) {
				createMapFromDBObject(dbObject, mf, entity, name, refAnn);
			} else if (mf.isMultipleValues()) {
				// multiple references in a List
				Class referenceObjClass = mf.getSubType();
				Collection references = (Collection) tryConstructor((!mf
						.isSet()) ? ArrayList.class : HashSet.class, mf
								.getCTor());

				if (refAnn.lazy()) {
					if (dbObject.containsField(name)) {
						references = proxyFactory.createListProxy(references,
								referenceObjClass, refAnn.ignoreMissing(),
								datastoreProvider);
						ProxiedEntityReferenceList referencesAsProxy = (ProxiedEntityReferenceList) references;

						// TODO test for existence could be done in one go
						// instead of one-by-one lookups.

						Object dbVal = dbObject.get(name);
						if (dbVal instanceof List) {
							List refList = (List) dbVal;
							for (Object dbRefObj : refList) {
								DBRef dbRef = (DBRef) dbRefObj;
								addToReferenceList(mf, refAnn,
										referencesAsProxy, dbRef);
							}
						} else {
							DBRef dbRef = (DBRef) dbObject.get(name);
							addToReferenceList(mf, refAnn, referencesAsProxy,
									dbRef);
						}
					}
				} else {

					if (dbObject.containsField(name)) {
						Object dbVal = dbObject.get(name);
						if (dbVal instanceof List) {
							List refList = (List) dbVal;
							for (Object dbRefObj : refList) {
								DBRef dbRef = (DBRef) dbRefObj;
								BasicDBObject refDbObject = (BasicDBObject) dbRef
								.fetch();

								if (refDbObject == null) {
									if (!refAnn.ignoreMissing()) {
										throw new MappingException(
												"The reference("
												+ dbRef.toString()
												+ ") could not be fetched for "
												+ mf.getFullName());
									}
								} else {
									Object refObj = createInstance(
											referenceObjClass, refDbObject);
									refObj = mapDBObjectToEntity(refDbObject,
											refObj);
									references.add(refObj);
								}
							}
						} else {
							DBRef dbRef = (DBRef) dbObject.get(name);
							BasicDBObject refDbObject = (BasicDBObject) dbRef
							.fetch();
							if (refDbObject == null) {
								if (!refAnn.ignoreMissing()) {
									throw new MappingException("The reference("
											+ dbRef.toString()
											+ ") could not be fetched for "
											+ mf.getFullName());
								}
							} else {
								Object newEntity = createInstance(
										referenceObjClass, refDbObject);
								newEntity = mapDBObjectToEntity(refDbObject,
										newEntity);
								// TODO Add Lifecycle call for newEntity
								references.add(newEntity);
							}
						}
					}
				}

				// FIXME us, array detection before

				if (mf.getType().isArray()) {
					Object[] array = convertToArray(mf.getSubType(), references);
					mf.setFieldValue(entity, array);
				} else {
					mf.setFieldValue(entity, references);
				}
			} else {
				// single reference
				Class referenceObjClass = fieldType;
				if (dbObject.containsField(name)) {
					DBRef dbRef = (DBRef) dbObject.get(name);

					Object resolvedObject = null;
					if (refAnn.lazy()) {
						if (exists(dbRef)) {
							resolvedObject = createOrReuseProxy(
									referenceObjClass, dbRef);
						} else {
							if (!refAnn.ignoreMissing()) {
								throw new MappingException("The reference("
										+ dbRef.toString()
										+ ") could not be fetched for "
										+ mf.getFullName());
							}
						}
					} else {
						resolvedObject = resolveObject(dbRef,
								referenceObjClass, refAnn.ignoreMissing(), mf);
					}

					mf.setFieldValue(entity, resolvedObject);

				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Locale parseLocale(final String localeString) {
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

	/** Converts known types from mongodb -> java. */
	public static Object objectFromValue(final Class javaType,
			final BasicDBObject dbObject, final String name) {
		return objectFromValue(javaType, dbObject.get(name));
	}

	private static boolean compatibleTypes(final Class type1, final Class type2) {
		if (type1.equals(type2)) {
			return true;
		}
		return (type1.isAssignableFrom(type2) || ((type1.isPrimitive() || type2
				.isPrimitive()) && type2.getSimpleName().toLowerCase().equals(
						type1.getSimpleName().toLowerCase())));// &&
		// valType.getName().startsWith("java.lang") &&
		// javaType.getName().startsWith("java.lang") ));

	}

	/** Converts known types from mongodb -> java. */
	protected static Object objectFromValue(final Class javaType,
			final Object val) {

		if (val == null) {
			return null;
		}
		if (javaType == null) {
			return val;
		}

		Class valType = val.getClass();

		if (compatibleTypes(javaType, valType)) {
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
			return parseLocale(val.toString());
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
	public Object objectToValue(Class javaType, final Object obj) {
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
			return ((Key) obj).toRef(this);
		} else {
			return obj;
		}

	}

	/**
	 * Converts known types from java -> mongodb. Really it just converts enums
	 * and locales to strings
	 */
	public Object objectToValue(final Object obj) {
		if (obj == null) {
			return null;
		}
		return objectToValue(obj.getClass(), obj);
	}

	private Object resolveObject(final DBRef dbRef,
			final Class referenceObjClass, final boolean ignoreMissing,
			final MappedField mf) {
		BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

		if (refDbObject != null) {
			Object refObj = createInstance(referenceObjClass, refDbObject);
			refObj = mapDBObjectToEntity(refDbObject, refObj);
			return refObj;
		}

		if (!ignoreMissing) {
			throw new MappingException("The reference(" + dbRef.toString()
					+ ") could not be fetched for " + mf.getFullName());
		} else {
			return null;
		}

	}

	private boolean exists(final DBRef dbRef) {
		// TODO that might be cheaper to implement?
		return dbRef.fetch() != null;
	}

	private Object createOrReuseProxy(final Class referenceObjClass,
			final DBRef dbRef) {
		Map<String, Object> cache = proxyCache.get();
		String cacheKey = createCacheKey(dbRef);
		Object proxyAlreadyCreated = cache.get(cacheKey);
		if (proxyAlreadyCreated != null) {
			return proxyAlreadyCreated;
		}

		Object newProxy = proxyFactory.createProxy(referenceObjClass, new Key(
				dbRef), datastoreProvider);
		cache.put(cacheKey, newProxy);
		return newProxy;
	}

	private String createCacheKey(DBRef dbRef) {
		return dbRef.getId().toString();
	}

	private void addToReferenceList(final MappedField mf,
			final Reference refAnn,
			final ProxiedEntityReferenceList referencesAsProxy,
			final DBRef dbRef) {
		if (!exists(dbRef)) {
			if (!refAnn.ignoreMissing()) {
				throw new MappingException("The reference(" + dbRef.toString()
						+ ") could not be fetched for " + mf.getFullName());
			}
		} else {
			referencesAsProxy.__add(new Key(dbRef));
		}
	}

	private void createMapFromDBObject(final BasicDBObject dbObject,
			final MappedField mf, final Object entity, final String name,
			final Reference refAnn) throws IllegalAccessException {
		Class referenceObjClass = mf.getSubType();
		Map map = (Map) tryConstructor(HashMap.class, mf.getCTor());

		if (dbObject.containsField(name)) {
			if (refAnn.lazy()) {
				// replace map by proxy to it.
				map = proxyFactory.createMapProxy(map, referenceObjClass,
						refAnn.ignoreMissing(), datastoreProvider);
			}

			BasicDBObject dbVal = (BasicDBObject) dbObject.get(name);
			for (Map.Entry entry : dbVal.entrySet()) {
				DBRef dbRef = (DBRef) entry.getValue();

				if (refAnn.lazy()) {
					ProxiedEntityMap proxiedMap = (ProxiedEntityMap) map;
					proxiedMap.__put(entry.getKey(), new Key(dbRef));
				} else {
					Object resolvedObject = resolveObject(dbRef,
							referenceObjClass, refAnn.ignoreMissing(), mf);
					map.put(entry.getKey(), resolvedObject);
				}
			}
		}
		mf.setFieldValue(entity, map);
	}

	// could be made configurable
	private final LazyProxyFactory proxyFactory = new CGLibLazyProxyFactory();
	// could be made configurable
	private DatastoreProvider datastoreProvider = new DefaultDatastoreProvider();

}
