package risotto.belief_nets;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import risotto.distributions.*;
import risotto.remote_data.*;
import SmarterTokenizer;

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

	/** Mark the variables in <tt>xx</tt> as not observed.
	  * Clear any cached variables which represent information that must be
	  * revised, but do not carry out the revision. Notify remote observers
	  * that these variables are no longer evidence (if ever they were).
	  */
	public void clear_evidence( Enumeration some_variables ) throws RemoteException
	{
		// INCOMPLETE !!!

		while ( some_variables.hasMoreElements() )
		{
			AbstractVariable absx = (AbstractVariable) some_variables.nextElement();
			Variable x;

			// GENERAL POLICY ENFORCED HERE: ALLOW CHANGES TO MEMBER DATA ONLY IF !!!
			// THE VARIABLE IS LOCAL AND NOT REMOTE !!! OTHERWISE A WHOLE SET OF
			// "get/set" METHODS IS REQUIRED -- BARF. !!! 

			try { x = (Variable) absx; }
			catch (ClassCastException ex)
			{
				System.err.println( "BeliefNetwork.clear_evidence: "+absx.get_fullname()+" is "+absx.getClass().getName()+" (not derived from Variable)" );
				continue;
			}

			if ( ! (x.posterior instanceof DeltaDistribution) )
				continue;

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

	public void assign_evidence( AbstractVariable some_variable, double value ) throws RemoteException
	{
		Variable x = to_Variable( some_variable, "BeliefNetwork.assign_evidence" );

		// GENERAL POLICY ENFORCED HERE: ALLOW CHANGES TO MEMBER DATA ONLY IF !!!
		// THE VARIABLE IS LOCAL AND NOT REMOTE !!! OTHERWISE A WHOLE SET OF
		// "get/set" METHODS IS REQUIRED -- BARF. !!! 

		DeltaDistribution delta = new DeltaDistribution();
		delta.support_point = new double[1];
		delta.support_point[0] = value;
		x.posterior = delta;

		x.pi = null;
		x.lambda = null;
	}

	public Distribution get_pi_message( Variable some_parent, Variable some_child ) throws Exception
	{
		if ( some_child.pi_messages.get(some_parent) == null )
			compute_pi_message( some_parent, some_child );
		return (Distribution) some_child.pi_messages.get(some_parent);
	}

	public Distribution get_pi( Variable x ) throws Exception
	{
		if ( x.pi == null )
			compute_pi( x );
		return x.pi;
	}

	public Distribution compute_pi_message( Variable parent, Variable child ) throws Exception
	{
		Object childs_lambda_message = null;

		// To compute a pi message for the child, we need to incorporate lambda messages
		// from all children except for the one to which we are sending the pi message.
		// So use an enumerator which won't return the child's lambda message.

		SkipsEnumeration remaining_lambda_messages = new SkipsEnumeration( parent.lambda_messages, child );
		PiMessageHelper pmh = PiLambdaMessageHelperLoader.load_pi_message_helper( get_pi(parent), (Enumeration)remaining_lambda_messages );
		if ( pmh == null ) 
			throw new Exception( "BeliefNetwork.compute_pi_message: attempt to load pi helper class failed; parent: "+parent.get_name()+" child: "+child.get_name() );

System.out.println( "BeliefNetwork.compute_pi_message: parent: "+parent.get_name()+" child: "+child.get_name() );
System.out.println( "  loaded helper: "+pmh.getClass() );
		remaining_lambda_messages.rewind();
		Distribution pi_message = pmh.compute_pi_message( parent.pi, (Enumeration)remaining_lambda_messages );
System.out.println( "BeliefNetwork.compute_pi_message: pi message:\n"+pi_message.format_string( "--" ) );
		child.pi_messages.put( parent, pi_message );
		return pi_message;
	}

	public Distribution compute_pi( Variable x ) throws Exception
	{
		// Special case: if node x is a root node, its pi is just its distribution.

		if ( x.parents.size() == 0 )
		{
			x.pi = (Distribution) x.distribution;
			return x.pi;
		}

		// General case: x is not a root node; collect pi-messages from parents,
		// then use x's distribution and those pi-messages to compute pi.

		for ( Enumeration e = x.get_parents(); e.hasMoreElements(); )
			get_pi_message( to_Variable( e.nextElement(), "BeliefNetwork.compute_pi" ), x );

		PiHelper ph = PiLambdaHelperLoader.load_pi_helper( x.distribution, x.pi_messages.elements() );
		if ( ph == null ) 
			throw new Exception( "BeliefNetwork.compute_pi: attempt to load pi helper class failed; x: "+x.get_fullname() );
System.out.println( "BeliefNetwork.compute_pi: x: "+x.get_fullname() );
System.out.println( "  loaded helper: "+ph.getClass() );

		// Put pi messages into correspondence with parents.
		// This is really becoming a pain... I should go back to arrays. !!!
		Object[] messages = new Object[ x.parents.size() ];
		Enumeration e = x.parents.elements();
		for ( int i = 0; e.hasMoreElements(); )
			messages[i++] = x.pi_messages.get( e.nextElement() );

		x.pi = ph.compute_pi( x.distribution, new ArrayEnumeration(messages) );
System.out.println( "BeliefNetwork.compute_pi: computed pi:" );
System.out.println( x.pi.format_string( "...." ) );
		return x.pi;
	}

	public Distribution compute_posterior( Variable x ) throws Exception
	{
System.err.println( "BeliefNetwork.compute_posterior: x: "+x.get_fullname() );
		// To compute the posterior for this variable, we need to compute
		// pi and lambda first. For pi, we need a pi-message from each parent
		// and the conditional distribution for this variable; for lambda,
		// we need a lambda-message from each child. Then the posterior is
		// just the product of pi and lambda, but it needs to be normalized.

		get_pi( x );
		// get_lambda( x );
		// PosteriorHelper ph = PosteriorHelperLoader.load_posterior_helper( x.pi, x.lambda );
		// x.posterior = ph.compute_posterior( x.pi, x.lambda );
		// return x.posterior;
		throw new Exception( "BeliefNetwork.compute_posterior: can't handle "+x.get_fullname() );
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
	public Distribution get_posterior( AbstractVariable some_variable ) throws RemoteException
	{
		Variable x = to_Variable( some_variable, "BeliefNetwork.get_posterior" );

		// GENERAL POLICY ENFORCED HERE: ALLOW CHANGES TO MEMBER DATA ONLY IF !!!
		// THE VARIABLE IS LOCAL AND NOT REMOTE !!! OTHERWISE A WHOLE SET OF
		// "get/set" METHODS IS REQUIRED -- BARF. !!! 

		try
		{
			if ( x.posterior == null )
				compute_posterior(x);
			return x.posterior;
		}
		catch (Exception e)
		{
			throw new RemoteException( "BeliefNetwork.get_posterior: "+e );
		}
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution get_posterior( AbstractVariable[] x ) throws RemoteException
	{
		throw new RemoteException( "BeliefNetwork.get_posterior: not implemented." );
	}

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		// This code is a sizeable hack. I should make it more sensible.

		st.nextToken();
		name = st.sval;

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "BeliefNetwork.pretty_input: input doesn't have opening bracket; parser state: "+st );

		for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
		{
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

	/** Parse a string containing a description of a belief network. The description
	  * is contained within curly braces, which are included in the string.
	  * The content in curly braces is preceded by the name of the belief network.
	  */
	public void parse_string( String description ) throws IOException, RemoteException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this belief network as a string. 
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  */
	public String format_string() throws RemoteException
	{
		String result = "";
		result += this.getClass().getName()+" "+name+"\n"+"{"+"\n";

		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
		{
			AbstractVariable x = (AbstractVariable) enum.nextElement();
			result += x.format_string( "\t" );
		}

		result += "}"+"\n";
		return result;
	}

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_output( OutputStream os ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string() );
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

		throw new RemoteException( "BeliefNetwork.add_variable: incomplete implementation." );

		// FILL THIS IN !!!
		// return xx;
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

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			Variable x = (Variable) enumv.nextElement();

			Enumeration parents_names = x.parents.keys();
			while ( parents_names.hasMoreElements() )
			{
				String parent_name = (String) parents_names.nextElement();
System.err.println( "BeliefNetwork.assign_references: parent_name: "+parent_name );

				int period_index;
				if ( (period_index = parent_name.lastIndexOf(".")) != -1 )
				{
					// Parent is in some other belief network -- first get a reference to the
					// other belief network, then get a reference to the parent variable within
					// the other network.

					try 
					{
						String parent_bn_name = parent_name.substring( 0, period_index );
						AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) BeliefNetworkContext.reference_table.get( parent_bn_name );
						AbstractVariable p = parent_bn.name_lookup( parent_name.substring( period_index+1 ) );
System.err.println( "parent network: "+parent_bn.remoteToString() );
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

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			// DOES THIS WORK ??? IT SHOULD SINCE WE ARE WORKING W/ LOCALS !!!
			Variable x = (Variable) enumv.nextElement();

			Enumeration parents_names = x.parents.keys();
			while ( parents_names.hasMoreElements() )
			{
				String parent_name = (String) parents_names.nextElement();

				int period_index;
				if ( (period_index = parent_name.lastIndexOf(".")) != -1 )
				{
					String bn_name = parent_name.substring(0,period_index);

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
						}
						else
						{
							// Try to load from local disk.
							try { BeliefNetworkContext.load_network(bn_name); }
							catch (IOException e)
							{
								throw new UnknownNetworkException( "BeliefNetwork.locate_references: attempt to load network failed:\n"+e );
							}
						}
					}
				}
			}
		}
	}

	protected static Variable to_Variable( Object some_variable, String msg_leader ) throws RemoteException
	{
		try { return  (Variable) some_variable; }
		catch (ClassCastException ex)
		{
			throw new RemoteException( msg_leader+": "+some_variable+" is not derived from Variable" ); 
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
