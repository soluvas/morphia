/**
 * 
 */
package com.google.code.morphia.mapping.validation.classrules;

import java.util.ArrayList;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class RegisterAfterUseTest extends JUnit3TestBase {
	
	public static class Broken extends AbstractMongoEntity {
		@Property("foo")
		@Embedded("bar")
		ArrayList l;
	}
	
	
	public void testRegisterAfterUse() throws Exception {
		
		// this would have failed: morphia.map(Broken.class);

		Broken b = new Broken();
		ds.save(b); // imho must not work
		fail();
		
		// doe not revalidate due to being used already!
		morphia.map(Broken.class);
		fail();
	}
}
