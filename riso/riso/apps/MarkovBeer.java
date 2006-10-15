/* MarkovBeer.java -- a Markov chain in which the state is the number of bottles.
 * Copyright (C) 2003, Robert Dodier.
 * You can redistribute this program and/or modify it under the terms of
 * Version 2 of the GNU General Public License as published by the Free Software Foundation.
 */

package riso.apps;

import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;

public class MarkovBeer
{
	public static void main( String[] args )
	{
		Remote remote;
		AbstractTemporalBeliefNetwork tbn;
		AbstractBeliefNetwork bn;
		AbstractVariable x;

		double[] jj = new double[1];
		double[] z = new double[1];
		z[0] = 0;

		try
		{
			int n = 100;

			String s = args.length == 1 ? args[0] : "chain-100state-determinate";
			remote = Naming.lookup( "rmi://localhost/"+s );
			tbn = (AbstractTemporalBeliefNetwork) remote;
			
			for ( int i = 0; i < n; i++ )
				tbn.create_timeslice(i);

			remote = tbn.name_lookup( "slice[0]" );
			bn = (AbstractBeliefNetwork) remote;
			x = (AbstractVariable) bn.name_lookup("x");
			bn.assign_evidence( x, n-1 );

			System.err.println( "Exactly "+(n-1)+" bottles of beer on the wall," );
			System.err.println( "Exactly "+(n-1)+" bottles of beer;" );

			System.err.println( "Take one or more down, pass them around," );

			for ( int i = 1; i < n; i++ )
			{
				remote = tbn.name_lookup( "slice["+i+"]" );
				bn = (AbstractBeliefNetwork) remote;
				x = (AbstractVariable) bn.name_lookup("x");
				Distribution p = bn.get_posterior(x);

				double mean = p.expected_value();
				double sdev = p.sqrt_variance();
				double min_x = 0, max_x = n-1;

				for ( int j = 0; j < n; j++ )
				{
					jj[0] = j;
					if ( p.p(jj) != 0 )
					{
						min_x = j;
						break;
					}
				}

				for ( int j = n-1; j >= 0; j-- )
				{
					jj[0] = j;
					if ( p.p(jj) != 0 )
					{
						max_x = j;
						break;
					}
				}

				if ( sdev == 0 )
					System.err.println( "Exactly "+mean+" bottles of beer on the wall." );
				else
					System.err.println( "Approximately "+mean+" bottles of beer on the wall." );

				System.err.println("");

				if ( sdev == 0 )
				{
					System.err.println( "Exactly "+mean+" bottles of beer on the wall," );
					System.err.println( "Exactly "+mean+" bottles of beer;" );
				}
				else
				{
					System.err.println( "Approximately "+mean+" bottles of beer on the wall," );
					System.err.println( "Maybe as few as "+min_x+" or as many as "+max_x+";" );
				}

				if ( p.p(z) > 0.99 )
				{
					if ( p.p(z) == 1 )
					{
						System.err.println( "Go to the store and buy some more," );
						System.err.println( "No more bottles of beer on the wall." );
					}
					else
					{
						System.err.println( "We're almost certainly out of beer;" );
						System.err.println( "could be as many as "+max_x+", but that's just wishful thinking." );
					}

					break;
				}

				System.err.println( "Take one or more down, pass them around," );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
