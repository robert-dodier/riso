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
package riso.belief_nets;

import java.io.*;
import java.util.*;
import java.rmi.*;
import riso.distributions.*;
import riso.remote_data.*;
import SmarterTokenizer;

public class TemporalBeliefNetwork extends BeliefNetwork implements AbstractTemporalBeliefNetwork
{
	BeliefNetwork template, most_recent, shadow_most_recent;
	Hashtable slices = new Hashtable();

	/** Do-nothing constructor, exists just to throw <tt>RemoteException</tt>.
	  */
	public TemporalBeliefNetwork() throws RemoteException {}

	/** Return an array of references to the slices in this temporal belief network.
	  * Any slices which have been destroyed do not appear on the list returned.
	  * 
	  * <p> The array returned by this method is UNORDERED -- the slices do not necessarily
	  * appear in the order they were created. However, it is easy to put them in order by
	  * sorting the slices according to their names.
	  */
	public AbstractBeliefNetwork[] get_slices() throws RemoteException
	{
		AbstractBeliefNetwork[] list = new AbstractBeliefNetwork[ slices.size() ];

		int i = 0;
		for ( Enumeration e = slices.elements(); e.hasMoreElements(); )
			list[i++] = (AbstractBeliefNetwork) e.nextElement();
		
		return list;
	}

	/** Return a reference to the belief network which ``shadows'' the most
	  * recent time slice. If no slices have been instantiated, this method
	  * returns null.
	  */
	public AbstractBeliefNetwork get_shadow_most_recent() throws RemoteException
	{
		return shadow_most_recent;
	}

	/** If <tt>name</tt> is a simple name, i.e. a name which does not
	  * contain a period, it might be the name of a variable or a belief network
	  * contained by this temporal belief network. If it's a variable, then
	  * return a reference to a shadow of the most recent instance (i.e., greatest
	  * timestamp) of the variable of the given name. If it's a belief network,
	  * search the list of b.n.'s contained by this one. Otherwise, the name is
	  * a compound name, e.g. <tt>slice[192].density</tt>, so <tt>density</tt>
	  * is sought within the belief network <tt>slice[192]</tt> which
	  * is contained within this top-level belief network.
	  * Returns <tt>null</tt> if the variable isn't in this belief network.
	  */
	public Remote name_lookup( String some_name ) throws RemoteException
	{
		check_stale( "name_lookup" );

		int period_index = some_name.indexOf(".");

		if ( period_index == -1 )
			// Simple name -- may be a variable, or may be a belief network.
			try
			{
				if ( shadow_most_recent == null ) return null;
				return (Remote) shadow_most_recent.variables.get(some_name);
			}
			catch (NoSuchElementException e) { return (Remote) slices.get( template.name+"."+some_name); }
		else
		{
			// Compound name -- punt.
			String slice_name = template.name+"."+some_name.substring(0,period_index);
			BeliefNetwork slice = (BeliefNetwork) slices.get(slice_name);
			if ( slice == null ) return null;
			return slice.name_lookup( some_name.substring(period_index+1) );
		}
	}

	/** Return a reference to the variable of the given name with the
	  * specified timestamp. If an instance with the timestamp doesn't
	  * exist, a new timeslice (with the specified timestamp) is created.
	  * Returns <tt>null</tt> if the variable isn't in this belief network.
	  * @throws Exception If the timeslice cannot be created; see
	  *   <tt>create_timeslice</tt>.
	  */
	public Remote name_lookup( String variable_name, long timestamp ) throws Exception
	{
		check_stale( "name_lookup" );
			
		String timestamped_bn_name = this.name+"["+timestamp+"]";
		BeliefNetwork slice = (BeliefNetwork) slices.get( timestamped_bn_name );

		if ( slice == null ) slice = create_timeslice( timestamp );

		Variable x = (Variable) slice.variables.get( variable_name );

		return x;
	}

	/** Create a new timeslice of this temporal belief network. Each timeslice is a <tt>BeliefNetwork</tt>,
	  * with links to the previous timeslice or timeslices, and links to other belief networks are
	  * established as necessary.
	  *
	  * <p> If some timeslices have been created and all of them destroyed, so that there are
	  * no slices left, then the created timeslice looks just as if it were the first slice.
	  * This might not be the best choice; probably the information from the now-destroyed
	  * slices should be wrapped into the priors for the newly-created slice.
	  */
	public BeliefNetwork create_timeslice( long timestamp ) throws Exception
	{
		check_stale( "create_timeslice" );

		BeliefNetwork slice = (BeliefNetwork) template.getClass().newInstance();

		slice.variables = new NullValueHashtable();
		slice.name = template.name+"."+"slice["+timestamp+"]";
		slice.stale = false;
		slice.belief_network_context = this.belief_network_context;

		slices.put( slice.name, slice );

		for ( Enumeration evariables = template.variables.elements(); evariables.hasMoreElements(); )
		{
			Variable x;
			
			// Try to make sure that x has same type as corresponding variable in template.
			Object o = evariables.nextElement();
			if ( o == NullValueHashtable.NULL_VALUE )
				x = new Variable();
			else
			{
				Variable template_x = (Variable) o;
				x = (Variable) template_x.getClass().newInstance();
				x.type = template_x.type;
				x.states_names = template_x.states_names; // don't bother to clone
				x.name = template_x.name;
				x.distribution = (ConditionalDistribution)((Variable)template_x).distribution.clone();
				x.belief_network = slice;
			}

			x.belief_network = slice;
			if ( x.distribution instanceof AbstractConditionalDistribution )
				((AbstractConditionalDistribution)x.distribution).associated_variable = x;
   
   			slice.variables.put( x.name, x );
		}

		// Now run through the list of template variables again, this time to set up parent links.
		// Look for parent names of the form "prev[xxx]" -- this represents the xxx variable in the
		// most recent timeslice. For the first timeslice, a parent reference is allocated but set
		// to null, and the pi message from that parent is taken as the parent prior.

		for ( Enumeration template_variables = template.variables.elements(); template_variables.hasMoreElements(); )
		{
			Variable template_x = (Variable) template_variables.nextElement();
			String slice_variable_name = template_x.name;
			Variable slice_x = (Variable) slice.name_lookup( slice_variable_name );
			Enumeration eparents_names = template_x.parents_names.elements();
			while ( eparents_names.hasMoreElements() )
			{
				String pname = (String) eparents_names.nextElement();
				if ( pname.startsWith("prev[") && pname.endsWith("]") )
				{
					String real_pname = pname.substring(0,pname.length()-1).substring(5);
					if ( most_recent != null && ! most_recent.is_stale() )
						slice_x.add_parent( most_recent.name+"."+real_pname );
					else
					{
						// Create an "anchor" variable which has no parents, and make that the parent
						// of slice_x. As its distribution, the anchor will have the prior that was
						// specified in the template description.

						String anchor_name = real_pname+"-anchor";
						Variable anchor;

						try { anchor = (Variable) slice.variables.get(anchor_name); }
						catch (NoSuchElementException e) { anchor = null; }

						if ( anchor == null )
						{
							anchor = (Variable) template_x.getClass().newInstance();
							anchor.name = anchor_name;
System.err.println( "construct anchor variable "+anchor.name+" for "+slice_x.name );
							anchor.distribution = (Distribution) template_x.parents_priors_hashtable.get(pname);
System.err.println( "\t"+"anchor.distribution "+(anchor.distribution==null?"is null":"is NOT null")+"; pname: "+pname );
							anchor.belief_network = slice;
							slice.variables.put( anchor.name, anchor );
						}

						slice_x.add_parent( anchor.name );
					}
				}
				else
					slice_x.add_parent( pname );
			}
		}

		slice.assign_references();
		
		most_recent = slice;	// SLICES CAN ONLY BE CREATED IN ORDER OF INCREASING TIMESTAMP !!!
		if ( shadow_most_recent != null ) shadow_most_recent.set_stale();
		shadow_most_recent = create_shadow( most_recent );
		return slice;
	}

	/** Create a shadow of a belief network: create a belief network with variables
	  * which have the same names as variables in the b.n. to be shadowed,
		* make each shadowing variables a child of the corresponding variable in
		* the shadowed b.n., and set the conditional distribution of each child to
		* <tt>riso.distributions.Identity</tt>.
		*/
	public BeliefNetwork create_shadow( BeliefNetwork shadowed_bn ) throws RemoteException
	{
		BeliefNetwork shadow_bn = new BeliefNetwork();
		shadow_bn.name = shadowed_bn.name+"-shadow";
		shadow_bn.belief_network_context = shadowed_bn.belief_network_context;

		for ( Enumeration e = shadowed_bn.variables.elements(); e.hasMoreElements(); )
		{
			Variable x = (Variable) e.nextElement(), shadow_x = new Variable();
			shadow_x.name = x.name;
			shadow_x.type = x.type;
			shadow_x.distribution = new riso.distributions.Identity();
			shadow_x.belief_network = shadow_bn;
			shadow_x.parents_names.addElement( x.name );
			shadow_x.add_parent(x,0);
			shadow_bn.variables.put( shadow_x.name, shadow_x );
		}

		return shadow_bn;
	}

	/** Destroy a timeslice. The slice is removed from the list of slices in
	  * this temporal bn, and the slice is marked stale so that all operations
	  * on it will fail. The evidence in the specified timeslice and previous
	  * slices is rolled up and put into anchor variable in the next slice. 
	  */
	public void destroy_timeslice( long timestamp )
	{
		String slice_name = template.name+"."+"slice["+timestamp+"]";
		BeliefNetwork slice = (BeliefNetwork ) slices.remove( slice_name );
		if ( slice == most_recent ) shadow_most_recent.set_stale();
		slice.set_stale();
		// WHAT ABOWT THE PRIOR SETTIMG BVSIMESS ???
	}

	/** Read a description of this belief network from an input stream.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		check_stale( "pretty_input" );

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "TemporalBeliefNetwork.pretty_input: input doesn't have opening bracket; parser state: "+st );

		st.nextToken();
		try { template = (BeliefNetwork) java.rmi.server.RMIClassLoader.loadClass(st.sval).newInstance(); }
		catch (Exception e) { throw new IOException( "TemporalBeliefNetwork.pretty_input: failed, "+e ); }

		st.nextToken();
		template.name = st.sval;

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "TemporalBeliefNetwork.pretty_input: template description doesn't have opening bracket; parser state: "+st );

		for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
		{
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				String variable_type = st.sval;
				Variable new_variable = null;

				try
				{
					Class variable_class = java.rmi.server.RMIClassLoader.loadClass( variable_type );
					new_variable = (Variable) variable_class.newInstance();
				}
				catch (Exception e)
				{
					throw new IOException("TemporalBeliefNetwork.pretty_input: can't create an object of type "+variable_type );
				}

				new_variable.belief_network = template;
				new_variable.pretty_input(st);
				template.variables.put( new_variable.name, new_variable );
System.err.println( "pretty_input: put "+new_variable.name );
			}
			else
			{
				throw new IOException( "TemporalBeliefNetwork.pretty_input: unexpected token: "+st );
			}
		}

		name = template.name;
	}

	/** Create a description of this temporal belief network as a string. 
	  */
	public String format_string() throws RemoteException
	{
		check_stale( "format_string" );

		String result = "";
		result += this.getClass().getName()+" "+name+"\n"+"{"+"\n";

		for ( Enumeration eslice = slices.elements(); eslice.hasMoreElements(); )
		{
			BeliefNetwork bn = (BeliefNetwork) eslice.nextElement();
			result += "\t"+"% "+bn.get_fullname()+"\n";
			for ( Enumeration evar = bn.variables.elements(); evar.hasMoreElements(); )
			{
				AbstractVariable x = (AbstractVariable) evar.nextElement();
				result += x.format_string( "\t" );
			}
		}

		result += "}"+"\n";
		return result;
	}

	/** Test out this class.
	  */
	public static void main( String[] args )
	{
		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			TemporalBeliefNetwork tbn = (TemporalBeliefNetwork) bnc.load_network( args[0] );
			bnc.rebind(tbn);

			Thread t = new Thread(new KbdRunner(tbn));
			t.setPriority( Thread.MIN_PRIORITY );
			t.start();
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); }
	}
}

class KbdRunner implements Runnable
{
	TemporalBeliefNetwork tbn;

	KbdRunner( TemporalBeliefNetwork tbn ) { this.tbn = tbn; }
	
	public void run()
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			int idestroy = 1, icreate = 1;

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( "-".equals(st.sval) )
				{
					System.err.println( "---------- destroy time slice["+idestroy+"] ------------" );
					tbn.destroy_timeslice(idestroy);
					++idestroy;
				}
				else if ( "+".equals(st.sval) )
				{
					System.err.println( "----------- create time slice["+icreate+"] -------------" );
					tbn.create_timeslice(icreate);
					++icreate;
				}
				else if ( "?".equals(st.sval) )
					System.err.println( "tbn:"+"\n"+tbn.format_string() );
				else
					System.err.println( "what? st: "+st );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
		System.exit(1);
	}
}
