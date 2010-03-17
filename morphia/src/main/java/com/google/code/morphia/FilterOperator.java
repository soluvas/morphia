package com.google.code.morphia;

/**
 * 
 * @author Scott Hernandez
 *
 */
public enum FilterOperator {
	EQUAL("$eq"),
	GREATER_THAN("$gt"),
	GREATER_THAN_OR_EQUAL("$gte"),
	LESS_THAN("$lt"),
	LESS_THAN_OR_EQUAL("$lte"),
	EXISTS("$exists"),
	TYPE("$type"),
	NOT("$not"),
	MOD("$mod"),
	SIZE("$size"),
	IN("$in"),
	NOT_IN("$nin"),
	ALL("$all"),
	NOT_EQUAL("$ne");
	
	private String value;
	private FilterOperator(String val) {
		value = val;
	}
	
	private boolean equals(String val) {
		return value.equals(val);
	}

	public String val() { return value;}
	
	public static FilterOperator fromString(String val) {
		for (int i = 0; i < values().length; i++) {
			FilterOperator fo = values()[i];
			if(fo.equals(val)) return fo;
		}
		return null;
	}
}