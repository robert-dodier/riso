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
import numerical.*;

/** class Gaussian, a Gaussian (normal) density.
  */

public class Gaussian implements Density, Serializable, Cloneable
{
	/** The descriptive data which can be changed without causing the interface
	  * functions to break down is public. The other data is protected.
	  * Included in the public data are the regularization parameters. 
	  */

	/** Dimensionality of the space in which the density lives.
	  */
	protected int ndims;

	/** Mean vector of the density.
	  */
	public double[] mu;

	/** Covariance matrix of the density.
	  */
	protected double[][] Sigma;

	protected double[][] Sigma_inverse;
	protected double det_Sigma;
	protected double[][] L_Sigma;

	/** Regularization parameters. <code>beta</code> is supposed to be a matrix,
	  * but we assume it's diagonal, and just store the diagonal.
	  * The flag <code>is_regularized</code> tells whether or not to pay
	  * attention to regularization parameters. 
	  */
	public boolean is_regularized;
	public double[] mu_hat, beta, gamma;
	public double alpha, eta;

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

	public int pretty_input( InputStream is ) { return 0; }

	/** Print the data necessary to reconstruct this Gaussian. The inverse and
	  * Cholesky decomposition of the covariance are not printed. If the flag
	  * is_regularized is false, the regularization parameters aren't printed either.
	  * The leading_ws argument is printed at the start of every line of output.
	  */
	public int pretty_output( OutputStream os, String leading_ws )
	{
		return 0;
	}

	public boolean update( double[][] x, boolean[] is_present, double[] responsibility ) { return false; }

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
