package riso.belief_nets;

import java.io.*;
import java.rmi.*;
import java.util.*;
import SmarterTokenizer;

public class UpdateCheck extends PathAnalysis
{
	public static Vector all_update_checks( AbstractBeliefNetwork bn, Vector existing_evidence, Vector new_evidence ) throws RemoteException
	{
		Enumeration e;

		// The evidence sets can be overlapping. Remove from the existing evidence set
		// any variable which is in the new evidence set.

		for ( e = new_evidence.elements(); e.hasMoreElements(); )
			existing_evidence.removeElement( e.nextElement() );

		System.err.print( "existing evidence (new evidence removed): " );
		for ( e = existing_evidence.elements(); e.hasMoreElements(); )
			System.err.print( ((AbstractVariable)e.nextElement()).get_name()+" " );
		System.err.println("");

		System.err.print( "new evidence: " );
		for ( e = new_evidence.elements(); e.hasMoreElements(); )
			System.err.print( ((AbstractVariable)e.nextElement()).get_name()+" " );
		System.err.println("");

		// For each variable x in bn, determine whether x is d-connected to some variable in the
		// new evidence set, given as evidence the existing evidence set.

		Vector to_update = new Vector();

		AbstractVariable[] u = bn.get_variables();
		for ( int i = 0; i < u.length; i++ )
		{
			if ( existing_evidence.contains( u[i] ) || new_evidence.contains( u[i] ) )
				continue;

			for ( Enumeration enew = new_evidence.elements(); enew.hasMoreElements(); )
			{
				AbstractVariable y = (AbstractVariable) enew.nextElement();
				if ( are_d_connected(  u[i], y, existing_evidence ) )
				{
					to_update.addElement( u[i] );
					break;
				}
			}
		}

		System.err.print( "variables to be updated: " );
		for ( e = to_update.elements(); e.hasMoreElements(); )
			System.err.print( ((AbstractVariable)e.nextElement()).get_name()+" " );
		System.err.println("");

		return to_update;
	}

	public static void main( String[] args )
	{
		String bn_name = "", context_name = "";
		Vector evidence_names = new Vector();

		for ( int i = 0; i < args.length; i++ )
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
			default:
					System.err.println( "PathAnalysis.main: "+args[i]+" -- huh???" );
			}
		}

		try
		{
			AbstractBeliefNetworkContext bnc;
			
			if ( "".equals(context_name) )
			{
				BeliefNetworkContext local_bnc = new BeliefNetworkContext(null);
				local_bnc.add_path( "/bechtel/users10/krarti/dodier/belief-nets/assorted" );
				bnc = local_bnc;
			}
			else
			{
				String url = "rmi://"+context_name;
				System.err.println( "MessageTest: url: "+url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( url );
			}

			AbstractBeliefNetwork bn = bnc.load_network( bn_name );
			Hashtable path_sets;

			Vector new_evidence = new Vector(), existing_evidence = new Vector();

			StreamTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			do
			{
				System.err.print( "give new evidence variables (terminate w/ period): " );

				existing_evidence = (Vector) new_evidence.clone();
				new_evidence.removeAllElements();

				for ( st.nextToken(); ! ".".equals(st.sval); st.nextToken() )
				{
					System.err.println( " -- add to new evidence: "+st.sval );

					String xname = st.sval;
					int pindex;

					try
					{
						AbstractVariable x;

						if ( (pindex = xname.indexOf(".")) != -1 )
						{
							// This evidence variable is in another network.
							String other_bn_name = xname.substring( 0, pindex );
							AbstractBeliefNetwork other_bn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_beliefnetwork(other_bn_name,bnc) );
							x = other_bn.name_lookup( xname.substring( pindex+1 ) );
						}
						else
						{
							x = bn.name_lookup( xname );
						}

						new_evidence.addElement( x );
					}
					catch (UnknownNetworkException e)
					{
						System.err.println( "UpdateCheck: can't locate variable "+xname+"; skip it." );
					}
				}

				all_update_checks( bn, existing_evidence, new_evidence );
			}
			while ( true );

			// System.exit(0);
		}
		catch (Exception e)
		{
			System.err.println( "PathAnalysis.main:" );
			e.printStackTrace();
			System.exit(1);
		}
	}
}
