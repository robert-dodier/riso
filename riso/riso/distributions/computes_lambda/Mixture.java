package riso.distributions.computes_lambda;
import java.io.*;
import riso.distributions.*;

/** Yeah, it's sort of confusing that this class has the same name as
  * in <tt>riso.distributions</tt>, but that's a consequence of the 
  * naming scheme used to locate message helpers. This class implements
  * a helper which can handle a list of <tt>riso.distributions.Mixture</tt>
  * messages.
  */
public class Mixture implements LambdaHelper
{
	/** @throws RuntimeException Since this method is not implemented!
	  *   It's conceivable this method could be invoked -- for example,
	  *   if a mixture has a component which is itself a mixture. Cross
	  *   that bridge when we get to it...
	  */
	public double ignored_scale( Distribution[] lambda_messages )
	{
		throw new RuntimeException( "computes_lambda.Mixture.ignored_scale: not implemented." );
	}
	
	/** @return The product of the incoming likelihood messages, which is
	  *   again a <tt>riso.distributions.Mixture</tt>.
	  * @see LambdaHelper.compute_lambda
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may noninformative; skip over those.
		// Construct a list containing only the informative lambda messages.

		int i, j, ninformative = 0;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( !( lambda_messages[i] instanceof Noninformative ) )
				++ninformative;
		}
			
		riso.distributions.Mixture[] informative_lambdas = new riso.distributions.Mixture[ ninformative ];

		for ( i = 0, j = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] instanceof Noninformative )
				continue;

			informative_lambdas[j++] = (riso.distributions.Mixture) lambda_messages[i];
		}

		riso.distributions.Mixture q = mixture_product( informative_lambdas );

		return q;
	}

	/** Computes a mixture from the product of a set of mixtures.
	  */
	public static riso.distributions.Mixture mixture_product( riso.distributions.Mixture[] mixtures ) throws Exception
	{
		if ( mixtures.length == 1 )
			try { return (riso.distributions.Mixture) mixtures[0].remote_clone(); }
			catch (CloneNotSupportedException e) 
			{
				throw new RuntimeException( "Mixture.mixture_product: unexpected: "+e );
			}

		int i, nproduct = 1;
		for ( i = 0; i < mixtures.length; i++ )
			nproduct *= mixtures[i].ncomponents();
		riso.distributions.Mixture product = new riso.distributions.Mixture( 1, nproduct );
System.err.println( "Mixture.mixture_product: nproduct: "+nproduct );

		// If possible, set up the lambda helper. We can set the helper
		// now if in every mixture all components are the same type.
		// Each mixture may have a different type.

		boolean common_types = true;
		for ( i = 0; i < mixtures.length; i++ )
		{
			if ( mixtures[i].common_type == null )
			{
				common_types = false;
				break;
			}
		}

		LambdaHelper helper = null;
		if ( common_types )
		{
			Distribution[] comp0_list = new Distribution[ mixtures.length ];
			for ( i = 0; i < mixtures.length; i++ )
				comp0_list[i] = mixtures[i].components[0];
			
			helper = LambdaHelperLoader.load_lambda_helper( comp0_list );
		}

		int[] k = new int[ mixtures.length ], l = new int[1];
		product_inner_loop( mixtures, product, k, l, mixtures.length-1, helper );

		// Fix up mixing coefficients.

		double sum = 0;
		for ( i = 0; i < product.ncomponents(); i++ ) sum += product.mix_proportions[i];
		for ( i = 0; i < product.ncomponents(); i++ ) product.mix_proportions[i] /= sum;
		
		return product;
	}
	
	static void product_inner_loop( riso.distributions.Mixture[] mixtures, riso.distributions.Mixture product, int[] k, int[] l, int m, LambdaHelper helper )  throws Exception
	{
		if ( m == -1 )
		{
			// Recursion has bottomed out.
			compute_one_product( mixtures, product, k, l, helper );
		}
		else
		{
			for ( int i = 0; i < mixtures[m].ncomponents(); i++ )
			{
				k[m] = i;
				product_inner_loop( mixtures, product, k, l, m-1, helper );
			}
		}
	}

	static void compute_one_product( riso.distributions.Mixture[] mixtures, riso.distributions.Mixture product, int[] k, int[] l, LambdaHelper helper ) throws Exception
	{
		double mix_proportion = 1;
		Distribution[] mix_combo = new Distribution[ mixtures.length ];

		for ( int i = 0; i < mixtures.length; i++ )
		{
			mix_proportion *= mixtures[i].mix_proportions[ k[i] ];
			mix_combo[i] = mixtures[i].components[ k[i] ];
		}

		if ( helper == null )
			helper = LambdaHelperLoader.load_lambda_helper( mix_combo );

		mix_proportion *= helper.ignored_scale( mix_combo );
		product.components[ l[0] ] =  helper.compute_lambda( mix_combo );

		product.mix_proportions[ l[0] ] = mix_proportion;
		++l[0];
	}
	
}
