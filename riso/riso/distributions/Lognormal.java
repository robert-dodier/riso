package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

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

		associated_gaussian = new Gaussian( mu, sigma );
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

	/** Formats a string representation of this distribution.
	  * Since the representation is only one line of output, 
	  * the argument <tt>leading_ws</tt> is ignored.
	  */
	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+" { ";
		result += "mu "+mu+"  sigma "+sigma;
		result += " }"+"\n";
		return result;
	}

	/** Parse a string containing a description of an instance of this distribution.
	  * The description is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Write an instance of this distribution to an output stream.
	  *
	  * @param os The output stream to print on.
	  * @param leading_ws Since the representation is only one line of output, 
	  *   this argument is ignored.
	  * @throws IOException If the output fails; this is possible, but unlikely.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Lognormal.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "mu" ) )
				{
					st.nextToken();
					mu = Format.atof( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "sigma" ) )
				{
					st.nextToken();
					sigma = Format.atof( st.sval );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "Lognormal.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Lognormal.pretty_input: no closing bracket on input." );

		associated_gaussian = new Gaussian( mu, sigma );
	}
}
