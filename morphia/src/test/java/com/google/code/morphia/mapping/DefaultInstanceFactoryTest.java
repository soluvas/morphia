package com.google.code.morphia.mapping;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;

/**
 * The class <code>DefaultInstanceFactoryTest</code> contains tests for the
 * class <code>{@link DefaultInstanceFactory}</code>.
 * 
 * @generatedBy CodePro at 3/20/11 10:25 PM
 * @author doc
 * @version $Revision: 1.0 $
 */
public class DefaultInstanceFactoryTest extends TestBase
{

    @Test
    public void testDefaultInstanceFactory() throws Exception
    {
        final DefaultInstanceFactory defaultInstanceFactory = new DefaultInstanceFactory();
        final E1 e1 = defaultInstanceFactory.newInstance(E1.class);
        Assert.assertTrue(e1 instanceof E1);
		final E2 e2 = defaultInstanceFactory.newInstance(E2.class);
		Assert.assertTrue(e2 instanceof E2);
    }
}

class E1
{
    public E1()
    {
    }
}

class E2
{

    private final String n;

    public E2(final String n)
    {
        this.n = n;
    }

}