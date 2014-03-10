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
  * This is a conditional distribution; the parents are also discrete.
  */
public class ConditionalDiscrete extends AbstractConditionalDistribution
{
	public double[][] probabilities;
	public int[] dimensions_child, dimensions_parents;
	public int ndims_child, ndims_parents;

	/** Make a deep copy of this object and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		ConditionalDiscrete copy = (ConditionalDiscrete) super.clone();

		copy.probabilities = Matrix.copy( probabilities );
		copy.dimensions_child = (int[]) dimensions_child.clone();
		copy.dimensions_parents = (int[]) dimensions_parents.clone();
		copy.ndims_child = ndims_child;
		copy.ndims_parents = ndims_parents;

		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() { return ndims_child; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() { return ndims_parents; }

	/** Returns the number of states of the variable associated with this distribution.
	  * If the variable is more than 1-dimensional, the product of the number of states
	  * in each dimension is returned.
	  */
	public int get_nstates() { return probabilities[0].length; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		Discrete p = new Discrete();
		p.ndims = ndims_child;
		p.dimensions = (int[]) dimensions_child.clone();

		// Compute indexing polynomial, then return conditional probabilities.
		// Probability table is stored in row-major form, so parent indices
		// come first. Child indices are 0, 0, ..., 0.

		int i, ii = 0;

		for ( i = 0; i < ndims_parents-1; i++ )
			ii = dimensions_parents[i+1] * (ii + (int) c[i]);
		ii += (int) c[ndims_parents-1];

		p.probabilities = (double[]) probabilities[ii].clone();

		return p;
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c )
	{
		// Compute indexing polynomial, then return table value.
		// Probability table is stored in row-major form, so parent indices
		// come first. 

		int i, ii = 0, jj = 0;

		for ( i = 0; i < ndims_parents-1; i++ )
			ii = dimensions_parents[i+1] * (ii + (int) c[i]);
		ii += (int) c[ndims_parents-1];

		for ( i = 0; i < ndims_child-1; i++ )
			jj = dimensions_child[i+1] * (jj + (int) x[i]);
		jj += (int) x[ndims_child-1];

		return probabilities[ii][jj];
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c )
	{
		// Compute indexing polynomial to get to base of conditional
		// probability table. 

		int i, j, ii = 0, jj = 0;

		for ( i = 0; i < ndims_parents-1; i++ )
			ii = dimensions_parents[i+1] * (ii + (int) c[i]);
		ii += (int) c[ndims_parents-1];

		double[] pp = probabilities[ii], x = new double[ndims_child];
		double s = 0, r = Math.random();

		for ( i = 0; i < pp.length; i++ )
			if ( r < (s += pp[i]) )
				break;
		
		for ( j = ndims_child-1; j >= 0; j-- )
		{
			x[j] = i % dimensions_child[j];
			i -= (int) x[j];
			i /= dimensions_child[j];
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
		int i, j, k;

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;

		if ( ndims_child != 1 )
			result += more_leading_ws+"ndimensions-child "+ndims_child+"\n";
		if ( ndims_parents != 1 )
			result += more_leading_ws+"ndimensions-parents "+ndims_parents+"\n";
		result += more_leading_ws+"dimensions-child { ";
		for ( i = 0; i < ndims_child; i++ )
			result += dimensions_child[i]+" ";
		result += "}"+"\n";
		result += more_leading_ws+"dimensions-parents { ";
		for ( i = 0; i < ndims_parents; i++ )
			result += dimensions_parents[i]+" ";
		result += "}"+"\n";

		int[] parents_block_sizes = new int[ndims_parents];
		int[] child_block_sizes = new int[ndims_child];
		parents_block_sizes[ndims_parents-1] = 1;
		child_block_sizes[ndims_child-1] = 1;

		for ( i = ndims_parents-2; i >= 0; i-- )
			parents_block_sizes[i] = parents_block_sizes[i+1]*dimensions_parents[i+1];
		for ( i = ndims_child-2; i >= 0; i-- )
			child_block_sizes[i] = child_block_sizes[i+1]*dimensions_child[i+1];

		result += more_leading_ws+"probabilities"+"\n"+more_leading_ws+"{";

		boolean following_context = false;

		// To speed up the formatting of the probabilities, first allocate a large string
		// buffer and write into it. Then append the string buffer to result.
		// Estimate the size of buffer needed according to the number of probabilities
		// to be formatted; assume each has 17 digits after the decimal; plus a zero and
		// the decimal and a space, that makes 20 characters per number to be printed.
		// There's also a comment string printed on every line, of say 40 char.

		int buffer_size = 20 * probabilities.length * probabilities[0].length + 40 * probabilities.length + 1000;
		StringBuffer sb = new StringBuffer( buffer_size );

		for ( i = 0; i < probabilities.length; i++ )
		{
			sb.append( "\n\n" ); sb.append( still_more_ws ); sb.append( "% context" );
			for ( k = 0; k < ndims_parents; k++ )
			{
				sb.append( "[" ); sb.append( (i/parents_block_sizes[k])%dimensions_parents[k] ); sb.append( "]" );
			}
			sb.append( "\n" ); sb.append( still_more_ws );
			following_context = true;

			for ( j = 0; j < probabilities[i].length; j++ )
			{
				if ( ndims_child > 1 && j % child_block_sizes[ndims_child-2] == 0 )
				{
					sb.append( "\n" ); sb.append( still_more_ws );
				}

				if ( ndims_child > 2 && j % child_block_sizes[ndims_child-3] == 0 )
				{
					if ( following_context )
						following_context = false;
					else
					{
						sb.append( "\n" ); sb.append( still_more_ws );
					}

					sb.append( "% probabilities" );
					for ( k = 0; k < ndims_child-2; k++ )
					{
						sb.append( "[" ); sb.append( (j/child_block_sizes[k])%dimensions_child[k] ); sb.append( "]" );
					}
					sb.append( "[][]" ); sb.append( "\n" ); sb.append( still_more_ws );
				}

				sb.append( probabilities[i][j] ); sb.append( " " );
			}
		}

		result += sb.toString();

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

		// Unless we're told otherwise, assume number of parent dimensions
		// is 1, likewise with child dimensions.

		ndims_parents = ndims_child = 1;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "ConditionalDiscrete.pretty_input: input doesn't have opening bracket (found "+st+" instead)." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions-child" ) )
				{
					st.nextToken();
					ndims_child = Integer.parseInt( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions-parents" ) )
				{
					st.nextToken();
					ndims_parents = Integer.parseInt( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions-child" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-child'' lacks opening bracket (found "+st.sval+" instead)." );

					dimensions_child = new int[ndims_child];
					for ( int i = 0; i < ndims_child; i++ )
					{
						st.nextToken();
						dimensions_child[i] = Integer.parseInt( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-child'' lacks closing bracket (found "+st.sval+" instead)." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions-parents" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-parents'' lacks opening bracket (found "+st.sval+" instead)." );

					dimensions_parents = new int[ndims_parents];
					for ( int i = 0; i < ndims_parents; i++ )
					{
						st.nextToken();
						dimensions_parents[i] = Integer.parseInt( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-parents'' lacks closing bracket (found "+st.sval+" instead)." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "probabilities" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``probabilities'' lacks opening bracket (found "+st.sval+" instead)." );

					int i, j, parents_size = 1, child_size = 1;

					for ( i = 0; i < ndims_child; i++ )
						child_size *= dimensions_child[i];
					for ( i = 0; i < ndims_parents; i++ )
						parents_size *= dimensions_parents[i];

					probabilities = new double[parents_size][child_size];

					for ( i = 0; i < parents_size; i++ )
						for ( j = 0; j < child_size; j++ )
						{
							st.nextToken();
							probabilities[i][j] = Double.parseDouble( st.sval );
						}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``probabilities'' lacks closing bracket (found "+st.sval+" instead)." );
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
			System.err.println( "ConditionalDiscrete.pretty_input: tokenizer state: "+st );
			throw new IOException( "ConditionalDiscrete.pretty_input: attempt to read object failed:\n"+e );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println( "ConditionalDiscrete.pretty_input: tokenizer state: "+st );
			throw new IOException( "ConditionalDiscrete.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "ConditionalDiscrete.pretty_input: no closing bracket on input." );

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
		throw new Exception( "ConditionalDiscrete.update: not implemented." );
	}

    /** Ensures that for every combination of parent values, the conditional
      * distribution sums to 1. If not, a message is printed and the distribution
      * is normalized to 1.
      */
    public void ensure_normalization()
    {
        for (int i = 0; i < probabilities.length; i++)
        {
            double sum = 0;
            for (int j = 0; j < probabilities[i].length; j++)
                sum += probabilities[i][j];

            if (Math.abs(sum - 1) > 1e-12)
            {
                System.err.println ("ConditionalDiscrete.ensure_normalization: probabilities["+i+"] sums to "+sum+", not 1; error == "+(sum-1)+"; enforce normalization.");

                for (int j = 0; j < probabilities[i].length; j++)
                    probabilities[i][j] /= sum;
            }
        }
    }
}
