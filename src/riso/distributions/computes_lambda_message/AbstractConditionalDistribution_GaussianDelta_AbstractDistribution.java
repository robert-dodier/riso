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

public class AbstractConditionalDistribution_GaussianDelta_AbstractDistribution implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AbstractConditionalDistribution</tt>
	  * followed by one <tt>GaussianDelta</tt>, followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.GaussianDelta", 1 );
		s[2] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** When lambda is a delta function, the lambda message is equivalent to the predictive
	  * distribution calculated by substituting a delta function over the parent to which
	  * this message is being sent for the pi message from that parent. So attempt to locate
	  * an appropriate helper for that pi computation.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages_in ) throws Exception
	{
		Distribution[] pi_messages = (Distribution[]) pi_messages_in.clone();
		AbstractVariable[] parents = ((AbstractVariable)((AbstractConditionalDistribution)pxu).associated_variable).get_parents();

		int special_u_index = -1;
		boolean special_u_discrete = false;
		int special_u_nstates = -1;

		for ( int i = 0; i < pi_messages.length; i++ ) 
			if ( pi_messages[i] == null )
			{
				special_u_index = i;
				special_u_discrete = parents[i].is_discrete();
				special_u_nstates = parents[i].get_distribution().get_nstates();
				pi_messages[i] = (special_u_discrete ? ((Delta)new DiscreteDelta()) : ((Delta)new GaussianDelta()));
				break;
			}

        // IS CACHED HELPER MEANINGFUL HERE ???
		PiHelper pi_helper = PiHelperLoader.load_pi_helper( null, pxu, pi_messages );
		PiAsLambda pi_as_lambda = new PiAsLambda( pxu, (Delta)lambda, pi_messages, pi_helper, special_u_index, special_u_discrete, special_u_nstates );
				
		return pi_as_lambda;
	}

	class PiAsLambda extends AbstractDistribution
	{
		ConditionalDistribution pxu;
		Delta lambda;
		Distribution[] pi_messages;
		int special_u_index;
		boolean special_u_discrete;
		int special_u_nstates;
		PiHelper pi_helper;

		public PiAsLambda( ConditionalDistribution pxu, Delta lambda, Distribution[] pi_messages, PiHelper pi_helper, int special_u_index, boolean special_u_discrete, int special_u_nstates )
		{
System.err.println( "PiAsLambda: special_u_index, special_u_discrete, pi_helper: "+special_u_index+", "+special_u_discrete+", "+pi_helper.getClass().getName() );
			this.pxu = pxu;
			this.lambda = lambda;
			this.pi_messages = pi_messages;
			this.special_u_index = special_u_index;
			this.special_u_discrete = special_u_discrete;
			this.special_u_nstates = special_u_nstates;
			this.pi_helper = pi_helper;
		}

		public double p( double[] u ) throws Exception
		{
			if ( special_u_discrete )
			{
				int[] dimensions = new int[1], support_pt = new int[1];
				dimensions[0] = special_u_nstates;
				support_pt[0] = (int) u[0];
				pi_messages[special_u_index] = new DiscreteDelta( dimensions, support_pt );
			}
			else
				pi_messages[special_u_index] = new GaussianDelta(u);

			Distribution pi = pi_helper.compute_pi( pxu, pi_messages );
			double pp = pi.p( lambda.get_support() );
System.err.println( "PiAsLambda.p: pi: "+pi.getClass().getName()+", u: "+u[0]+", pi.p(x): "+pp );
			return pp;
		}

		public Object clone() throws CloneNotSupportedException
		{
			// These objects are immutable (HOW TO ENFORCE???) so cloning is trivial.

System.err.println( this.getClass().getName()+".clone: return reference to this." );
			return this;
		}

		public double[] effective_support( double tolerance ) throws Exception
		{
			throw new SupportNotWellDefinedException( this.getClass().getName()+".effective_support: refuse to compute support for a likelihood." );
		}
	}
}
