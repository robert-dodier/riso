package riso.distributions;
import java.io.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class approximates a density function by a spline.
  */
public class SplineDensity extends AbstractDistribution
{
	protected boolean expected_value_OK = false, sqrt_variance_OK = false;
	protected double expected_value_result, sqrt_variance_result;

	protected static double sqr( double x )  { return x*x; }

	/** The spline function.
	  */
	public MonotoneSpline spline = null;

	/** Construct a density approximation from the specified list of pairs
	  * <tt>(x,p(x))</tt>. Fudge the spline parameters so that this spline density
	  * integrates to unity.
	  */
	public SplineDensity( double[] x, double[] px ) throws Exception
	{
		spline = new MonotoneSpline( x, px );
		double total = cdf0( x[ x.length-1 ] );

		for ( int i = 0; i < x.length; i++ )
		{
			spline.f[i] /= total;
			spline.d[i] /= total;
			spline.alpha2[i] /= total;
			spline.alpha3[i] /= total;
		}
System.err.println( "SplineDensity: total: "+total+", cdf (should be 1): "+cdf0(x[x.length-1]) );
	}

	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Computes the density at the point <code>x</code>. If <tt>x</tt> is outside the
	  * support of the spline, return zero.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x ) throws Exception
	{
		if ( x[0] < spline.x[0] || x[0] > spline.x[spline.x.length-1] ) return 0;

		return spline.f( x[0] );
	}

	/** Compute the cumulative distribution function.
	  * If <tt>x</tt> is to the right of the support of this spline,
	  * skip the computation and return 0; to the left, return 1.
	  */
	public double cdf( double x ) throws Exception
	{
		if ( x <= spline.x[0] ) return 0;
		if ( x >= spline.x[ spline.x.length-1 ] ) return 1;

		return cdf0(x);
	}

	/** Compute the cumulative distribution function.
	  */
	public double cdf0( double x ) throws Exception
	{
		double sum = 0;
		int i;

		// Do complete intervals first. Could cache these results; hmm.
		for ( i = 0; spline.x[i+1] < x; i++ )
		{
			double dx  = spline.x[i+1] - spline.x[i];
			double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3;

			double term = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
			sum += term;
		}

		// Now do the last partial interval.
		double dx  = x - spline.x[i];
		double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3;

		double term = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
		sum += term;

		return sum;
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This method is not implemented.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "SplineDensity.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This method is not implemented.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "SplineDensity.random: not implemented." );
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "SplineDensity.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() 
	{
		// Retrieve the cached result, if possible. Otherwise we need to compute it.

		if ( ! expected_value_OK )
		{
			double sum = 0;
			for ( int i = 0; i < spline.x.length-1; i++ )
			{
				double dx = spline.x[i+1] - spline.x[i];
				double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3, dx5 = dx*dx4;

				double term1 = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
				double term2 = dx2*spline.f[i]/2 + dx3*spline.d[i]/6 + dx4*spline.alpha2[i]/12 + dx5*spline.alpha3[i]/20;
				sum += spline.x[i+1]*term1 - term2;
			}

			expected_value_result = sum;
			expected_value_OK = true;
		}

		return expected_value_result;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance()
	{
		// Retrieve the cached result, if possible. Otherwise we need to compute it.

		if ( ! sqrt_variance_OK )
		{
			double sum = 0, sum2 = 0;
			for ( int i = 0; i < spline.x.length-1; i++ )
			{
				double dx = spline.x[i+1] - spline.x[i];
				double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3, dx5 = dx*dx4, dx6 = dx*dx5;

				double term1 = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
				double term2 = dx2*spline.f[i]/2 + dx3*spline.d[i]/6 + dx4*spline.alpha2[i]/12 + dx5*spline.alpha3[i]/20;
				double term3 = dx3*spline.f[i]/6 + dx4*spline.d[i]/24 + dx5*spline.alpha2[i]/60 + dx6*spline.alpha3[i]/120;

				sum  += spline.x[i+1]*term1 - term2;
				sum2 += sqr(spline.x[i+1])*term1 - 2*spline.x[i+1]*term2 + 2*term3;
			}

			sqrt_variance_result = Math.sqrt( sum2 - sum*sum );
			sqrt_variance_OK = true;
		}

		return sqrt_variance_result;
	}

	/** Returns the support of the spline.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double[] support = new double[2];
		support[0] = spline.x[0];
		support[1] = spline.x[ spline.x.length-1 ];
		return support;
	}

	/** Formats the parameters of the spline into a string.
	  */
	public String format_string( String leading_ws )
	{
		String result = "", more_ws = leading_ws+"\t";
		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		result += more_ws+"% x\tf\td\talpha2\talpha3; "+(spline.x.length-1)+" intervals\n";
		for ( int i = 0; i < spline.x.length; i++ )
			result += more_ws+spline.x[i]+"\t"+spline.f[i]+"\t"+spline.d[i]+"\t"+spline.alpha2[i]+"\t"+spline.alpha3[i]+"\n";
		result += "}"+"\n";
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
	  * This method is not implemented.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		throw new IOException( "SplineDensity.pretty_input: not implemented." );
	}
}
