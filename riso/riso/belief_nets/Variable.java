package belief_nets;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import distributions.*;

public class Variable extends UnicastRemoteObject implements AbstractVariable
{
	/** Flag to indicate no type has been assigned to this variable.
	  */
	public static final int VT_NONE = 0;

	/** Flag to indicate this variable is a discrete variable.
	  */
	public static final int VT_DISCRETE = 1;

	/** Flag to indicate this variable is a continuous variable.
	  */
	public static final int VT_CONTINUOUS = 2;

	/** List of names of parent variables. Parents can be within the
	  * belief network to which this variable belongs, or they can belong
	  * to other networks. This list should only be used if there is reason
	  * to think the list of parent variable references is out of date --
	  * for example, after editing the belief network.
	  */
	String[] parents_names;

	/** Name of this variable. The name can contain any word characters
	  * recognized by <tt>SmarterTokenzer</tt>; these include at least
	  * alphabetic, numeric, hyphen, and underscore characters, and maybe
	  * others.
	  * @see SmarterTokenizer
	  */
	String name;

	/** Tells whether this variable is discrete or continuous.
	  */
	int type;

	/** List of references to the parent variables of this variable.
	  * Parents variables can be in the belief network to which this
	  * variable belongs, or another local network, or a remote network.
	  * There is a one-to-one correspondence between this list and
	  * the list of parent names; if a named parent can't be located,
	  * the corresponding reference is <tt>null</tt>.
	  */
	AbstractVariable[] parents;
	
	/** List of references to child variables of this variable.
	  * As with the parents, the children can be in the belief network
	  * to which this variable belongs, or another local network, or a
	  * remote network. Constructing this list is a little tricky, since
	  * children can be in other belief networks; HOW IS THIS LIST
	  * CONSTRUCTED ???
	  */
	AbstractVariable[] children;

	/** The conditional distribution of this variable given its parents.
	  */
	ConditionalDistribution distribution;

	/** The marginal distribution of this variable given any evidence
	  * in the belief network. This distribution may often be null, meaning
	  * that it needs to be recomputed.
	  */
	Distribution posterior;

	/** Construct an empty variable. 
	  */
	public Variable() throws RemoteException
	{
		name = null;
		type = VT_NONE;
		parents = children = null;
		distribution = null;
	}

	/** Retrieve the name of this variable.
	  */
	public String get_name() throws RemoteException
	{
		return name;
	}

	/** Retrieve the list of references to parents of this variable.
	  * If any parent named in the list of parents' names can't be
	  * located, the corresponding element in this list is <tt>null</tt>.
	  */
	public AbstractVariable[] get_parents() throws RemoteException
	{
		return parents;
	}

	/** Retrieve the list of references to known children of this variable.
	  * Some children may be unknown due to remote network errors.
	  * (A remote variable may name this variable as a parent, but we might
	  * not have a reference to that variable due to remote exceptions.)
	  */
	public AbstractVariable[] get_children() throws RemoteException
	{
		return children;
	}

	/** Parse an input stream (represented as a tokenizer) for fields
	  * of this variable. THIS FUNCTION IS A HACK -- NEEDS WORK !!!
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
	{
		st.nextToken();
		name = st.sval;

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "Variable.pretty_input: missing left curly brace." );

		for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF && st.ttype != '}'; st.nextToken() )
		{
			// FILL THIS IN !!!
		}
	}

	/** Output this variable to a stream in the same format as that
	  * input by <tt>pretty_input</tt>, except that... COMPLETE !!!
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException, RemoteException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.println( leading_ws+this.getClass().getName()+" "+name+"{ }" );
	}
}
