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

/** This class represents a conditional density based on a regression
  * function and a noise model.
  */
public class RegressionDensity implements ConditionalDensity, Serializable, Cloneable
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

	/** Return a cross-section through the regression density at <code>c</code>.
	  * @param c Point at which to return the cross-section.
	  * @return A <code>Density</code> which represents a cross-section through
	  *   the regression density.
	  * @see Density.get_density
	  */
	public Density get_density( double[] c )
	{
		double[] y = regression_model(c);
		LocationScaleDensity cross_section = (LocationScaleDensity) noise_model.clone();
		cross_section.set_location(y);
		return cross_section;
	}

	/** Compute a likelihood message, to be sent to parents. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public Density compute_likelihood( Density[] children );

	/** Compute a prediction message, to be sent to children. This is defined
	  * as <code>p(x|``e above'')</code> ... NEEDS WORK !!!
	  */
	public Density compute_prediction( Density[] parents );

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
	  */
	public double update( double[][] x, double[][] c, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "RegressionModel.update: not implemented." );
	}
}
