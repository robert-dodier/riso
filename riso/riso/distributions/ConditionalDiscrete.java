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
  * This is a conditional distribution; the parents are also discrete.
  */
public class ConditionalDiscrete implements ConditionalDensity, Serializable, Cloneable
{
	double[][] probabilities;
	int[] dimensions_child, dimensions_parents;
	int ndims_child, ndims_parents;

	/** Make a deep copy of this discrete density object and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		ConditionalDiscrete copy = new ConditionalDiscrete();

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

	/** For a given value <code>c</code> of the parents, return a density
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Density get_density( double[] c )
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

	/** Return an instance of a random variable from this density.
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

		double[] pp = probabilities[ii];
		double s = 0, r = Math.random(), x = new double[ndims_child];

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

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( InputStream is ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			Reader r = new BufferedReader(new InputStreamReader(is));
			StreamTokenizer st = new StreamTokenizer(r);
			st.wordChars( '$', '%' );
			st.wordChars( '?', '@' );
			st.wordChars( '[', '_' );
			st.ordinaryChar('/');
			st.slashStarComments(true);
			st.slashSlashComments(true);

			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "ConditionalDiscrete.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions-child" ) )
				{
					st.nextToken();
					ndims_child = (int) st.nval;
					dimensions_child = new int[ndims_child];
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions-parents" ) )
				{
					st.nextToken();
					ndims_parents = (int) st.nval;
					dimensions_parents = new int[ndims_parents];
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions-child" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-child'' lacks opening bracket." );

					for ( int i = 0; i < ndims; i++ )
					{
						st.nextToken();
						dimensions_child[i] = (int) st.nval;
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-child'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions-parents" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-parents'' lacks opening bracket." );

					for ( int i = 0; i < ndims; i++ )
					{
						st.nextToken();
						dimensions_parents[i] = (int) st.nval;
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-parents'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "probabilities" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``probabilities'' lacks opening bracket." );

					int i, size = 1;

					for ( i = 0; i < ndims_child; i++ )
						size *= dimensions_child[i];
					for ( i = 0; i < ndims_parents; i++ )
						size *= dimensions_parents[i];

					probabilities = new double[size];

					for ( i = 0; i < size; i++ )
					{
						st.nextToken();
						probabilities[i] = st.nval;
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``probabilities'' lacks closing bracket." );
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
			throw new IOException( "ConditionalDiscrete.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "ConditionalDiscrete.pretty_input: no closing bracket on input." );
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

		dest.println( more_leading_ws+"ndimensions-child "+ndims_child );
		dest.println( more_leading_ws+"ndimensions-parents "+ndims_parents );
		dest.print( more_leading_ws+"dimensions-child { " );
		for ( i = 0; i < ndims_child; i++ )
			dest.print( dimensions_child[i]+" " );
		dest.println( "}" );
		dest.print( more_leading_ws+"dimensions-parents { " );
		for ( i = 0; i < ndims_parents; i++ )
			dest.print( dimensions_parents[i]+" " );
		dest.println( "}" );

		int[] block_sizes = new int[ndims_child+ndims_parents];
		block_sizes[ndims-1] = 1;
		for ( i = ndims-2; i >= 0; i-- )
			block_sizes[i] = block_sizes[i+1]*dimensions[i+1];

		dest.print( more_leading_ws+"probabilities"+"\n"+more_leading_ws+"{" );
		for ( i = 0; i < probabilities.length; i++ )
		{
			if ( ndims > 2 && i % block_sizes[ndims-3] == 0 )
			{
				dest.print( "\n\n"+still_more_ws+"/* probabilities" );
				for ( j = 0; j < ndims-2; j++ )
					dest.print( "["+i/block_sizes[j]+"]" );
				dest.print( "[][] */"+"\n"+still_more_ws );
			}
			else if ( i % block_sizes[ndims-2] == 0 )
				dest.print( "\n"+still_more_ws );

			dest.print( probabilities[i]+" " );
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
		throw new Exception( "ConditionalDiscrete.update: not implemented." );
	}
}
