package riso.apps;

import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import numerical.Format;

public class Informativeness
{
	public static void main( String[] args )
	{
		boolean all_x = false, do_rebind = false;
		String bn_name = "", context_name = "";
		int i;

		for ( i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_name = args[++i];
				break;
			case 'c':
				context_name = args[++i];
				break;
			case 'r':
				do_rebind = true;
				break;
			}
		}

		AbstractBeliefNetworkContext bnc = null;
		AbstractBeliefNetwork bn = null;
		AbstractVariable x = null, e_var = null;

		try
		{
			if ( "".equals(context_name) )
			{
				BeliefNetworkContext local_bnc = new BeliefNetworkContext();
				bnc = local_bnc;
			}
			else
			{
				String url = "rmi://"+context_name;
				System.err.println( "Informativeness: url: "+url );
				long t0 = System.currentTimeMillis();
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( url );
				long tf = System.currentTimeMillis();
				System.err.println( "Informativeness: Naming.lookup complete (for belief net context), elapsed time: "+((tf-t0)/1000.0)+" [s]" );
			}

			bn = (AbstractBeliefNetwork) bnc.load_network( bn_name );
			if ( do_rebind )
			{
				System.err.println( "Informativeness: rebind belief net." );
				bnc.rebind( bn );
			}

			String e_name, x_name;
			double e_value;

			for ( i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;

				switch ( args[i].charAt(1) )
				{
				case 'x':
					if ( "-xall".equals( args[i] ) )
					{
						AbstractVariable[] u = bn.get_variables();
						for ( int j = 0; j < u.length; j++ )
						{
							Distribution xposterior = bn.get_posterior( u[j] );
							System.out.println( "Informativeness: posterior for "+u[j].get_name()+":" );
							System.out.print( "  "+xposterior.format_string( "  " ) );
						}
					}
					else
					{
						x_name = args[++i];
						long t0 = System.currentTimeMillis();
						x = (AbstractVariable) bn.name_lookup( x_name );
						long tf = System.currentTimeMillis();
						System.err.println( "Informativeness: Naming.lookup complete (for variable ref), elapsed time: "+((tf-t0)/1000.0)+" [s]" );
						if ( x == null )
							throw new Exception( "name_lookup failed: x: "+x_name );
						Distribution xposterior = bn.get_posterior( x );
						System.out.println( "Informativeness: posterior for "+x.get_name()+":" );
						System.out.print( "  "+xposterior.format_string( "  " ) );
					}
					break;
				case 'e':
					if ( args[i].length() > 2 && args[i].charAt(2) == '-' )
					{
						e_name = args[++i];
						System.err.println( "Informativeness.main: evidence: clear "+e_name );
						e_var = (AbstractVariable) bn.name_lookup( e_name );
						bn.clear_posterior( e_var );
					}
					else
					{
						e_name = args[++i];
						e_value = Format.atof( args[++i] );
						System.err.println( "Informativeness.main: evidence: set "+e_name+" to "+e_value );

						long t0 = System.currentTimeMillis();
						e_var = (AbstractVariable) bn.name_lookup( e_name );
						long tf = System.currentTimeMillis();
						System.err.println( "Informativeness: Naming.lookup complete (for variable ref), elapsed time: "+((tf-t0)/1000.0)+" [s]" );
						bn.assign_evidence( e_var, e_value );
					}
					break;
				default:
					continue;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
