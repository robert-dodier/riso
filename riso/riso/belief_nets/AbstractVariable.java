/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.belief_nets;

import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import riso.general.*;

/** This is the interface for concrete variable classes.
  * This interface extends <tt>Remote</tt> so that it is possible 
  * to call these methods on remote variables.
  */
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

    /** Retrieve the type (discrete or continuous) of this variable.
      */
    public int get_type() throws RemoteException;

    /** Retrieve the list of identifiers of states of this variable.
      */
    public java.util.Vector get_states_names() throws RemoteException;

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

	/** Retrieve the list of the priors of parents of this variable.
	  */
	public Distribution[] get_parents_priors() throws RemoteException;

	/** Retrieve a reference to the marginal distribution of this variable,
	  * ignoring any evidence. The reference is null if the prior
	  * has not been computed.
	  */
	public Distribution get_prior() throws RemoteException;

	/** Retrieve a reference to the posterior distribution of this variable
	  * given any evidence variables. The reference is null if the posterior
	  * has not been computed.
	  */
	public Distribution get_posterior() throws RemoteException;

	/** Retrieve a reference to the predictive distribution of this variable
	  * given any evidence variables. The reference is null if the predictive 
	  * distribution has not been computed.
	  */
	public Distribution get_pi() throws RemoteException;

	/** This method requests pi messages and computes a pi function for this variable,
	  * but the pi reference is NOT set to the result.
	  */
	public Distribution compute_pi() throws RemoteException;

	/** Retrieve a reference to the likelihood function of this variable given 
	  * any evidence variables. The reference is null if the likelihood
	  * function has not been computed.
	  */
	public Distribution get_lambda() throws RemoteException;

	/** Retrieve the list of predictive messages coming into this variable
	  * given any evidence variables. The list is an array with the number
	  * of elements equal to the number of parents; if some pi message has
	  * not been computed, the corresponding element is null.
	  */
	public Distribution[] get_pi_messages() throws RemoteException;

	/** Retrieve the list of likelihood messages coming into this variable
	  * given any evidence variables. The list is an array with the number
	  * of elements equal to the number of children; if some lambda message has
	  * not been computed, the corresponding element is null.
	  */
	public Distribution[] get_lambda_messages() throws RemoteException;

	/** Tell this variable to add another to its list of parents.
	  */
	public void add_parent( String parent_name ) throws RemoteException;

	/** Tell this variable to add another to its list of children.
	  */
	public void add_child( AbstractVariable x ) throws RemoteException;

	/** Tell if this variable is discrete or not. If it is not discrete,
	  * then it is continuous.
	  */
	public boolean is_discrete() throws RemoteException;

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

	/** Set the likelihood function for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All lambda messages are cleared, and the posterior is cleared.
	  */
	public void set_lambda( Distribution p ) throws RemoteException;

	/** Set the predictive distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All pi messages are cleared, and the posterior is cleared.
	  */
	public void set_pi( Distribution p ) throws RemoteException;
	
	/** Set the posterior distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All pi and lambda messages are cleared. <tt>pi</tt> is set to the argument <tt>p</tt>,
	  * and <tt>lambda</tt> is set to <tt>Noninformative</tt>.
	  * THIS METHOD SHOULD SPECIAL-CASE <tt>p instanceof Delta</tt> !!!
	  */
	public void set_posterior( Distribution p ) throws RemoteException;
	
	/** Set the conditional distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * Pi is cleared, posterior is cleared; lambda is not cleared, pi and lambda messages are not cleared.
	  */
	public void set_distribution( ConditionalDistribution p ) throws RemoteException;
}
