/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999--2002, Robert Dodier.
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
package riso.approximation;
import riso.distributions.*;
import riso.numerical.*;

public class CrossEntropyIntegrand implements Callback_1d
{
	Distribution target, approximation;
	double[] x1 = new double[1];

	public CrossEntropyIntegrand( Distribution target, Distribution approximation )
	{
		this.target = target;
		this.approximation = approximation;
	}

	/** Computes <tt>target.p(x) * log(approximation.p(x))</tt>.
	  * Returns zero if both densities are zero at the point of evaluation.
	  */
	public double f( double x ) throws Exception
	{
		x1[0] = x;
		double px = target.p(x1), qx = approximation.p(x1);

		if ( px == 0 && qx == 0 )
			return 0;
		else
			return -px*Math.log(qx);
	}
}

