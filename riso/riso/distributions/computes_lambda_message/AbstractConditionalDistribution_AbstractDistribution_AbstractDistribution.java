package riso.distributions.computes_lambda_message;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.belief_nets.*;
import riso.approximation.*;
import numerical.*;

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
  *
  * <p> To construct the lambda message, two helpers are needed: one to compute values of the integral,
  * and the other to compute the integrand (which is then used to compute the integral).
  */
public class AbstractConditionalDistribution_AbstractDistribution_AbstractDistribution implements LambdaMessageHelper
{
	public Distribution compute_lambda_message( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		return new IntegralCache( pxuuu, lambda, pi_messages );
	}

	protected static class IntegralCache extends AbstractDistribution implements Callback_1d
	{
		FunctionCache cache;
		Integral_wrt_x integral_wrt_x;

		boolean is_discrete;
		double lambda_a, lambda_b;

		boolean support_known = false;
		double[] known_support = null;

		class Integral_wrt_x implements Callback_1d
		{
			ConditionalDistribution pxuuu;
			Distribution lambda;
			Distribution[] pi_messages;

			x_Integrand x_integrand;

			class x_Integrand implements Callback_1d
			{
				Integral_wrt_u integral_wrt_u;
				double[] u, u1 = new double[1], x1 = new double[1], xu = new double[2];
				double special_u, x;

				public double f( double x ) throws Exception
				{
System.err.println( "x_Integrand.f: x: "+x );
					x1[0] = x;
					xu[0] = x;
					xu[1] = special_u;
					return lambda.p( x1 ) * integral_wrt_u.f( xu );
				}

				class Integral_wrt_u implements Callback_nd
				{
					int u_skip_index;
					double[] pxuuu_a, pxuuu_b;
					boolean[] is_discrete;
					u_Integrand u_integrand;

					class u_Integrand implements Callback_nd
					{
						/** The argument <tt>u</tt> contains ALL the parent
						  * values, including the one corresponding to the
						  * parent to which we are sending this lambda message. 
						  */
						public double f( double[] u ) throws Exception
						{
System.err.println( "u_Integrand.f: u: " );
for ( int j = 0; j < u.length; j++ ) System.err.println( u[j]+" " );
							int i;
							double pi_product = 1;

							for ( i = 0; i < u.length; i++ )
							{
								if ( pi_messages[i] == null ) continue;

								u1[0] = u[i];
								pi_product *= pi_messages[i].p( u1 );
							}

							return pxuuu.p( x1, u ) * pi_product;
						}
					}

					/** Search the list of <tt>pi_messages</tt> to see which
					  * one is the one corresponding to the special parent.
					  * Also note whether each parent is discrete or not.
					  * Set limits of integration equal to the effective 
					  * support for the corresponding pi message.
					  */
					Integral_wrt_u( Distribution[] pi_messages ) throws RemoteException
					{
System.err.println( "Integral_wrt_u(Dist[]): called." );
						pxuuu_a = new double[ pi_messages.length ];
						pxuuu_b = new double[ pi_messages.length ];

						for ( int i = 0; i < pi_messages.length; i++ )
						{
							if ( pi_messages[i] == null )
								u_skip_index = i;
							else
							{
								is_discrete[i] = (pi_messages[i] instanceof Discrete);
								double[] ab = pi_messages[i].effective_support( 1e-6 );
								pxuuu_a[i] = ab[0];
								pxuuu_b[i] = ab[1];
							}
						}
System.err.println( "Integral_wrt_u: u_skip_index: "+u_skip_index );
for ( int j = 0; j < pi_messages.length; j++ )
if ( pi_messages[j] != null ) {
System.err.print( "pxuuu_a["+j+"]: "+pxuuu_a[j]+" pxuuu_b["+j+"]: "+pxuuu_b[j] );
System.err.println( (is_discrete[j]?" is discrete.":"is NOT discrete.") ); }
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
System.err.println( "Integral_wrt_u.f: x: "+xu[0]+" u: "+xu[1] );
						x1[0] = xu[0];	// set value for use by u_Integrand.f
						u[ u_skip_index ] = xu[1];	// ditto

						boolean[] skip_integration = new boolean[ u.length ];
						skip_integration[ u_skip_index ] = true;

						try
						{
							// Integrate pxuuu w.r.t. all parents except one.
							double pxu = ExtrapolationIntegral.do_integral( u.length, skip_integration, is_discrete, pxuuu_a, pxuuu_b, u_integrand, 1e-4, null, u );
							return pxu;
						}
						catch (ExtrapolationIntegral.DifficultIntegralException e)
						{
							System.err.println( "Integral_wrt_u.f: WARNING:\n\t"+e );
							return e.best_approx;
						}
						catch (Exception e2)
						{
							throw new RemoteException( "Integral_wrt_u.f: failed:\n\t"+e2 );
						}
					}
				}
			}

			Integral_wrt_x( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws RemoteException
			{
System.err.println( "Integral_wrt_x(CondDist,Dist,Dist[]): called." );
				is_discrete = (pxuuu instanceof ConditionalDiscrete);

				this.lambda = lambda;
				this.pxuuu = pxuuu;
				this.pi_messages = (Distribution[]) pi_messages.clone();

				x_integrand = this. new x_Integrand();

				double[] lambda_support = lambda.effective_support( 1e-8 );

				lambda_a = lambda_support[0];
				lambda_b = lambda_support[1];
			}

			public double f( double x ) throws Exception
			{
System.err.println( "Integral_wrt_x.f: x: "+x );
				try
				{
					double px = ExtrapolationIntegral.do_integral1d( is_discrete, lambda_a, lambda_b, x_integrand, 1e-4 );
					return px;
				}
				catch (ExtrapolationIntegral.DifficultIntegralException e)
				{
					System.err.println( "x_Integrand.f: WARNING:\n\t"+e );
					return e.best_approx;
				}
				catch (Exception e2)
				{
					throw new RemoteException( "x_Integrand.f: failed:\n\t"+e2 );
				}
			}
		}

		private IntegralCache() throws RemoteException
		{
System.err.println( "IntegralCache(): called (SHOULDN'T BE!)" );
		}

		IntegralCache( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws RemoteException
		{
System.err.println( "IntegralCache(CondDist,Dist,Dist[]): called." );
			cache = new FunctionCache( -1e0, -1e0, this );
			integral_wrt_x = new Integral_wrt_x( pxuuu, lambda, pi_messages );
		}

		public double p( double[] u ) throws RemoteException
		{
System.err.println( "IntegralCache.p: u: "+u[0] );
			try { return cache.lookup( u[0] ); }
			catch (Exception e) { throw new RemoteException( "IntegralCache.p: unexpected: "+e ); }
		}

		public double f( double u ) throws Exception
		{
System.err.println( "IntegralCache.f: u: "+u );
			return integral_wrt_x.f( u );
		}

		public MixGaussians initial_mix( double[] support ) throws RemoteException
		{
System.err.println( "Integral_wrt_x.initial_mix: support: "+support[0]+", "+support[1] );
			// Pave the support with bumps. THIS IS VERY SIMPLE-MINDED !!!
			// SHOULD LOOK FOR REGIONS OF HIGH DENSITY !!!

			int nbumps = 10;	// IS THERE A BETTER CHOICE ???

			MixGaussians q = null;
			try { q = new MixGaussians( 1, nbumps ); }
			catch (RemoteException e)
			{
				throw new RemoteException( "Integral_wrt_x.initial_mix: can't create initial mix: "+e );
			}

			double s = (support[1] - support[0])/nbumps/2.0;

			for ( int i = 0; i < nbumps; i++ )
				q.components[i] = new Gaussian( support[0]+(2*i+1)*s, s );

			return q;
		}
		
		public Object remote_clone() throws CloneNotSupportedException, RemoteException
		{
			// These objects are immutable (HOW TO ENFORCE???) so cloning
			// is not a useful operation.

System.err.println( "IntegralCache.remote_clone: return reference to this." );
			return this;
		}

		public double[] effective_support( double tolerance ) throws RemoteException
		{
System.err.println( "IntegralCache.effective_support: called." );
			// Skip time-consuming support calculation if possible.
			if ( support_known ) return known_support;

			Exception most_recent = null;

			for ( double scale = 1; scale > 0.01 && scale < 100; )
			{
				try 
				{
					known_support = Intervals.effective_support( this, 1, tolerance );
System.err.println( "Integral_wrt_x.effective_support: effective support: "+known_support[0]+", "+known_support[1] );
					support_known = true;
					return known_support;
				}
				catch (Intervals.ScaleTooBigException e)
				{
					most_recent = e;
					scale /= 2;
				}
				catch (SupportNotWellDefinedException e2)
				{
					most_recent = e2;
					scale *= 2;
				}
				catch (Exception e3) { throw new RemoteException( "Integral_wrt_x.effective_support: failed:\n\t"+e3 ); }
			}

			throw new RemoteException( "Intervals.effective_support: failed; most recent exception:\n\t"+most_recent );
		}
	}
}
