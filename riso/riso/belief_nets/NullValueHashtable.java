package risotto.belief_nets;

import java.util.*;

/** This class extends <tt>Hashtable</tt> to allow one to store
  * null values in the hash table.
  */
public class NullValueHashtable extends Hashtable
{
	public static final Object NULL_VALUE = new Object();

	/** This constructor just calls the corresponding constructor of <tt>Hashtable</tt>.
	  * @see java.util.Hashtable#Hashtable(int initialCapacity, float loadFactor)
	  */
	public NullValueHashtable( int initialCapacity, float loadFactor )
	{
		super( initialCapacity, loadFactor );
	}

	/** This constructor just calls the corresponding constructor of <tt>Hashtable</tt>.
	  * @see java.util.Hashtable#Hashtable(int initialCapacity)
	  */
	public NullValueHashtable( int initialCapacity ) { super( initialCapacity ); }

	/** This constructor just calls the corresponding constructor of <tt>Hashtable</tt>.
	  * @see java.util.Hashtable#Hashtable()
	  */
	public NullValueHashtable() { super(); }
	
	/** This method returns an enumeration of the values in this hashtable.
	  * Null values in the enumeration are represented by references to 
	  * <tt>NullValueHashtable.NULL_VALUE</tt>.
	  * @see java.util.Hashtable#elements()
	  */
	public synchronized Enumeration elements() { return super.elements(); }

	/** Tests if some key maps into the specified value in this hashtable.
	  * A test for a null value can be carried out with <tt>value == null</tt>
	  * or <tt>value == NULL_VALUE</tt>; it works either way.
	  * @see java.util.Hashtable#contains(Object value)
	  */
	public synchronized boolean contains(Object value)
	{
		if ( value == null ) value = NULL_VALUE;
		return super.contains(value);
	}

	/** Returns the value to which the specified key is mapped in this hashtable.
	  * Note that the return value differs from that of <tt>Hashtable.get</tt>!
	  * @return The value to which the key maps; this may be <tt>null</tt>.
	  * @throws NoSuchElementException If the key does not map to any value.
	  * @see java.util.Hashtable#get(Object key)
	  */
	public synchronized Object get( Object key )
	{
		Object value = super.get(key);
		if ( value == NULL_VALUE )
			return null;
		else if ( value == null )
			throw new NoSuchElementException( "NullValueHashtable.get" );
		else
			return value;
	}

	/** This method maps <tt>key</tt> to <tt>value</tt>; the latter may
	  * be <tt>null</tt>, but not the former.
	  * @return The previous value to which the key was mapped; the return value
	  *   is <tt>null</tt> if the key wasn't mapped to any value, or it is <tt>NULL_VALUE</tt>
	  *   if it was previously mapped to <tt>null</tt>. Yes, this is very ugly!
	  * @throws NullPointerException If the key is null.
	  * @see java.util.Hashtable#put(Object key, Object value)
	  */
	public synchronized Object put( Object key, Object value )
	{
		if ( value == null ) value = NULL_VALUE;
		return super.put( key, value );
	}
}
