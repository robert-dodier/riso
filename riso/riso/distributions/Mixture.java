/* Copyright (c) 1997 Robert Dodier and the Joint Center for Energy Management,
 * University of Colorado at Boulder. All Rights Reserved.
 *
 * By copying this software, you agree to the following:
 *  1. This software is distributed for non-commercial use only.
 *     (For a commercial license, contact the copyright holders.)
 *  2. This software can be re-distributed at no charge so long as
 *     this copyright statement remains intact.
 *
 * ROBERT DODIER AND THE JOINT CENTER FOR ENERGY MANAGEMENT MAKE NO
 * REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.
 * ROBERT DODIER AND THE JOINT CENTER FOR ENERGY MANAGEMENT SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY YOU AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package densities;
import java.io.*;

/** This class represents an additive mixture of densities.
  * The descriptive data which can be changed without causing the interface
  * functions to break down is public. The other data is protected.
  * Included in the public data are the regularization parameters. 
  * @see Density
  */
public class Mixture implements Density, Serializable, Cloneable
{
	/** Dimensionality of the space in which the density lives.
	  */
	protected int ndims;

	/** Number of component densities in the mixture.
	  */
	protected int ncomponents;

	/** Mixing proportions; these must be nonnegative and sum to 1.
	  */
	public double[] mix_proportions;

	/** Regularization parameters.
	  */
	public double[] gamma;

	/** List of mixture components.
	  */
	public Density[] components;

	/** Default maximum number of EM iterations.
	  */
	public final static int NITER_MAX = 1000;

	/** Default stopping criterion -- EM algorithm halts if change in
	  * negative log-likelihood is less than this.
	  */
	public final static double STOPPING_CRITERION = 1e-4;

	/** Flag telling whether this object has been set up yet.
	  */
	protected boolean is_ok = false;

	/** Make a deep copy of this mixture density object and return the copy.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Mixture copy = new Mixture();
		copy.ndims = ndims;
		copy.ncomponents = ncomponents;
		copy.mix_proportions = (double[]) mix_proportions.clone();
		copy.gamma = (double[]) gamma.clone();

		copy.components = new Density[ncomponents];
		for ( int i = 0; i < ncomponents; i++ )
			copy.components[i] = (Density) components[i].clone();

		copy.is_ok = is_ok;
		return copy;
	}

	/** Return the dimensionality of the space in which the density lives.
	  */
	public int ndimensions() { return ndims; }

	/** Return the number of components in the mixture. Assume that none
	  * of the components is null.
	  */
	public int ncomponents() { return ncomponents; }

	/** Compute the density at the point <code>x</code>.
	  * This is the sum of the densities of each of the components,
	  * weighted by their respective mixing proportions.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x )
	{
		double sum = 0;
		for ( int i = 0; i < ncomponents; i++ )
			sum += mix_proportions[i] * components[i].p(x);

		return sum;
	}

	/** Return an instance of a random variable from this density.
	  * A component is selected according to the mixing proportions,
	  * then a random variable is generated from that component.
	  */
	public double[] random()
	{
		double sum = 0, r = Math.random();
		for ( int i = 0; i < ncomponents-1; i++ )
		{
			sum += mix_proportions[i];
			if ( r < sum )
				return components[i].random();
		}

		return components[ncomponents-1].random();
	}

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  */
	public void pretty_input( InputStream is ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			Reader r = new BufferedReader(new InputStreamReader(is));
			StreamTokenizer st = new StreamTokenizer(r);
			st.wordChars( '$', '%' );
			st.wordChars( '?', '@' );
			st.wordChars( '[', '_' );
			st.ordinaryChar('/');
			st.slashStarComments(true);
			st.slashSlashComments(true);

			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Mixture.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions" ) )
				{
					st.nextToken();
					ndims = (int) st.nval;
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ncomponents" ) )
				{
					st.nextToken();
					ncomponents = (int) st.nval;
					mix_proportions = new double[ ncomponents ];
					gamma = new double[ ncomponents ];

					for ( int i = 0; i < ncomponents; i++ )
					{
						mix_proportions[i] = 1.0/ncomponents;
						gamma[i] = 1;
					}
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "mixing-proportions" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "Mixture.pretty_input: ``mixing-proportions'' lacks opening bracket." );

					for ( int i = 0; i < ncomponents; i++ )
					{
						st.nextToken();
						mix_proportions[i] = st.nval;
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "Mixture.pretty_input: ``mixing-proportions'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "regularization-gammas" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "Mixture.pretty_input: ``regularization-gammas'' lacks opening bracket." );

					for ( int i = 0; i < ncomponents; i++ )
					{
						st.nextToken();
						gamma[i] = st.nval;
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "Mixture.pretty_input: ``regularization-gammas'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "components" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "Mixture.pretty_input: ``components'' lacks opening bracket." );

					components = new Density[ ncomponents ];

					for ( int i = 0; i < ncomponents; i++ )
					{
						// The token must be the name of a class.
						// The component will construct another tokenizer
						// from ``is'', but this is OK.

						try
						{
							Class component_class = Class.forName( st.sval );
							components[i] = (Density) component_class.newInstance();
						}
						catch (Exception e)
						{
							throw new IOException( "Mixture.pretty_input: attempt to create component failed:\n"+e );
						}

						components[i].pretty_input( is );
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "Mixture.pretty_input: ``components'' lacks a closing bracket." );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}

			if ( ! found_closing_bracket )
				throw new IOException( "Mixture.pretty_input: no closing bracket." );
		}
		catch (IOException e)
		{
			throw new IOException( "Mixture.pretty_input: attempt to read network failed:\n"+e );
		}

		is_ok = true;
	}

	/** Write a description of this density model to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		if ( !is_ok )
			throw new IOException( "Mixture.pretty_output: attempt to output a mixture before it is set up." );

		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.println( leading_ws+this.getClass().getName()+"\n"+leading_ws+"{" );
		String more_leading_ws = leading_ws+"\t";
		dest.println( more_leading_ws+"ndimensions "+ndims );
		dest.println( more_leading_ws+"ncomponents "+ncomponents );

		int i;

		dest.print( more_leading_ws+"mixing-proportions { " );
		for ( i = 0; i < ncomponents; i++ )
			dest.print( mix_proportions[i]+" " );
		dest.println( "}" );

		dest.print( more_leading_ws+"regularization-gammas { " );
		for ( i = 0; i < ncomponents; i++ )
			dest.print( gamma[i]+" " );
		dest.println( "}" );

		dest.println( more_leading_ws+"components\n"+more_leading_ws+"{" );

		String still_more_ws = more_leading_ws+"\t";
		for ( i = 0; i < ncomponents; i++ )
		{
			dest.println( still_more_ws+"/* Component "+i+" */" );
			components[i].pretty_output( os, still_more_ws );
			dest.println("");
		}

		dest.println( more_leading_ws+"}" );
		dest.println( leading_ws+"}" );
	}

	/** Update the mixture with the given data, using the EM algorithm as
	  * described in Sec. 4.3 of Dempster, Laird, and Rubin [1]. This amounts
	  * to is assigning responsibility to each component according to how
	  * well it accounts for each datum (E-step), then doing a update of each
	  * component (M-step), weighting the data by the responsibility. The M-step
	  * could use either maximum likelihood or maximum penalized likelihood.
	  * The process is repeated to convergence, which is guaranteed by the
	  * results of Dempster et al.
	  *
	  * Equations for updating the mixing proportions using penalized maximum
	  * likelihood are described by Ormoneit and Tresp [2].
	  *
	  * References:
	  * [1] Dempster, A., N. Laird, and D. Rubin. (1977) ``Maximum Likelihood
	  *   from Incomplete Data via the {\sc em} Algorithm.'' <em> J. Royal
	  *   Statistical Soc. B,</em> 39(1):1--38.
	  * [2] Ormoneit, D., and V. Tresp. (1996) ``Improved Gaussian Mixture
	  *   Density Estimates Using Bayesian Penalty Terms and Network
	  *   Averaging.'' <em>Advances in Neural Information Processing Systems 8,</em>
	  *   D. Touretzky, M. Mozer, and M. Hasselmo, eds. Cambridge, MA: MIT Press.
	  *
	  * @param x The data. It is assumed no data are missing. This array has
	  *   #columns equal to the dimensionality of the model, and #rows equal
	  *   to the number of data.
	  * @param responsibility The <code>i</code>'th element of this array
	  *   is the probability that this mixture generated the corresponding
	  *   datum. Unless this mixture is a component of a super-mixture or
	  *   something else is going on, these numbers will all be 1; in this
	  *   case this array can be <code>null</code>. Do not confuse this input
	  *   array with the responsibilities that <code>update</code> will compute
	  *   for its component densities.
	  * @param niter_max Maximum number of pairs of E- and M-steps. While the
	  *   the E-step is relatively fast, the M-step may involve arbitrarily
	  *   complex model fitting, such as neural network training.
	  * @param stopping_criterion If the change in negative log-likelihood from
	  *   one iteration to the next is less than <code>stopping_criterion</code>
	  *   then the iteration stops.
	  * @return The negative log-likelihood at the end of the iteration.
	  * @throws Exception If the update process fails for any reason.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		if ( niter_max < 0 ) niter_max = NITER_MAX;
		if ( stopping_criterion < 0 ) stopping_criterion = STOPPING_CRITERION;

		int j;
		double  nll = 0;
		for ( j = 0; j < x.length; j++ )
			nll += -Math.log( p( x[j] ) );
		System.err.println( "Mixture.update: initial neg. log likelihood: "+nll );

		int niter = 0;
		double prev_nll = 1e12;

		while ( niter++ < niter_max && (prev_nll-nll) > stopping_criterion )
		{
			// Notation follows Ormoneit and Tresp, ``Improved Gaussian Mixture...''
			// h == responsibility, kappa == mixing proportions, gamma == regularization
			// parameters for mixing proportions. h[i][k] == p( model i | x[k] ).

			// Compute responsibility. This is the E step.
			
			int i, k, m = x.length;
			double total;
			double[] kappa = mix_proportions;
			double[][] h = new double[ncomponents][m];

			for ( k = 0; k < m; k++ )
			{
				total = 0;
				for ( i = 0; i < ncomponents; i++ )
				{
					h[i][k] = kappa[i] * components[i].p( x[k] );
					total += h[i][k];
				}
				for ( i = 0; i < ncomponents; i++ )
					h[i][k] /= total;
			}

			// Compute updated parameters for each component. This is the M step.

			double  sum_gamma = 0;
			for ( i = 0; i < ncomponents; i++ )
				sum_gamma += gamma[i];

			for ( i = 0; i < ncomponents; i++ )
			{
				kappa[i] = 0;
				for ( k = 0; k < m; k++ )
					kappa[i] += h[i][k];
				kappa[i] += gamma[i] - 1;
				kappa[i] /= (m + sum_gamma - ncomponents);

				// Who knows what appropriate values for niter_max and 
				// stopping_criterion might be -- ask for default values.

				components[i].update( x, h[i], -1, -1 );
			}

			nll = 0;
			for ( j = 0; j < x.length; j++ )
				nll += -Math.log( p( x[j] ) );
			System.err.println( "Mixture.update: "+niter+"iterations, neg. log likelihood: "+nll );
		}

		System.err.println( "Mixture.update: "+niter+"iterations, final neg. log likelihood: "+nll );

		return nll;
	}
}
