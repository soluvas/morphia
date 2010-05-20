/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

import java.util.List;

import com.google.code.morphia.Key;

/**
 * @author Uwe SchÃ¤fer, (schaefer@thomas-daily.de)
 *
 */
public interface ProxiedEntityReferenceList
{

    void __add(Key key);

    List<Key<?>> __getKeysAsList();

    boolean __isFetched();

    Class __getReferenceObjClass();

}
