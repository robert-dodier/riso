package riso.distributions.computes_posterior;
import riso.distributions.*;
import riso.distributions.computes_lambda.*;
import SeqTriple;

public class Discrete_Discrete extends AbstractPosteriorHelper
{
	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Discrete", 2 );
		description_array = s;
	}

	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		Distribution[] p = new Distribution[2];
		p[0] = pi;
		p[1] = lambda;

		return (new riso.distributions.computes_lambda.Discrete()).compute_lambda( p );
	}
}
