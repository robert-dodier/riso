package riso.belief_nets;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import riso.distributions.*;
import riso.remote_data.*;
import SmarterTokenizer;
import Semaphore;

/** General policy enforced here: allow changes to member data only if
  * the variable is local and not remote. <barf> Otherwise a whole set of
  * "get/set" methods is required </barf>.
  */
public class BeliefNetwork extends RemoteObservableImpl implements AbstractBeliefNetwork, Serializable, Perishable
{
	Hashtable variables = new NullValueHashtable();
	String name = null;

	/** This flag tells if this object is marked as ``stale.'' If the flag is
	  * set, all remote method invocations should fail; local method calls
	  * will succeed. I wonder if that is a poor design. ???
	  */
	public boolean stale = false;

	/** The context to which this belief network belongs. This variable is set by
	  * <tt>BeliefNetworkContext.load_network</tt> and by <tt>BeliefNetworkContext.parse_network</tt>.
	  * Since this variable is publicly accessible, the context can be changed at any time.
	  */
	public BeliefNetworkContext belief_network_context = null;

	/** Create an empty belief network. The interesting initialization
	  * occurs in <tt>pretty_input</tt>. A belief network can also be
	  * built by creating new variables and linking them in
	  * using <tt>add_variable</tt>.
	  * @see pretty_input
	  * @see BeliefNetwork.add_variable, Variable.add_parent
	  */
	public BeliefNetwork() throws RemoteException {}

	/** This method throws a <tt>RemoteException</tt> if the this b.n. is
	  * stale or the belief network context which contains this b.n. is stale.
	  * <tt>caller</tt> is the name of whatever called this method.
	  */
	void check_stale( String caller ) throws RemoteException
	{
		if ( this.is_stale() )
			throw new RemoteException("BeliefNetwork."+caller+": failed; reference is stale." );
	}

	/** This belief network is stale if its <tt>stale</tt> flag is set or if
	  * the context which contains this belief network is stale.
	  */
	public boolean is_stale() { return stale || (belief_network_context != null && belief_network_context.is_stale()); }

	/** Marks stale all the variables in this belief network,
	  * then sets the <tt>stale</tt> flag.
	  */
	public void set_stale()
	{
		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
			((Variable)enum.nextElement()).set_stale();
		
		stale = true;
	}
	
	/** Simplified representation of this belief network,
	  * especially useful for debugging. 
	  */
	public String toString()
	{
		try { check_stale("toString"); }
		catch (RemoteException e) { return name+": "+e; }

		String classname = this.getClass().getName(), n = null, n2 = null;
		try { n = get_fullname(); }
		catch (RemoteException e) { n = "(unknown name)"; }
		try { n2 = belief_network_context.get_name(); }
		catch (RemoteException e) { n2 = "(unknown context)"; }
		return "["+classname+" "+n+", "+variables.size()+" variables; context: "+n2+"]";
	}

	/** Like <tt>toString</tt>, but this implementation returns something
	  * useful when this object is remote.
	  */
	public String remoteToString()
	{
		try { check_stale("remoteToString"); }
		catch (RemoteException e) { return name+": "+e; }
		return toString()+"(remote)";
	}

	/** Retrieve the context in which this belief network lives.
	  */
	public AbstractBeliefNetworkContext get_context() throws RemoteException
	{
		check_stale( "get_context" );
		return belief_network_context;
	}

	/** Retrieve just the name of this belief network.
	  */
	public String get_name() throws RemoteException
	{
		check_stale( "get_name" );
		return name;
	}

	/** Retrieve the full name of this belief network.
	  * This includes the name of the registry host from which this
	  * belief network may be retrieved, and the registry port number,
	  * if different from the default port number.
	  */
	public String get_fullname() throws RemoteException
	{
		check_stale( "get_fullname" );
		String ps = belief_network_context.registry_port==Registry.REGISTRY_PORT ? "" : ":"+belief_network_context.registry_port;
		return belief_network_context.registry_host+ps+"/"+name;
	}

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public AbstractVariable[] get_variables() throws RemoteException
	{
		check_stale( "get_variables" );
		AbstractVariable[] u = new AbstractVariable[ variables.size() ];
		Enumeration e = variables.elements();
		for ( int i = 0; e.hasMoreElements(); i++ )
			u[i] = (AbstractVariable) e.nextElement();

		return u;
	}

	/** Clear the posterior of <tt>some_variable</tt> but do not recompute it. This method also
	  * clears the pi and lambda for this variable. Notify remote observers
	  * that the posterior for this variable is no longer known (if it ever was).
	  * If this variable was evidence, notify parents and children that pi and
	  * lambda messages originating from this variable are now invalid.
	  */
	public void clear_posterior( AbstractVariable some_variable ) throws RemoteException
	{
		check_stale( "clear_posterior" );
		Variable x = to_Variable( some_variable, "BeliefNetwork.clear_posterior" );

		Distribution p = x.posterior; // hold on to a reference for a moment

		x.pi = null;
		x.lambda = null;
		x.posterior = null;

		x.notify_observers( "pi", x.pi );
		x.notify_observers( "lambda", x.lambda );
		x.notify_observers( "posterior", x.posterior );

		if ( p instanceof Delta ) // then this variable was evidence
		{
System.err.println( "BeliefNetwork.clear_posterior: tell parents of "+x.get_name() );
			x.notify_all_invalid_lambda_message();

System.err.println( "BeliefNetwork.clear_posterior: tell children of "+x.get_name() );
			x.notify_all_invalid_pi_message();
		}
	}

	/** Clear the posterior, pi, lambda, and pi and lambda messages received by
	  * <tt>some_variable</tt>, not necessarily in that order.
	  */
	public void clear_all( AbstractVariable some_variable ) throws RemoteException
	{
		check_stale( "clear_messages_etc" );
		Variable x = to_Variable( some_variable, "BeliefNetwork.clear_messages_etc" );

		// Call clear_posterior first because the invalid message methods check to 
		// see that each message is non-null before notifying the corresponding
		// parent or child.

		clear_posterior( some_variable );

		// Now clear out the pi and lambda messages.

		for ( int i = 0; i < x.pi_messages.length; i++ )
			x.pi_messages[i] = null;

		for ( int i = 0; i < x.lambda_messages.length; i++ )
			x.lambda_messages[i] = null;
	}

	public void assign_evidence( AbstractVariable some_variable, double value ) throws RemoteException
	{
		check_stale( "assign_evidence" );
		Variable x = to_Variable( some_variable, "BeliefNetwork.assign_evidence" );

		// If this variable is evidence, and the evidence is the same, then do nothing.

		if ( x.posterior instanceof Delta )
		{
			double[] support_point = ((Delta)x.posterior).get_support();
			if ( support_point.length == 1 && support_point[0] == value )
				return;
		}

		Delta delta;
		if ( x.type == Variable.VT_DISCRETE )
		{
			int[] support_point = new int[1];
			support_point[0] = (int)value;

			if ( x.distribution instanceof Discrete )
				delta = new DiscreteDelta( ((Discrete)x.distribution).dimensions, support_point );
			else if ( x.distribution instanceof ConditionalDiscrete )
				delta = new DiscreteDelta( ((ConditionalDiscrete)x.distribution).dimensions_child, support_point );
			else if ( x.states_names.size() > 0 && x.distribution.ndimensions_child() == 1 )
			{
				int[] dimension0 = new int[1];
				dimension0[0] = x.states_names.size();
				delta = new DiscreteDelta( dimension0, support_point );
			}
			else
				throw new RemoteException( "BeliefNetwork.assign_evidence: don't know how to assign to discrete variable "+x.get_fullname() );
		}
		else if ( x.type == Variable.VT_CONTINUOUS )
		{
			double[] support_point = new double[1];
			support_point[0] = value;
			delta = new GaussianDelta( support_point ); 
		}
		else
			throw new RemoteException( "BeliefNetwork.assign_evidence: don't know how to assign to "+x.get_fullname()+"; type: "+x.type );

		x.posterior = delta;
		x.pi = delta;
		x.lambda = delta;

		int i;

System.err.println( "BeliefNetwork.assign_evidence: tell parents of "+x.get_name() );
		x.notify_all_invalid_lambda_message();

System.err.println( "BeliefNetwork.assign_evidence: tell children of "+x.get_name() );
		x.notify_all_invalid_pi_message();

		x.notify_observers( "pi", x.pi );
		x.notify_observers( "lambda", x.lambda );
		x.notify_observers( "posterior", x.posterior );
	}

	public void get_all_lambda_messages( Variable x ) throws Exception
	{
		check_stale( "get_all_lambda_messages" );

		// Make sure that all requests are issued before any are processed in this VM.
		Thread.currentThread().setPriority( Thread.MAX_PRIORITY );

long t0 = System.currentTimeMillis();

		int i = 0, nmsg_requests = 0;
		LambdaMessageObserver lmo = new LambdaMessageObserver(x);

		while ( true )
		{
			AbstractVariable child = null;

			try 
			{
				for ( ; i < x.children.length; i++ )
				{
					child = x.children[i];
					if ( x.lambda_messages[i] == null )
					{
						child.get_bn().request_lambda_message( lmo, x, child );
						++nmsg_requests;
					}
					else // we have a lambda message; but check its validity.
						child.get_name(); // if this fails, remove the child.
				}

				break;
			}
			catch (RemoteException e)
			{
				// MAYBE I NEED TO BE ABLE TO DISTINGUISH FAILED CONNECTIONS FROM OTHER EXCEPTIONS ???
				x.remove_child( child );
			}
		}
long t1 = System.currentTimeMillis();
System.err.println( "get_all_lambda_messages: sent "+nmsg_requests+" requests for "+x.get_fullname()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
		Thread.currentThread().setPriority( Thread.NORM_PRIORITY );

t0 = System.currentTimeMillis();
		Thread lmo_msg_consumer = new MessageConsumer( lmo.lambda_messages_semaphore, nmsg_requests );
		lmo_msg_consumer.start();
		lmo_msg_consumer.join();
t1 = System.currentTimeMillis();
System.err.println( "get_all_lambda_messages: received "+nmsg_requests+" requests for "+x.get_fullname()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
		lmo.mark_stale();
	}

	/** Compute the prior of each parent of <tt>x</tt> and cache the priors
	  * with <tt>x</tt>. If the attempt to contact a parent fails, try to
	  * <tt>reconnect</tt> (q.v.).
	  */
	public void get_all_parents_priors( Variable x ) throws RemoteException
	{
		check_stale( "get_all_parents_priors" );

		for ( int i = 0; i < x.parents.length; i++ )
		{
			AbstractBeliefNetwork parent_bn;
			try { parent_bn = x.parents[i].get_bn(); }
			catch (RemoteException e)
			{
				x.reconnect_parent(i);
				parent_bn = x.parents[i].get_bn();
			}

			if ( x.parents_priors[i] == null )
				x.parents_priors[i] = parent_bn.get_prior( x.parents[i] );
		}
	}

	/** Compute a pi message to x from each parent of x. If the attempt
	  * to contact a parent fails, try to <tt>reconnect</tt> (q.v.),
	  * and if the reconnection fails, use the parent's prior.
	  */
	public void get_all_pi_messages( Variable x ) throws Exception
	{
		check_stale( "get_all_pi_messages" );
		PiMessageObserver pmo = new PiMessageObserver(x);
		int nmsg_requests = 0;

		// Make sure that all requests are issued before any are processed in this VM.
		Thread.currentThread().setPriority( Thread.MAX_PRIORITY );

long t0 = System.currentTimeMillis();
		for ( int i = 0; i < x.parents.length; i++ )
		{
			// We really only need the parent bn reference in case we need to
			// compute a pi message, but even if we don't need to, get one 
			// anyway, since we need to see if the parent is alive.

			AbstractBeliefNetwork parent_bn = null;

			try { parent_bn = x.parents[i].get_bn(); }
			catch (RemoteException e)
			{
System.err.println( "get_all_pi_messages: get_bn FAILED: "+e );
				try
				{
					x.reconnect_parent(i);
					parent_bn = x.parents[i].get_bn();
				}
				catch (RemoteException e2)
				{
System.err.println( "get_all_pi_messages: get_bn TWICE FAILED: "+e2 );
				}
			}

			if ( parent_bn == null )
{
System.err.println( "get_all_pi_messages: use prior for parent "+i+" of "+x.get_fullname() );
				x.pi_messages[i] = x.parents_priors[i];
}
			else if ( x.pi_messages[i] == null )
			{
				parent_bn.request_pi_message( pmo, x.parents[i], x );
				++nmsg_requests;
			}
			// else parent_bn is alive && pi mesg[i] is already computed; nothing to do.
		}
long t1 = System.currentTimeMillis();
System.err.println( "get_all_pi_messages: sent "+nmsg_requests+" requests for "+x.get_fullname()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
		Thread.currentThread().setPriority( Thread.NORM_PRIORITY );

t0 = System.currentTimeMillis();
		Thread pmo_msg_consumer = new MessageConsumer( pmo.pi_messages_semaphore, nmsg_requests );
		pmo_msg_consumer.start();
		pmo_msg_consumer.join();
t1 = System.currentTimeMillis();
System.err.println( "get_all_pi_messages: received "+nmsg_requests+" requests for "+x.get_fullname()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
		pmo.mark_stale();
	}

	/** Fire up a thread to carry out the lambda message computation, then
	  * return to the caller. The caller will be notified (via 
	  * <tt>RemoteObservable.notify_observers</tt>) when the message is ready.
	  */
	public void request_lambda_message( RemoteObserver observer, AbstractVariable parent, AbstractVariable child ) throws RemoteException
	{
		((RemoteObservable)child).add_observer( observer, "lambda-message-to["+parent.get_fullname()+"]" );
		(new LambdaMessageThread(this,parent,child)).start();
	}

	/** Fire up a thread to carry out the pi message computation, then
	  * return to the caller. The caller will be notified (via 
	  * <tt>RemoteObservable.notify_observers</tt>) when the message is ready.
	  */
	public void request_pi_message( RemoteObserver observer, AbstractVariable parent, AbstractVariable child ) throws RemoteException
	{
		((RemoteObservable)parent).add_observer( observer, "pi-message-to["+child.get_fullname()+"]" );
		(new PiMessageThread(this,parent,child)).start();
	}

	/** This method DOES NOT put the newly computed lambda message into the
	  * list of lambda messages for the <tt>parent</tt> variable.
	  */
	public Distribution compute_lambda_message( AbstractVariable parent, AbstractVariable child_in ) throws RemoteException
	{
		check_stale( "compute_lambda_message" );
		Variable child = to_Variable( child_in, "BeliefNetwork.compute_lambda_message" );

		// To compute a lambda message for a parent, we need to incorporate
		// lambda messages coming in from the children of the child, as well
		// as pi messages coming into the child from other parents.

		// HOWEVER, we can make a slight optimization -- recall that if all
		// incoming lambda messages are noninformative, then the outgoing
		// lambda message is also noninformative, and we can ignore any
		// incoming pi messages.
		
		try { if ( child.lambda == null ) compute_lambda( child ); }
		catch (Exception e)
		{
			e.printStackTrace();
			child.notify_observers( "lambda-message-to["+parent.get_fullname()+"]", null );
			throw new RemoteException( "compute_lambda_message: from: "+child.get_fullname()+" to: "+parent.get_fullname()+": "+e );
		}

		Distribution[] remaining_pi_messages = new Distribution[ child.parents.length ];

		if ( !(child.lambda instanceof Noninformative) )
		{
			// Lambda messages are informative -- need to take pi 
			// messages into account.

			for ( int i = 0; i < child.parents.length; i++ )
				if ( parent.equals( child.parents[i] ) )
					remaining_pi_messages[i] = null;
				else
				{
					// We need a pi message; check to see that the parent bn can
					// be contacted. If parent bn can't be contacted, use the parent 
					// variable's prior, otherwise compute a pi message (if needed).

					AbstractVariable a_parent = child.parents[i];
					AbstractBeliefNetwork parent_bn = null;

					try { parent_bn = a_parent.get_bn(); }
					catch (RemoteException e)
					{
System.err.println( "compute_lambda_message: get_bn FAILED: "+e );
						try
						{
							child.reconnect_parent(i);
							a_parent = child.parents[i];
							parent_bn = a_parent.get_bn();
						}
						catch (RemoteException e2)
						{
System.err.println( "compute_lambda_message: get_bn TWICE FAILED: "+e2 );
						}
					}

					if ( parent_bn == null )
{
System.err.println( "compute_lambda_message: use prior for parent "+i+" of "+child.get_fullname() );
						child.pi_messages[i] = child.parents_priors[i];
}
					else if ( child.pi_messages[i] == null )
					{
						child.pi_messages[i] = parent_bn.compute_pi_message( a_parent, child_in );
					}
					// else parent bn was contacted && pi mesg[i] was already computed; nothing to do.

					remaining_pi_messages[i] = child.pi_messages[i];
				}
		}

		// This call works fine if child.lambda is noninformative -- the
		// remaining_pi_messages array is full of nulls, but they're ignored.

		LambdaMessageHelper lmh = null;
		
		try { lmh = LambdaMessageHelperLoader.load_lambda_message_helper( child.distribution, child.lambda, remaining_pi_messages ); }
		catch (Exception e) { e.printStackTrace(); }

		if ( lmh == null )
		{
			child.notify_observers( "lambda-message-to["+parent.get_fullname()+"]", null );
			throw new RemoteException( "compute_lambda_message: attempt to load lambda helper class failed;\n\tparent: "+parent.get_name()+" child: "+child.get_name() );
		}

		Distribution lambda_message;
		
		try { lambda_message = lmh.compute_lambda_message( child.distribution, child.lambda, remaining_pi_messages ); }
		catch (Exception e)
		{
			e.printStackTrace();
			child.notify_observers( "lambda-message-to["+parent.get_fullname()+"]", null );
			throw new RemoteException( "compute_lambda_message: from: "+child.get_fullname()+" to: "+parent.get_fullname()+": "+e );
		}

System.err.println( "compute_lambda_message: from: "+child.get_name()+" to: "+parent.get_name()+" type: "+lambda_message.getClass()+" helper: "+lmh.getClass() );
		child.notify_observers( "lambda-message-to["+parent.get_fullname()+"]", lambda_message );
		return lambda_message;
	}

	/** This method DOES NOT put the newly computed pi message into the
	  * list of pi messages for the <tt>child</tt> variable.
	  */
	public Distribution compute_pi_message( AbstractVariable parent_in, AbstractVariable child ) throws RemoteException
	{
		check_stale( "compute_pi_message" );
		Variable parent = to_Variable( parent_in, "BeliefNetwork.compute_pi_message" );

		// To compute a pi message for the child, we need to incorporate
		// lambda messages from all children except for the one to which
		// we are sending the pi message.

		// HOWEVER, if this variable is evidence, then the pi message is
		// always a spike -- don't bother with incoming lambda messages.

		if ( parent.posterior instanceof Delta )
		{
System.err.println( "compute_pi_message: parent.posterior instanceof Delta; early return." );
			parent.notify_observers( "pi-message-to["+child.get_fullname()+"]", parent.posterior );
			return parent.posterior;
		}

		Distribution[] remaining_lambda_messages = new Distribution[ parent.children.length ];

		int i = 0;
		while ( true )
		{
			AbstractVariable a_child = null;

			try
			{
				for ( ; i < parent.children.length; i++ )
				{
					if ( child.equals( parent.children[i] ) )
						remaining_lambda_messages[i] = null;
					else
					{
						if ( parent.lambda_messages[i] == null )
						{
							a_child = parent.children[i];
							parent.lambda_messages[i] = a_child.get_bn().compute_lambda_message( parent_in, a_child );
						}
						remaining_lambda_messages[i] = parent.lambda_messages[i];
					}
				}

				break;
			}
			catch (RemoteException e)
			{
				// MAYBE I NEED TO BE ABLE TO DISTINGUISH FAILED CONNECTIONS FROM OTHER EXCEPTIONS ???
				parent.remove_child( a_child );
			}
		}

		try { if ( parent.pi == null ) compute_pi( parent ); }
		catch (Exception e)
		{
			e.printStackTrace();
			parent.notify_observers( "pi-message-to["+child.get_fullname()+"]", null );
			throw new RemoteException( "compute_pi_message: from: "+parent.get_fullname()+" to: "+child.get_fullname()+": "+e );
		}

		PiMessageHelper pmh = null;
		
		try { pmh = PiMessageHelperLoader.load_pi_message_helper( parent.pi, remaining_lambda_messages ); }
		catch (Exception e) { e.printStackTrace(); }

		if ( pmh == null ) 
		{
			parent.notify_observers( "pi-message-to["+child.get_fullname()+"]", null );
			throw new RemoteException( "compute_pi_message: attempt to load pi helper class failed; parent: "+parent.get_name()+" child: "+child.get_name() );
		}

		Distribution pi_message;
		
		try { pi_message = pmh.compute_pi_message( parent.pi, remaining_lambda_messages ); }
		catch (Exception e)
		{
			e.printStackTrace();
			parent.notify_observers( "pi-message-to["+child.get_fullname()+"]", null );
			throw new RemoteException( "compute_pi_message: from: "+parent.get_fullname()+" to: "+child.get_fullname()+": "+e );
		}

System.err.println( "compute_pi_message: from: "+parent.get_name()+" to: "+child.get_name()+" type: "+pi_message.getClass()+" helper: "+pmh.getClass() );
		parent.notify_observers( "pi-message-to["+child.get_fullname()+"]", pi_message );
		return pi_message;
	}

	/** This method DOES set lambda for the variable <tt>x</tt>.
	  */
	public Distribution compute_lambda( Variable x ) throws Exception
	{
		check_stale( "compute_lambda" );

		// Special case: if node x is an uninstantiated leaf, its lambda
		// is noninformative.
		if ( x.children.length == 0 )
		{
			x.lambda = new Noninformative();
			x.notify_observers( "lambda", x.lambda );
			return x.lambda;
		}

		// General case: collect lambda-messages from children, 
		// load lambda helper, and compute lambda.

		get_all_lambda_messages( x );

		LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( x.lambda_messages );
		if ( lh == null )
		{
			x.notify_observers( "lambda", null );
			throw new Exception( "compute_lambda: attempt to load lambda helper class failed; x: "+x.get_fullname() );
		}

		x.lambda = lh.compute_lambda( x.lambda_messages );

System.err.println( "compute_lambda: "+x.get_name()+" type: "+x.lambda.getClass()+" helper: "+lh.getClass() );
		x.notify_observers( "lambda", x.lambda );
		return x.lambda;
	}

	/** This method DOES set pi for the variable <tt>x</tt>.
	  */
	public Distribution compute_pi( Variable x ) throws Exception
	{
		check_stale( "compute_pi" );

		// General case: x is not a root node; collect pi-messages from parents,
		// then use x's distribution and those pi-messages to compute pi.
		// This also works when x is a root node -- in that case pi is just
		// the marginal distribution of x.

		get_all_pi_messages( x );

		PiHelper ph = PiHelperLoader.load_pi_helper( x.distribution, x.pi_messages );
		if ( ph == null ) 
		{
			x.notify_observers( "pi", null );
			throw new Exception( "compute_pi: attempt to load pi helper class failed; x: "+x.get_fullname() );
		}

		x.pi = ph.compute_pi( x.distribution, x.pi_messages );

System.err.println( "compute_pi: "+x.get_name()+" type: "+x.pi.getClass()+" helper: "+ph.getClass() );
		x.notify_observers( "pi", x.pi );
		return x.pi;
	}

	public Distribution compute_prior( Variable x ) throws Exception
	{
		check_stale( "compute_prior" );

		get_all_parents_priors(x);
		PiHelper ph = PiHelperLoader.load_pi_helper( x.distribution, x.parents_priors );
		if ( ph == null ) 
		{
			x.notify_observers( "prior", null );
			throw new Exception( "compute_prior: attempt to load pi helper class failed; x: "+x.get_fullname() );
		}

		x.prior = ph.compute_pi( x.distribution, x.parents_priors );

System.err.println( "compute_prior: "+x.get_name()+" type: "+x.prior.getClass()+" helper: "+ph.getClass() );
		x.notify_observers( "prior", x.prior );
		return x.prior;
	}

	public Distribution compute_posterior( Variable x ) throws Exception
	{
		check_stale( "compute_posterior" );

		// To compute the posterior for this variable, we need to compute
		// pi and lambda first. For pi, we need a pi-message from each parent
		// and the conditional distribution for this variable; for lambda,
		// we need a lambda-message from each child. Then the posterior is
		// just the product of pi and lambda, but it needs to be normalized.

		if ( x.pi == null ) compute_pi( x );
		if ( x.lambda == null ) compute_lambda( x );

		PosteriorHelper ph = PosteriorHelperLoader.load_posterior_helper( x.pi, x.lambda );
		if ( ph == null )
		{
			x.notify_observers( "posterior", null );
			throw new Exception( "compute_posterior: attempt to load posterior helper class failed; x: "+x.get_fullname() );
		}

		x.posterior = ph.compute_posterior( x.pi, x.lambda );

		// Now notify remote observers that we have computed a new posterior.
		// DO WE ALWAYS WANT THE NEXT TWO FUNCTION CALLS TOGETHER???

		x.notify_observers( "posterior", x.posterior );

System.err.println( "compute_posterior: "+x.get_name()+" type: "+x.posterior.getClass()+" helper: "+ph.getClass() );
		return x.posterior;
	}

	/** @throws IllegalArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( AbstractVariable x, AbstractVariable e ) throws RemoteException, IllegalArgumentException
	{
		check_stale( "compute_information" );
		throw new IllegalArgumentException("BeliefNetwork.compute_information: not yet.");
	}

	/** Retrieve a reference to the marginal prior for <tt>some_variable</tt>,
	  * that is, the posterior computed by ignoring any relevant evidence.
	  * If the prior is not yet computed, it is computed.
	  */
	public Distribution get_prior( AbstractVariable some_variable ) throws RemoteException
	{
		check_stale( "get_prior" );
		Variable x = to_Variable( some_variable, "BeliefNetwork.get_prior" );

		try
		{
			if ( x.prior == null )
				compute_prior(x);
			return x.prior;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "get_prior: "+x.get_fullname()+": "+e );
		}
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution get_posterior( AbstractVariable some_variable ) throws RemoteException
	{
		check_stale( "get_posterior" );
		Variable x = to_Variable( some_variable, "BeliefNetwork.get_posterior" );

		try
		{
			if ( x.posterior == null )
				compute_posterior(x);
			return x.posterior;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RemoteException( "get_posterior: "+x.get_fullname()+": "+e );
		}
	}

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution get_posterior( AbstractVariable[] x ) throws RemoteException
	{
		check_stale( "get_posterior" );
		throw new RemoteException( "BeliefNetwork.get_posterior: not implemented." );
	}

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		check_stale( "pretty_input" );

		st.nextToken();
		name = st.sval;

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "BeliefNetwork.pretty_input: input doesn't have opening bracket; parser state: "+st );

		for ( st.nextToken(); st.ttype != '}'; st.nextToken() )
		{
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				String variable_type = st.sval;
				Variable new_variable = null;

				try
				{
					Class variable_class = java.rmi.server.RMIClassLoader.loadClass( variable_type );
					new_variable = (Variable) variable_class.newInstance();
				}
				catch (Exception e)
				{
					throw new IOException("BeliefNetwork.pretty_input: can't create an object of type "+variable_type );
				}

				new_variable.belief_network = this;
				new_variable.pretty_input(st);
				variables.put( new_variable.name, new_variable );
			}
			else
			{
				throw new IOException( "BeliefNetwork.pretty_input: unexpected token: "+st );
			}
		}

		try { assign_references(); }
		catch (UnknownParentException e)
		{
			throw new IOException( "BeliefNetwork.pretty_input: attempt to read belief network failed:\n"+e );
		}
	}

	/** Parse a string containing a description of a belief network. The description
	  * is contained within curly braces, which are included in the string.
	  * The content in curly braces is preceded by the name of the belief network.
	  */
	public void parse_string( String description ) throws IOException, RemoteException
	{
		check_stale( "parse_string" );
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this belief network as a string. 
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  */
	public String format_string() throws RemoteException
	{
		check_stale( "format_string" );

		String result = "";
		result += this.getClass().getName()+" "+name+"\n"+"{"+"\n";

		for ( Enumeration enum = variables.elements(); enum.hasMoreElements(); )
		{
			AbstractVariable x = (AbstractVariable) enum.nextElement();
			result += x.format_string( "\t" );
		}

		result += "}"+"\n";
		return result;
	}

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public void pretty_output( OutputStream os ) throws IOException
	{
		check_stale( "pretty_output" );
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string() );
	}

	/** Write a description of this belief network to a string.
	  * using the format required by the "dot" program. All the probabilistic
	  * information is thrown away; only the names of the variables and
	  * the parent-to-child relations are kept.
	  * See <a href="http://www.research.att.com/sw/tools/graphviz/">
	  * the Graphviz homepage</a> for information about "dot" and other
	  * graph visualization software.
	  *
	  * @return A string containing the belief network in "dot" format.
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public String dot_format() throws RemoteException
	{
		check_stale( "dot_format" );

		// First find the list of all belief networks upstream of this one.

		Vector bn_list = new Vector();
		Vector lost_parents = new Vector();
		upstream_recursion( this, bn_list, lost_parents );

		String result = "";
		result += "digraph \""+get_fullname()+"\" {\n";

		// Put in the list of parents whose connections we've lost.
		// These won't be in any subgraph.

		for ( Enumeration enum = lost_parents.elements(); enum.hasMoreElements(); )
		{
			String s = (String) enum.nextElement();
			NameInfo ni = NameInfo.parse_variable( s, null );
			result += "\""+s+"\" [ color=yellow, label=\""+ni.host_name+":"+ni.rmi_port+"/\\n"+ni.beliefnetwork_name+".\\n"+ni.variable_name+"\" ];\n";
		}

		// Now print out a description of each belief network.
		for ( Enumeration enum = bn_list.elements(); enum.hasMoreElements(); )
			result += one_dot_format( (AbstractBeliefNetwork)enum.nextElement() );

		result += "}\n";
		return result;
	}

	static void upstream_recursion( AbstractBeliefNetwork bn, Vector bn_list, Vector lost_parents ) throws RemoteException
	{
		if ( bn == null ) return;

		if ( ! bn_list.contains( bn ) )
		{
			bn_list.addElement( bn );

			AbstractVariable[] variables = bn.get_variables();
			for ( int i = 0; i < variables.length; i++ )
			{
				AbstractVariable[] parents = variables[i].get_parents();
				String[] parents_names = variables[i].get_parents_names();

				for ( int j = 0; j < parents.length; j++ )
				{
					AbstractBeliefNetwork parent_bn;
					try { parent_bn = parents[j].get_bn(); }
					catch (RemoteException e)
					{
						parent_bn = null;
						lost_parents.addElement( parents_names[j] );
					}

					upstream_recursion( parent_bn, bn_list, lost_parents );
				}
			}
		}
	}

	static String one_dot_format( AbstractBeliefNetwork bn ) throws RemoteException
	{
		int i, j;
		String result = "";
		AbstractVariable[] variables = bn.get_variables();

		Vector invisibly_linked = new Vector();

		String bn_ihigh = "\"invis-"+bn.get_fullname()+"-high\"";
		String bn_ilow = "\"invis-"+bn.get_fullname()+"-low\"";

		result += "  "+bn_ihigh+" [style=invis, width=\"0.1\", height=\"0.1\", label=\"\"];\n";
		result += "  "+bn_ilow+" [style=invis, width=\"0.1\", height=\"0.1\", label=\"\"];\n";

		// Print the name of each variable with a shorter label.
		// Put in link to each variable from each of its parents.
		// For each variable without parents in bn, put in an invisible link from "invis-*-high";
		// for each variable without chilren in bn, put in an invisible link to "invis-*-low".
		// Note each belief network which contains a parent of some variable in bn.

		for ( i = 0; i < variables.length; i++ )
		{
			AbstractVariable x = variables[i];
			result += "  \""+x.get_fullname()+"\" [ label=\""+x.get_name()+"\"";
			if ( x.get_posterior() instanceof Delta )
				// This node is an evidence node, so color it differently.
				result += ", color=gray92, style=filled";
			result += " ];\n";

			AbstractVariable[] parents = x.get_parents(), children = x.get_children();
			String[] parents_names = x.get_parents_names();
				
			boolean local_root = true, local_leaf = true;

			for ( j = 0; j < parents.length; j++ )
			{
				String parent_name;
				try { parent_name = parents[j].get_fullname(); }
				catch (RemoteException e) { parent_name = parents_names[j]; }
				result += "  \""+parent_name+"\"->\""+x.get_fullname()+"\";\n";

				AbstractBeliefNetwork parent_bn = null;
				try { parent_bn = parents[j].get_bn(); }
				catch (RemoteException e) {}

				if ( parent_bn == null )
				{
					// NEED TO ADD INVISIBLE LINKING HERE !!!
				}
				else
				{
					if ( ! parent_bn.equals(bn) && ! invisibly_linked.contains( parent_bn ) )
						invisibly_linked.addElement( parent_bn );

					if ( parent_bn.equals(bn) )
						local_root = false;
				}
			}

			j = 0;
			while ( true )
			{
				AbstractVariable child = null;

				try
				{
					for ( ; j < children.length; j++ )
					{
						child = children[j];
						if ( child.get_bn().equals(bn) )
							local_leaf = false;
					}

					break;
				}
				catch (RemoteException e)
				{
					// MAYBE I NEED TO BE ABLE TO DISTINGUISH FAILED CONNECTIONS FROM OTHER EXCEPTIONS ???
					// x.remove_child( child );	// NEEDS LOCAL REFERENCE FOR THIS !!!
					System.err.println( "BeliefNetwork.one_dot_format: leaf-finding failed; stumble forward: "+e );
				}
			}

			if ( local_root )
				result += "  "+bn_ihigh+" -> \""+x.get_fullname()+"\" [style=invis];\n";

			if ( local_leaf )
				result += "  \""+x.get_fullname()+"\" -> "+bn_ilow+" [style=invis];\n";
		}

		for ( i = 0; i < invisibly_linked.size(); i++ )
		{
			AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) invisibly_linked.elementAt(i);
			String parent_ilow = "\"invis-"+parent_bn.get_fullname()+"-low\"";
			result += "  "+parent_ilow+" -> "+bn_ihigh+" [style=invis];\n";
		}

		// The variables in bn all go into a cluster labeled with bn's name.

		result += "  subgraph \"cluster_"+bn.get_fullname()+"\" {\n";
		result += "    label = \""+bn.get_name()+"\";\n";
		result += "    color = gray;\n";

		for ( i = 0; i < variables.length; i++ )
		{
			AbstractVariable x = variables[i];
			result += "    \""+x.get_fullname()+"\";\n";
		}

		result += "  }\n";
		return result;
	}

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public Remote name_lookup( String some_name ) throws RemoteException
	{
		check_stale( "name_lookup" );
		return (Remote) variables.get(some_name);
	}

	/** Add a variable to the belief network. A new object of type
	  * <tt>Variable</tt> is created if the argument <tt>new_variable</tt>
	  * is <tt>null</tt>, otherwise <tt>new_variable</tt> is used.
	  * A caller that wants to construct a belief network out of variables
	  * more complicated than <tt>Variable</tt> should allocate a new
	  * instance of the desired class (which must be derived from
	  * <tt>Variable</tt>) and pass that in. Parents can be linked to the
	  * new variable by <tt>Variable.add_parent</tt>. 
	  *
	  * @param name Name of the new variable.
	  * @param new_variable Reference to variable to add to belief network;
	  *   if <tt>null</tt>, then a new <tt>Variable</tt> object is allocated.
	  * @return A reference to the new variable. If the attempt to create
	  *   a new variable fails, returns <tt>null</tt>.
	  * @see Variable.add_parent
	  */
	public AbstractVariable add_variable( String name_in, AbstractVariable new_variable ) throws RemoteException
	{
		check_stale( "add_variable" );
		Variable x = (Variable) new_variable;

		if ( x == null )
		{
			try { x = new Variable(); }
			catch (RemoteException e) { return null; }
		}

		x.name = name_in;
		x.belief_network = this;

		variables.put( x.name, x );
		return x;
	}

	/** Assign references to parents and children. This is usually called
	  * once, just after initialization, but it could be called more than
	  * once. For example, if one edits the belief network via
	  * <tt>add_variable</tt> and <tt>remove_variable</tt>, this function
	  * should be called to update the references after editing.
	  * @throws UnknownParentException If a reference for some parent
	  *   referred to by a variable in this network cannot be obtained.
	  */
	void assign_references() throws RemoteException
	{
		check_stale( "assign_references" );
		if ( variables.size() == 0 )
			// Empty network -- no references to assign.
			return;

		try { locate_references(); }
		catch (UnknownNetworkException e)
		{
			throw new UnknownParentException( "BeliefNetwork.assign_references: some referred-to network can't be located:\n"+e );
		}

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			Variable x = (Variable) enumv.nextElement();
			x.parents = new AbstractVariable[ x.parents_names.size() ];
			x.pi_messages = new Distribution[ x.parents_names.size() ];
			x.parents_priors = new Distribution[ x.parents_names.size() ];

			for ( int i = 0; i < x.parents_names.size(); i++ )
			{
				String parent_name = (String) x.parents_names.elementAt(i);
				NameInfo ni = NameInfo.parse_variable( parent_name, belief_network_context );

				if ( ni.beliefnetwork_name != null )
				{
					// Parent is in some other belief network -- first get a reference
					// to the other belief network, then get a reference to the parent
					// variable within the other network.

					try 
					{
						AbstractBeliefNetwork parent_bn = (AbstractBeliefNetwork) belief_network_context.get_reference(ni);
System.err.println( "BeliefNetwork.assign_references: parent_name: "+parent_name+"; parent_bn is "+(parent_bn==null?"null":"NOT null") );
						AbstractVariable p = (AbstractVariable) parent_bn.name_lookup( ni.variable_name );
						x.parents[i] = p;	// p could be null here
						if ( p != null )
						{
							p.add_child( x );
							x.parents_priors[i] = parent_bn.get_prior(p);
						}
					}
					catch (Exception e)
					{
						System.err.println( "BeliefNetwork.assign_references: failed attempt to look up: "+parent_name );
						System.err.println( "  exception: "+e );
					}
				}
				else
				{
					// Parent is within this belief network.

					try
					{
						Variable p = (Variable) name_lookup(parent_name);
						x.parents[i] = p;	// p could be null here
						if ( p != null ) p.add_child( x );
						// Don't bother storing a prior; connection never fails.
					}
					catch (RemoteException e)
					{
						// Should never happen, as the parent is local; what to do???
						System.err.println( "BeliefNetwork.assign_references: failed attempt to look up: "+parent_name );
						System.err.println( "  unexpected RemoteException"+e );
					}
				}
			}
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
	void locate_references() throws RemoteException
	{
		check_stale( "locate_references" );
		if ( variables.size() == 0 )
			// Empty network; no references to locate.
			return;

		String missing = "";

		for ( Enumeration enumv = variables.elements(); enumv.hasMoreElements(); )
		{
			// DOES THIS WORK ??? IT SHOULD SINCE WE ARE WORKING W/ LOCALS !!!
			Variable x = (Variable) enumv.nextElement();

			for ( int i = 0; i < x.parents_names.size(); i++ )
			{
				String parent_name = (String) x.parents_names.elementAt(i);
				NameInfo ni = NameInfo.parse_variable( parent_name, belief_network_context );

				if ( ni.beliefnetwork_name != null )
				{
					Remote bn = null;
					try { bn = belief_network_context.get_reference(ni); }
					catch (RemoteException e)
					{
						System.err.println( "BeliefNetwork.locate_references: get_reference failed; stagger forward. Exception: "+e );
					}

					if ( bn == null )
					{
						// The parent bn is not running yet; find a context into
						// which we can load it, and then load it.

						try
						{
							AbstractBeliefNetworkContext bnc = x.locate_context(ni);
							AbstractBeliefNetwork parent_bn = bnc.load_network( ni.beliefnetwork_name );
							bnc.bind( parent_bn );
						}
						catch (Exception e)
						{
							missing += parent_name+" ";
						}
					}
				}
			}
		}
// System.err.println( "BeliefNetwork.locate_references: reference table: " );
// System.err.println( "  "+belief_network_context.reference_table );
		if ( ! missing.equals("") )
			throw new UnknownNetworkException( "BeliefNetwork.locate_references: can't find: "+missing );
	}

	/** In order to work with instance data, we need to have a class
	  * reference, not an interface reference. This is much simpler than
	  * writing get/set methods for every datum, although it does limit
	  * computations to local variables.
	  */
	protected Variable to_Variable( AbstractVariable x, String msg_leader ) throws RemoteException
	{
		check_stale( "to_Variable" );

		try { if ( x instanceof Variable ) return (Variable) x; }
		catch (ClassCastException e)
		{
			e.printStackTrace();
			throw new RemoteException( "to_Variable: BIZARRE: "+e );
		}

		// If x is a variable in this belief network, return a local reference.
		try
		{
			AbstractVariable xref = (AbstractVariable) name_lookup( x.get_name() );
			if ( xref != null )
				return (Variable) xref;
			else
				throw new RemoteException( msg_leader+": "+x.get_name()+" has a null reference in "+get_fullname()+"; can't convert to local variable." );
		}
		catch (Exception e)
		{
			throw new RemoteException( msg_leader+": "+x.get_name()+" isn't on the list of names in "+get_fullname()+"; can't convert to local variable." );
		}
	}
}

class LambdaMessageThread extends Thread
{
	BeliefNetwork belief_network;
	AbstractVariable parent, child;
	
	LambdaMessageThread( BeliefNetwork bn_in, AbstractVariable parent_in, AbstractVariable child_in )
	{
		belief_network = bn_in;
		parent = parent_in;
		child = child_in;
	}

	public void run()
	{
long t0 = System.currentTimeMillis();
		try { belief_network.compute_lambda_message( parent, child ); }
		catch (RemoteException e)
		{
			System.err.println( "LambdaMessageThread: failed: " );
			e.printStackTrace();
			try { ((RemoteObservable)child).notify_observers( "lambda-message-to["+parent.get_fullname()+"]", null ); }
			catch (RemoteException e2) {}
		}

long tf = System.currentTimeMillis();
try { System.err.println( "LambdaMessageThread: complete message from "+child.get_fullname()+" to "+parent.get_fullname()+"; time elapsed: "+((tf-t0)/1000.0)+" [s]" ); }
catch (RemoteException e) { System.err.println( "LambdaMessageThread: complete message; time elapsed: "+((tf-t0)/1000.0)+" [s]" ); }
	}
}

class PiMessageThread extends Thread
{
	BeliefNetwork belief_network;
	AbstractVariable parent, child;

	PiMessageThread( BeliefNetwork bn_in, AbstractVariable parent_in, AbstractVariable child_in )
	{
		belief_network = bn_in;
		parent = parent_in;
		child = child_in;
	}

	public void run()
	{
long t0 = System.currentTimeMillis();
		try { belief_network.compute_pi_message( parent, child ); }
		catch (RemoteException e)
		{
			System.err.println( "PiMessageThread: failed: " );
			e.printStackTrace();
			try { ((RemoteObservable)parent).notify_observers( "pi-message-to["+child.get_fullname()+"]", null ); }
			catch (RemoteException e2) {}
		}

long tf = System.currentTimeMillis();
try { System.err.println( "PiMessageThread: complete message from "+parent.get_fullname()+" to "+child.get_fullname()+"; time elapsed: "+((tf-t0)/1000.0)+" [s]" ); }
catch (RemoteException e) { System.err.println( "PiMessageThread: complete message; time elapsed: "+((tf-t0)/1000.0)+" [s]" ); }
	}
}

class LambdaMessageObserver extends RemoteObserverImpl
{
	Variable x;
	Semaphore lambda_messages_semaphore = new Semaphore(0);
	boolean stale = false;

	LambdaMessageObserver( Variable x_in ) throws RemoteException { x = x_in; }

	public void mark_stale() { stale = true; }

	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		if ( stale ) throw new RemoteException( "LambdaMessageObserver: stale." );

		boolean found = false;
System.err.println( "LambdaMessageObserver: update for "+x.get_fullname()+" from "+((AbstractVariable)o).get_fullname()+", type: "+arg.getClass().getName() );
		for ( int i = 0; i < x.children.length; i++ )
		{
			if ( x.children[i].equals(o) )
			{
				x.lambda_messages[i] = (Distribution) arg;
				lambda_messages_semaphore.V();
				found = true;
				break;
			}
		}

		if ( !found ) throw new RemoteException( "LambdaMessageObserver.update: child "+((AbstractVariable)o).get_fullname()+" not found." );
	}
}

class PiMessageObserver extends RemoteObserverImpl
{
	Variable x;
	Semaphore pi_messages_semaphore = new Semaphore(0);
	boolean stale = false;

	PiMessageObserver( Variable x_in ) throws RemoteException { x = x_in; }

	public void mark_stale() { stale = true; }

	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		if ( stale ) throw new RemoteException( "PiMessageObserver: stale." );

		boolean found = false;
System.err.println( "PiMessageObserver: update for "+x.get_fullname()+" from "+((AbstractVariable)o).get_fullname()+", type: "+arg.getClass().getName() );
		for ( int i = 0; i < x.parents.length; i++ )
		{
			if ( x.parents[i].equals(o) )
			{
				x.pi_messages[i] = (Distribution) arg;
				pi_messages_semaphore.V();
				found = true;
				break;
			}
		}

		if ( !found ) throw new RemoteException( "PiMessageObserver.update: parent "+((AbstractVariable)o).get_fullname()+" not found." );
	}
}

class MessageConsumer extends Thread
{
	int nmsgs;
	Semaphore semaphore;

	MessageConsumer( Semaphore s, int n ) { semaphore = s; nmsgs = n; }

	public void run()
	{
		for ( int i = 0; i < nmsgs; i++ )
		{
			semaphore.P();
		}
	}
}
