package riso.distributions.computes_lambda_message;
import java.util.*;
import riso.distributions.*;

public class IndexedDistribution_GaussianDelta_ implements LambdaMessageHelper
{
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
