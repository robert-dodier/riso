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
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.approximation.*;
import riso.regression.*;
import riso.remote_data.*;
import riso.numerical.*;
import riso.general.*;

public class RemoteQuery
{
	static Distribution d = null, d2 = null;
	static BeliefNetworkContext bnc = null;
	static AbstractBeliefNetwork bn = null;
	static Remote remote = null;

	static long new_slice_index = 1, old_slice_index = 1;

	public static void main( String[] args )
	{
        System.out.println ("Hello, I'll read the stuff you type. See: http://riso.sf.net/bnc-pn-rq.html");

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
		for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
		{
			try
			{
				if ( st.ttype == '>' )
				{
					st.nextToken();

					if (get_bn_reference( st.sval, ps ) != null)
                    {
                        ps.println( "  obtained reference: "+bn );
                        AbstractVariable[] bnv = bn.get_variables();
                        ps.println( "RemoteQuery: variables in "+bn.get_fullname()+":" );
                        for ( int i = 0; i < bnv.length; i++ )
                            ps.println( "\t"+bnv[i].get_name() );
                    }
				}
				else if ( st.ttype == '!' )
				{
					// Three ways to enter a callback:
					// (1) ! variablename of-interest   -- do not requery if arg is null
					// (2) !? variablename of-interest  -- requery if arg is null
					// (3) !?- variablename of-interest -- requery if arg is null; if non-null, only print class name

					st.nextToken();

					boolean requery = false, do_print_update = true;
					if ( "?".equals(st.sval) )
					{
						requery = true;
						st.nextToken();
					}
					else if ( "?-".equals(st.sval) )
					{
						requery = true;
						do_print_update = false;
						st.nextToken();
					}

					String observable_name = st.sval;
					st.nextToken();
					String of_interest = st.sval;

                    try
                    {
					    Remote remote = bn.name_lookup( observable_name );
					    ((RemoteObservable)remote).add_observer( new QueryObserver(ps,requery,do_print_update), of_interest );
					    ps.println( "RemoteQuery: get "+of_interest+" of "+((AbstractVariable)remote).get_name()+" from callback; "+(requery?"requery":"do not requery")+" if null." );
                    }
                    catch (NoSuchElementException e)    // name_lookup should fail with a more specific exception !!!
                    {
                        ps.println ("RemoteQuery: attempt to set up callback failed; no such variable: "+observable_name);
                    }
				}
				else if ( "+".equals( st.sval ) )
				{
					// Add a slice to a temporal bn. Takes an optional slice index.
					st.eolIsSignificant(true);
					for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOL; st.nextToken() )
						new_slice_index = Integer.parseInt(st.sval);
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
						old_slice_index = Integer.parseInt(st.sval);
					st.eolIsSignificant(false);
					AbstractTemporalBeliefNetwork tbn = (AbstractTemporalBeliefNetwork) remote;
					ps.println( "RemoteQuery: remove slice "+old_slice_index+" from "+tbn.get_fullname() );
					tbn.destroy_timeslice( old_slice_index++ );
				}
				else if ( "?".equals( st.sval ) )
				{
					ps.println( "RemoteQuery: context "+bn.get_context().get_name()+"; belief network:" );
					ps.print( bn.format_string("") );
				}
				else if ( "dot".equals( st.sval ) )
				{
					ps.println( "RemoteQuery: belief network, dot format: " );
					ps.print( bn.dot_format() );
				}
				else if ( "compute".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval, vname = null;
					if ( ! "kl".equals(what) ) { st.nextToken(); vname = st.sval; }

                    try
                    {
                        AbstractVariable v = (vname==null?null:(AbstractVariable)bn.name_lookup(st.sval));
					    handle_compute (what, v, true, ps);
                    }
                    catch (NoSuchElementException e)    // name_lookup should fail with a more specific exception !!!
                    {
                        ps.println ("RemoteQuery: ``compute "+what+"'' failed; no such variable: "+st.sval);
                    }
				}
				else if ( "set".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;
					st.nextToken();

                    try
                    {
                        AbstractVariable v = (AbstractVariable) bn.name_lookup(st.sval);
					    handle_set (what, v, true, ps, st);
                    }
                    catch (NoSuchElementException e)    // name_lookup should fail with a more specific exception !!!
                    {
                        ps.println ("RemoteQuery: ``set "+what+"'' failed; no such variable: "+st.sval);
                    }
				}
				else if ( "parse".equals( st.sval ) )
				{
					handle_parse( st, ps );
				}
				else if ( "get".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;
					st.nextToken();

                    try
                    {
                        AbstractVariable v = (AbstractVariable) bn.name_lookup(st.sval);
					    handle_get (what, v, true, ps);
                    }
                    catch (NoSuchElementException e)    // name_lookup should fail with a more specific exception !!!
                    {
                        ps.println ("RemoteQuery: ``get "+what+"'' failed; no such variable: "+st.sval);
                    }
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

					double[] x = null;
					if ( "cdf".equals(what) || "mass".equals(what) )
					{
                        x = new double[1];
						st.nextToken();
						x[0] = Double.parseDouble( st.sval );
					}
                    else if ("p".equals(what))
                    {
                        int n = d.ndimensions();
                        x = new double[n];
                        for (int i = 0; i < n; i++)
                        {
                            st.nextToken();
                            x[i] = Double.parseDouble (st.sval);
                        }
System.err.print ("get-d: x:"); for (int i = 0; i < n; i++) System.err.print (" "+x[i]); System.err.println ("");
                    }

					handle_distribution_get( what, x, true, ps );
				}
				else if ( "eval".equals( st.sval ) )
				{
					st.nextToken();
					String what = st.sval;
					st.nextToken();
					AbstractVariable v = null;
                    
                    try { v = (AbstractVariable) bn.name_lookup(st.sval); }

                    catch (NoSuchElementException e)
                    {
                        ps.println ("RemoteQuery: ``eval "+what+"'' failed; no such variable: "+st.sval);
                        continue;
                    }

					Object o;
					if ( "F".equals(what) || "dFdx".equals(what) )
						o = handle_get( "distribution", v, false, ps );
					else
						o = handle_get( what, v, false, ps );
					
					if ( "F".equals(what) || "dFdx".equals(what) )
					{
						RegressionModel rm = ((RegressionDensity)o).regression_model;

						int n = rm.ndimensions_in();
						double[] x = new double[n];
						for ( int i = 0; i < n; i++ )
						{
							st.nextToken();
							x[i] = Double.parseDouble(st.sval);
						}

						if ( "F".equals(what) )
						{
							double[] y = rm.F(x);
							ps.print( "F( " );
							Matrix.pretty_output( x, ps, " " );
							ps.print( ") == " );
							Matrix.pretty_output( y, ps, " " );
							ps.println("");
						}
						else
						{
							double[][] D = rm.dFdx(x);
							ps.print( "dFdx( " );
							Matrix.pretty_output( x, ps, " " );
							ps.print( ") == " );
							if ( D.length == 1 )
								Matrix.pretty_output( D[0], ps, " " );
							else
								Matrix.pretty_output( D, ps, "\t" );
							ps.println("");
						}
					}
					else if ( "pi".equals(what) || "lambda".equals(what) || "prior".equals(what) || "posterior".equals(what) || "parents-priors".equals(what) )
					{
						Distribution p = (Distribution) o;
						d2 = d;
						d = p;
						int n = p.ndimensions();
						double[] x = new double[n];
						for ( int i = 0; i < n; i++ )
						{
							st.nextToken();
							x[i] = Double.parseDouble(st.sval);
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
						int ii = Integer.parseInt(st.sval);
						d2 = d;
						d = p[ii];
						int n = p[ii].ndimensions();
						double[] x = new double[n];
						for ( int i = 0; i < n; i++ )
						{
							st.nextToken();
							x[i] = Double.parseDouble(st.sval);
						}
						double r = p[ii].p(x);
						ps.print( "p["+ii+"]( " );
						for ( int i = 0; i < x.length; i++ ) ps.print( x[i]+" " );
						ps.println( ") == "+r );
					}
					else if ( "distribution".equals(what) )
					{
						ConditionalDistribution cd = (ConditionalDistribution) o;
						double[] x = new double[ cd.ndimensions_child() ];
						double[] u = new double[ cd.ndimensions_parent() ];
						for ( int i = 0; i < x.length; i++ )
						{
							st.nextToken();
							x[i] = Double.parseDouble( st.sval );
						}
						for ( int i = 0; i < u.length; i++ )
						{
							st.nextToken();
							u[i] = Double.parseDouble( st.sval );
						}
						double r = cd.p(x,u);
						ps.print( "p(" );
						for ( int i = 0; i < x.length; i++ ) ps.print( x[i]+"," );
						ps.print( "|" );
						for ( int i = 0; i < u.length; i++ ) ps.print( u[i]+"," );
						ps.println( ") == "+r );
					}
					else
					{
						ps.println( "RemoteQuery: eval: what is "+what+" ?" );
					}
				}
				else // assume st.sval is name of a variable
				{
					String x_name = st.sval;
                    AbstractVariable v = null;

					st.nextToken();

                    double e = 0;
                    boolean do_assign_evidence = false;

					if ( st.ttype == '=' )
					{
						st.nextToken();
						e = Double.parseDouble( st.sval );
                        do_assign_evidence = true;
                    }

                    try { v = (AbstractVariable) bn.name_lookup(x_name); }

                    catch (NoSuchElementException ee)
                    {
                        ps.println ("RemoteQuery: no such variable: "+x_name);
                        continue;
                    }

                    if (do_assign_evidence)
                    {
						ps.println( "RemoteQuery: set "+v.get_fullname()+" to "+e );
						bn.assign_evidence( v, e );
					}
					else if ( "?-".equals( st.sval ) ) // get posterior, but don't print it.
					{
						long t0 = System.currentTimeMillis();
						d2 = d;
						d = bn.get_posterior(v);
						long tf = System.currentTimeMillis();
						ps.println( "RemoteQuery: posterior type: "+d.getClass().getName()+" for "+v.get_fullname()+", elapsed "+((tf-t0)/1000.0)+" [s]" );

						// Spline density is an important special case.
						if ( d instanceof SplineDensity )
							ps.println( "\t"+"(posterior is SplineDensity; "+((SplineDensity)d).spline.x.length+" nodes.)" );
					}
                    else if ("?joint".equals (st.sval))
                    {
                        ps.println ("RemoteQuery: CODE FOR HANDLING ``?joint'' IS AN UGLY HACK. FIX IT.");

                        Vector x_names = new Vector();
                        x_names.addElement (x_name);

                        st.eolIsSignificant (true);
                        for (st.nextToken(); st.ttype != StreamTokenizer.TT_EOL; st.nextToken())
                            x_names.addElement (st.sval);
                        st.eolIsSignificant (false);

                        try
                        {
                            AbstractVariable[] vv = new AbstractVariable [x_names.size()];
                            for (int i = 0; i < x_names.size(); i++)
                                vv[i] = (AbstractVariable) bn.name_lookup((String) x_names.elementAt(i));

                            d2 = d;
                            long t0 = System.currentTimeMillis();
                            d = bn.get_posterior (vv);
                            long tf = System.currentTimeMillis();
                            ps.print ("RemoteQuery: posterior for");
                            for (int i = 0; i < vv.length; i++)
                                ps.print (" "+v.get_fullname());
                            ps.println (": elapsed "+((tf-t0)/1000.0)+" [s]");
                            ps.print ("\t"+d.format_string ("\t"));
                        }
                        catch (NoSuchElementException ee)
                        {
                            ps.println ("RemoteQuery: "+ee);
                            continue;
                        }
                    }
					else if ( "?".equals( st.sval ) ) // get posterior, and print it.
					{
						long t0 = System.currentTimeMillis();
						d2 = d;
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
                    // Ugh. This is really awful: "?" prints the bn description,
                    // but "x ?" prints the posterior, and "x str" prints the description of x.
                    // I really should rethink the whole set of commands. !!!
                    else if ("str".equals (st.sval))
                    {
                        ps.println(v.format_string("    "));
                    }
					else
					{
						ps.println( "RemoteQuery: unknown: "+st.sval );
					}
				}
			}
            catch (RemoteException e)
            {
                ps.println ("RemoteQuery: "+e+"; nested: "+e.detail);
            }
			catch (Exception e)
			{
				e.printStackTrace();
				ps.println( "RemoteQuery: operation failed; stagger forward. " );
			}
		}
	}

	static void handle_distribution_get( String what, double[] x, boolean do_print, PrintStream ps ) throws Exception
	{
		if ( "p".equals(what) )
		{
			if ( do_print ) ps.println( (d==null?"(d==null)":"  "+d.p(x)) );
		}
		else if ( "cdf".equals(what) )
		{
			if ( do_print ) ps.println( (d==null?"(d==null)":"  "+d.cdf(x[0])) );
		}
		else if ( "mass".equals(what) )
		{
			if ( do_print )
				if ( d == null )
					ps.println( "(d==null)" );
				else
				{
					Matrix.pretty_output( find_mass(d,x[0]), ps, " " );
					ps.println();
				}
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
			ps.println( "RemoteQuery.handle_distribution_get: what is "+what );
		}
	}

	static Object handle_compute( String what, AbstractVariable x, boolean do_print, PrintStream ps ) throws Exception
	{
		if ( "pi".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".compute_pi(): " );
			d2 = d;
			d = x.compute_pi();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "kl".equals(what) )
		{
			riso.apps.ComputeKL kl_doer = new riso.apps.ComputeKL( d, d2 );
			ps.println( "RemoteQuery: KL(d,d2) == "+kl_doer.do_compute_kl() );
			return null;
		}
		else
		{
			ps.println( "RemoteQuery.handle_compute: what is "+what );
			return null;
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
			d2 = d;
			d = x.get_prior();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "posterior".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".posterior: " );
			d2 = d;
			d = x.get_posterior();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "pi".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".pi: " );
			d2 = d;
			d = x.get_pi();
			if ( do_print ) ps.print( (d==null?"(null)\n":"\n"+d.format_string("")) );
			return d;
		}
		else if ( "lambda".equals(what) )
		{
			ps.print( "RemoteQuery: "+x.get_name()+".lambda: " );
			d2 = d;
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
		else
		{
			ps.println( "RemoteQuery.handle_get: what is "+what );
			return null;
		}
	}

	static Object handle_set( String what, AbstractVariable x, boolean do_print, PrintStream ps, SmarterTokenizer st ) throws Exception
	{
		if ( "distribution".equals(what) )
		{
			// Slurp a description. This has the same form as a description in a
			// belief network file: ClassName { description }
			
			st.nextToken();
			ConditionalDistribution cd = (ConditionalDistribution) Class.forName(st.sval).newInstance();
			st.nextBlock();
			cd.parse_string(st.sval);
			x.set_distribution(cd);

			return cd;
		}
		else
		{
			ps.println( "RemoteQuery.handle_set: don't know how to handle `"+what+"'." );
			return null;
		}
	}

	/** Slurp a description. This has the same form as a description in a
	  * belief network file: "ClassName name { description }"
	  */
	static Object handle_parse( SmarterTokenizer st, PrintStream ps ) throws Exception
	{
		st.nextBlock();

		if ( bnc == null ) bnc = new BeliefNetworkContext(null);

		bn = bnc.parse_network(st.sval);
		bnc.rebind(bn);

		return bn;
	}

	public static AbstractBeliefNetwork get_bn_reference( String bn_name, PrintStream ps ) throws Exception
	{
		// The belief network specified may be contained within another;
		// only the top-level b.n. will be in the registry. Parse the
		// name as necessary.

        // If the bn can't be located, this method returns null,
        // and the global variable "bn" is NOT modified.

		int slash_index = bn_name.indexOf("/"), period_index = bn_name.substring(slash_index+1).indexOf(".");

		if ( period_index == -1 )
		{
			String url = "rmi://" + (slash_index == -1 ? "localhost/" : "") + bn_name;

			if ( ps != null ) ps.println( "RemoteQuery: url: "+url );
            
            try
            {
			    remote = Naming.lookup( url );
			    bn = (AbstractBeliefNetwork) remote;
            }
            catch (NotBoundException e)
            {
                ps.println ("RemoteQuery: attempt to get bn reference failed; no such RMI resource: "+url);
                return null;
            }
            catch (UnknownHostException e)
            {
                 ps.println ("RemoteQuery: attempt to get bn reference failed; no such host: "+url);
                 return null;
            }
		}
		else
		{
			String toplevel_name = bn_name.substring(0,slash_index+1+period_index);
			String nested_name = bn_name.substring(slash_index+1+period_index+1);
			String url = "rmi://" + (slash_index == -1 ? "localhost/" : "") + toplevel_name;

			if ( ps != null ) ps.println( "RemoteQuery: url: "+url+"; nested: "+nested_name );

            try
            {
			    bn = (AbstractBeliefNetwork) Naming.lookup( url );
			    remote = bn.name_lookup( nested_name );
			    bn = (AbstractBeliefNetwork) remote;
            }
            catch (NoSuchElementException e)    // name_lookup should fail with a more specific exception !!!
            {
                ps.println ("RemoteQuery: attempt to get bn reference failed; no such nested name: "+nested_name);
                return null;
            }
            catch (NotBoundException e)
            {
                ps.println ("RemoteQuery: attempt to get bn reference failed; no such RMI resource: "+url);
                return null;
            }
            catch (UnknownHostException e)
            {
                 ps.println ("RemoteQuery: attempt to get bn reference failed; no such host: "+url);
                 return null;
            }
		}

		return bn;
	}

	/** Find an interval containing the specified amount of mass.
	  * The endpoints are chosen so that half the remaining mass is to the right
	  * and the other half is to the left of the interval returned.
	  */
	public static double[] find_mass( Distribution d, double mass ) throws Exception
	{
		double epsilon = 1 - mass;

		// Use bisection search. First find x s.t. 1-cdf(x) = epsilon/2,
		// then find x s.t. cdf(x) = epsilon/2, and return those two as interval.

		double[] interval = new double[2];
		double m = d.expected_value(), s = d.sqrt_variance(), k = 1/Math.sqrt(epsilon);

		double z0 = m, z1 = m+k*s;
		while ( z1 - z0 > 0.1*s )
		{
			double zm = z0 + (z1-z0)/2;
			double Fm = d.cdf(zm);
			if ( Fm > 1-epsilon/2 )
				z1 = zm;
			else 
				z0 = zm;
		}

		interval[1] = z1;

		z1 = m; z0 = m-k*s;
		while ( z1 - z0 > 0.1*s)
		{
			double zm = z0 + (z1-z0)/2;
			double Fm = d.cdf(zm);
			if ( Fm < epsilon/2 )
				z0 = zm;
			else 
				z1 = zm;
		}

		interval[0] = z0;

		return interval;
	}
}

class QueryObserver extends RemoteObserverImpl
{
	PrintStream ps;
	boolean requery, do_print_update;

	public QueryObserver( PrintStream ps, boolean requery, boolean do_print_update ) throws RemoteException
	{
		this.ps = ps;
		this.requery = requery;
		this.do_print_update = do_print_update;
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
				ps.println( "\tStart thread for get_posterior() and return." );
				(new ComputationThread(x)).start();
			}
		}
		else
		{
			if ( do_print_update )
				try { ps.print( "\n\t"+((Distribution)arg).format_string("\t") ); }
				catch (Exception e) { e.printStackTrace(); throw new RemoteException( "QueryObserver: "+e ); }
			else
			{
				ps.println( "\n\t"+arg.getClass() );

				// Spline density is an important special case.
				if ( arg instanceof SplineDensity )
					ps.println( "\t"+"(arg is SplineDensity; "+((SplineDensity)arg).spline.x.length+" nodes.)" );
			}
		}
	}
}

/** Do a calc on behalf of <tt>update</tt> -- calling <tt>get_posterior</tt>
  * from <tt>update</tt> causes deadlock.
  */
class ComputationThread extends Thread
{
	AbstractVariable x;

	ComputationThread( AbstractVariable x ) { this.x = x; }

	public void run()
	{
		try { x.get_bn().get_posterior(x); }
		catch (RemoteException e) { System.err.println( "ComputationThread.run: failed; "+e ); } 
	}
}
