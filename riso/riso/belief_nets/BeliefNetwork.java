package belief_nets;

import java.rmi.*;
import java.io.*;
import remote_data.*;

public class BeliefNetwork extends UnicastRemoteObject implements AbstractBeliefNetwork
{
	Variable[] variables = null;
	String name = null;

	/** Create an empty belief network. The interesting initialization
	  * occurs in <tt>pretty_input</tt>. A belief network can also be
	  * built by creating new variables and linking them in
	  * using <tt>add_variable</tt>.
	  * @see pretty_input
	  * @see add_variable
	  */
	public BeliefNetwork() throws RemoteException {}

	/** Mark the variable <tt>x</tt> as not observed. Clear the posterior
	  * distributions for the variables to which <tt>x</tt> is d-connected.
	  * Do not recompute posterior probabilities in the belief network.
	  */
	public void clear_evidence( Variable x ) throws RemoteException
	{
		if ( ! x.is_evidence )
			return;

		x.is_evidence = false;

		int i;
		for ( i = 0; i < nvariables; i++ )
		{
			if ( d_connected_thru_parent( variables[i], x ) )
			{
				variables[i].pi = null;
				variables[i].posterior = null;
			}
			else if ( d_connected_thru_child( variables[i], x ) )
			{
				variables[i].lambda = null;
				variables[i].posterior = null;
			}
		}
	}

	/** Mark all variables as not observed.
	  * Clear all posterior distributions, since they are no longer valid.
	  * Do not recompute posterior probabilities in the belief network.
	  * COULD TRY HARDER TO ONLY CLEAR WHAT NEEDS TO BE CLEARED !!!
	  */
	public void clear_all_evidence() throws RemoteException
	{
		int i;
		for ( i = 0; i < nvariables; i++ )
		{
			variables[i].is_evidence = false;
			variables[i].posterior = null;
		}
	}

	public void compute_posterior( Variable x ) throws RemoteException
	{
	}

	public void compute_all_posteriors() throws RemoteException
	{
	}

	/** @throws BadArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( Variable x, Variable e ) throws Exception, RemoteException /* BadArgumentException; !!! */
	{
		throw new Exception("BeliefNetwork::compute_information: not yet.");
		return 0;
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution posterior( Variable x ) throws RemoteException
	{
		if ( x.posterior == null )
			compute_posterior(x);
		return x.posterior;
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution posterior( Variable[] x ) throws RemoteException
	{
		throw new Exception( "BeliefNetwork.posterior(Variable[] x): not implemented." );
	}

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public Variable[] get_variables() throws RemoteException
	{
		return variables;
	}

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException, RemoteException
	{
		st.nextToken();
		name = st.sval.clone();

		st.nextToken();	// eat open parenthesis -- should check it...

		for ( st.nextToken(); ! "}".equals(st.sval); st.nextToken() )
		{
			String variable_type = st.sval;
			Variable new_variable = null;

			try
			{
				Class variable_class = Class.forName(variable_type);
				new_variable = variable_class.newInstance();
			}
			catch (Exception e)
			{
				throw new IOException("BeliefNetwork.pretty_input: can't "+
					"create an object of type "+variable_type );
			}

			st.nextToken();
			String variable_name = st.sval.clone();

			new_variable.name = variable_name;
			new_variable.pretty_input(st);
		}

		locate_references();	// assign references to parents and children
	}

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  */
	public void pretty_output( OutputStream os ) throws IOException, RemoteException;

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public Variable name_lookup( String name ) throws RemoteException;

	/** Add a variable to the belief network. A new object of type
	  * <tt>Variable</tt> is created if the argument <tt>new_variable</tt>
	  * is <tt>null</tt>, otherwise <tt>new_variable</tt> is used.
	  * A caller that wants to construct a belief network out of variables
	  * more complicated than <tt>Variable</tt> should allocate a new
	  * instance of the desired class (which must be derived from
	  * <tt>Variable</tt>) and pass that in.
	  * @param name Name of the new variable.
	  * @param parents_names Names of the parents of the new variable. These
	  *   can include names of the form <tt>some_bn.some_variable</tt>, whee
	  *   <tt>some_bn</tt> is the name of another belief network; an attempt
	  *   will be made to locate the referred-to belief network.
	  * @return A reference to the new variable.
	  * @throws Exception If a parent cannot be located.
	  */
	public Variable add_variable( String name_in, String[] parents_names, Varin
	{
		if ( new_variable == null )
			new_variable = new Variable();

		new_variable.name = name_in;
		// FILL THIS IN !!!

		return new_variable;
	}

	/** Assign references to parents and children. This is usually called
	  * once, just after initialization, but it could be called more than
	  * once. For example, if one edits the belief network via
	  * <tt>add_variable</tt> and <tt>remove_variable</tt>, this function
	  * should be called to update the references after editing.
	  * @throws UnknownParentException If a reference for some parent
	  *   referred to by a variable in this network cannot be obtained.
	  */
	void assign_references() throws UnknownParentException
	{
	}

	/** Verify that all other networks referred to by this one can be
	  * located. First go through the list of all the parents' names 
	  * in this network and find any parents of the form "<tt>something.x</tt>".
	  * See if each <tt>something</tt> on the list of networks for which
	  * a reference exists; if not, try to add it to the list. If it can't
	  * be added to the list, throw an exception. If this method returns
	  * without throwing an exception, it means that any referred-to networks 
	  * are now in the reference table.
	  *
	  * @throws UnknownNetworkException If some network can't be located.
	  * @see BeliefNetworkContext.reference_table
	  * @see BeliefNetworkContext.load_network
	  */
	void locate_references() throws UnknownNetworkException
	{
		int i, j;

		for ( i = 0; i < variables.length; i++ )
		{
			for ( j = 0; j < variables[i].parents_names.length; j++ )
			{
				int period_index;
				if ( (period_index = variables[i].parents_names[j].lastIndexOf(".")) != -1 )
				{
					String bn_name = variables[i].parents_names[j].substring(0,period_index);
					if ( BeliefNetworkContext.reference_table.get(bn_name) == null )
					{
						BeliefNetwork new_reference = BeliefNetworkContext.load_network(bn_name);
						BeliefNetworkContext.reference_table.add(bn_name,new_reference);
					}
				}
			}
		}
	}
}
