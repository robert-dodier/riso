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
import java.util.*;
import riso.numerical.*;
import riso.general.*;

/** An instance of this class represents the distribution of the maximum
  * of a set of variables with any distributions.
  */
public class DistributionMax extends AbstractDistribution
{
	/** The list of distributions represented by this distribution.
	  */
	Distribution[] p;

	/** Constructs a <tt>DistributionMax</tt> given the specified list
	  * of distributions. References (not objects) are copied.
	  *
	  * <p> Any distributions with effective support disjoint from,
	  * and to the left of, the effective support of some other distribution is
	  * not put the list of distributions for this maximum.
	  * NOT IMPLEMENTED YET !!!
	  */
	public DistributionMax( Distribution[] p )
	{
		this.p = (Distribution[]) p.clone();
	}

	/** Return a shallow copy of this object. References (not objects) are copied.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		DistributionMax copy = (DistributionMax) super.clone();
		copy.p = (Distribution[]) this.p.clone();
		return copy;
	}
	
	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Compute cumulative distribution function for this distribution.
	  * This is just the product of the individual cdf's.
	  */
	public double cdf( double x ) throws Exception
	{
		double prod = 1;
		for ( int i = 0; i < p.length; i++ )
			prod *= p[i].cdf(x);
		return prod;
	}

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x ) throws Exception
	{
		double sum = 0;
		for ( int i = 0; i < p.length; i++ )
		{
			double prod = 1;
			for ( int j = 0; j < p.length; j++ )
			{
				if ( j == i )
					prod *= p[j].p(x);
				else
					prod *= p[j].cdf(x[0]);
			}
			
			sum += prod;
		}

		return sum;
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "DistributionMax.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This method is not implemented.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "DistributionMax.random: not implemented." );
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "DistributionMax.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  * This method is not implemented.
	  */
	public double expected_value() throws Exception
	{
		throw new Exception( "DistributionMax.expected_value: not implemented." );
	}

	/** Returns the square root of the variance of this distribution.
	  * This method is not implemented.
	  */
	public double sqrt_variance() throws Exception
	{
		throw new Exception( "DistributionMax.sqrt_variance: not implemented." );
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution; uses a numerical search.
	  *
	  * @param epsilon This much mass or less lies outside the interval
	  *   which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double x0 = 1e300, x1 = -1e300;

		for ( int i = 0; i < p.length; i++ )
		{
			double[] ii = p[i].effective_support(epsilon);
			if ( ii[0] < x0 ) x0 = ii[0];
			if ( ii[1] > x1 ) x1 = ii[1];
		}
System.err.println( "DistributionMax.effective_support: initial: "+x0+", "+x1 );

		// Make sure [x0,x1] contains at least mass 1-epsilon/2.

		double m = this.cdf(x1) - this.cdf(x0);
		while ( m < 1-epsilon/2 )
		{
			double xmid = (x0+x1)/2, h = x1-x0;
			x1 = xmid+h;
			x0 = xmid-h;
			m = this.cdf(x1) - this.cdf(x0);
		}
System.err.println( "\t"+"at least 1-"+(epsilon/2)+" in "+x0+", "+x1 );

		// Now try to make [x0,x1] smaller, while still containing mass 1-epsilon.
		// First chop off upper tail, then chop off lower tail.

		double z0 = x0, z1 = x1, zm = x1;
		for ( int i = 0; i < 20; i++ ) // reduce initial interval by 10^(-6) factor.
		{
			zm = z0 + (z1-z0)/2;
			if ( this.cdf(zm) > 1-epsilon/2 )
				z1 = zm;
			else 
				z0 = zm;
		}
		x1 = zm;

		z0 = x0; z1 = x1;
		for ( int i = 0; i < 20; i++ ) // reduce initial interval by 10^(-6) factor.
		{
			zm = z0 + (z1-z0)/2;
			if ( this.cdf(zm) > epsilon/2 )
				z1 = zm;
			else 
				z0 = zm;
		}
		x0 = zm;
System.err.println( "\t"+"final: "+this.cdf(x0)+" below "+x0+", "+(1-this.cdf(x1))+" above "+x1 );

		double[] interval = new double[2];
		interval[0] = x0;
		interval[1] = x1;
		return interval;
	}

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "", more_ws = leading_ws+"\t";
		result += this.getClass().getName()+"\n"+leading_ws+"{\n";
		for ( int i = 0; i < p.length; i++ )
		{
			result += more_ws+"% Component "+i+"\n";
			result += more_ws+p[i].format_string(more_ws);
		}

		result += leading_ws+"}"+"\n";
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
		Vector plist = new Vector();

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "DistributionMax.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
				else // must be a distribution description
				{
					Class c = java.rmi.server.RMIClassLoader.loadClass(st.sval);
					Distribution pp = (Distribution) c.newInstance();
					st.nextBlock();
					pp.parse_string(st.sval);
					plist.addElement(pp);
				}
			}

			p = new Distribution[ plist.size() ];
			plist.copyInto(p);
		}
		catch (Exception e)
		{
			throw new IOException( "DistributionMax.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "DistributionMax.pretty_input: no closing bracket on input." );
	}
}
