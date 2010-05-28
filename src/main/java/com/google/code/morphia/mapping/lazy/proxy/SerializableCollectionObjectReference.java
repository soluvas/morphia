/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;

public class SerializableCollectionObjectReference<T> extends AbstractReference implements ProxiedEntityReferenceList {
	
	private ArrayList<Key<?>> listOfKeys;
	
	public SerializableCollectionObjectReference(final Collection<T> type, final Class<T> referenceObjClass,
			final boolean ignoreMissing, final DatastoreProvider p) {
		
		super(p, referenceObjClass, ignoreMissing);
		
		object = type;
		listOfKeys = new ArrayList<Key<?>>();
	}
	
	@Override
	protected synchronized Object fetch() {
		Collection<T> c = (Collection<T>) object;
		c.clear();
		
		int numberOfEntitiesExpected = listOfKeys.size();
		List<T> retrievedEntities = p.get().getByKeys(referenceObjClass, (List) __getKeysAsList());
		
		if (!ignoreMissing && (numberOfEntitiesExpected != retrievedEntities.size())) {
			throw new LazyReferenceFetchingException("During the lifetime of a proxy of type '"
					+ c.getClass().getSimpleName() + "', some referenced Entities of type '"
					+ referenceObjClass.getSimpleName() + "' have disappeared from the Datastore.");
		}
		
		c.addAll(retrievedEntities);
		return c;
	}
	
	public List<Key<?>> __getKeysAsList() {
		return Collections.unmodifiableList(listOfKeys);
	}
	
	@Override
	public void __add(final Key key) {
		listOfKeys.add(key);
	}
	
	@Override
	protected void beforeWriteObject() {
		if (!__isFetched())
			return;
		else {
			syncKeys();
			((Collection<T>) object).clear();
		}
	}
	
	private void syncKeys() {
		
		Datastore ds = p.get();
		
		listOfKeys.clear();
		for (Object e : ((Collection) object)) {
			listOfKeys.add(ds.getKey(e));
		}
	}
}