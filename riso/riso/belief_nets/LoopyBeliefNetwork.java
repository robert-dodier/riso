/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2004, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
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
import java.util.*;
import riso.distributions.*;
import riso.remote_data.*;

/** An instance of this class represents a belief network,
  * with methods to implement loopy belief propagation.
  * The representation is the same as for the superclass,
  * but the methods are different.
  */
public class LoopyBeliefNetwork extends BeliefNetwork
{
	/** Create an empty object of this type. 
	  */
	public LoopyBeliefNetwork() throws RemoteException {}

	/** Clear the posterior of <tt>some_variable</tt> but do not recompute it. This method also
	  * clears the pi and lambda for this variable. Notify remote observers
	  * that the posterior for this variable is no longer known (if it ever was).
      *
      * Do not bother to tell parents and children that lambda and pi
      * messages originating from this variable are now invalid.
	  */
	public void clear_posterior( AbstractVariable some_variable ) throws RemoteException
	{
		check_stale( "clear_posterior" );
		Variable x = to_Variable( some_variable, "LoopyBeliefNetwork.clear_posterior" );

		x.pi = null;
		x.lambda = null;
		x.posterior = null;

		x.notify_observers( "pi", x.pi );
		x.notify_observers( "lambda", x.lambda );
		x.notify_observers( "posterior", x.posterior );
	}

	public void assign_evidence( AbstractVariable some_variable, double value ) throws RemoteException
	{
		check_stale( "assign_evidence" );
		Variable x = to_Variable( some_variable, "LoopyBeliefNetwork.assign_evidence" );

		// If this variable is evidence, and the evidence is the same, then do nothing.

		if ( x.posterior instanceof Delta )
		{
			double[] support_point = ((Delta)x.posterior).get_support();
			if ( support_point.length == 1 && support_point[0] == value )
				return;
		}

		Delta delta = null;

        boolean treat_as_continuous = false;

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
            {
				System.err.println ("LoopyBeliefNetwork.assign_evidence: can't tell how to assign to discrete variable "+x.get_fullname()+"; treat as continuous and hope for the best.");
                treat_as_continuous = true;
            }
		}

        if (x.type == Variable.VT_NONE)
        {
            System.err.println ("LoopyBeliefNetwork.assign_evidence: type not specified for "+x.get_fullname()+"; treat as continuous and hope for the best.");
            treat_as_continuous = true;
        }

		if (x.type == Variable.VT_CONTINUOUS || treat_as_continuous)
		{
			double[] support_point = new double[1];
			support_point[0] = value;
			delta = new GaussianDelta( support_point ); 
		}

		x.posterior = delta;
		x.pi = delta;
		x.lambda = delta;

		x.notify_observers( "pi", x.pi );
		x.notify_observers( "lambda", x.lambda );
		x.notify_observers( "posterior", x.posterior );
	}

	// IMPLEMENT THIS EVENTUALLY, FOR NOW JUST REIMPLEMENT get_all_lambda_messages_local !!!
    // public void get_all_lambda_messages( Variable x ) throws Exception

	// IMPLEMENT THIS EVENTUALLY, FOR NOW JUST REIMPLEMENT get_all_pi_messages_local !!!
    // public void get_all_pi_messages( Variable x ) throws Exception

    public void initialize_messages ()
    {
        Noninformative n = new Noninformative ();

		for (Enumeration e = variables.elements(); e.hasMoreElements();)
        {
            AbstractVariable x = (AbstractVariable) e.nextElement();

            try
            {
                // assign_evidence sets pi, lambda, and posterior
                // NEXT LINE WON'T WORK BECAUSE random IS NOT A METHOD OF ConditionalDistribution !!!
                // assign_evidence (variables[i], variables[i].get_distribution().random());
                // ASSUME 0 IS IN SUPPORT OF CONDITIONAL DISTRIBUTION !!!
                assign_evidence (x, 0);
            }
            catch (Exception ex)
            {
                System.err.println ("LoopyBeliefNetwork.initialize_messages: oops: "+ex+"; stagger forward.");
            }
        }

		for (Enumeration e = variables.elements(); e.hasMoreElements();)
        {
            AbstractVariable x = (AbstractVariable) e.nextElement();

            try
            {
                AbstractVariable[] children = x.get_children ();
                AbstractVariable[] parents  = x.get_parents ();
                // WILL get_lambda_messages, get_pi_messages WORK AS EXPECTED IF x IS REMOTE ???
                Distribution[] lambda_messages = x.get_lambda_messages ();
                Distribution[] pi_messages     = x.get_pi_messages ();

                for (int j = 0; j < children.length; j++)
                    lambda_messages[j] = n;

                for (int j = 0; j < parents.length; j++)
                    pi_messages[j] = (Distribution) parents[j].get_pi().clone();
            }
            catch (Exception ex)
            {
                System.err.println ("LoopyBeliefNetwork.initialize_messages: oops: "+ex+"; stagger forward.");
            }
        }
    }
}
