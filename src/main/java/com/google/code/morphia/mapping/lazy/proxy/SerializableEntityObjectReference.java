/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;

public class SerializableEntityObjectReference extends AbstractReference
implements ProxiedEntityReference {
	// TODO store key raw as soon as it is possible (Serialization issue)
	private final String keyAsString;

	@SuppressWarnings("unchecked")
	public SerializableEntityObjectReference(final Class targetClass,
			final DatastoreProvider p, final Key key) {

		super(p, targetClass, false);
		keyAsString = key.getId().toString();
	}

	@Override
	public String __getEntityId() {
		return keyAsString;
	}

	@Override
	protected Object fetch() {

		Object entity = p.get().get(referenceObjClass, keyAsString);
		if (entity == null) {
			throw new LazyReferenceFetchingException(
					"During the lifetime of the proxy, the Entity identified by '"
					+ keyAsString + "' disappeared from the Datastore.");
		}
		return entity;
	}

	@Override
	protected void beforeWriteObject() {
		object = null;
	}
}