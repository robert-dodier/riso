package riso.belief_nets;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import riso.distributions.*;
import SmarterTokenizer;

public class Variable extends UnicastRemoteObject implements AbstractVariable, Serializable
{
	/** Most recently computed pi for this variable. This is
	  * defined as <tt>p(this variable|evidence above)</tt>.
	  */
	protected Distribution pi = null;

	/** Most recently computed lambda for this variable. This is
	  * defined as <tt>p(evidence below|this variable)</tt>.
	  */
	protected Distribution lambda = null;

	/** List of the pi-messages coming in to this variable from its parents.
	  * This list parallels the list of parents.
	  */
	protected Distribution[] pi_messages = new Distribution[0];

	/** List of the lambda-messages coming in to this variable from its 
	  * children. This list parallels the list of children.
	  */
	protected Distribution[] lambda_messages = new Distribution[0];

	/** Reference to the belief network which contains this variable.
	  * It's occaisonally useful to get a reference to the belief network
	  * given a reference to a variable within that network. 
	  */
	BeliefNetwork belief_network = null;

	/** Flag to indicate no type has been assigned to this variable.
	  */
	public static final int VT_NONE = 0;

	/** Flag to indicate this variable is a discrete variable.
	  */
	public static final int VT_DISCRETE = 1;

	/** Flag to indicate this variable is a continuous variable.
	  */
	public static final int VT_CONTINUOUS = 2;

	/** Name of this variable. The name can contain any word characters
	  * recognized by <tt>SmarterTokenizer</tt>; these include at least
	  * alphabetic, numeric, hyphen, and underscore characters, and maybe
	  * others.
	  * @see SmarterTokenizer
	  */
	String name = null;

	/** Tells whether this variable is discrete or continuous.
	  */
	int type = VT_CONTINUOUS;

	/** List of the names of the states of this variable, if discrete.
	  */
	Vector states_names = new Vector();

	/** List of the names of the parents of this variable.
	  * The parent's name includes the name of the belief network to
	  * which it belongs, if the parent belongs to a different belief
	  * network than the child.
	  */
	Vector parents_names = new Vector();

	/** List of the names of the children of this variable.
	  * The child's name includes the name of the belief network to
	  * which it belongs, if the child belongs to a different belief
	  * network than the parent.
	  */
	Vector childrens_names = new Vector();

	/** List of references to parent variables. Parents can be within the
	  * belief network to which this variable belongs, or they can belong
	  * to other networks. This list is parallel to the list of parents' names.
	  * If the parent cannot be located, the reference is <tt>null</tt>.
	  */
	AbstractVariable[] parents = new AbstractVariable[0];

	/** List of child variables of this variable. This list is parallel
	  * to the list of childrens' names. Constructing this list is a
	  * little tricky, since children can be in other belief networks;
	  * HOW IS THIS LIST CONSTRUCTED ???
	  */
	AbstractVariable[] children = new AbstractVariable[0];

	/** The conditional distribution of this variable given its parents.
	  */
	ConditionalDistribution distribution = null;

	/** The marginal distribution of this variable given any evidence
	  * in the belief network. This distribution may often be null, meaning
	  * that it needs to be recomputed.
	  */
	Distribution posterior = null;

	/** Construct an empty variable. 
	  */
	public Variable() throws RemoteException {}

	/** Retrieves a reference to the belief network which contains this
	  * variable.
	  */
	public AbstractBeliefNetwork get_bn() throws RemoteException
	{
		return belief_network;
	}

	/** Retrieve just the name of this variable alone; doesn't
	  * include the name of the belief network.
	  */
	public String get_name() throws RemoteException { return name; }

	/** Retrieve the name of this variable, including the name of the
	  * belief network which contains it.
	  */
	public String get_fullname() throws RemoteException
	{
		if ( belief_network == null )
			// Cover your ass with a big piece of plywood.
			return "(unknown network)."+name;
		else
			return belief_network.get_fullname()+"."+name;
	}

	/** Retrieve a list of the names of the parent variables of this variable.
	  */
	public String[] get_parents_names() throws RemoteException
	{
		String[] s = new String[ parents_names.size() ];
		for ( int i = 0; i < parents_names.size(); i++ )
			s[i] = (String) parents_names.elementAt(i);
		return s;
	}

	/** Retrieve the list of references to parents of this variable.
	  * If any parent named in the list of parents' names can't be
	  * located, the corresponding element in this list is <tt>null</tt>.
	  */
	public AbstractVariable[] get_parents() throws RemoteException
	{
		return parents;
	}

	/** Retrieve a list of the names of the child variables of this variable.
	  */
	public String[] get_childrens_names() throws RemoteException
	{
		String[] s = new String[ childrens_names.size() ];
		for ( int i = 0; i < childrens_names.size(); i++ )
			s[i] = (String) childrens_names.elementAt(i);
		return s;
	}

	/** Retrieve the list of references to known children of this variable.
	  * Some children may be unknown (i.e., null reference).
	  * (A remote variable may name this variable as a parent, but we might
	  * not have a reference to that variable due to remote exceptions.)
	  */
	public AbstractVariable[] get_children() throws RemoteException
	{
		return children;
	}

	/** Retrieve a reference to the conditional distribution of this variable given its parents.
	  * The reference is null if no distribution has yet been specified for this variable.
	  */
	public ConditionalDistribution get_distribution() throws RemoteException
	{
		return distribution;
	}

	/** Retrieve a reference to the posterior distribution of this variable
	  * given any evidence variables. The reference is null if the posterior
	  * has not been computed.
	  */
	public Distribution get_posterior() { return posterior; }

	/** Retrieve a reference to the predictive distribution of this variable
	  * given any evidence variables. The reference is null if the predictive 
	  * distribution has not been computed.
	  */
	public Distribution get_pi() { return pi; }

	/** Retrieve a reference to the likelihood function of this variable given 
	  * any evidence variables. The reference is null if the likelihood
	  * function has not been computed.
	  */
	public Distribution get_lambda() { return lambda; }

	/** Retrieve the list of predictive messages coming into this variable
	  * given any evidence variables. The list is an array with the number
	  * of elements equal to the number of parents; if some pi message has
	  * not been computed, the corresponding element is null. The list has
	  * zero length until the parent references have been set up, usually
	  * shortly after parsing the description for this variable.
	  */
	public Distribution[] get_pi_messages() { return pi_messages; }

	/** Retrieve the list of likelihood messages coming into this variable
	  * given any evidence variables. The list is an array with the number
	  * of elements equal to the number of children; if some lambda message has
	  * not been computed, the corresponding element is null. The list has
	  * zero length until the child references have been set up, usually
	  * shortly after parsing the description for this variable.
	  */
	public Distribution[] get_lambda_messages() { return lambda_messages; }

	/** Tells this variable to add another to its list of parents.
	  * @param parent_name Name of the parent of the new variable. This 
	  *   can be a name of the form <tt>some_bn.some_variable</tt>, where
	  *   <tt>some_bn</tt> is the name of another belief network; an attempt
	  *   will be made to locate the referred-to belief network.
	  * @throws RemoteException If the parent cannot be located.
	  */
	public void add_parent( String parent_name ) throws RemoteException
	{
System.err.println( "add_parent: add "+parent_name+" to "+this.name );
		int i, new_index = parents_names.size();
		parents_names.addElement( parent_name );

		AbstractVariable parent = null;
		int dot_index = parent_name.lastIndexOf(".");
		if ( dot_index != -1 )
		{
			String parent_bn_name = parent_name.substring( 0, dot_index );
			AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) belief_network.belief_network_context.reference_table.get( parent_bn_name );
			parent = parent_bn.name_lookup( parent_name.substring(dot_index) );
		}
		else
		{
			parent = belief_network.name_lookup( parent_name );
		}

		if ( parent == null )
			throw new RemoteException( "Variable.add_parent: can't locate "+parent_name );

		AbstractVariable[] old_parents = parents;
		parents = new AbstractVariable[ parents_names.size() ];
		for ( i = 0; i < new_index; i++ )
			parents[i] = old_parents[i];
		parents[new_index] = parent;

		pi_messages = new Distribution[ parents.length ];
		pi = null;
		posterior = null;
		// SHOULD I CLEAR lambda AND lambda_messages HERE ???

		// Notify observers that the posterior has been cleared.
		belief_network.set_changed( this );
		belief_network.notify_observers( this, this.posterior );

		parent.add_child( this );
	}

	/** Tells this variable to remove a child variable from its list 
	  * of children. This often happens because the child is in a remote belief
	  * network that is no longer reachable; or it could happen due to
	  * editing the belief network to which this variable belongs.
	  */
	public void remove_child( AbstractVariable x )
	{
		int i, j, child_index = -1;
		for ( i = 0; i < children.length; i++ )
			if ( children[i] == x )
			{
				child_index = i;
				break;
			}

		if ( child_index == -1 ) return;	// SOMETHING MORE INFORMATIVE HERE ???

System.err.println( "Variable.remove_child: "+childrens_names.elementAt(child_index)+" from children of "+this.name );
		childrens_names.removeElementAt( child_index );
		AbstractVariable[] old_children = children;
		children = new AbstractVariable[ old_children.length-1 ];
		for ( i = 0, j = 0; i < old_children.length; i++ )
		{
			if ( i == child_index ) continue;
			children[j++] = old_children[i];
		}

		Distribution lm = lambda_messages[ child_index ];
		boolean informative_child = (lm != null && !(lm instanceof Noninformative));

		if ( informative_child )
		{
System.err.println( "\tchild is informative; clear lambda and posterior." );
			// Now that the child has gone away, clear its lambda message;
			// that changes the lambda and posterior of this variable.

			Distribution[] old_lambda_messages = lambda_messages;
			lambda_messages = new Distribution[ old_lambda_messages.length-1 ];
			for ( i = 0, j = 0; i < old_lambda_messages.length; i++ )
			{
				if ( i == child_index ) continue;
				lambda_messages[j++] = old_lambda_messages[i];
			}

			lambda = null;
			posterior = null;
			// SHOULD I CLEAR pi AND pi_messages HERE ???

			// Notify observers that the posterior has been cleared.
			belief_network.set_changed( this );
			belief_network.notify_observers( this, this.posterior );
		}
		else
		{
System.err.println( "\tchild is not informative." );
			// The child to be removed didn't contribute any info, so don't
			// disturb lambda messages originating from the remaining children.

			Distribution[] old_lambda_messages = lambda_messages;
			lambda_messages = new Distribution[ old_lambda_messages.length-1 ];
			for ( i = 0, j = 0; i < old_lambda_messages.length; i++ )
			{
				if ( i == child_index ) continue;
				lambda_messages[j++] = old_lambda_messages[i];
			}
		}
	}

	/** Tells this variable to add another to its list of children.
	  */
	public void add_child( AbstractVariable x ) throws RemoteException
	{
		String child_name = x.get_fullname();
		int i, new_index = childrens_names.size();
		childrens_names.addElement( child_name );

		AbstractVariable[] old_children = children;
		children = new AbstractVariable[ childrens_names.size() ];
		for ( i = 0; i < new_index; i++ )
			children[i] = old_children[i];
		children[ new_index ] = x;

		lambda_messages = new Distribution[ children.length ];
		lambda = null;
		posterior = null;
		// SHOULD I CLEAR pi AND pi_messages HERE ???

		// Notify observers that the posterior has been cleared.
		belief_network.set_changed( this );
		belief_network.notify_observers( this, this.posterior );
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  * This method constructs a tokenizer for the string (via <tt>StringReader</tt>)
	  * and then calls <tt>pretty_input</tt>, which see.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Parse an input stream (represented as a tokenizer) for fields
	  * of this variable. THIS FUNCTION IS A HACK -- NEEDS WORK !!!
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		st.nextToken();
		name = st.sval;

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "Variable.pretty_input: missing left curly brace; parser state: "+st );

		for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF && st.ttype != '}'; st.nextToken() )
		{
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				if ( "type".equals(st.sval) )
				{
					st.nextToken();
					if ( "continuous".equals(st.sval) )
						type = VT_CONTINUOUS;
					else if ( "discrete".equals(st.sval) )
					{
						type = VT_DISCRETE;
						st.nextToken();
						if ( st.ttype == '{' )
						{
							// Parse list of states' names.
							for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
								states_names.addElement( st.sval );
						}
						else
							st.pushBack();
					}
					else
						throw new IOException( "Variable.pretty_input: unknown variable type: "+st.sval );
				}
				else if ( "parents".equals(st.sval) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
					{
						st.pushBack();
						continue;
					}

					for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF && st.ttype != '}'; st.nextToken() )
					{
						if ( st.ttype == StreamTokenizer.TT_WORD )
						{
							// Set value=null since we don't yet have a reference for the parent;
							// we'll find the reference later and fix up this table entry.
							parents_names.addElement( st.sval );
						}
						else
							throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token in parent list; parser state: "+st );
					}
				}
				else if ( "distribution".equals(st.sval) )
				{
					// The next token must be the name of a class.
					try
					{
						st.nextToken();
						Class component_class = java.rmi.server.RMIClassLoader.loadClass( st.sval );
						distribution = (ConditionalDistribution) component_class.newInstance();
						distribution.set_variable( this );	// for benefit of certain distribution classes; ignored by most kinds
					}
					catch (Exception e)
					{
						throw new IOException( "Variable.pretty_input: attempt to create distribution failed:\n"+e );
					}

					st.nextBlock();
					distribution.parse_string( st.sval );
				}
				else
					throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token; parser state: "+st );
			}
			else
				throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token; parser state: "+st );
		}
	}

	/** Write a description of this variable to an output stream.
	  * This is slightly asymmetric w.r.t. to <tt>pretty_input</tt>:
	  * this function writes the class name onto the output stream before
	  * writing the variable name and any descriptive data; whereas
	  * <tt>pretty_input</tt> expects that the class name has been stripped
	  * from the input stream and the variable name is the first token.
	  *
	  * <p> The format is that used by <tt>format_string</tt>, which see.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Create a description of this variable as a string. This is 
	  * useful for obtaining descriptions of remote variables.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		String result = "";
		result += leading_ws+this.getClass().getName()+" "+name+"\n"+leading_ws+"{"+"\n";

		String more_leading_ws = leading_ws+"\t";

		result += more_leading_ws+"type ";
		switch ( type )
		{
		case VT_CONTINUOUS:
			result += "continuous\n";
			break;
		case VT_DISCRETE:
			result += "discrete";
			if ( states_names != null && states_names.size() > 0 )
			{
				result += " { ";
				for ( Enumeration e = states_names.elements(); e.hasMoreElements(); )
					result += "\""+e.nextElement()+"\""+" ";
				result += "}";
			}
			result += "\n";
			break;
		case VT_NONE:
			result += "% no type specified\n";
		}

		Enumeration enump = parents_names.elements();
		if ( enump.hasMoreElements() )
		{
			result += more_leading_ws+"parents";
			result += " { ";
			while ( enump.hasMoreElements() )
				result += (String) enump.nextElement()+" ";
			result += "}\n";
		}

		Enumeration enumc = childrens_names.elements();
		if ( enumc.hasMoreElements() )
		{
			result += more_leading_ws+"% children";
			result += " { ";
			while ( enumc.hasMoreElements() )
				result += (String) enumc.nextElement()+" ";
			result += "}\n";
		}

		if ( distribution == null )
			result += more_leading_ws+"% no distribution specified"+"\n";
		else
		{
			result += more_leading_ws+"distribution"+" ";
			try { result += distribution.format_string( more_leading_ws ); }
			catch (IOException e) { throw new RemoteException( "Variable.format_string: failed: "+e ); }
		}

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Simplified output, especially suitable for debugging.
	  */
	public String toString()
	{
		return "["+this.getClass().getName()+" "+name+"]";
	}

	/** Tell if this variable is discrete or not. If it is not discrete,
	  * then it is continuous.
	  */
	public boolean is_discrete() throws RemoteException
	{
		if ( type == VT_NONE ) throw new RemoteException( "Variable.is_discrete: variable has no type." );
		return type == VT_DISCRETE;
	}

	/** Translates values named by strings into numeric values.
	  * This applies only to discrete variables.
	  *
	  * @return Index of <tt>string_value</tt> within the list of named values given in the
	  *   "type" definition in the belief network description file, or otherwise set up.
	  * @exception RemoteException If this variable is not discrete, or or if it is discrete
	  *   but the string value has not been established.
	  */
	public int numeric_value( String string_value ) throws RemoteException
	{
		if ( type != VT_DISCRETE )
			throw new RemoteException( "Variable.numeric_value: variable "+name+" is not discrete." );

		int i;

		if ( states_names == null || (i = states_names.indexOf( string_value )) == -1 )
			throw new RemoteException( "Variable.numeric_value: variable "+name+" doesn't have a state ``"+string_value+"''" );
		
		return i;
	}

	public void notify_all_invalid_lambda_message() 
	{
		try
		{
			for ( int i = 0; i < parents.length; i++ )
				parents[i].invalid_lambda_message_notification( this );
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			System.err.println( "Variable.notify_all_invalid_lambda_message: stagger forward." );
			// SOMETHING MORE INFORMATIVE HERE ???
		}
	}

	public void notify_all_invalid_pi_message() 
	{
		int i = 0;
		while ( true )
		{
			AbstractVariable child = null;

			try 
			{
				for ( ; i < children.length; i++ )
				{
					child = children[i];
					child.invalid_pi_message_notification( this );
				}

				return;
			}
			catch (RemoteException e)
			{
				remove_child( child );
			}
		}
	}

	/** This method is called by a child to notify this variable that the lambda-message
	  * from the child is no longer valid. This parent variable must clear its lambda
	  * function and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_lambda_message_notification( AbstractVariable child ) throws RemoteException
	{
		int i, child_index = -1;
		for ( i = 0; i < children.length; i++ )
			if ( child.equals( children[i] ) )
			{
				child_index = i;
				break;
			}

		if ( child_index == -1 ) throw new RemoteException( "Variable.invalid_lambda_message_notification: "+child.get_fullname()+" is apparently not a child of "+this.get_fullname() );

		if ( lambda_messages[ child_index ] == null )
		{	
			// Nothing to do -- we haven't received any information from the child.
			return;
		}

		lambda_messages[ child_index ] = null;
		if ( posterior instanceof Delta ) return; // nothing further to do

		lambda = null;
		posterior = null;

		// Notify observers that the posterior has been cleared.
		belief_network.set_changed( this );
		belief_network.notify_observers( this, this.posterior );

		for ( i = 0; i < parents.length; i++ )
			parents[i].invalid_lambda_message_notification( this );

		for ( i = 0; i < children.length; i++ )
			if ( i != child_index )
				children[i].invalid_pi_message_notification( this );
	}

	/** This method is called by a parent to notify this variable that the pi-message
	  * from the parent is no longer valid. This child variable must clear its pi
	  * distribution and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_pi_message_notification( AbstractVariable parent ) throws RemoteException
	{
		int i, parent_index = -1;
		for ( i = 0; i < parents.length; i++ )
			if ( parent.equals( parents[i] ) )
			{
				parent_index = i;
				break;
			}

		if ( parent_index == -1 ) throw new RemoteException( "Variable.invalid_pi_message_notification: "+parent.get_fullname()+" is apparently not a parent of "+this.get_fullname() );

		if ( pi_messages[ parent_index ] == null )
		{	
			// Nothing to do -- we haven't received any information from the parent.
			return;
		}

		if ( posterior instanceof Delta )
		{
			// The posterior, pi, and pi messages (except for the one we now
			// know is invalid) for this variable remain valid, but
			// outgoing lambda messages are now invalid. 

			pi_messages[ parent_index ] = null;
			for ( i = 0; i < parents.length; i++ )
				if ( i != parent_index )
					parents[i].invalid_lambda_message_notification( this );

			return;
		}

		pi = null;
		pi_messages[ parent_index ] = null;
		posterior = null;

		// Notify observers that the posterior has been cleared.
		belief_network.set_changed( this );
		belief_network.notify_observers( this, this.posterior );

		if ( lambda == null || !(lambda instanceof Noninformative) )
		{
			for ( i = 0; i < parents.length; i++ )
				if ( i != parent_index )
					parents[i].invalid_lambda_message_notification( this );
		}

		for ( i = 0; i < children.length; i++ )
			children[i].invalid_pi_message_notification( this );
	}
}
