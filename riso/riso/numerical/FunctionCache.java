package numerical;
import TopDownSplayTree;

public class FunctionCache extends TopDownSplayTree
{
	/** If the interval containing <tt>x</tt> is this small or
	  * smaller, we can carry out the interpolation.
	  */
	public double close_enough = 1e-1;

	/** If the estimated error from the interpolation is greater than this,
	  * then reject the interpolated value and compute a new one the hard way.
	  * IGNORED AT PRESENT; WILL USE WITH RATIONAL INTERPOLATION SCHEME !!!
	  */
	public double error_tolerance = 1e-4;

	/** This is the function cached by this object.
	  */
	public Callback_1d target;

	/** Sets the parameters for this function cache.
	  * @param close_enough Pass in -1 to use default value.
	  * @param error_tolerance Pass in -1 to use default value.
	  * @param target The function to approximate.
	  */
	public FunctionCache( double close_enough, double error_tolerance, Callback_1d target )
	{
		if ( close_enough > 0 ) this.close_enough = close_enough;
		if ( close_enough > 0 ) this.error_tolerance = error_tolerance;
		this.target = target;
	}

	/** Compute a new function value, cache it, and return it.
	  */
	public double cache_new_value( double x ) throws Exception
	{
		double fx = target.f( x );
		insert( x, fx );
System.err.println( "FunctionCache.cache_new_value: x: "+x+" fx: "+fx );
		return fx;
	}

	/** See if we can generate a value by interpolation;
	  * failing that, compute the function value, cache it,
	  * and return it.
	  */
	public double lookup( double x ) throws Exception
	{
		if ( root == null ) return cache_new_value( x );

		root = TopDownSplayTree.splay( x, root );
		TopDownSplayTree.TreeNode a, b;

		if ( x > root.key )
		{
			if ( root.right == null ) return cache_new_value( x );

			a = root;
			b = TopDownSplayTree.min( root.right );
		}
		else if ( x < root.key )
		{
			if ( root.left == null ) return cache_new_value( x );

			a = TopDownSplayTree.max( root.left );
			b = root;
		}
		else
		{
			return root.value;
		}

if ( x < a.key || x > b.key ) throw new RuntimeException( "Integral.p: x: "+x+" not in ["+a.key+", "+b.key+"]." );
		double da = x-a.key, dab = b.key-a.key;
if ( dab < 0 ) throw new RuntimeException( "Integral.p: dab: "+dab+" < 0." );

		// If we're in a small interval (which should give us
		// an accurate interpolation) return interpolated value.

		if ( dab < close_enough )
		{
			double interpolated_value = (1-da/dab)*a.value + da/dab*b.value;
			return interpolated_value;
		}
		else
		{
			return cache_new_value( x );
		}
	}
}
