package riso.distributions;
import java.io.*;
import java.rmi.*;

/** An instance of this class represent a log-normal distribution, that is,
  * the logarithm of a log-normal variable has a normal (Gaussian)
  * distribution.
  */
public class Lognormal extends AbstractDistribution
{
	/** The location parameter of this distribution. This parameter
	  * is equal to the median of the distribution.
	  */
	protected double mu;

	/** The scale parameter of this distribution.
	  */
	protected double sigma;

	/** The Gaussian distribution which has the same parameters as
	  * this lognormal.
	  */
	protected Gaussian associated_gaussian;

	/** Constructs a lognormal with the specified parameters.
	  */
	public Lognormal( double mu, double sigma ) throws RemoteException
	{
		this.mu = mu;
		this.sigma = sigma;
		double[] mu1 = new double[1];

		double[][] Sigma1 = new double[1][1];
		mu1[0] = mu;
		Sigma1[0][0] = sigma*sigma;

		associated_gaussian = new Gaussian( mu1, Sigma1 );
	}

    /** Default constructor for this class.
      * It's declared here to show that it can throw a remote exception.
      */
	public Lognormal() throws RemoteException {}

	/** Returns the number of dimensions in which this distribution lives.
	  * This number is always 1.
	  */
	public int ndimensions() throws RemoteException { return 1; }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density; this is an array
	  *   of length 1.
	  */
	public double p( double[] x ) throws RemoteException
	{
		// Density function given by Papoulis, Probability, Random Variables,
		// and Stochastic Processes (1984), Eq. 5-10.

		if ( x[0] <= 0 ) return 0;

		final double SQRT_2PI = Math.sqrt( 2 * Math.PI );
		double z = (Math.log(x[0]) - mu)/sigma;
		return Math.exp( -z*z/2 ) / sigma / x[0] / SQRT_2PI;
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful.
	  */
	public double log_prior() throws RemoteException
	{
		throw new RemoteException( "Lognormal.log_prior: not implemented." );
	}

	/** Returns an instance of a random variable from this distribution.
	  * An instance is generated from the Gaussian with the same parameters
	  * as this lognormal, and we take the exponential of that.
	  */
	public double[] random() throws RemoteException
	{
		double[] x = associated_gaussian.random();
		x[0] = Math.exp( x[0] );
		return x;
	}

	/** Uses data to modify the parameters of the distribution.
	  * This method is not yet implemented.
	  *
	  * @param x The data. Each row has one component, and the number of
	  *   rows is equal to the number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this distribution produced the corresponding datum <code>x[i]</code>.
	  * @param niter_max Maximum number of iterations of the update algorithm,
	  *   if applicable.
	  * @param stopping_criterion A number which describes when to stop the
	  *   update algorithm, if applicable.
	  * @return Negative log-likelihood at end of update.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception, RemoteException
	{
		throw new RemoteException( "Lognormal.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws RemoteException
	{
		return Math.exp( mu + sigma*sigma/2 );
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws RemoteException
	{
		return expected_value() * Math.sqrt( Math.exp( sigma*sigma ) - 1 );
	}

	/** Returns an interval which contains almost all of the mass of this 
	  * distribution. The left end of the interval is at zero, and beyond
	  * the right end is a mass less than or equal to <tt>epsilon</tt>.
	  *
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws RemoteException
	{
		// Use Gaussian to find two-tailed approximate support.
		// We'll use a one-sided tail for the lognormal.

		double[] g_support = associated_gaussian.effective_support( 2*epsilon );

		double[] support = new double[2];
		support[0] = 0;
		support[1] = Math.exp( g_support[1] );

		return support;
	}
}
