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

/** This class implements a probability distribution over integers 0, 1, 2, ....
  * This is an unconditional distribution.
  */
public class Discrete extends AbstractDistribution
{
	public double[] probabilities;
	public int[] dimensions;
	public int ndims;

	/** Create a new object, and don't fill in any of the member data.
	  */
	public Discrete() {}

	/** Create a new object, but don't fill in the probabilities.
	  */
	public Discrete( int[] dimensions )
	{
		ndims = dimensions.length;
		this.dimensions = (int[]) dimensions.clone();
		int i, size = 1;
		for ( i = 0; i < ndims; i++ )
			size *= dimensions[i];
		probabilities = new double[size];
	}

	/** Make a deep copy of this object and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Discrete copy = (Discrete) super.clone();

		copy.probabilities = (double[]) probabilities.clone();
		copy.dimensions = (int[]) dimensions.clone();
		copy.ndims = ndims;

		return copy;
	}

	/** Return the number of dimensions in which this distribution lives.
	  */
	public int ndimensions() { return ndims; }

	/** Returns the number of states of the variable associated with this distribution.
	  * If the variable is more than 1-dimensional, the product of the number of states
	  * in each dimension is returned.
	  */
	public int get_nstates() { return probabilities.length; }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x ) throws Exception
	{
		// Compute indexing polynomial, then return table value.

		int i, ii = 0;

		for ( i = 0; i < ndims-1; i++ )
			ii = dimensions[i+1] * (ii + (int) x[i]);
		ii += (int) x[ndims-1];

		return probabilities[ii];
	}

	/** Return an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception
	{
		double[] x = new double[ndims];
		double r = Math.random(), s = 0;
		int i, j;

		for ( i = 0; i < probabilities.length-1; i++ )
			if ( r < (s += probabilities[i]) )
				break;
		
		for ( j = ndims-1; j >= 0; j-- )
		{
			x[j] = i % dimensions[j];
			i -= (int) x[j];
			i /= dimensions[j];
		}

		return x;
	}

	/** Create a description of this distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";
		int i, j;

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;

		if ( ndims != 1 )
			result += more_leading_ws+"ndimensions "+ndims+"\n";
		result += more_leading_ws+"dimensions { ";
		for ( i = 0; i < ndims; i++ )
			result += dimensions[i]+" ";
		result += "}"+"\n";

		int[] block_sizes = new int[ndims];
		block_sizes[ndims-1] = 1;
		for ( i = ndims-2; i >= 0; i-- )
			block_sizes[i] = block_sizes[i+1]*dimensions[i+1];

		result += more_leading_ws+"probabilities"+"\n"+more_leading_ws+"{";
		for ( i = 0; i < probabilities.length; i++ )
		{
			if ( ndims > 2 && i % block_sizes[ndims-3] == 0 )
			{
				result += "\n\n"+still_more_ws+"/* probabilities";
				for ( j = 0; j < ndims-2; j++ )
					result += "["+(i/block_sizes[j])%dimensions[j]+"]";
				result += "[][] */"+"\n"+still_more_ws;
			}
			else if ( ndims > 1 && i % block_sizes[ndims-2] == 0 )
				result += "\n"+still_more_ws;
			else if ( ndims == 1 && i == 0 )
				result += "\n"+still_more_ws;

			result += probabilities[i]+" ";
		}

		result += "\n"+more_leading_ws+"}"+"\n"+leading_ws+"}"+"\n";
		return result;
	}

	/** Read a description of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		// Assume number of dimensions is 1, unless told otherwise.
		ndims = 1;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Discrete.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions" ) )
				{
					st.nextToken();
					ndims = Integer.parseInt( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "Discrete.pretty_input: ``dimensions'' lacks opening bracket." );

					dimensions = new int[ndims];
					for ( int i = 0; i < ndims; i++ )
					{
						st.nextToken();
						dimensions[i] = Integer.parseInt( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "Discrete.pretty_input: ``dimensions'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "probabilities" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "Discrete.pretty_input: ``probabilities'' lacks opening bracket." );

					int i, size = 1;
					for ( i = 0; i < ndims; i++ )
						size *= dimensions[i];
					probabilities = new double[size];

					for ( i = 0; i < size; i++ )
					{
						st.nextToken();
						probabilities[i] = Double.parseDouble( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "Discrete.pretty_input: ``probabilities'' lacks closing bracket." );
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
			throw new IOException( "Discrete.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Discrete.pretty_input: no closing bracket on input." );

        ensure_normalization();
	}

	/** Use data to modify the parameters of the distribution. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this distribution produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture distributions, although
	  *   other uses can be imagined.
	  * @param niter_max Maximum number of iterations of the update algorithm,
	  *   if applicable.
	  * @param stopping_criterion A number which describes when to stop the
	  *   update algorithm, if applicable.
	  * @return Some indication of goodness-of-fit, such as MSE or negative
	  *   log-likelihood.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Discrete.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws Exception
	{
		if ( ndims > 1 ) throw new IllegalArgumentException( "Discrete.expected_value: not meaningful for #dimensions == "+ndims );

		double sum = 0;
		for ( int i = 0; i < probabilities.length; i++ )
			sum += i*probabilities[i];
		return sum;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception
	{
		if ( ndims > 1 ) throw new IllegalArgumentException( "Discrete.sqrt_variance: not meaningful for #dimensions == "+ndims );

		double sum = 0, sum2 = 0;
		for ( int i = 0; i < probabilities.length; i++ )
		{
			sum += i*probabilities[i];
			sum2 += i*i*probabilities[i];
		}

		double var = sum2 - sum*sum;
		return Math.sqrt(var);
	}

	/** Returns <tt>{0, n-1}</tt> where <tt>n</tt> is the number of elements
	  * in the support of this distribution. 
	  * @param epsilon This argument is ignored.
	  * @throws IllegalArgumentException If the number of dimensions is more than 1.
	  */
	public double[] effective_support( double epsilon ) throws IllegalArgumentException
	{
		if ( ndims > 1 )
			throw new IllegalArgumentException( "Gaussian.effective_support: can't handle "+ndims+" dimensions." );
		
		double[] support = new double[2];

		support[0] = 0;
		support[1] = dimensions[0]-1;

		return support;
	}

	public double assign_p( int[] ix, double q ) throws Exception
	{
		// Compute indexing polynomial, then set table value; return old value.

		int i, ii = 0;

		for ( i = 0; i < ndims-1; i++ )
			ii = dimensions[i+1] * (ii + ix[i]);
		ii += ix[ndims-1];

		double old = probabilities[ii];
		probabilities[ii] = q;
		return old;
	}

    /** Ensures that the distribution sums to 1.
      * If not, a message is printed and the distribution is normalized to 1.
      */
	public void ensure_normalization() 
	{
		double sum = 0;
		for (int i = 0; i < probabilities.length; i++)
			sum += probabilities[i];

        if (Math.abs(sum - 1) > 1e-12)
        {
            System.err.println ("Discrete.ensure_normalization: probabilities sums to "+sum+", not 1; error == "+(sum-1)+"; enforce normalization.");

		    for (int i = 0; i < probabilities.length; i++)
			    probabilities[i] /= sum;
        }
	}
}
