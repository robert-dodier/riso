package regression;

/** Interface which all regression models implement. The basic functionality
  * of regression models is spelled out here.
  */

interface RegressionModel
{
	/** Return the output of the regression function at the specified input.
	  * @param x Input point.
	  * @return Output of regression function.
	  */
	public double[] F( double[] x );

	/** Return the Jacobian matrix (i.e., matrix of partial derivatives) 
	  * of the regression function w.r.t. the input.
	  * @param x Input point.
	  * @return Jacobian matrix at <code>x</code>.
	  */
	public double[][] dFdx( double[] x );

	/** Use data to modify the parameters of the regression model. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param is_present Each component <code>is_present[i]</code> of this
	  *   vector tells whether the corresponding datum <code>x[i]</code>
	  *   is present or missing.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this density produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture densities, although
	  *   other uses can be imagined.
	  * @return Some indication of goodness-of-fit, such as MSE or negative
	  *   log-likelihood.
	  */
	public double update( double[][] x, double[][] y, boolean[] is_x_present, boolean[] is_y_present, int niter_max, double stopping_criterion );	// OUGHT TO INDICATE ERRORS BY THROW!!!

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  */
	public void pretty_input( java.io.InputStream is ) throws java.io.IOException;

	/** Write a description of this density model to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public void pretty_output( java.io.OutputStream os, String leading_ws ) throws java.io.IOException;

	public int ndimensions_in();
	public int ndimensions_out();
};
