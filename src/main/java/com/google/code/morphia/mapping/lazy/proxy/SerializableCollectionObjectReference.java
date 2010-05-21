/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;

public class SerializableCollectionObjectReference extends AbstractReference
implements ProxiedEntityReferenceList {

	private ArrayList<String> listOfKeysAsStrings;

	public SerializableCollectionObjectReference(final Collection type,
			final Class referenceObjClass, final boolean ignoreMissing,
			final DatastoreProvider p) {

		super(p, referenceObjClass, ignoreMissing);

		object = type;
		listOfKeysAsStrings = new ArrayList<String>();
	}

	@Override
	protected synchronized Object fetch() {
		Collection c = (Collection) object;
		c.clear();

		int numberOfEntitiesExpected = listOfKeysAsStrings.size();
		List retrievedEntities = p.get().getByKeys(referenceObjClass,
				(List) __getKeysAsList());

		if (!ignoreMissing
				&& (numberOfEntitiesExpected != retrievedEntities.size())) {
			throw new LazyReferenceFetchingException(
					"During the lifetime of a proxy of type '"
					+ c.getClass().getSimpleName()
					+ "', some referenced Entities of type '"
					+ referenceObjClass.getSimpleName()
					+ "' have disappeared from the Datastore.");
		}

		c.addAll(retrievedEntities);
		return c;
	}

	public List<Key<?>> __getKeysAsList() {
		List l = new ArrayList(listOfKeysAsStrings.size());
		for (String s : listOfKeysAsStrings) {
			l.add(new Key(referenceObjClass, s));
		}
		return l;
	}

	@Override
	public void __add(final Key key) {
		listOfKeysAsStrings.add(key.getId().toString());
	}

	@Override
	protected void beforeWriteObject() {
		((List) object).clear();
	}
}