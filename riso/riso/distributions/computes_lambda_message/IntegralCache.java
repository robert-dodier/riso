/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions.computes_lambda_message;
import java.io.*;
import riso.approximation.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** This class wraps the integral evaluation with a cache so that the integral
  * need not be evaluated every time; if the integral has been evaluated for a
  * nearby value of the special parent, an interpolated value is returned.
  */
public class IntegralCache extends AbstractDistribution implements Callback_1d, Serializable
{
	public FunctionCache cache;
	Integral_wrt_x integral_wrt_x;

	/** Return the number of dimensions of this distribution. 
	  * ASSUME 1 !!!
	  */
	public int ndimensions() { return 1; }

	/** Create some semblance of a description of this object.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String more_ws = leading_ws+"\t";
		String result = getClass().getName()+"\n"+leading_ws+"{"+"\n";
		result += more_ws+"cache { "+cache.size+" points }\n";
		result += more_ws+integral_wrt_x.format_string(more_ws);
		result += leading_ws+"}\n";
		return result;
	}

	class Integral_wrt_x implements Callback_1d, Serializable
	{
		ConditionalDistribution pxuuu;
		Distribution lambda;
		Distribution[] pi_messages;

		x_Integrand x_integrand;
		qk21_IntegralHelper1d ih1d;

		boolean x_is_discrete;
		ObjectCache support_cache = new ObjectCache( 0.4, 100 );
		double[] quasi;
		int[] integration_index;
		int ngenerate;

		/** Create some semblance of a description of this object.
		  */
		public String format_string( String leading_ws ) throws IOException
		{
			String more_ws = leading_ws+"\t";
			String result = this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
			result += more_ws+"conditional "+pxuuu.getClass().getName()+"\n";
			result += more_ws+"lambda "+lambda.getClass().getName()+"\n";
			result += more_ws+"pi messages { ";
			for ( int i = 0; i < pi_messages.length; i++ )
				result += (pi_messages[i] == null?"(null)":pi_messages[i].getClass().getName())+" ";
			result += "}\n"+leading_ws+"}\n";
			return result;
		}

		class x_Integrand implements Callback_1d, Serializable
		{
			Integral_wrt_u integral_wrt_u;
			double[] u, u1 = new double[1], x1 = new double[1], xu = new double[2];
			double special_u, x;
			int special_u_index;

			class Integral_wrt_u implements Callback_nd, Serializable
			{
				double[] pxuuu_a, pxuuu_b;
				boolean[] u_is_discrete, skip_integration;
				u_Integrand u_integrand;
				IntegralHelper ih;

				class u_Integrand implements Callback_nd, Serializable
				{
					/** The argument <tt>u</tt> contains ALL the parent
					  * values, including the one corresponding to the
					  * parent to which we are sending this lambda message. 
					  */
					public double f( double[] u ) throws Exception
					{
						double pi_product = 1;

						for ( int i = 0; i < u.length; i++ )
						{
							if ( pi_messages[i] == null || pi_messages[i] instanceof Delta ) continue;

							u1[0] = u[i];
							double pp = pi_messages[i].p( u1 );
// System.err.print( "pimsg["+i+"].p("+u1[0]+")="+pp+"," );
							pi_product *= pp;
						}

						double pxup = pxuuu.p( x1, u );
						double pp = pxup * pi_product;
// System.err.print( "  pxuuu.p("+x1[0]+"|" ); for(int i=0;i<u.length;i++) System.err.print( u[i]+"," );
// System.err.println( ")="+pxup+"; return "+pp );
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
					AbstractVariable child = (AbstractVariable) ((AbstractConditionalDistribution)pxuuu).associated_variable;
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
							double[] ab = pi_messages[i].effective_support( 1e-4 );
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
					ih = IntegralHelperFactory.make_helper( u_integrand, pxuuu_a, pxuuu_b, u_is_discrete, skip_integration );

System.err.println( "Integral_wrt_u: special_u_index: "+special_u_index );
// System.err.println( "\tfrom "+child.get_name()+" to "+parents[special_u_index].get_name() );
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
// System.err.print( "Integral_wrt_u.f: xu: " ); for(int i=0;i<xu.length;i++) System.err.print( xu[i]+"," );
// System.err.println( " ih.do_integral(u) == "+pxu );
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
				double lpx = lambda.p( x1 );
				double iwufxu = integral_wrt_u.f( xu );
				double r = lpx*iwufxu;
				return r;
			}
		}

		Integral_wrt_x( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws Exception
		{
			AbstractVariable child = (AbstractVariable) ((AbstractConditionalDistribution)pxuuu).associated_variable;
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
			ih1d = new qk21_IntegralHelper1d( x_integrand, null, x_is_discrete );
		}

		public double f( double u ) throws Exception
		{
			if ( lambda instanceof Delta )
			{
				x_integrand.xu[0] = ((Delta)lambda).get_support()[0];
				x_integrand.xu[1] = u;
				double r = x_integrand.integral_wrt_u.f( x_integrand.xu );
				return r;
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
			int nintegrate = 0;
			for ( int i = 0; i < pi_messages.length; i++ )
				if ( ! x_integrand.integral_wrt_u.skip_integration[i] && ! x_integrand.integral_wrt_u.u_is_discrete[i] )
					++nintegrate;

			integration_index = new int[nintegrate];
			for ( int i = 0, j = 0; i < pi_messages.length; i++ )
				if ( ! x_integrand.integral_wrt_u.skip_integration[i] && ! x_integrand.integral_wrt_u.u_is_discrete[i] )
					integration_index[j++] = i;

			quasi = new double[integration_index.length];
			ngenerate = 20 * nintegrate; // CONSTANT HERE !!!
			if ( ngenerate == 0 ) ngenerate = 1; // nintegrate could be zero

			int ndiscrete = 1;
			for ( int i = 0; i < pi_messages.length; i++ )
			{
				if ( pi_messages[i] instanceof Discrete )
					ndiscrete *= ((Discrete)pi_messages[i]).probabilities.length;
			}

			double[][] random_supports = new double[ ngenerate*ndiscrete ][];
System.err.println( "\t"+"generate "+random_supports.length+" random supports." );

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
				// In 2 or more dimensions, use low discrepency sequence to get parent values;
				// otherwise (in 1 dimension) use ordinary pseudo-random numbers.

				if ( quasi.length > 1 ) LowDiscrepency.infaur( new boolean[2], quasi.length, ngenerate ); // IGNORE FLAGS !!!

				for ( int i = 0; i < ngenerate; i++ )
				{
					if ( quasi.length > 1 ) LowDiscrepency.gofaur(quasi);
					else if ( quasi.length == 1 ) quasi[0] = Math.random();
					// else there is no need to generate parent values.

					for ( int j = 0; j < integration_index.length; j++ )
					{
						int jj = integration_index[j];
						double a = x_integrand.integral_wrt_u.pxuuu_a[jj], b = x_integrand.integral_wrt_u.pxuuu_b[jj];
						uuu[jj] = a + (b-a)*quasi[j];
					}

					Distribution px = pxuuu.get_density(uuu);
System.err.print( "generate_supports: bottomed out; uuu: " );
Matrix.pretty_output(uuu,System.err,",");
System.err.println( " px: "+px.getClass() );
System.err.print( "\t"+"rnd_supports["+ii[0]+"]: " );
					rnd_supts[ ii[0]++ ] = px.effective_support( tol );
System.err.println( rnd_supts[ii[0]-1][0]+", "+rnd_supts[ii[0]-1][1] );
				}
			}
			else
			{
				if ( m == x_integrand.special_u_index )
				{
					uuu[m] = x_integrand.special_u;
System.err.println( "\t"+"assign special_u == "+x_integrand.special_u+" to uuu["+m+"]" );
					generate_supports( rnd_supts, ii, uuu, m+1, tol );
				}
				else if ( pi_messages[m] instanceof Discrete )
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
					if ( pi_messages[m] instanceof Delta )
						uuu[m] = pi_messages[m].expected_value();
					generate_supports( rnd_supts, ii, uuu, m+1, tol );
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

	public Object clone() throws CloneNotSupportedException
	{
		// These objects are immutable (HOW TO ENFORCE???) so cloning
		// is not a useful operation.

System.err.println( "IntegralCache.clone: return reference to this." );
		return this;
	}

	public double[] effective_support( double tolerance ) throws Exception
	{
		throw new SupportNotWellDefinedException( "IntegralCache.effective_support: refuse to compute support for a likelihood." );
	}
}
