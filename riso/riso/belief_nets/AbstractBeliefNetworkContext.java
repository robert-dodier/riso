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
import java.rmi.*;

/** This interface defines methods, which are remotely visible, for 
  * loading and looking up belief networks. Note that networks loaded
  * by these methods are running on the machine containing the implementation
  * of this interface. This provides a means of starting up a belief
  * network on remote machine.
  */
public interface AbstractBeliefNetworkContext extends Remote
{
	/** This method returns the name with which this context is
	  * registered in the RMI registry.
	  */
	public String get_name() throws RemoteException;
	
	/** This method searches the path list to locate the belief network
	  * file. The filename must have the form "<tt>something.riso</tt>".
	  * The name of the belief network in this case is "<tt>something</tt>".
	  * This method does not bind the belief network in the RMI registry;
	  * call <tt>AbstractBeliefNetworkContext.bind</tt> for that.
	  *
	  * @param bn_name Name of the belief network; does not include
	  *   "<tt>.riso</tt>".
	  * @return A reference to the belief network -- may be a remote reference.
	  * @throws RemoteException If the network cannot be located, or cannot
	  *   be read in successfully.
	  * @see AbstractBeliefNetwork.pretty_input
	  */
	public AbstractBeliefNetwork load_network( String bn_name ) throws RemoteException;

	/** This method is similar to <tt>load_network</tt>, except that the
	  * belief network description is supplied as a string instead of a file.
	  * If the string is successfully parsed, a new belief network is 
	  * created and a reference is placed in the reference table. The name
	  * of the belief network is found in the description.
	  * This method does not bind the belief network in the RMI registry;
	  * call <tt>AbstractBeliefNetworkContext.bind</tt> for that.
	  *
	  * @param description Description of a belief network; the format is
	  *   the same as for loading a belief network from a file.
	  * @return A reference to the belief network -- may be a remote reference.
	  * @throws RemoteException If the description cannot be parsed.
	  * @see AbstractBeliefNetwork.pretty_input
	  */
	public AbstractBeliefNetwork parse_network( String description ) throws RemoteException;

	/** Given the name of a belief network, this method returns a reference
	  * to that belief network. The belief network name <tt>bn_name</tt>
	  * has the form <tt>something</tt> or <tt>qualified-hostname/something</tt>
	  * -- if the former, first check the list of belief nets loaded into
	  * this context, and return a reference if the b.n. is indeed loaded
	  * into this context, and if that fails then try to obtain a reference
	  * from the RMI registry running on the local host; if the latter,
	  * a reference is sought in the RMI registry running on the named host.
	  *
	  * <p> The reference returned is of type <tt>Remote</tt>, and thus
	  * it can be cast to any of the remote interfaces implemented by
	  * the belief network. The most important of these interfaces is
	  * <tt>AbstractBeliefNetwork</tt>, but <tt>RemoteObservable</tt> is
	  * sometimes useful as well.
	  *
	  * <p> This method does not load the belief network if it is not
	  * yet loaded, nor does it bind the belief network in the RMI registry.
	  */
	public Remote get_reference( NameInfo i ) throws RemoteException;

	/** This method gets a list of the names of helper classes known to this context.
	  * The argument <tt>helper_type</tt> is usually one of the following:
	  * <ul>
	  * <li> <tt>computes_pi</tt>
	  * <li> <tt>computes_lambda</tt>
	  * <li> <tt>computes_pi_message</tt>
	  * <li> <tt>computes_lambda_message</tt>
	  * <li> <tt>computes_posterior</tt>
	  * </ul>
	  * but not necessarily, since it's just a string which is pasted into
	  * a directory path.
	  *
	  * <p> The return value is an array of strings. If this context is either a
	  * local context or a context on the codebase host, you can call
	  * <tt>Class.forName</tt> with one of these strings as the argument.
	  */
	public String[] get_helper_names( String helper_type ) throws RemoteException;

	/** Binds the given reference in the RMI registry.
	  * Fails if the name is already bound.
	  */
	public void bind( AbstractBeliefNetwork bn ) throws RemoteException;

	/** Rebinds the given reference in the RMI registry.
	  */
	public void rebind( AbstractBeliefNetwork bn ) throws RemoteException;

	/** Cause the belief network context process to exit. This is to allow
	  * a remote operator to kill the context. This operation succeeds on
	  * stale contexts -- <tt>check_stale</tt> is not called.
	  */
	public void exit() throws RemoteException;

	/** Execute the <tt>main</tt> method in class <tt>class_name</tt>,
	  * with command-line arguments <tt>args</tt>.
	  */
	public void execute_app( String class_name, String[] args ) throws RemoteException;
}

