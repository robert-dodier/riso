package riso.distributions.computes_posterior;
import riso.distributions.Discrete;
import riso.distributions.*;
import riso.distributions.computes_lambda.*;
import SeqTriple;

public class Discrete_AbstractDistribution extends AbstractPosteriorHelper
{
	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, one <tt>Discrete</tt> and 
	  * one <tt>AbstractDistribution</tt>.
	  */
	public SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Discrete", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		description_array = s;
	}

	public Distribution compute_posterior( Distribution pi_in, Distribution lambda ) throws Exception
	{
		Discrete p = new Discrete(), pi = (Discrete) pi_in;
		p.ndims = pi.ndims;
		p.dimensions = (int[]) pi.dimensions.clone();
		p.probabilities = new double[ pi.probabilities.length ];
		
		if ( pi.ndims > 1 ) throw new IllegalArgumentException( "compute_posterior: can't handle #dimensions: "+pi.ndims );
		double[] x = new double[1];
		double sum = 0;

		for ( int i = 0; i < pi.probabilities.length; i++ )
		{
			x[0] = i;
			double pp = pi.probabilities[i] * lambda.p( x );
			p.probabilities[i] = pp;
			sum += pp;
		}

		for ( int i = 0; i < p.probabilities.length; i++ )
			p.probabilities[i] /= sum;

		return p;
	}
}
