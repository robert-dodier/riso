package belief_nets;

import java.rmi.*;
import java.io.*;
import remote_data.*;

public class BeliefNetwork extends UnicastRemoteObject implements AbstractBeliefNetwork, RemoteObservable, RemoteObserver, Serializable
{
	/** Mark the variable <tt>x</tt> as not observed. Clear the posterior
	  * distributions for the variables to which <tt>x</tt> is d-connected.
	  * Do not recompute posterior probabilities in the belief network.
	  */
	public void clear_evidence( Variable x )
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
	public void clear_all_evidence()
	{
		int i;
		for ( i = 0; i < nvariables; i++ )
		{
			variables[i].is_evidence = false;
			variables[i].posterior = null;
		}
	}

	public void compute_posterior( Variable x );

	public void compute_all_posteriors();

	/** @throws BadArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( Variable x, Variable e ) throws Exception; /* BadArgumentException; !!! */

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution posterior( Variable x )
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
	public Distribution posterior( Variable[] x )
	{
		throw new Exception( "BeliefNetwork.posterior(Variable[] x): not implemented." );
	}

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public Variable[] get_variables();

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException;

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  */
	public void pretty_output( OutputStream os ) throws IOException;

	/** Make a deep copy of this belief network object and return it. Note
	  * that we can't say "<code>... extends Cloneable</code>" and get the same
	  * effect, since <code>Object.clone</code> is protected, not public.
	  */
	public Object clone() throws CloneNotSupportedException;

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public Variable name_lookup( String name );
}
