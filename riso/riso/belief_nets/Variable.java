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
import riso.remote_data.*;
import riso.general.*;

/** An instance of this class is a variable, also called a node, in a belief network.
  * A variable is a place-holder in a set of propositions; it's convenient to label
  * the place-holder with something, so we assign a name to it.
  */
public class Variable extends RemoteObservableImpl implements AbstractVariable, Serializable, Perishable
{
	/** List of other variables for which there are outstanding message requests.
	  * Another variable is put on this list when a message from this variable to the other
	  * one is requested, and it is removed from the list when the message is ready.
	  */
	Vector pending_message_recipients = new Vector(5);

	/** Most recently computed prior for this variable. This is defined as
	  * <tt>p(this variable)</tt>, i.e. the marginal in the absence of evidence.
	  */
	protected Distribution prior = null;

	/** Most recently computed pi for this variable. This is
	  * defined as <tt>p(this variable|evidence above)</tt>.
	  */
	protected Distribution pi = null;

	/** Most recently computed lambda for this variable. This is
	  * defined as <tt>p(evidence below|this variable)</tt>.
	  */
	protected Distribution lambda = null;

	/** List of the priors (i.e., marginal distributions) of parents of this
	  * variable. This list parallels the list of parents.
	  */
	protected Distribution[] parents_priors = new Distribution[0];
	
	/** Table which contains priors for specified parents. The parent name is the key,
	  * and the prior distribution is the value. If a prior is specified in the variable
	  * description, that prior is used instead of requesting a computation from the 
	  * parent.
	  */
	protected Hashtable parents_priors_hashtable = new Hashtable(5);

	/** List of the pi-messages coming in to this variable from its parents.
	  * This list parallels the list of parents.
	  */
	protected Distribution[] pi_messages = new Distribution[0];

	/** List of the lambda-messages coming in to this variable from its 
	  * children. This list parallels the list of children.
	  */
	protected Distribution[] lambda_messages = new Distribution[0];

    PiHelper pi_helper_cache;

    LambdaHelper lambda_helper_cache;

    PiMessageHelper[] pi_message_helper_cache;

    LambdaMessageHelper[] lambda_message_helper_cache;

    PosteriorHelper posterior_helper_cache;

	/** Reference to the belief network which contains this variable.
	  * It's occaisonally useful to get a reference to the belief network
	  * given a reference to a variable within that network. 
	  */
	BeliefNetwork belief_network = null;

	/** This flag tells if this object is marked as ``stale.'' If the flag is
	  * set, all remote method invocations should fail; local method calls
	  * will succeed. I wonder if that is a poor design. ???
	  */
	public boolean stale = false;

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
	int type = VT_CONTINUOUS;

	/** List of the names of the states of this variable, if discrete.
	  */
	Vector states_names = new Vector(5);

	/** List of the names of the parents of this variable.
	  * The parent's name includes the name of the belief network to
	  * which it belongs, if the parent belongs to a different belief
	  * network than the child.
	  */
	Vector parents_names = new Vector(5);

	/** List of the names of the children of this variable.
	  * The child's name includes the name of the belief network to
	  * which it belongs, if the child belongs to a different belief
	  * network than the parent.
	  */
	Vector childrens_names = new Vector(5);

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

	/** Construct an empty variable. The default variable type is
	  * ``continuous.''
	  */
	public Variable() throws RemoteException { type = VT_CONTINUOUS; } 

	/** This method throws a <tt>StaleReferenceException</tt> if the this variable is
	  * stale or the belief network which contains this variable is stale.
	  * <tt>caller</tt> is the name of whatever called this method.
	  */
	void check_stale( String caller ) throws StaleReferenceException
	{
		if ( this.is_stale() )
			throw new StaleReferenceException("Variable."+caller+" failed.");
	}

	/** This method returns <tt>true</tt> if the this variable is
	  * stale or the belief network which contains this variable is stale.
	  */
	public boolean is_stale() { return stale || (belief_network != null && belief_network.is_stale()); }

	/** Tells all parents and children that messages originating from this
	  * variable are now invalid, then sets the <tt>stale</tt> flag for this variable.
	  */
	public void set_stale()
	{
		for ( int i = 0; i < parents.length; i++ )
		{
			if ( parents[i] == null ) continue;

			try { parents[i].invalid_lambda_message_notification( this ); }
			catch (StaleReferenceException e) {} // eat it; don't bother with stack trace.
			catch (RemoteException e) // don't bother to reconnect here.
{ e.printStackTrace(); }
		}

		for ( int i = 0; i < children.length; i++ )
			try { children[i].invalid_pi_message_notification( this ); }
			catch (StaleReferenceException e) {} // eat it; don't bother with stack trace.
			catch (RemoteException e) // don't worry about exception.
{ e.printStackTrace(); }

		stale = true;
	}

	/** Retrieves a reference to the belief network which contains this
	  * variable.
	  */
	public AbstractBeliefNetwork get_bn() throws RemoteException
	{
		check_stale( "get_bn" );
		return belief_network;
	}

	/** Retrieve just the name of this variable alone; doesn't
	  * include the name of the belief network.
	  */
	public String get_name() throws RemoteException
	{
		check_stale( "get_name" );
		return name; 
	}

	/** Retrieve the name of this variable, including the name of the
	  * belief network which contains it.
	  */
	public String get_fullname() throws RemoteException
	{
		check_stale( "get_fullname" );
		if ( belief_network == null )
			// Cover your ass with a big piece of plywood.
			return "(unknown network)."+name;
		else
			return belief_network.get_fullname()+"."+name;
	}

    /** Retrieve the type (discrete or continuous) of this variable.
      */
    public int get_type() throws RemoteException { return type; }

    /** Retrieve the list of identifiers of states of this variable.
      * This method clones the <tt>states_names</tt> list.
      */
    public Vector get_states_names() throws RemoteException { return (Vector) states_names.clone(); }

	/** Retrieve a list of the names of the parent variables of this variable.
	  */
	public String[] get_parents_names() throws RemoteException
	{
		check_stale( "get_parents_names" );
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
		check_stale( "get_parents" );
		return parents;
	}

	/** Retrieve a list of the names of the child variables of this variable.
	  */
	public String[] get_childrens_names() throws RemoteException
	{
		check_stale( "get_childrens_names" );
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
		check_stale( "get_children" );
		return children;
	}

	/** Retrieve a reference to the conditional distribution of this variable given its parents.
	  * The reference is null if no distribution has yet been specified for this variable.
	  */
	public ConditionalDistribution get_distribution() throws RemoteException
	{
		check_stale( "get_distribution" );
		return distribution;
	}

	/** Retrieve a reference to the posterior distribution of this variable
	  * given any evidence variables. The reference is null if the posterior
	  * has not been computed.
	  */
	public Distribution get_posterior() throws RemoteException
	{
		check_stale( "get_posterior" );
		return posterior;
	}

	/** Retrieve a reference to the marginal distribution of this variable,
	  * ignoring any evidence. The reference is null if the prior
	  * has not been computed.
	  */
	public Distribution get_prior() throws RemoteException
	{
		check_stale( "get_prior" );
		return prior;
	}

	/** Retrieve a reference to the predictive distribution of this variable
	  * given any evidence variables. The reference is null if the predictive 
	  * distribution has not been computed.
	  */
	public Distribution get_pi() throws RemoteException
	{
		check_stale( "get_pi" );
		return pi;
	}

	/** This method requests pi messages and computes a pi function for this variable,
	  * but the pi reference is NOT set to the result.
	  * This is intended for calculation of likelihood functions -- this variable is
	  * evidence, but we need to compute pi and then find <tt>pi.p(posterior.get_support())</tt>,
	  * essentially.
	  */
	public Distribution compute_pi() throws RemoteException
	{
		check_stale( "compute_pi" );

		try
		{
			belief_network.get_all_pi_messages(this);
            // HEY !!! SHOULD WE SET this.pi_helper_cache TO THE RETURN VALUE HERE ???
			PiHelper ph = PiHelperLoader.load_pi_helper( this.pi_helper_cache, distribution, pi_messages );
			return ph.compute_pi( distribution, pi_messages );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "Variable.compute_pi for "+get_fullname()+" failed; "+e );
		}
	}

	/** Retrieve a reference to the likelihood function of this variable given 
	  * any evidence variables. The reference is null if the likelihood
	  * function has not been computed.
	  */
	public Distribution get_lambda() throws RemoteException
	{
		check_stale( "get_lambda" );
		return lambda;
	}

	/** Retrieve the list of the priors of parents of this variable.
	  */
	public Distribution[] get_parents_priors() throws RemoteException
	{
		check_stale( "get_parents_priors" );
		return parents_priors;
	}

	/** Retrieve the list of predictive messages coming into this variable
	  * given any evidence variables. The list is an array with the number
	  * of elements equal to the number of parents; if some pi message has
	  * not been computed, the corresponding element is null. The list has
	  * zero length until the parent references have been set up, usually
	  * shortly after parsing the description for this variable.
	  */
	public Distribution[] get_pi_messages() throws RemoteException
	{
		check_stale( "get_pi_messages" );
		return pi_messages;
	}

	/** Retrieve the list of likelihood messages coming into this variable
	  * given any evidence variables. The list is an array with the number
	  * of elements equal to the number of children; if some lambda message has
	  * not been computed, the corresponding element is null. The list has
	  * zero length until the child references have been set up, usually
	  * shortly after parsing the description for this variable.
	  */
	public Distribution[] get_lambda_messages() throws RemoteException
	{
		check_stale( "get_lambda_messages" );
		return lambda_messages;
	}

	/** Set the likelihood function for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All lambda messages are cleared, and the posterior is cleared.
	  */
	public void set_lambda( Distribution p ) throws RemoteException
	{
		check_stale( "set_lambda" );

		for ( int i = 0; i < lambda_messages.length; i++ ) lambda_messages[i] = null;

		notify_all_invalid_pi_message();
		notify_all_invalid_lambda_message();

		lambda = p;
		posterior = null;

		notify_observers( "lambda", this.lambda );
		notify_observers( "posterior", this.posterior );
	}

	/** Set the predictive distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All pi messages are cleared, and the posterior is cleared.
	  */
	public void set_pi( Distribution p ) throws RemoteException
	{
		check_stale( "set_pi" );

		for ( int i = 0; i < pi_messages.length; i++ ) pi_messages[i] = null;

		notify_all_invalid_pi_message();
		notify_all_invalid_lambda_message();

		pi = p;
		posterior = null;

		notify_observers( "pi", this.pi );
		notify_observers( "posterior", this.posterior );
	}
	
	/** Set the posterior distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All pi and lambda messages are cleared. <tt>pi</tt> is set to the argument <tt>p</tt>,
	  * and <tt>lambda</tt> is set to <tt>Noninformative</tt>.
	  * THIS METHOD SHOULD SPECIAL-CASE <tt>p instanceof Delta</tt> !!!
	  */
	public void set_posterior( Distribution p ) throws RemoteException
	{
		check_stale( "set_posterior" );

		posterior = p;
		pi = p;
		lambda = new Noninformative();

		notify_all_invalid_lambda_message();
		notify_all_invalid_pi_message();

		notify_observers( "pi", pi );
		notify_observers( "lambda", lambda );
		notify_observers( "posterior", posterior );
	}

	/** Set the conditional distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * Pi is cleared, posterior is cleared; lambda is not cleared, pi and lambda messages are not cleared.
	  */
	public void set_distribution( ConditionalDistribution p ) throws RemoteException
	{
		check_stale( "set_distribution" );

		p.set_variable(this);
		distribution = p;

		posterior = null;
		pi = null;

		notify_all_invalid_lambda_message();
		notify_all_invalid_pi_message();

		notify_observers( "pi", pi );
		notify_observers( "posterior", posterior );
	}

	/** Tells this variable to add another to its list of parents.
	  * @param parent_name Name of the parent of the new variable. This 
	  *   can be a name of the form <tt>some_bn.some_variable</tt>, where
	  *   <tt>some_bn</tt> is the name of another belief network; an attempt
	  *   will be made to locate the referred-to belief network.
	  * @throws RemoteException If the parent cannot be located.
	  */
	public void add_parent( String parent_name ) throws RemoteException
	{
		check_stale( "add_parent" );

		int i, new_index = parents_names.size();
		parents_names.addElement( parent_name );

		AbstractBeliefNetwork parent_bn = null;
		AbstractVariable parent = null;
		NameInfo ni = NameInfo.parse_variable( parent_name, this.belief_network.belief_network_context );

		if ( ni.beliefnetwork_name != null )
		{
			// First seek the parent bn in the same context as the bn of the variable who is asking.
			if ( (parent_bn = (AbstractBeliefNetwork) this.belief_network.belief_network_context.get_reference(ni)) == null )
			{
				try { ni.resolve_variable(); }
				// catch (Exception e) { throw new RemoteException( "Variable.add_parent: failed, "+e ); }
catch (Exception e) { e.printStackTrace(); throw new RemoteException( "Variable.add_parent: failed, "+e ); }
				parent_bn = (AbstractBeliefNetwork) ni.beliefnetwork;
				parent = ni.variable;
			}
			else
			{
				parent = (AbstractVariable) parent_bn.name_lookup( ni.variable_name );
			}
		}
		else
			parent = (AbstractVariable) this.belief_network.name_lookup( ni.variable_name );

		if ( parent == null )
			System.err.println( "Variable.add_parent: can't locate "+parent_name+"; stagger forward." );

		// Now we've resolved the name into a reference; put reference on parents list.
		add_parent( parent, new_index );
	}

	public void add_parent( AbstractVariable parent, int new_index ) throws RemoteException
	{
		AbstractVariable[] old_parents = parents;
		parents = new AbstractVariable[ parents_names.size() ];
		for ( int i = 0; i < new_index; i++ )
			parents[i] = old_parents[i];
		parents[new_index] = parent;

		Distribution[] old_priors = parents_priors;
		Distribution[] old_pi_messages = pi_messages;
		parents_priors = new Distribution[ parents.length ];
		pi_messages = new Distribution[ parents.length ];

		for ( int i = 0; i < new_index; i++ )
		{
			parents_priors[i] = old_priors[i];
			pi_messages[i] = old_pi_messages[i];
		}

		String new_parent_name = (String) parents_names.elementAt(new_index);
		Distribution prior;

		if ( (prior = (Distribution) parents_priors_hashtable.get(new_parent_name)) != null )
		{
			// Use specified prior for parent when there's no pi message.
System.err.println( "add_parent: use "+prior.getClass().getName()+" prior for "+new_parent_name );
			parents_priors[new_index] = prior;
		}
		else
		{
			// If the new parent is remote, we might need a prior to use in case of communication failure.
			// However, let's be a little conservative; request a prior only if the parent bn is in a
			// different context. If it's in the same context, don't request a prior.

			AbstractBeliefNetwork parent_bn = parent.get_bn();
			if ( parent_bn != null && parent_bn.get_context() != this.belief_network.get_context() )
				parents_priors[new_index] = parent_bn.get_prior(parent);
		}

		pi = null;
		posterior = null;
		prior = null;
		// SHOULD I CLEAR lambda AND lambda_messages HERE ???

		notify_observers( "pi", this.pi );
		notify_observers( "posterior", this.posterior );

		if ( parent != null ) parent.add_child( this );
	}

	/** Tells this variable to remove a child variable from its list 
	  * of children. This often happens because the child is in a remote belief
	  * network that is no longer reachable; or it could happen due to
	  * editing the belief network to which this variable belongs.
	  */
	public void remove_child( AbstractVariable x ) throws RemoteException
	{
		check_stale( "remove_child" );

		int i, j, child_index = -1;
		for ( i = 0; i < children.length; i++ )
			if ( children[i] == x )
			{
				child_index = i;
				break;
			}

		if ( child_index == -1 ) return;	// SOMETHING MORE INFORMATIVE HERE ???

		childrens_names.removeElementAt( child_index );
		AbstractVariable[] old_children = children;
		children = new AbstractVariable[ old_children.length-1 ];
		for ( i = 0, j = 0; i < old_children.length; i++ )
		{
			if ( i == child_index ) continue;
			children[j++] = old_children[i];
		}

		Distribution lm = lambda_messages[ child_index ];
		boolean informative_child = (lm != null && !(lm instanceof Noninformative));

		if ( informative_child )
		{
			// Now that the child has gone away, clear its lambda message;
			// that changes the lambda and posterior of this variable.

			Distribution[] old_lambda_messages = lambda_messages;
			lambda_messages = new Distribution[ old_lambda_messages.length-1 ];
			for ( i = 0, j = 0; i < old_lambda_messages.length; i++ )
			{
				if ( i == child_index ) continue;
				lambda_messages[j++] = old_lambda_messages[i];
			}

			lambda = null;
			posterior = null;
			// SHOULD I CLEAR pi AND pi_messages HERE ???

			notify_observers( "lambda", this.lambda );
			notify_observers( "posterior", this.posterior );
		}
		else
		{
			// The child to be removed didn't contribute any info, so don't
			// disturb lambda messages originating from the remaining children.

			Distribution[] old_lambda_messages = lambda_messages;
			lambda_messages = new Distribution[ old_lambda_messages.length-1 ];
			for ( i = 0, j = 0; i < old_lambda_messages.length; i++ )
			{
				if ( i == child_index ) continue;
				lambda_messages[j++] = old_lambda_messages[i];
			}
		}
	}

	/** Tells this variable to add another to its list of children.
	  * If the new child has the same name as an existing child, treat it as a replacement;
	  * clear lambda and the posterior for this variable if the existing child provided an informative
	  * lambda message. Otherwise, do not clear the lambda or posterior -- either the existing
	  * child was non-informative, or the child is new and we can treat it as having not yet
	  * provided any information.
	  */
	public void add_child( AbstractVariable x ) throws RemoteException
	{
		check_stale( "add_child" );

		String child_name = x.get_fullname();

		if ( childrens_names.contains( child_name ) )
		{
			int i = childrens_names.indexOf( child_name );
			AbstractVariable c = children[i];
			try
			{
				if ( c != null )
				{
					String n = c.get_fullname();
					// return; WHY DOESN'T THIS WORK AS EXPECTED ???
				}
			}
			catch (RemoteException e) { System.err.println( "add_child: replace dead link for child["+i+"]: "+child_name ); }

			children[i] = x;
			if ( lambda_messages[i] != null && !( lambda_messages[i] instanceof Noninformative) )
			{
				lambda_messages[i] = null;
				lambda = null;
				posterior = null;

				notify_all_invalid_pi_message();
				notify_observers( "lambda", this.lambda );
				notify_observers( "posterior", this.posterior );
			}
		}
		else
		{
			int i, new_index = childrens_names.size();
			childrens_names.addElement( child_name );

			AbstractVariable[] old_children = children;
			children = new AbstractVariable[ childrens_names.size() ];
			for ( i = 0; i < new_index; i++ )
				children[i] = old_children[i];
			children[ new_index ] = x;

			Distribution[] old_lambda_messages = lambda_messages;
			lambda_messages = new Distribution[ children.length ];
			System.arraycopy( old_lambda_messages, 0, lambda_messages, 0, old_lambda_messages.length );
			// The last lambda_message element is null, showing we've received no information yet.
		}
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  * This method constructs a tokenizer for the string (via <tt>StringReader</tt>)
	  * and then calls <tt>pretty_input</tt>, which see.
	  */
	public void parse_string( String description ) throws IOException
	{
		check_stale( "parse_string" );
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
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
							parents_names.addElement( st.sval );
						}
						else
							throw new IOException( "Variable.pretty_input: parsing "+name+": unexpected token in parent list; parser state: "+st );
					}
				}
				else if ( "parent-prior".equals(st.sval) )
				{
					st.nextToken();
					String parent_name = st.sval;
					Distribution prior = null;

					try
					{
						st.nextToken();
						Class pclass = java.rmi.server.RMIClassLoader.loadClass(st.sval);
						prior = (Distribution) pclass.newInstance();
						prior.set_variable(this);	// for benefit of certain distribution classes; ignored by most kinds
					}
					catch (Exception e)
					{
						throw new IOException( "Variable.pretty_input: attempt to create distribution failed:\n"+e );
					}

					st.nextBlock();
					prior.parse_string(st.sval);
					parents_priors_hashtable.put( parent_name, prior );
				}
				else if ( "distribution".equals(st.sval) )
				{
					try
					{
						st.nextToken();
						Class component_class = java.rmi.server.RMIClassLoader.loadClass(st.sval);
						distribution = (ConditionalDistribution) component_class.newInstance();
						distribution.set_variable(this);	// for benefit of certain distribution classes; ignored by most kinds
					}
					catch (Exception e)
					{
						throw new IOException( "Variable.pretty_input: attempt to create distribution failed:\n"+e );
					}

					st.nextBlock();
					distribution.parse_string(st.sval);
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
		check_stale( "pretty_output" );
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Create a description of this variable as a string. This is 
	  * useful for obtaining descriptions of remote variables.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		check_stale( "format_string" );

		String result = "";
		result += this.getClass().getName()+" "+name+"\n"+leading_ws+"{"+"\n";

		String more_leading_ws = leading_ws+"\t";

		result += more_leading_ws+"type ";
		switch ( type )
		{
		case VT_CONTINUOUS:
			result += "continuous\n";
			break;
		case VT_DISCRETE:
			result += "discrete";
			if ( states_names != null && states_names.size() > 0 )
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

		Enumeration enump = parents_names.elements();
		if ( enump.hasMoreElements() )
		{
			result += more_leading_ws+"parents";
			result += " { ";
			while ( enump.hasMoreElements() )
				result += (String) enump.nextElement()+" ";
			result += "}\n";
		}

		Enumeration enumc = childrens_names.elements();
		if ( enumc.hasMoreElements() )
		{
			result += more_leading_ws+"% children";
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
			try { result += distribution.format_string( more_leading_ws ); }
			catch (IOException e) { throw new RemoteException( "Variable.format_string: failed: "+e ); }
		}

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Simplified output, especially suitable for debugging.
	  */
	public String toString()
	{
		try { check_stale( "toString" ); }
		catch (RemoteException e) { return name+": "+e; }
		return "["+this.getClass().getName()+" "+name+"]";
	}

	/** Tell if this variable is discrete or not. If it is not discrete,
	  * then it is continuous.
	  */
	public boolean is_discrete() throws RemoteException
	{
		check_stale( "is_discrete" );
		if ( type == VT_NONE ) throw new RemoteException( "Variable.is_discrete: variable has no type." );
		return type == VT_DISCRETE;
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
		check_stale( "numeric_value" );

		if ( type != VT_DISCRETE )
			throw new RemoteException( "Variable.numeric_value: variable "+name+" is not discrete." );

		int i;

		if ( states_names == null || (i = states_names.indexOf( string_value )) == -1 )
			throw new RemoteException( "Variable.numeric_value: variable "+name+" doesn't have a state ``"+string_value+"''" );
		
		return i;
	}

	public void notify_all_invalid_lambda_message() throws RemoteException
	{
		check_stale( "notify_all_invalid_lambda_message" );

		try
		{
			for ( int i = 0; i < parents.length; i++ )
			{
				if ( parents[i] == null ) continue;

				try { parents[i].invalid_lambda_message_notification( this ); }
				catch (RemoteException e)
				{
System.err.println( "notify_all_invalid_lambda_message: "+e );
					try { reconnect_parent(i); }
					catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
					parents[i].invalid_lambda_message_notification( this );
				}
			}
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			System.err.println( "Variable.notify_all_invalid_lambda_message: stagger forward." );
			// SOMETHING MORE INFORMATIVE HERE ???
		}
	}

	public void notify_all_invalid_pi_message() throws RemoteException
	{
		check_stale( "notify_all_invalid_pi_message" );

		int i = 0;
		while ( true )
		{
			AbstractVariable child = null;

			try 
			{
				for ( ; i < children.length; i++ )
				{
					child = children[i];
					try { child.invalid_pi_message_notification( this ); }
					catch (ServerException e) { throw e.detail; }
				}

				return;
			}
			catch (java.rmi.ConnectException e) { remove_child( child ); }
			catch (StaleReferenceException e) { remove_child( child ); }
			catch (Throwable t)
			{
				System.err.println( "invalid_pi_message_notification: skip child ["+i+"]; "+t );
				++i;
			}
		}
	}

	/** This method is called by a child to notify this variable that the lambda-message
	  * from the child is no longer valid. This parent variable must clear its lambda
	  * function and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_lambda_message_notification( AbstractVariable child ) throws RemoteException
	{
		check_stale( "invalid_lambda_message_notification" );

		int i, child_index = -1;
		for ( i = 0; i < children.length; i++ )
			if ( child.equals( children[i] ) )
			{
				child_index = i;
				break;
			}

		if ( child_index == -1 ) throw new RemoteException( "Variable.invalid_lambda_message_notification: "+child.get_fullname()+" is apparently not a child of "+this.get_fullname() );

		if ( lambda_messages[ child_index ] == null )
		{	
			// Nothing to do -- we haven't received any information from the child.
			return;
		}

		lambda_messages[ child_index ] = null;
		if ( posterior instanceof Delta ) return; // nothing further to do

		lambda = null;
		posterior = null;

		notify_observers( "lambda", this.lambda );
		notify_observers( "posterior", this.posterior );

		for ( i = 0; i < parents.length; i++ )
		{
			if ( parents[i] == null ) continue;

			try { parents[i].invalid_lambda_message_notification( this ); }
			catch (RemoteException e)
			{
System.err.println( "invalid_lambda_message_notification: "+e );
				try { reconnect_parent(i); }
				catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
				parents[i].invalid_lambda_message_notification( this );
			}
		}

		i = 0;
		while ( true )
		{
			AbstractVariable some_child = null;

			try 
			{
				for ( ; i < children.length; i++ )
					if ( i != child_index )
					{
						some_child = children[i];
						try { some_child.invalid_pi_message_notification( this ); }
						catch (ServerException e) { throw e.detail; }
					}

				break;
			}
			catch (java.rmi.ConnectException e)
			{
				remove_child( some_child );
				if ( i < child_index ) --child_index; // shift down one
			}
			catch (StaleReferenceException e)
			{
				remove_child( some_child );
				if ( i < child_index ) --child_index; // shift down one
			}
			catch (Throwable t)
			{
				System.err.println( "invalid_lambda_message_notification: skip child ["+i+"]; "+t );
				++i;
			}
		}
	}

	/** This method is called by a parent to notify this variable that the pi-message
	  * from the parent is no longer valid. This child variable must clear its pi
	  * distribution and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_pi_message_notification( AbstractVariable parent ) throws RemoteException
	{
		check_stale( "invalid_pi_message_notification" );

		int i, parent_index = -1;
		for ( i = 0; i < parents.length; i++ )
			if ( parent.equals( parents[i] ) )
			{
				parent_index = i;
				break;
			}

		if ( parent_index == -1 ) throw new RemoteException( "Variable.invalid_pi_message_notification: "+parent.get_fullname()+" is apparently not a parent of "+this.get_fullname() );

		if ( pi_messages[ parent_index ] == null )
		{	
			// Nothing to do -- we haven't received any information from the parent.
			return;
		}

		if ( posterior instanceof Delta )
		{
			// The posterior, pi, and pi messages (except for the one we now
			// know is invalid) for this variable remain valid, but
			// outgoing lambda messages are now invalid. 

			pi_messages[ parent_index ] = null;
			for ( i = 0; i < parents.length; i++ )
				if ( i != parent_index )
					try { parents[i].invalid_lambda_message_notification( this ); }
					catch (RemoteException e)
					{	
System.err.println( "invalid_pi_message_notification: "+e );
						try { reconnect_parent(i); }
						catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
						parents[i].invalid_lambda_message_notification( this );
					}

			return;
		}

		pi = null;
		pi_messages[ parent_index ] = null;
		posterior = null;

		notify_observers( "pi", this.pi );
		notify_observers( "posterior", this.posterior );

		if ( lambda == null || !(lambda instanceof Noninformative) )
		{
			for ( i = 0; i < parents.length; i++ )
				if ( i != parent_index )
					try { parents[i].invalid_lambda_message_notification( this ); }
					catch (RemoteException e)
					{	
System.err.println( "invalid_pi_message_notification: "+e );
						try { reconnect_parent(i); }
						catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
						parents[i].invalid_lambda_message_notification( this );
					}
		}

		notify_all_invalid_pi_message();
	}

	/** Try to contact a lost parent. See if the parent's belief network went
	  * down and came back up; if the parent b.n. is not up, try to have it
	  * reloaded by the parent's context.
	  */
	public void reconnect_parent( int i ) throws RemoteException
	{
		check_stale( "reconnect_parent" );

		AbstractBeliefNetwork parent_bn;
		NameInfo ni = null;

		parents[i] = null; // parent reference will be null unless the name lookup succeeds.

		try
		{
			String parent_name = (String) parents_names.elementAt(i);
			ni = NameInfo.parse_variable( parent_name, null );

			try
			{
				String url = "rmi://"+ni.host_name+":"+ni.rmi_port+"/"+ni.beliefnetwork_name;
				parent_bn = (AbstractBeliefNetwork) Naming.lookup(url);
				parent_bn.get_name();
			}
			catch (NotBoundException e)
			{
				AbstractBeliefNetworkContext bnc = locate_context(ni);
				parent_bn = bnc.load_network( ni.beliefnetwork_name );
				bnc.bind( parent_bn );
			}
			catch (RemoteException e)
			{
				AbstractBeliefNetworkContext bnc = locate_context(ni);
				parent_bn = bnc.load_network( ni.beliefnetwork_name );
				bnc.rebind( parent_bn );
			}
		}
		catch (Exception e) { throw new java.rmi.ConnectException( "reconnect_parent: i="+i+": "+e ); }

		try { parents[i] = (AbstractVariable) parent_bn.name_lookup( ni.variable_name ); }
		catch (Exception e) { throw new java.rmi.ConnectException( "reconnect_parent: i="+i+": "+e ); }

		if ( parents[i] == null ) 
			throw new java.rmi.ConnectException( "reconnect_parent: parent["+i+"] can't be located, although its belief network was located." );

		try
		{
			parents[i].add_child( this );
			pi_messages[i] = null; // pi message needs to be refreshed
			parents_priors[i] = parent_bn.get_prior( parents[i] );
		}
		catch (Exception e) { throw new java.rmi.ConnectException( "reconnect_parent: i="+i+": "+e ); }
	}

	/** This method finds a reference to a belief network context that
	  * can be used to load the variable described by <tt>name_info</tt>.
	  * The reference may be remote (obtained by consulting an RMI registry)
	  * but a local reference will be returned if possible.
	  * 
	  * <p> If a context can't be located, this method throws an exception.
	  */
	public AbstractBeliefNetworkContext locate_context( NameInfo name_info ) throws Exception
	{
		check_stale( "locate_context" );

		name_info.resolve_host();

		if ( this.belief_network != null && this.belief_network.belief_network_context != null )
		{
			BeliefNetworkContext bnc = this.belief_network.belief_network_context;
			if ( name_info.host_name.equals(bnc.registry_host) && name_info.rmi_port == bnc.registry_port )
				return bnc;
		}

		// Try to find a context in the list of registered objects.
		// Return the first context we find (if any).

		String url = "rmi://"+name_info.host_name+":"+name_info.rmi_port+"/";
		String[] names;

		try { names = Naming.list(url); }
		catch (Exception e) { e.printStackTrace(); return null; }

		for ( int i = 0; i < names.length; i++ )
		{
			Remote o;
			try { o = Naming.lookup( names[i] ); }
			catch (Exception e)
			{
				continue;
			}

			if ( o instanceof AbstractBeliefNetworkContext )
			{
System.err.println( "locate_context: found "+names[i] );
				return (AbstractBeliefNetworkContext) o;
			}
		}

		System.err.println( "locate_context: can't find a context in "+url );
		throw new Exception( "locate_context failed: "+name_info );
	}
}
