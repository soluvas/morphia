/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia.ext;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Converters;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.converters.IntegerConverter;
import com.google.code.morphia.converters.SimpleValueConverter;
import com.google.code.morphia.converters.TypeConverter;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.BasicDBObject;

/**
 *
 * @author Scott Hernandez
 */
public class CustomConvertersTest extends TestBase {

	@SuppressWarnings("rawtypes") 
	static class CharacterToByteConverter extends TypeConverter implements SimpleValueConverter {
		public CharacterToByteConverter() { super(Character.class, char.class); }
		
		@Override
		public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
			if (fromDBObject == null) return null;
			IntegerConverter intConv = new IntegerConverter();
			Integer i = (Integer)intConv.decode(targetClass, fromDBObject, optionalExtraInfo);
			return new Character((char)i.intValue());
		}
		
		@Override
		public Object encode(Object value, MappedField optionalExtraInfo) {
			Character c = (Character)value;
			return (int)c.charValue();
		}
	}
	
	@Converters(CharacterToByteConverter.class)
	static class CharEntity {
		@Id ObjectId id = new ObjectId();
		Character c = 'a';
	}

	@Test
	public void testIt() {
		morphia.map(CharEntity.class);
		
		ds.save(new CharEntity());
		CharEntity ce = ds.find(CharEntity.class).get();
		Assert.assertNotNull(ce.c);
		Assert.assertEquals(ce.c.charValue() , 'a');
		
		BasicDBObject dbObj = (BasicDBObject) ds.getCollection(CharEntity.class).findOne();
		Assert.assertTrue(dbObj.getInt("c") == (int)'a');
	}
}

