package riso.distributions;
import java.util.*;

public class ComputesLambdaMessage_ConditionalDiscrete_Discrete_Discrete implements LambdaMessageHelper
{
	Distribution[] pi_messages;
	Discrete lambda_message;
	ConditionalDiscrete px;
	Discrete lambda;
	int skip_index = -1, first_skip = -1, skipped_ndims = 0;

	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda_in, Distribution[] pi_messages_in ) throws Exception
	{
		pi_messages = pi_messages_in;
		px = (ConditionalDiscrete) px_in;
		lambda = (Discrete) lambda_in;

		int i, n, nonnull_total = 0;
		for ( first_skip = 0, i = 0; i < pi_messages.length; i++ )
		{
			if ( pi_messages[i] == null )
			{
				if ( skip_index != -1 )
					throw new IllegalArgumentException( "compute_lambda_message: more than one null pi message!" );
				skip_index = i;
			}
			else
			{
				nonnull_total += pi_messages[i].ndimensions();
				if ( skip_index == -1 )
					first_skip += pi_messages[i].ndimensions();
			}
		}

		skipped_ndims = px.ndimensions_parent() - nonnull_total;
System.err.println( "compute_lambda_message: skip_index: "+skip_index+", first_skip: "+first_skip );
System.err.println( "\t"+"nonnull_total: "+nonnull_total+" skipped_ndims: "+skipped_ndims );

		lambda_message = new Discrete();
		lambda_message.ndims = skipped_ndims;
		lambda_message.dimensions = new int[ lambda_message.ndims ];
		System.arraycopy( px.dimensions_parents, first_skip, lambda_message.dimensions, 0, lambda_message.ndims );

		for ( n = 1, i = 0; i < lambda_message.ndims; i++ ) 
			n *= lambda_message.dimensions[i];
		lambda_message.probabilities = new double[ n ];

		// The outer summations are over the parents for the pi messages.
		// The inner summations are over the child, using lambda.

		double[] iu_skip = new double[ lambda_message.ndims ];
		loopover_pi_summation( iu_skip, 0 );

System.err.print( "compute_lambda_message: return message: "+lambda_message.format_string( "****" ) );
		return lambda_message;
	}

	void loopover_pi_summation( double[] iu_skip, int m ) throws Exception
	{
		if ( m == lambda_message.ndims )
		{
			// We've set up some configuration of values for the parents;
			// now sum over the child and index the result by the parent
			// configuration.

			int k, kk = 0;
			for ( k = 0; k < iu_skip.length-1; k++ )
				kk = lambda_message.dimensions[k+1] * (kk + (int) iu_skip[k]);
			kk += (int) iu_skip[ iu_skip.length-1 ];

			double[] iu = new double[ px.ndims_parents ];
			System.arraycopy( iu_skip, 0, iu, first_skip, iu_skip.length );
			double p = outer_pi_summation( iu, 0 );
			lambda_message.probabilities[kk] = p;
		}
		else
		{
			for ( int k = 0; k < lambda_message.dimensions[m]; k++ )
			{
				iu_skip[m] = k;
				loopover_pi_summation( iu_skip, m+1 );
			}
		}
	}

	double outer_pi_summation( double[] iu, int m ) throws Exception
	{
		if ( m == px.ndims_parents )
		{
// System.err.println( "outer_pi_summation: iu: " );
// numerical.Matrix.pretty_output( iu, System.err, " " );
// System.err.println("");
			double[] ix = new double[ px.ndims_child ];
			return inner_pi_summation( iu, ix, 0 );
		}
		else
		{
			if ( m == first_skip )
			{
				return outer_pi_summation( iu, m+lambda_message.ndims );
			}
			else
			{
				double sum = 0;
				for ( int k = 0; k < px.dimensions_parents[m]; k++ )
				{
					iu[m] = k;
					sum += outer_pi_summation( iu, m+1 );
				}
				return sum;
			}
		}
	}

	double inner_pi_summation( double[] iu, double[] ix, int n ) throws Exception
	{
		if ( n == px.ndims_child )
		{
			// We have a complete configuration of children as well as parents;
			// compute and return the summand.
// System.err.println( "inner_pi_summation: ix: " );
// numerical.Matrix.pretty_output( ix, System.err, " " );

			int k, kk;
			double pi_prod = 1;
			for ( kk = 0, k = 0; k < pi_messages.length; k++ )
			{
				if ( k == skip_index )
				{
					kk += skipped_ndims;
					continue;
				}

				double[] iuk = new double[ pi_messages[k].ndimensions() ];
				System.arraycopy( iu, kk, iuk, 0, pi_messages[k].ndimensions() );
				pi_prod *= pi_messages[k].p( iuk );
				kk += pi_messages[k].ndimensions();
// System.err.print( "  iuk: " );
// numerical.Matrix.pretty_output( iuk, System.err, " " );
// System.err.println( "  pi_prod: "+pi_prod );
			}
// if ( pi_messages.length == 0 ) System.err.println("");

			return lambda.p( ix ) * pi_prod * px.p( ix, iu );
		}
		else
		{
			double sum = 0;
			for ( int k = 0; k < px.dimensions_child[n]; k++ )
			{
				ix[n]= k;
				sum += inner_pi_summation( iu, ix, n+1 );
			}
			return sum;
		}
	}
}
