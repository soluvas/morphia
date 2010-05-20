package com.google.code.morphia.mapping.lazy;

import java.util.Collection;
import java.util.LinkedList;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestReferenceCollection extends ProxyTestBase
{

    @Test
    public final void testCreateProxy()
    {
        A a = new A();
        B b1 = new B();
        B b2 = new B();

        a.bs.add(b1);
        a.bs.add(b2);

        Collection<B> lazyBs = a.lazyBs;
        lazyBs.add(b1);
        lazyBs.add(b2);

        ds.save(b2, b1, a);

        a = ds.get(a);
        
        lazyBs = a.lazyBs;
        Assert.assertNotNull(lazyBs);
		assertNotFetched(lazyBs);

        Assert.assertNotNull(lazyBs.iterator().next());
		assertFetched(lazyBs);

        a = deserialize(a);

        lazyBs = a.lazyBs;
        Assert.assertNotNull(lazyBs);
		assertNotFetched(lazyBs);

        Assert.assertNotNull(lazyBs.iterator().next());
		assertFetched(lazyBs);

        a = deserialize(a);

        ds.save(a);
        lazyBs = a.lazyBs;
		assertNotFetched(lazyBs);
    }

   

    public static class A extends AbstractMongoEntity
    {
        @Reference
        Collection<B> bs = new LinkedList();

        @Reference(lazy = true)
        Collection<B> lazyBs = new LinkedList();

    }

    public static class B extends AbstractMongoEntity
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
