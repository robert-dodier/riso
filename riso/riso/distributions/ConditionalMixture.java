package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** An instance of this class represents a conditional mixture model.
  * This is similar to an unconditional mixture (represented by the
  * <tt>Mixture</tt> class), but the mixing coefficients can vary with
  * the context (i.e., the parent variables). In the <tt>Mixture</tt> class,
  * mixing coefficients are stored in an array, since they don't change;
  * here, each mixing coefficient is returned by a function that takes the
  * context as an argument. In addition, the mixture components are
  * conditional distributions, not unconditional.
  *
  * <p> This class is declared <tt>abstract</tt> (i.e., it cannot be
  * instantiated) because there is no generic way to compute the mixing
  * coefficient function; each derived class implements that in its own way.
  */
public abstract class ConditionalMixture extends AbstractConditionalDistribution
{
	/** This array contains a reference to each mixture component.
	  * Each component is a conditional distribution.
	  */
	public ConditionalDistribution[] components;

	/** Returns the mixing coefficient for the <tt>i</tt>'th component,
	  * given the parent context <tt>c</tt>.
	  */
	public double mixing_coefficient( int i, double[] c );
}