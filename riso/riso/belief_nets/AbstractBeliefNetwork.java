package belief_nets;

import java.io.*;
import distributions.*;

public interface AbstractBeliefNetwork
{
	/** Mark the variable <tt>x</tt> as not observed.
	  */
	public void clear_evidence( AbstractVariable x );

	/** Mark all variables as not observed.
	  */
	public void clear_all_evidence();

	/** Assign the value <tt>a</tt> to the variable <tt>x</tt>.
	  * A call to <tt>posterior(x)</tt> will then return a delta function
	  * centered on <tt>a</tt>. Either a continuous or discrete value may
	  * be represented by <tt>a</tt>.
	  */
	public void assign_evidence( AbstractVariable x, double a );

	public void compute_posterior( AbstractVariable x );

	public void compute_all_posteriors();

	/** @throws BadArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( AbstractVariable x, AbstractVariable e ) throws Exception; /* BadArgumentException; !!! */

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable x );

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable[] x );

	/** Retrieve a list of references to the parent variables of <tt>x</tt>.
	  */
	public AbstractVariable[] parents_of( AbstractVariable x );

	/** Retrieve a list of references to the child variables of <tt>x</tt>.
	  */
	public AbstractVariable[] children_of( AbstractVariable x );

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public AbstractVariable[] get_variables();

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
	public AbstractVariable name_lookup( String name );
}
