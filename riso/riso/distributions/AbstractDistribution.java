package riso.distributions;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import SmarterTokenizer;

/** Abstract base class for unconditional distributions.
  * Since Distribution is derived from ConditionalDistribution, any
  * unconditional distribution must implement all of the functions defined
  * for conditional distributions. Some of these have trivial implementations,
  * which are given here. It would be cleaner, perhaps, to put these in
  * the definition of Distribution, but Java doesn't allow code in an
  * interface... <sigh>. So here they are.
  */
abstract public class AbstractDistribution extends UnicastRemoteObject implements Distribution
{
	/** Default constructor for this class just calls super().
	  * It's declared here to show that it can throw a remote exception.
	  */
	public AbstractDistribution() throws RemoteException { super(); }

	/** The "child" in this case is just the variable itself.
	  * So <tt>ndimensions_child</tt> equals <tt>ndimensions</tt>.
	  */
	public int ndimensions_child() throws RemoteException { return ndimensions(); }

	/** There are no parents, so return zero.
	  */
	public int ndimensions_parent() throws RemoteException { return 0; }

	/** Context doesn't matter since this distribution is unconditional.
	  * Return <tt>this</tt> for every value of <tt>c</tt>.
	  */
	public Distribution get_density( double[] c ) throws RemoteException { return this; }

	/** Compute the density at the point <code>x</code>.
	  * Ignore the context <tt>c</tt>.
	  */
	public double p( double[] x, double[] c ) throws RemoteException { return p(x); }

	/** Return an instance of a random variable from this distribution.
	  * Ignore the context <tt>c</tt>.
	  */
	public double[] random( double[] c ) throws RemoteException { return random(); }

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws RemoteException
	{
		throw new RemoteException( getClass().getName()+".log_prior: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception RemoteException
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		throw new RemoteException( getClass().getName()+".remote_clone: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception RemoteException
	  */
	public int ndimensions() throws RemoteException
	{
		throw new RemoteException( getClass().getName()+".ndimensions: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception RemoteException
	  */
	public double[] random() throws RemoteException
	{
		throw new RemoteException( getClass().getName()+".random: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception RemoteException
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception, RemoteException
	{
		throw new RemoteException( getClass().getName()+".update: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception RemoteException
	  */
	public void parse_string( String description ) throws IOException
	{
		throw new RemoteException( getClass().getName()+".parse_string: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception RemoteException
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException, RemoteException
	{
		throw new RemoteException( getClass().getName()+".pretty_input: not implemented." );
	}

	/** Print the class name.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException, RemoteException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Put the class name into a string.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		return getClass().getName()+"\n";
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws RemoteException
	{
		throw new RemoteException( getClass().getName()+".expected_value: not implemented." );
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws RemoteException
	{
		throw new RemoteException( getClass().getName()+".sqrt_variance: not implemented." );
	}

	/** Returns the support of this distribution, if it is a finite interval;
	  * otherwise returns an interval which contains almost all of the mass.
	  * @param epsilon If an approximation is made, this much mass or less
	  *   lies outside the interval which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws RemoteException
	{
		throw new RemoteException( getClass().getName()+".effective_support: not implemented." );
	}
}
