package riso.apps;

import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import numerical.Format;
import SmarterTokenizer;

public class RemoteQuery
{
	public static void main( String[] args )
	{
		boolean all_x = false;
		String bn_name = "";
		int i;

		for ( i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_name = args[++i];
				break;
			}
		}

		try
		{
			String url = "rmi://"+bn_name;
			System.err.println( "RemoteQuery: url: "+url );
			AbstractBeliefNetwork bn = (AbstractBeliefNetwork) Naming.lookup( url );

			AbstractVariable[] bnv = bn.get_variables();
			System.out.println( "RemoteQuery: variables in "+bn.get_fullname()+":" );
			for ( i = 0; i < bnv.length; i++ )
			{
				System.out.println( "\t"+bnv[i].get_name() );
			}

			SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( System.in ) ) );

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				try
				{
					if ( "?".equals( st.sval ) )
					{
						System.out.println( "RemoteQuery: belief network: " );
						System.out.print( bn.format_string() );
					}
					else
					{
						String x_name = st.sval;
						st.nextToken();
						if ( st.ttype == '=' )
						{
							st.nextToken();
							double e = Format.atof( st.sval );
							System.out.println( "RemoteQuery: set "+x_name+" to "+e );
							bn.assign_evidence( bn.name_lookup( x_name ), e );
						}
						else if ( "?".equals( st.sval ) )
						{
							Distribution xposterior = bn.get_posterior( bn.name_lookup( x_name ) );
							System.out.println( "RemoteQuery: posterior for "+x_name+":" );
							System.out.print( "  "+xposterior.format_string( "\t" ) );
						}
						else if ( "-".equals( st.sval ) )
						{
							bn.clear_posterior( bn.name_lookup( x_name ) );
							System.out.println( "RemoteQuery: clear evidence: "+x_name );
						}
					}
				}
				catch (RemoteException e)
				{
					e.printStackTrace();
					System.err.println( "RemoteQuery: operation failed; stagger forward. " );
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		System.exit(0);
	}
}
