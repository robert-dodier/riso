package riso.regression;
import java.io.*;
import java.rmi.*;

/** Interface which all regression models implement. The basic functionality
  * of regression models is spelled out here.
  */

public interface RegressionModel
{
	/** Make a deep copy of this object and return a reference to the copy.
	  * If the object is remote, the returned reference is a remote reference.
	  */
	public Object remote_clone() throws CloneNotSupportedException;

	/** Return the output of the regression function at the specified input.
	  * @param x Input point.
	  * @return Output of regression function.
	  */
	public double[] F( double[] x ) throws Exception;

	/** Return the Jacobian matrix (i.e., matrix of partial derivatives) 
	  * of the regression function w.r.t. the input.
	  * @param x Input point.
	  * @return Jacobian matrix at <code>x</code>.
	  */
	public double[][] dFdx( double[] x ) throws Exception;

	/** Use data to modify the parameters of the regression model. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this density produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture densities, although
	  *   other uses can be imagined. If this array is <code>null</code> then
	  *   assume that all responsibilities are 1.
	  * @return Some indication of goodness-of-fit, such as MSE or negative
	  *   log-likelihood.
	  */
	public double update( double[][] x, double[][] y, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception;

	/** Parse a string containing a description of a regression model.
	  * The description is contained within curly braces, which are
	  * included in the string.
	  */
	public void parse_string( String description ) throws IOException;

	/** Create a description of this regression model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException;

	public int ndimensions_in();
	public int ndimensions_out();
};
