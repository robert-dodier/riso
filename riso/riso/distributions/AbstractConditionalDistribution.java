package riso.distributions;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import riso.belief_nets.*;

/** Abstract base class for conditional distributions.
  * This class implements only a few methods; most of the methods from the
  * <tt>ConditionalDistribution</tt> interface are not implemented,
  * and so must be provided by subclasses.
  *
  * <p> This classs is helpful in part because message-passing algorithms can be
  * formulated as generic for all conditional distributions -- handlers
  * are named only by classes, not by interfaces.
  */
public abstract class AbstractConditionalDistribution implements ConditionalDistribution, Serializable
{
	/** This conditional distribution is associated with the belief network variable <tt>associated_variable</tt>.
	  * This reference is necessary for some distributions, and generally useful for debugging.
	  */
	public AbstractVariable associated_variable;

	/** Cache a reference to the variable with which this conditional distribution
	  * is associated.
	  */
	public void set_variable( Variable x ) { associated_variable = x; }

	/** Return a copy of this conditional distribution.
	  */
	public abstract Object clone() throws CloneNotSupportedException;
}
