package com.google.code.morphia;

import com.google.com.morphia.ofy.Query;
import com.mongodb.Mongo;
/**
 * 
 * @author Scott Hernandez
 */
public interface Datastore {
	<T> T get(Object clazzOrEntity, long id);
	<T> T get(Object clazzOrEntity, String id);
	<T> Iterable<T> get(Object clazzOrEntity, long[] ids);
	<T> Iterable<T> get(Object clazzOrEntity, String[] ids);

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
	<T> Query<T> find(Object clazzOrEntity);
	<T> Query<T> find(Object clazzOrEntity, String property, Object value);
	<T> Query<T> find(Object clazzOrEntity, String property, Object value, int offset, int size);
	
	/**
	 * Gets the count of the CollectionName
	 */
	<T> long getCount(Object clazzOrEntity);
	<T> long getCount(Query<T> query);
	
	Morphia getMorphia();
	Mongo getMongo();
}
