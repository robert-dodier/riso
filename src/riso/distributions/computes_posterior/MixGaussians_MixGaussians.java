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
import riso.distributions.*;
import riso.general.*;

public class MixGaussians_MixGaussians implements PosteriorHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>MixGaussians</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.MixGaussians", 2 );
		description_array = s;
	}

	/** @see PosteriorHelper.compute_posterior
	  * @see MixGaussians.mixture_product
	  */
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		MixGaussians[] mixtures = new MixGaussians[2];

		// Either pi or lambda could be a Gaussian; promote to MixGaussian.
		// Otherwise, the arguments must be MixGaussians.

		if ( pi instanceof Gaussian )
			mixtures[0] = new MixGaussians( (Gaussian)pi );
		else
			mixtures[0] = (MixGaussians) pi;

		if ( lambda instanceof Gaussian )
			mixtures[1] = new MixGaussians( (Gaussian)lambda );
		else
			mixtures[1] = (MixGaussians) lambda;

		MixGaussians product = MixGaussians.mixture_product( mixtures );

		if ( product.ncomponents() == 1 )
			return product.components[0];
		else
			return product;
	}
}
