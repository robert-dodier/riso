package riso.distributions.computes_lambda_message;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.belief_nets.*;
import riso.approximation.*;
import numerical.*;
import ObjectCache;

/** This class implements a lambda message helper for a variable <tt>x</tt> with
  * one or more parents <tt>u1,...,un</tt>. Except for the parent to which
  * we are sending the lambda message, each parent sends a pi message to
  * <tt>x</tt>. Let us suppose that we are sending the lambda message to
  * parent <tt>uk</tt>. The lambda message is defined as
  * <pre>
  *   p( e \ e_u1(above) | uk )
  *     = \int p( x, e \ e_uk(above) | uk ) dx
  *     = \int p( x, e_x(below) + e_x(above) \ e_uk(above) | uk ) dx
  *     = \int p( e_x(below) | x ) p( x | e_x(above) \ e_uk(above), uk ) p( e_x(above) \ e_uk(above) | uk ) dx
  *     = p( e_x(above) \ e_uk(above) ) \int p( e_x(below) | x )
  *         \int ... \int p( x | u1,...,un ) p( u1,...,un \ uk |  e_x(above) \ e_uk(above), uk ) 
  *         du1 ... du_{k-1} du_{k+1} ... du_n dx
  * </pre>
  * The factor <tt>p( e_x(above) \ e_uk(above) )</tt> doesn't depend on <tt>uk</tt> so we can ignore it
  * (a lambda message need not integrate to anything in particular).
  * Now note that 
  * <pre>
  *    p( u1,...,un \ uk | e_x(above) \ e_uk(above), uk ) = \prod_{j \neq k} p( uj | e_uj(above) )
  * </pre>
  * so finally we have
  * <pre>
  *   p( e \ e_u1(above) | uk )
  *     = \int p( e_x(below) | x ) \int ... \int p( x | u1,...,un )
  *         \prod_{j \neq k} p( uj | e_uj(above) ) du1 ... du_{k-1} du_{k+1} ... du_n dx
  * </pre>
  * In this last equation the lambda function of <tt>x</tt> appears, <tt>p( e_x(below) | x )</tt>,
  * and the pi messages coming to <tt>x</tt> from parents other than <tt>uk</tt>,
  * <tt>p( uj | e_uj(above) ), j \neq k</tt>. The conditional distribution of <tt>x</tt> given
  * all its parents, <tt>p( x | u1,...,un )</tt>, links the other pieces together.
  *
  * <p> Note that this integral is a function of the parent <tt>uk</tt>.
  * The message which is sent up the parent is NOT a Gaussian mixture or other approximation;
  * the message is a direct representation of the integral. The evaluation of the integral is
  * put off until the posterior or lambda of <tt>uk</tt> needs to be computed.
  */
public class AbstractConditionalDistribution_AbstractDistribution_AbstractDistribution implements LambdaMessageHelper
{
	public Distribution compute_lambda_message( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		return new IntegralCache( pxuuu, lambda, pi_messages );
	}
}

/** This class wraps the integral evaluation with a cache so that the integral
  * need not be evaluated every time; if the integral has been evaluated for a
  * nearby value of the special parent, an interpolated value is returned.
  */
class IntegralCache extends AbstractDistribution implements Callback_1d
{
	FunctionCache cache;
	Integral_wrt_x integral_wrt_x;

	class Integral_wrt_x implements Callback_1d
	{
		ConditionalDistribution pxuuu;
		Distribution lambda;
		Distribution[] pi_messages;

		x_Integrand x_integrand;
		IntegralHelper1d ih1d;

		boolean x_is_discrete;
		ObjectCache support_cache = new ObjectCache( 0.4, 100 );
		int nrndp = 5;			// #random parent values; for cond. support calc

		class x_Integrand implements Callback_1d
		{
			Integral_wrt_u integral_wrt_u;
			double[] u, u1 = new double[1], x1 = new double[1], xu = new double[2];
			double special_u, x;
			int special_u_index;

			class Integral_wrt_u implements Callback_nd
			{
				double[] pxuuu_a, pxuuu_b;
				boolean[] u_is_discrete, skip_integration;
				u_Integrand u_integrand;
				IntegralHelper ih;

				class u_Integrand implements Callback_nd
				{
					/** The argument <tt>u</tt> contains ALL the parent
					  * values, including the one corresponding to the
					  * parent to which we are sending this lambda message. 
					  */
					public double f( double[] u ) throws Exception
					{
						int i;
						double pi_product = 1;

						for ( i = 0; i < u.length; i++ )
						{
							if ( pi_messages[i] == null ) continue;

							u1[0] = u[i];
							pi_product *= pi_messages[i].p( u1 );
						}

						double pp = pxuuu.p( x1, u ) * pi_product;
						return pp;
					}
				}

				/** Search the list of <tt>pi_messages</tt> to see which
				  * one is the one corresponding to the special parent.
				  * Also note whether each parent is discrete or not.
				  * Set limits of integration equal to the effective 
				  * support for the corresponding pi message.
				  */
				Integral_wrt_u( Distribution[] pi_messages ) throws Exception
				{
					AbstractVariable[] parents = null;
					AbstractVariable child = ((AbstractConditionalDistribution)pxuuu).associated_variable;
					if ( child != null )
						parents = child.get_parents();

					pxuuu_a = new double[ pi_messages.length ];
					pxuuu_b = new double[ pi_messages.length ];
					u_is_discrete = new boolean[ pi_messages.length ];

					skip_integration = new boolean[ pi_messages.length ];

					for ( int i = 0; i < pi_messages.length; i++ )
					{
						if ( pi_messages[i] == null )
							special_u_index = i;
						else
						{
							if ( parents != null )
								u_is_discrete[i] = parents[i].is_discrete();
							else
								// This is unsatisfactory; distributions of other classes can be discrete. !!!
								u_is_discrete[i] = (pi_messages[i] instanceof Discrete);
							double[] ab = pi_messages[i].effective_support( 1e-6 );
							pxuuu_a[i] = ab[0];
							pxuuu_b[i] = ab[1];
							if ( pi_messages[i] instanceof Delta )
							{
								skip_integration[i] = true;
								u[i] = ((Delta)pi_messages[i]).get_support()[0];
							}
						}
					}

					skip_integration[ special_u_index ] = true;

					u_integrand = new u_Integrand();
					ih = new IntegralHelper( u_integrand, pxuuu_a, pxuuu_b, u_is_discrete, skip_integration );

System.err.println( "Integral_wrt_u: special_u_index: "+special_u_index );
System.err.println( "\tfrom "+child.get_name()+" to "+parents[special_u_index].get_name() );
for ( int j = 0; j < pi_messages.length; j++ )
if ( pi_messages[j] != null ) {
System.err.print( "\tpxuuu_a["+j+"]: "+pxuuu_a[j]+" pxuuu_b["+j+"]: "+pxuuu_b[j] );
System.err.print( "; "+parents[j].get_name()+(u_is_discrete[j]?" is discrete.":" is NOT discrete.") );
System.err.println( (skip_integration[j]?" (do NOT integrate)":" (do integrate)") ); }
				}
					
				/** Set up and compute integral over all parents other
				  * than the one <tt>u_k</tt> to which we are sending the
				  * lambda message. Set the value for <tt>u_k</tt> and mark
				  * it so we don't integrate over it. The integral is a
				  * function of <tt>x</tt> and <tt>u_k</tt>; unpack the
				  * argument <tt>xu</tt> into those two pieces.
				  */
				public double f( double[] xu ) throws Exception
				{
					x1[0] = xu[0];	// set value for use by u_Integrand.f
					u[ special_u_index ] = xu[1];	// ditto

					try
					{
						// Integrate pxuuu w.r.t. all parents except one.
						double pxu = ih.do_integral( u );
						return pxu;
					}
					catch (Exception e)
					{
						e.printStackTrace();
						throw new Exception( "Integral_wrt_u.f: failed:\n\t"+e );
					}
				}
			}

			public x_Integrand( Distribution[] pi_messages ) throws Exception
			{
				u = new double[ pi_messages.length ];
				integral_wrt_u = this. new Integral_wrt_u( pi_messages );
			}

			public double f( double x ) throws Exception
			{
				x1[0] = x;
				xu[0] = x;
				xu[1] = special_u;
				return lambda.p( x1 ) * integral_wrt_u.f( xu );
			}
		}

		Integral_wrt_x( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws Exception
		{
			AbstractVariable child = ((AbstractConditionalDistribution)pxuuu).associated_variable;
			if ( child != null )
				x_is_discrete = child.is_discrete();
			else
				// This is unsatisfactory; distributions of other classes can be discrete. !!!
				x_is_discrete = (pxuuu instanceof ConditionalDiscrete);

			this.lambda = lambda;
			this.pxuuu = pxuuu;
			this.pi_messages = (Distribution[]) pi_messages.clone();

			x_integrand = this. new x_Integrand( pi_messages );
	
			// At this point, we don't know what interval we'll be integrating over.
			ih1d = new IntegralHelper1d( x_integrand, null, x_is_discrete );
		}

		public double f( double u ) throws Exception
		{
			if ( lambda instanceof Delta )
			{
				x_integrand.xu[0] = ((Delta)lambda).get_support()[0];
				x_integrand.xu[1] = u;
				return x_integrand.integral_wrt_u.f( x_integrand.xu );
			}

			try
			{
				x_integrand.special_u = u;
				
				// At this point we have the info needed to establish the range of integration.
				// We have to do this every time through -- location of support
				// changes with context.
				double[][] x_support = effective_conditional_support( 1e-4 );

				ih1d.a = new double[ x_support.length ];
				ih1d.b = new double[ x_support.length ];
				for ( int i = 0; i < x_support.length; i++ )
				{
					ih1d.a[i] = x_support[i][0];
					ih1d.b[i] = x_support[i][1];
				}

				double px = ih1d.do_integral();
				return px;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new Exception( "Integral_wrt_x.f: failed:\n\t"+e );
			}
		}

		/** Set range equal to support of p(x|u1,...,un), using the given
		  * value of special_u and random values for all other parents.
		  */
		double[][] effective_conditional_support( double tol ) throws Exception
		{
			double[][] support = (double[][]) support_cache.lookup( x_integrand.special_u );
			if ( support == null )
				return (double[][]) support_cache.cache_new_value( x_integrand.special_u, compute_conditional_support(tol) );
			else
				return support;
		}

		double[][] compute_conditional_support( double tol ) throws Exception
		{
			int nrandom = 1;
			for ( int i = 0; i < pi_messages.length; i++ )
			{
				if ( i == x_integrand.special_u_index ) continue;

				if ( pi_messages[i] instanceof Discrete )
					nrandom *= ((Discrete)pi_messages[i]).probabilities.length;
				else
					nrandom *= nrndp;
			}

			double[][] random_supports = new double[ nrandom ][];

			int[] ii = new int[1];
			double[] uuu = new double[ pi_messages.length ];
			generate_supports( random_supports, ii, uuu, 0, tol );
		
			double[][] merged = Intervals.union_merge_intervals( random_supports );
System.err.println( "  compute_cond_supt --- special_u: "+x_integrand.special_u+", merged supports: " );
for ( int j = 0; j < merged.length; j++ )
System.err.println( "\t\t["+merged[j][0]+", "+merged[j][1]+"]" );
			return merged;
		}

		void generate_supports( double[][] rnd_supts, int[] ii, double[] uuu, int m, double tol ) throws Exception
		{
			if ( m == pi_messages.length )
			{
				Distribution px = pxuuu.get_density(uuu);
				rnd_supts[ ii[0]++ ] = px.effective_support( tol );
			}
			else
			{
				if ( m == x_integrand.special_u_index )
				{
					uuu[m] = x_integrand.special_u;
					generate_supports( rnd_supts, ii, uuu, m+1, tol );
				}
				else
				{
					if ( pi_messages[m] instanceof Discrete )
					{
						int n = ((Discrete)pi_messages[m]).probabilities.length;
						for ( int i = 0; i < n; i++ )
						{
							uuu[m] = i;
							generate_supports( rnd_supts, ii, uuu, m+1, tol );
						}
					}
					else
					{
						int n = nrndp;
						for ( int i = 0; i < n; i++ )
						{
							uuu[m] = pi_messages[m].random()[0];
							generate_supports( rnd_supts, ii, uuu, m+1, tol );
						}
					}
				}
			}
		}
	}

	private IntegralCache() {}	// force callers to use other constructor to set up integrals

	IntegralCache( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		integral_wrt_x = new Integral_wrt_x( pxuuu, lambda, pi_messages );
		cache = new FunctionCache( 1e-2, -1e0, integral_wrt_x );
	}

	public double p( double[] u ) throws Exception
	{
		try { return cache.lookup( u[0] ); }
		catch (Exception e) { e.printStackTrace(); throw new Exception( "IntegralCache.p: unexpected: "+e ); }
	}

	public double f( double u ) throws Exception
	{
		try { return cache.lookup( u ); }
		catch (Exception e) { e.printStackTrace(); throw new Exception( "IntegralCache.f: unexpected: "+e ); }
	}

	public Object remote_clone() throws CloneNotSupportedException
	{
		// These objects are immutable (HOW TO ENFORCE???) so cloning
		// is not a useful operation.

System.err.println( "IntegralCache.remote_clone: return reference to this." );
		return this;
	}

	public double[] effective_support( double tolerance ) throws Exception
	{
		throw new SupportNotWellDefinedException( "IntegralCache.effective_support: refuse to compute support for a likelihood." );
	}
}
