package risotto.belief_nets;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import risotto.distributions.*;
import risotto.remote_data.*;

public class BeliefNetwork extends RemoteObservableImpl implements AbstractBeliefNetwork
{
	Hashtable variables = new NullValueHashtable();
	String name = null;

	/** Create an empty belief network. The interesting initialization
	  * occurs in <tt>pretty_input</tt>. A belief network can also be
	  * built by creating new variables and linking them in
	  * using <tt>add_variable</tt>.
	  * @see pretty_input
	  * @see add_variable
	  */
	public BeliefNetwork() throws RemoteException {}

	/** Simplified representation of this belief network,
	  * especially useful for debugging. 
	  */
	public String toString()
	{
		String classname = this.getClass().getName(), n = null;
		try { n = get_name(); }
		catch (RemoteException e) { n = "(unknown name)"; }
		return "["+classname+" "+n+", "+variables.size()+" variables]";
	}

	/** Like <tt>toString</tt>, but this implementation returns something
	  * useful when this object is remote.
	  */
	public String remoteToString()
	{
		return toString()+"(remote)";
	}

	/** Retrieve the name of this belief network.
	  * This includes the name of the host and the RMI registry port number,
	  * if different from the default port number.
	  */
	public String get_name() throws RemoteException
	{
		String ps = BeliefNetworkContext.registry_port==Registry.REGISTRY_PORT ? "" : ":"+BeliefNetworkContext.registry_port;
		String host = null;
		try { host = InetAddress.getLocalHost().getHostName().toLowerCase(); }
		catch (java.net.UnknownHostException e) { host = "(unknown host)"; }
		return host+ps+"/"+name;
	}

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public Enumeration get_variables() throws RemoteException
	{
		return variables.elements();
	}

	/** Mark the variable <tt>x</tt> as not observed. Clear the posterior
	  * distributions for the variables to which <tt>x</tt> is d-connected.
	  * Do not recompute posterior probabilities in the belief network.
	  */
	public void clear_evidence( AbstractVariable x ) throws RemoteException
	{
		// if ( ! x.is_evidence )
			// return;

		// x.is_evidence = false;

		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
		{
			// if ( d_connected_thru_parent( variables[i], x ) )
			// {
				// variables[i].pi = null;
				// variables[i].posterior = null;
			// }
			// else if ( d_connected_thru_child( variables[i], x ) )
			// {
				// variables[i].lambda = null;
				// variables[i].posterior = null;
			// }
		}
	}

	/** Mark all variables as not observed.
	  * Clear all posterior distributions, since they are no longer valid.
	  * Do not recompute posterior probabilities in the belief network.
	  * COULD TRY HARDER TO ONLY CLEAR WHAT NEEDS TO BE CLEARED !!!
	  */
	public void clear_all_evidence() throws RemoteException
	{
		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
		{
			// variables[i].is_evidence = false;
			// variables[i].posterior = null;
		}
	}

	public void assign_evidence( AbstractVariable x, double value ) throws RemoteException
	{
		System.err.println( "BeliefNetwork.assign_evidence: assign "+value+" to "+x.get_name()+"." );
	}

	public void compute_posterior( AbstractVariable x ) throws RemoteException
	{
		System.err.println( "BeliefNetwork.compute_posterior: variable: "+x.get_name() );
	}

	public void compute_all_posteriors() throws RemoteException
	{
		System.err.println( "BeliefNetwork.compute_all_posteriors: not implemented." );
	}

	/** @throws IllegalArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( AbstractVariable x, AbstractVariable e ) throws RemoteException, IllegalArgumentException
	{
		throw new IllegalArgumentException("BeliefNetwork.compute_information: not yet.");
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable x ) throws RemoteException
	{
		Variable xx = (Variable) x;	// WILL THIS WORK REMOTELY ???

		if ( xx.posterior == null )
			compute_posterior(xx);
		return xx.posterior;
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable[] x ) throws RemoteException
	{
		return null;
	}

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException, RemoteException
	{
		// HACK CITY !!! COMPLETE THIS !!!

		st.nextToken();
System.err.println( "BeliefNetwork.pretty_input: name: ttype: "+st.ttype+"  sval: "+st.sval );
		name = st.sval;

		st.nextToken();
// !!! System.err.println( "BeliefNetwork.pretty_input: ttype: "+st.ttype+"  sval: "+st.sval );
		if ( st.ttype != '{' )
			throw new IOException( "BeliefNetwork.pretty_input: input doesn't have opening bracket; parser state: "+st );

		for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
		{
// !!! System.err.println( "BeliefNetwork.pretty_input: top of loop: ttype: "+st.ttype+"  sval: "+st.sval );
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				String variable_type = st.sval;
				Variable new_variable = null;

				try
				{
					Class variable_class = Class.forName(variable_type);
					new_variable = (Variable) variable_class.newInstance();
				}
				catch (Exception e)
				{
					throw new IOException("BeliefNetwork.pretty_input: can't create an object of type "+variable_type );
				}

				new_variable.belief_network = this;
				new_variable.pretty_input(st);
				variables.put( new_variable.name, new_variable );
			}
			else
			{
				throw new IOException( "BeliefNetwork.pretty_input: unexpected token: "+st );
			}
		}

		try { assign_references(); }
		catch (UnknownParentException e)
		{
			throw new IOException( "BeliefNetwork.pretty_input: attempt to read belief network failed:\n"+e );
		}
	}

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_output( OutputStream os ) throws IOException, RemoteException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( this.getClass().getName()+" "+name+"\n"+"{"+"\n" );

		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
		{
			AbstractVariable x = (AbstractVariable) enum.nextElement();
			x.pretty_output( os, "\t" );
		}

		dest.print( "}"+"\n" );
	}


	/** Write a description of this belief network to a string.
	  * using the format required by the "dot" program. All the probabilistic
	  * information is thrown away; only the names of the variables and
	  * the parent-to-child relations are kept.
	  * See <a href="http://www.research.att.com/sw/tools/graphviz/">
	  * the Graphviz homepage</a> for information about "dot" and other
	  * graph visualization software.
	  *
	  * @return A string containing the belief network in "dot" format.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public String dot_format() throws RemoteException
	{
		String result = "";
		result += "digraph \""+name+"\" {\n";

		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
		{
			AbstractVariable x = (AbstractVariable) enum.nextElement();
			String xname = x.get_name();

			result += " \""+xname+"\";\n";

			Enumeration enump = x.get_parents_names();
			while ( enump.hasMoreElements() )
				result += " \""+(String)enump.nextElement()+"\"->\""+xname+"\";\n";
		}

		result += "}\n";
		return result;
	}

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public AbstractVariable name_lookup( String some_name ) throws RemoteException
	{
		return (AbstractVariable) variables.get(some_name);
	}

	/** Add a variable to the belief network. A new object of type
	  * <tt>Variable</tt> is created if the argument <tt>new_variable</tt>
	  * is <tt>null</tt>, otherwise <tt>new_variable</tt> is used.
	  * A caller that wants to construct a belief network out of variables
	  * more complicated than <tt>Variable</tt> should allocate a new
	  * instance of the desired class (which must be derived from
	  * <tt>Variable</tt>) and pass that in.
	  * @param name Name of the new variable.
	  * @param parents_names Names of the parents of the new variable. These
	  *   can include names of the form <tt>some_bn.some_variable</tt>, where
	  *   <tt>some_bn</tt> is the name of another belief network; an attempt
	  *   will be made to locate the referred-to belief network.
	  * @return A reference to the new variable.
	  * @throws UnknownParentException If a parent cannot be located.
	  */
	public AbstractVariable add_variable( String name_in, String[] parents_names, AbstractVariable new_variable ) throws UnknownParentException, RemoteException
	{
		Variable xx = (Variable) new_variable;

		if ( xx == null )
			xx = new Variable();

		xx.name = name_in;
		xx.belief_network = this;
		// FILL THIS IN !!!

		return xx;
	}

	/** Assign references to parents and children. This is usually called
	  * once, just after initialization, but it could be called more than
	  * once. For example, if one edits the belief network via
	  * <tt>add_variable</tt> and <tt>remove_variable</tt>, this function
	  * should be called to update the references after editing.
	  * @throws UnknownParentException If a reference for some parent
	  *   referred to by a variable in this network cannot be obtained.
	  */
	void assign_references() throws UnknownParentException
	{
		if ( variables.size() == 0 )
			// Empty network -- no references to assign.
			return;

		try { locate_references(); }
		catch (UnknownNetworkException e)
		{
			throw new UnknownParentException( "BeliefNetwork.assign_references: some referred-to network can't be located:\n"+e );
		}

// !!! System.err.println( "BeliefNetwork.assign_references: top of main loop." );

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			Variable x = (Variable) enumv.nextElement();

			Enumeration parents_names = x.parents.keys();
			while ( parents_names.hasMoreElements() )
			{
				String parent_name = (String) parents_names.nextElement();
// !!! System.err.println( "variable: "+x.name+"  parent_name: "+parent_name );

				int period_index;
				if ( (period_index = parent_name.lastIndexOf(".")) != -1 )
				{
// !!! System.err.println( parent_name+" is in some other belief network." );
					// Parent is in some other belief network -- first get a reference to the
					// other belief network, then get a reference to the parent variable within
					// the other network.

					try 
					{
						String parent_bn_name = parent_name.substring( 0, period_index );
// !!! System.err.println( "other belief network name: "+parent_bn_name );
						AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) BeliefNetworkContext.reference_table.get( parent_bn_name );
						AbstractVariable p = parent_bn.name_lookup( parent_name.substring( period_index+1 ) );
System.err.println( "parent network: "+parent_bn.remoteToString() );
// !!! System.err.println( "parent reference is "+(p==null?"null":"non-null") );
						x.parents.put( parent_name, p );	// p could be null here
						if ( p != null ) p.add_child( x.name, x );
					}
					catch (Exception e)
					{
						System.err.println( "BeliefNetwork.assign_references: failed attempt to look up: "+parent_name );
						System.err.println( "  exception: "+e );
					}
				}
				else
				{
					// Parent is within this belief network.

					try
					{
						Variable p = (Variable) name_lookup(parent_name);
// !!! System.err.println( "parent reference is "+(p==null?"null":"non-null") );
						x.parents.put( parent_name, p );	// p could be null here
						if ( p != null ) p.add_child( x.name, x );
					}
					catch (RemoteException e)
					{
						// Should never happen, as the parent is local; what to do???
						System.err.println( "BeliefNetwork.assign_references: failed attempt to look up: "+parent_name );
						System.err.println( "  unexpected RemoteException"+e );
					}
				}
			}
		}
	}

	/** Verify that all other networks referred to by this one can be
	  * located. First go through the list of all the parents' names 
	  * in this network and find any parents of the form "<tt>something.x</tt>".
	  * See if each <tt>something</tt> on the list of networks for which
	  * a reference exists; if not, try to add it to the list. If it can't
	  * be added to the list, throw an exception. If this method returns
	  * without throwing an exception, it means that any referred-to networks 
	  * are now in the reference table.
	  *
	  * @throws UnknownNetworkException If some network can't be located.
	  * @see BeliefNetworkContext.reference_table
	  * @see BeliefNetworkContext.load_network
	  */
	void locate_references() throws UnknownNetworkException
	{
		int i, j;

		if ( variables.size() == 0 )
			// Empty network; no references to locate.
			return;

// !!! System.err.println( "BeliefNetwork.locate_references: top of main loop." );

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			// DOES THIS WORK ??? IT SHOULD SINCE WE ARE WORKING W/ LOCALS !!!
			Variable x = (Variable) enumv.nextElement();
System.err.println( "x: "+x );

			Enumeration parents_names = x.parents.keys();
			while ( parents_names.hasMoreElements() )
			{
				String parent_name = (String) parents_names.nextElement();
// !!! System.err.println( "variable: "+x.name+"  parent_name: "+parent_name );

				int period_index;
				if ( (period_index = parent_name.lastIndexOf(".")) != -1 )
				{
// !!! System.err.println( parent_name+" is in another belief network." );
					String bn_name = parent_name.substring(0,period_index);
// !!! System.err.println( "belief network name: "+bn_name );

					if ( BeliefNetworkContext.reference_table.get(bn_name) == null )
					{
						// We need to obtain a reference to the parent's b.n.,
						// which is either remote or on the local disk.
						// If remote, its name has the form "host/bn.x".
						// Otherwise, it must be on the local disk.

						int slash_index;
						if ( (slash_index = parent_name.lastIndexOf("/")) != -1 )
						{
							// Remote network, try to look it up.
							BeliefNetworkContext.add_rmi_reference( bn_name );
// !!! System.err.println( "successfully looked up remote network: "+bn_name );
						}
						else
						{
							// Try to load from local disk.
// !!! System.err.println(  "no reference for "+bn_name+" in table, try to load it." );
							try { BeliefNetworkContext.load_network(bn_name); }
							catch (IOException e)
							{
								throw new UnknownNetworkException( "BeliefNetwork.locate_references: attempt to load network failed:\n"+e );
							}
// !!! System.err.println( "successfully loaded belief network: "+bn_name );
						}
					}
				}
			}
		}
	}
}

/** This is a helper class to handle updates from any remote belief network
  * which has parents or children in the local belief network; this class
  * handles updates for the remote belief network as a whole.
  */
class BeliefNetworkObserver implements RemoteObserver
{

	BeliefNetworkObserver() throws RemoteException {}

	/** This method is called after the remote belief network has notified
	  * us (via <tt>VariableObserver.update</tt>) that there are changed
	  * variables. Now we start the computations to recompute the posterior
	  * distributions as needed in the local belief network, and we also
	  * pass the notice on to any belief networks observing the local
	  * network. The computations are carried out in a separate thread
	  * so that we can return quickly to the remote observable which called us.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		// SYNCHRONIZATION PROBLEM HERE !!! WHEN WILL POSTERIORS BE AVAILABE ???
		// (compute_thread = new Thread(new ComputePosteriorsRunner())).start();
		// local_bn.notify_all_observers();
	}
}

/** This is a helper class to handle updates from any remote belief network
  * which has parents or children in the local belief network; this class
  * handles updates of variables in the remote belief network.
  */
class VariableObserver implements RemoteObserver
{
	VariableObserver() throws RemoteException {}

	/** This method is called when a variable in a remote belief network
	  * which is a parent or a child of some variable in the local network
	  * is changed; generally that means we need to recompute the 
	  * posterior distributions in the local belief network.
	  * So the immediate action is to remove the current posteriors for
	  * all the variables d-connected to the one which has changed.
	  * When all variable updates are finished, an update for the 
	  * remote belief network as a whole will be issued, which is handled
	  * by <tt>BeliefNetworkObserver.update</tt>.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		risotto.belief_nets.AbstractVariable x = (risotto.belief_nets.AbstractVariable) of_interest;

		// NOW FIND THE SUBNETWORK WHICH CONSISTS OF ALL THE VARIABLES
		// WHICH ARE D-CONNECTED TO x, AND REMOVE THEIR POSTERIORS !!!
		// FORWARD TO UPDATE TO ANY OBSERVERS WHICH ARE WATCH VARIABLES
		// IN THE SET D-CONNECTED-TO(x) !!!
	}
}
