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
package riso.test;
import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import riso.belief_nets.*;
import riso.regression.*;
import riso.general.*;

public class IndexedDistributionTest
{
	public static void main( String[] args )
	{
		try
		{
			System.err.println( "bn: "+args[0]+", variable: "+args[1] );
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			BeliefNetwork bn = (BeliefNetwork) bnc.load_network( args[0] );
			bnc.rebind(bn);
			AbstractVariable rcs = (AbstractVariable) bn.name_lookup( args[1] );

			IndexedDistribution id = (IndexedDistribution) rcs.get_distribution();

			double[] x = new double[1];

			for ( double theta = 0; theta < 6.28; theta += 0.1 )
			{
				System.out.print( theta+"\t" );
				
				x[0] = theta;
				for ( int i = 0; i < 3; i++ )
				{
					RegressionModel rm = ((RegressionDensity)id.components[i]).regression_model;
					System.out.print( rm.F(x)[0]+"  " );
				}
				System.out.println("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
