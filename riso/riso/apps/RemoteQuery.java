package riso.apps;

import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.remote_data.*;
import numerical.Format;
import SmarterTokenizer;

public class RemoteQuery
{
	public static void main( String[] args )
	{
		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext();

			SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( System.in ) ) );
			AbstractBeliefNetwork bn = null;


			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				try
				{
					if ( st.ttype == '>' )
					{
						st.nextToken();
						String url = "rmi://"+st.sval;
						System.err.println( "RemoteQuery: url: "+url );
						bn = (AbstractBeliefNetwork) Naming.lookup( url );

						AbstractVariable[] bnv = bn.get_variables();
						System.out.println( "RemoteQuery: variables in "+bn.get_fullname()+":" );
						for ( int i = 0; i < bnv.length; i++ )
							System.out.println( "\t"+bnv[i].get_name() );
					}
					else if ( st.ttype == '!' )
					{
						st.nextToken();
						String obsvble_name = st.sval;
						st.nextToken();
						String query_name = st.sval;

						int pindex = obsvble_name.indexOf(".");
						String bn_name = obsvble_name.substring(0,pindex);
						Remote remote = bnc.get_reference( bn_name );
						AbstractVariable of_interest = ((AbstractBeliefNetwork)remote).name_lookup( obsvble_name.substring(pindex+1) );
						AbstractVariable to_query = bn.name_lookup( query_name );
						((RemoteObservable)remote).add_observer( new QueryObserver(to_query), of_interest );
					}
					else if ( "?".equals( st.sval ) )
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
							System.out.print( "\t"+xposterior.format_string( "\t" ) );
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

class QueryObserver extends RemoteObserverImpl
{
	AbstractVariable to_query;

	public QueryObserver() throws RemoteException {}

	public QueryObserver( AbstractVariable to_query_in ) throws RemoteException
	{
		to_query = to_query_in;
		System.err.println( "QueryObserver: query "+to_query.get_fullname()+" when updated." );
	}

	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		System.err.println( "QueryObserver.update: callback from: "+o );
		System.err.println( "  of_interest: "+of_interest );
		AbstractVariable x = (AbstractVariable) of_interest;
		System.err.println( "  x: "+x.get_fullname() );
		System.err.println( "  computing posterior of "+to_query.get_fullname()+"..." );

		Distribution px = to_query.get_bn().get_posterior( to_query );
		System.out.println( "QueryObserver: posterior:" );
		System.out.print( "\t"+px.format_string( "\t" ) );
	}
}
