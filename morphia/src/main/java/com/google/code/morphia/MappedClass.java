/**
 * 
 */
package com.google.code.morphia;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.annotations.MongoCollectionName;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoEmbedded;
import com.google.code.morphia.annotations.MongoID;
import com.google.code.morphia.annotations.MongoReference;
import com.google.code.morphia.annotations.MongoTransient;
import com.google.code.morphia.annotations.MongoValue;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * Represents a mapped class between the MongoDB DBObject and the java POJO.
 * 
 * This class will validate classes to make sure they meet the requirement for persistence.
 * 
 * @author Scott Hernandez
 */
public class MappedClass {
    private static final Logger logger = Logger.getLogger(MappedClass.class.getName());
	
    /** special fields representing the Key of the object */
    public Field idField, collectionNameField;
	
    /** special annotations representing the type the object */
	public MongoDocument mongoDocumentAnnotation;
	public MongoEmbedded mongoEmbeddedAnnotation;
	
    /** the collectionName based on the type and @MongoDocument value(); this can be overriden by the @MongoCollectionName field on the instance*/
	public String defaultCollectionName;

	/** a list of the fields to map */
	public List<Field> persistenceFields = new ArrayList<Field>();
	
	@SuppressWarnings("unchecked")
	/** the type we are mapping to/from */
	public Class clazz;
	
	@SuppressWarnings("unchecked")
	public MappedClass(Class clazz) {
        this.clazz = clazz;

        mongoEmbeddedAnnotation = ReflectionUtils.getClassMongoEmbeddedAnnotation(clazz);
        mongoDocumentAnnotation = ReflectionUtils.getClassMongoDocumentAnnotation(clazz);

        defaultCollectionName = (mongoDocumentAnnotation == null || mongoDocumentAnnotation.value().equals(Mapper.IGNORED_FIELDNAME)) ? clazz.getSimpleName() : mongoDocumentAnnotation.value();
        for (Field field : ReflectionUtils.getDeclaredAndInheritedFields(clazz, false)) {
            if (field.isAnnotationPresent(MongoID.class)) {
                idField = field;
            } else if (field.isAnnotationPresent(MongoCollectionName.class)) {
            	collectionNameField = field;
            } else if (field.isAnnotationPresent(MongoTransient.class)) {
            	continue;
            } else if (	field.isAnnotationPresent(MongoValue.class) || 
            			field.isAnnotationPresent(MongoReference.class) || 
            			field.isAnnotationPresent(MongoEmbedded.class) || 
            			isSupportedType(field.getType())) {
                persistenceFields.add(field);   	
            } else {
            	logger.warning("Ignoring (will not persist) field: " + clazz.getName() + "." + field.getName() + " [" + field.getType().getSimpleName() + "]");
            }
        }
        
        Validate();
	}
	
	/** Checks to see if it a Map/Set/List or a property supported by the MangoDB java driver*/
	@SuppressWarnings("unchecked")
	public boolean isSupportedType(Class clazz) {
		return  ReflectionUtils.isValidMapValueType(clazz) || 
				ReflectionUtils.implementsInterface(clazz, List.class) || 
				ReflectionUtils.implementsInterface(clazz, Map.class) || 
				ReflectionUtils.implementsInterface(clazz, Set.class);
	}
	
	public void Validate() {
		// No @MongoDocument with @MongoEmbedded
        if (mongoDocumentAnnotation != null && mongoEmbeddedAnnotation != null ) {
            throw new MongoMappingException(
                    "In [" + clazz.getName()
                           + "]: Cannot have both @MongoDocument and @MongoEmbedded annotation at class level.");
        }

        for (Field field : persistenceFields) {
            field.setAccessible(true);
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("In [" + clazz.getName() + "]: Processing field: " + field.getName());
            }

            if ( field.isAnnotationPresent(MongoValue.class) ) {
                // make sure that the property type is supported
                if ( !ReflectionUtils.implementsInterface(field.getType(), List.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Set.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Map.class)
                        && !ReflectionUtils.isPropertyType(field.getType())
                        ) {
                    throw new MongoMappingException("In [" + clazz.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @MongoValue is of type that cannot be mapped (type is "
                            + field.getType().getName() + ").");
                }

            } else if (field.isAnnotationPresent(MongoEmbedded.class)) {
                if ( !ReflectionUtils.implementsInterface(field.getType(), List.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Set.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Map.class)
                        && (!field.getType().isInterface() && ReflectionUtils.getClassMongoEmbeddedAnnotation(field.getType()) == null) ) {

                    throw new MongoMappingException(
                            "In ["
                                    + clazz.getName()
                                    + "]: Field ["
                                    + field.getName()
                                    + "] which is annotated as @MongoEmbedded is of type [" + field.getType().getName() + "] which cannot be embedded.");
                }

            } else if (field.isAnnotationPresent(MongoReference.class)) {
                if ( !ReflectionUtils.implementsInterface(field.getType(), List.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Set.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Map.class)
                        && (!field.getType().isInterface() && ReflectionUtils.getClassMongoDocumentAnnotation(field.getType()) == null) ) {

                    throw new MongoMappingException(
                            "In ["
                                    + clazz.getName()
                                    + "]: Field ["
                                    + field.getName()
                                    + "] which is annotated as @MongoReference is of type [" + field.getType().getName() + "] which cannot be referenced.");
                }
            }
        }

        // make sure @MongoCollectionName field is a String
        if (collectionNameField != null && collectionNameField.getType() != String.class) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: Field [" + collectionNameField.getName()
                    + "] which is annotated as @MongoCollectionName must be of type java.lang.String, but is of type: "
                    + collectionNameField.getType().getName());
        }

        
//        // make sure @MongoId field is a String
//        if (idField != null && idField.getType() != String.class) {
//            throw new MongoMappingException("In [" + clazz.getName() + "]: Field [" + idField.getName()
//                    + "] which is annotated as @MongoID must be of type java.lang.String, but is of type: "
//                    + idField.getType().getName());
//        }

        
        //Only embedded class can have no id field
        if (idField == null && mongoEmbeddedAnnotation == null) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: No field is annotated with @MongoID; but it is required");
        }

        
        //Embedded classes should not have an id
        if (mongoEmbeddedAnnotation != null && idField != null) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: @MongoEmbedded classes cannot specify a @MongoID field");
        }

        //Embedded classes should not have a CollectionName
        if (mongoEmbeddedAnnotation != null && collectionNameField != null) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: @MongoEmbedded classes cannot specify a @MongoCollectionName field");
        }

        //Embedded classes can not have a fieldName value() specified
        if (mongoEmbeddedAnnotation != null && !mongoEmbeddedAnnotation.value().equals(Mapper.IGNORED_FIELDNAME)) {
            throw new MongoMappingException("In [" + clazz.getName() + "]: @MongoEmbedded classes cannot specify a fieldName value(); this is on applicable on fields");
        }
	}
	
	@Override @SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (obj instanceof Class) return equals((Class)obj);
		else if (obj instanceof MappedClass) return equals((MappedClass)obj);
		else return false;
	}

	public boolean equals(MappedClass clazz) {
		return this.clazz.equals(clazz);
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Class clazz) {
		return this.clazz.equals(clazz);
	}
}