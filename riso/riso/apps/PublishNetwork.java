package riso.apps;

import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import GlobalDirectory;

public class PublishNetwork
{
	public static Vector souvenirs = new Vector();

	public static void main( String[] args )
	{
		String context_name = "";
		Vector bn_names = new Vector();
		String directory_name = "sonero/global-directory";

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
			case 'd':
				directory_name = args[++i];
				break;
			}
		}

		AbstractBeliefNetworkContext bnc = null;
		AbstractBeliefNetwork bn = null;

		try
		{
			String directory_url = "rmi://"+directory_name;
			GlobalDirectory directory = null;
			
			try { directory = (GlobalDirectory) Naming.lookup( directory_url ); }
			catch (Exception e) { System.err.println( "PublishNetwork: attempt to contact "+directory_url+" failed." ); }

			if ( "".equals(context_name) )
			{
				BeliefNetworkContext local_bnc = new BeliefNetworkContext();
				bnc = local_bnc;
			}
			else
			{
				String context_url = "rmi://"+context_name;
				System.err.println( "PublishNetwork: context_url: "+context_url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( context_url );
			}

			for ( int i = 0; i < bn_names.size(); i++ )
			{
				String bn_name = (String) bn_names.elementAt(i);
				bn = (AbstractBeliefNetwork) bnc.load_network( bn_name );
				System.err.println( "PublishNetwork: rebind belief net: "+bn.get_fullname() );
				bnc.rebind( bn );

				if ( directory != null )
				{
					try { souvenirs.addElement( directory.make_entry( bn.get_fullname() ) ); }
					catch (Exception e) { System.err.println( "PublishNetwork: attempt to make directory entry "+bn.get_fullname()+" failed." ); }
				}
			}
		}
		catch (Exception e)
		{
			System.err.println( "PublishNetwork: attempt failed: " );
			e.printStackTrace();
			System.exit(1);
		}
	}
}
