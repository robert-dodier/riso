package riso.apps;

import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.remote_data.*;

class Op
{
	String bn_name, operation;
	Op( String s1, String s2 ) { bn_name = s1; operation = s2; }
}

public class PublishNetwork
{
	public static void main( String[] args )
	{
		String context_name = "";
		Vector bn_operations = new Vector();
		int nloaded = 0;

		for ( int i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'c':
				context_name = args[++i];
				break;
			case 'b':
				bn_operations.addElement( new Op(args[++i],"b") );
				break;
			case 'u':
				bn_operations.addElement( new Op(args[++i],"u") );
				break;
			case 'r':
				bn_operations.addElement( new Op(args[++i],"r") );
				break;
			}
		}

		AbstractBeliefNetworkContext bnc = null;
		AbstractBeliefNetwork bn = null;

		try
		{
			if ( "".equals(context_name) )
			{
				bnc = new BeliefNetworkContext(null);
			}
			else
			{
				String context_url = "rmi://"+context_name;
				System.err.println( "PublishNetwork: context_url: "+context_url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( context_url );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		for ( int i = 0; i < bn_operations.size(); i++ )
		{
			try
			{
				Op op = (Op) bn_operations.elementAt(i);
				switch ( op.operation.charAt(0) )
				{
				case 'b':
					bn = (AbstractBeliefNetwork) bnc.load_network( op.bn_name );
					System.err.println( "PublishNetwork: bind belief net: "+bn.get_fullname() );
					bnc.bind(bn);
					++nloaded;
					break;
				case 'r':
					bn = (AbstractBeliefNetwork) bnc.load_network( op.bn_name );
					System.err.println( "PublishNetwork: bind or rebind belief net: "+bn.get_fullname() );
					bnc.rebind(bn);
					++nloaded;
					break;
				case 'u':
					System.err.println( "PublishNetwork: unbind: "+op.bn_name );
					Remote o = Naming.lookup( "rmi://"+op.bn_name );
					((Perishable)o).set_stale();
					Naming.unbind( "rmi://"+op.bn_name );
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println( "PublishNetwork: stagger on." );
			}
		}

		if ( nloaded == 0 ) System.exit(0);
	}
}
