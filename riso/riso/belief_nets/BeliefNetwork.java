package belief_nets;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import distributions.*;

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

	/** Retrieve the name of this belief network.
	  */
	public String get_name() throws RemoteException
	{
		return name;
	}

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public AbstractVariable[] get_variables() throws RemoteException
	{
		return variables;
	}

	/** Mark the variable <tt>x</tt> as not observed. Clear the posterior
	  * distributions for the variables to which <tt>x</tt> is d-connected.
	  * Do not recompute posterior probabilities in the belief network.
	  */
	public void clear_evidence( AbstractVariable x ) throws RemoteException
	{
		// if ( ! x.is_evidence )
			// return;

		// x.is_evidence = false;

		int i;
		for ( i = 0; i < variables.length; i++ )
		{
			// if ( d_connected_thru_parent( variables[i], x ) )
			// {
				// variables[i].pi = null;
				// variables[i].posterior = null;
			// }
			// else if ( d_connected_thru_child( variables[i], x ) )
			// {
				// variables[i].lambda = null;
				// variables[i].posterior = null;
			// }
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
		for ( i = 0; i < variables.length; i++ )
		{
			// variables[i].is_evidence = false;
			// variables[i].posterior = null;
		}
	}

	public void assign_evidence( AbstractVariable x, double value ) throws RemoteException
	{
	}

	public void compute_posterior( AbstractVariable x ) throws RemoteException
	{
	}

	public void compute_all_posteriors() throws RemoteException
	{
	}

	/** @throws BadArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( AbstractVariable x, AbstractVariable e ) throws RemoteException, IllegalArgumentException
	{
		throw new IllegalArgumentException("BeliefNetwork::compute_information: not yet.");
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable x ) throws RemoteException
	{
		Variable xx = (Variable) x;	// WILL THIS WORK REMOTELY ???

		if ( xx.posterior == null )
			compute_posterior(xx);
		return xx.posterior;
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable[] x ) throws RemoteException
	{
		return null;
	}

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException, RemoteException
	{
		st.nextToken();
		System.err.println( "name: ttype: "+st.ttype+"  sval: "+st.sval );
		name = st.sval;

		st.nextToken();
		System.err.println( "ttype: "+st.ttype+"  sval: "+st.sval );
		if ( st.ttype != '{' )
			throw new IOException( "BeliefNetwork.pretty_input: input doesn't have opening bracket." );

		for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
		{
		System.err.println( "top of loop: ttype: "+st.ttype+"  sval: "+st.sval );
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				String variable_type = st.sval;
				Variable new_variable = null;

				try
				{
					Class variable_class = Class.forName(variable_type);
					new_variable = (Variable) variable_class.newInstance();
				}
				catch (Exception e)
				{
					throw new IOException("BeliefNetwork.pretty_input: can't "+
						"create an object of type "+variable_type );
				}

				st.nextToken();
				String variable_name = st.sval;

				new_variable.name = variable_name;
				new_variable.pretty_input(st);
			}
			else
			{
				throw new IOException( "BeliefNetwork.pretty_input: unexpected token: "+st );
			}
		}

		try { assign_references(); }
		catch (UnknownParentException e)
		{
			throw new IOException( "attempt to read belief network failed:\n"+e );
		}
	}

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_output( OutputStream os ) throws IOException, RemoteException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( this.getClass().getName()+" "+name+"\n"+"{"+"\n" );

		if ( variables != null )
			for ( int i = 0; i < variables.length; i++ )
				variables[i].pretty_output( os, "\t" );

		dest.print( "}"+"\n" );
	}

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public AbstractVariable name_lookup( String some_name ) throws RemoteException
	{
		for ( int i = 0; i < variables.length; i++ )
		{
			if ( some_name.equals(variables[i].name) )
				return variables[i];
		}

		return null;
	}

	/** Add a variable to the belief network. A new object of type
	  * <tt>Variable</tt> is created if the argument <tt>new_variable</tt>
	  * is <tt>null</tt>, otherwise <tt>new_variable</tt> is used.
	  * A caller that wants to construct a belief network out of variables
	  * more complicated than <tt>Variable</tt> should allocate a new
	  * instance of the desired class (which must be derived from
	  * <tt>Variable</tt>) and pass that in.
	  * @param name Name of the new variable.
	  * @param parents_names Names of the parents of the new variable. These
	  *   can include names of the form <tt>some_bn.some_variable</tt>, where
	  *   <tt>some_bn</tt> is the name of another belief network; an attempt
	  *   will be made to locate the referred-to belief network.
	  * @return A reference to the new variable.
	  * @throws UnknownParentException If a parent cannot be located.
	  */
	public AbstractVariable add_variable( String name_in, String[] parents_names, AbstractVariable new_variable ) throws UnknownParentException, RemoteException
	{
		Variable xx = (Variable) new_variable;

		if ( xx == null )
			xx = new Variable();

		xx.name = name_in;
		// FILL THIS IN !!!

		return xx;
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
		if ( variables == null )
			// Empty network -- no references to assign.
			return;

		try { locate_references(); }
		catch (UnknownNetworkException e)
		{
			throw new UnknownParentException( "some referred-to network can't be located:\n"+e );
		}

		for ( int i = 0; i < variables.length; i++ )
		{
			String parent_name = "!!!";
			Enumeration enum = variables[i].parents.keys();
			while ( enum.hasMoreElements() )
			{
				AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) BeliefNetworkContext.reference_table.get(enum.nextElement());
				try
				{
					variables[i].parents.put(parent_name,parent_bn.name_lookup("???"));
				}
				catch (RemoteException e)
				{
					variables[i].parents.put(parent_name,null);
				}
				// COMPLETE !!! THIS IS NOT CORRECT !!!
			}

			enum = variables[i].children.keys();
		}
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

		if ( variables == null )
			// Empty network; no references to locate.
			return;

		for ( i = 0; i < variables.length; i++ )
		{
			Enumeration enum = variables[i].parents.keys();
			while ( enum.hasMoreElements() )
			{
				String parent_name = (String) enum.nextElement();
				int period_index;
				if ( (period_index = parent_name.lastIndexOf(".")) != -1 )
				{
					String bn_name = parent_name.substring(0,period_index);
					if ( BeliefNetworkContext.reference_table.get(bn_name) == null )
					{
						AbstractBeliefNetwork new_reference = null;
						try { new_reference = BeliefNetworkContext.load_network(bn_name); }
						catch (IOException e)
						{
							throw new UnknownNetworkException( "attempt to load network failed:\n"+e );
						}

						BeliefNetworkContext.reference_table.put(bn_name,new_reference);
					}
				}
			}
		}
	}
}
