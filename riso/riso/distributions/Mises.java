/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999-2001, Robert Dodier.
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
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represent a von Mises distribution,
  * also called a circular Gaussian distribution.
  */
public class Mises extends AbstractDistribution
{
	/** The location parameter of this distribution. 
	  * Don't modify this parameter -- create a new instance if a different parameter is needed.
	  */
	public double a;

	/** The scale parameter of this distribution.
	  * Don't modify this parameter -- create a new instance if a different parameter is needed.
	  */
	public double b;

	/** Construct an instance with default parameters, namely <tt>a==0</tt> and <tt>b==1</tt>.
	  */
	public Mises() {}

	/** Constructs a lognormal with the specified parameters.
	  */
	public Mises( double a, double b )
	{
		this.a = a;
		this.b = b;
	}

	/** Returns the number of dimensions in which this distribution lives.
	  * This number is always 1.
	  */
	public int ndimensions() { return 1; }

	/** Compute the density at the point <code>x</code>.
	  * Density function given by Weisstein,
	  * <a href="http://mathworld.wolfram.com/vonMisesDistribution.html">``Von Mises Distribution.''</a>
	  * @param x Point at which to evaluate density; this is an array
	  *   of length 1.
	  */
	public double p( double[] x )
	{
		return Math.exp( b * Math.cos(x[0]-a) )/( 2 * Math.PI * SpecialMath.mBesselFirstZero(b) );
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Mises.log_prior: not implemented." );
	}

	/** Returns an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "Mises.random: not implemented." );
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
		throw new Exception( "Mises.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value()
	{
		return a;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance()
	{
		// double var = 1 - Ia(b)/I0(b);
		// return Math.sqrt(var);
		throw new Exception( "Mises.sqrt_variance: not implemented." );
	}

	/** Returns an interval which contains almost all of the mass of this 
	  * distribution. 
	  * ALWAYS RETURNS [0,2 PI] !!! WE CAN DO BETTER !!!
	  *
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double[] support = new double[2];
		support[0] = 0;
		support[1] = 2*Math.PI;

		return support;
	}

	/** Formats a string representation of this distribution.
	  * Since the representation is only one line of output, 
	  * the argument <tt>leading_ws</tt> is ignored.
	  */
	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass()+" { ";
		result += "a "+a+"  b "+b;
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
				throw new IOException( "Mises.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "a" ) )
				{
					st.nextToken();
					a = Format.atof( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "b" ) )
				{
					st.nextToken();
					b = Format.atof( st.sval );
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
			throw new IOException( "Mises.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Mises.pretty_input: no closing bracket on input." );
	}
}
