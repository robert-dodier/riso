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
package riso.approximation;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** This class contains a public static method to create a Gaussian mixture approximation to
  * an unconditional distribution.
  */
public class GaussianMixApproximation
{
	public static double nequivalent = Double.POSITIVE_INFINITY;
	public static boolean debug = false;

	/** This method creates a Gaussian mixture approximation to an unconditional distribution.
	  * The approach is described in Sections 5.5 and 5.6 of my dissertation.
	  * Briefly, the usual expectation-maximization algorithm for fitting a mixture to data
	  * is generalized to fitting a mixture to a continuous density.
	  * Discrete summations are replaced by integrations -- integrands are constructed
	  * as instances of helper classes. An effort is made to simplify the resulting mixture
	  * by throwing out components which have little mass or which are redundant with 
	  * another component.
	  *
	  * @param target This is the distribution to be approximated.
	  * @param approximation On input, an initial guess (cannot be null). On output, the final approximation.
	  * @param supports List of support intervals (i.e., a union of intervals) over which to construct
	  *  the approximation. This is not necessarily the support of the target.
	  * @param tolerance Hmm, this parameter is never used, for some reason. !!!
	  */
	public static MixGaussians do_approximation( Distribution target, MixGaussians approximation, double[][] supports, double tolerance ) throws Exception
	{
System.err.println( "GaussianMixApproximation.do_approximation: need approx. to "+target.getClass() );

		// Take care of a couple of trivial cases first.

		if ( target instanceof MixGaussians )
			return (MixGaussians) target.clone();

		if ( target instanceof Gaussian )
		{
			approximation = new MixGaussians( target.ndimensions(), 1 );
			approximation.components[0] = (Gaussian) target.clone();
			return approximation;
		}

		// Now the real work begins.

		int i, j, k, max_iterations = 10;		// CHANGE !!!

		double[] new_alpha = new double[ approximation.ncomponents() ];
		double[] new_mu = new double[ approximation.ncomponents() ];
		double[] new_sigma2 = new double[ approximation.ncomponents() ];

		CrossEntropyIntegrand cei = new CrossEntropyIntegrand( target, approximation );
		EntropyIntegrand ei = new EntropyIntegrand( target );

		IntegralHelper1d ceih = new IntegralHelper1d( cei, supports, false );
		IntegralHelper1d eih = new IntegralHelper1d( ei, supports, false );

		double te = eih.do_integral();
System.err.println( "do_approximation: TARGET ENTROPY: "+te );
		double ce0 = ceih.do_integral();
System.err.println( "do_approximation: INITIAL CROSS-ENTROPY: "+ce0 );

		IntegralHelper1d[] mpih = new IntegralHelper1d[  approximation.ncomponents()  ];
		IntegralHelper1d[] mih = new IntegralHelper1d[  approximation.ncomponents()  ];
		IntegralHelper1d[] vih = new IntegralHelper1d[  approximation.ncomponents()  ];

		for ( i = 0; i <  approximation.ncomponents(); i++ )
		{
			mpih[i] = new IntegralHelper1d( new MixingProportionIntegrand( i, target, approximation ), supports, false );
			mih[i] = new IntegralHelper1d( new MeanIntegrand( i, target, approximation ), supports, false );
			vih[i] = new IntegralHelper1d( new VarianceIntegrand( i, target, approximation ), supports, false );
		}

		double S[][] = new double[1][1];

		double sum_gamma = 0;
		for ( i = 0; i <  approximation.ncomponents(); i++ )
			sum_gamma += approximation.gamma[i];

		for ( k = 0; k < max_iterations; k++ )
		{
			for ( i = 0; i <  approximation.ncomponents(); i++ )
			{
				double a = mpih[i].do_integral();
				if ( nequivalent == Double.POSITIVE_INFINITY )
					new_alpha[i] = a;
				else
					new_alpha[i] = (nequivalent*a + approximation.gamma[i] - 1)/(nequivalent + sum_gamma - approximation.ncomponents());

				Gaussian g = (Gaussian) approximation.components[i];

				double m = mih[i].do_integral();
				if ( nequivalent == Double.POSITIVE_INFINITY )
					new_mu[i] = m/a;
				else
					new_mu[i] = (nequivalent*m + g.eta*g.mu_hat[0]) / (nequivalent*a + g.eta);

				double v = vih[i].do_integral();
				double delta_mu = m - g.mu_hat[0];
				if ( nequivalent == Double.POSITIVE_INFINITY )
					new_sigma2[i] = v/a;
				else
				{
					double ns = nequivalent*v + g.eta*delta_mu*delta_mu + 2*g.beta[0];
					// NEXT LINE HAS 1 OR g.eta ???
					new_sigma2[i] = ns/(nequivalent*a + g.eta + 2*(g.alpha-1));
				}
			}

			double suma = 0;
			for ( i = 0; i <  approximation.ncomponents(); i++ )
				suma += new_alpha[i];
			for ( i = 0; i <  approximation.ncomponents(); i++ )
				new_alpha[i] /= suma;

			for ( i = 0; i <  approximation.ncomponents(); i++ )
			{
				approximation.mix_proportions[i] = new_alpha[i];
				((Gaussian)approximation.components[i]).mu[0] = new_mu[i];
				S[0][0] = new_sigma2[i];
				((Gaussian)approximation.components[i]).set_Sigma( S );
			}

			// Here's an easy step toward simplification:
			// throw out mixture components which have very small weight.

			final double MIN_MIX_PROPORTION = 5e-3;
			Vector too_light = new Vector();

			for ( i = 0; i < approximation.ncomponents(); i++ )
			{
				if ( approximation.mix_proportions[i] < MIN_MIX_PROPORTION )
				{
					too_light.addElement( new Integer(i) );
				}
			}

			approximation.remove_components( too_light, null );

			// Here's another easy one: throw out a component if it
			// appears to be nearly the same as some other component.

			Vector duplicates = new Vector(), duplicated = new Vector();

			for ( i = 0; i < approximation.ncomponents(); i++ )
			{
				double m_i = approximation.components[i].expected_value();
				double s_i = approximation.components[i].sqrt_variance();

				for ( j = i+1; j < approximation.ncomponents(); j++ )
				{
					double m_j = approximation.components[j].expected_value();
					double s_j = approximation.components[j].sqrt_variance();

					if ( s_i == 0 || s_j == 0 ) continue;

					double dm = Math.abs(m_i-m_j), rs = s_i/s_j, s_ij = Math.sqrt( 1/( 1/(s_i*s_i) + 1/(s_j*s_j) ) );

					if ( dm/s_ij < 2.5e-1 && rs > 1 - 2e-1 && rs < 1 + 2e-1 )
					{
						duplicates.addElement( new Integer(i) );
						duplicated.addElement( new Integer(j) );
						break; // go on to next i
					}
				}
			}

			approximation.remove_components( duplicates, duplicated );

			if ( debug )
			{
				System.err.println( "iteration: "+k );
				System.err.print( "approximation mixing proportions: " ); 
				Matrix.pretty_output( approximation.mix_proportions, System.err, " " );
				System.err.println("");

				System.err.print( "approximation means: " );
				for ( i = 0; i <  approximation.ncomponents(); i++ )
					System.err.print( ((Gaussian)approximation.components[i]).mu[0]+" " );
				System.err.println("");

				System.err.print( "approximation std devs: " );
				for ( i = 0; i <  approximation.ncomponents(); i++ )
					System.err.print( Math.sqrt( ((Gaussian)approximation.components[i]).get_Sigma()[0][0] )+" " );
				System.err.println("");
			}

			double ce = ceih.do_integral();
			System.err.println( "CROSS ENTROPY["+k+"]: "+ce+"; target entropy: "+te+"\n" );
		}

// !!! double[] x = new double[1];
// !!! int N = 200;
// !!! double dx = (supports[0][1]-supports[0][0])/N;
// !!! for ( i = 0; i < N; i++ ) {
// !!! x[0] = (i+0.5)*dx + supports[0][0];
// !!! double papprox = approximation.p(x);
// !!! double ptarget = target.p(x);
// !!! System.err.println( "  "+x[0]+"  "+ptarget+"  "+papprox ); }

		return approximation;
	}

	/** A main program to carry out the mixture approximation algorithm.
	  * Usage:
	  * <pre>
	  *   java riso.approximation.GaussianMixApproximation target approx0 x0 x1 N
	  * </pre>
	  * where
	  * <ol>
	  * <li> <tt>target</tt> File containing description of the target distribution.
	  * <li> <tt>approx0</tt> File containing description of the initial approximation.
	  * <li> <tt>x0, x1</tt> Endpoints of the interval over which to construct the approximation.
	  * <li> <tt>N</tt> Equivalent sample size. This is essentially a regularization parameter.
	  * </ol>
	  */
	public static void main( String[] args )
	{
		System.err.println( "target file: "+args[0] );
		System.err.println( "initial approx file: "+args[1] );
		System.err.println( "target support: ["+args[2]+", "+args[3]+"]" );
		System.err.println( "equivalent sample size: "+args[4] );

		try
		{
			int i;
			FileInputStream fis = new FileInputStream( args[0] );
			SmarterTokenizer p_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			Distribution p = null;

			try 
			{
				p_st.nextToken();
				Class p_class = java.rmi.server.RMIClassLoader.loadClass( p_st.sval );
				p = (Distribution) p_class.newInstance();
				p_st.nextBlock();
				p.parse_string( p_st.sval );
			}
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: attempt to construct target failed: "+e );
				System.exit(1);
			}

			fis.close(); fis = new FileInputStream( args[1] );
			SmarterTokenizer q_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			MixGaussians q = null;

			try
			{
				q_st.nextToken();
				Class q_class = java.rmi.server.RMIClassLoader.loadClass( q_st.sval );
				q = (MixGaussians) q_class.newInstance();
				q_st.nextBlock();
				q.parse_string( q_st.sval );
			}
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: attempt to construct approximation failed: "+e );
				System.exit(1);
			}

			double[][] support = new double[1][2];
			support[0][0] = Double.parseDouble( args[2] );
			support[0][1] = Double.parseDouble( args[3] );
			GaussianMixApproximation.nequivalent = Double.parseDouble( args[4] );

			GaussianMixApproximation.debug = true;

			try { q = GaussianMixApproximation.do_approximation( p, q, support, 1e-2 ); }
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: do_approximation failed; "+e );
				System.exit(1);
			}

			System.out.print( "final approximation:\n"+q.format_string("") );
			System.out.print( "\t"+"effective support of target: " );
			System.out.println( p.effective_support(1e-4)[0]+", "+p.effective_support(1e-4)[1] );
			System.out.print( "\t"+"effective support of approx: " );
			System.out.println( q.effective_support(1e-4)[0]+", "+q.effective_support(1e-4)[1] );

			double x[] = new double[1];
			for ( i = 0; i < 50; i++ )
			{
				x[0] = support[0][0]+i*(support[0][1]-support[0][0])/50.0;
				System.out.println( "x: "+x[0]+" p: "+p.p(x)+" q: "+q.p(x)+" q/p: "+ q.p(x)/p.p(x) );
			}
		}
		catch (Exception e)
		{
			System.err.println( "GaussianMixApproximation.main: something went ker-blooey: "+e );
		}

		System.exit(1);
	}
}

class MixingProportionIntegrand implements Callback_1d
{
	int q_index;
	Distribution target;
	MixGaussians approximation;
	double[] x1 = new double[1];

	MixingProportionIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	/** Computes <tt>target.p(x) * approximation.responsibility(q_index, x)</tt>.
	  */
	public double f( double x ) throws Exception
	{
		x1[0] = x;
		return target.p( x1 ) * approximation.responsibility( q_index, x1 );
	}
}

class MeanIntegrand implements Callback_1d
{
	int q_index;
	Distribution target;
	MixGaussians approximation;
	double[] x1 = new double[1];

	MeanIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	/** Computes <tt>x * target.p(x) * approximation.responsibility(q_index, x)</tt>.
	  */
	public double f( double x ) throws Exception
	{
		x1[0] = x;
		return x * target.p(x1) * approximation.responsibility( q_index, x1 );
	}
}

class VarianceIntegrand implements Callback_1d
{
	int q_index;
	Distribution target;
	MixGaussians approximation;
	double[] x1 = new double[1];

	VarianceIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	/** Computes <tt>dx^2 * target.p(x) * approximation.responsibility(q_index, x)</tt>,
	  * with <tt>dx = x - mu</tt> and <tt>mu</tt> the mean of the component indicated by <tt>q_index</tt>.
	  */
	public double f( double x ) throws Exception
	{
		x1[0] = x;
		double mu = ((Gaussian)approximation.components[q_index]).mu[0];
		double dx = x - mu;
		return dx*dx * target.p(x1) * approximation.responsibility(q_index,x1);
	}
}
