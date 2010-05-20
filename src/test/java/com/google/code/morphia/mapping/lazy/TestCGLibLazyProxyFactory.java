package com.google.code.morphia.mapping.lazy;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestCGLibLazyProxyFactory extends TestBase
{

    @Test
    public final void testCreateProxy()
    {
        DatastoreHolder.getInstance().set(this.ds);
        final E e = new E();
        e.setFoo("bar");
        final Key<E> key = this.ds.save(e);

        final E eProxy = new CGLibLazyProxyFactory().createProxy(E.class, key, new DefaultDatastoreProvider());
        System.out.println("got a proxy");
        Assert.assertEquals("bar", eProxy.getFoo());
    }

    public static class E extends AbstractMongoEntity
    {
        private String foo;

        public void setFoo(final String string)
        {
            this.foo = string;
        }

        public String getFoo()
        {
            return this.foo;
        }
    }

}
