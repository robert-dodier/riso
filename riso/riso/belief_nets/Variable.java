package risotto.belief_nets;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import risotto.distributions.*;

public class Variable extends UnicastRemoteObject implements AbstractVariable
{
	/** Reference to the belief network which contains this variable.
	  * It's occaisonally useful to get a reference to the belief network
	  * given a reference to a variable within that network. The reference
	  * can be a remote reference.
	  */
	AbstractBeliefNetwork belief_network = null;

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
	int type = VT_NONE;

	/** List of the names of the states of this variable, if discrete.
	  */
	String[] states_names = null;

	/** List of parent variables. Parents can be within the
	  * belief network to which this variable belongs, or they can belong
	  * to other networks.
	  * In this table, the name of the parent (including the belief network
	  * name, if different from the name of the belief network to which
	  * this variable belongs) is the key, and a reference to the parent
	  * is the value. If a parent can't be located, the corresponding
	  * reference is <tt>null</tt>.
	  */
	Hashtable parents = new NullValueHashtable();

	/** List of child variables of this variable.
	  * As with the parents, the children can be in the belief network
	  * to which this variable belongs, or another local network, or a
	  * remote network. Constructing this list is a little tricky, since
	  * children can be in other belief networks; HOW IS THIS LIST
	  * CONSTRUCTED ???
	  */
	Hashtable children = new NullValueHashtable();

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
			return belief_network.get_name()+"."+name;
	}

	/** Retrieve a list of the names of the parent variables of this variable.
	  */
	public Enumeration get_parents_names() throws RemoteException
	{
		return parents.keys();
	}

	/** Retrieve the list of references to parents of this variable.
	  * If any parent named in the list of parents' names can't be
	  * located, the corresponding element in this list is <tt>null</tt>.
	  */
	public Enumeration get_parents() throws RemoteException
	{
		return parents.elements();
	}

	/** Retrieve a list of the names of the child variables of this variable.
	  */
	public Enumeration get_childrens_names() throws RemoteException
	{
		return children.keys();
	}

	/** Retrieve the list of references to known children of this variable.
	  * Some children may be unknown (i.e., null reference).
	  * (A remote variable may name this variable as a parent, but we might
	  * not have a reference to that variable due to remote exceptions.)
	  */
	public Enumeration get_children() throws RemoteException
	{
		return children.elements();
	}

	/** Retrieve a reference to the conditional distribution of this variable given its parents.
	  * The reference is null if no distribution has yet been specified for this variable.
	  */
	public ConditionalDistribution get_distribution() throws RemoteException
	{
		return distribution;
	}

	/** Retrieve a reference to the posterior distribution of this variable given 
	  * any evidence variables. The reference is null if the posterior has not been
	  * computed given the current evidence.
	  */
	public Distribution get_posterior() throws RemoteException
	{
		return posterior;
	}

	/** Tell this variable to add another to its list of children.
	  * Since the other variable may be remote, we need a method to
	  * do this, since we can't access the children list directly.
	  */
	public void add_child( String child_name, AbstractVariable x ) throws RemoteException
	{
		children.put(child_name,x);
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
							Vector names = new Vector();
							for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
								names.addElement( st.sval );
System.err.println( "Variable.pretty_input: read "+names.size()+" state names: "+names );
							states_names = new String[ names.size() ];
							names.copyInto( states_names );
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
						if ( st.ttype == StreamTokenizer.TT_WORD )
							// Set value=null since we don't yet have a reference for the parent;
							// we'll find the reference later and fix up this table entry.
							parents.put( st.sval, null );
						else
							throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token in parent list; parser state: "+st );
				}
				else if ( "distribution".equals(st.sval) )
				{
					// The next token must be the name of a class.
					try
					{
						st.nextToken();
						Class component_class = Class.forName( st.sval );
						distribution = (ConditionalDistribution) component_class.newInstance();
					}
					catch (Exception e)
					{
						throw new IOException( "Variable.pretty_input: attempt to create distribution failed:\n"+e );
					}

					distribution.pretty_input( st );
				}
				else
					throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token; parser state: "+st );
			}
			else
				throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token; parser state: "+st );
		}
	}

	/** Output this variable to a stream in the same format as that
	  * input by <tt>pretty_input</tt>, except that... COMPLETE !!!
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException, RemoteException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( leading_ws+this.getClass().getName()+" "+name+"\n"+leading_ws+"{"+"\n" );

		String more_leading_ws = leading_ws+"\t";

		dest.print( more_leading_ws+"type " );
		switch ( type )
		{
		case VT_CONTINUOUS:
			dest.print( "continuous\n" );
			break;
		case VT_DISCRETE:
			dest.print( "discrete" );
			if ( states_names != null )
			{
				dest.print( " { " );
				for ( int i = 0; i < states_names.length; i++ )
					dest.print( "\""+states_names[i]+"\""+" " );
				dest.print( "}" );
			}
			dest.print( "\n" );
			break;
		case VT_NONE:
			dest.print( "% no type specified\n" );
		}

		dest.print( more_leading_ws+"parents" );
		Enumeration enump = parents.keys();
		if ( ! enump.hasMoreElements() )
			dest.print( "\n" );
		else
		{
			dest.print( " { " );
			while ( enump.hasMoreElements() )
				dest.print( (String) enump.nextElement()+" " );
			dest.print( "}\n" );
		}

		dest.print( more_leading_ws+"% children" );
		Enumeration enumc = children.keys();
		if ( ! enumc.hasMoreElements() )
			dest.print( "\n" );
		else
		{
			dest.print( " { " );
			while ( enumc.hasMoreElements() )
				dest.print( (String) enumc.nextElement()+" " );
			dest.print( "}\n" );
		}

		if ( distribution == null )
			dest.print( more_leading_ws+"% no distribution specified"+"\n" );
		else
		{
			dest.print( more_leading_ws+"distribution"+" " );
			distribution.pretty_output( os, more_leading_ws );
		}

		dest.print( leading_ws+"}"+"\n" );
	}

	/** Simplified output, especially suitable for debugging.
	  */
	public String toString()
	{
		return "["+this.getClass().getName()+" "+name+"]";
	}
}
