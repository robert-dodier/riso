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
import riso.distributions.*;
import SeqTriple;

public class AR1_Gaussian_Gaussian implements LambdaMessageHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AR1</tt>
	  * followed by one <tt>Gaussian</tt>, followed by two
	  * <tt>Gaussian</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.AR1", 1 );
		s[1] = new SeqTriple( "riso.distributions.Gaussian", 1 );
		s[2] = new SeqTriple( "riso.distributions.Gaussian", 2 );
		return s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi ) throws Exception
	{
		AR1 ar1 = (AR1) px_in;
		ar1.assign_parents();

		if ( pi[ar1.rho_parent_index] instanceof GaussianDelta && pi[ar1.sigma_parent_index] instanceof GaussianDelta )
		{
			double rho = pi[ar1.rho_parent_index].expected_value();
			double sigma = pi[ar1.sigma_parent_index].expected_value();

			double lambda_msg_mu = lambda.expected_value()/rho;
			double lambda_msg_sigma2 = (sqr(lambda.sqrt_variance()) + sqr(sigma))/sqr(rho);
			return new Gaussian( lambda_msg_mu, Math.sqrt(lambda_msg_sigma2) );
		}

		System.err.println( "AR1_Gaussian.compute_lambda_message: can't handle "+pi[ar1.rho_parent_index].getClass().getName()+" and "+pi[ar1.sigma_parent_index].getClass().getName()+"; punt." );
		LambdaMessageHelper lmh = new AbstractConditionalDistribution_AbstractDistribution_AbstractDistribution();
		return lmh.compute_lambda_message( px_in, lambda, pi );
	}

	double sqr( double x ) { return x*x; }
}
