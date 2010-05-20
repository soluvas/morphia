package com.google.code.morphia.mapping.lazy;

import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestLazySingleReference extends ProxyTestBase {

	// public final void testCreateProxy() {
	// RootEntity root = new RootEntity();
	// ReferencedEntity referenced = new ReferencedEntity();
	//
	// root.r = referenced;
	// root.r.setFoo("bar");
	//
	// ds.save(referenced);
	// ds.save(root);
	//
	// root = ds.get(root);
	//
	// assertNotFetched(root.r);
	// assertEquals("bar", root.r.getFoo());
	// assertFetched(root.r);
	// assertEquals("bar", root.r.getFoo());
	//
	// // now remove it from DB
	// ds.delete(root.r);
	//
	// root = deserialize(root);
	// assertNotFetched(root.r);
	//
	// try {
	// // must fail
	// root.r.getFoo();
	// fail("Expected Exception did not happen");
	// } catch (LazyReferenceFetchingException expected) {
	// // fine
	// }
	//
	// }
	//
	// public final void testShortcutInterface() {
	// RootEntity root = new RootEntity();
	// ReferencedEntity reference = new ReferencedEntity();
	//
	// root.r = reference;
	// reference.setFoo("bar");
	//
	// Key<ReferencedEntity> k = ds.save(reference);
	// String keyAsString = k.getId().toString();
	// ds.save(root);
	//
	// root = ds.get(root);
	//
	// ReferencedEntity p = root.r;
	//
	// assertIsProxy(p);
	// assertNotFetched(p);
	// assertEquals(keyAsString, ((ProxiedEntityReference) p).__getEntityId());
	// // still unfetched?
	// assertNotFetched(p);
	// p.getFoo();
	// // should be fetched now.
	// assertFetched(p);
	//
	// root = deserialize(root);
	// p = root.r;
	// assertNotFetched(p);
	// p.getFoo();
	// // should be fetched now.
	// assertFetched(p);
	//
	// root = ds.get(root);
	// p = root.r;
	// assertNotFetched(p);
	// ds.save(root);
	// assertNotFetched(p);
	// }


	public final void testSameProxy() {
		RootEntity root = new RootEntity();
		ReferencedEntity reference = new ReferencedEntity();

		root.r = reference;
		root.secondReference = reference;
		reference.setFoo("bar");

		ds.save(reference);
		ds.save(root);

		root = ds.get(root);
		assertSame(root.r, root.secondReference);
	}

	public final void testSerialization() {
		RootEntity e1 = new RootEntity();
		ReferencedEntity e2 = new ReferencedEntity();

		e1.r = e2;
		e2.setFoo("bar");

		ds.save(e2);
		ds.save(e1);

		e1 = deserialize(ds.get(e1));

		assertNotFetched(e1.r);
		assertEquals("bar", e1.r.getFoo());
		assertFetched(e1.r);

		e1 = deserialize(e1);
		assertNotFetched(e1.r);
		assertEquals("bar", e1.r.getFoo());
		assertFetched(e1.r);

	}

	public static class RootEntity extends AbstractMongoEntity {
		@Reference(lazy = true)
		ReferencedEntity r;
		@Reference(lazy = true)
		ReferencedEntity secondReference;

	}

	public static class ReferencedEntity extends AbstractMongoEntity {
		private String foo;

		public void setFoo(final String string) {
			foo = string;
		}

		public String getFoo() {
			return foo;
		}
	}

}
