package riso.belief_nets;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import riso.distributions.*;
import riso.remote_data.*;
import SmarterTokenizer;

/** General policy enforced here: allow changes to member data only if
  * the variable is local and not remote. <barf> Otherwise a whole set of
  * "get/set" methods is required </barf>.
  */
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
	public AbstractVariable[] get_variables() throws RemoteException
	{
		AbstractVariable[] u = new AbstractVariable[ variables.size() ];
		Enumeration e = variables.elements();
		for ( int i = 0; e.hasMoreElements(); i++ )
			u[i] = (AbstractVariable) e.nextElement();

		return u;
	}

	/** Clear the posterior of <tt>some_variable</tt> but do not recompute it. This method also
	  * clears the pi and lambda for this variable. Notify remote observers
	  * that the posterior for this variable is no longer known (if it ever was).
	  * If this variable was evidence, notify parents and children that pi and
	  * lambda messages originating from this variable are now invalid.
	  */
	public void clear_posterior( AbstractVariable some_variable ) throws RemoteException
	{
		Variable x = to_Variable( some_variable, "BeliefNetwork.clear_posterior" );

		Distribution p = x.posterior; // hold on to a reference for a moment

		x.pi = null;
		x.lambda = null;
		x.posterior = null;

		// Notify observers that the posterior has been cleared.
		set_changed( x );
		notify_observers( x, x.posterior );

		if ( p instanceof Delta ) // then this variable was evidence
		{
System.err.println( "BeliefNetwork.clear_posterior: tell parents of "+x.get_name() );
			x.notify_all_invalid_lambda_message();

System.err.println( "BeliefNetwork.clear_posterior: tell children of "+x.get_name() );
			x.notify_all_invalid_pi_message();
		}
	}

	public void assign_evidence( AbstractVariable some_variable, double value ) throws RemoteException
	{
		Variable x = to_Variable( some_variable, "BeliefNetwork.assign_evidence" );

		// If this variable is evidence, and the evidence is the same, then do nothing.

		if ( x.posterior instanceof Delta )
		{
			double[] support_point = ((Delta)x.posterior).get_support();
			if ( support_point.length == 1 && support_point[0] == value )
				return;
		}

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
				throw new RemoteException( "BeliefNetwork.assign_evidence: don't know about "+x.distribution.getClass() );	
		}
		else if ( x.type == Variable.VT_CONTINUOUS )
		{
			double[] support_point = new double[1];
			support_point[0] = value;
			delta = new GaussianDelta( support_point ); 
		}
		else
			throw new RemoteException( "BeliefNetwork.assign_evidence: don't know how to assign to "+x.get_fullname() );

		x.posterior = delta;
		x.pi = delta;
		x.lambda = delta;

		int i;

System.err.println( "BeliefNetwork.assign_evidence: tell parents of "+x.get_name() );
		x.notify_all_invalid_lambda_message();

System.err.println( "BeliefNetwork.assign_evidence: tell children of "+x.get_name() );
		x.notify_all_invalid_pi_message();

		// Notify observers that the posterior has been set.
		set_changed( x );
		notify_observers( x, x.posterior );
	}

	public void get_all_lambda_messages( Variable x ) throws Exception
	{
		int i = 0;
		while ( true )
		{
			AbstractVariable child = null;

			try 
			{
				for ( ; i < x.children.length; i++ )
				{
					if ( x.lambda_messages[i] == null )
					{
						child = x.children[i];
						x.lambda_messages[i] = child.get_bn().compute_lambda_message( x, child );
					}
				}

				return;
			}
			catch (RemoteException e)
			{
				// MAYBE I NEED TO BE ABLE TO DISTINGUISH FAILED CONNECTIONS FROM OTHER EXCEPTIONS ???
				x.remove_child( child );
			}
		}
	}

	public void get_all_pi_messages( Variable x ) throws Exception
	{
		for ( int i = 0; i < x.parents.length; i++ )
			if ( x.pi_messages[i] == null )
			{
				AbstractVariable parent = x.parents[i];
				x.pi_messages[i] = parent.get_bn().compute_pi_message( parent, x );
			}
	}

	/** This method DOES NOT put the newly computed lambda message into the
	  * list of lambda messages for the <tt>parent</tt> variable.
	  */
	public Distribution compute_lambda_message( AbstractVariable parent, AbstractVariable child_in ) throws RemoteException
	{
		Variable child = to_Variable( child_in, "BeliefNetwork.compute_lambda_message" );

		// To compute a lambda message for a parent, we need to incorporate
		// lambda messages coming in from the children of the child, as well
		// as pi messages coming into the child from other parents.

		// HOWEVER, we can make a slight optimization -- recall that if all
		// incoming lambda messages are noninformative, then the outgoing
		// lambda message is also noninformative, and we can ignore any
		// incoming pi messages.
		
		try { if ( child.lambda == null ) compute_lambda( child ); }
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "compute_lambda_message: from: "+child.get_fullname()+" to: "+parent.get_fullname()+": "+e );
		}

		Distribution[] remaining_pi_messages = new Distribution[ child.parents.length ];

		if ( !(child.lambda instanceof Noninformative) )
		{
			// Lambda messages are informative -- need to take pi 
			// messages into account.

			for ( int i = 0; i < child.parents.length; i++ )
				if ( parent.equals( child.parents[i] ) )
					remaining_pi_messages[i] = null;
				else
				{
					if ( child.pi_messages[i] == null )
					{
						AbstractVariable a_parent = child.parents[i];
						child.pi_messages[i] = a_parent.get_bn().compute_pi_message( a_parent, child_in );
					}
					remaining_pi_messages[i] = child.pi_messages[i];
				}
		}

		// This call works fine if child.lambda is noninformative -- the
		// remaining_pi_messages array is full of nulls, but they're ignored.

		LambdaMessageHelper lmh = null;
		
		try { lmh = LambdaMessageHelperLoader.load_lambda_message_helper( child.distribution, child.lambda, remaining_pi_messages ); }
		catch (Exception e) { e.printStackTrace(); }

		if ( lmh == null )
			throw new RemoteException( "compute_lambda_message: attempt to load lambda helper class failed;\n\tparent: "+parent.get_name()+" child: "+child.get_name() );

		Distribution lambda_message;
		
		try { lambda_message = lmh.compute_lambda_message( child.distribution, child.lambda, remaining_pi_messages ); }
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "compute_lambda_message: from: "+child.get_fullname()+" to: "+parent.get_fullname()+": "+e );
		}

System.err.println( "compute_lambda_message: from: "+child.get_name()+" to: "+parent.get_name()+" type: "+lambda_message.getClass()+" helper: "+lmh.getClass() );
		return lambda_message;
	}

	/** This method DOES NOT put the newly computed pi message into the
	  * list of pi messages for the <tt>child</tt> variable.
	  */
	public Distribution compute_pi_message( AbstractVariable parent_in, AbstractVariable child ) throws RemoteException
	{
		Variable parent = to_Variable( parent_in, "BeliefNetwork.compute_pi_message" );

		// To compute a pi message for the child, we need to incorporate
		// lambda messages from all children except for the one to which
		// we are sending the pi message.

		// HOWEVER, if this variable is evidence, then the pi message is
		// always a spike -- don't bother with incoming lambda messages.

		if ( parent.posterior instanceof Delta ) return parent.posterior;

		Distribution[] remaining_lambda_messages = new Distribution[ parent.children.length ];

		int i = 0;
		while ( true )
		{
			AbstractVariable a_child = null;

			try
			{
				for ( ; i < parent.children.length; i++ )
				{
					if ( child.equals( parent.children[i] ) )
						remaining_lambda_messages[i] = null;
					else
					{
						if ( parent.lambda_messages[i] == null )
						{
							a_child = parent.children[i];
							parent.lambda_messages[i] = a_child.get_bn().compute_lambda_message( parent_in, a_child );
						}
						remaining_lambda_messages[i] = parent.lambda_messages[i];
					}
				}

				break;
			}
			catch (RemoteException e)
			{
				// MAYBE I NEED TO BE ABLE TO DISTINGUISH FAILED CONNECTIONS FROM OTHER EXCEPTIONS ???
				parent.remove_child( a_child );
			}
		}

		try { if ( parent.pi == null ) compute_pi( parent ); }
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "compute_pi_message: from: "+parent.get_fullname()+" to: "+child.get_fullname()+": "+e );
		}

		PiMessageHelper pmh = null;
		
		try { pmh = PiMessageHelperLoader.load_pi_message_helper( parent.pi, remaining_lambda_messages ); }
		catch (Exception e) { e.printStackTrace(); }

		if ( pmh == null ) 
			throw new RemoteException( "compute_pi_message: attempt to load pi helper class failed; parent: "+parent.get_name()+" child: "+child.get_name() );

		Distribution pi_message;
		
		try { pi_message = pmh.compute_pi_message( parent.pi, remaining_lambda_messages ); }
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "compute_pi_message: from: "+parent.get_fullname()+" to: "+child.get_fullname()+": "+e );
		}

System.err.println( "compute_pi_message: from: "+parent.get_name()+" to: "+child.get_name()+" type: "+pi_message.getClass()+" helper: "+pmh.getClass() );
		return pi_message;
	}

	/** This method DOES set lambda for the variable <tt>x</tt>.
	  */
	public Distribution compute_lambda( Variable x ) throws Exception
	{
		// Special case: if node x is an uninstantiated leaf, its lambda
		// is noninformative.
		if ( x.children.length == 0 )
		{
			x.lambda = new Noninformative();
			return x.lambda;
		}

		// General case: collect lambda-messages from children, 
		// load lambda helper, and compute lambda.

		get_all_lambda_messages( x );

		LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( x.lambda_messages );
		if ( lh == null )
			throw new Exception( "compute_lambda: attempt to load lambda helper class failed; x: "+x.get_fullname() );

		x.lambda = lh.compute_lambda( x.lambda_messages );

System.err.println( "compute_lambda: "+x.get_name()+" type: "+x.lambda.getClass()+" helper: "+lh.getClass() );
		return x.lambda;
	}

	/** This method DOES set pi for the variable <tt>x</tt>.
	  */
	public Distribution compute_pi( Variable x ) throws Exception
	{
		// General case: x is not a root node; collect pi-messages from parents,
		// then use x's distribution and those pi-messages to compute pi.
		// This also works when x is a root node -- in that case pi is just
		// the marginal distribution of x.

		get_all_pi_messages( x );

		PiHelper ph = PiHelperLoader.load_pi_helper( x.distribution, x.pi_messages );
		if ( ph == null ) 
			throw new Exception( "compute_pi: attempt to load pi helper class failed; x: "+x.get_fullname() );

		x.pi = ph.compute_pi( x.distribution, x.pi_messages );

System.err.println( "compute_pi: "+x.get_name()+" type: "+x.pi.getClass()+" helper: "+ph.getClass() );
		return x.pi;
	}

	public Distribution compute_posterior( Variable x ) throws Exception
	{
		// To compute the posterior for this variable, we need to compute
		// pi and lambda first. For pi, we need a pi-message from each parent
		// and the conditional distribution for this variable; for lambda,
		// we need a lambda-message from each child. Then the posterior is
		// just the product of pi and lambda, but it needs to be normalized.

		if ( x.pi == null ) compute_pi( x );
		if ( x.lambda == null ) compute_lambda( x );

		PosteriorHelper ph = PosteriorHelperLoader.load_posterior_helper( x.pi, x.lambda );
		if ( ph == null )
			throw new Exception( "compute_posterior: attempt to load posterior helper class failed; x: "+x.get_fullname() );

		x.posterior = ph.compute_posterior( x.pi, x.lambda );

		// Now notify remote observers that we have computed a new posterior.
		// DO WE ALWAYS WANT THE NEXT TWO FUNCTION CALLS TOGETHER???

		set_changed( x );
		notify_observers( x, x.posterior );

System.err.println( "compute_posterior: "+x.get_name()+" type: "+x.posterior.getClass()+" helper: "+ph.getClass() );
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

		try
		{
			if ( x.posterior == null )
				compute_posterior(x);
			return x.posterior;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "get_posterior: "+x.get_fullname()+": "+e );
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
					Class variable_class = java.rmi.server.RMIClassLoader.loadClass( variable_type );
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
		// First find the list of all belief networks upstream of this one.

		Vector bn_list = new Vector();
		upstream_recursion( this, bn_list );

		// Now print out a description of each belief network.

		String result = "";
		result += "digraph \""+get_fullname()+"\" {\n";

		for ( Enumeration enum = bn_list.elements(); enum.hasMoreElements(); )
			result += one_dot_format( (AbstractBeliefNetwork)enum.nextElement() );

		result += "}\n";
		return result;
	}

	static void upstream_recursion( AbstractBeliefNetwork bn, Vector bn_list ) throws RemoteException
	{
		if ( ! bn_list.contains( bn ) )
		{
			bn_list.addElement( bn );

			AbstractVariable[] variables = bn.get_variables();
			for ( int i = 0; i < variables.length; i++ )
			{
				AbstractVariable[] parents = variables[i].get_parents();
				for ( int j = 0; j < parents.length; j++ )
					upstream_recursion( parents[j].get_bn(), bn_list );
			}
		}
	}

	static String one_dot_format( AbstractBeliefNetwork bn ) throws RemoteException
	{
		int i, j;
		String result = "";
		AbstractVariable[] variables = bn.get_variables();

		Vector invisibly_linked = new Vector();

		String bn_ihigh = "\"invis-"+bn.get_fullname()+"-high\"";
		String bn_ilow = "\"invis-"+bn.get_fullname()+"-low\"";

		result += "  "+bn_ihigh+" [style=invis, width=\"0.1\", height=\"0.1\", label=\"\"];\n";
		result += "  "+bn_ilow+" [style=invis, width=\"0.1\", height=\"0.1\", label=\"\"];\n";

		// Print the name of each variable with a shorter label.
		// Put in link to each variable from each of its parents.
		// For each variable without parents in bn, put in an invisible link from "invis-*-high";
		// for each variable without chilren in bn, put in an invisible link to "invis-*-low".
		// Note each belief network which contains a parent of some variable in bn.

		for ( i = 0; i < variables.length; i++ )
		{
			AbstractVariable x = variables[i];
			result += "  \""+x.get_fullname()+"\" [ label=\""+x.get_name()+"\"";
			if ( x.get_posterior() instanceof Delta )
				// This node is an evidence node, so color it differently.
				result += ", color=gray92, style=filled";
			result += " ];\n";

			AbstractVariable[] parents = x.get_parents(), children = x.get_children();
				
			boolean local_root = true, local_leaf = true;

			for ( j = 0; j < parents.length; j++ )
			{
				result += "  \""+parents[j].get_fullname()+"\"->\""+x.get_fullname()+"\";\n";

				AbstractBeliefNetwork parent_bn = parents[j].get_bn();
				if ( ! parent_bn.equals(bn) && ! invisibly_linked.contains( parent_bn ) )
					invisibly_linked.addElement( parent_bn );

				if ( parent_bn.equals(bn) )
					local_root = false;
			}

			j = 0;
			while ( true )
			{
				AbstractVariable child = null;

				try
				{
					for ( ; j < children.length; j++ )
					{
						child = children[j];
						if ( child.get_bn().equals(bn) )
							local_leaf = false;
					}

					break;
				}
				catch (RemoteException e)
				{
					// MAYBE I NEED TO BE ABLE TO DISTINGUISH FAILED CONNECTIONS FROM OTHER EXCEPTIONS ???
					// x.remove_child( child );	// NEEDS LOCAL REFERENCE FOR THIS !!!
					System.err.println( "BeliefNetwork.one_dot_format: leaf-finding failed; stumble forward: "+e );
				}
			}

			if ( local_root )
				result += "  "+bn_ihigh+" -> \""+x.get_fullname()+"\" [style=invis];\n";

			if ( local_leaf )
				result += "  \""+x.get_fullname()+"\" -> "+bn_ilow+" [style=invis];\n";
		}

		for ( i = 0; i < invisibly_linked.size(); i++ )
		{
			AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) invisibly_linked.elementAt(i);
			String parent_ilow = "\"invis-"+parent_bn.get_fullname()+"-low\"";
			result += "  "+parent_ilow+" -> "+bn_ihigh+" [style=invis];\n";
		}

		// The variables in bn all go into a cluster labeled with bn's name.

		result += "  subgraph \"cluster_"+bn.get_fullname()+"\" {\n";
		result += "    label = \""+bn.get_name()+"\";\n";
		result += "    color = gray;\n";

		for ( i = 0; i < variables.length; i++ )
		{
			AbstractVariable x = variables[i];
			result += "    \""+x.get_fullname()+"\";\n";
		}

		result += "  }\n";
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
				NameInfo ni = NameInfo.parse_variable( parent_name, belief_network_context );

				if ( ni.beliefnetwork_name != null )
				{
					// Parent is in some other belief network -- first get a reference
					// to the other belief network, then get a reference to the parent
					// variable within the other network.

					try 
					{
						AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) belief_network_context.get_reference(ni);
System.err.println( "BeliefNetwork.assign_references: parent_name: "+parent_name+"; parent_bn is "+(parent_bn==null?"null":"NOT null") );
						AbstractVariable p = parent_bn.name_lookup( ni.variable_name );
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
				NameInfo ni = NameInfo.parse_variable( parent_name, belief_network_context );

				if ( ni.beliefnetwork_name != null )
				{
					Remote bn = null;
					try { bn = belief_network_context.get_reference(ni); }
					catch (RemoteException e)
					{
						System.err.println( "BeliefNetwork.locate_references: get_reference failed; stagger forward. Exception: "+e );
					}

					if ( bn == null )
					{
						// The parent bn is not running yet; try to load it from the
						// local disk. If that fails, we're sunk.

						try { ni.resolve_host(); }
						catch (Exception e)
						{
e.printStackTrace();
							throw new UnknownNetworkException( "BeliefNetwork.locate_references: attempt to resolve "+ni.host_name+" failed." );
						}

						InetAddress localhost;
						try { localhost = InetAddress.getLocalHost(); }
						catch (java.net.UnknownHostException e)
						{
							throw new RuntimeException( "BeliefNetwork.locate_references: attempt to obtain localhost failed." );
						}

						if ( ni.host.equals(localhost) )
						{
							try
							{
								bn = belief_network_context.load_network(ni.beliefnetwork_name);
System.err.println( "BeliefNetwork.locate_references: bind belief net: "+((AbstractBeliefNetwork)bn).get_fullname() );
								belief_network_context.bind( (AbstractBeliefNetwork) bn );
							}
							catch (IOException e)
							{
								throw new UnknownNetworkException( "BeliefNetwork.locate_references: attempt to load network failed: "+e );
							}
						}
						else
						{
							// Maybe we could now try to contact a bn context on the !!!
							// host and ask it to load the bn -- future development ???
							throw new UnknownNetworkException( "BeliefNetwork.locate_references: attempt to locate remote parent failed: "+parent_name );
						}
					}
				}
			}
		}
System.err.println( "BeliefNetwork.locate_references: reference table: " );
System.err.println( "  "+belief_network_context.reference_table );
	}

	/** In order to work with instance data, we need to have a class
	  * reference, not an interface reference. This is much simpler than
	  * writing get/set methods for every datum, although it does limit
	  * computations to local variables.
	  */
	protected Variable to_Variable( AbstractVariable x, String msg_leader ) throws RemoteException
	{
		try { if ( x instanceof Variable ) return (Variable) x; }
		catch (ClassCastException e)
		{
			e.printStackTrace();
			throw new RemoteException( "to_Variable: BIZARRE: "+e );
		}

		// If x is a variable in this belief network, return a local reference.
		try
		{
			AbstractVariable xref = name_lookup( x.get_name() );
			if ( xref != null )
				return (Variable) xref;
			else
				throw new RemoteException( msg_leader+": "+x.get_name()+" has a null reference in "+get_fullname()+"; can't convert to local variable." );
		}
		catch (Exception e)
		{
			throw new RemoteException( msg_leader+": "+x.get_name()+" isn't on the list of names in "+get_fullname()+"; can't convert to local variable." );
		}
	}
}
