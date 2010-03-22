/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.code.morphia.MappedClass.MappedField;
import com.google.code.morphia.annotations.CollectionName;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.ObjectId;

/**
 *
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class Mapper {
    private static final Logger logger = Logger.getLogger(Mapper.class.getName());

	private static final String CLASS_NAME_KEY = "className";
	
	public static final String ID_KEY = "_id";
	public static final String COLLECTION_NAME_KEY = "_ns";	
	public static final String IGNORED_FIELDNAME = ".";

    /** Set of classes that have been validated for mapping by this mapper */
    private final ConcurrentHashMap<String,MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
    
    private final ThreadLocal<Map<String, Object>> history = new ThreadLocal<Map<String, Object>>();


    Mapper() {
    }

    boolean isMapped(Class c) {
        return mappedClasses.containsKey(c.getName());
    }

    void addMappedClass(Class c) {
    	MappedClass mc = new MappedClass(c);
    	mc.validate();
        mappedClasses.put(c.getName(), mc);
    }

    void addMappedClass(MappedClass mc) {
    	mc.validate();
        mappedClasses.put(mc.clazz.getName(), mc);
    }

    Map<String, MappedClass> getMappedClasses() {
        return mappedClasses;
    }

    public MappedClass getMappedClass(Object obj) {
		if (obj == null) return null;
		return mappedClasses.get(obj.getClass().getName());
	}

    void clearHistory() {
        history.remove();
    }

    public String getCollectionName(Object object) throws IllegalAccessException {
    	if (object instanceof Class) return getCollectionName((Class) object);
    	
    	MappedClass mc = getMappedClass(object);
    	if (mc == null) mc = new MappedClass(object.getClass());
    	
    	return (mc.collectionNameField != null && mc.collectionNameField.get(object) != null) ? (String)mc.collectionNameField.get(object) : mc.defCollName;
    }
    
	public String getCollectionName(Class clazz) throws IllegalAccessException {
	  	MappedClass mc = getMappedClass(clazz);
    	if (mc == null) mc = new MappedClass(clazz);
    	return mc.defCollName;
  }

    private String getID(Object object) throws IllegalAccessException {
    	return (String)getMappedClass(object).idField.get(object);
    }

    Class getClassForName( String className, Class defaultClass ) {
    	if (mappedClasses.containsKey(className)) return mappedClasses.get(className).clazz;
        try {
            Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return c;
        } catch ( ClassNotFoundException ex ) {
            return defaultClass;
        }
    }

    Object createEntityInstanceForDbObject( Class entityClass, BasicDBObject dbObject ) throws Exception {
        // see if there is a className value
        String className = (String) dbObject.get(CLASS_NAME_KEY);
        Class c = entityClass;
        if ( className != null ) {
            c = getClassForName(className, entityClass);
        }

        Constructor constructor = c.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    Object fromDBObject(Class entityClass, BasicDBObject dbObject) throws Exception {
        history.set(new HashMap<String, Object>());
        
        Object entity = createEntityInstanceForDbObject(entityClass, dbObject);
        
        mapDBObjectToEntity(dbObject, entity);

        history.remove();
        return entity;
    }

    DBObject toDBObject( Object entity ) throws Exception {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put(CLASS_NAME_KEY, entity.getClass().getCanonicalName());

        MappedClass mc = getMappedClass(entity);
        if (mc == null) mc = new MappedClass(entity.getClass());
        
        String collName = (mc.collectionNameField == null) ? null :  (String)mc.collectionNameField.get(entity);
        if (collName != null && collName.length() > 0 ) dbObject.put(COLLECTION_NAME_KEY, collName);

        for (MappedField mf : mc.persistenceFields) {
            Field field = mf.field;
            Class fieldType = field.getType();
            
        	field.setAccessible(true);

            if ( mf.hasAnnotation(Id.class) ) {
                Object value = field.get(entity);
                if ( value != null ) {
                    dbObject.put(ID_KEY, fixupId(value));
                }
            } else if ( mf.hasAnnotation(Reference.class) ) {
                mapReferencesToDBObject(entity, mf, dbObject);
            } else  if (mf.hasAnnotation(Embedded.class)){
                mapEmbeddedToDBObject(entity, mf, dbObject);
            } else if ( mf.hasAnnotation(Property.class)
                    || ReflectionUtils.isPropertyType(field.getType())
                    || ReflectionUtils.implementsAnyInterface(fieldType, Map.class, List.class, Set.class) ) {
                mapValuesToDBObject(entity, mf, dbObject);
            } else {
            	logger.warning("Ignoring field: " + field.getName() + " [" + field.getType().getSimpleName() + "]");
            }
            
        }
        
        return dbObject;
    }

    void mapReferencesToDBObject( Object entity, MappedField mf, BasicDBObject dbObject) throws Exception {
        Reference mongoReference = (Reference)mf.getAnnotation(Reference.class);
        String name = mf.name;
        Class fieldType = mf.field.getType();
        Object fieldValue = mf.field.get(entity);
        if (ReflectionUtils.implementsAnyInterface(fieldType, List.class, Set.class)) {
            Collection coll = (Collection) fieldValue;
            if ( coll != null ) {
                List values = new ArrayList();
                for ( Object o : coll ) {
                    values.add(new DBRef(null, getCollectionName(o), fixupId(getID(o))));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }
        } else if (ReflectionUtils.implementsInterface(fieldType, Map.class)) {
            Map<Object,Object> map = (Map<Object,Object>) fieldValue;
            if ( map != null ) {
                Map values = mongoReference.mapClass().newInstance();
                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
                    values.put(entry.getKey(), new DBRef(null, getCollectionName(entry.getValue()), fixupId(getID(entry.getValue()))));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else {
            if ( fieldValue != null ) {
                dbObject.put(name, new DBRef(null, getCollectionName(fieldValue), fixupId(getID(fieldValue))));
            } else {
                dbObject.removeField(name);
            }
        }
    }

    void mapEmbeddedToDBObject( Object entity, MappedField mf, BasicDBObject dbObject ) throws Exception {
        String name = mf.name;
        
        Class fieldType = mf.field.getType();
        Object fieldValue = mf.field.get(entity);

        if (ReflectionUtils.implementsAnyInterface(fieldType, List.class, Set.class)) {
            Collection coll = (Collection)fieldValue;
            if ( coll != null ) {
                List values = new ArrayList();
                for ( Object o : coll ) {
                    values.add(toDBObject(o));
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }

        } else if (ReflectionUtils.implementsInterface(fieldType, Map.class)) {
            Map<String, Object> map = (Map<String, Object>) fieldValue;
            if ( map != null ) {
                BasicDBObject mapObj = new BasicDBObject();
                for ( Map.Entry<String,Object> entry : map.entrySet() ) {
                    mapObj.put(entry.getKey(), toDBObject(entry.getValue()));
                }
                dbObject.put(name, mapObj);
            } else {
                dbObject.removeField(name);
            }

        } else {
            if ( fieldValue != null ) {
                dbObject.put(name, toDBObject(fieldValue));
            } else {
                dbObject.removeField(name);
            }
        }
    }

    void mapValuesToDBObject( Object entity, MappedField mf, BasicDBObject dbObject ) throws Exception {
        String name = mf.name;
        Class fieldType = mf.field.getType();
        Object fieldValue = mf.field.get(entity);

        //sets and list are stored in mongodb as ArrayLists
        if (ReflectionUtils.implementsAnyInterface(fieldType, Set.class, List.class)) {
            Class paramClass = ReflectionUtils.getParameterizedClass(mf.field);
            Collection coll = (Collection) fieldValue;
            if ( coll != null ) {
            	List values = (coll instanceof ArrayList) ? (List)coll : new ArrayList();
                
            	if ( paramClass != null ) {
                    for ( Object o : coll ) {
                    	values.add(objectToValue(paramClass, o));
                    }
                } else {
                    for ( Object o : coll ) {
                    	values.add(objectToValue(o));
                    }
                }
                dbObject.put(name, values);
            } else {
                dbObject.removeField(name);
            }
        } else if (ReflectionUtils.implementsInterface(fieldType, Map.class)) {
            Map<Object,Object> map = (Map<Object,Object>) mf.field.get(entity);
            if ( map != null ) {
                Map mapForDb = new HashMap();
                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
                	mapForDb.put(entry.getKey(), objectToValue(entry.getValue()));
                }
                dbObject.put(name, mapForDb);
            } else {
                dbObject.removeField(name);
            }
        } else {
            if ( fieldValue != null ) {
            	dbObject.put(name, objectToValue(fieldValue));
            } else {
                dbObject.removeField(name);
            }
        }
    }

    Object mapDBObjectToEntity( BasicDBObject dbObject, Object entity ) throws Exception {
        // check the history key (a key is the namespace + id)
        String key = (!dbObject.containsField(ID_KEY)) ? null : dbObject.getString(COLLECTION_NAME_KEY) + "[" + dbObject.getString(ID_KEY) + "]";
        
//        if (!dbObject.containsField(COLLECTIONNAME_KEY) || !dbObject.containsField(ID_KEY))
//        	throw new RuntimeException("DBOject is missing _ns or _id; " + dbObject.toString());
        
        if (history.get() == null) {
            history.set(new HashMap<String, Object>());
        }
        if ( key != null ) {
            if (history.get().containsKey(key)) {
                return history.get().get(key);
            } else {
                history.get().put(key, entity);
            }
        }

        MappedClass mc = getMappedClass(entity);
        if (mc == null) mc = new MappedClass(entity.getClass());
                
        for (MappedField mf : mc.persistenceFields) {
            Field field = mf.field;
//            String name = mf.name;
            field.setAccessible(true);

            if ( mf.hasAnnotation(Id.class) ) {
                if ( dbObject.get(ID_KEY) != null ) {
                    field.set(entity, objectFromValue(field.getType(), dbObject, ID_KEY));
                }
            } else if ( mf.hasAnnotation(CollectionName.class) ) {
                if ( dbObject.get(COLLECTION_NAME_KEY) != null ) {
                    field.set(entity, dbObject.get(COLLECTION_NAME_KEY).toString());
                }

            } else if ( mf.hasAnnotation(Reference.class) ) {
                mapReferencesFromDBObject(dbObject, mf, entity);

            } else if ( mf.hasAnnotation(Embedded.class) ) {
                mapEmbeddedFromDBObject(dbObject, mf, entity);
                
            } else if ( mf.hasAnnotation(Property.class)
                    || ReflectionUtils.isPropertyType(field.getType())
                    || ReflectionUtils.implementsAnyInterface(field.getType(), List.class, Set.class, Map.class)) {
                mapValuesFromDBObject(dbObject, mf, entity);
            } else {
            	logger.warning("Ignoring field: " + field.getName() + " [" + field.getType().getSimpleName() + "]");
            }
        }
        return entity;
    }

    void mapValuesFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) throws Exception {
        Property mongoValue = (Property)mf.getAnnotation(Property.class);
        String name = mf.name;

        Class fieldType = mf.field.getType();
        
        boolean bSet = ReflectionUtils.implementsInterface(fieldType, Set.class);
        boolean bList = ReflectionUtils.implementsInterface(fieldType, List.class);
        
        if (bSet || bList ) {
            if ( dbObject.containsField(name) ) {
                Class paramClass = ReflectionUtils.getParameterizedClass(mf.field);
   
                //List and Sets are stored as List in mongodb
                List list = (List) dbObject.get(name);
                
                
                Collection values;
                //map back to the java datatype (List/Set)
                if (bList)
                	values = mongoValue != null ? mongoValue.listClass().newInstance() : new ArrayList();
                else
                	values = mongoValue != null ? mongoValue.setClass().newInstance() : new HashSet();
                		
                if ( paramClass != null ) {
                    if (paramClass == Locale.class) {
                        for ( Object o : list ) {
                            values.add(parseLocale((String)o));
                        }
                        mf.field.set(entity, values);
                    } else if (paramClass.isEnum()) {
                        for ( Object o : list ) {
                            values.add(Enum.valueOf(paramClass, (String)o));
                        }
                        mf.field.set(entity, values);
                    } else {
                        for ( Object o : list ) {
                            values.add(o);
                        }
                        mf.field.set(entity, values);
                    }
                } else {
                	mf.field.set(entity, list);
                }

            } else {
            	mf.field.set(entity, mongoValue != null ? mongoValue.listClass().newInstance() : new ArrayList());
            }

        } else if (ReflectionUtils.implementsInterface(fieldType, Map.class)) {
            if ( dbObject.containsField(name) ) {
                Map<Object,Object> map = (Map<Object,Object>) dbObject.get(name);
                Map values = mongoValue != null ? mongoValue.mapClass().newInstance() : new HashMap();
                for ( Map.Entry<Object,Object> entry : map.entrySet() ) {
                    if ( entry.getValue().getClass() == Locale.class ) {
                        values.put(entry.getKey(), parseLocale((String)entry.getValue()));
                    } else if ( entry.getValue().getClass().isEnum() ) {
                        Class enumClass = entry.getValue().getClass();
                        values.put(entry.getKey(), Enum.valueOf(enumClass, (String)entry.getValue()));
                    } else {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
                mf.field.set(entity, values);

            } else {
            	mf.field.set(entity, mongoValue != null ? mongoValue.mapClass().newInstance() : new HashMap());
            }

        } else {
            if ( dbObject.containsField(name) ) {
            	mf.field.set(entity, objectFromValue(mf.field.getType(), dbObject, name));
            }
        }
    }
    void mapEmbeddedFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) throws Exception {
        Embedded mongoEmbedded = (Embedded)mf.getAnnotation(Embedded.class);
        String name = mf.name;

        Class fieldType = mf.field.getType();
        boolean bSet = ReflectionUtils.implementsInterface(fieldType, Set.class);
        boolean bList =ReflectionUtils.implementsInterface(fieldType, List.class);
        if ( bSet || bList ) {

        	// multiple documents in a List
            Class docObjClass = ReflectionUtils.getParameterizedClass(mf.field);
            Collection docs = (bList) ? mongoEmbedded.listClass().newInstance() : mongoEmbedded.setClass().newInstance();

            if ( dbObject.containsField(name) ) {
                Object value = dbObject.get(name);
                if ( value instanceof List ) {
                    List refList = (List) value;
                    for ( Object docDbObject : refList ) {
                        Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)docDbObject);
                        docObj = mapDBObjectToEntity((BasicDBObject)docDbObject, docObj);
                        docs.add(docObj);
                    }
                } else {
                    BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
                    Object docObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
                    docObj = mapDBObjectToEntity(docDbObject, docObj);
                    docs.add(docObj);
                }
            }
            mf.field.set(entity, docs);
        } else if (ReflectionUtils.implementsInterface(fieldType, Map.class)) {
            Class docObjClass = ReflectionUtils.getParameterizedClass(mf.field, 1);
            Map map = mongoEmbedded.mapClass().newInstance();
            if ( dbObject.containsField(name) ) {
                BasicDBObject value = (BasicDBObject) dbObject.get(name);
                for ( Map.Entry entry : value.entrySet() ) {
                    Object docObj = createEntityInstanceForDbObject(docObjClass, (BasicDBObject)entry.getValue());
                    docObj = mapDBObjectToEntity((BasicDBObject)entry.getValue(), docObj);
                    map.put(entry.getKey(), docObj);
                }
            }
            mf.field.set(entity, map);

        } else {
            // single document
            Class docObjClass = fieldType;
            if ( dbObject.containsField(name) ) {
                BasicDBObject docDbObject = (BasicDBObject) dbObject.get(name);
                Object refObj = createEntityInstanceForDbObject(docObjClass, docDbObject);
                refObj = mapDBObjectToEntity(docDbObject, refObj);
                mf.field.set(entity, refObj);
            }
        }
    }

    void mapReferencesFromDBObject( BasicDBObject dbObject, MappedField mf, Object entity ) throws Exception {
        Reference mongoReference = (Reference)mf.getAnnotation(Reference.class);
        String name = mf.name;

        
        Class fieldType = mf.field.getType();
        boolean bSet = ReflectionUtils.implementsInterface(fieldType, Set.class);
        boolean bList = ReflectionUtils.implementsInterface(fieldType, List.class);

        if (bSet || bList) {
            // multiple references in a List
            Class referenceObjClass = ReflectionUtils.getParameterizedClass(mf.field);
            Collection references = bSet ? mongoReference.setClass().newInstance() : mongoReference.listClass().newInstance();
            
            if ( dbObject.containsField(name) ) {
                Object value = dbObject.get(name);
                if ( value instanceof List ) {
                    List refList = (List) value;
                    for ( Object dbRefObj : refList ) {
                        DBRef dbRef = (DBRef) dbRefObj;
                        BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                        Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                        refObj = mapDBObjectToEntity(refDbObject, refObj);
                        references.add(refObj);
                    }
                } else {
                    DBRef dbRef = (DBRef) dbObject.get(name);
                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
                    Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                    refObj = mapDBObjectToEntity(refDbObject, refObj);
                    references.add(refObj);
                }
            }
            mf.field.set(entity, references);

        } else if (ReflectionUtils.implementsInterface(fieldType, Map.class)) {
            Class referenceObjClass = ReflectionUtils.getParameterizedClass(mf.field, 1);
            Map map = mongoReference.mapClass().newInstance();
            if ( dbObject.containsField(name) ) {
                BasicDBObject value = (BasicDBObject) dbObject.get(name);
                for ( Map.Entry entry : value.entrySet() ) {
                    DBRef dbRef = (DBRef) entry.getValue();
                    BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                    Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                    refObj = mapDBObjectToEntity(refDbObject, refObj);
                    map.put(entry.getKey(), refObj);
                }
            }
            mf.field.set(entity, map);
            
        } else {
        	
            // single reference
            Class referenceObjClass = fieldType;
            if ( dbObject.containsField(name) ) {
                DBRef dbRef = (DBRef) dbObject.get(name);
                BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();

                Object refObj = createEntityInstanceForDbObject(referenceObjClass, refDbObject);
                refObj = mapDBObjectToEntity(refDbObject, refObj);
                mf.field.set(entity, refObj);
            }
        }
    }
    
    private static Locale parseLocale(String localeString) {
        if (localeString != null && localeString.length() > 0) {
            StringTokenizer st = new StringTokenizer(localeString, "_");
            String language = st.hasMoreElements() ? st.nextToken() : Locale.getDefault().getLanguage();
            String country = st.hasMoreElements() ? st.nextToken() : "";
            String variant = st.hasMoreElements() ? st.nextToken() : "";
            return new Locale(language, country, variant);
        }
        return null;
    }
    
    /** turns the object intto an ObjectId if it is/should-be one */
	public static Object fixupId(Object id) {
		if ((id instanceof String) && ObjectId.isValid((String)id)) return new ObjectId((String)id);
		return id;
	}
    /** Converts known types from mongodb -> java. Really it just converts enums and locales from strings */
    public static Object objectFromValue( Class c, BasicDBObject dbObject, String name ) {
        if (c == String.class) {
            return dbObject.getString(name);
        } else if (c == Integer.class || c == int.class) {
            return dbObject.getInt(name);
        } else if (c == Long.class || c == long.class) {
            return dbObject.getLong(name);
        } else if (c == Locale.class) {
            return parseLocale(dbObject.getString(name));
        } else if (c.isEnum()) {
            return Enum.valueOf(c, dbObject.getString(name));
        }
        return dbObject.get(name);
    }

    /** Converts known types from java -> mongodb. Really it just converts enums and locales to strings */
    public static Object objectToValue(Class clazz, Object obj) {

    	if(clazz == null) clazz = obj.getClass();
        if ( clazz.isEnum() ) {
            return ((Enum) obj).name();
        } else if ( clazz == Locale.class ) {
          	return ((Locale) obj).toString();
        } else {
            return obj;
        }
    	
    }
    
    /** Converts known types from java -> mongodb. Really it just converts enums and locales to strings */
    public static Object objectToValue(Object obj) {
    	return objectToValue(obj.getClass(), obj);
    }
}
