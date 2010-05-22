/**
 * 
 */
package com.google.code.morphia.mapping;

import java.io.IOException;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;

/**
 * @author doc
 * 
 */
public class TestSerializerTest extends JUnit3TestBase {
	
	private static final String TEST_TEXT = "In 1970, the British Empire lay in ruins, and foreign nationalists frequented the streets - many of them Hungarians (not the streets - the foreign nationals). Anyway, many of these Hungarians went into tobacconist's shops to buy cigarettes.... ";

	public final void testSerialize() throws IOException, ClassNotFoundException {
		byte[] test = new byte[2048];
		byte[] stringBytes = TEST_TEXT.getBytes();
		System.arraycopy(stringBytes, 0, test, 0, stringBytes.length);
		
		byte[] ser = Serializer.serialize(test, false);
		byte[] after = (byte[]) Serializer.deserialize(ser, false);
		assertTrue(ser.length > 2048);
		assertTrue(after.length == 2048);
		assertTrue(new String(after).startsWith(TEST_TEXT));

		ser = Serializer.serialize(test, true);
		after = (byte[]) Serializer.deserialize(ser, true);
		assertTrue(ser.length < 2048);
		assertTrue(after.length == 2048);
		assertTrue(new String(after).startsWith(TEST_TEXT));
	}

	public final void testSerializedAttribute() throws IOException, ClassNotFoundException {
		byte[] test = new byte[2048];
		byte[] stringBytes = TEST_TEXT.getBytes();
		System.arraycopy(stringBytes, 0, test, 0, stringBytes.length);
		
		E e = new E();
		e.payload1 = test;
		e.payload2 = test;
		
		ds.save(e);
		e = ds.get(e);
		
		assertTrue(e.payload1.length == 2048);
		assertTrue(new String(e.payload1).startsWith(TEST_TEXT));
		
		assertTrue(e.payload2.length == 2048);
		assertTrue(new String(e.payload2).startsWith(TEST_TEXT));
		
	}
	
	public static class E {
		@Id
		String id;
		@Serialized
		byte[] payload1;
		@Serialized(compress = true)
		byte[] payload2;

	}
}