/* Copyright (c) 1997 Robert Dodier and the Joint Center for Energy Management,
 * University of Colorado at Boulder. All Rights Reserved.
 *
 * By copying this software, you agree to the following: This software is
 * distributed for non-commercial use only. (For a commercial license,
 * contact the copyright holders.) This software can be re-distributed at
 * no charge so long as this copyright statement remains intact.
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

/** This class represents a conditional density based on a regression
  * function and a noise model.
  */

public interface RegressionDensity
{
	protected int ndimensions_child, ndimensions_parent;

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() { return ndimensions_child; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() { return ndimensions_parent; }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c )
	{
		double[] y = regression_model(c);
		double[] residual = x.clone();
		for ( int i = 0; i < ndimensions_child; i++ )
			residual[i] -= y[i];

		return noise_model( residual, null );
	}

	/** Return an instance of a random variable from this density.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c )
	{
		double[] epsilon = noise_model.random(null);
		double[] y = regression_model(c);
		for ( int i = 0; i < ndimensions_child; i++ )
			y[i] += epsilon[i];

		return y;
	}

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  */
	public void pretty_input( InputStream is ) throws IOException
	{
		throw new IOException( "RegressionDensity.pretty_input: not implemented." );
	}

	/** Write a description of this density model to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		throw new IOException( "RegressionDensity.pretty_output: not implemented." );
	}

	/** Use data to modify the parameters of the density. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The child data. Each row has a number of components equal
	  *   to ndimensions_child(), and the number of rows is the number of data.
	  * @param c The parent data. Each row has a number of components equal
	  *   to ndimensions_parent(), and the number of rows is the same
	  *   as for <code>x</code>.
	  * @param is_present Each component <code>is_present[i]</code> of this
	  *   vector tells whether the corresponding datum <code>x[i]</code>
	  *   is present or missing.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this density produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture densities, although
	  *   other uses can be imagined.
	  * @return Whether the parameter updated succeeded or not.
	  */
	public boolean update( double[][] x, double[][] c, boolean[] is_present, double[] responsibility );
}
