/**
 * 
 */
package com.google.code.morphia.mapping;

import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class CharacterMappingTest extends JUnit3TestBase {
	public static class ContainsChar {
		@Id
		String id;
		char c;
	}
	
	public static class ContainsCharacter {
		@Id
		String id;
		Character c;
	}
	
	public static class ContainsCharArray {
		@Id
		String id;
		char c[];
	}
	
	public static class ContainsCharacterArray {
		@Id
		String id;
		Character c[];
	}
	

	@Test
	public void testCharMapping() throws Exception {
		morphia.map(ContainsChar.class);
		ContainsChar entity = new ContainsChar();
		char testChar = 'a';
		entity.c = testChar;
		Key<ContainsChar> savedKey = ds.save(entity);
		ContainsChar loaded = ds.get(ContainsChar.class, savedKey.getId());
		assertEquals(testChar, loaded.c);
		assertNotNull(loaded.id);
	}
	
	@Test
	public void testCharacterMapping() throws Exception {
		morphia.map(ContainsCharacter.class);
		ContainsCharacter entity = new ContainsCharacter();
		Character testChar = 'a';
		entity.c = testChar;
		Key<ContainsCharacter> savedKey = ds.save(entity);
		ContainsCharacter loaded = ds.get(ContainsCharacter.class, savedKey.getId());
		assertEquals(testChar, loaded.c);
		assertNotNull(loaded.id);
	}
	
	@Test
	public void testCharArrayMapping() throws Exception {
		morphia.map(ContainsCharArray.class);
		ContainsCharArray entity = new ContainsCharArray();
		char[] testChar = "My Hoovercraft is full of eels".toCharArray();
		entity.c = testChar;
		ds.save(entity);
		entity = ds.get(entity);
		
		for (int i = 0; i < testChar.length; i++) {
			char c = testChar[i];
			assertEquals(c, entity.c[i]);
		}
		assertNotNull(entity.id);
	}
	
	@Test
	public void testCharacterArrayMapping() throws Exception {
		morphia.map(ContainsCharacterArray.class);
		ContainsCharacterArray entity = new ContainsCharacterArray();
		Character[] testChar = new Character[] { new Character('a'), new Character('b') };
		entity.c = testChar;
		ds.save(entity);
		entity = ds.get(entity);
		
		assertEquals(testChar[0], entity.c[0]);
		assertEquals(testChar[1], entity.c[1]);
		assertNotNull(entity.id);
	}

}
