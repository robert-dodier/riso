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
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
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

						System.out.println( "  obtained reference: "+bn );
						AbstractVariable[] bnv = bn.get_variables();
						System.out.println( "RemoteQuery: variables in "+bn.get_fullname()+":" );
						for ( int i = 0; i < bnv.length; i++ )
							System.out.println( "\t"+bnv[i].get_name() );
					}
					else if ( st.ttype == '!' )
					{
						st.nextToken();
						String observable_name = st.sval;
						st.nextToken();
						String of_interest = st.sval;

						Remote remote = bn.name_lookup( observable_name );
						((RemoteObservable)remote).add_observer( new QueryObserver(), of_interest );
						System.err.println( "RemoteQery: get posterior of "+((AbstractVariable)remote).get_name()+" from callback." );
					}
					else if ( "?".equals( st.sval ) )
					{
						System.out.println( "RemoteQuery: belief network: " );
						System.out.print( bn.format_string() );
					}
					else if ( "dot".equals( st.sval ) )
					{
						System.out.println( "RemoteQuery: belief network, dot format: " );
						System.out.print( bn.dot_format() );
					}
					else if ( "get".equals( st.sval ) )
					{
						st.nextToken();
						String what = st.sval;
						st.nextToken();
						handle_get( what, (AbstractVariable)bn.name_lookup(st.sval) );
					}
					else // assume st.sval is name of a variable
					{
						String x_name = st.sval;
						st.nextToken();
						if ( st.ttype == '=' )
						{
							st.nextToken();
							double e = Format.atof( st.sval );
							System.out.println( "RemoteQuery: set "+x_name+" to "+e );
							bn.assign_evidence( (AbstractVariable)bn.name_lookup( x_name ), e );
						}
						else if ( "?".equals( st.sval ) )
						{
							long t0 = System.currentTimeMillis();
							Distribution xposterior = bn.get_posterior( (AbstractVariable)bn.name_lookup( x_name ) );
							long tf = System.currentTimeMillis();
							System.out.println( "RemoteQuery: posterior for "+x_name+", elapsed "+((tf-t0)/1000.0)+" [s]" );
							System.out.print( "\t"+xposterior.format_string( "\t" ) );
						}
						else if ( "-".equals( st.sval ) )
						{
							bn.clear_posterior( (AbstractVariable)bn.name_lookup( x_name ) );
							System.out.println( "RemoteQuery: clear posterior: "+x_name );
						}
						else if ( "all-".equals( st.sval ) )
						{
							bn.clear_all( (AbstractVariable)bn.name_lookup( x_name ) );
							System.out.println( "RemoteQuery: clear all: "+x_name );
						}
						else
						{
							System.out.println( "RemoteQuery: unknown: "+st.sval );
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.err.println( "RemoteQuery: operation failed; stagger forward. " );
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}


		System.exit(0);
	}

	static void handle_get( String what, AbstractVariable x ) throws Exception
	{
		if ( "distribution".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".distribution: " );
			ConditionalDistribution p = x.get_distribution();
			System.out.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
		}
		else if ( "parents-bns".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".parents-bns: " );
			AbstractVariable[] p = x.get_parents();
			if ( p == null ) System.out.println( "(null)" );
			else if ( p.length == 0 ) System.out.println( "(empty list)" );
			else
			{
				System.out.println("");
				for ( int i = 0; i < p.length; i++ )
				{
					Remote pbn = p[i].get_bn();
					System.out.println( x.get_name()+".parent["+i+"].get_bn: "+pbn );
				}
			}
		}
		else if ( "parents-priors".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".parents_priors: " );
			Distribution[] p = x.get_parents_priors();
			if ( p == null ) System.out.println( "(null)" );
			else if ( p.length == 0 ) System.out.println( "(empty list)" );
			else
			{
				System.out.println("");
				for ( int i = 0; i < p.length; i++ )
				{
					System.out.print( x.get_name()+".parents_priors["+i+"]: " );
					if ( p[i] == null )
					{
						System.out.println( "(null)" );
						continue;
					}
					System.out.println( "\n"+p[i].format_string("") );
				}
			}
		}
		else if ( "prior".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".prior: " );
			Distribution p = x.get_prior();
			System.out.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
		}
		else if ( "posterior".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".posterior: " );
			Distribution p = x.get_posterior();
			System.out.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
		}
		else if ( "pi".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".pi: " );
			Distribution p = x.get_pi();
			System.out.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
		}
		else if ( "lambda".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".lambda: " );
			Distribution p = x.get_lambda();
			System.out.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
		}
		else if ( "pi-messages".equals(what) )
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".pi_messages: " );
			Distribution[] p = x.get_pi_messages();
			if ( p == null ) System.out.println( "(null)" );
			else if ( p.length == 0 ) System.out.println( "(empty list)" );
			else
			{
				System.out.println("");
				for ( int i = 0; i < p.length; i++ )
				{
					System.out.print( x.get_name()+".pi_messages["+i+"]: " );
					if ( p[i] == null )
					{
						System.out.println( "(null)" );
						continue;
					}
					System.out.println( "\n"+p[i].format_string("") );
				}
			}
		}
		else if ( "lambda-messages".equals(what) ) 
		{
			System.out.print( "RemoteQuery: "+x.get_name()+".lambda_messages: " );
			Distribution[] p = x.get_lambda_messages();
			if ( p == null ) System.out.println( "(null)" );
			else if ( p.length == 0 ) System.out.println( "(empty list)" );
			else
			{
				System.out.println("");
				for ( int i = 0; i < p.length; i++ )
				{
					System.out.print( x.get_name()+".lambda_messages["+i+"]: " );
					if ( p[i] == null )
					{
						System.out.println( "(null)" );
						continue;
					}
					System.out.println( "\n"+p[i].format_string("") );
				}
			}
		}
		else
			System.err.println( "RemoteQuery.handle_get: what is "+what );
	}
}

class QueryObserver extends RemoteObserverImpl
{
	public QueryObserver() throws RemoteException {}

	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		AbstractVariable x = (AbstractVariable) o;
		System.err.println( "QueryObserver.update: callback from: "+x.get_fullname() );
		System.err.println( "  of_interest: "+of_interest );

		long t0 = System.currentTimeMillis();
		Distribution px = x.get_bn().get_posterior(x);
		long tf = System.currentTimeMillis();
		System.out.println( "QueryObserver: posterior, elapsed "+((tf-t0)/1000.0)+" [s]" );
		try { System.out.print( "\t"+px.format_string( "\t" ) ); }
		catch (Exception e) { e.printStackTrace(); throw new RemoteException( "QueryObserver: "+e ); }
	}
}
