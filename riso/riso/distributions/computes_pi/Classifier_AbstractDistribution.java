package riso.distributions.computes_pi;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import numerical.*;
import TopDownSplayTree;
import SeqTriple;

/** @see PiHelper
 */
public class Classifier_AbstractDistribution implements PiHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Classifier</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	public SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Classifier", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi_messages ) throws Exception
	{
		IntegralCache integral_cache = new IntegralCache( pxu, pi_messages );

		int[] dimensions = new int[1], ii = new int[1];
		dimensions[0] = ((Classifier)pxu).ncategories();
		Discrete dd = new Discrete( dimensions );

		for ( int i = 0; i < dimensions[0]; i++ )
		{
			ii[0] = i;
			double q = integral_cache.f(i);
			dd.assign_p( ii, q );
		}

		dd.normalize_p();
		return dd;
	}
}
