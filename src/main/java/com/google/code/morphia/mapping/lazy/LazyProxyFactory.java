/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

import java.util.Collection;

import com.google.code.morphia.Key;

/**
 * @author uwe schaefer
 */
public interface LazyProxyFactory
{
    <T> T createProxy(Class<T> targetClass, final Key<T> key, final DatastoreProvider p);

    <T extends Collection> T createListProxy(T listToProxy, Class referenceObjClass, DatastoreProvider p);

}
