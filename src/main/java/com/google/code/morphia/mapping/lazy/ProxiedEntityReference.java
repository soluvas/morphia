/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

/**
 * @author Uwe SchÃ¤fer, (schaefer@thomas-daily.de)
 *
 */
public interface ProxiedEntityReference
{
    String __getEntityId();

    boolean __isFetched();
}
