package riso.apps;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.remote_data.*;
import SmarterTokenizer;

public class PublishNetworkString 
{
	public static void main( String[] args )
	{
		String context_name = "";
		char op = 'b';

		for ( int i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'c':
				context_name = args[++i];
				break;
			case 'b':
				op = 'b';
				++i;
				break;
			case 'r':
				op = 'r';
				++i;
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
				System.err.println( "PublishNetworkString: context_url: "+context_url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( context_url );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextBlock();
			bn = (AbstractBeliefNetwork) bnc.parse_network( st.sval );

			switch ( op )
			{
			case 'b':
				System.err.println( "PublishNetworkString: bind belief net: "+bn.get_fullname() );
				bnc.bind(bn);
				break;
			case 'r':
				System.err.println( "PublishNetworkString: bind or rebind belief net: "+bn.get_fullname() );
				bnc.rebind(bn);
				break;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
