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
import java.rmi.server.*;
import java.util.*;
import riso.distributions.*;
import riso.utility_models.*;
import riso.remote_data.*;
import SmarterTokenizer;

public class UtilityNode extends Variable
{
	UtilityModel utility_model;

	/** Construct an empty utility node.
	  */
	public UtilityNode() throws RemoteException { super(); }

	/** Tells all children that messages originating from this
	  * variable are now invalid, then sets the <tt>stale</tt> flag for this variable.
	  * This method DOES NOT notify parents, since a utility node cannot send a 
	  * lambda message.
	  */
	public void set_stale()
	{
		for ( int i = 0; i < children.length; i++ )
			try { children[i].invalid_pi_message_notification( this ); }
			catch (StaleReferenceException e) {} // eat it; don't bother with stack trace.
			catch (RemoteException e) // don't worry about exception.
{ e.printStackTrace(); }

		stale = true;
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a distribution associated with it.
	  */
	public ConditionalDistribution get_distribution() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.get_distributed: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a posterior associated with it.
	  */
	public Distribution get_posterior() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.get_posterior: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a prior associated with it.
	  */
	public Distribution get_prior() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.get_prior: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a pi distribution associated with it.
	  */
	public Distribution get_pi() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.get_pi: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a pi distribution associated with it.
	  */
	public Distribution compute_pi() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.compute_pi: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a pi distribution associated with it.
	  */
	public Distribution get_lambda() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.get_lambda: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a prior associated with it.
	  */
	public Distribution[] get_parents_priors() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.get_parents_priors: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have lambda messages associated with it.
	  */
	public Distribution[] get_lambda_messages() throws RemoteException
	{
		 throw new RuntimeException( "UtilityNode.get_lambda_messages: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a likelihood function associated with it.
	  */
	public void set_lambda( Distribution p ) throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.set_lambda: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a pi distribution associated with it.
	  */
	public void set_pi( Distribution p ) throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.set_pi: operation not defined." );
	}
	
	/** THROWS AN EXCEPTION. A utility node doesn't have a posterior distribution associated with it.
	  */
	public void set_posterior( Distribution p ) throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.set_posterior: operation not defined." );
	}

	/** THROWS AN EXCEPTION. A utility node doesn't have a conditional distribution associated with it.
	  */
	public void set_distribution( ConditionalDistribution p ) throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.set_distribution: operation not defined." );
	}

	/** Parse an input stream (represented as a tokenizer) for fields
	  * of this variable. THIS FUNCTION IS A HACK -- NEEDS WORK !!!
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		check_stale( "pretty_input" );

		st.nextToken();
		name = st.sval;

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "Variable.pretty_input: missing left curly brace; parser state: "+st );

		for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF && st.ttype != '}'; st.nextToken() )
		{
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				if ( "utility-model".equals(st.sval) )
				{
					try
					{
						st.nextToken();
						Class c = java.rmi.server.RMIClassLoader.loadClass(st.sval);
						utility_model = (UtilityModel) c.newInstance();
						utility_model.set_variable(this);
					}
					catch (Exception e)
					{
						throw new IOException( "UtilityNode.pretty_input: attempt to create distribution failed:\n"+e );
					}

					st.nextBlock();
					utility_model.parse_string(st.sval);
				}
				else
					super.pretty_input(st); // THIS WON'T WORK AS INTENDED !!! WORK IT OUT !!!
			}
			else
				throw new IOException( "UtilityNode.pretty_input: parsing "+name+": unexpected token; parser state: "+st );
		}
	}

	/** Create a description of this variable as a string. This is 
	  * useful for obtaining descriptions of remote variables.
	  * HOW CAN THIS WORK CORRECTLY FOR SUBCLASSES ???
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		return leading_ws+this.getClass().getName()+" { }\n";
	}

	public void notify_all_invalid_lambda_message() throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.notify_all_invalid_lambda_message: operation not defined." );
	}

	public void invalid_lambda_message_notification( AbstractVariable child ) throws RemoteException
	{
		throw new RuntimeException( "UtilityNode.invalid_lambda_message_notification: operation not defined." );
	}
}
