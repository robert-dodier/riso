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
import Comparator;
import ShellSort;
import SeqTriple;

/** @see PiHelper
  */
public class FunctionalRelation_AbstractDistribution implements PiHelper
{
	public static int NGRID = 256;
	public static double SUPPORT_EPSILON = 1e-4;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>FunctionalRelation</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.FunctionalRelation", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution pyx_in, Distribution[] pi_messages ) throws Exception
	{
		FunctionalRelation pyx = (FunctionalRelation) pyx_in;

		// How best to go about this depends on how many non-delta pi messages we have.
		// If only one, use the existing code in FunctionalRelation_AbstractDistribution.
		// If two, we need to integrate numerically over the second non-delta; use qk21.
		// Otherwise, we need two or more numerical integrations; use quasi Monte Carlo.

		int ndelta = 0;
		for ( int i = 0; i < pi_messages.length; i++ ) if ( pi_messages[i] instanceof Delta ) ++ndelta;

		if ( pi_messages.length - ndelta == 0 )
			// Well, this is a little strange -- another helper should have picked this up.
			return (new FunctionalRelation_GaussianDelta()).compute_pi( pyx, pi_messages );
		else if ( pi_messages.length - ndelta == 1 )
			return compute_pi_1nondelta( pyx, pi_messages );
		else if ( pi_messages.length - ndelta == 2 )
			return compute_pi_2nondelta( pyx, pi_messages );
		else
			return compute_pi_many_nondelta( pyx, pi_messages );
	}

	public Distribution compute_pi_many_nondelta( FunctionalRelation pyx, Distribution[] pi_messages ) throws Exception
	{
		return null;
	}

	public Distribution compute_pi_2nondelta( FunctionalRelation pyx, Distribution[] pi_messages ) throws Exception
	{
		return null;
	}

	/** Figure out which pi-message is non-delta, then use the usual inversion formula
	  * <pre>
	  *   p_y(y) = sum_{x \in X*} p_x(x)/|dF/dx(x)|
	  * </pre>
	  * where <tt>X*</tt> is the set of roots of the equation <tt>y=F(x)</tt>.
	  */
	public Distribution compute_pi_1nondelta( FunctionalRelation pyx, Distribution[] pi_messages ) throws Exception
	{
		int k = -1;
		for ( int i = 0; i < pi_messages.length; i++ )
			if ( ! (pi_messages[i] instanceof Delta) ) 
			{
				k = i;
				break;
			}

		return inversion_formula( pyx, pi_messages, k );
	}

	public Distribution inversion_formula( FunctionalRelation pyx, Distribution[] pi_messages, int k ) throws Exception
	{
		double[] xk_supt = pi_messages[k].effective_support( SUPPORT_EPSILON ), x = new double[pi_messages.length];
		double[][] ypy = new double[ NGRID+1 ][2];
		Distribution px = pi_messages[k];

		// Let x range over the effective support of px, computing the corresponding p(y) as we go.
		// We'll sort on y afterwards.

		for ( int i = 0; i < NGRID+1; i++ )
		{
			x[k] = xk_supt[0] + i*(xk_supt[1]-xk_supt[0])/(double)NGRID;
			ypy[i][0] = pyx.F(x);

			double[] xx = pyx.component_roots( ypy[i][0], k, xk_supt[0], xk_supt[1], x );

			double sum = 0;
			for ( int j = 0; j < xx.length; j++ )
			{
				x[k] = xx[j];
				double[] grad = pyx.dFdx(x);
				if ( grad[k] == 0 )
					; // JUST OMIT THIS POINT !!! THERE IS A SINGULARITY HERE -- WHAT'S THE RIGHT THING TO DO ???
				else
					sum += px.p(x)/Math.abs(grad[k]);
			}

			ypy[i][1] = sum; // if no roots were found, sum is still zero.
		}

		// Sort by y. Remove any duplicate y's -- take the average of the corresponding p(y).

		ShellSort.do_sort( (Object[])ypy, 0, ypy.length-1, new PairComparator() );

		int ndistinct = 1;
		for ( int i = 1; i < ypy.length; i++ ) if ( ypy[i][0] != ypy[i-1][0] ) ++ndistinct;

		double[] y = new double[ndistinct], py = new double[ndistinct];

		y[0] = ypy[0][0];
		double py_sum = ypy[0][1];
		int n = 1, j = 0;
		
		for ( int i = 1; i < ypy.length; i++ )
		{
			if ( ypy[i][0] != ypy[i-1][0] )
			{
				py[j] = py_sum/n; // fix up py for duplicated y value -- works right if n == 1.
				++j;
				y[j] = ypy[i][0];

				py_sum = ypy[i][1];
				n = 1;
			}
			else
			{
				py_sum += ypy[i][1];
				++n;
			}
		}

		// Fix up final y, py pair.
		py[j] = py_sum/n;

		return new SplineDensity( y, py );
	}

	class PairComparator implements Comparator
	{
		public boolean greater( Object a, Object b )
		{
			return ((double[])a)[0] > ((double[])b)[0]; // sort on first column
		}
	}
}
