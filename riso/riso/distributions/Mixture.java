package riso.distributions;
import java.io.*;
import java.rmi.*;
import java.util.*;
import numerical.*;
import SmarterTokenizer;

/** This class represents an additive mixture of distributions.
  * The descriptive data which can be changed without causing the interface
  * functions to break down is public. The other data is protected.
  * Included in the public data are the regularization parameters. 
  * @see Distribution
  */
public class Mixture extends AbstractDistribution
{
	/** If all components are the same type, this variable can be set
	  * to indicate the common type of all components.
	  */
	public Class common_type = null;

	/** Set the maximum number of iterations for component updates. 
	  * @see update
	  */
	transient public int component_niter_max = -1;	// -1 means ``default.''

	/** Set the stopping criterion for component updates.
	  * @see update
	  */
	transient public double component_stopping_criterion = -1;	// -1 means ``default.''

	/** Dimensionality of the space in which the distribution lives.
	  */
	protected int ndims;

	/** Number of component distributions in the mixture.
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
	public Distribution[] components;

	/** Default maximum number of EM iterations.
	  */
	public final static int NITER_MAX = 1000;

	/** Default stopping criterion. See description of <tt>update</tt>,
	  */
	public final static double STOPPING_CRITERION = 1e-4;

	/** Constructs a new mixture with the specified number of
	  * dimensions and components. Components can be set up one by one
	  * since <tt>Mixture.components</tt> is <tt>public</tt>.
	  * This constructor allocates the arrays to which member variables
	  * refer, and fills them with neutral values; except for the
	  * <tt>components</tt> array, which is left empty.
	  */
	public Mixture( int ndimensions, int ncomponents ) throws RemoteException
	{
		this.ndims = ndimensions;
		this.ncomponents = ncomponents;

		components = new Distribution[ ncomponents ];
		mix_proportions = new double[ ncomponents ];
		gamma = new double[ ncomponents ];

		for ( int i = 0; i < ncomponents; i++ )
		{
			mix_proportions[i] = 1.0/ncomponents;
			gamma[i] = 1;
		}
	}

	/** This do-nothing constructor exists only to declare the exception.
	  * @throws RemoteException
	  */
	public Mixture() throws RemoteException {}

	/** Make a deep copy of this mixture distribution and return the copy.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		Mixture copy = null;
		try { copy = new Mixture(); }
		catch (RemoteException e) { throw new CloneNotSupportedException( "Mixture.remote_clone failed: "+e ); }

		copy.ndims = ndims;
		copy.ncomponents = ncomponents;
		copy.mix_proportions = (double[]) mix_proportions.clone();
		copy.gamma = (double[]) gamma.clone();

		copy.components = new Distribution[ncomponents];
		for ( int i = 0; i < ncomponents; i++ )
			try { copy.components[i] = (Distribution) components[i].remote_clone(); }
			catch (CloneNotSupportedException e) 
			{
				throw new RemoteException( "Mixture.remote_clone: unexpected: "+e );
			}

		return copy;
	}

	/** Return the dimensionality of the space in which the distribution lives.
	  */
	public int ndimensions() { return ndims; }

	/** Return the number of components in the mixture. Assume that none
	  * of the components is null.
	  */
	public int ncomponents() { return ncomponents; }

    /** Computes <tt>p( bump == i | x )</tt>, a.k.a. the ``responsibility''
	  * of bump <tt>i</tt> for datum <tt>x</tt>.
	  */
	public double responsibility( int i, double[] x )
	{
		try { return mix_proportions[i] * components[i].p(x) / p(x); }
		catch (RemoteException e)
		{
			throw new RuntimeException( "Mixture.responsibilty: RemoteException thrown from local method call. "+e );
		}
	}

	/** Compute the density at the point <code>x</code>.
	  * This is the sum of the densities of each of the components,
	  * weighted by their respective mixing proportions.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x ) throws RemoteException
	{
		double sum = 0;
		for ( int i = 0; i < ncomponents; i++ )
			sum += mix_proportions[i] * components[i].p(x);

		return sum;
	}

	/** Return an instance of a random variable from this distribution.
	  * A component is selected according to the mixing proportions,
	  * then a random variable is generated from that component.
	  */
	public double[] random() throws RemoteException
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

	/** Read a description of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Mixture.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions" ) )
				{
					st.nextToken();
					ndims = Format.atoi( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ncomponents" ) )
				{
					st.nextToken();
					ncomponents = Format.atoi( st.sval );
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
						mix_proportions[i] = Format.atof( st.sval );
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
						gamma[i] = Format.atof( st.sval );
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

					components = new Distribution[ ncomponents ];

					for ( int i = 0; i < ncomponents; i++ )
					{
						// The next token must be the name of a class.
						try
						{
							st.nextToken();
							Class component_class = Class.forName( st.sval );
							components[i] = (Distribution) component_class.newInstance();
						}
						catch (Exception e)
						{
							throw new IOException( "Mixture.pretty_input: attempt to create component failed:\n"+e );
						}

						st.nextBlock();
						components[i].parse_string( st.sval );
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
			throw new IOException( "Mixture.pretty_input: attempt to read mixture failed:\n"+e );
		}
	}

	/** Write a description of this distribution to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		String result = "";
		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = leading_ws+"\t";
		result += more_leading_ws+"ndimensions "+ndims+"\n";
		result += more_leading_ws+"ncomponents "+ncomponents+"\n";

		int i;

		result += more_leading_ws+"mixing-proportions { ";
		for ( i = 0; i < ncomponents; i++ )
			result += mix_proportions[i]+" ";
		result += "}"+"\n";

		result += more_leading_ws+"regularization-gammas { ";
		for ( i = 0; i < ncomponents; i++ )
			result += gamma[i]+" ";
		result += "}"+"\n";

		result += more_leading_ws+"components\n"+more_leading_ws+"{"+"\n";

		String still_more_ws = more_leading_ws+"\t";
		for ( i = 0; i < ncomponents; i++ )
		{
			result += still_more_ws+"% Component "+i+"\n";
			result += still_more_ws+components[i].format_string( still_more_ws );
			result += "\n";
		}

		result += more_leading_ws+"}"+"\n";
		result += leading_ws+"}"+"\n";
		return result;
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
	  *   for its component distributions.
	  * @param niter_max Maximum number of pairs of E- and M-steps. While the
	  *   the E-step is relatively fast, the M-step may involve arbitrarily
	  *   complex model fitting, such as neural network training.
	  * @param stopping_criterion If the maximum absolute-value change in mixing proportions from
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
		double  nll = 1e11;

		nll = 0;
		for ( j = 0; j < x.length; j++ )
			nll += -Math.log( p( x[j] ) );
		System.err.println( "Mixture.update: initial neg. log likelihood: "+nll );

		double prev_nll = 1e12;
		double[] prev_mix_proportions = (double[]) mix_proportions.clone();
		double max_abs_diff_mix_proportions = -1;
		int niter;

		for ( niter = 0; niter < niter_max && (niter == 0 || max_abs_diff_mix_proportions > stopping_criterion); ++niter )
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

				// System.err.println( "Mixture.update: ---------- update "+i+"'th component; current mixing proportion: "+kappa[i] );
// double min_h = 1e100, max_h = -1e100;
// for ( k = 0; k < m; k++ )
	// if ( h[i][k] < min_h )
		// min_h = h[i][k];
	// else if ( h[i][k] > max_h )
		// max_h = h[i][k];
// System.err.println( "--------- component["+i+"]: min resp.: "+min_h+" max resp.: "+max_h );

				// Slight hack here -- use member data to set the parameters which control
				// the updates for the components. These aren't arguments to this method
				// because this is an implementation of an interface method; we can't change
				// the arguments.

				components[i].update( x, h[i], component_niter_max, component_stopping_criterion );
			}

			prev_nll = nll;
			nll = 0;
			for ( j = 0; j < x.length; j++ )
				nll += -Math.log( p( x[j] ) );

			double nlp = 0;		// negative log prior
			for ( j = 0; j < kappa.length; j++ )
				nlp += -(gamma[j]-1) * Math.log( kappa[j] );
			for ( j = 0; j < components.length; j++ )
				nlp += -components[j].log_prior();

			System.err.println( "Mixture.update: "+niter+" iterations, neg. log likelihood: "+nll );
			System.err.println( "  neg. log prior: "+nlp+"  neg. log posterior: "+(nll+nlp) );

			// Now see how much the mixing proportions are changing. 
			// If there is a small absolute difference, we will stop.

			double a;
			max_abs_diff_mix_proportions = -1;
			for ( j = 0; j < mix_proportions.length; j++ )
				if ( (a = Math.abs(mix_proportions[j]-prev_mix_proportions[j])) > max_abs_diff_mix_proportions )
					max_abs_diff_mix_proportions = a;

			for ( j = 0; j < mix_proportions.length; j++ )
				prev_mix_proportions[j] = mix_proportions[j];
		}

		System.err.println( "Mixture.update: "+niter+" iterations, final neg. log likelihood: "+nll );

		return nll;
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Mixture.expected_value: "+ndims+" dimensions is too many." );

		double sum = 0;

		for ( int i = 0; i < mix_proportions.length; i++ )
			sum += mix_proportions[i] * components[i].expected_value();

		return sum;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Mixture.expected_value: "+ndims+" dimensions is too many." );

		double s, m, sum = 0, sum2 = 0;

		for ( int i = 0; i < mix_proportions.length; i++ )
		{
			m = components[i].expected_value();
			s = components[i].sqrt_variance();
			sum += mix_proportions[i] * m;
			sum2 += mix_proportions[i] * ( s*s + m*m );
		}

		double var = sum2 - sum*sum;
		return Math.sqrt(var);
	}

	/** Returns the support of this distribution, if it is a finite interval;
	  * otherwise returns an interval which contains almost all of the mass.
	  * @param epsilon If an approximation is made, this much mass or less
	  *   lies outside the interval which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Mixture.expected_value: "+ndims+" dimensions is too many." );
		
		double[] mix_support = new double[2];
		mix_support[0] = +1e100;
		mix_support[1] = -1e100;

		for ( int i = 0; i < mix_proportions.length; i++ )
		{
			if ( mix_proportions[i] == 0 ) continue;

			// CORRECT ADJUSTMENT HERE ???
			double epsilon_i = epsilon/mix_proportions[i]/mix_proportions.length;

			if ( epsilon_i >= 1 ) continue;		// when mixing proportion is very small, ignore component entirely

			double[] support_i;

			try { support_i = components[i].effective_support( epsilon_i ); }
			catch (SupportNotWellDefinedException e)
			{
				throw new SupportNotWellDefinedException( "Mixture.effective_support: component["+i+"]: "+e );
			}
			catch (RemoteException e2)
			{
				throw new RemoteException( "Mixture.effective_support: failed attempt to compute support of component "+i+"; type is "+components[i].getClass() );
			}
// System.err.println( "Mixture.effective_support: ["+i+"]: "+support_i[0]+", "+support_i[1] );
			if ( support_i[0] < mix_support[0] ) mix_support[0] = support_i[0];
			if ( support_i[1] > mix_support[1] ) mix_support[1] = support_i[1];
		}

// System.err.println( "Mixture.effective_support: final: "+mix_support[0]+", "+mix_support[1] );
		return mix_support;
	}

	/** Returns a Gaussian mixture containing three bumps per non-Gaussian
	  * component, and one bump per Gaussian.
	  *
	  * @param support This argument is ignored.
	  * @see Distribution.initial_mix
	  */
	public MixGaussians initial_mix( double[] support ) throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Mixture.initial_mix: "+ndims+" dimensions is too many." );

		// First count up the Gaussian and non-Gaussian components.

		int i, j, ngaussian = 0;
		for ( i = 0; i < ncomponents; i++ )
			if ( components[i] instanceof Gaussian )
				++ngaussian;

		int nnongaussian = ncomponents - ngaussian;

		// Now allocate 3 bumps per non-Gaussian and 1 bump per Gaussian.

		int nmix = 3*nnongaussian + ngaussian;
		MixGaussians mix = new MixGaussians( 1, nmix );

		for ( i = 0, j = 0; i < ncomponents; i++ )
		{
			if ( components[i] instanceof Gaussian )
				try { mix.components[j++] = (Gaussian) components[i].remote_clone(); }
				catch (CloneNotSupportedException e) { throw new RemoteException( "Mixture.initial_mix: unexpected: "+e ); }
			else
			{
				double m = components[i].expected_value();
				double s = components[i].sqrt_variance();

				mix.components[j++] = new Gaussian( m, s );
				mix.components[j++] = new Gaussian( m-s, s );
				mix.components[j++] = new Gaussian( m+s, s );
			}
		}

		return mix;
	}

	/** Remove some components from the mixture. After removing the
	  * components on the list <tt>to_remove</tt>, the mixing proportions
	  * of the remaining components are normalized to unity.
	  */
	public void remove_components( Vector to_remove )
	{
		int i, j, k, nremove = to_remove.size();

		Distribution[] remaining_comps = new Distribution[ ncomponents-nremove ];
		double[] remaining_mix_props = new double[ ncomponents-nremove ];
		double[] remaining_gamma = new double[ ncomponents-nremove ];

		double weight_total = 0;

		for ( i = 0, j = 0; i < ncomponents; i++ )
		{
			boolean do_remove = false;
			for ( k = 0; k < nremove; k++ )
				if ( components[i] == to_remove.elementAt(k) )
				{
					do_remove = true;
					break;
				}

			if ( do_remove )
			{
System.err.println( "Mixture.remove_components: rm component["+i+"], weight: "+mix_proportions[i] );
			}
			else
			{
				remaining_comps[j] = components[i];
				remaining_mix_props[j] = mix_proportions[i];
				remaining_gamma[j] = gamma[i];

				weight_total += mix_proportions[i];
				++j;
			}
		}

		components = remaining_comps;
		mix_proportions = remaining_mix_props;
		gamma = remaining_gamma;
		ncomponents -= nremove;

		for ( i = 0; i < ncomponents; i++ )
			mix_proportions[i] /= weight_total;

System.err.println( "Mixture.remove_components: "+ncomponents+" remain." );
	}
}
