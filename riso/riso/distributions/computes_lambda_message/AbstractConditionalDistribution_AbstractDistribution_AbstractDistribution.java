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
  *     = p( e_x(above) \ e_uk(above) ) \int p( e_x(below) | x ) \int ... \int p( x | u1,...,un ) p( u1,...,un \ uk |  e_x(above) \ e_uk(above), uk )  du1 ... du_{k-1} du_{k+1} ... du_n dx
  * </pre>
  * The factor <tt>p( e_x(above) \ e_uk(above) )</tt> doesn't depend on <tt>uk</tt> so we can ignore it
  * (a lambda message need not integrate to anything in particular).
  * Now note that 
  * <pre>
  *    p( u1,...,un \ uk | e_x(above) \ e_uk(above), uk ) = \prod_{j \neq k} p( uj | e_uj(above) )
  * </pre>
  * so finally we have
  * <pre>
  *   p( e \ e_u1(above) | uk ) = \int p( e_x(below) | x ) \int ... \int p( x | u1,...,un ) \prod_{j \neq k} p( uj | e_uj(above) ) du1 ... du_{k-1} du_{k+1} ... du_n dx
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
	public static class Integral_wrt_x extends AbstractDistribution implements Callback_1d
	{
		public class Integrand implements Callback_1d
		{
			protected transient double[] u_array = new double[1], x_array = new double[1];
			protected transient double u;

			public double f( double x ) throws Exception
			{
				x_array[0] = x;
				u_array[0] = u;
				return lambda.p( x_array ) * pxu.p( x_array, u_array );
			}
		}

		public class Integral_wrt_u extends AbstractDistribution implements Callback_nd
		{
			public class Integrand implements Callback_nd
			{
				public double f( double[] u_in )
				{
					int i;
					double pi_product = 1;

					for ( i = 0; i < u_array.length; i++ )
					{
						if ( i == skip_index ) continue;

						u1[0] = u_array[i];
						pi_product *= pi_messages[i].p( u1 );
					}

					return conditional.p( x1, u_array ) * pi_product;
				}

		ConditionalDistribution pxu;
		Distribution lambda;

		protected Integrand integrand;

		protected boolean is_discrete;
		protected double[] lambda_a, lambda_b;

		protected transient double[] u_array = new double[1];

		protected boolean support_known = false;
		protected double[] known_support = null;

		private Integral() throws RemoteException {}

		public Integral( ConditionalDistribution pxu_in, Distribution lambda_in ) throws RemoteException
		{
System.err.println( "Integral: constructor called." );
			// ACTUALLY MAY BE MORE COMPLICATED THAN THIS !!!
			// SINCE CHILD x CAN BE DISCRETE EVEN IF PARENT u IS CONTINUOUS !!!
			is_discrete = (pxu instanceof ConditionalDiscrete);

			lambda = lambda_in;
			pxu = pxu_in;

			integrand = this. new Integrand();

			double[] lambda_support = lambda.effective_support( 1e-8 );
System.err.println( "Integral: lambda support: "+lambda_support[0]+", "+lambda_support[1] );
System.err.println( "\t"+"integrand: "+integrand+"; lambda: "+lambda+", pxu: "+pxu );

			lambda_a = new double[1];
			lambda_b = new double[1];

			lambda_a[0] = lambda_support[0];
			lambda_b[0] = lambda_support[1];
		}

		public double p( double[] u_in ) throws RemoteException
		{
			if ( lambda instanceof Delta )
				return pxu.p( ((Delta)lambda).get_support(), u_in );

			integrand.u = u_in[0];

			double sum = 0;
			try
			{
				for ( int i = 0; i < lambda_a.length; i++ )
					sum += ExtrapolationIntegral.do_integral1d( is_discrete, lambda_a[i], lambda_b[i], integrand, 1e-4 );
			}
			catch (Exception e)
			{
				throw new RemoteException( "Integral.p: evaluation failed at u="+u_in[0]+": "+e );
			}

			return sum;
		}

		public double f( double u_in ) throws Exception
		{
			u_array[0] = u_in;
			return p( u_array );
		}

		public MixGaussians initial_mix( double[] support ) throws RemoteException
		{
System.err.println( "Integral.initial_mix: support: "+support[0]+", "+support[1] );
			// Pave the support with bumps. THIS IS VERY SIMPLE-MINDED !!!
			// SHOULD LOOK FOR REGIONS OF HIGH DENSITY !!!

			int nbumps = 10;	// IS THERE A BETTER CHOICE ???

			MixGaussians q = null;
			try { q = new MixGaussians( 1, nbumps ); }
			catch (RemoteException e)
			{
				throw new RemoteException( "Integral.initial_mix: can't create initial mix: "+e );
			}

			double s = (support[1] - support[0])/nbumps/2.0;

			for ( int i = 0; i < nbumps; i++ )
				q.components[i] = new Gaussian( support[0]+(2*i+1)*s, s );

			return q;
		}
		
		public double[] effective_support( double tolerance )
		{
			// Skip time-consuming support calculation if possible.
			if ( support_known ) return known_support;

System.err.println( "Integral.effective_support: this: "+this );
			known_support = Intervals.effective_support( this, 1, tolerance );	// SCALE MAY CHANGE !!! HOW ???
System.err.println( "Integral.effective_support: effective support: "+known_support[0]+", "+known_support[1] );

			support_known = true;
			return known_support;
		}
	
		public Object remote_clone() throws CloneNotSupportedException, RemoteException
		{
			Integral copy = new Integral();

			copy.integrand = integrand;	// all instances are essentially the same; no need to create a new one.

			copy.pxu = (pxu == null ? null : (ConditionalDistribution) pxu.remote_clone());
			copy.lambda = (lambda == null ? null : (Distribution) lambda.remote_clone());

			copy.is_discrete = is_discrete;
			copy.lambda_a = (lambda_a == null ? null : (double[]) lambda_a.clone());
			copy.lambda_b = (lambda_b == null ? null : (double[]) lambda_b.clone());

			copy.support_known = support_known;
			copy.known_support = (known_support == null ? null : (double[]) known_support.clone());

System.err.println( "Integral.remote_clone: copy: "+copy );
			return copy;
		}
	}

	/** Ignores <tt>pi_messages</tt>.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		return new Integral( pxu, lambda );
	}
}
