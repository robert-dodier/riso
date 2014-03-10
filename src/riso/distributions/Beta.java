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

/** An instance of this class represents a beta distribution.
  */
public class Beta extends AbstractDistribution
{
	/** 
	  */
	public double p;

	/** 
	  */
	public double q;

	/** Constructs a beta distribution with the specified parameters.
	  */
	public Beta( double p, double q )
	{
		this.p = p;
		this.q = q;
	}

	/** Default constructor for this class. Sets parameters <tt>p</tt> and <tt>q</tt> to 1.
	  */
	public Beta() { p = q = 1; }

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
		if ( x[0] <= 0 || x[0] >= 1 ) return 0;

		return Math.exp( -SpecialMath.logBeta(p,q)+(p-1)*Math.log(x[0])+(q-1)*Math.log(1-x[0]) );
	}

	/** Compute the cumulative distribution function of this beta distribution.
	  */
	public double cdf( double x )
	{
		return SpecialMath.incompleteBeta(x,p,q);
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Beta.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This method is not implemented.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "Beta.random: not implemented." );
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Beta.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() 
	{
		return p/(p+q);
	}

	/** Returns the square root of the variance of this distribution.
	  * This is equal to the scale parameter times the square root of
	  * the shape parameter.
	  */
	public double sqrt_variance()
	{
		double var = p*q/((p+q)*(p+q)*(p+q+1));
		return Math.sqrt(var);
	}

	/** Always returns the interval <tt>[0,1]</tt> -- this is the support of
	  * all beta distributions.
	  * @param epsilon Ignored.
	  * @return The  interval <tt>[0,1]</tt> represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double[] interval = new double[2];
		interval[0] = 0;
		interval[1] = 1;
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
		result += "p "+p+"  q "+q;
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
				throw new IOException( "Beta.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "p" ) )
				{
					st.nextToken();
					p = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "q" ) )
				{
					st.nextToken();
					q = Double.parseDouble( st.sval );
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
			throw new IOException( "Beta.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Beta.pretty_input: no closing bracket on input." );
	}
}
