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

public class Mixture
{
	/** The descriptive data which can be changed without causing the interface
	  * functions to break down is public. The other data is protected.
	  * Included in the public data are the regularization parameters. 
	  */

	/** Dimensionality of the space in which the density lives.
	  */
	protected int ndims;

	/** Mixing proportions; these must be nonnegative and sum to 1.
	  */
	public double[] mix_proportions;

	/** Regularization parameters.
	  */
	public double[] gamma;

	/** List of mixture components.
	  */
	public Density[] models;

	/** Return the dimensionality of the space in which the density lives.
	  */
	public int ndimensions() { return ndims; }

	/** Return the number of components in the mixture. Assume that none
	  * of the components is null.
	  */
	public int ncomponents() { return models.length; }

	/** Compute the density at the point <code>x</code>.
	  * This is the sum of the densities of each of the components,
	  * weighted by their respective mixing proportions.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x )
	{
		double sum = 0;
		for ( int i = 0; i < models.length; i++ )
			sum += mix_proportions[i] * models[i].p(x);

		return sum;
	}

	/** Return an instance of a random variable from this density.
	  * A component is selected according to the mixing proportions,
	  * then a random variable is generated from that component.
	  */
	public double[] random()
	{
		double sum = 0, r = Math.random();
		for ( int i = 0; i < models.length-1; i++ )
		{
			sum += mix_proportions[i];
			if ( r < sum )
				return models[i].random();
		}

		return models[models.length-1].random();
	}

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  */
	public int pretty_input( InputStream is )
	{
	}
}
