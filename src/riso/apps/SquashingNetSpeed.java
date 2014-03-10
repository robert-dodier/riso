/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2001, Robert Dodier.
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

package riso.apps;
import riso.regression.SquashingNetwork;

public class SquashingNetSpeed
{
	public static void main( String args[] )
	{
		try
		{
			int nin = 50, nhidden = 10, noutput = 1;
			SquashingNetwork net = new SquashingNetwork( nin, nhidden, noutput );

			double[] x = new double[nin];
			for ( int i = 0; i < nin; i++ ) x[i] = i;

			int ndata = 1000000;

			long t0 = System.currentTimeMillis();

			for ( int i = 0; i < ndata; i++ )
			{
				net.F(x);
			}

			long t1 = System.currentTimeMillis();

			System.err.println( "elapsed: "+(t1-t0)/1000.0+" [s]" );
			System.err.println( (t1-t0)*1000.0/ndata+" microseconds per net.F(x)" );
		}
		catch (Exception e)
		{
			System.err.println( "exception: "+e );
			System.exit(1);
		}
	}
}
