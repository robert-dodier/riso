package risotto.distributions;
import java.io.*;
import java.rmi.*;

/** Interface for all unconditional distribution models. Note that an
  * unconditional distribution is a special kind of conditional distribution.
  */
public interface Distribution extends ConditionalDistribution, Remote
{
	/** Return the number of dimensions in which this distribution lives.
	  */
	public int ndimensions() throws RemoteException;

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x ) throws RemoteException;

	/** Return an instance of a random variable from this distribution.
	  */
	public double[] random() throws RemoteException;

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
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception, RemoteException;
}
