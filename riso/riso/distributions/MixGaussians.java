package distributions;
import java.io.*;

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
	/** Read a description of this density model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  *
	  * @param is Input stream to read from.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.wordChars( '$', '%' );
			st.wordChars( '?', '@' );
			st.wordChars( '[', '_' );
			st.ordinaryChar('/');
			st.slashStarComments(true);
			st.slashSlashComments(true);

			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "MixGaussians.pretty_input: input doesn't have opening bracket." );

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
						throw new IOException( "MixGaussians.pretty_input: ``mixing-proportions'' lacks opening bracket." );

					for ( int i = 0; i < ncomponents; i++ )
					{
						st.nextToken();
						mix_proportions[i] = st.nval;
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
						gamma[i] = st.nval;
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
						if ( "distributions.Gaussian".equals(st.sval) )
						{
							components[i] = (Distribution) new Gaussian();
							components[i].pretty_input( st );
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

		is_ok = true;
	}
}
