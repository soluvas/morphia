package com.google.code.morphia.mapping.lazy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestKeySerializability extends TestBase
{

    @Test
    public final void testForStringIdKeyToBeSerializable()
    {
        A a = new A();
        Key<A> key = ds.save(a);
        deserialize(key);
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
        String foo = "bar";
    }

}
