package com.google.code.morphia.mapping.lazy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestLazySingleReference extends TestBase
{

    @Test
    public final void testCreateProxy()
    {
        E1 e1 = new E1();
        E2 e2 = new E2();

        e1.e2a = e2;
        e2.setFoo("bar");

        ds.save(e2);
        ds.save(e1);

        e1 = ds.get(e1);
        System.out.println("before fetching");
        e2 = e1.e2a;
        System.out.println("still before fetching");

        System.out.println("expecting fetch here ");
        Assert.assertEquals("bar", e2.getFoo());
        Assert.assertNull(e1.e2b);
    }

    @Test
    public final void testShortcutInterface()
    {
        E1 e1 = new E1();
        E2 e2 = new E2();

        e1.e2a = e2;
        e2.setFoo("bar");

        Key<E2> k = ds.save(e2);
        String keyAsString = k.getId().toString();
        ds.save(e1);

        e1 = ds.get(e1);

        E2 p = e1.e2a;

        Assert.assertTrue(p instanceof ProxiedEntityReference);
        Assert.assertFalse(((ProxiedEntityReference) p).__isFetched());
        Assert.assertEquals(keyAsString, ((ProxiedEntityReference) p).__getEntityId());
        // still unfetched?
        Assert.assertFalse(((ProxiedEntityReference) p).__isFetched());
        p.getFoo();
        // should be fetched now.
        Assert.assertTrue(((ProxiedEntityReference) p).__isFetched());

        e1 = deserialize(e1);
        p = e1.e2a;
        Assert.assertFalse(((ProxiedEntityReference) p).__isFetched());
        p.getFoo();
        // should be fetched now.
        Assert.assertTrue(((ProxiedEntityReference) p).__isFetched());

        e1 = ds.get(e1);
        p = e1.e2a;
        Assert.assertFalse(((ProxiedEntityReference) p).__isFetched());
        ds.save(e1);
        Assert.assertFalse(((ProxiedEntityReference) p).__isFetched());
    }

    public final void testSameProxy()
    {
        E1 e1 = new E1();
        E2 e2 = new E2();

        e1.e2a = e2;
        e1.e2b = e2;
        e2.setFoo("bar");

        ds.save(e2);
        ds.save(e1);

        e1 = ds.get(e1);
        System.out.println("before fetching");

        Assert.assertSame(e1.e2a, e1.e2b);
    }

    @Test
    public final void testSerialization()
    {
        E1 e1 = new E1();
        E2 e2 = new E2();

        e1.e2a = e2;
        e2.setFoo("bar");

        ds.save(e2);
        ds.save(e1);

        e1 = deserialize(ds.get(e1));

        System.out.println("expecting fetch here ");
        Assert.assertEquals("bar", e1.e2a.getFoo());
        Assert.assertEquals("bar", e1.e2a.getFoo());
        Assert.assertEquals("bar", e1.e2a.getFoo());

        e1 = deserialize(e1);
        System.out.println("and again");
        Assert.assertEquals("bar", e1.e2a.getFoo());
        Assert.assertEquals("bar", e1.e2a.getFoo());

    }

    private E1 deserialize(E1 e1)
    {
        try
        {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(e1);
        os.close();
        byte[] ba = baos.toByteArray();

        return (E1) new ObjectInputStream(new ByteArrayInputStream(ba)).readObject();
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }

    }
    public static class E1 extends AbstractMongoEntity
    {
        @Reference(lazy = true)
        E2 e2a;
        @Reference(lazy = true)
        E2 e2b;

    }

    public static class E2 extends AbstractMongoEntity
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
