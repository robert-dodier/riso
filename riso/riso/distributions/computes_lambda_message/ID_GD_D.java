package riso.distributions.computes_lambda_message;
import java.util.*;
import riso.distributions.*;
import SeqTriple;

public class ID_GD_D implements LambdaMessageHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>IndexedDistribution</tt>
	  * one <tt>GaussianDelta</tt>, and one <tt>Discrete</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.IndexedDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.GaussianDelta", 1 );
		s[2] = new SeqTriple( "riso.distributions.Discrete", 1 );
		return s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		IndexedDistribution px = (IndexedDistribution) px_in;
		px.check_components();

		double[] lambda_supt = ((GaussianDelta)lambda).get_support();
		Discrete pi_message;
		if ( pi_messages[0] != null ) // there are exactly 2 messages to check
			pi_message = (Discrete) pi_messages[0];
		else
			pi_message = (Discrete) pi_messages[1];

		// Check to see what kind of components the indexed distribution has.
		// If they're all conditional Gaussian, the lambda message is a mixture of Gaussians;
		// otherwise it's a general mixture.

		boolean all_gaussian = true;
		for ( int i = 0; i < px.components.length; i++ )
			if ( ! (px.components[i] instanceof ConditionalGaussian) )
			{
				all_gaussian = false;
				break;
			}

		Mixture lambda_message;
		if ( all_gaussian )
		{
			lambda_message = new MixGaussians( 1, pi_message.probabilities.length );
			for ( int i = 0; i < px.components.length; i++ )
			{
				ConditionalGaussian cg = (ConditionalGaussian) px.components[i];
				cg.check_matrices();
				double alpha = cg.a_mu_1c2[0][0];
				double beta = cg.b_mu_1c2[0];
				double sigma = Math.sqrt( cg.Sigma_1c2[0][0] );
				lambda_message.components[i] = new Gaussian( (lambda_supt[0]-beta)/alpha, sigma/alpha );
				lambda_message.mix_proportions[i] = pi_message.probabilities[i] / alpha;
			}
		}
		else
		{
			lambda_message = new Mixture( 1, pi_message.probabilities.length );
			for ( int i = 0; i < px.components.length; i++ )
			{
				if ( px.components[i] instanceof Distribution )
				{
					// This component is not conditional on the parent receiving the lambda message,
					// so throw in a Noninformative component into the mixture.
					double p = px.components[i].p( lambda_supt, null );
					lambda_message.components[i] = new Noninformative();
					lambda_message.mix_proportions[i] = p*pi_message.probabilities[i];
System.err.println( "ID_GD_D: throw in noninformative; p: "+p+", lambda_supt: "+lambda_supt[0]+", pimesg.prob["+i+"]: "+pi_message.probabilities[i] );
				}
				else if ( px.components[i] instanceof Gaussian )
				{
					ConditionalGaussian cg = (ConditionalGaussian) px.components[i];
					cg.check_matrices();
					double alpha = cg.a_mu_1c2[0][0];
					double beta = cg.b_mu_1c2[0];
					double sigma = Math.sqrt( cg.Sigma_1c2[0][0] );
					lambda_message.components[i] = new Gaussian( (lambda_supt[0]-beta)/alpha, sigma/alpha );
					lambda_message.mix_proportions[i] = pi_message.probabilities[i] / alpha;
				}
				else 
				{
					lambda_message.components[i] = new IntegralCache( px.components[i], lambda, new Distribution[1] );
					lambda_message.mix_proportions[i] = pi_message.probabilities[i];
				}
			}
		}

System.err.println( "*1*.compute_lambda_message: return lambda message:\n"+lambda_message.format_string("  ") );
		return lambda_message;
	}
}
