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
	public Gamma( double alpha, double beta )
	{
		this.alpha = alpha;
		this.beta = beta;
		Z = Math.exp( SpecialMath.logGamma(alpha) + alpha*Math.log(beta) );
	}

	/** Default constructor for this class. Sets shape and scale parameters to 1.
	  */
	public Gamma() { alpha = beta = 1; }

	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x )
	{
		if ( x[0] <= 0 ) return 0;

		return (1/Z) * Math.exp( (alpha-1)*Math.log(x[0]) - x[0]/beta );
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Gamma.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This method is not implemented.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "Gamma.random: not implemented." );
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Gamma.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  * This is equal to the product of the shape and scale parameters.
	  */
	public double expected_value() 
	{
		return alpha*beta;
	}

	/** Returns the square root of the variance of this distribution.
	  * This is equal to the scale parameter times the square root of
	  * the shape parameter.
	  */
	public double sqrt_variance()
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
	public double[] effective_support( double epsilon ) throws Exception
	{
		// Use bisection search to find small interval containing x
		// such that F(x) < epsilon, then take x as the
		// right end of that interval -- the resulting [0,x] will
		// be a little bit too wide, but that's OK.

		double z0 = 0, z1 = beta;

		// First make sure z1 is beyond the required point.

		double Fz1;
		do
		{
			z1 *= 2;
			Fz1 = SpecialMath.incompleteGamma( alpha, z1/beta );
// System.err.println( "Gamma.effective_support: search for initial z1; z1: "+z1+" 1-Fz1: "+(1-Fz1) );
		}
		while ( Fz1 < 1-epsilon );

// System.err.println( "Gamma.effective_support: initial z1: "+z1 );
		while ( z1 - z0 > 0.25 )
		{
			double zm = z0 + (z1-z0)/2;
			double Fm = SpecialMath.incompleteGamma( alpha, zm/beta );
			if ( Fm > 1-epsilon )
				z1 = zm;
			else 
				z0 = zm;
// System.err.println( "Gamma.effective_support: z0: "+z0+" zm: "+zm+" z1: "+z1+" Fm: "+Fm );
		}

// System.err.println( "Gamma.effective_support: epsilon: "+epsilon+" z1: "+z1 );

		double[] interval = new double[2];
		interval[0] = 0;
		interval[1] = z1;
		return interval;
	}

	/** Formats a string representation of this distribution.
	  * Since the representation is only one line of output, 
	  * the argument <tt>leading_ws</tt> is ignored.
	  */
	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+" { ";
		result += "alpha "+alpha+"  beta "+beta;
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
				throw new IOException( "Gamma.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "alpha" ) )
				{
					st.nextToken();
					alpha = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "beta" ) )
				{
					st.nextToken();
					beta = Double.parseDouble( st.sval );
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
			throw new IOException( "Gamma.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Gamma.pretty_input: no closing bracket on input." );

		Z = Math.exp( SpecialMath.logGamma(alpha) + alpha*Math.log(beta) );
	}
}
