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

/** An object of this class represents a continuous distribution 
  * whose mass is concentrated at one point. An instance of this
  * class can be used anywhere that an instance of <tt>Gaussian</tt>
  * can be used.
  */
public class GaussianDelta extends Gaussian implements Delta
{
	public GaussianDelta()
	{
		mu = new double[1]; // initialized to zero
		ndims = mu.length;
		Sigma = new double[ndims][ndims];		// initialized w/ zeros
		L_Sigma = new double[ndims][ndims];		// initialized w/ zeros
		Sigma_inverse = null;					// inverse undefined !!!
		det_Sigma = 0;
	}

	public GaussianDelta( double[] support_point )
	{
		mu = (double[]) support_point.clone();

		ndims = mu.length;
		Sigma = new double[ndims][ndims];		// initialized w/ zeros
		L_Sigma = new double[ndims][ndims];		// initialized w/ zeros
		Sigma_inverse = null;					// inverse undefined !!!
		det_Sigma = 0;
	}

	public GaussianDelta( double support_point )
	{
		mu = new double[1];
		mu[0] = support_point;

		ndims = mu.length;
		Sigma = new double[ndims][ndims];		// initialized w/ zeros
		L_Sigma = new double[ndims][ndims];		// initialized w/ zeros
		Sigma_inverse = null;					// inverse undefined !!!
		det_Sigma = 0;
	}

	/** Return the point on which the mass of this density is concentrated.
	  */
	public double[] get_support() { return (double[]) mu.clone(); }

	/** Computes the density at the point <code>x</code>. 
	  * This method is a little strange since this distribution is continuous, and strictly 
	  * speaking a meaningful density can't be defined in that case. Oh, well.
	  *
	  * @return 1 if <tt>x</tt> is equal to the support point and 0 otherwise.
	  * @param x Point at which to evaluate density.
	  * @throws IllegalArgumentException If the support point is not defined.
	  */
	public double p( double[] x ) throws IllegalArgumentException
	{
		if ( mu != null )
		{
			for ( int i = 0; i < mu.length; i++ )
				if ( x[i] != mu[i] )
					return 0;
			return Double.POSITIVE_INFINITY;
		}
		else
			throw new IllegalArgumentException( "GaussianDelta.p: support point not defined." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This function always returns the point on which this distribution
	  * is concentrated, since all the mass is there.
	  */
	public double[] random() throws Exception
	{
		if ( mu != null )
			return (double[]) mu.clone();
		else
			throw new IllegalArgumentException( "GaussianDelta.random: support point not defined." );
	}

	/** Place-holder for method to update the parameters of this distribution;
	  * not implemented -- see exception description.
	  * @throws Exception Because this method can't be meaningfully implemented
	  *   for this type of distribution.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "GaussianDelta.update: not meaningful for this distribution." );
	}

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Supplies sequence of tokens for this method; 
	  *   should not parse numbers, but rather should read them as string values.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' ) throw new IOException( "GaussianDelta.pretty_input: input doesn't have opening bracket." );

			st.nextToken();
			if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "support-point" ) )
			{
				st.nextToken();		// eat left curly brace

				Vector support_vector = new Vector();
				for ( st.nextToken(); st.ttype == StreamTokenizer.TT_WORD; st.nextToken() )
					support_vector.addElement(st.sval);
				
				mu = new double[ support_vector.size() ];
				for ( int i = 0; i < mu.length; i++ )
					mu[i] = Double.parseDouble( (String) support_vector.elementAt(i) );
			}

			st.nextToken();
			if ( st.ttype == '}' ) found_closing_bracket = true;
		}
		catch (IOException e)
		{
			throw new IOException( "GaussianDelta.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "GaussianDelta.pretty_input: no closing bracket on input." );

		ndims = mu.length;
		Sigma = new double[ndims][ndims];		// initialized w/ zeros
		L_Sigma = new double[ndims][ndims];		// initialized w/ zeros
		Sigma_inverse = null;					// inverse undefined !!!
		det_Sigma = 0;
	}

	/** Create a description of this distribution model as a string.
	  * @param leading_ws Leading whitespace; this argument is ignored
	  *   by this method.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";
		result += this.getClass().getName()+" "+"{"+" ";
		if ( mu != null )
		{
			result += "support-point { ";
			for ( int i = 0; i < mu.length; i++ )
				result += mu[i]+" ";
			result += "}"+" ";
		}
		result += "}"+"\n";
		return result;
	}
}
