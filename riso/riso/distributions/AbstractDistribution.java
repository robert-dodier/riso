package distributions;
import java.io.*;

/** Abstract base class for unconditional distributions.
  * Since Distribution is derived from ConditionalDistribution, any
  * unconditional distribution must implement all of the functions defined
  * for conditional distributions. Some of these have trivial implementations,
  * which are given here. It would be cleaner, perhaps, to put these in
  * the definition of Distribution, but Java doesn't allow code in an
  * interface... <sigh>. So here they are.
  */
abstract public class AbstractDistribution implements Distribution
{
	/** WHY DO I HAVE TO PUT THIS HERE???
	  */
	abstract public Object clone() throws CloneNotSupportedException;

	/** The "child" in this case is just the variable itself.
	  * So <tt>ndimensions_child</tt> equals <tt>ndimensions</tt>.
	  */
	public int ndimensions_child() { return ndimensions(); }

	/** There are no parents, so return zero.
	  */
	public int ndimensions_parent() { return 0; }

	/** Context doesn't matter since this distribution is unconditional.
	  * Return <tt>this</tt> for every value of <tt>c</tt>.
	  */
	public Distribution get_density( double[] c ) { return this; }

	/** Compute the density at the point <code>x</code>.
	  * Ignore the context <tt>c</tt>.
	  */
	public double p( double[] x, double[] c ) { return p(x); }

	/** Return an instance of a random variable from this distribution.
	  * Ignore the context <tt>c</tt>.
	  */
	public double[] random( double[] c ) { return random(); }
}
