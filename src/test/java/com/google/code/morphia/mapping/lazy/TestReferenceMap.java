package com.google.code.morphia.mapping.lazy;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestReferenceMap extends ProxyTestBase
{
	
	public final void testCreateProxy()
	{
		A a = new A();
		B b1 = new B();
		B b2 = new B();
		
		a.bs.put("b1", b1);
		a.bs.put("b1+", b1);
		a.bs.put("b2", b2);
		
		ds.save(b2, b1, a);
		a = ds.get(a);
		
		assertIsProxy(a.bs);
		assertNotFetched(a.bs);
		assertEquals(3, a.bs.size());
		assertFetched(a.bs);
		
		B b1read = a.bs.get("b1");
		assertNotNull(b1read);
		
		assertEquals(b1, a.bs.get("b1"));
		assertEquals(b1, a.bs.get("b1+"));
		// currently fails:
		// assertSame(a.bs.get("b1"), a.bs.get("b1+"));
		assertNotNull(a.bs.get("b2"));
		
		a = deserialize(a);
		assertNotFetched(a.bs);
		assertEquals(3, a.bs.size());
		assertFetched(a.bs);
		assertEquals(b1, a.bs.get("b1"));
		assertEquals(b1, a.bs.get("b1+"));
		assertNotNull(a.bs.get("b2"));
		
		// make sure, saving does not fetch
		a = deserialize(a);
		assertNotFetched(a.bs);
		ds.save(a);
		assertNotFetched(a.bs);
	}
	
	
	
	public static class A extends AbstractMongoEntity
	{
		@Reference(lazy = true)
		Map<String, B> bs = new HashMap<String, B>();
	}
	
	public static class B
	{
		@Id
		private String id = new ObjectId().toStringMongod();
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			B other = (B) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}
		
	}
	
}
