package com.google.code.morphia;

import com.google.code.morphia.utils.IndexDirection;
import com.mongodb.DB;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
/**
 * 
 * @author Scott Hernandez
 */
public interface Datastore {	
	/** Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id) */
	DBRef createRef(Object entity);
	/** Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id) */
	DBRef createRef(Object clazzOrEntity, Object id);

	/** Find the given entity (by collectionName/id); think of this as refresh */
	<T> T get(Object entityOrRef);
	/** Find the given entity (by id); shorthand for {@code findOne("_id =", ids)} */
	<T> T get(Object clazzOrEntity, long id);
	/** Find the given entity (by id); shorthand for {@code findOne("_id =", ids)} */
	<T> T get(Object clazzOrEntity, String id);
	/** Find the given entities (by id); shorthand for {@code find("_id in", ids)} */
	<T> Query<T> get(Object clazzOrEntity, long[] ids);
	/** Find the given entities (by id); shorthand for {@code find("_id in", ids)} */
	<T> Query<T> get(Object clazzOrEntity, String[] ids);

	/** Saves the entity (Object) and updates the @MongoID, @MondoCollectionName fields */
	<T> void save(T entity);
	/** Savess the entities (Objects) and updates the @MongoID, @MondoCollectionName fields */
	<T> void save(Iterable<T> entities);

	/** Deletes the given entity (by id) */
	<T> void delete(T entity);
	/** Deletes the given entity (by id) */
	<T> void delete(Object clazzOrEntity, long id);
	/** Deletes the given entity (by id) */
	<T> void delete(Object clazzOrEntity, String id);
	/** Deletes the given entities (by id) */
	<T> void delete(Object clazzOrEntity, long[] ids);
	/** Deletes the given entities (by id) */
	<T> void delete(Object clazzOrEntity, String[] ids);

	/** Find all instances by collectionName */
	<T> Query<T> find(Object clazzOrEntity);
	
	/** 
	 * <p>
	 * Find all instances by collectionName, and filter property.
	 * </p><p>
	 * This is the same as: {@code find(clazzOrEntity).filter(property, value); }
	 * </p>
	 */
	<T> Query<T> find(Object clazzOrEntity, String property, Object value);
	
	/** 
	 * <p>
	 * Find all instances by collectionName, and filter property.
	 * </p><p>
	 * This is the same as: {@code find(clazzOrEntity).filter(property, value).offset(offset).limit(size); }
	 * </p>
	 */
	<T> Query<T> find(Object clazzOrEntity, String property, Object value, int offset, int size);
	
	/** Gets the count of the CollectionName */
	<T> long getCount(Object clazzOrEntity);
	/** Gets the count of items returned by this query; same as {@code query.countAll()}*/
	<T> long getCount(Query<T> query);

	/** Ensures (creating if necessary) the index and direction */
	void ensureIndex(Object clazzOrEntity, String name, IndexDirection dir);
	/** Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed)}*/
	void ensureSuggestedIndexes();
	
	/** The instance this Datastore is using */
	Morphia getMorphia();
	/** The instance this Datastore is using */
	Mongo getMongo();
	/** The instance this Datastore is using */
	DB getDB();
}
