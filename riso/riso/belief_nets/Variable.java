package risotto.belief_nets;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import risotto.distributions.*;
import SmarterTokenizer;

public class Variable extends UnicastRemoteObject implements AbstractVariable
{
	/** Most recently computed pi-message for this variable. This is
	  * defined as <tt>p(this variable|evidence above)</tt>.
	  */
	protected Distribution pi = null;

	/** Most recently computed lambda-message for this variable. This is
	  * defined as <tt>p(evidence below|this variable)</tt>.
	  */
	protected Distribution lambda = null;

	/** List of the pi-messages coming in to this variable from its parents.
	  * A reference to the parent is the key in this hash table.
	  */
	protected Hashtable pi_messages = new Hashtable();

	/** List of the lambda-messages coming in to this variable from its 
	  * children. A reference to the child is the key in this hash table.
	  */
	protected Hashtable lambda_messages = new Hashtable();

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
	Vector states_names = null;

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
System.err.println( "Variable.pretty_input: read "+states_names.size()+" state names: "+states_names );
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
			if ( states_names != null )
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

		result += more_leading_ws+"parents";
		Enumeration enump = parents.keys();
		if ( ! enump.hasMoreElements() )
			result += "\n";
		else
		{
			result += " { ";
			while ( enump.hasMoreElements() )
				result += (String) enump.nextElement()+" ";
			result += "}\n";
		}

		result += more_leading_ws+"% children";
		Enumeration enumc = children.keys();
		if ( ! enumc.hasMoreElements() )
			result += "\n";
		else
		{
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
			distribution.format_string( more_leading_ws );
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
}
