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
	static BeliefNetworkContext bnc = null;
	static AbstractBeliefNetwork bn = null;

	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( System.in ) ) );
			parse_input( st, System.out );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}


		System.exit(0);
	}

	public static void parse_input( SmarterTokenizer st, PrintStream ps ) throws Exception
	{
		if ( bnc == null ) bnc = new BeliefNetworkContext(null);

		for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
		{
			try
			{
				if ( st.ttype == '>' )
				{
					// The belief network specified may be contained within another;
					// only the top-level b.n. will be in the registry. Parse the
					// name as necessary.

					st.nextToken();
					int slash_index = st.sval.indexOf("/"), period_index = st.sval.substring(slash_index+1).indexOf(".");

					if ( period_index == -1 )
					{
						String url = "rmi://"+st.sval;
						ps.println( "RemoteQuery: url: "+url );
						bn = (AbstractBeliefNetwork) Naming.lookup( url );
					}
					else
					{
						String toplevel_name = st.sval.substring(0,slash_index+1+period_index);
						String nested_name = st.sval.substring(slash_index+1+period_index+1);
						String url = "rmi://"+toplevel_name;
						ps.println( "RemoteQuery: url: "+url+"; nested: "+nested_name );
						bn = (AbstractBeliefNetwork) Naming.lookup( url );
						bn = (AbstractBeliefNetwork) bn.name_lookup( nested_name );
					}

					ps.println( "  obtained reference: "+bn );
					AbstractVariable[] bnv = bn.get_variables();
					ps.println( "RemoteQuery: variables in "+bn.get_fullname()+":" );
					for ( int i = 0; i < bnv.length; i++ )
						ps.println( "\t"+bnv[i].get_name() );
				}
				else if ( st.ttype == '!' )
				{
					st.nextToken();
					String observable_name = st.sval;
					st.nextToken();
					String of_interest = st.sval;

					Remote remote = bn.name_lookup( observable_name );
					((RemoteObservable)remote).add_observer( new QueryObserver(ps), of_interest );
					ps.println( "RemoteQuery: get posterior of "+((AbstractVariable)remote).get_name()+" from callback." );
				}
				else if ( "?".equals( st.sval ) )
				{
					ps.println( "RemoteQuery: belief network: " );
					ps.print( bn.format_string() );
				}
				else if ( "dot".equals( st.sval ) )
				{
					ps.println( "RemoteQuery: belief network, dot format: " );
					ps.print( bn.dot_format() );
				}
				else if ( "get".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;
					st.nextToken();
					handle_get( what, (AbstractVariable)bn.name_lookup(st.sval), true, ps );
				}
				else if ( "eval".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;
					st.nextToken();
					AbstractVariable v = (AbstractVariable) bn.name_lookup(st.sval);
					Object o = handle_get( what, v, false, ps );
					
					if ( "pi".equals(what) || "lambda".equals(what) || "prior".equals(what) || "posterior".equals(what) || "parents-priors".equals(what) )
					{
						Distribution p = (Distribution) o;
						int n = p.ndimensions();
						double[] x = new double[n];
						for ( int i = 0; i < n; i++ )
						{
							st.nextToken();
							x[i] = Format.atof(st.sval);
						}
						double r = p.p(x);
						ps.print( "p( " );
						for ( int i = 0; i < x.length; i++ ) ps.print( x[i]+" " );
						ps.println( ") == "+r );
					}
					else if ( "pi-messages".equals(what) || "lambda-messages".equals(what) )
					{
						Distribution[] p = (Distribution[]) o;
						st.nextToken();
						int ii = Format.atoi(st.sval);
						int n = p[ii].ndimensions();
						double[] x = new double[n];
						for ( int i = 0; i < n; i++ )
						{
							st.nextToken();
							x[i] = Format.atof(st.sval);
						}
						double r = p[ii].p(x);
						ps.print( "p["+ii+"]( " );
						for ( int i = 0; i < x.length; i++ ) ps.print( x[i]+" " );
						ps.println( ") == "+r );
					}
					else if ( "distribution".equals(what) )
					{
						ps.println( "RemoteQuery: eval distribution: not implemented." );
					}
					else
					{
						ps.println( "RemoteQuery: eval: what is "+what+" ?" );
					}
				}
				else // assume st.sval is name of a variable
				{
					String x_name = st.sval;
					st.nextToken();
					if ( st.ttype == '=' )
					{
						st.nextToken();
						double e = Format.atof( st.sval );
						ps.println( "RemoteQuery: set "+x_name+" to "+e );
						bn.assign_evidence( (AbstractVariable)bn.name_lookup( x_name ), e );
					}
					else if ( "?".equals( st.sval ) )
					{
						long t0 = System.currentTimeMillis();
						Distribution xposterior = bn.get_posterior( (AbstractVariable)bn.name_lookup( x_name ) );
						long tf = System.currentTimeMillis();
						ps.println( "RemoteQuery: posterior for "+x_name+", elapsed "+((tf-t0)/1000.0)+" [s]" );
						ps.print( "\t"+xposterior.format_string( "\t" ) );
					}
					else if ( "-".equals( st.sval ) )
					{
						bn.clear_posterior( (AbstractVariable)bn.name_lookup( x_name ) );
						ps.println( "RemoteQuery: clear posterior: "+x_name );
					}
					else if ( "all-".equals( st.sval ) )
					{
						bn.clear_all( (AbstractVariable)bn.name_lookup( x_name ) );
						ps.println( "RemoteQuery: clear all: "+x_name );
					}
					else
					{
						ps.println( "RemoteQuery: unknown: "+st.sval );
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				ps.println( "RemoteQuery: operation failed; stagger forward. " );
			}
		}
	}

	static Object handle_get( String what, AbstractVariable x, boolean do_print, PrintStream ps ) throws Exception
	{
		if ( "distribution".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".distribution: " );
			ConditionalDistribution p = x.get_distribution();
			if ( do_print ) ps.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
			return p;
		}
		else if ( "parents-bns".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".parents-bns: " );
			AbstractVariable[] p = x.get_parents();
			if ( do_print )
			{
				if ( p == null ) ps.println( "(null)" );
				else if ( p.length == 0 ) ps.println( "(empty list)" );
				else
				{
					ps.println("");
					for ( int i = 0; i < p.length; i++ )
					{
						Remote pbn = p[i].get_bn();
						ps.println( x.get_name()+".parent["+i+"].get_bn: "+pbn );
					}
				}
			}

			return p;
		}
		else if ( "parents-priors".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".parents_priors: " );
			Distribution[] p = x.get_parents_priors();
			if ( do_print )
			{
				if ( p == null ) ps.println( "(null)" );
				else if ( p.length == 0 ) ps.println( "(empty list)" );
				else
				{
					ps.println("");
					for ( int i = 0; i < p.length; i++ )
					{
						ps.print( x.get_name()+".parents_priors["+i+"]: " );
						if ( p[i] == null )
						{
							ps.println( "(null)" );
							continue;
						}
						ps.println( "\n"+p[i].format_string("") );
					}
				}
			}

			return p;
		}
		else if ( "prior".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".prior: " );
			Distribution p = x.get_prior();
			if ( do_print ) ps.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
			return p;
		}
		else if ( "posterior".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".posterior: " );
			Distribution p = x.get_posterior();
			if ( do_print ) ps.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
			return p;
		}
		else if ( "pi".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".pi: " );
			Distribution p = x.get_pi();
			if ( do_print ) ps.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
			return p;
		}
		else if ( "lambda".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".lambda: " );
			Distribution p = x.get_lambda();
			if ( do_print ) ps.print( (p==null?"(null)\n":"\n"+p.format_string("")) );
			return p;
		}
		else if ( "pi-messages".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".pi_messages: " );
			Distribution[] p = x.get_pi_messages();
			if ( do_print )
			{
				if ( p == null ) ps.println( "(null)" );
				else if ( p.length == 0 ) ps.println( "(empty list)" );
				else
				{
					ps.println("");
					for ( int i = 0; i < p.length; i++ )
					{
						ps.print( x.get_name()+".pi_messages["+i+"]: " );
						if ( p[i] == null )
						{
							ps.println( "(null)" );
							continue;
						}
						ps.println( "\n"+p[i].format_string("") );
					}
				}
			}

			return p;
		}
		else if ( "lambda-messages".equals(what) ) 
		{
			ps.print( "RemoteQuery: "+x.get_name()+".lambda_messages: " );
			Distribution[] p = x.get_lambda_messages();
			if ( do_print )
			{
				if ( p == null ) ps.println( "(null)" );
				else if ( p.length == 0 ) ps.println( "(empty list)" );
				else
				{
					ps.println("");
					for ( int i = 0; i < p.length; i++ )
					{
						ps.print( x.get_name()+".lambda_messages["+i+"]: " );
						if ( p[i] == null )
						{
							ps.println( "(null)" );
							continue;
						}
						ps.println( "\n"+p[i].format_string("") );
					}
				}
			}

			return p;
		}
		else
		{
			ps.println( "RemoteQuery.handle_get: what is "+what );
			return null;
		}
	}
}

class QueryObserver extends RemoteObserverImpl
{
	PrintStream ps;

	public QueryObserver( PrintStream ps ) throws RemoteException { this.ps = ps; }

	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		AbstractVariable x = (AbstractVariable) o;
		ps.println( "QueryObserver.update: callback from: "+x.get_fullname() );
		ps.println( "  of_interest: "+of_interest );

		long t0 = System.currentTimeMillis();
		Distribution px = x.get_bn().get_posterior(x);
		long tf = System.currentTimeMillis();
		ps.println( "QueryObserver: posterior, elapsed "+((tf-t0)/1000.0)+" [s]" );
		try { ps.print( "\t"+px.format_string( "\t" ) ); }
		catch (Exception e) { e.printStackTrace(); throw new RemoteException( "QueryObserver: "+e ); }
	}
}
