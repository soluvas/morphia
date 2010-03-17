package com.google.com.morphia.ofy;

/**
 * @author Scott Hernandez
 */
public interface Query<T> extends Iterable<T>
{
	/**
	 * <p>Create a filter based on the specified condition and value. Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code filter("age >=", age)}</li>
	 * <li>{@code filter("age =", age)}</li>
	 * <li>{@code filter("age", age)} (if no operator, = is assumed)</li>
	 * <li>{@code filter("age !=", age)}</li>
	 * <li>{@code filter("age in", ageList)}</li>
	 * <li>{@code filter("customers.loyaltyYears in", yearsList)}</li>
	 * </ul>
	 * 
	 * <p>You can filter on id properties <strong>if</strong> this query is
	 * restricted to a Class<T>.
	 */
	public Query<T> filter(String condition, Object value);
	
	/**
	 * <p>Sorts based on a property.  Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code sort("age")}</li>
	 * <li>{@code sort("-age")} (descending sort)</li>
	 * <li>{@code sort("age,date")}</li>
	 * <li>{@code sort("age,-date")} (age ascending, date descending)</li>
	 * </ul>
	 */
	public Query<T> order(String condition);
	
	/**
	 * Limit the fetched result set to a certain number of values.
	 * 
	 * @param value must be >= 0.  A value of 0 indicates no limit.
	 */
	public Query<T> limit(int value);
	
	/**
	 * Starts the query results at a particular zero-based offset.
	 * 
	 * @param value must be >= 0
	 */
	public Query<T> offset(int value);
	
	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions.</p>
	 * 
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p> 
	 */
	public String toString();
	
	/**
	 * Gets the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the only instance in the result, or null if the result set is empty.
	 */
	public T get();
	
	/**
	 * Get the key of the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the key of the first instance in the result, or null if the result set is empty.
	 */
	public Key<T> getKey();
	
	/**
	 * Execute the query and get the results.  This method is provided for orthogonality;
	 * Query.fetch().iterator() is identical to Query.iterator().
	 */
	public Iterable<T> fetch();
	
	/**
	 * Execute the query and get the ids of the results.  This is more efficient than
	 * fetching the actual results (transfers less data).
	 */
	public Iterable<T> fetchIdsOnly();
		
	/**
	 * <p>Count the total number of values in the result, <strong>ignoring <em>limit</em> and <em>offset</em>.</p>
	 */
	public long countAll();
}