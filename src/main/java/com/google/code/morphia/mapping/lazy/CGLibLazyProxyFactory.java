/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.code.morphia.Key;
import com.thoughtworks.proxy.factory.CglibProxyFactory;
import com.thoughtworks.proxy.kit.ObjectReference;
import com.thoughtworks.proxy.toys.dispatch.Dispatching;
import com.thoughtworks.proxy.toys.hotswap.HotSwapping;

/**
 * i have to admin, there are plenty of open questions for me on that
 * Key-business...
 * 
 * @author uwe schaefer
 */
public class CGLibLazyProxyFactory implements LazyProxyFactory
{
    public CGLibLazyProxyFactory()
    {
    }

    @SuppressWarnings(
    { "unchecked", "deprecation" })
    @Override
    public <T> T createProxy(Class<T> targetClass, Key<T> key, DatastoreProvider p)
    {
        CglibProxyFactory factory = new CglibProxyFactory();
        SerializableObjectReference objectReference = new SerializableObjectReference(targetClass, p, key);
        T backend = (T) HotSwapping.object(new Class[]
        { targetClass, Serializable.class }, factory, objectReference, true);

        T proxy = (T) Dispatching.object(new Class[]
        { ProxiedEntityReference.class, targetClass, Serializable.class }, new Object[]
        { objectReference, backend }, factory);

        return proxy;

    }
    static class SerializableObjectReference implements ObjectReference, Serializable, ProxiedEntityReference
    {
        // TODO store key raw as soon as it is possible (Serialization issue)
        private final String keyAsString;
        private final DatastoreProvider p;
        private final Class targetClass;
        private transient Object target;
        private final static Object NULL = new Object();

        public SerializableObjectReference(Class targetClass, DatastoreProvider p, Key key)
        {
            this.targetClass = targetClass;
            this.p = p;
            this.keyAsString = key.getId().toString();
        }

        @Override
        public void set(Object item)
        {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public synchronized Object get()
        {
            if (target == null)
            {
                Object retrievedObject = p.get().get(targetClass, keyAsString);
                if (retrievedObject == null)
                    target = NULL;
                else
                    target = retrievedObject;
            }
            return target == NULL ? null : target;
        }

        @Override
        public String __getEntityId()
        {
            return keyAsString;
        }

        @Override
        public boolean __isFetched()
        {
            return target != null;
        }
    }

    @Override
    public <T extends Collection> T createListProxy(T listToProxy, Class referenceObjClass, DatastoreProvider p)
    {
        CglibProxyFactory factory = new CglibProxyFactory();
        Class<? extends Collection> targetClass = listToProxy.getClass();
        SerializableCollectionObjectReference objectReference = new SerializableCollectionObjectReference(listToProxy,
                referenceObjClass, p);
        T backend = (T) HotSwapping.object(new Class[]
        { targetClass, Serializable.class }, factory, objectReference, true);

        T proxy = (T) Dispatching.object(new Class[]
        { ProxiedEntityReferenceList.class, targetClass, Serializable.class }, new Object[]
        { objectReference, backend }, factory);

        return proxy;

    }
    static class SerializableCollectionObjectReference implements ObjectReference, Serializable,
            ProxiedEntityReferenceList
    {

        private ArrayList<String> listOfKeysAsStrings;
        private DatastoreProvider p;
        private Class referenceObjClass;
        private Collection list;

        public SerializableCollectionObjectReference(Collection type, Class referenceObjClass, DatastoreProvider p)
        {
            this.list = type;
            this.listOfKeysAsStrings = new ArrayList<String>();
            this.referenceObjClass = referenceObjClass;
            this.p = p;
        }

        @Override
        public Object get()
        {
            if (!__isFetched())
            {
                list.addAll(p.get().getByKeys(referenceObjClass, (List) __getKeysAsList()));
            }
            return list;
        }

        public List<Key<?>> __getKeysAsList()
        {
            List l = new ArrayList(listOfKeysAsStrings.size());
            for (String s : listOfKeysAsStrings)
            {
                l.add(new Key(this.referenceObjClass, s));
            }
            return l;
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException
        {
            // excessive hoop-jumping in order not to have to recreate the
            // instance.
            // as soon as weÂ´d have an ObjectFactory, that would be unnecessary
            list.clear();
            out.defaultWriteObject();
        }

        @Override
        public void set(Object item)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void __add(Key key)
        {
            listOfKeysAsStrings.add(key.getId().toString());
        }


        @Override
        public boolean __isFetched()
        {
            return !list.isEmpty() || listOfKeysAsStrings.isEmpty();
        }

        @Override
        public Class __getReferenceObjClass()
        {
            return referenceObjClass;
        }
    }
}
