package belief_nets;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
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

	/** Name of this variable. The name can contain any word characters
	  * recognized by <tt>SmarterTokenzer</tt>; these include at least
	  * alphabetic, numeric, hyphen, and underscore characters, and maybe
	  * others.
	  * @see SmarterTokenizer
	  */
	String name = null;

	/** Tells whether this variable is discrete or continuous.
	  */
	int type = VT_NONE;

	/** List of parent variables. Parents can be within the
	  * belief network to which this variable belongs, or they can belong
	  * to other networks. This list should only be used if there is reason
	  * to think the list of parent variable references is out of date --
	  * for example, after editing the belief network.
	  * In this table, the name of the parent (including the belief network
	  * name, if different from the name of the belief network to which
	  * this variable belongs) is the key, and a reference to the parent
	  * is the value. If a parent can't be located, the corresponding
	  * reference is <tt>null</tt>.
	  */
	Hashtable parents = new Hashtable();

	/** List of child variables of this variable.
	  * As with the parents, the children can be in the belief network
	  * to which this variable belongs, or another local network, or a
	  * remote network. Constructing this list is a little tricky, since
	  * children can be in other belief networks; HOW IS THIS LIST
	  * CONSTRUCTED ???
	  */
	Hashtable children = new Hashtable();

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
		Enumeration enum = parents.elements();
		AbstractVariable[] refs = new AbstractVariable[ parents.size() ];

		for ( int i = 0; enum.hasMoreElements(); )
			refs[i++] = (AbstractVariable) enum.nextElement();

		return refs;
	}

	/** Retrieve the list of references to known children of this variable.
	  * Some children may be unknown (i.e., null reference).
	  * (A remote variable may name this variable as a parent, but we might
	  * not have a reference to that variable due to remote exceptions.)
	  */
	public AbstractVariable[] get_children() throws RemoteException
	{
		Enumeration enum = children.elements();
		AbstractVariable[] refs = new AbstractVariable[ children.size() ];

		for ( int i = 0; enum.hasMoreElements(); )
			refs[i++] = (AbstractVariable) enum.nextElement();

		return refs;
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
