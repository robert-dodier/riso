package riso.belief_nets;

import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import SmarterTokenizer;

public interface AbstractVariable extends Remote
{
	/** Retrieves a reference to the belief network which contains this
	  * variable.
	  */
	public AbstractBeliefNetwork get_bn() throws RemoteException;

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

	/** Tell this variable to add another to its list of parents.
	  */
	public void add_parent( String parent_name ) throws RemoteException;

	/** Tell this variable to add another to its list of children.
	  */
	public void add_child( AbstractVariable x ) throws RemoteException;

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

	/** This method is called by a child to notify this variable that the lambda-message
	  * from the child is no longer valid. This parent variable must clear its lambda
	  * function and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_lambda_message_notification( AbstractVariable child ) throws RemoteException;

	/** This method is called by a parent to notify this variable that the pi-message
	  * from the parent is no longer valid. This child variable must clear its pi
	  * distribution and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_pi_message_notification( AbstractVariable parent ) throws RemoteException;
}
