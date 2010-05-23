/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

import java.util.Map;

import com.google.code.morphia.Datastore;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public interface EncodingContext {
	Datastore getDatastore();
	
	Map getEntityCache();
	
	// this is temporary
	// Mapper getMapper();
}
