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
public class IndexedDistribution_Discrete implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>IndexedDistribution</tt>
	  * followed by any number of <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.IndexedDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.Discrete", -1 );
		description_array = s;
	}

    class component_pair { Distribution upd; double alpha; }

	/** All incoming pi-messages are discrete, so this node must not have any
	  * continuous parents. Thus the pi distribution for this node is simply
	  * a mixture of the components, with the mixture coefficients equal to
	  * the product of the probabilities in the pi-messages. Compute the
	  * mixture coefficients first, and count how many are non-zero; then 
	  * create a mixture with that many components. As an important special
	  * case, a pi-message may represent evidence, and so all of the elements
	  * in its probability vector will be zero, except for one element.
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
if (riso.belief_nets.Global.debug > 0)
{
    System.err.println ("IndexedDistribution_Discrete.compute_pi: pi_messages: ");
    for (int i = 0; i < pi_messages.length; i++)
        System.err.print (pi_messages[i] != null ? pi_messages[i].format_string("") : "(null)");
}

		IndexedDistribution py = (IndexedDistribution) py_in;
		py.check_components();

		int m = 0;
		int[] ii = new int[ py.indexes.length ];
        Vector components = new Vector();

		assign_components (pi_messages, ii, m, py, components);

		Mixture pye = new Mixture (1, components.size());
        for (int i = 0; i < components.size(); i++)
        {
            component_pair cp = (component_pair) components.elementAt(i);
            pye.components[i] = cp.upd;
            pye.mix_proportions[i] = cp.alpha;
        }

		pye = Mixture.flatten(pye);

		try { pye = MixGaussians.convert_mixture(pye); }
		catch (IllegalArgumentException e) {} // eat it; pye has some non-Gaussian component -- that's OK.

if (riso.belief_nets.Global.debug > 0)
    System.err.println ("IndexedDistribution_Discrete.compute_pi: pye.components.length: "+pye.components.length);

		if ( pye.components.length == 1 ) return pye.components[0];
		else return pye;
	}

	void assign_components (Distribution[] pi_messages, int[] ii, int m, IndexedDistribution py, Vector components) throws Exception
	{
		if ( m == py.indexes.length )
		{
			double alpha = 1;
			for ( int i = 0; i < py.indexes.length; i++ )
			{
				Discrete pimsg = (Discrete) pi_messages[ py.indexes[i] ];
				alpha *= pimsg.probabilities[ ii[i] ];
			}

if (riso.belief_nets.Global.debug > 0)
{
    System.err.print ("IndexedDistribution_Discrete.assign_components: ii: ");
    for (int i = 0; i < ii.length; i++)
        System.err.print (ii[i]+" "); System.err.println("; alpha: "+alpha);
}

			if ( alpha == 0 ) return;

            int i_linear = convert_to_linear_index (ii, pi_messages);
if (riso.belief_nets.Global.debug > 0)
    System.err.println ("IndexedDistribution_Discrete.assign_components: i_linear: "+i_linear);
            Object d = py.components [i_linear];

            if (d instanceof Distribution)
            {
                d = ((Distribution)d).clone();
            }
            else
            {
                // d must be a conditional distribution. 
                // Compute pi for this distribution, using pi messages other than
                // the ones associated with index parents.

                Distribution[] nonindex_pi_messages = new Distribution [py.non_indexes.length];

                for ( int i = 0; i < py.non_indexes.length; i++ )
                    nonindex_pi_messages[i] = pi_messages[ py.non_indexes[i] ];

                ConditionalDistribution cpd = (ConditionalDistribution) d;

                // IS CACHED HELPER MEANINGFUL HERE ???
                PiHelper helper = PiHelperLoader.load_pi_helper( null, cpd, nonindex_pi_messages );

                d = helper.compute_pi (cpd, nonindex_pi_messages);
            }

if (riso.belief_nets.Global.debug > 0)
    System.err.println ("IndexedDistribution_Discrete.assign_components: make component with alpha: "+alpha+", description: "+((Distribution)d).format_string("\t"));

            component_pair cp = new component_pair();
            cp.upd = (Distribution) d;
            cp.alpha = alpha;
            components.addElement (cp);
		}
		else
		{
			Discrete pimsg = (Discrete) pi_messages[ py.indexes[m] ];
			for ( ii[m] = 0; ii[m] < pimsg.probabilities.length; ii[m]++ )
				assign_components (pi_messages, ii, m+1, py, components);
		}
	}

    /** Flatten tuple into a single index: compute addressing polynomial.
      * Assume row major ordering. Determine dimensions from <tt>pi_messages</tt>.
      * For now, assume all pi messages are 1-dimensional; should relax that later. !!!
      */
    int convert_to_linear_index (int[] ii, Distribution[] pi_messages)
    {
        int i_linear = 0;

        for (int i = 0; i < ii.length; i++)
        {
            Discrete d = (Discrete) pi_messages[i];
if (d.dimensions.length > 1) throw new IllegalArgumentException ("IndexedDistribution_Discrete.convert_to_linear_index: pi_messages["+i+"].dimensions.length == "+d.dimensions.length+" > 1; oops.");   // !!!
            i_linear = i_linear * d.probabilities.length + ii[i];
        }

        return i_linear;
    }
}
