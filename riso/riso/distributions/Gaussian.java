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
import numerical.*;

/** class Gaussian, a Gaussian (normal) density.
  * The descriptive data which can be changed without causing the interface
  * functions to break down is public. The other data is protected.
  * Included in the public data are the regularization parameters. 
  * If not otherwise specified, the prior mean, covariance, etc, are given
  * neutral values, so that they have no effect on parameter estimation.
  * JAVADOC COMMENTS NEED WORK !!!
  */
public class Gaussian implements Density, Serializable, Cloneable
{
	/** Dimensionality of the space in which the density lives.
	  */
	protected int ndims;

	/** Mean vector of the density.
	  */
	public double[] mu;

	/** Covariance matrix of the density. If this ever changes, its inverse,
	  * determinant, and Cholesky decomposition must be recomputed.
	  */
	protected double[][] Sigma;

	/** Inverse of the covariance matrix. 
	  */
	protected double[][] Sigma_inverse;

	/** Determinant of the covariance matrix.
	  */
	protected double det_Sigma;

	/** Lower-triangular Cholesky decomposition of the covariance matrix.
	  */
	protected double[][] L_Sigma;

	/** Prior mean.
	  */
	public double[] mu_hat;

	/** Prior covariance; this is supposed to be a matrix,
	  * but we assume it's diagonal, and just store the diagonal.
	  */
	public double[] beta;

	/** Prior mixing proportions.
	  */
	public double[] gamma;

	/** Prior ???
	  */
	public double alpha;

	/** Prior ???
	  */
	public double eta;

	public Gaussian() { mu = null; Sigma = null; }

	public Gaussian( double[] mu_in, double[][] Sigma_in ) throws IllegalArgumentException
	{
		ndims = mu.length;

		if ( Sigma_in.length != ndims || Sigma_in[0].length != ndims )
			throw new IllegalArgumentException( "In Gaussian constructor: Sigma_in not same size as mu_in" );

		mu = (double[]) mu_in.clone();
		Sigma = Matrix.copy( Sigma_in );

		Sigma_inverse = Matrix.inverse( Sigma );
		det_Sigma = Matrix.determinant( Sigma );
		L_Sigma = Matrix.cholesky( Sigma );
	}

	public int ndimensions() { return ndims; }

	public void pretty_input( InputStream is ) throws IOException
	{
		throw new IOException( "Gaussian.pretty_input: not implemented." );
	}

	/** Print the data necessary to reconstruct this Gaussian. The inverse and
	  * Cholesky decomposition of the covariance are not printed. 
	  * <code>leading_ws</code> is printed at the start of every line of output.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		throw new IOException( "Gaussian.pretty_output: not implemented." );
		os.println( leading_ws+Class.getClass(this).getName()+"\n"+leading_ws+"{" );
		String more_leading_ws = "\t"+leading_ws;

		os.print( more_leading_ws+"mean { " );
		Matrix.pretty_output( mu, os, " " );
		os.println( "}" );

		os.println( more_leading_ws+"covariance"+more_leading_ws+"{" );
		Matrix.pretty_output( Sigma, os, "\t"+more_leading_ws );

		os.print( more_leading_ws+"prior-mean { " );
		Matrix.pretty_output( mu_hat, os, " " );
		os.println( "}" );

		os.print( more_leading_ws+"prior-covariance { " );
		Matrix.pretty_output( beta, os, " " );
		os.println( "}" );


	}

	/** Computed updated parameters of this density by penalized 
	  * maximum likelihood, as described by Ormoneit and Tresp [1]. MORE !!!
	  *
	  * [1] Ormoneit, D., and V. Tresp. (1996) ``Improved Gaussian Mixture
	  *   Density Estimates Using Bayesian Penalty Terms and Network
	  *   Averaging.'' <em>Advances in Neural Information Processing Systems 8,</em>
	  *   D. Touretzky, M. Mozer, and M. Hasselmo, eds. Cambridge, MA: MIT Press.
	  *
	  * @param x The data.
	  * @param responsibility Each component of this vector is a scalar telling
	  *   the probability that this density produced the corresponding datum.
	  * @param niter_max Maximum number of iterations of the update algorithm.
	  * @param stopping_criterion Stop when the change in negative log-
	  *   likelihood from one iteration to the next is smaller than this.
	  * @return Negative log-likelihood at end of iterations.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  * @see Density.update
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Gaussian.update: not implemented." );
	}

	public double p( double[] x ) { return 0; }

	/** Compute an instance of a random variable from this Gaussian.
	  * Generate a random vector N(0,I), then transform using mu and
	  * the (lower triangular) Cholesky decomposition of Sigma.
	  */
	public double[] random()
	{
		if ( L_Sigma == null )
			L_Sigma = Matrix.cholesky( Sigma );

		int i;
		double[] x = new double[ndims];

		java.util.Random rng = new java.util.Random();
		for ( i = 0; i < ndims; i++ )
			x[i] = (double) rng.nextGaussian();

		x = Matrix.add( Matrix.multiply( L_Sigma, x ), mu );
		return x;
	}

	/** Accessor functions for the covariance. These functions implement a 
	  * read-only access mechanism for Sigma, since it is important to make
	  * sure that the inverse and Cholesky decomposition are updated when Sigma
	  * is changed; this is handled by set_Sigma.
	  */
	void set_Sigma( double[][] Sigma_in )
	{
		Sigma = Matrix.copy( Sigma_in );
		Sigma_inverse = Matrix.inverse( Sigma );
		det_Sigma = Matrix.determinant( Sigma );
		L_Sigma = Matrix.cholesky( Sigma );
	}

	double[][] get_Sigma() { return Matrix.copy( Sigma ); }

	/** Make a copy of this Gaussian density. All member objects are likewise
	  * cloned.
	  * @returns A field by field copy of this object.
	  * @exception CloneNotSupportedException Thrown only if some member
	  *  object is not cloneable; should never be thrown.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Gaussian copy = new Gaussian();
		copy.ndims = ndims;
		copy.Sigma_inverse = Matrix.copy( Sigma_inverse );
		copy.det_Sigma = det_Sigma;
		copy.L_Sigma = Matrix.copy( L_Sigma );
		copy.Sigma = Matrix.copy( Sigma );
		copy.mu = (double[]) mu.clone();
		copy.mu_hat = (double[]) mu_hat.clone();
		copy.beta = (double[]) beta.clone();
		copy.gamma = (double[]) gamma.clone();
		copy.alpha = alpha;
		copy.eta = eta;

		return copy;
	}
}
