package riso.distributions.computes_lambda_message;
import java.util.*;
import riso.distributions.*;
import SeqTriple;

public class IndexedDistribution_GaussianDelta_ implements LambdaMessageHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>IndexedDistribution</tt>
	  * followed by one <tt>GaussianDelta</tt>.
	  */
	public SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.IndexedDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.GaussianDelta", 1 );
		return s;
	}

	/** Ignores <tt>pi_messages</tt>.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		IndexedDistribution px = (IndexedDistribution) px_in;
		px.check_components();

		Discrete lambda_message = new Discrete();
		lambda_message.ndims = 1;
		lambda_message.dimensions = (int[]) px.index_dimensions.clone();
		lambda_message.probabilities = new double[ lambda_message.dimensions[0] ];

		double[] ii = new double[1], xx = new double[1];
		xx[0] = ((GaussianDelta)lambda).get_support()[0];

		for ( int i = 0; i < lambda_message.probabilities.length; i++ )
		{
			ii[0] = i;
			lambda_message.probabilities[i] = px.p( xx, ii );
		}

		return lambda_message;
	}
}
