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

	boolean is_discrete;
	double[][] lambda_supports;

	boolean support_known = false;
	double[] known_support = null;

	class Integral_wrt_x implements Callback_1d
	{
		ConditionalDistribution pxuuu;
		Distribution lambda;
		Distribution[] pi_messages;

		x_Integrand x_integrand;
		IntegralHelper1d ih1d;

		class x_Integrand implements Callback_1d
		{
			Integral_wrt_u integral_wrt_u;
			double[] u, u1 = new double[1], x1 = new double[1], xu = new double[2];
			double special_u, x;

			class Integral_wrt_u implements Callback_nd
			{
				int u_skip_index;
				double[] pxuuu_a, pxuuu_b;
				boolean[] is_discrete, skip_integration;
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
// System.err.print( "\tu_Integrand.f: u: " );
// for ( int j = 0; j < u.length; j++ ) System.err.print( u[j]+" " );
						int i;
						double pi_product = 1;

						for ( i = 0; i < u.length; i++ )
						{
							if ( pi_messages[i] == null ) continue;

							u1[0] = u[i];
							pi_product *= pi_messages[i].p( u1 );
						}

						double pp = pxuuu.p( x1, u ) * pi_product;
// System.err.println( "; x: "+x1[0]+"; pxuuu.p(x1,u): "+pxuuu.p( x1, u )+" pi_product: "+pi_product+"; return: "+pp );
						return pp;
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
					is_discrete = new boolean[ pi_messages.length ];

					skip_integration = new boolean[ pi_messages.length ];

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
							if ( pi_messages[i] instanceof Delta )
							{
								skip_integration[i] = true;
								u[i] = ((Delta)pi_messages[i]).get_support()[0];
							}
						}
					}

					skip_integration[ u_skip_index ] = true;

					u_integrand = new u_Integrand();
					ih = new IntegralHelper( u_integrand, pxuuu_a, pxuuu_b, is_discrete, skip_integration );

System.err.println( "Integral_wrt_u: u_skip_index: "+u_skip_index );
AbstractVariable child = ((AbstractConditionalDistribution)pxuuu).associated_variable;
AbstractVariable[] parents = child.get_parents();
System.err.println( "\tfrom "+child.get_name()+" to "+parents[u_skip_index].get_name() );
for ( int j = 0; j < pi_messages.length; j++ )
if ( pi_messages[j] != null ) {
System.err.print( "\tpxuuu_a["+j+"]: "+pxuuu_a[j]+" pxuuu_b["+j+"]: "+pxuuu_b[j] );
System.err.print( "; "+parents[j].get_name()+(is_discrete[j]?" is discrete.":" is NOT discrete.") );
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
// System.err.println( "Integral_wrt_u.f: x: "+xu[0]+" u: "+xu[1] );
					x1[0] = xu[0];	// set value for use by u_Integrand.f
					u[ u_skip_index ] = xu[1];	// ditto

					try
					{
						// Integrate pxuuu w.r.t. all parents except one.
						double pxu = ih.do_integral( u );
						return pxu;
					}
					catch (Exception e)
					{
						throw new RemoteException( "Integral_wrt_u.f: failed:\n\t"+e );
					}
				}
			}

			public x_Integrand( Distribution[] pi_messages ) throws RemoteException
			{
System.err.println( "x_Integrand(Dist[]): called." );
				u = new double[ pi_messages.length ];
				integral_wrt_u = this. new Integral_wrt_u( pi_messages );
			}

			public double f( double x ) throws Exception
			{
// System.err.println( "x_Integrand.f: x: "+x );
				x1[0] = x;
				xu[0] = x;
				xu[1] = special_u;
				return lambda.p( x1 ) * integral_wrt_u.f( xu );
			}
		}

		Integral_wrt_x( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws RemoteException
		{
System.err.println( "Integral_wrt_x(CondDist,Dist,Dist[]): called." );
			is_discrete = (pxuuu instanceof ConditionalDiscrete);

			this.lambda = lambda;
			this.pxuuu = pxuuu;
			this.pi_messages = (Distribution[]) pi_messages.clone();

			x_integrand = this. new x_Integrand( pi_messages );

			lambda_supports = new double[1][];
			lambda_supports[0] = lambda.effective_support( 1e-8 );
	
			ih1d = new IntegralHelper1d( x_integrand, lambda_supports, is_discrete );
		}

		public double f( double u ) throws Exception
		{
// System.err.print( "Integral_wrt_x.f: u: "+u );
			if ( lambda instanceof Delta )
			{
				x_integrand.xu[0] = ((Delta)lambda).get_support()[0];
// System.err.println( "; lambda instanceof Delta; concentrated on: "+x_integrand.xu[0] );
				x_integrand.xu[1] = u;
				return x_integrand.integral_wrt_u.f( x_integrand.xu );
			}
// System.err.println("");

			try
			{
				x_integrand.special_u = u;
				double px = ih1d.do_integral();
				return px;
			}
			catch (Exception e)
			{
				throw new RemoteException( "Integral_wrt_x.f: failed:\n\t"+e );
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
		integral_wrt_x = new Integral_wrt_x( pxuuu, lambda, pi_messages );
		cache = new FunctionCache( 1e-2, -1e0, integral_wrt_x );
	}

	public double p( double[] u ) throws RemoteException
	{
// System.err.print( "IntegralCache.p: cache: "+cache+"; " ); System.err.println( "u: "+u[0] );
		try { return cache.lookup( u[0] ); }
		catch (Exception e) { throw new RemoteException( "IntegralCache.p: unexpected: "+e ); }
	}

	public double f( double u ) throws Exception
	{
// System.err.println( "IntegralCache.f: u: "+u );
		try { return cache.lookup( u ); }
		catch (Exception e) { throw new Exception( "IntegralCache.f: unexpected: "+e ); }
	}

	public MixGaussians initial_mix( double[] support ) throws RemoteException
	{
System.err.println( "IntegralCache.initial_mix: support: "+support[0]+", "+support[1] );
		Vector q_vector = new Vector();
		int i, npavers = 7, ngrid = 500;
		
		// Look for regions of high density.

		double dx = (support[1]-support[0])/ngrid;
		double[] px = new double[ ngrid ], x1 = new double[1];

		for ( i = 0; i < ngrid; i++ )
		{
			x1[0] = support[0]+(i+0.5)*dx;
			px[i] = p(x1);
		}

		for ( i = 1; i < ngrid-1; i++ )
		{
			// ??? if ( px[i-2] < px[i-1] && px[i-1] < px[i] && px[i] > px[i+1] && px[i+1] > px[i+2] )
			if ( px[i-1] < px[i] && px[i] > px[i+1] )
			{
				x1[0] = support[0]+(i+0.5)*dx;

				// ESTIMATE 2ND DERIVATIVE OF THE PROBABILITY DENSITY AND SET SIGMA ACCORDINGLY !!!
				// COULD USE A MORE ACCURATE FORMULA !!!

				double dp2 = (px[i-1] - 2*px[i] + px[i+1])/(dx*dx);
				double s = 1 / Math.pow( -dp2, 1/3.0 ) / Math.pow( 2*Math.PI, 1/6.0 );
System.err.println( "IntegralCache.initial_mix: may be bump at "+x1[0]+"; take stddev = "+s );
				q_vector.addElement( new Gaussian( x1[0], s ) );
			}
		}

		int nbumps = q_vector.size();

		// Pave over support, in case bumps are too widely spread.

		double s = (support[1] - support[0])/npavers/2.0;

		for ( i = 0; i < npavers; i++ )
			q_vector.addElement( new Gaussian( support[0]+(2*i+1)*s, s ) );

		MixGaussians q = null;
System.err.println( "IntegralCache.initial_mix: total number of components: "+q_vector.size() );
		try { q = new MixGaussians( 1, q_vector.size() ); }
		catch (RemoteException e)
		{
			throw new RemoteException( "IntegralCache.initial_mix: can't create initial mix: "+e );
		}

		q_vector.copyInto( q.components );

		// Now fudge the mixing coefficients so that the pavement gets
		// less weight than the bumps. The bumps precede the pavement
		// in the list of components.

		for ( i = 0; i < nbumps; i++ ) q.mix_proportions[i] *= 2e1;
		double sum = 0;
		for ( i = 0; i < q.mix_proportions.length; i++ ) sum += q.mix_proportions[i];
		for ( i = 0; i < q.mix_proportions.length; i++ ) q.mix_proportions[i] /= sum;

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
System.err.println( "IntegralCache.effective_support: throw SupportNotWellDefinedException !!!" );
		throw new SupportNotWellDefinedException( "IntegralCache.effective_support: can we avoid support calc for all likelihoods???" );
	}
}
