import java.util.*;

public class ObjectCache extends TopDownSplayTree
{
	/** List of objects maintained by this cache. The index of each object
	  * in this list is stored as the value in the splay tree.
	  */
	public Vector object_list;

	/** If the interval containing <tt>x</tt> is this small or
	  * smaller, we can carry out the interpolation.
	  */
	public double close_enough = 1e-1;

	/** Sets the parameters for this function cache.
	  * @param close_enough Pass in -1 to use default value.
	  * @param initial_size Pass in -1 to use default value.
	  */
	public ObjectCache( double close_enough, int initial_size_in )
	{
		int initial_size = 100;
		if ( close_enough > 0 ) this.close_enough = close_enough;
		if ( initial_size_in > 0 ) initial_size = initial_size_in;
		object_list = new Vector( initial_size );
	}

	/** Add the object <tt>value</tt> to the cache with key <tt>key</tt>,
	  * and return <tt>value</tt>. It is assumed that <tt>key</tt> is not
	  * already in the cache.
	  */
	public Object cache_new_value( double key, Object value ) throws Exception
	{
		int value_index = object_list.size();
		object_list.addElement( value );
		insert( key, value_index );
if ( size % 400 == 0 ) System.err.println( "ObjectCache.cache_new_value: size now: "+size );
		return value;
	}

	/** See if <tt>key</tt> has a close or exact match in the cache.
	  * @return <tt>null</tt> if there is no close or exact match.
	  */
	public Object lookup( double key ) throws Exception
	{
		if ( root == null ) return null;
		root = TopDownSplayTree.splay( key, root );

		TopDownSplayTree.TreeNode below, above;

		if ( key == root.key || Math.abs(key-root.key) < close_enough )
		{
			return object_list.elementAt( (int)root.value );
		}
		else if ( key > root.key )
		{
			if ( root.right == null ) return null;
			below = TopDownSplayTree.min( root.right );
			if ( Math.abs(key-below.key) < close_enough )
				return object_list.elementAt( (int)below.value );
			else
				return null;
		}
		else // key < root.key
		{
			if ( root.left == null ) return null;
			above = TopDownSplayTree.max( root.left );
			if ( Math.abs(key-above.key) < close_enough )
				return object_list.elementAt( (int)above.value );
			else
				return null;
		}
	}
}
