/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.converters.DefaultConverters;
import com.google.code.morphia.mapping.cache.EntityCacheKey;
import com.google.code.morphia.mapping.cache.ProxyCacheKey;
import com.google.code.morphia.mapping.cache.first.FirstLevelEntityCache;
import com.google.code.morphia.mapping.cache.first.FirstLevelProxyCache;
import com.google.code.morphia.mapping.lazy.LazyFeatureDependencies;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBRef;

@SuppressWarnings("unchecked")
class ReferenceMapper {
	
	private final Mapper mapper;
	private final DefaultConverters converters;
	
	public ReferenceMapper(Mapper mapper, DefaultConverters converters) {
		this.mapper = mapper;
		this.converters = converters;
	}
	
	void toDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject, MapperOptions opts) {
		
		String name = mf.getName();
		
		Object fieldValue = mf.getFieldValue(entity);
		
		if (mf.isMap()) {
			writeMap(mf, dbObject, name, fieldValue, opts);
		} else if (mf.isMultipleValues()) {
			writeCollection(mf, dbObject, name, fieldValue, opts);
		} else {
			writeSingle(dbObject, name, fieldValue);
		}
		
	}
	
	private void writeSingle(final BasicDBObject dbObject, String name, Object fieldValue) {
		if (fieldValue != null) {
			DBRef dbrefFromKey = getKey(fieldValue).toRef(mapper);
			dbObject.put(name, dbrefFromKey);

		}
	}
	
	private void writeCollection(final MappedField mf, final BasicDBObject dbObject, String name, Object fieldValue, MapperOptions opts) {
		if (fieldValue != null) {
			List values = new ArrayList();
			
			if (ProxyHelper.isProxy(fieldValue) && ProxyHelper.isUnFetched(fieldValue)) {
				ProxiedEntityReferenceList p = (ProxiedEntityReferenceList) fieldValue;
				List<Key<?>> getKeysAsList = p.__getKeysAsList();
				for (Key<?> key : getKeysAsList) {
					values.add(key.toRef(mapper));
				}
			} else {
				
				if (mf.getType().isArray()) {
					for (Object o : (Object[]) fieldValue) {
						values.add(getKey(o).toRef(mapper));
					}
				} else {
					for (Object o : (Iterable) fieldValue) {
						values.add(getKey(o).toRef(mapper));
					}
				}
			}
			if (values.size() > 0 || opts.storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}
	
	private void writeMap(final MappedField mf, final BasicDBObject dbObject, String name, Object fieldValue, MapperOptions opts) {
		Map<Object, Object> map = (Map<Object, Object>) fieldValue;
		if ((map != null)) {
			Map values = (Map) ReflectionUtils.newInstance(mf.getCTor(), HashMap.class);
			
			if (ProxyHelper.isProxy(map) && ProxyHelper.isUnFetched(map)) {
				ProxiedEntityReferenceMap proxy = (ProxiedEntityReferenceMap) map;
				
				Map<String, Key<?>> refMap = proxy.__getReferenceMap();
				for (Map.Entry<String, Key<?>> entry : refMap.entrySet()) {
					String strKey = entry.getKey();
					values.put(strKey, entry.getValue().toRef(mapper));
				}
			} else {
				for (Map.Entry<Object, Object> entry : map.entrySet()) {
					String strKey = converters.encode(entry.getKey()).toString();
					values.put(strKey, getKey(entry.getValue()).toRef(mapper));
				}
			}
			if (values.size() > 0 || opts.storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}
	
	private Key<?> getKey(final Object entity) {
		try {
			if (entity instanceof ProxiedEntityReference) {
				ProxiedEntityReference proxy = (ProxiedEntityReference) entity;
				return proxy.__getKey();
			}
			MappedClass mappedClass = mapper.getMappedClass(entity);
			Object id = mappedClass.getIdField().get(entity);
			Key key = new Key(mappedClass.getCollectionName(), ReflectionUtils.asObjectIdMaybe(id));
			return key;
		} catch (IllegalAccessException iae) {
			throw new RuntimeException(iae);
		}
	}
	
	void fromDBObject(final BasicDBObject dbObject, final MappedField mf, final Object entity) {
		String name = mf.getName();
		
		Class fieldType = mf.getType();
		
		Reference refAnn = mf.getAnnotation(Reference.class);
		if (mf.isMap()) {
			readMap(dbObject, mf, entity, name, refAnn);
		} else if (mf.isMultipleValues()) {
			readCollection(dbObject, mf, entity, name, refAnn);
		} else {
			readSingle(dbObject, mf, entity, name, fieldType, refAnn);
		}
		
	}
	
	private void readSingle(final BasicDBObject dbObject, final MappedField mf,
			final Object entity, String name, Class fieldType, Reference refAnn) {
		Class referenceObjClass = fieldType;
		if (dbObject.containsField(name)) {
			DBRef dbRef = (DBRef) dbObject.get(name);
			
			Object resolvedObject = null;
			if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
				if (exists(referenceObjClass, dbRef)) {
					resolvedObject = createOrReuseProxy(referenceObjClass, dbRef);
				} else {
					if (!refAnn.ignoreMissing()) {
						throw new MappingException("The reference(" + dbRef.toString() + ") could not be fetched for "
								+ mf.getFullName());
					}
				}
			} else {
				resolvedObject = resolveObject(dbRef, referenceObjClass, refAnn.ignoreMissing(), mf);
			}
			
			mf.setFieldValue(entity, resolvedObject);
			
		}
	}
	
	private void readCollection(final BasicDBObject dbObject, final MappedField mf,
			final Object entity, String name, Reference refAnn) {
		// multiple references in a List
		Class referenceObjClass = mf.getSubType();
		Collection references = (Collection) ReflectionUtils.newInstance(mf.getCTor(), (!mf.isSet()) ? ArrayList.class
                		: HashSet.class);
		
		if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
			if (dbObject.containsField(name)) {
				references = mapper.proxyFactory.createListProxy(references, referenceObjClass, refAnn.ignoreMissing(),
						mapper.datastoreProvider);
				ProxiedEntityReferenceList referencesAsProxy = (ProxiedEntityReferenceList) references;
				
				// FIXME us test for existence could be done in one go
				// instead of one-by-one lookups.
				
				Object dbVal = dbObject.get(name);
				if (dbVal instanceof List) {
					List refList = (List) dbVal;
					for (Object dbRefObj : refList) {
						DBRef dbRef = (DBRef) dbRefObj;
						addToReferenceList(mf, refAnn, referencesAsProxy, dbRef);
					}
				} else {
					DBRef dbRef = (DBRef) dbObject.get(name);
					addToReferenceList(mf, refAnn, referencesAsProxy, dbRef);
				}
			}
		} else {
			
			if (dbObject.containsField(name)) {
				Object dbVal = dbObject.get(name);
				if (dbVal instanceof List) {
					List refList = (List) dbVal;
					for (Object dbRefObj : refList) {
						DBRef dbRef = (DBRef) dbRefObj;
						BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
						
						if (refDbObject == null) {
							if (!refAnn.ignoreMissing()) {
								throw new MappingException("The reference(" + dbRef.toString()
										+ ") could not be fetched for " + mf.getFullName());
							}
						} else {
							Object refObj = ReflectionUtils.createInstance(referenceObjClass, refDbObject);
							refObj = mapper.fromDb(refDbObject, refObj);
							references.add(refObj);
						}
					}
				} else {
					DBRef dbRef = (DBRef) dbObject.get(name);
					BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
					if (refDbObject == null) {
						if (!refAnn.ignoreMissing()) {
							throw new MappingException("The reference(" + dbRef.toString()
									+ ") could not be fetched for " + mf.getFullName());
						}
					} else {
						Object newEntity = ReflectionUtils.createInstance(referenceObjClass, refDbObject);
						newEntity = mapper.fromDb(refDbObject, newEntity);
						references.add(newEntity);
					}
				}
			}
		}
		
		if (mf.getType().isArray()) {
			Object[] array = ReflectionUtils.convertToArray(mf.getSubType(), references);
			mf.setFieldValue(entity, array);
		} else {
			mf.setFieldValue(entity, references);
		}
	}
	
	boolean exists(Class c, final DBRef dbRef) {
		Datastore ds = mapper.datastoreProvider.get();
		return ds.createQuery(c).filter(Mapper.ID_KEY, dbRef.getId()).countAll() == 1;
	}
	
	Object resolveObject(final DBRef dbRef, final Class referenceObjClass, final boolean ignoreMissing,
			final MappedField mf) {
		
		FirstLevelEntityCache entityCache = mapper.getFirstLevelCacheProvider().getEntityCache();
		EntityCacheKey ck = new EntityCacheKey(referenceObjClass, dbRef.getId().toString());
		Object cached = entityCache.get(ck);
		if (cached != null)
			return cached;
		
		BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
		
		if (refDbObject != null) {
			Object refObj = ReflectionUtils.createInstance(referenceObjClass, refDbObject);
			refObj = mapper.fromDb(refDbObject, refObj);
			entityCache.put(ck, refObj);
			return refObj;
		}
		
		if (!ignoreMissing) {
			throw new MappingException("The reference(" + dbRef.toString() + ") could not be fetched for "
					+ mf.getFullName());
		} else {
			return null;
		}
	}
	
	private void addToReferenceList(final MappedField mf, final Reference refAnn, final ProxiedEntityReferenceList referencesAsProxy, final DBRef dbRef) {
		if (!exists(mf.getSubType(), dbRef)) {
			if (!refAnn.ignoreMissing()) {
				throw new MappingException("The reference(" + dbRef.toString() + ") could not be fetched for "
						+ mf.getFullName());
			}
		} else {
			referencesAsProxy.__add(new Key(dbRef));
		}
	}
	
	private void readMap(final BasicDBObject dbObject, final MappedField mf,
			final Object entity,
			final String name, final Reference refAnn) {
		Class referenceObjClass = mf.getSubType();
		Map map = (Map) ReflectionUtils.newInstance(mf.getCTor(), HashMap.class);
		
		if (dbObject.containsField(name)) {
			if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
				// replace map by proxy to it.
				map = mapper.proxyFactory.createMapProxy(map, referenceObjClass, refAnn.ignoreMissing(),
						mapper.datastoreProvider);
			}
			
			BasicDBObject dbVal = (BasicDBObject) dbObject.get(name);
			for (Map.Entry<String, ?> entry : dbVal.entrySet()) {
				DBRef dbRef = (DBRef) entry.getValue();
				
				if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
					ProxiedEntityReferenceMap proxiedMap = (ProxiedEntityReferenceMap) map;
					proxiedMap.__put(entry.getKey(), new Key(dbRef));
				} else {
					Object resolvedObject = resolveObject(dbRef, referenceObjClass, refAnn.ignoreMissing(), mf);
					map.put(entry.getKey(), resolvedObject);
				}
			}
		}
		mf.setFieldValue(entity, map);
	}
	
	private Object createOrReuseProxy(final Class referenceObjClass, final DBRef dbRef) {
		
		FirstLevelProxyCache proxyCache = mapper.getFirstLevelCacheProvider().getProxyCache();
		ProxyCacheKey cacheKey = new ProxyCacheKey(dbRef);
		Object proxyAlreadyCreated = proxyCache.get(cacheKey);
		if (proxyAlreadyCreated != null) {
			return proxyAlreadyCreated;
		}
		
		Object newProxy = mapper.proxyFactory.createProxy(referenceObjClass, new Key(dbRef), mapper.datastoreProvider);
		proxyCache.put(cacheKey, newProxy);
		return newProxy;
	}
	

}
