package riso.distributions.computes_posterior;
import riso.distributions.*;
import riso.approximation.*;
import TopDownSplayTree;
import SeqTriple;

public class AbstractDistribution_AbstractDistribution extends AbstractPosteriorHelper
{
	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>AbstractDistribution</tt>.
	  */
	public SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 2 );
		description_array = s;
	}

	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		Distribution[] p_l = new Distribution[2];
		p_l[0] = pi;
		p_l[1] = lambda;
		DistributionProduct dp = new DistributionProduct( false, pi instanceof Discrete,  p_l );

		MixGaussians q = dp.initial_mix( pi.effective_support( 1e-6 ) );
		double tolerance = 1e-5;
		q = GaussianMixApproximation.do_approximation( (Distribution)dp, q, dp.support, tolerance );

// System.err.println( "compute_posterior: lambda class: "+lambda.getClass() );
// if ( lambda instanceof riso.distributions.computes_lambda_message.IntegralCache ) {
// double[][] xy = ((riso.distributions.computes_lambda_message.IntegralCache)lambda).cache.dump();
// double[] x = new double[1];
// System.err.println( "dump: (lambda eval at) (lambda eval) (dist prod eval) (approx eval)" );
// for ( int i = 0; i < xy.length; i++ ) {
// x[0] = xy[i][0];
// System.err.println( xy[i][0]+"\t"+xy[i][1]+"\t"+dp.p(x)+"\t"+q.p(x) ); } }
		if ( q.ncomponents() == 1 )
			return q.components[0];
		else
			return q;
	}
}
