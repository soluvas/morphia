package com.google.code.morphia.mapping.lazy;

import com.google.code.morphia.Key;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestCGLibLazyProxyFactory extends ProxyTestBase
{
	public final void testCreateProxy()
	{
		final E e = new E();
		e.setFoo("bar");
		final Key<E> key = ds.save(e);
		E eProxy = new CGLibLazyProxyFactory().createProxy(E.class, key,
				new DefaultDatastoreProvider());

		assertNotFetched(eProxy);
		assertEquals("bar", eProxy.getFoo());
		assertFetched(eProxy);

		eProxy = deserialize(eProxy);
		assertNotFetched(eProxy);
		assertEquals("bar", eProxy.getFoo());
		assertFetched(eProxy);

	}
	public static class E extends AbstractMongoEntity
	{
		private String foo;

		public void setFoo(final String string)
		{
			foo = string;
		}

		public String getFoo()
		{
			return foo;
		}
	}

}
