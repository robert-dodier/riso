/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions.computes_posterior;
import riso.approximation.*;
import riso.distributions.*;
import riso.general.*;

public class Mixture_Mixture implements PosteriorHelper
{
	public static double MIN_MIX_PROPORTION = 5e-3;

    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>Mixture</tt>'s.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Mixture", 2 );
		description_array = s;
	}

	/** @see PosteriorHelper.compute_posterior
	  */
	public Distribution compute_posterior( Distribution pi_in, Distribution lambda_in ) throws Exception
	{
		Mixture pi, lambda;

		if ( pi_in instanceof Mixture ) pi = Mixture.flatten( (Mixture)pi_in );
		else { pi = new Mixture(1,1); pi.components[0] = pi_in; }
			
		if ( lambda_in instanceof Mixture ) lambda = Mixture.flatten( (Mixture)lambda_in );
		else { lambda = new Mixture(1,1); lambda.components[0] = lambda_in; }

		// pi and lambda are now both flat, and both mixtures.

		Mixture pxe = new Mixture( 1, pi.ncomponents()*lambda.ncomponents() );

		for ( int ii = 0, i = 0; i < pi.ncomponents(); i++ )
		{
			for ( int j = 0; j < lambda.ncomponents(); j++ )
			{
				// Compute one product of a component of pi times a component of lambda.
				// In general, one product can be computed by a posterior helper, but for the sake
				// of speed, handle some special cases here. There are no components which are
				// Mixture or MixGaussians, since we flattened pi and lambda at the outset.

				if ( lambda.components[j] instanceof Noninformative )
				{
					try { pxe.components[ii] = (Distribution) pi.components[i].clone(); }
					catch (CloneNotSupportedException e) { pxe.components[ii] = pi.components[i]; } // !!!
					pxe.mix_proportions[ii] = pi.mix_proportions[i] * lambda.mix_proportions[j];
					++ii;
				}
				else if ( pi.components[i] instanceof Gaussian && lambda.components[j] instanceof Gaussian )
				{
					Gaussian[] g2 = new Gaussian[2];
					g2[0] = (Gaussian) pi.components[i];
					g2[1] = (Gaussian) lambda.components[j];
					double[] ignored_scale = new double[1];
					
					pxe.components[ii] = Gaussian.densities_product( g2, ignored_scale );
					pxe.mix_proportions[ii] = ignored_scale[0] * pi.mix_proportions[i] * lambda.mix_proportions[j];
					++ii;
				}
				else // Here we give up on special cases and go to a very general case.
				{
					Distribution[] dd = new Distribution[2];
					dd[0] = pi.components[i];
					dd[1] = lambda.components[j];
					double[] mass = new double[1];

					DistributionProduct dp = new DistributionProduct( false, false, dd );
					pxe.components[ii] = SplineApproximation.do_approximation( (Distribution)dp, dp.support, mass );
					pxe.mix_proportions[ii] = mass[0] * pi.mix_proportions[i] * lambda.mix_proportions[j];
					++ii;
				}
			}
		}

		// Prune the posterior, convert to mixture of Gaussians if possible, return.
		// First we need to normalize the mixing proportions to sum to 1.

		double sum = 0;
		for ( int i = 0; i < pxe.ncomponents(); i++ ) sum += pxe.mix_proportions[i];
		for ( int i = 0; i < pxe.ncomponents(); i++ ) pxe.mix_proportions[i] /= sum;

		try { pxe = MixGaussians.convert_mixture(pxe); }
		catch (IllegalArgumentException e) {} // eat it; posterior has some non-Gaussian component -- that's OK.

		if ( pxe.components.length == 1 )
			return pxe.components[0];
		else
			return pxe;
	}
}
