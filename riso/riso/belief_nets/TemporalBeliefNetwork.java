package riso.belief_nets;

import java.io.*;
import java.util.*;
import java.rmi.*;
import riso.distributions.*;
import riso.remote_data.*;
import SmarterTokenizer;

public class TemporalBeliefNetwork extends BeliefNetwork 
{
	BeliefNetwork template, most_recent;
	Hashtable slices = new Hashtable();

	/** Do-nothing constructor, exists just to throw <tt>RemoteException</tt>.
	  */
	public TemporalBeliefNetwork() throws RemoteException {}

	/** If <tt>variable_name</tt> is a simple name, e.g. <tt>density</tt>, then
	  * return a reference to the most recent instance (i.e., greatest
	  * timestamp) of the variable of the given name.
	  * Otherwise, the name is a compound name, e.g. <tt>slice[192].density</tt>, so
	  * <tt>density</tt> is sought within the belief network <tt>slice[192]</tt> which
	  * is contained within this top-level belief network.
	  * Returns <tt>null</tt> if the variable isn't in this belief network.
	  */
	public Remote name_lookup( String variable_name ) throws RemoteException
	{
		check_stale( "name_lookup" );

		int period_index = variable_name.indexOf(".");

		if ( period_index == -1 )
			// Simple name.
			return (Remote) most_recent.variables.get( variable_name );
		else
		{
			// Compound name -- punt.
			String slice_name = template.name+"."+variable_name.substring(0,period_index);
			BeliefNetwork slice = (BeliefNetwork) slices.get(slice_name);
			if ( slice == null ) return null;
			return slice.name_lookup( variable_name.substring(period_index+1) );
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
	  */
	public BeliefNetwork create_timeslice( long timestamp ) throws Exception
	{
		check_stale( "create_timeslice" );

		BeliefNetwork slice = (BeliefNetwork) template.getClass().newInstance();

		slice.variables = new NullValueHashtable();
		slice.name = template.name+".slice["+timestamp+"]";
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
System.err.println( "extracted real pname: "+real_pname+", from "+pname );
					if ( most_recent != null ) // SHOULD add_parent EVEN IF null AND USE PRIOR !!!
						slice_x.add_parent( most_recent.name+"."+real_pname );
				}
				else
					slice_x.add_parent( pname );
			}
		}

		slice.assign_references();
		
		most_recent = slice;	// SLICES CAN ONLY BE CREATED IN ORDER OF INCREASING TIMESTAMP !!!
		return slice;
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

				new_variable.belief_network = this;
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

			System.err.println( "----------- create time slice[1] -------------" );
			tbn.create_timeslice(1);
			System.err.println( "----------- create time slice[2] -------------" );
			tbn.create_timeslice(2);
			System.err.println( "----------- create time slice[3] -------------" );
			tbn.create_timeslice(3);
			System.err.println( "tbn:"+"\n"+tbn.format_string() );
		}
		catch (Exception e) { e.printStackTrace(); }
		System.exit(1);
	}
}
