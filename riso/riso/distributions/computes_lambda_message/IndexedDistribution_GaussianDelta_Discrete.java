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
package riso.distributions.computes_lambda_message;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.general.*;

public class IndexedDistribution_GaussianDelta_Discrete implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>IndexedDistribution</tt>
	  * one <tt>GaussianDelta</tt>, and one <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.IndexedDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.GaussianDelta", 1 );
		s[2] = new SeqTriple( "riso.distributions.Discrete", 1 );
		description_array = s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		IndexedDistribution px = (IndexedDistribution) px_in;
		px.check_components();

		double[] lambda_supt = ((GaussianDelta)lambda).get_support();

        int i_recipient;
		if ( pi_messages[0] == null ) // there are exactly 2 messages to check
            i_recipient = 0;
		else
			i_recipient = 1;

        // If the recipient (parent which requested the lambda message) is discrete,
        // the message is a discrete distribution, whatever the conditional distribution here.

        AbstractVariable[] parents = ((AbstractVariable)px.associated_variable).get_parents();
        AbstractVariable recipient = parents [i_recipient];

        if (recipient.is_discrete())
        {
            Discrete prior = (Discrete) recipient.get_bn().get_prior (recipient);
            Discrete lambda_message = (Discrete) prior.clone();     // has the right dimensions -- we'll set probabilities later.

            if (prior.dimensions.length > 1)
                throw new IllegalArgumentException ("IndexedDistribution_GaussianDelta_Discrete.compute_lambda_message: recipient has "+prior.dimensions.length+" dimensions; can't handle more than 1.");

            pi_messages = (Distribution[]) pi_messages.clone();     // don't write over original value.
            pi_messages [i_recipient] = new DiscreteDelta();        // DOES THIS WANT TO BE A FILLED-IN OBJECT ???

            // IS CACHED HELPER MEANINGFUL HERE ???
            PiHelper helper = PiHelperLoader.load_pi_helper (null, px, pi_messages);

if (riso.belief_nets.Global.debug > 0)
    System.err.println ("IndexedDistribution_GaussianDelta_Discrete.compute_lambda_message: found pi helper: "+helper.getClass());

            int[] recipient_value = new int[1];
            for (int i = 0; i < prior.dimensions[0]; i++)
            {
                recipient_value[0] = i;
                pi_messages [i_recipient] = new DiscreteDelta (prior.dimensions, recipient_value);
                Distribution pi = helper.compute_pi (px, pi_messages);
                double q = pi.p (lambda_supt);

if (riso.belief_nets.Global.debug > 0)
    System.err.println ("IndexedDistribution_GaussianDelta_Discrete.compute_lambda_message: recipient value: "+((DiscreteDelta)pi_messages[i_recipient]).support_point[0]+"; evaluate pi (type "+pi.getClass()+") at: "+lambda_supt[0]+" yielding "+q+"; description: "+pi.format_string(""));

                lambda_message.probabilities[i] = q;
            }

if (riso.belief_nets.Global.debug > 0)
    System.err.println ("IndexedDistribution_GaussianDelta_Discrete.compute_lambda_message: return lambda_message: "+lambda_message.format_string(""));

            return lambda_message;
        }

		// Check to see what kind of components the indexed distribution has.
		// If they're all conditional Gaussian, the lambda message is a mixture of Gaussians;
		// otherwise it's a general mixture.

		boolean all_gaussian = true;

		for ( int i = 0; i < px.components.length; i++ )
			if ( ! (px.components[i] instanceof ConditionalGaussian) )
			{
				all_gaussian = false;
				break;
			}

		Discrete pi_message;
		if ( pi_messages[0] != null ) // there are exactly 2 messages to check
			pi_message = (Discrete) pi_messages[0];
		else
			pi_message = (Discrete) pi_messages[1];

		Mixture lambda_message;
		if ( all_gaussian )
		{
			lambda_message = new MixGaussians( 1, pi_message.probabilities.length );
			for ( int i = 0; i < px.components.length; i++ )
			{
				ConditionalGaussian cg = (ConditionalGaussian) px.components[i];
				cg.check_matrices();
				double alpha = cg.a_mu_1c2[0][0];
				double beta = cg.b_mu_1c2[0];
				double sigma = Math.sqrt( cg.Sigma_1c2[0][0] );
				lambda_message.components[i] = new Gaussian( (lambda_supt[0]-beta)/alpha, sigma/alpha );
				lambda_message.mix_proportions[i] = pi_message.probabilities[i] / alpha;
			}
		}
		else
		{
			lambda_message = new Mixture( 1, pi_message.probabilities.length );
			for ( int i = 0; i < px.components.length; i++ )
			{
				if ( px.components[i] instanceof Distribution )
				{
					// This component is not conditional on the parent receiving the lambda message,
					// so throw in a Noninformative component into the mixture.
					double p = px.components[i].p( lambda_supt, null );
					lambda_message.components[i] = new Noninformative();
					lambda_message.mix_proportions[i] = p*pi_message.probabilities[i];

if (riso.belief_nets.Global.debug > 0)
    System.err.println( "IndexedDistribution_GaussianDelta_Discrete: throw in noninformative; p: "+p+", lambda_supt: "+lambda_supt[0]+", pimesg.prob["+i+"]: "+pi_message.probabilities[i] );
				}
				else if ( px.components[i] instanceof ConditionalGaussian )
				{
					ConditionalGaussian cg = (ConditionalGaussian) px.components[i];
					cg.check_matrices();
					double alpha = cg.a_mu_1c2[0][0];
					double beta = cg.b_mu_1c2[0];
					double sigma = Math.sqrt( cg.Sigma_1c2[0][0] );
					lambda_message.components[i] = new Gaussian( (lambda_supt[0]-beta)/alpha, sigma/alpha );
					lambda_message.mix_proportions[i] = pi_message.probabilities[i] / alpha;
				}
				else 
				{
					lambda_message.components[i] = new IntegralCache( px.components[i], lambda, new Distribution[1] );
					lambda_message.mix_proportions[i] = pi_message.probabilities[i];
				}
			}
		}

		lambda_message = Mixture.flatten(lambda_message);

		try { lambda_message = MixGaussians.convert_mixture(lambda_message); }
		catch (IllegalArgumentException e) {} // eat it; lambda_message has some non-Gaussian component -- that's OK.

if (riso.belief_nets.Global.debug > 0)
    System.err.println( "compute_lambda_message: return lambda message:" );

		if ( lambda_message.components.length == 1 )
        {
if (riso.belief_nets.Global.debug > 0)
    System.err.println( lambda_message.components[0].format_string("  ") );
			return lambda_message.components[0];
        }
		else
        {
if (riso.belief_nets.Global.debug > 0)
    System.err.println( lambda_message.format_string("  ") );
			return lambda_message;
        }
	}
}
