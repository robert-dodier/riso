package risotto.belief_nets;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import risotto.distributions.*;
import SmarterTokenizer;

public class Variable extends UnicastRemoteObject implements AbstractVariable
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
		int i, new_index = childrens_names.size();
		childrens_names.addElement( child_name );

		AbstractVariable[] old_children = children;
		children = new AbstractVariable[ childrens_names.size() ];
		for ( i = 0; i < new_index; i++ )
			children[i] = old_children[i];
		children[ new_index ] = x;

		Distribution[] old_lambdas = lambda_messages;
		lambda_messages = new Distribution[ childrens_names.size() ];
		for ( i = 0; i < new_index; i++ )
			lambda_messages[i] = old_lambdas[i];
		lambda_messages[ new_index ] = null;
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
					{
						if ( st.ttype == StreamTokenizer.TT_WORD )
						{
							// Set value=null since we don't yet have a reference for the parent;
							// we'll find the reference later and fix up this table entry.
System.err.println( "Variable.pretty_input: name: "+name+" add parent name: "+st.sval );
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
		Enumeration enump = parents_names.elements();
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
		Enumeration enumc = childrens_names.elements();
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
			result += distribution.format_string( more_leading_ws );
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
