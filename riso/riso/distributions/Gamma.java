package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;

/** An instance of this class represents a gamma distribution.
  */
public class Gamma extends AbstractDistribution
{
	/** Normalization parameter for this distribution.
	  * This is a function of the shape and scale parameters.
	  */
	protected double Z;

	/** The ``shape'' parameter of this distribution.
	  */
	protected double alpha;

	/** The ``scale'' parameter of this distribution.
	  */
	protected double beta;

	/** Constructs a gamma distribution with the specified parameters.
	  */
	public Gamma( double alpha, double beta ) throws RemoteException
	{
		this.alpha = alpha;
		this.beta = beta;
		Z = Math.exp( SpecialMath.logGamma(alpha) + alpha*Math.log(beta) );
	}

	/** Default constructor for this class.
	  * It's declared here to show that it can throw a remote exception.
	  */
	public Gamma() throws RemoteException {}

	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() throws RemoteException { return 1; }

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x ) throws RemoteException
	{
		if ( x[0] <= 0 ) return 0;

		return (1/Z) * Math.exp( (alpha-1)*Math.log(x[0]) - x[0]/beta );
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws RemoteException
	{
		throw new RemoteException( "Gamma.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This method is not implemented.
	  */
	public double[] random() throws RemoteException
	{
		throw new RemoteException( "Gamma.random: not implemented." );
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception, RemoteException
	{
		throw new RemoteException( "Gamma.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  * This is equal to the product of the shape and scale parameters.
	  */
	public double expected_value() throws RemoteException
	{
		return alpha*beta;
	}

	/** Returns the square root of the variance of this distribution.
	  * This is equal to the scale parameter times the square root of
	  * the shape parameter.
	  */
	public double sqrt_variance() throws RemoteException
	{
		return beta * Math.sqrt( alpha );
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution; uses a numerical search to find <tt>x</tt> such that
	  * the tail mass to the right of <tt>x</tt> is less than <tt>epsilon</tt>.
	  *
	  * @param epsilon This much mass or less lies outside the interval
	  *   which is returned.
	  * @return An interval represented as a 2-element array; element 0 is
	  *   zero, and element 1 is <tt>x</tt>, as defined above.
	  */
	public double[] effective_support( double epsilon ) throws RemoteException
	{
		// Use bisection search to find small interval containing x
		// such that F(x) < epsilon, then take x as the
		// right end of that interval -- the resulting [0,x] will
		// be a little bit too wide, but that's OK.

		double z0 = 0, z1 = 10*alpha;

		// First make sure z1 is beyond the required point.

		for ( double Fz1 = SpecialMath.iGamma( alpha, z1/beta ); Fz1 < 1-epsilon; z1 *= 2 )
			;

System.err.println( "Gamma.effective_support: initial z1: "+z1 );
		while ( z1 - z0 > 0.25 )
		{
			double zm = z0 + (z1-z0)/2;
			double Fm = SpecialMath.iGamma( alpha, zm/beta );
			if ( Fm > 1-epsilon )
				z1 = zm;
			else 
				z0 = zm;
System.err.println( "Gamma.effective_support: z0: "+z0+" zm: "+zm+" z1: "+z1+" Fm: "+Fm );
		}

System.err.println( "Gamma.effective_support: epsilon: "+epsilon+" z1: "+z1 );

		double[] interval = new double[2];
		interval[0] = 0;
		interval[1] = z1;
		return interval;
	}
}
