/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
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
	static Distribution d = null;
	static BeliefNetworkContext bnc = null;
	static AbstractBeliefNetwork bn = null;
	static Remote remote = null;

	static long new_slice_index = 1, old_slice_index = 1;

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
						remote = Naming.lookup( url );
						bn = (AbstractBeliefNetwork) remote;
					}
					else
					{
						String toplevel_name = st.sval.substring(0,slash_index+1+period_index);
						String nested_name = st.sval.substring(slash_index+1+period_index+1);
						String url = "rmi://"+toplevel_name;
						ps.println( "RemoteQuery: url: "+url+"; nested: "+nested_name );
						bn = (AbstractBeliefNetwork) Naming.lookup( url );
						remote = bn.name_lookup( nested_name );
						bn = (AbstractBeliefNetwork) remote;
					}

					ps.println( "  obtained reference: "+bn );
					AbstractVariable[] bnv = bn.get_variables();
					ps.println( "RemoteQuery: variables in "+bn.get_fullname()+":" );
					for ( int i = 0; i < bnv.length; i++ )
						ps.println( "\t"+bnv[i].get_name() );
				}
				else if ( st.ttype == '!' )
				{
					// Two ways to enter a callback:
					// (1) ! variablename of-interest   -- do not requery if arg is null
					// (2) !? variablename of-interest  -- requery if arg is null

					st.nextToken();

					boolean requery = false;
					if ( "?".equals(st.sval) )
					{
						requery = true;
						st.nextToken();
					}

					String observable_name = st.sval;
					st.nextToken();
					String of_interest = st.sval;

					Remote remote = bn.name_lookup( observable_name );
					((RemoteObservable)remote).add_observer( new QueryObserver(ps,requery), of_interest );
					ps.println( "RemoteQuery: get "+of_interest+" of "+((AbstractVariable)remote).get_name()+" from callback; "+(requery?"requery":"do not requery")+" if null." );
				}
				else if ( "+".equals( st.sval ) )
				{
					// Add a slice to a temporal bn. Takes an optional slice index.
					st.eolIsSignificant(true);
					for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOL; st.nextToken() )
						new_slice_index = Format.atoi(st.sval);
					st.eolIsSignificant(false);
					AbstractTemporalBeliefNetwork tbn = (AbstractTemporalBeliefNetwork) remote;
					ps.println( "RemoteQuery: add slice "+new_slice_index+" to "+tbn.get_fullname() );
					tbn.create_timeslice( new_slice_index++ );
				}
				else if ( "-".equals( st.sval ) )
				{
					// Delete oldest slice from a temporal bn. Takes an optional slice index.
					st.eolIsSignificant(true);
					for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOL; st.nextToken() )
						old_slice_index = Format.atoi(st.sval);
					st.eolIsSignificant(false);
					AbstractTemporalBeliefNetwork tbn = (AbstractTemporalBeliefNetwork) remote;
					ps.println( "RemoteQuery: remove slice "+old_slice_index+" from "+tbn.get_fullname() );
					tbn.destroy_timeslice( old_slice_index++ );
				}
				else if ( "?".equals( st.sval ) )
				{
					ps.println( "RemoteQuery: context "+bn.get_context().get_name()+"; belief network:" );
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
				else if ( "get-tbn".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;
					handle_get( what, null, true, ps );
				}
				else if ( "get-d".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;

					double x = 0;
					if ( "p".equals(what) || "cdf".equals(what) )
					{
						st.nextToken();
						x = Format.atof( st.sval );
					}

					handle_distribution_get( what, x, true, ps );
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
					AbstractVariable v = (AbstractVariable) bn.name_lookup(x_name);

					st.nextToken();

					if ( st.ttype == '=' )
					{
						st.nextToken();
						double e = Format.atof( st.sval );
						ps.println( "RemoteQuery: set "+v.get_fullname()+" to "+e );
						bn.assign_evidence( v, e );
					}
					else if ( "?-".equals( st.sval ) ) // get posterior, but don't print it.
					{
						long t0 = System.currentTimeMillis();
						d = bn.get_posterior(v);
						long tf = System.currentTimeMillis();
						ps.println( "RemoteQuery: posterior type: "+d.getClass().getName()+" for "+v.get_fullname()+", elapsed "+((tf-t0)/1000.0)+" [s]" );
					}
					else if ( "?".equals( st.sval ) ) // get posterior, and print it.
					{
						long t0 = System.currentTimeMillis();
						d = bn.get_posterior(v);
						long tf = System.currentTimeMillis();
						ps.println( "RemoteQuery: posterior for "+v.get_fullname()+", elapsed "+((tf-t0)/1000.0)+" [s]" );
						ps.print( "\t"+d.format_string( "\t" ) );
					}
					else if ( "-".equals( st.sval ) )
					{
						bn.clear_posterior(v);
						ps.println( "RemoteQuery: clear posterior: "+v.get_fullname() );
					}
					else if ( "all-".equals( st.sval ) )
					{
						bn.clear_all(v);
						ps.println( "RemoteQuery: clear all: "+v.get_fullname() );
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

	static void handle_distribution_get( String what, double x, boolean do_print, PrintStream ps ) throws Exception
	{
		if ( "p".equals(what) )
		{
			double[] xx = new double[1];
			xx[0] = x;
			if ( do_print ) ps.println( (d==null?"(d==null)":"  "+d.p(xx)) );
		}
		else if ( "cdf".equals(what) )
		{
			if ( do_print ) ps.println( (d==null?"(d==null)":"  "+d.cdf(x)) );
		}
		else if ( "mean".equals(what) )
		{
			if ( do_print ) ps.println( (d==null?"(d==null)":"  "+d.expected_value()) );
		}
		else if ( "stddev".equals(what) )
		{
			if ( do_print ) ps.println( (d==null?"(d==null)":"  "+d.sqrt_variance()) );
		}
		else if ( "support".equals(what) )
		{
			double[] supt = d.effective_support(1e-6);
			if ( do_print ) ps.println( (d==null?"(d==null)":"  ("+supt[0]+", "+supt[1]+")") );
		}
		else
		{
			ps.println( "RemoteQuery.handle_get: what is "+what );
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
		else if ( "parents".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".parents: " );
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
						ps.print( x.get_name()+".parent["+i+"].get_fullname: " );
						try { ps.println( p[i].get_fullname() ); }
						catch (RemoteException e) { ps.println( "OOPS! "+e ); }
					}
				}
			}

			return p;
		}
		else if ( "children".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".children: " );
			AbstractVariable[] c = x.get_children();
			if ( do_print )
			{
				if ( c == null ) ps.println( "(null)" );
				else if ( c.length == 0 ) ps.println( "(empty list)" );
				else
				{
					ps.println("");
					for ( int i = 0; i < c.length; i++ )
					{
						ps.print( x.get_name()+".child["+i+"].get_fullname: " );
						try { ps.println( c[i].get_fullname() ); }
						catch (RemoteException e) { ps.println( "OOPS! "+e ); }
					}
				}
			}

			return c;
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
						ps.print( x.get_name()+".parent["+i+"].get_bn: " );
						try { ps.println( ""+p[i].get_bn() ); }
						catch (RemoteException e) { ps.println( "OOPS! "+e ); }
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
			d = x.get_prior();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "posterior".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".posterior: " );
			d = x.get_posterior();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "pi".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".pi: " );
			d = x.get_pi();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "lambda".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".lambda: " );
			d = x.get_lambda();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
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
		else if ( "slices".equals(what) )
		{
			AbstractTemporalBeliefNetwork tbn = (AbstractTemporalBeliefNetwork) remote;
			ps.println( "RemoteQuery: "+tbn.get_fullname()+" slices: " );
			AbstractBeliefNetwork[] slices = tbn.get_slices();
			if ( do_print )
			{
				if ( slices == null ) ps.println( "(null)" );
				else if ( slices.length == 0 ) ps.println( "(empty list)" );
				else
				{
					for ( int i = 0; i < slices.length; i++ )
						ps.println( "\t"+slices[i].get_fullname() );
				}
			}

			return slices;
		}
		else if ( "shadow-most-recent".equals(what) )
		{
			AbstractTemporalBeliefNetwork tbn = (AbstractTemporalBeliefNetwork) remote;
			ps.println( "RemoteQuery: "+tbn.get_fullname()+" shadow-most-recent:" );
			AbstractBeliefNetwork shadow = tbn.get_shadow_most_recent();
			if ( do_print )
			{
				if ( shadow == null ) ps.println( "(null)" );
				else ps.print( shadow.format_string() );
			}

			return shadow;
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
	boolean requery;

	public QueryObserver( PrintStream ps, boolean requery ) throws RemoteException
	{
		this.ps = ps;
		this.requery = requery;
	}

	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		AbstractVariable x = (AbstractVariable) o;
		ps.print( "QueryObserver.update: local time "+(new java.util.Date())+"; callback sends "+of_interest+" from "+x.get_fullname()+": " );

		if ( arg == null ) 
		{
			ps.println( "(null)" );
			if ( requery ) 
			{
				ps.println( "\tExecute get_posterior() and return." );
				x.get_bn().get_posterior(x);
			}
		}
		else
			try { ps.print( "\n\t"+((Distribution)arg).format_string("\t") ); }
			catch (Exception e) { e.printStackTrace(); throw new RemoteException( "QueryObserver: "+e ); }
	}
}
