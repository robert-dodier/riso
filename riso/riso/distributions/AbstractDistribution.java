package riso.distributions;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import riso.approximation.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** Abstract base class for unconditional distributions.
  * Since Distribution is derived from ConditionalDistribution, any
  * unconditional distribution must implement all of the functions defined
  * for conditional distributions. Some of these have trivial implementations,
  * which are given here. It would be cleaner, perhaps, to put these in
  * the definition of Distribution, but Java doesn't allow code in an
  * interface... <sigh>. So here they are.
  */
abstract public class AbstractDistribution implements Distribution, Serializable
{
	/** This distribution is associated with the belief network variable <tt>associated_variable</tt>.
	  * This reference is necessary for some distributions, and generally useful for debugging.
	  */
	public AbstractVariable associated_variable;

	/** Cache a reference to the variable with which this distribution
	  * is associated.
	  */
	public void set_variable( Variable x ) { associated_variable = x; }

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
	public double p( double[] x, double[] c ) throws Exception { return p(x); }

	/** Return an instance of a random variable from this distribution.
	  * Ignore the context <tt>c</tt>.
	  */
	public double[] random( double[] c ) throws Exception { return random(); }

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( getClass()+".log_prior: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  */
	public Object remote_clone() throws CloneNotSupportedException 
	{
		throw new CloneNotSupportedException( getClass()+".remote_clone: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception Exception
	  */
	public int ndimensions()
	{
		throw new RuntimeException( getClass()+".ndimensions: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception Exception
	  */
	public double[] random() throws Exception
	{
		throw new Exception( getClass()+".random: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception Exception
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( getClass()+".update: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception IOException
	  */
	public void parse_string( String description ) throws IOException
	{
		throw new IOException( getClass()+".parse_string: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception IOException
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		throw new IOException( getClass()+".pretty_input: not implemented." );
	}

	/** Print the class name.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Put the class name into a string.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		return getClass()+"\n";
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws Exception
	{
		throw new Exception( getClass()+".expected_value: not implemented." );
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception
	{
		throw new Exception( getClass()+".sqrt_variance: not implemented." );
	}

	/** Returns the support of this distribution, if it is a finite interval;
	  * otherwise returns an interval which contains almost all of the mass.
	  * @param epsilon If an approximation is made, this much mass or less
	  *   lies outside the interval which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		throw new Exception( getClass()+".effective_support: not implemented." );
	}

	/** This method simply calls <tt>GaussianMixApproximation.</tt>
	  * <tt>initial_mix</tt> to obtain a generic approximation; a derived
	  * class should override this method if a more specific approximation
	  * is needed.
	  *
	  * @param support This argument is ignored.
	  * @see Distribution.initial_mix
	  * @see GaussianMixApproximation.initial_mix
	  */
	public MixGaussians initial_mix( double[] support ) throws Exception
	{
		return GaussianMixApproximation.initial_mix( this );
	}
}
