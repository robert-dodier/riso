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

/** An object of this class represents a discrete distribution 
  * whose mass is concentrated at one point. An instance of this
  * class can be used anywhere that an instance of <tt>Discrete</tt>
  * can be used.
  */
public class DiscreteDelta extends Discrete implements Delta
{
	/** The point on which the mass of this distribution is concentrated.
	  */
	public int[] support_point = null;

	/** Construct an empty object.
	  */
	public DiscreteDelta() { super(); }

	/** Construct a copy of this object and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		DiscreteDelta copy = (DiscreteDelta) super.clone();
		copy.support_point = (int[]) this.support_point.clone();
		return copy;
	}

	/** Given the dimensions of a discrete probability space and a support point,
	  * this method constructs a discrete delta distribution.
	  */
	public DiscreteDelta( int[] dimensions_in, int[] support_point_in ) 
	{
		ndims = dimensions_in.length;
		dimensions = (int[]) dimensions_in.clone();
		support_point = (int[]) support_point_in.clone();

		int i, ii = 0, nprobabilities = 1;
		for ( i = 0; i < ndims; i++ )
			nprobabilities *= dimensions[i];

		probabilities = new double[ nprobabilities ];

		// Compute indexing polynomial, then set table value.

		for ( i = 0; i < ndims-1; i++ )
			ii = dimensions[i+1] * (ii + support_point[i]);
		ii += support_point[ndims-1];

		probabilities[ii] = 1;
	}

	/** Return the point on which the mass of this density is concentrated.
	  */
	public double[] get_support() throws IllegalArgumentException { return random(); }

	/** Computes the density at the point <code>x</code>. 
	  *
	  * @return 1 if <tt>x</tt> is equal to the support point and 0 otherwise.
	  * @param x Point at which to evaluate density.
	  * @throws IllegalArgumentException If the support point is not defined.
	  */
	public double p( double[] x ) throws IllegalArgumentException
	{
		if ( support_point != null )
		{
			for ( int i = 0; i < support_point.length; i++ )
				if ( (int)x[i] != support_point[i] )
					return 0;
			return 1;
		}
		else
			throw new IllegalArgumentException( "DiscreteDelta.p: support point not defined." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This function always returns the point on which this distribution
	  * is concentrated, since all the mass is there.
	  */
	public double[] random() throws IllegalArgumentException
	{
		if ( support_point != null )
		{
			double[] r = new double[ support_point.length ];
			for ( int i = 0; i < r.length; i++ ) r[i] = support_point[i];
			return r;
		}
		else
			throw new IllegalArgumentException( "DiscreteDelta.random: support point not defined." );
	}

	/** Place-holder for method to update the parameters of this distribution;
	  * not implemented -- see exception description.
	  * @throws Exception Because this method can't be meaningfully implemented
	  *   for this type of distribution.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "DiscreteDelta.update: not meaningful for this distribution." );
	}

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Supplies sequence of tokens for this method; 
	  *   should not parse numbers, but rather should read them as string values.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		throw new IOException( "DiscreteDelta.pretty_input: not implemented." );
	}

	/** Create a description of this distribution model as a string.
	  * @param leading_ws Leading whitespace; this argument is ignored
	  *   by this method.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";
		result += this.getClass().getName()+" "+"{"+" ";
		if ( support_point != null )
		{
			result += "support-point { ";
			for ( int i = 0; i < support_point.length; i++ )
				result += support_point[i]+" ";
			result += "}"+" ";
		}
		result += "}"+"\n";
		return result;
	}
}
