package risotto.belief_nets;

import java.io.*;
import java.rmi.*;
import risotto.distributions.*;
import SmarterTokenizer;

public interface AbstractVariable extends Remote
{
	/** Retrieve just the name of this variable alone; doesn't
	  * include the name of the belief network.
	  */
	public String get_name() throws RemoteException;

	/** Retrieve the name of this variable, including the name of the
	  * belief network which contains it.
	  */
	public String get_fullname() throws RemoteException;

	/** Retrieve a list of the names of the parent variables of this variable.
	  */
	public String[] get_parents_names() throws RemoteException;

	/** Retrieve a list of references to the parent variables of this variable.
	  */
	public AbstractVariable[] get_parents() throws RemoteException;

	/** Retrieve a list of the names of the child variables of this variable.
	  */
	public String[] get_childrens_names() throws RemoteException;

	/** Retrieve a list of references to the child variables of this variable.
	  */
	public AbstractVariable[] get_children() throws RemoteException;

	/** Retrieve a reference to the conditional distribution of this variable given its parents.
	  * The reference is null if no distribution has yet been specified for this variable.
	  */
	public ConditionalDistribution get_distribution() throws RemoteException;

	/** Retrieve a reference to the posterior distribution of this variable given 
	  * any evidence variables. The reference is null if the posterior has not been
	  * computed given the current evidence.
	  */
	public Distribution get_posterior() throws RemoteException;

	/** Tell this variable to add another to its list of children.
	  * Since the other variable may be remote, we need a method to
	  * do this, since we can't access the children list directly.
	  */
	public void add_child( String child_name, AbstractVariable x ) throws RemoteException;

	/** Translates values named by strings into numeric values.
	  * This applies only to discrete variables; if the variable is continuous,
	  * or if it is discrete but the string value has not been established
	  * (as in a "type" definition in a belief network description file),
	  * then this method throws an exception.
	  */
	public int numeric_value( String string_value ) throws RemoteException;

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException, RemoteException;

	/** Create a description of this variable as a string. 
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws RemoteException;
}
