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
import riso.numerical.*;
import riso.general.*;

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

	/** Compute the density at the point <tt>x</tt>.
	  * Density function given by Weisstein,
	  * <a href="http://mathworld.wolfram.com/vonMisesDistribution.html">``Von Mises Distribution.''</a>
	  * @param x Point at which to evaluate density; this is an array
	  *   of length 1.
	  */
	public double p( double[] x )
	{
		return Math.exp( b * Math.cos(x[0]-a) )/( 2 * Math.PI * SpecialMath.modBesselFirstZero(b) );
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
	  * This method implements the maximum likelihood formulas worked out
	  * in hand-written notes, 8 Dec 2001.
	  * Let <tt>theta[i]</tt> be a list of angles. Let <tt>S = \sum_i sin theta[i]</tt>.
	  * Let <tt>C = \sum_i cos theta[i]</tt>. Let <tt>R^2 = S^2 + C^2</tt>.
	  * Then the maximum likelihood estimate of the parameter <tt>a</tt> is
	  * <tt>atan2(S,C)</tt>, and the m.l. estimate of <tt>b</tt> is a solution 
	  * of <tt>(sin \hat a)/S R^2/n = I1(b)/I0(b)</tt>, where <tt>n</tt> is
	  * the number of data and <tt>I0, I1</tt> are the modified Bessel functions
	  * of the first kind and orders zero and one, respectively.
	  *
	  * @param theta The data. Each row has one component, and the number of
	  *   rows is equal to the number of data.
	  * @param responsibility Each component of this vector 
	  *   <tt>responsibility[i]</tt> is a scalar telling the probability
	  *   that this distribution produced the corresponding datum <tt>theta[i]</tt>.
	  * @param niter_max Maximum number of iterations of the update algorithm,
	  *   if applicable.
	  * @param stopping_criterion A number which describes when to stop the
	  *   update algorithm, if applicable. In this method, the maximum likelihood
	  *   estimate of <tt>b</tt> is found by solving an equation to the precision
	  *   given by <tt>stopping_criterion</tt>.
	  * @return Negative log-likelihood at end of update.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  */
	public double update( double[][] theta, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		double S = 0, C = 0, n = 0;

		if ( responsibility == null )
			for ( int i = 0; i < theta.length; i++ )
			{
				S += Math.sin(theta[i][0]); 
				C += Math.cos(theta[i][0]); 
				n += 1;
			}
		else
			for ( int i = 0; i < theta.length; i++ )
			{
				S += responsibility[i] * Math.sin(theta[i][0]); 
				C += responsibility[i] * Math.cos(theta[i][0]); 
				n += responsibility[i];
			}

		a = Math.atan2( S, C );

		double R2 = C*C+S*S, lhs = (Math.sin(a)/S) * (R2/n);

		double b_lo = 0, b_hi = 1;
		while ( I10_ratio(b_hi) < lhs && b_hi < Double.POSITIVE_INFINITY )
		{
			b_lo = b_hi;
			b_hi *= 2;
		}

		if ( I10_ratio(b_hi) < lhs )
		{
			b = Double.POSITIVE_INFINITY;
			System.err.println( "Mises.update: variance is apparently very small; set b to "+b+" and stagger forward." );
			return weighted_nll( theta, responsibility );
		}

		int niter = 0;

		// Set default values if necessary.
		if ( niter_max == -1 ) niter_max = 50;
		if ( stopping_criterion == -1 ) stopping_criterion = 1e-4;

		while ( b_hi - b_lo > stopping_criterion && niter++ < niter_max )
		{
			b = b_lo + (b_hi - b_lo)/2;
			
			if ( I10_ratio(b) > lhs )
				b_hi = b;
			else 
				b_lo = b;
		}

		return weighted_nll( theta, responsibility );
	}

	/** Computes the ratio <tt>I1(u)/I0(u)</tt> for the argument <tt>u</tt>.
	  */
	public static double I10_ratio( double u )
	{
		return SpecialMath.modBesselFirstOne(u)/SpecialMath.modBesselFirstZero(u);
	}

	/** Computes the negative log likelihood, weighting each case by the responsibility.
	  */
	public double weighted_nll( double[][] theta, double[] responsibility )
	{
		double log_denom = 0, n = 0;

		if ( responsibility == null )
			for ( int i = 0; i < theta.length; i++ )
			{
				log_denom += b * Math.cos( theta[i][0] - a );
				n += 1;
			}
		else
			for ( int i = 0; i < theta.length; i++ )
			{
				log_denom += responsibility[i] * b * Math.cos( theta[i][0] - a );
				n += responsibility[i];
			}

		return -log_denom + n * Math.log( 2 * Math.PI * SpecialMath.modBesselFirstZero(b) );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value()
	{
		return a;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception
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
		result += this.getClass().getName()+" { ";
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
					a = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "b" ) )
				{
					st.nextToken();
					b = Double.parseDouble( st.sval );
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
