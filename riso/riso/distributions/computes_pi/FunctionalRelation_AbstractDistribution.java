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
	  * followed by one <tt>AbstractDistribution</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.FunctionalRelation", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution pyx_in, Distribution[] pi_messages ) throws Exception
	{
		FunctionalRelation pyx = (FunctionalRelation) pyx_in;

		double[] supt_x = pi_messages[0].effective_support( SUPPORT_EPSILON ), x = new double[1];
		double[][] ypy = new double[ NGRID+1 ][2];
		Distribution px = pi_messages[0];

		// Let x range over the effective support of px, computing the corresponding p(y) as we go.
		// We'll sort on y afterwards.

		for ( int i = 0; i < NGRID+1; i++ )
		{
			x[0] = supt_x[0] + i*(supt_x[1]-supt_x[0])/(double)NGRID;
			ypy[i][0] = pyx.F(x);

			double[] xx = pyx.component_roots( ypy[i][0], 0, supt_x[0], supt_x[1], x );

			double sum = 0;
			for ( int j = 0; j < xx.length; j++ )
			{
				x[0] = xx[j];
				double[] grad = pyx.dFdx(x);
				if ( grad[0] == 0 )
					; // JUST OMIT THIS POINT !!! THERE IS A SINGULARITY HERE -- WHAT'S THE RIGHT THING TO DO ???
				else
					sum += px.p(x)/Math.abs(grad[0]);
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
