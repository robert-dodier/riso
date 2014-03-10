/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.numerical.*;
import riso.general.*;

/** An instance of this class represent a log-normal distribution, that is,
  * the logarithm of a log-normal variable has a normal (Gaussian)
  * distribution.
  */
public class Lognormal extends AbstractDistribution
{
	/** The location parameter of this distribution. This parameter
	  * is equal to the median of the distribution. Don't modify this parameter -- create a
	  * new <tt>Lognormal</tt> if different parameters are needed.
	  */
	public double mu;

	/** The scale parameter of this distribution. Don't modify this parameter -- create a
	  * new <tt>Lognormal</tt> if different parameters are needed.
	  */
	public double sigma;

	/** The Gaussian distribution which has the same parameters as
	  * this lognormal. Don't modify this parameter -- create a
	  * new <tt>Lognormal</tt> if different parameters are needed.
	  */
	public Gaussian associated_gaussian;

	/** Construct a lognormal with unspecified parameters.
	  */
	public Lognormal() {}

	/** Constructs a lognormal with the specified parameters.
	  */
	public Lognormal( double mu, double sigma )
	{
		this.mu = mu;
		this.sigma = sigma;

		associated_gaussian = new Gaussian( mu, sigma );
	}

	/** Returns the number of dimensions in which this distribution lives.
	  * This number is always 1.
	  */
	public int ndimensions() { return 1; }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density; this is an array
	  *   of length 1.
	  */
	public double p( double[] x )
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
	public double log_prior() throws Exception
	{
		throw new Exception( "Lognormal.log_prior: not implemented." );
	}

	/** Returns an instance of a random variable from this distribution.
	  * An instance is generated from the Gaussian with the same parameters
	  * as this lognormal, and we take the exponential of that.
	  */
	public double[] random() throws Exception
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
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Lognormal.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value()
	{
		return Math.exp( mu + sigma*sigma/2 );
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance()
	{
		return expected_value() * Math.sqrt( Math.exp( sigma*sigma ) - 1 );
	}

	/** Returns an interval which contains almost all of the mass of this 
	  * distribution. The left end of the interval is at zero, and beyond
	  * the right end is a mass less than or equal to <tt>epsilon</tt>.
	  *
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
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
					mu = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "sigma" ) )
				{
					st.nextToken();
					sigma = Double.parseDouble( st.sval );
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
