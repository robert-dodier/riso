package riso.distributions.computes_pi;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import numerical.*;
import TopDownSplayTree;

/** @see PiHelper
 */
public class Classifier_AbstractDistribution implements PiHelper
{
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
