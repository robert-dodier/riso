package riso.distributions.computes_lambda;
import java.util.*;
import riso.distributions.*;
import SeqTriple;

public class Discrete implements LambdaHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any number of <tt>Discrete</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Discrete", -1 );
		return s;
	}

	/** Compute the likelihood function for a variable. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public riso.distributions.Distribution compute_lambda( riso.distributions.Distribution[] lambda_messages ) throws Exception
	{
		int i, j;

		// Some of the lambda messages may be null or noninformative; skip over those.

		riso.distributions.Discrete p = null;
		boolean first_informative = true;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] == null || lambda_messages[i] instanceof Noninformative )
				continue;

			if ( first_informative )
			{
				p = (riso.distributions.Discrete) lambda_messages[i].remote_clone();
				first_informative = false;
				continue;
			}

			riso.distributions.Discrete q = (riso.distributions.Discrete) lambda_messages[i];
			if ( q.probabilities.length != p.probabilities.length )
				throw new IllegalArgumentException( "computes_lambda.Discrete.compute_lambda: some lambda messages have different lengths." );

			for ( j = 0; j < p.probabilities.length; j++ )
				p.probabilities[j] *= q.probabilities[j];
		}

		// Strictly speaking, we don't have to normalize the result,
		// but this code may be called from someplace that needs it normalized.

		double sum = 0;
		for ( i = 0; i < p.probabilities.length; i++ ) sum += p.probabilities[i];
		for ( i = 0; i < p.probabilities.length; i++ ) p.probabilities[i] /= sum;
		
		return p;
	}
}
