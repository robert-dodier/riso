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
import java.rmi.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** @see PiHelper
  */
public class AR1_Gaussian implements PiHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AR1</tt>
	  * followed by three <tt>Gaussian</tt>, one for the correlation
	  * coefficient, one for the noise magnitude, and one for the previous
	  * state.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AR1", 1 );
		s[1] = new SeqTriple( "riso.distributions.Gaussian", 3);
		return s;
	}

	/** This problem is easiest if rho and sigma are deltas; then the
	  * distribution of the state is a simple conditional Gaussian.
	  * BAIL OUT IF SITUATION IS NOT SIMPLE!!!
	  */
	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi_msgs ) throws Exception
	{
		AR1 ar1 = (AR1) pxu;
		ar1.assign_parents();

		if ( pi_msgs[ar1.rho_parent_index] instanceof GaussianDelta && pi_msgs[ar1.sigma_parent_index] instanceof GaussianDelta )
		{
			double rho = pi_msgs[ar1.rho_parent_index].expected_value();
			double sigma = pi_msgs[ar1.sigma_parent_index].expected_value();
			double E_x = pi_msgs[ar1.prev_parent_index].expected_value();
			double sigma_x = pi_msgs[ar1.prev_parent_index].sqrt_variance();

			return new Gaussian( rho*E_x, Math.sqrt( sigma*sigma + rho*rho*sigma_x*sigma_x ) );
		}

		System.err.println( "AR1_Gaussian.compute_pi: can't handle "+pi_msgs[0].getClass().getName()+", "+pi_msgs[1].getClass().getName()+", "+pi_msgs[2].getClass().getName()+"; punt." );
		PiHelper ph = new AbstractConditionalDistribution_AbstractDistribution();
		return ph.compute_pi( pxu, pi_msgs );
	}
}
