/* Copyright (c) 1997 Robert Dodier and the Joint Center for Energy Management,
 * University of Colorado at Boulder. All Rights Reserved.
 *
 * By copying this software, you agree to the following:
 *  1. This software is distributed for non-commercial use only.
 *     (For a commercial license, contact the copyright holders.)
 *  2. This software can be re-distributed at no charge so long as
 *     this copyright statement remains intact.
 *
 * ROBERT DODIER AND THE JOINT CENTER FOR ENERGY MANAGEMENT MAKE NO
 * REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.
 * ROBERT DODIER AND THE JOINT CENTER FOR ENERGY MANAGEMENT SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY YOU AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package densities;
import java.io.*;

/** This class implements a probability distribution over integers 0, 1, 2, ....
  * This is an unconditional distribution.
  */
public class Discrete implements Density, Serializable, Cloneable
{
	double[] probability_table;
	int[] lengths;
	int ndims;

	/** Make a deep copy of this discrete density object and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Discrete copy = new Discrete();

		copy.probability_table = (double[]) probability_table.clone();
		copy.lengths = (int[]) lengths.clone();
		copy.ndims = ndims;

		return copy;
	}

	/** Return the number of dimensions in which this density function lives.
	  */
	public int ndimensions() { return ndims; }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x )
	{
		// Compute indexing polynomial, then return table value.

		int i, ii = 0;

		for ( i = 0; i < ndims-1; i++ )
			ii = lengths[i+1] * (ii + (int) x[i]);
		ii += (int) x[ndims-1];

		return probability_table[ii];
	}

	/** Return an instance of a random variable from this density.
	  */
	public double[] random()
	{
		double[] x = new double[ndims];
		double r = Math.random(), s = 0;
		int i, j;

		for ( i = 0; i < probability_table.length-1; i++ )
			if ( r < (s += probability_table[i]) )
				break;
		
		for ( j = ndims-1; j >= 0; j-- )
		{
			x[j] = i % lengths[j];
			i -= (int) x[j];
			i /= lengths[j];
		}

		return x;
	}

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( InputStream is ) throws IOException
	{
		throw new IOException( "Discrete.pretty_input: not implemented." );
	}

	/** Write a description of this density model to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  * @throws IOException If the attempt to write the model fails.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		int i, j;

		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.println( leading_ws+this.getClass().getName()+"\n"+leading_ws+"{" );
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;

		dest.println( more_leading_ws+"ndimensions "+ndims );
		dest.print( more_leading_ws+"lengths { " );
		for ( i = 0; i < ndims; i++ )
			dest.print( lengths[i]+" " );
		dest.println( "}" );

		int[] block_sizes = new int[ndims];
		block_sizes[ndims-1] = 1;
		for ( i = ndims-2; i >= 0; i++ )
			block_sizes[i] = block_sizes[i+1]*lengths[i+1];

		dest.print( more_leading_ws+"probability-table"+"\n"+more_leading_ws+"{" );
		for ( i = 0; i < probability_table.length; i++ )
		{
			boolean at_least_one_newline = false;
			for ( j = 0; j < ndims; j++ )
				if ( i % block_sizes[j] == 0 )
				{
					dest.println( "" );
					at_least_one_newline = true;
				}
			
			if ( at_least_one_newline )
				dest.print( still_more_ws );

			dest.print( probability_table[i] );
		}

		dest.print( "\n"+more_leading_ws+"}"+"\n"+leading_ws+"}"+"\n" );
	}

	/** Use data to modify the parameters of the density. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this density produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture densities, although
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
}
