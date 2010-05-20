/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import java.util.ConcurrentModificationException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceFetchingException extends
		ConcurrentModificationException {
	public LazyReferenceFetchingException(final String msg) {
		super(msg);
	}
}
