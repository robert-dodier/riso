package riso.apps;

import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;

public class PublishNetwork
{
	public static void main( String[] args )
	{
		String context_name = "";
		Vector bn_names = new Vector();

		for ( int i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_names.addElement( args[++i] );
				break;
			case 'c':
				context_name = args[++i];
				break;
			}
		}

		AbstractBeliefNetworkContext bnc = null;
		AbstractBeliefNetwork bn = null;

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
				System.err.println( "PublishNetwork: url: "+url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( url );
			}

			for ( int i = 0; i < bn_names.size(); i++ )
			{
				String bn_name = (String) bn_names.elementAt(i);
				bn = (AbstractBeliefNetwork) bnc.load_network( bn_name );
				System.err.println( "PublishNetwork: rebind belief net: "+bn.get_fullname() );
				bnc.rebind( bn );
			}
		}
		catch (Exception e)
		{
			System.err.println( "PublishNetwork: attempt failed: "+e );
			e.printStackTrace();
			System.exit(1);
		}

		// System.exit(0);
	}
}
