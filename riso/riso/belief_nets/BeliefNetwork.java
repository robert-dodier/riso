package risotto.belief_nets;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import risotto.distributions.*;
import risotto.remote_data.*;
import SmarterTokenizer;

public class BeliefNetwork extends RemoteObservableImpl implements AbstractBeliefNetwork, Serializable
{
	Hashtable variables = new NullValueHashtable();
	String name = null;

	/** The context to which this belief network belongs. This variable is set by
	  * <tt>BeliefNetworkContext.load_network</tt> and by <tt>BeliefNetworkContext.parse_network</tt>.
	  * Since this variable is publicly accessible, the context can be changed at any time.
	  */
	public BeliefNetworkContext belief_network_context = null;

	/** Create an empty belief network. The interesting initialization
	  * occurs in <tt>pretty_input</tt>. A belief network can also be
	  * built by creating new variables and linking them in
	  * using <tt>add_variable</tt>.
	  * @see pretty_input
	  * @see BeliefNetwork.add_variable, Variable.add_parent
	  */
	public BeliefNetwork() throws RemoteException {}

	/** Simplified representation of this belief network,
	  * especially useful for debugging. 
	  */
	public String toString()
	{
		String classname = this.getClass().getName(), n = null;
		try { n = get_fullname(); }
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

	/** Retrieve just the name of this belief network.
	  */
	public String get_name() throws RemoteException
	{
		return name;
	}

	/** Retrieve the full name of this belief network.
	  * This includes the name of the registry host from which this
	  * belief network may be retrieved, and the registry port number,
	  * if different from the default port number.
	  */
	public String get_fullname() throws RemoteException
	{
		String ps = BeliefNetworkContext.registry_port==Registry.REGISTRY_PORT ? "" : ":"+BeliefNetworkContext.registry_port;
		return BeliefNetworkContext.registry_host+ps+"/"+name;
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

			if ( ! (x.posterior instanceof Delta) )
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

		Delta delta;
		if ( x.type == Variable.VT_DISCRETE )
		{
			int[] support_point = new int[1];
			support_point[0] = (int)value;

			if ( x.distribution instanceof Discrete )
				delta = new DiscreteDelta( ((Discrete)x.distribution).dimensions, support_point );
			else if ( x.distribution instanceof ConditionalDiscrete )
				delta = new DiscreteDelta( ((ConditionalDiscrete)x.distribution).dimensions_child, support_point );
			else
				throw new RemoteException( "BeliefNetwork.assign_evidence: don't know about "+x.distribution.getClass() );		}
		else
			throw new RemoteException( "BeliefNetwork.assign_evidence: don't know how to assign to "+x.get_fullname() );

		x.posterior = delta;

		x.pi = null;
		x.lambda = null;
	}

	public void get_all_lambda_messages( Variable x ) throws Exception
	{
		for ( int i = 0; i < x.children.length; i++ )
			if ( x.lambda_messages[i] == null )
			{
				// SEE NOTE IN get_all_pi_messages !!!
				Variable child = to_Variable( x.children[i], "BeliefNetwork.get_all_pi_messages" );
				x.lambda_messages[i] = compute_lambda_message( x, child );
			}
	}

	public void get_all_pi_messages( Variable x ) throws Exception
	{
		for ( int i = 0; i < x.parents.length; i++ )
			if ( x.pi_messages[i] == null )
			{
				// SOMEHOW NEED TO COMMUNICAE PI MESSAGES FROM ONE BN TO !!!
				// ANOTHER. PROBABLY NEED get_pi_message( parent, child ) !!!
				// IN AbstractBeliefNetwork, AND SOMETHING LIKE !!!
				// x.pi_messages[i] = x.parents[i].get_bn().get_pi_msg(...) !!!
				Variable parent = to_Variable( x.parents[i], "BeliefNetwork.get_all_pi_messages" );
				x.pi_messages[i] = compute_pi_message( parent, x );
			}
	}

	/** This method DOES NOT put the newly computed lambda message into the
	  * list of lambda messages for the <tt>parent</tt> variable.
	  */
	public Distribution compute_lambda_message( Variable parent, Variable child ) throws Exception
	{
System.err.println( "compute_lambda_message: to: "+parent.get_name()+" from: "+child.get_name() );
		// To compute a lambda message for a parent, we need to incorporate
		// lambda messages coming in from the children of the child, as well
		// as pi messages coming into the child from other parents.
		
		Distribution[] remaining_pi_messages = new Distribution[ child.parents.length ];
		for ( int i = 0; i < child.parents.length; i++ )
			if ( child.parents[i] == parent )
				remaining_pi_messages[i] = null;
			else
			{
				if ( child.pi_messages[i] == null )
				{
					// SEE NOTE SOMEWHERE ELSE !!!
					Variable a_parent = to_Variable( child.parents[i], "BeliefNetwork.compute_lambda_message" );
					child.pi_messages[i] = compute_pi_message( a_parent, child );
				}
				remaining_pi_messages[i] = child.pi_messages[i];
			}

		if ( child.lambda == null ) compute_lambda( child );

		LambdaMessageHelper lmh = LambdaMessageHelperLoader.load_lambda_message_helper( child.distribution, child.lambda, remaining_pi_messages );

		if ( lmh == null )
			throw new Exception( "BeliefNetwork.compute_lambda_message: attempt to load lambda helper class failed;\n\tparent: "+parent.get_name()+" child: "+child.get_name() );

		Distribution lambda_message = lmh.compute_lambda_message( child.distribution, child.lambda, remaining_pi_messages );
System.err.println( "BeliefNetwork.compute_lambda_message: parent: "+parent.get_name()+" child: "+child.get_name() );
System.err.println( "  loaded helper: "+lmh.getClass() );
System.err.println( "BeliefNetwork.compute_lambda_message: lambda message:\n"+lambda_message.format_string( "**" ) );

		return lambda_message;
	}

	/** This method DOES NOT put the newly computed pi message into the
	  * list of pi messages for the <tt>child</tt> variable.
	  */
	public Distribution compute_pi_message( Variable parent, Variable child ) throws Exception
	{
System.err.println( "compute_pi_message: from: "+parent.get_name()+" to: "+child.get_name() );
		// To compute a pi message for the child, we need to incorporate
		// lambda messages from all children except for the one to which
		// we are sending the pi message.

		Distribution[] remaining_lambda_messages = new Distribution[ parent.children.length ];
		for ( int i = 0; i < parent.children.length; i++ )
			if ( parent.children[i] == child )
				remaining_lambda_messages[i] = null;
			else
			{
				if ( parent.lambda_messages[i] == null )
				{
					Variable a_child = to_Variable( parent.children[i], "BeliefNetwork.compute_pi_message" );
					parent.lambda_messages[i] = compute_lambda_message( parent, a_child );
				}
				remaining_lambda_messages[i] = parent.lambda_messages[i];
			}

		if ( parent.pi == null ) compute_pi( parent );
		PiMessageHelper pmh = PiMessageHelperLoader.load_pi_message_helper( parent.pi, remaining_lambda_messages );
		if ( pmh == null ) 
			throw new Exception( "BeliefNetwork.compute_pi_message: attempt to load pi helper class failed; parent: "+parent.get_name()+" child: "+child.get_name() );

System.err.println( "BeliefNetwork.compute_pi_message: parent: "+parent.get_name()+" child: "+child.get_name() );
System.err.println( "  loaded helper: "+pmh.getClass() );
		Distribution pi_message = pmh.compute_pi_message( parent.pi, remaining_lambda_messages );
System.err.println( "BeliefNetwork.compute_pi_message: pi message:\n"+pi_message.format_string( "--" ) );

		return pi_message;
	}

	/** This method DOES set lambda for the variable <tt>x</tt>.
	  */
	public Distribution compute_lambda( Variable x ) throws Exception
	{
System.err.println( "compute_lambda: x: "+x.get_name() );
		// Special case: if node x is instantiated, its lambda is a spike.
		if ( x.posterior instanceof Delta && x.lambda == null )
		{
System.err.println( "compute_lambda: special case, "+x.get_name()+" is evidence node." );
			x.lambda = x.posterior;
			return x.lambda;
		}

		// Special case: if node x is an uninstantiated leaf, its lambda
		// is noninformative.
		if ( x.children.length == 0 )
		{
System.err.println( "compute_lambda: special case, "+x.get_name()+" is uninstantiated leaf." );
			x.lambda = new Noninformative();
			return x.lambda;
		}

System.err.print( "compute_lambda: general case; x.children.length: "+x.children.length );
System.err.println( " posterior is "+(x.posterior==null?"null":("class: "+x.posterior.getClass().getName())));
		// General case: collect lambda-messages from children, 
		// load lambda helper, and compute lambda.

		get_all_lambda_messages( x );

		LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( x.lambda_messages );
		if ( lh == null )
			throw new Exception( "BeliefNetwork.compute_lambda: attempt to load lambda helper class failed; x: "+x.get_fullname() );
System.err.println( "BeliefNetwork.compute_lambda: x: "+x.get_fullname() );
System.err.println( "  loaded helper: "+lh.getClass() );

		x.lambda = lh.compute_lambda( x.lambda_messages );
System.err.println( "BeliefNetwork.compute_lambda: computed lambda:" );
System.err.println( x.lambda.format_string( "...." ) );
		return x.lambda;
	}

	/** This method DOES set pi for the variable <tt>x</tt>.
	  */
	public Distribution compute_pi( Variable x ) throws Exception
	{
System.err.println( "compute_pi: x: "+x.get_name() );
		// Special case: if node x is a root node, its pi is just its distribution.

		if ( x.parents.length == 0 )
		{
			x.pi = (Distribution) x.distribution;
			return x.pi;
		}

		// General case: x is not a root node; collect pi-messages from parents,
		// then use x's distribution and those pi-messages to compute pi.

		get_all_pi_messages( x );

		PiHelper ph = PiHelperLoader.load_pi_helper( x.distribution, x.pi_messages );
		if ( ph == null ) 
			throw new Exception( "BeliefNetwork.compute_pi: attempt to load pi helper class failed; x: "+x.get_fullname() );
System.err.println( "BeliefNetwork.compute_pi: x: "+x.get_fullname() );
System.err.println( "  loaded helper: "+ph.getClass() );

		x.pi = ph.compute_pi( x.distribution, x.pi_messages );
System.err.println( "BeliefNetwork.compute_pi: computed pi:" );
System.err.println( x.pi.format_string( "...." ) );
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

		if ( x.pi == null ) compute_pi( x );
		if ( x.lambda == null ) compute_lambda( x );

		PosteriorHelper ph = PosteriorHelperLoader.load_posterior_helper( x.pi, x.lambda );
		if ( ph == null )
			throw new Exception( "BeliefNetwork.compute_posterior: attempt to load posterior helper class failed; x: "+x.get_fullname() );

		x.posterior = ph.compute_posterior( x.pi, x.lambda );
		return x.posterior;
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
			e.printStackTrace();
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

			String[] parents_names = x.get_parents_names();
			for ( int i = 0; i < parents_names.length; i++ )
				result += " \""+parents_names[i]+"\"->\""+xname+"\";\n";
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
	  * <tt>Variable</tt>) and pass that in. Parents can be linked to the
	  * new variable by <tt>Variable.add_parent</tt>. 
	  *
	  * @param name Name of the new variable.
	  * @param new_variable Reference to variable to add to belief network;
	  *   if <tt>null</tt>, then a new <tt>Variable</tt> object is allocated.
	  * @return A reference to the new variable. If the attempt to create
	  *   a new variable fails, returns <tt>null</tt>.
	  * @see Variable.add_parent
	  */
	public AbstractVariable add_variable( String name_in, AbstractVariable new_variable )
	{
		Variable x = (Variable) new_variable;

		if ( x == null )
		{
			try { x = new Variable(); }
			catch (RemoteException e) { return null; }
		}

		x.name = name_in;
		x.belief_network = this;

		variables.put( x.name, x );
		return x;
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
			x.parents = new AbstractVariable[ x.parents_names.size() ];
			x.pi_messages = new Distribution[ x.parents_names.size() ];

			for ( int i = 0; i < x.parents_names.size(); i++ )
			{
				String parent_name = (String) x.parents_names.elementAt(i);
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
						AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) belief_network_context.reference_table.get( parent_bn_name );
						AbstractVariable p = parent_bn.name_lookup( parent_name.substring( period_index+1 ) );
System.err.println( "parent network: "+parent_bn.remoteToString() );
						x.parents[i] = p;	// p could be null here
						if ( p != null ) p.add_child( x );
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
						x.parents[i] = p;	// p could be null here
						if ( p != null ) p.add_child( x );
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
		if ( variables.size() == 0 )
			// Empty network; no references to locate.
			return;

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			// DOES THIS WORK ??? IT SHOULD SINCE WE ARE WORKING W/ LOCALS !!!
			Variable x = (Variable) enumv.nextElement();

			for ( int i = 0; i < x.parents_names.size(); i++ )
			{
				String parent_name = (String) x.parents_names.elementAt(i);

				int period_index;
				if ( (period_index = parent_name.lastIndexOf(".")) != -1 )
				{
					String bn_name = parent_name.substring(0,period_index);

					if ( belief_network_context.reference_table.get(bn_name) == null )
					{
						// We need to obtain a reference to the parent's b.n.,
						// which is either remote or on the local disk.
						// If remote, its name has the form "host/bn.x".
						// Otherwise, it must be on the local disk.

						int slash_index;
						if ( (slash_index = parent_name.lastIndexOf("/")) != -1 )
						{
							// Remote network, try to look it up; if found, add it to the
							// list of belief network references known in the current context.
							belief_network_context.add_lookup_reference( bn_name );
						}
						else
						{
							// Try to load from local disk.
							try { belief_network_context.load_network(bn_name); }
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

	protected Variable to_Variable( AbstractVariable x, String msg_leader ) throws RemoteException
	{
		if ( x instanceof Variable )
			return (Variable) x;

		// If x is a variable in this belief network, return a local reference.
		AbstractVariable xref = name_lookup( x.get_name() );
		if ( xref != null )
			return (Variable) xref;
		else
			throw new RemoteException( msg_leader+": "+x.get_name()+" is a remote variable; can't convert to local variable." );
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
