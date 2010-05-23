/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.mapping.mapper.conv.SimpleValueConverter;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


class EmbeddedMapper {
	private final Mapper mapper;
	
	public EmbeddedMapper(Mapper mapper) {
		this.mapper = mapper;
	}
	
	void mapEmbeddedToDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject,
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
					DBObject convertedVal = mapper.toDBObject(entryVal, involvedObjects);
					
					if (mf.getSubType().equals(entryVal.getClass())) {
						convertedVal.removeField(Mapper.CLASS_NAME_FIELDNAME);
					}
					
					String strKey = SimpleValueConverter.objectToValue(entry.getKey()).toString();
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
					DBObject dbObj = mapper.toDBObject(o, involvedObjects);
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
			DBObject dbObj = fieldValue == null ? null : mapper.toDBObject(fieldValue, involvedObjects);
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
	
	void mapEmbeddedFromDBObject(final BasicDBObject dbObject, final MappedField mf, final Object entity) {
		String name = mf.getName();
		
		Class fieldType = mf.getType();
		try {
			
			if (mf.isMap()) {
				Map map = (Map) ReflectionUtils.tryConstructor(HashMap.class, mf.getCTor());
				
				if (dbObject.containsField(name)) {
					BasicDBObject dbVal = (BasicDBObject) dbObject.get(name);
					for (Map.Entry entry : dbVal.entrySet()) {
						Object newEntity = ReflectionUtils.createInstance(mf.getSubType(), (BasicDBObject) entry.getValue());
						
						newEntity = mapper.mapDBObjectToEntity((BasicDBObject) entry.getValue(), newEntity);
						// TODO Add Lifecycle call for newEntity
						Object objKey = SimpleValueConverter.objectFromValue(mf.getMapKeyType(), entry.getKey());
						map.put(objKey, newEntity);
					}
				}
				
				if (map.size() > 0) {
					mf.setFieldValue(entity, map);
				}
			} else if (mf.isMultipleValues()) {
				// multiple documents in a List
				Class newEntityType = mf.getSubType();
				Collection values = (Collection) ReflectionUtils.tryConstructor((!mf.isSet()) ? ArrayList.class
						: HashSet.class, mf.getCTor());
				
				if (dbObject.containsField(name)) {
					Object dbVal = dbObject.get(name);
					
					List<BasicDBObject> dbVals = (dbVal instanceof List) ? (List<BasicDBObject>) dbVal : Collections
							.singletonList((BasicDBObject) dbVal);
					
					for (BasicDBObject dbObj : dbVals) {
						Object newEntity = ReflectionUtils.createInstance(newEntityType, dbObj);
						newEntity = mapper.mapDBObjectToEntity(dbObj, newEntity);
						values.add(newEntity);
					}
				}
				if (values.size() > 0) {
					if (mf.getType().isArray()) {
						Object[] array = SimpleValueConverter.convertToArray(mf.getSubType(), values);
						mf.setFieldValue(entity, array);
					} else {
						mf.setFieldValue(entity, values);
					}
				}
			} else {
				// single document
				if (dbObject.containsField(name)) {
					BasicDBObject dbVal = (BasicDBObject) dbObject.get(name);
					Object refObj = ReflectionUtils.createInstance(fieldType, dbVal);
					refObj = mapper.mapDBObjectToEntity(dbVal, refObj);
					if (refObj != null) {
						mf.setFieldValue(entity, refObj);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
