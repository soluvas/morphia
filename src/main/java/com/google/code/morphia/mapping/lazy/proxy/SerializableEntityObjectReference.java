/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;

public class SerializableEntityObjectReference extends AbstractReference
implements ProxiedEntityReference {

	private final Key key;

	@SuppressWarnings("unchecked")
	public SerializableEntityObjectReference(final Class targetClass,
			final DatastoreProvider p, final Key key) {

		super(p, targetClass, false);
		this.key = key;
	}

	public Key __getKey() {
		return key;
	}

	@Override
	protected Object fetch() {

		Object entity = p.get().getByKey(referenceObjClass, key);
		if (entity == null) {
			throw new LazyReferenceFetchingException(
					"During the lifetime of the proxy, the Entity identified by '"
					+ key + "' disappeared from the Datastore.");
		}
		return entity;
	}

	@Override
	protected void beforeWriteObject() {
		object = null;
	}
}