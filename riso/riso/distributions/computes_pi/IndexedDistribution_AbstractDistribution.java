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
package riso.distributions.computes_pi;
import java.util.*;
import riso.distributions.*;
import riso.general.*;

/** @see PiHelper
  */
public class IndexedDistribution_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>IndexedDistribution</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.IndexedDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** The pi distribution for this node is simply
	  * a mixture of the components, with the mixture coefficients equal to
	  * the product of the probabilities in the discrete pi-messages. Compute the
	  * mixture coefficients first, and count how many are non-zero; then 
	  * create a mixture with that many components. As an important special
	  * case, a discrete pi-message may represent evidence, and so all of the elements
	  * in its probability vector will be zero, except for one element.
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		IndexedDistribution py = (IndexedDistribution) py_in;
		py.check_components();

		int m = 0;
		int[] ii = new int[ py.indexes.length ];
		int nnonzero = count_nonzero( py, pi_messages, ii, m );

		Mixture pye = new Mixture( 1, nnonzero );
		ii = new int[ py.indexes.length ];
		int[] nmix = new int[1], npy = new int[1];
		assign_components( pi_messages, ii, m, py, pye, nmix, npy );

		pye = Mixture.flatten(pye);

		try { pye = MixGaussians.convert_mixture(pye); }
		catch (IllegalArgumentException e) {} // eat it; pye has some non-Gaussian component -- that's OK.

		if ( pye.components.length == 1 ) return pye.components[0];
		return pye;
	}

	int count_nonzero( IndexedDistribution py, Distribution[] pi_messages, int[] ii,  int m )
	{
		if ( m == py.indexes.length )
		{
			for ( int i = 0; i < py.indexes.length; i++ )
			{
				Discrete pimsg = (Discrete) pi_messages[ py.indexes[i] ];
				if ( pimsg.probabilities[ ii[i] ] == 0 )
					return 0;
			}
			return 1;
		}
		else
		{
			int sum = 0;
			Discrete pimsg = (Discrete) pi_messages[ py.indexes[m] ];
			for ( ii[m] = 0; ii[m] < pimsg.probabilities.length; ii[m]++ )
				sum += count_nonzero( py, pi_messages, ii, m+1 );
			return sum;
		}
	}

	void assign_components( Distribution[] pi_messages, int[] ii, int m, IndexedDistribution py, Mixture pye, int[] nmix, int[] npy ) throws Exception
	{
System.err.println( "m: "+m );
		if ( m == py.indexes.length )
		{
			double alpha = 1;
			for ( int i = 0; i < py.indexes.length; i++ )
			{
				Discrete pimsg = (Discrete) pi_messages[ py.indexes[i] ];
				alpha *= pimsg.probabilities[ ii[i] ];
			}
System.err.print( "ii: " ); for (int j=0;j<ii.length;j++) System.err.print(ii[j]+" "); System.err.println("");
System.err.println("alpha: "+alpha);
			if ( alpha == 0 ) { ++npy[0]; return; }

System.err.println( "nmix: "+nmix[0]+"  npy: "+npy[0] );
			
			// Some components may be Distribution (i.e., not conditional). We don't need to load a
			// pi helper for those.

			if ( py.components[npy[0]] instanceof Distribution )
			{
				pye.components[nmix[0]] = (Distribution) py.components[npy[0]].clone();
			}
			else
			{
				// For a given combination of indexes, we need to compute pi from a ConditionalDistribution
				// and the incoming non-index pi messages. Load an appropriate pi helper and compute pi.
			
				Distribution[] nonindex_pi_msgs = new Distribution[ py.non_indexes.length ];
				for ( int i = 0; i < py.non_indexes.length; i++ ) 
					nonindex_pi_msgs[i] = pi_messages[ py.non_indexes[i] ];

                // IS CACHED HELPER MEANINGFUL HERE ???
				PiHelper ph = PiHelperLoader.load_pi_helper( null, py.components[npy[0]], nonindex_pi_msgs );
				pye.components[nmix[0]] = ph.compute_pi( py.components[npy[0]], nonindex_pi_msgs );
			}

			pye.mix_proportions[nmix[0]] = alpha;
			++nmix[0];
			++npy[0];
		}
		else
		{
			Discrete pimsg = (Discrete) pi_messages[ py.indexes[m] ];
			for ( ii[m] = 0; ii[m] < pimsg.probabilities.length; ii[m]++ )
				assign_components( pi_messages, ii, m+1, py, pye, nmix, npy );
		}
	}
}
