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
	/** Constructs a new Gaussian mixture with the specified number of
	  * dimensions and components. Components can be set up one by one
	  * since <tt>Mixture.components</tt> is <tt>public</tt>.
	  * This constructor allocates the arrays to which member variables
	  * refer, and fills them with neutral values; the <tt>components</tt>
	  * array is filled with new <tt>Gaussian</tt> instances with zero mean
	  * and unit variance (in the specified number of dimensions).
	  */
	public MixGaussians( int ndimensions, int ncomponents ) throws RemoteException
	{
		int i;

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

	/** This do-nothing constructor exists solely to declare
	  * that it can throw a <tt>RemoteException</tt>.
	  */
	public MixGaussians() throws RemoteException {}

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
	  * Gaussian mixtures.
	  */
	public static MixGaussians product_mixture( MixGaussians[] mixtures ) throws RemoteException
	{
		if ( mixtures.length == 1 )
			try { return (MixGaussians) mixtures[0].remote_clone(); }
			catch (CloneNotSupportedException e) 
			{
				throw new RuntimeException( "MixGaussians.product_mixture: unexpected: "+e );
			}

		int nproduct = 1;
		for ( int i = 0; i < mixtures.length; i++ )
			nproduct *= mixtures[i].ncomponents;
		MixGaussians product = new MixGaussians( 1, nproduct );
System.err.println( "MixGaussians.product_mixture: nproduct: "+nproduct );

		int[] k = new int[ mixtures.length ], l = new int[1];
		product_inner_loop( mixtures, product, k, l, mixtures.length-1 );

		return product;
	}
	
	static void product_inner_loop( MixGaussians[] mixtures, MixGaussians product, int[] k, int[] l, int m ) throws RemoteException
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

	static void compute_one_product( MixGaussians[] mixtures, MixGaussians product, int[] k, int[] l ) throws RemoteException
	{
		double A = 0, B = 0, mix_proportion = 1;
		for ( int i = 0; i < mixtures.length; i++ )
		{
			mix_proportion *= mixtures[i].mix_proportions[ k[i] ];

			Gaussian p = (Gaussian) mixtures[i].components[ k[i] ];
			double mu = p.expected_value();
			double sigma = p.sqrt_variance();
			B += mu/(sigma*sigma);
			A += 1/(sigma*sigma);
		}

System.err.print( "MixGaussians.compute_one_product: k:" );
for ( int j = 0; j < k.length; j++ ) System.err.print( " "+k[j] );
System.err.println("");
System.err.println( " component["+l[0]+"] of product: mu: "+(B/A)+" sigma^2: "+(1/A)+" proportion: "+mix_proportion );

		product.mix_proportions[ l[0] ] = mix_proportion;
		product.components[ l[0] ] =  new Gaussian( B/A, Math.sqrt(1/A) );
		++l[0];

		// SHOULD WE TRY TO SET REGULARIZATION PARAMETERS TOO ???
	}
	
	/** Just returns a clone of this Gaussian mixture.
	  */
	public MixGaussians initial_mix() throws RemoteException
	{
		return (MixGaussians) remote_clone();
	}
}
