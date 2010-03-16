package com.google.code.morphia;

import java.util.Iterator;

import com.mongodb.DBObject;
/**
 * 
 * @author Scott Hernandez
 */
public interface Datastore {
	<T> T get(Object clazzOrEntity, long id);
	<T> T get(Object clazzOrEntity, String id);
	<T> Iterator<T> get(Object clazzOrEntity, long[] ids);
	<T> Iterator<T> get(Object clazzOrEntity, String[] ids);

	<T> void save(T entity);
	<T> void save(Iterable<T> entities);

	<T> void delete(T entity);
	<T> void delete(Object clazzOrEntity, long id);
	<T> void delete(Object clazzOrEntity, String id);
	<T> void delete(Object clazzOrEntity, long[] ids);
	<T> void delete(Object clazzOrEntity, String[] ids);

	/**
	 * Find all instances of collectionName
	 * @param <T> Type to return
	 * @param clazzOrObject Class or entity to get collectionName from
	 */
	<T> Iterator<T> find(Object clazzOrEntity);
	<T> Iterator<T> find(Object clazzOrEntity, DBObject query);
	<T> Iterator<T> find(Object clazzOrEntity, DBObject query, DBObject fields, int offset, int size);
	
	/**
	 * Gets the count of the CollectionName
	 */
	<T> long getCount(Object clazzOrEntity);
	<T> long getCount(Object clazzOrEntity, DBObject query);
}
