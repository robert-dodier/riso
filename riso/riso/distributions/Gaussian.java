package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

/** A Gaussian (normal) distribution.
  * The descriptive data which can be changed without causing the interface
  * functions to break down is public. The other data is protected.
  * Included in the public data are the regularization parameters. 
  * If not otherwise specified, the prior mean, prior covariance, and other
  * regularization parameters are given neutral values, so that they have
  * no effect on parameter estimation.
  */
public class Gaussian extends AbstractDistribution
{
	/** Dimensionality of the space in which the distribution lives.
	  */
	protected int ndims;

	/** Mean vector of the distribution.
	  */
	public double[] mu;

	/** Covariance matrix of the distribution. If this ever changes, its inverse,
	  * determinant, and Cholesky decomposition must be recomputed.
	  */
	protected double[][] Sigma;

	/** Inverse of the covariance matrix. 
	  */
	protected double[][] Sigma_inverse;

	/** Determinant of the covariance matrix.
	  */
	protected double det_Sigma;

	/** Lower-triangular Cholesky decomposition of the covariance matrix.
	  */
	protected double[][] L_Sigma;

	/** Prior mean.
	  */
	public double[] mu_hat;

	/** Prior covariance; this is supposed to be a matrix,
	  * but we assume it's diagonal, and just store the diagonal.
	  */
	public double[] beta;

	/** Scale parameter for prior covariance -- ???
	  */
	public double alpha;

	/** Scale parameter for prior mean -- <code>eta -> 0</code>
	  * implies very broad prior, i.e. little effect on posterior mean.
	  */
	public double eta;

	/** Create an empty, unusable object. <code>pretty_input</code> can be used
	  * read in parameters.
	  */
	public Gaussian() throws RemoteException { mu = null; Sigma = null; }

	/** Create a <code>Gaussian</code> with the given mean and covariance.
	  * Regularization parameters are given neutral values.
	  * @param mu_in Mean -- a vector.
	  * @param Sigma_in Covariance -- a matrix.
	  */
	public Gaussian( double[] mu_in, double[][] Sigma_in ) throws IllegalArgumentException, RemoteException
	{
		ndims = mu_in.length;

		if ( Sigma_in.length != ndims || Sigma_in[0].length != ndims )
			throw new IllegalArgumentException( "In Gaussian constructor: Sigma_in not same size as mu_in" );

		mu = (double[]) mu_in.clone();
		Sigma = Matrix.copy( Sigma_in );

		Sigma_inverse = Matrix.inverse( Sigma );
		det_Sigma = Matrix.determinant( Sigma );
		L_Sigma = Matrix.cholesky( Sigma );

		mu_hat = new double[ndims];		// initialized to zeros
		beta = new double[ndims];		// initialized to zeros
		alpha = ndims/2.0;
		eta = 0;
	}

	/** Return the number of dimensions of this <code>Gaussian</code>.
	  */
	public int ndimensions() { return ndims; }

	/** Read in a <code>Gaussian</code> from an input stream. 
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		// If "ndimensions" is omitted, assume 1

		ndims = 1;
		mu = new double[ndims];
		Sigma = new double[ndims][ndims];
		mu_hat = new double[ndims];
		beta = new double[ndims];
		alpha = ndims/2.0;
		eta = 0;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Gaussian.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions" ) )
				{
					st.nextToken();
					ndims = Format.atoi( st.sval );
					mu = new double[ndims];
					Sigma = new double[ndims][ndims];
					mu_hat = new double[ndims];
					beta = new double[ndims];
					alpha = ndims/2.0;
					eta = 0;
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "mean" ) )
				{
					if ( ndims == 1 )
					{
						st.nextToken();
						mu[0] = Format.atof( st.sval );
					}
					else
					{
						st.nextToken();
						if ( st.ttype != '{' ) throw new IOException( "Gaussian.pretty_input: ``mean'' lacks opening bracket." );

						for ( int i = 0; i < ndims; i++ )
						{
							st.nextToken();
							mu[i] = Format.atof( st.sval );
						}

						st.nextToken();
						if ( st.ttype != '}' ) throw new IOException( "Gaussian.pretty_input: ``mean'' lacks closing bracket." );
					}
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "covariance" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "Gaussian.pretty_input: ``covariance'' lacks opening bracket." );

					for ( int i = 0; i < ndims; i++ )
						for ( int j = 0; j < ndims; j++ )
						{
							st.nextToken();
							Sigma[i][j] = Format.atof( st.sval );
						}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "Gaussian.pretty_input: ``covariance'' lacks closing bracket." );

					Sigma_inverse = Matrix.inverse( Sigma );
					det_Sigma = Matrix.determinant( Sigma );
					L_Sigma = Matrix.cholesky( Sigma );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "prior-mean" ) )
				{
					if ( ndims == 1 )
					{
						st.nextToken();
						mu_hat[0] = Format.atof( st.sval );
					}
					else
					{
						st.nextToken();
						if ( st.ttype != '{' ) throw new IOException( "Gaussian.pretty_input: ``prior-mean'' lacks opening bracket." );

						for ( int i = 0; i < ndims; i++ )
						{
							st.nextToken();
							mu_hat[i] = Format.atof( st.sval );
						}

						st.nextToken();
						if ( st.ttype != '}' ) throw new IOException( "Gaussian.pretty_input: ``prior-mean'' lacks closing bracket." );
					}
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "prior-variance" ) )
				{
					if ( ndims == 1 )
					{
						st.nextToken();
						beta[0] = Format.atof( st.sval );
					}
					else
					{
						st.nextToken();
						if ( st.ttype != '{' ) throw new IOException( "Gaussian.pretty_input: ``prior-variance'' lacks opening bracket." );

						for ( int i = 0; i < ndims; i++ )
						{
							st.nextToken();
							beta[i] = Format.atof( st.sval );
						}

						st.nextToken();
						if ( st.ttype != '}' ) throw new IOException( "Gaussian.pretty_input: ``prior-variance'' lacks closing bracket." );
					}
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "prior-mean-scale" ) )
				{
					st.nextToken();
					eta = Format.atof( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "prior-variance-scale" ) )
				{
					st.nextToken();
					alpha = Format.atof( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "std-deviation" ) )
				{
					if ( ndims != 1 ) 
						throw new IOException( "Gaussian.pretty_input: ``std-deviation'' doesn't make sense when #dimensions is "+ndims );

					st.nextToken();
					double stddev = Format.atof( st.sval );
					Sigma[0][0] = stddev*stddev;
					Sigma_inverse = Matrix.inverse( Sigma );
					det_Sigma = Matrix.determinant( Sigma );
					L_Sigma = Matrix.cholesky( Sigma );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "Gaussian.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Gaussian.pretty_input: no closing bracket on input." );
	}

	/** Print the data necessary to reconstruct this Gaussian. The inverse and
	  * Cholesky decomposition of the covariance are not printed. 
	  * @param os The output stream to print on.
	  * @param leading_ws This is printed at the start of every line of output.
	  * @throws IOException If the output fails; this is possible, but unlikely.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Format a one-dimensional Gaussian. A slightly more compact
	  * format is used. NEED TO ADD A CORRESPONDING INPUT METHOD FOR
	  * THIS SIMPLIFIED FORMAT !!!
	  */
	public String format_string_1d( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+" { ";
		result += "mean "+mu[0]+"  std-deviation "+Math.sqrt(Sigma[0][0]);

		if ( eta != 0 )
			result += "  prior-mean "+mu_hat[0]+"  prior-mean-scale "+eta;
		if ( beta[0] != 0 )
			result += "  prior-variance "+beta[0];
		if ( alpha != 1/2.0 )
			result += "  prior-variance-scale "+alpha;

		result += " }"+"\n";
		return result;
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
		if ( ndims == 1 ) return format_string_1d( leading_ws );

		int i;
		String result = "";

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;

		result += more_leading_ws+"ndimensions "+ndims+"\n"+"\n";

		result += more_leading_ws+"mean { ";
		for ( i = 0; i < mu.length; i++ ) result += mu[i]+" ";
		result += "}"+"\n";

		result += more_leading_ws+"covariance"+more_leading_ws+"{"+"\n";
		for ( i = 0; i < Sigma.length; i++ )
		{
			result += "\t"+more_leading_ws;
			for ( int j = 0; j < Sigma[i].length; j++ )
				result += Sigma[i][j]+" ";
			result += "\n";
		}
		result += more_leading_ws+"}"+"\n";

		result += more_leading_ws+"prior-mean { ";
		for ( i = 0; i < mu_hat.length; i++ ) result += mu_hat[i]+" ";
		result += "}"+"\n";

		result += more_leading_ws+"prior-variance { ";
		for ( i = 0; i < beta.length; i++ ) result += beta[i]+" ";
		result += "}"+"\n";

		result += more_leading_ws+"prior-mean-scale "+eta+"\n";
		result += more_leading_ws+"prior-variance-scale "+alpha+"\n";

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Computed updated parameters of this distribution by penalized 
	  * maximum likelihood, as described by Ormoneit and Tresp [1].
	  * <p>
	  * [1] Ormoneit, D., and V. Tresp. (1996) ``Improved Gaussian Mixture
	  *   Density Estimates Using Bayesian Penalty Terms and Network
	  *   Averaging.'' <i>Advances in Neural Information Processing Systems 8,</i>
	  *   D. Touretzky, M. Mozer, and M. Hasselmo, eds. Cambridge, MA: MIT Press.
	  * <p>
	  * @param x The data.
	  * @param responsibility Each component of this vector is a scalar telling
	  *   the probability that this distribution produced the corresponding datum.
	  *   This is usually computed as part of a mixture distribution update.
	  * @param niter_max Ignored since the update algorithm is not iterative.
	  * @param stopping_criterion Ignored since the update algorithm is not iterative.
	  * @return Negative log-likelihood after update.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  * @see Distribution.update
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		int i, j, k;

		double[] sum_x = new double[ndims];		// initialized to zeros

		if ( responsibility != null )
		{
			double  sum_responsibility = 0;
			for ( i = 0; i < x.length; i++ )
			{
				for ( j = 0; j < ndims; j++ )
					sum_x[j] += responsibility[i] * x[i][j];
				sum_responsibility += responsibility[i]; 
			}
			
			for ( i = 0; i < ndims; i++ )
				mu[i] = (sum_x[i] + eta*mu_hat[i]) / (sum_responsibility + eta);

			// Now compute variance.
			// Collect outer sums (correlation matrix).

			for ( j = 0; j < ndims; j++ )
				for ( k = 0; k < ndims; k++ )
					Sigma[j][k] = 0;

			for ( i = 0; i < x.length; i++ )
			{
				double[] xx = (double[]) x[i].clone();
				Matrix.axpby( 1, xx, -1, mu );
				double h = responsibility[i];

				for ( j = 0; j < ndims; j++ )
					for ( k = 0; k < ndims; k++ )
						Sigma[j][k] += h * xx[j] * xx[k];
			}

			// Now add in terms for priors on mean and variance.

			double[] delta_mu = (double[]) mu.clone();
			Matrix.axpby( 1, delta_mu, -1, mu_hat );
			for ( j = 0; j < ndims; j++ )
				for ( k = 0; k < ndims; k++ )
					Sigma[j][k] += eta * delta_mu[j] * delta_mu[k];

			for ( j = 0; j < ndims; j++ )
				Sigma[j][j] += 2 * beta[j];

			for ( j = 0; j < ndims; j++ )
				for ( k = 0; k < ndims; k++ )
					Sigma[j][k] *= 1 / (sum_responsibility + 1 + 2*(alpha - (ndims+1)/2.0));
		}
		else
		{
			// Effectively treat all responsibility[i] as 1;
			// ignore regularization parameters.
			// First compute new mean.

			for ( i = 0; i < x.length; i++ )
				for ( j = 0; j < ndims; j++ )
					sum_x[j] += x[i][j];

			for ( j = 0; j < ndims; j++ )
				mu[j] = sum_x[j] / x.length;

			// Now compute variance.
			// Collect outer sums (correlation matrix).

			for ( j = 0; j < ndims; j++ )
				for ( k = 0; k < ndims; k++ )
					Sigma[j][k] = 0;

			for ( i = 0; i < x.length; i++ )
			{
				double[] xx = x[i];
				for ( j = 0; j < ndims; j++ )
					for ( k = 0; k < ndims; k++ )
						Sigma[j][k] += xx[j] * xx[k];
			}

			for ( j = 0; j < ndims; j++ )
				for ( k = 0; k < ndims; k++ )
					Sigma[j][k] *= 1.0/x.length;

			for ( j = 0; j < ndims; j++ )
				for ( k = 0; k < ndims; k++ )
					Sigma[j][k] -= mu[j] * mu[k];
		}

		// Recompute cached quantities.

		Sigma_inverse = Matrix.inverse( Sigma );
		det_Sigma = Matrix.determinant( Sigma );
		L_Sigma = Matrix.cholesky( Sigma );

		// Compute negative log-likelihood of given data and return it.

		double nll = 0;
		for ( i = 0; i < x.length; i++ )
		{
			double[] dx = (double[]) x[i].clone();
			Matrix.axpby( 1, dx, -1, mu );
			double  t = Matrix.dot( dx, Matrix.multiply( Sigma_inverse, dx ) );
			nll += t/2;
		}

		nll += (x.length/2.0) * Math.log(det_Sigma) + (x.length*ndims/2.0) * Math.log( 2*Math.PI );
		return nll;
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, according to the regularization parameters
	  * (prior mean and prior variance) that have been established for
	  * this distribution. 
	  * @return The log of the prior probability of the mean and variance
	  *   of this distribution, ignoring terms which do not depend on the
	  *   mean and variance.
	  */
	public double log_prior() throws RemoteException
	{
		if ( ndimensions() != 1 ) throw new RemoteException( "Gaussian.log_prior: don't know how to compute this for #dimensions == "+ndimensions() );

		double term1 = -Math.log(Sigma[0][0])/2;
		double term2 = -(eta/2)*(mu[0]-mu_hat[0])*(mu[0]-mu_hat[0])/Sigma[0][0];
		double term3 = -(alpha-1)*Math.log(Sigma[0][0]) -beta[0]/Sigma[0][0];
		return term1+term2+term3;
	}

	/** Compute the density of this <code>Gaussian</code> at a point.
	  * @param x The point at which to evaluate the density -- a vector.
	  * @return Density at the point <code>x</code>.
	  */
	public double p( double[] x ) throws RemoteException
	{
		double[] dx = (double[]) x.clone();
		Matrix.axpby( 1, dx, -1, mu );
		double  t = Matrix.dot( dx, Matrix.multiply( Sigma_inverse, dx ) );
		double  pp = Math.pow( 2*Math.PI, -ndims/2.0 ) * Math.exp( -t/2 ) / Math.sqrt( det_Sigma );

		return pp;
	}

	/** Compute an instance of a random variable from this Gaussian.
	  * Generate a random vector N(0,I), then transform using mu and
	  * the (lower triangular) Cholesky decomposition of Sigma.
	  * @return A random vector from this <code>Gaussian</code> distribution.
	  */
	public double[] random() throws RemoteException
	{
		if ( L_Sigma == null )
			L_Sigma = Matrix.cholesky( Sigma );

		int i;
		double[] x = new double[ndims];

		java.util.Random rng = new java.util.Random();
		for ( i = 0; i < ndims; i++ )
			x[i] = (double) rng.nextGaussian();

		x = Matrix.multiply( L_Sigma, x );
		Matrix.add( x, mu );

		return x;
	}

	/** Accessor function for the covariance. This function and <code>get_Sigma</code> implement a 
	  * read-only access mechanism for <code>Sigma</code>, since it is important to make
	  * sure that the inverse and Cholesky decomposition are updated when <code>Sigma</code>
	  * is changed; this update is handled by <code>set_Sigma</code>.
	  * @param Sigma_in New value for the covariance matrix.
	  */
	public void set_Sigma( double[][] Sigma_in )
	{
		Sigma = Matrix.copy( Sigma_in );
		Sigma_inverse = Matrix.inverse( Sigma );
		det_Sigma = Matrix.determinant( Sigma );
		L_Sigma = Matrix.cholesky( Sigma );
	}

	/** Accessor function for the covariance.
	  * @return A copy of the covariance matrix.
	  */
	public double[][] get_Sigma() { return Matrix.copy( Sigma ); }

	/** Make a deep copy of this Gaussian distribution.
	  * All member objects are likewise cloned.
	  * @return A field-by-field copy of this object.
	  * @exception CloneNotSupportedException Thrown only if some member
	  *  object is not cloneable; should never be thrown.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		Gaussian copy;
		try { copy = new Gaussian(); }
		catch (RemoteException e) { throw new CloneNotSupportedException(); }

		copy.ndims = ndims;
		copy.Sigma_inverse = Matrix.copy( Sigma_inverse );
		copy.det_Sigma = det_Sigma;
		copy.L_Sigma = Matrix.copy( L_Sigma );
		copy.Sigma = Matrix.copy( Sigma );
		copy.mu = (double[]) mu.clone();
		copy.mu_hat = (double[]) mu_hat.clone();
		copy.beta = (double[]) beta.clone();
		copy.alpha = alpha;
		copy.eta = eta;

		return copy;
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Gaussian.expected_value: can't handle "+ndims+" dimensions." );

		return mu[0];
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Gaussian.sqrt_variance: can't handle "+ndims+" dimensions." );

		return L_Sigma[0][0];
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution; uses a numerical search to find <tt>x</tt> such that
	  * <pre>
	  *   F((x-mu)/sigma) &lt; epsilon/2
	  * </pre>
	  * (where <tt>F</tt> is the unit Gaussian cumulative distribution function)
	  * and returns the interval <tt>[mu-x,mu+x]</tt>.
	  * @param epsilon If an approximation is made, this much mass or less
	  *   lies outside the interval which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws RemoteException
	{
		if ( ndims > 1 )
			throw new RemoteException( "Gaussian.effective_support: can't handle "+ndims+" dimensions." );

		// Use bisection search to find small interval containing x
		// such that F((x-mu)/sigma) < epsilon/2, then take x as the
		// right end of that interval -- the resulting [mu-x,mu+x] will
		// be a little bit too wide, but that's OK.

		if ( epsilon >= 1 )
			throw new RemoteException( "Gaussian.effective_support: epsilon == "+epsilon+" makes no sense." );

		double z0 = 0, z1 = 10;

		while ( z1 - z0 > 0.25 )
		{
			double zm = z0 + (z1-z0)/2;
			double Fm = (1+SpecialMath.error( zm/Math.sqrt(2.0) ))/2;
			if ( Fm > 1-epsilon/2 )
				z1 = zm;
			else 
				z0 = zm;
// System.err.println( "Gaussian.effective_support: z0: "+z0+" zm: "+zm+" z1: "+z1+" Fm: "+Fm );
		}
// System.err.println( "Gaussian.effective_support: epsilon: "+epsilon+" z1: "+z1 );

		double x = z1 * L_Sigma[0][0];

		double[] interval = new double[2];
		interval[0] = mu[0] - x;
		interval[1] = mu[0] + x;
		return interval;
	}
}
