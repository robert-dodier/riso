package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

/** This class represents an additive mixture of Gaussian densities.
  * There is little added functionality; the main thing is the name 
  * guarantees that all mixture components are <tt>Gaussian</tt>.
  * <p>
  * The descriptive data which can be changed without causing the interface
  * functions to break down is public. The other data is protected.
  * Included in the public data are the regularization parameters. 
  *
  * @see Mixture
  * @author Robert Dodier, Joint Center for Energy Management
  */
public class MixGaussians extends Mixture
{
	static public final MixGaussians ugp_mix = new MixGaussians(1,5);

	static
	{
		ugp_mix.mix_proportions[0] = 0.0938712541249544;
		ugp_mix.mix_proportions[1] = 0.27911817658465693;
		ugp_mix.mix_proportions[2] = 0.29570464200369806;
		ugp_mix.mix_proportions[3] = 0.21254165865721594;
		ugp_mix.mix_proportions[4] = 0.11876426862947471;

		ugp_mix.components[0] = new Gaussian( 0, 0.05338477731933904 );
		ugp_mix.components[1] = new Gaussian( 0, 0.2812625748483125 );
		ugp_mix.components[2] = new Gaussian( 0, 0.7424900145427081 );
		ugp_mix.components[3] = new Gaussian( 0, 1.1799024457653036 );
		ugp_mix.components[4] = new Gaussian( 0, 2.100157700236401 );
	}

	/** Constructs a new Gaussian mixture with the specified number of
	  * dimensions and components. Components can be set up one by one
	  * since <tt>Mixture.components</tt> is <tt>public</tt>.
	  * This constructor allocates the arrays to which member variables
	  * refer, and fills them with neutral values; the <tt>components</tt>
	  * array is filled with new <tt>Gaussian</tt> instances with zero mean
	  * and unit variance (in the specified number of dimensions).
	  */
	public MixGaussians( int ndimensions, int ncomponents )
	{
		int i;

		this.common_type = (new Gaussian(0,1)).getClass();

		this.ndims = ndimensions;
		this.ncomponents = ncomponents;

		components = new Distribution[ ncomponents ];
		mix_proportions = new double[ ncomponents ];
		gamma = new double[ ncomponents ];

		double[] mu = new double[ ndimensions ];
		double[][] Sigma = new double[ ndimensions ][ ndimensions ];

		for ( i = 0; i < ndimensions; i++ )
			Sigma[i][i] = 1;

		for ( i = 0; i < ncomponents; i++ )
		{
			components[i] = new Gaussian( mu, Sigma );
			mix_proportions[i] = 1.0/ncomponents;
			gamma[i] = 1;
		}
	}

	/** This constructs a 1-component mixture from a <tt>Gaussian</tt>.
	  * The <tt>p</tt> reference is COPIED, not cloned.
	  */
	public MixGaussians( Gaussian p )
	{
		common_type = p.getClass();
		ndims = p.ndimensions();
		ncomponents = 1;

		components = new Distribution[1];
		mix_proportions = new double[1];
		gamma = new double[1];

		components[0] = p;
		mix_proportions[0] = 1;
		gamma[0] = 1;
	}

	/** This constructor sets <tt>common_type</tt> to <tt>Gaussian</tt>.
	  */
	public MixGaussians() { common_type = (new Gaussian(0,1)).getClass(); }

	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  *
	  * @param st Stream tokenizer to read from.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "MixGaussians.pretty_input: input doesn't have opening bracket." );

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
						throw new IOException( "MixGaussians.pretty_input: ``mixing-proportions'' lacks opening bracket." );

					for ( int i = 0; i < ncomponents; i++ )
					{
						st.nextToken();
						mix_proportions[i] = Format.atof( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "MixGaussians.pretty_input: ``mixing-proportions'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "regularization-gammas" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "MixGaussians.pretty_input: ``regularization-gammas'' lacks opening bracket." );

					for ( int i = 0; i < ncomponents; i++ )
					{
						st.nextToken();
						gamma[i] = Format.atof( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "MixGaussians.pretty_input: ``regularization-gammas'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "components" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "MixGaussians.pretty_input: ``components'' lacks opening bracket." );

					components = new Distribution[ ncomponents ];

					for ( int i = 0; i < ncomponents; i++ )
					{
						// All components are Gaussian densities, so there's
						// no need to do ``Class.forName'' here.

						st.nextToken();
						if ( "riso.distributions.Gaussian".equals(st.sval) )
						{
							Gaussian new_gaussian = new Gaussian();
							new_gaussian.pretty_input( st );
							components[i] = new_gaussian;
						}
						else
							throw new IOException( "MixGaussians.pretty_input: component "+i+" isn't a Gaussian, it's a "+st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "MixGaussians.pretty_input: ``components'' lacks a closing bracket." );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}

			if ( ! found_closing_bracket )
				throw new IOException( "MixGaussians.pretty_input: no closing bracket." );
		}
		catch (IOException e)
		{
			throw new IOException( "MixGaussians.pretty_input: attempt to read network failed:\n"+e );
		}
	}

	/** Trims down the number of components in this mixture by removing
	  * some and modifying the parameters of others.
	  */
	public void reduce_mixture( int max_ncomponents, double KL_epsilon )
	{
		// If this mixture has too many components, get rid of the
		// smallest components and rearrange the remainder to fit the
		// original as best we can. IS THAT REALLY WHAT WE WANT ???

		
	}

	/** Computes a Gaussian mixture from the product of a set of
	  * Gaussian mixtures. Note that this is NOT THE SAME as computing a
	  * mixture for the product of variables with mixture densities;
	  * that operation is computed by <tt>product_mixture</tt>.
	  */
	public static MixGaussians mixture_product( MixGaussians[] mixtures )
	{
		if ( mixtures.length == 1 )
			try { return (MixGaussians) mixtures[0].remote_clone(); }
			catch (CloneNotSupportedException e) 
			{
				throw new RuntimeException( "MixGaussians.mixture_product: unexpected: "+e );
			}

		int i, nproduct = 1;
		for ( i = 0; i < mixtures.length; i++ )
			nproduct *= mixtures[i].ncomponents;
		MixGaussians product = new MixGaussians( 1, nproduct );
System.err.println( "MixGaussians.mixture_product: nproduct: "+nproduct );

		int[] k = new int[ mixtures.length ], l = new int[1];
		product_inner_loop( mixtures, product, k, l, mixtures.length-1 );

		// Fix up mixing coefficients.
		double sum = 0; 
		for ( i = 0; i < product.ncomponents; i++ ) sum += product.mix_proportions[i];
		for ( i = 0; i < product.ncomponents; i++ ) product.mix_proportions[i] /= sum;
System.err.println( "MixGaussians.mixture_product: sum: "+sum );

		return product;
	}
	
	static void product_inner_loop( MixGaussians[] mixtures, MixGaussians product, int[] k, int[] l, int m )
	{
		if ( m == -1 )
		{
			// Recursion has bottomed out.
			compute_one_product( mixtures, product, k, l );
		}
		else
		{
			for ( int i = 0; i < mixtures[m].ncomponents; i++ )
			{
				k[m] = i;
				product_inner_loop( mixtures, product, k, l, m-1 );
			}
		}
	}

	static void compute_one_product( MixGaussians[] mixtures, MixGaussians product, int[] k, int[] l )
	{
		Gaussian[] mix_combo = new Gaussian[ mixtures.length ];
		double[] ignored_scale = new double[1];
		double mix_coeff_product = 1;

		for ( int i = 0; i < mixtures.length; i++ )
		{
			mix_combo[i] = (Gaussian) mixtures[i].components[ k[i] ];
			mix_coeff_product *= mixtures[i].mix_proportions[ k[i] ];
		}

		product.components[ l[0] ] = Gaussian.densities_product( mix_combo, ignored_scale );
		product.mix_proportions[ l[0] ] = ignored_scale[0] * mix_coeff_product;
		++l[0];

		// SHOULD WE TRY TO SET REGULARIZATION PARAMETERS TOO ???
	}
	
	/** Computes a mixture approximation to the product of several variables which have Gaussian mixture
	  * densities. The approximation is computed for two variables, then three, four, etc.
	  */
	public static MixGaussians product_mixture( MixGaussians[] mixtures )
	{
		if ( mixtures.length == 1 )
			try { return (MixGaussians) mixtures[0].remote_clone(); }
			catch (CloneNotSupportedException e) 
			{
				throw new RuntimeException( "MixGaussians.product_mixture: unexpected: "+e );
			}

		MixGaussians product_mix = product2_mixture( mixtures[0], mixtures[1] );
		for ( int i = 2; i < mixtures.length; i++ )
			product_mix = product2_mixture( product_mix, mixtures[i] );

		return product_mix;
	}

	/** Computes a mixture approximation to the product of two variables which have Gaussian mixture
	  * densities. This approximation makes use of a standard approximation <tt>ugp_mix</tt> 
	  * to the product of zero mean, unit variance Gaussian variables.
	  */
	public static MixGaussians product2_mixture( MixGaussians mix1, MixGaussians mix2 )
	{
		try
		{
			int nproduct = mix1.ncomponents()*mix2.ncomponents()*ugp_mix.ncomponents();
			MixGaussians product = new MixGaussians( 1, nproduct );
	System.err.println( "MixGaussians.product2_mixture: nproduct: "+nproduct );

			int ii = 0;
			for ( int i = 0; i < mix1.ncomponents(); i++ )
				for ( int j = 0; j < mix2.ncomponents(); j++ )
				{
					double mu12 = mix1.components[i].expected_value() * mix2.components[j].expected_value();
					for ( int k = 0; k < ugp_mix.ncomponents(); k++ )
					{
						double alpha12w = mix1.mix_proportions[i]*mix2.mix_proportions[j]*ugp_mix.mix_proportions[k];
						product.mix_proportions[ii] = alpha12w;
						double sigma12w = mix1.components[i].sqrt_variance()*mix2.components[j].sqrt_variance()*ugp_mix.components[k].sqrt_variance();
						product.components[ii] = new Gaussian( mu12, sigma12w );
						++ii;
					}
				}

			return product;
		}
		catch (Exception e) { throw new RuntimeException("MixGaussians.product2_mixture: "+e ); }
	}

	/** Just returns a clone of this Gaussian mixture.
	  *
	  * @param support This argument is ignored.
	  * @see Distribution.initial_mix
	  */
	public MixGaussians initial_mix( double[] support )
	{
		try { return (MixGaussians) remote_clone(); }
		catch (CloneNotSupportedException e) 
		{
			throw new RuntimeException( "MixGaussians.initial_mix: unexpected: "+e );
		}
	}
}
