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
import riso.numerical.*;
import riso.general.*;

public class FunctionalRelation_AbstractDistribution_AbstractDistribution implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>FunctionalRelation</tt>
	  * followed by one <tt>AbstractDistribution</tt>, followed
	  * by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.FunctionalRelation", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[2] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** When the conditional distribution is a functional relation, every cross-section is a delta function,
	  * and the lambda message is equivalent to a predictive
	  * distribution calculated by substituting a delta function over the parent to which
	  * this message is being sent for the pi message from that parent. So attempt to locate
	  * an appropriate helper for that pi computation.
	  *
	  * <p> We might be called here when <tt>lambda</tt> is a delta function. If so, punt -- there is
	  * another helper better suited to that case.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages_in ) throws Exception
	{
		if ( lambda instanceof Delta )
			return (new AbstractConditionalDistribution_GaussianDelta_AbstractDistribution()).compute_lambda_message( pxu, lambda, pi_messages_in );

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
System.err.println( "compute_lambda_message: create pi_as_lambda; pxu, lambda, pi_helper: "+pxu.getClass()+", "+lambda.getClass()+", "+pi_helper.getClass() );
		PiAsLambda pi_as_lambda = new PiAsLambda( pxu, lambda, pi_messages, pi_helper, special_u_index, special_u_discrete, special_u_nstates );
				
		return pi_as_lambda;
	}

	class PiAsLambda extends AbstractDistribution
	{
		ConditionalDistribution pxu;
		Distribution lambda;
		Distribution[] pi_messages;
		int special_u_index;
		boolean special_u_discrete;
		int special_u_nstates;
		PiHelper pi_helper;

		public PiAsLambda( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages, PiHelper pi_helper, int special_u_index, boolean special_u_discrete, int special_u_nstates )
		{
System.err.println( "PiAsLambda: special_u_index, special_u_discrete, pi_helper: "+special_u_index+", "+special_u_discrete+", "+pi_helper.getClass() );
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
System.err.println( "PiAsLambda.p: pi_helper, pi: "+pi_helper.getClass()+", "+pi.getClass() );

			if ( pi instanceof Discrete )
			{
System.err.println( "PiAsLambda.p: EXECUTE UNTESTED CODE IN pi instanceof Discrete BRANCH !!!" );
				double sum = 0;
				double[] x = new double[1];

				for ( int i = 0; i < ((Discrete)pi).probabilities.length; i++ )
				{
					x[0] = i;
					sum += lambda.p(x) * pi.p(x);
				}

				return sum;
			}
			else if ( pi instanceof Delta )
			{
System.err.println( "PiAsLambda.p: pi instanceof Delta, lambda: "+lambda.getClass() );
				double[] x = ((Delta)pi).get_support(), x1 = new double[1];
				x1[0] = x[0];			// repackage support point as a 1-element arrray.
				return lambda.p(x1);
			}
			else
			{
				double[] pi_supt = pi.effective_support(1e-4);
				PiLambdaProduct plp = new PiLambdaProduct( pi, lambda );

				qags q = new qags();		// context for integration algorithm
				double[] result = new double[1], abserr = new double[1];
				int[] ier = new int[1];
				q.do_qags( plp, pi_supt[0], pi_supt[1], 1e-3, 1e-3, result, abserr, ier, 4 );	// set limit=4 !!!
System.err.println( "PiAsLambda.p: general case, pi: "+pi.getClass()+", u: "+u[0]+", int dx lambda(x) pi(x): "+result[0] );

				return result[0];
			}
		}

		public Object clone() throws CloneNotSupportedException
		{
			// These objects are immutable (HOW TO ENFORCE???) so cloning is trivial.

System.err.println( this.getClass()+".clone: return reference to this." );
			return this;
		}

		public double[] effective_support( double tolerance ) throws Exception
		{
			throw new SupportNotWellDefinedException( this.getClass()+".effective_support: refuse to compute support for a likelihood." );
		}
	}

	class PiLambdaProduct implements Callback_1d
	{
		Distribution lambda, pi;

		public PiLambdaProduct( Distribution pi, Distribution lambda ) { this.pi = pi; this.lambda = lambda; }

		public double f( double x ) throws Exception
		{
			double[] xx = new double[1];
			xx[0] = x;
			return lambda.p(xx)*pi.p(xx);
		}
	}
}
