/* MarkovBeer.java -- a Markov chain in which the state is the number of bottles.
 * Copyright (C) 2003, Robert Dodier.
 * You can redistribute this program and/or modify it under the terms of
 * Version 2 of the GNU General Public License as published by the Free Software Foundation.
 */
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

		double[] z = new double[1];
		z[0] = 0;

		try
		{
			int n = 100;

			remote = Naming.lookup( "rmi://localhost/chain-100state" );
			tbn = (AbstractTemporalBeliefNetwork) remote;
			
			for ( int i = 0; i < n; i++ )
				tbn.create_timeslice(i);

			remote = Naming.lookup( "rmi://localhost/chain-100state.slice[0]" );
			bn = (AbstractBeliefNetwork) remote;
			x = (AbstractVariable) bn.name_lookup("x");
			bn.assign_evidence( x, n-1 );

			for ( int i = 0; i < n; i++ )
			{
				remote = Naming.lookup( "rmi://localhost/chain-100state.slice["+i+"]" );
				bn = (AbstractBeliefNetwork) remote;
				x = (AbstractVariable) bn.name_lookup("x");
				Distribution p = bn.get_posterior(x);

				double mean = p.expected_value();
				double sdev = p.sqrt_variance();
				double[] support = p.effective_support( 1e-12 );

				if ( sdev == 0 )
				{
					System.err.println( "Exactly "+mean+" bottles of beer on the wall," );
					System.err.println( "Exactly "+mean+" bottles of beer;" );
				}
				else
				{
					System.err.println( "Approximately "+mean+" bottles of beer on the wall," );
					System.err.println( "Maybe as few as "+support[0]+" or as many as "+support[1]+";" );
				}

				System.err.println( "Take one or more down, pass them around," );

				if ( sdev == 0 )
					System.err.println( "Exactly "+mean+" bottles of beer on the wall." );
				else
					System.err.println( "Approximately "+mean+" bottles of beer on the wall." );

				if ( p.p(z) > 0.99 )
				{
					if ( p.p(z) == 1 )
						System.err.println( "We're all out, send somebody to the store." );
					else
					{
						System.err.println( "We're almost certainly out of beer;" );
						System.err.println( "could be as many as "+support[1]+", but that's just wishful thinking." );
					}

					break;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
