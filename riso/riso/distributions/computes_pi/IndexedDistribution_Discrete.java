package riso.distributions.computes_pi;
import java.util.*;
import riso.distributions.*;

/** @see PiHelper
  */
public class IndexedDistribution_Discrete implements PiHelper
{
	/** All incoming pi-messages are discrete, so this node must not have any
	  * continuous parents. Thus the pi distribution for this node is simply
	  * a mixture of the components, with the mixture coefficients equal to
	  * the product of the probabilities in the pi-messages. Compute the
	  * mixture coefficients first, and count how many are non-zero; then 
	  * create a mixture with that many components. As an important special
	  * case, a pi-message may represent evidence, and so all of the elements
	  * in its probability vector will be zero, except for one element.
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		IndexedDistribution py = (IndexedDistribution) py_in;

		int m = 0;
		int[] ii = new int[ pi_messages.length ];
		int nnonzero = count_nonzero( pi_messages, ii, m );

		Mixture pye = new Mixture( 1, nnonzero );
		ii = new int[ pi_messages.length ];
		int[] nmix = new int[1], npy = new int[1];
		assign_components( pi_messages, ii, m, py, pye, nmix, npy );

		if ( nnonzero == 1 ) return pye.components[0];
		else return pye;
	}

	int count_nonzero( Distribution[] pi_messages, int[] ii,  int m )
	{
		if ( m == pi_messages.length )
		{
			for ( int i = 0; i < pi_messages.length; i++ )
			{
				Discrete pimsg = (Discrete) pi_messages[i];
				if ( pimsg.probabilities[ ii[i] ] == 0 )
					return 0;
			}
			return 1;
		}
		else
		{
			int sum = 0;
			Discrete pimsg = (Discrete) pi_messages[m];
			for ( ii[m] = 0; ii[m] < pimsg.probabilities.length; ii[m]++ )
				sum += count_nonzero( pi_messages, ii, m+1 );
			return sum;
		}
	}

	void assign_components( Distribution[] pi_messages, int[] ii, int m, IndexedDistribution py, Mixture pye, int[] nmix, int[] npy ) throws Exception
	{
		if ( m == pi_messages.length )
		{
			double alpha = 1;
			for ( int i = 0; i < pi_messages.length; i++ )
			{
				Discrete pimsg = (Discrete) pi_messages[i];
				alpha *= pimsg.probabilities[ ii[i] ];
			}
			
			++npy[0];
			if ( alpha == 0 ) return;

			pye.components[nmix[0]] = (Distribution) py.components[npy[0]].remote_clone();
			pye.mix_proportions[nmix[0]] = alpha;
			++nmix[0];
		}
		else
		{
			Discrete pimsg = (Discrete) pi_messages[m];
			for ( ii[m] = 0; ii[m] < pimsg.probabilities.length; ii[m]++ )
				assign_components( pi_messages, ii, m+1, py, pye, nmix, npy );
		}
	}
}
