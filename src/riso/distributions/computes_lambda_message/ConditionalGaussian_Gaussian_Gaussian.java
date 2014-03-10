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
import riso.general.*;

public class ConditionalGaussian_Gaussian_Gaussian implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>ConditionalGaussian</tt>
	  * followed by one <tt>Gaussian</tt>, followed by any number of
	  * <tt>Gaussian</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.ConditionalGaussian", 1 );
		s[1] = new SeqTriple( "riso.distributions.Gaussian", 1 );
		s[2] = new SeqTriple( "riso.distributions.Gaussian", -1 );
		description_array = s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		ConditionalGaussian px = (ConditionalGaussian) px_in;

		int special_i = -1;

		for ( int i = 0; i < pi_messages.length; i++ )
			if ( pi_messages[i] == null )
			{
				special_i = i;
				break;
			}

		double[] a = px.a_mu_1c2[0];
		double b = px.b_mu_1c2[0];

		double lambda_msg_mu = (lambda.expected_value() - b)/a[special_i];
		double lambda_msg_sigma2 = (sqr(lambda.sqrt_variance()) + px.Sigma_1c2[0][0])/sqr(a[special_i]);

		for ( int i = 0; i < pi_messages.length; i++ )
		{
			if ( pi_messages[i] == null ) continue;

			double mu_i = pi_messages[i].expected_value(), sigma_i = pi_messages[i].sqrt_variance();

			lambda_msg_mu -= mu_i * a[i] / a[special_i];
			lambda_msg_sigma2 += sqr( sigma_i * a[i] / a[special_i] );
		}

		return new Gaussian( lambda_msg_mu, Math.sqrt(lambda_msg_sigma2) );
	}

	double sqr( double x ) { return x*x; }
}
