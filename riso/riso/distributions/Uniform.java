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
import java.rmi.server.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a uniform distribution over an interval.
  * Rectangles and hyper-rectangles are not supported; maybe they should be.
  */
public class Uniform extends AbstractDistribution
{
	/** The left end of the interval on which this uniform distribution is defined.
	  */
	public double a;

	/** The right end of the interval on which this uniform distribution is defined.
	  */
	public double b;

	/** Create and return a remote copy of this uniform distribution.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Uniform copy = new Uniform();
		copy.a = this.a;
		copy.b = this.b;
		return copy;
	}

	/** Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Compute the density at the point <code>x</code>. This returns <tt>1/(b-a)</tt>
	  * if <tt>x</tt> is between <tt>a</tt> and <tt>b</tt>, inclusive, and zero otherwise.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x )
	{
		if ( x[0] < a || x[0] > b ) return 0;

		return 1/(b-a);
	}

	/** Returns a random number drawn from the interval <tt>[a,b]</tt>.
	  */
	public double[] random()
	{
		double[] x = new double[1];
		x[0] = Math.random();			// between 0 and 1
		x[0] *= b-a;
		x[0] += a;
		return x;
	}

	/** Parses a string for the parameters of this uniform distribution.
	  * @exception IOException If the parse fails.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Inputs the parameters of this uniform distribution from a stream.
	  * @exception IOException If the input fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		st.nextToken();		// eat the left bracket

		st.nextToken();
		if ( "a".equals( st.sval ) )
		{
			st.nextToken();
			a = Format.atof( st.sval );
		}
		else
			throw new IOException( "Uniform.pretty_input: ``a'' not found; parser state: "+st );

		st.nextToken();
		if ( "b".equals( st.sval ) )
		{
			st.nextToken();
			b = Format.atof( st.sval );
		}
		else
			throw new IOException( "Uniform.pretty_input: ``b'' not found; parser state: "+st );

		st.nextToken();		// eat right bracket

		if ( b <= a ) throw new IOException( "Uniform.pretty_input: a=="+a+", b=="+b+"; what do you mean by that?" );
	}

	/** Output this uniform distribution to a stream, which can be input by <tt>pretty_input</tt>.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Format this uniform distribution into a string, which can be parsed by <tt>parse_string</tt>.
	  * @param leading_ws Ignored; the output is contained on one line, 
	  *   and not prefaced by <tt>leading_ws</tt>.
	  */
	public String format_string( String leading_ws )
	{
		return getClass()+" { a "+a+"  b "+b+" }\n";
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() 
	{
		return a+(b-a)/2;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance()
	{
		return (b-a)/2/Math.sqrt(3);
	}

	/** Returns the support of this distribution.
	  * @param epsilon This argument is ignored.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon )
	{
		double[] ab = new double[2];
		ab[0] = a;
		ab[1] = b;
		return ab;
	}

	/** Returns an approximation containing several components.
	  * The approximation is not very good.
	  */
	public MixGaussians initial_mix( double[] support )
	{
		int nbumps = 7;

		MixGaussians mix = new MixGaussians( 1, nbumps );

		double sigma = (b-a)/(nbumps+1.0)/2.0;

		for ( int i = 0; i < nbumps; i++ )
		{
			double mu = a + (b-a)*(i+1.0)/(nbumps+1.0);

			mix.components[i] = new Gaussian( mu, sigma );
		}

		return mix;
	}
}
