/**
 * 
 */
package com.google.code.morphia.mapping.lazy;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class LazyFeatureDependencies {
	
	private LazyFeatureDependencies() {
		
	}
	
	public static boolean fullFilled() {
		try {
			return Class.forName("net.sf.cglib.proxy.Enhancer") != null
					&& Class.forName("com.thoughtworks.proxy.toys.hotswap.HotSwapping") != null;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
