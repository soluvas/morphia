package com.google.code.morphia.mapping.lazy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestReferenceCollection extends TestBase
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
        boolean isFetched = ((ProxiedEntityReferenceList) lazyBs).__isFetched();
        Assert.assertFalse(isFetched);

        Assert.assertNotNull(lazyBs.iterator().next());
        isFetched = ((ProxiedEntityReferenceList) lazyBs).__isFetched();
        Assert.assertTrue(isFetched);

        a = deserialize(a);

        lazyBs = a.lazyBs;
        Assert.assertNotNull(lazyBs);
        isFetched = ((ProxiedEntityReferenceList) lazyBs).__isFetched();
        Assert.assertFalse(isFetched);

        Assert.assertNotNull(lazyBs.iterator().next());
        isFetched = ((ProxiedEntityReferenceList) lazyBs).__isFetched();
        Assert.assertTrue(isFetched);

        a = deserialize(a);

        ds.save(a);
        lazyBs = a.lazyBs;
        Assert.assertNotNull(lazyBs);
        isFetched = ((ProxiedEntityReferenceList) lazyBs).__isFetched();
        Assert.assertFalse(isFetched);
    }

   
    private <T> T deserialize(T t)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(t);
            os.close();
            byte[] ba = baos.toByteArray();

            return (T) new ObjectInputStream(new ByteArrayInputStream(ba)).readObject();
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }

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
