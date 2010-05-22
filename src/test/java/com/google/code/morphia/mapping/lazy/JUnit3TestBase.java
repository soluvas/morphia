/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

import junit.framework.TestCase;

import org.junit.Ignore;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
@Ignore
public class JUnit3TestBase extends TestCase {
	protected Mongo mongo;
	protected DB db;
	protected Datastore ds;
	protected Morphia morphia = new Morphia();

	public JUnit3TestBase() {
		try {
			this.mongo = new Mongo();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.db = this.mongo.getDB("morphia_test");
		this.ds = this.morphia.createDatastore(this.mongo, this.db.getName());
	}

	@Override
	protected void tearDown() throws Exception {
		this.mongo.dropDatabase("morphia_test");
		super.tearDown();
	}

}
