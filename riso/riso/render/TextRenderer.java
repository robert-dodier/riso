package riso.render;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class TextRenderer implements RenderDistribution
{
	public int npoints = 50;
	public PrintStream ps;

	public void do_render( Distribution q ) throws Exception
	{
		if ( q.ndimensions() > 1 )
			throw new IllegalArgumentException( "TextRenderer.do_render: "+q.ndimensions()+" is too many dimensions." );

		double[] support = q.effective_support( 1e-6 ), x = new double[1];
		Format gfmt = new Format( "%.8g" );

		if ( q instanceof Discrete )
		{
			for ( int i = 0; i <= (int)support[1]; i++ )
			{	
				x[0] = i;
				// ps.println( i+"  "+gfmt.form( q.p(x) ) );
				ps.println( i+"  "+q.p(x) );
			}
		}
		else
		{
			double dx = (support[1] - support[0])/npoints;
			for ( int i = 0; i < npoints; i++ )
			{
				x[0] = support[0] + (i+0.5)*dx;
				// ps.println( gfmt.form( x[0] )+"  "+gfmt.form( q.p(x) ) );
				ps.println( x[0]+"  "+q.p(x) );
			}
		}
	}

	public static void main( String[] args )
	{
		AbstractBeliefNetwork bn = null;
		AbstractVariable x = null;
		String description_filename = "", which = "posterior";
		int npoints = 50, which_index = 0;

		try
		{
			for ( int i = 0; i < args.length; i++ )
			{
				switch ( args[i].charAt(1) )
				{
				case 'f':
					description_filename = args[++i];
					break;
				case 'n':
					npoints = Format.atoi( args[++i] );
					break;
				case 'b':
					String url = "rmi://"+args[++i];
					System.err.println( "TextRenderer: url: "+url );
					bn = (AbstractBeliefNetwork) Naming.lookup( url );
					break;
				case 'v':
					x = (AbstractVariable) bn.name_lookup(args[++i]);
					System.err.println( "TextRenderer: obtained reference to variable "+x.get_fullname() );
					break;
				case 'p':
					which = args[++i];
					if ( "pi-message".equals(which) || "lambda-message".equals(which) )
					{
						which_index = Format.atoi(args[++i]);
						System.err.println( "which_index: "+which_index );
					}
					break;
				}
			}

			AbstractDistribution q = null;

			if ( x == null || bn == null )
			{
				FileInputStream fis = new FileInputStream( description_filename );
				SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			
				st.nextToken();
				System.err.println( "TextRenderer: distribution class: "+st.sval );

				q = (AbstractDistribution) java.rmi.server.RMIClassLoader.loadClass( st.sval ).newInstance();
				q.pretty_input( st );
			}
			else
			{
				if ( "prior".equals(which) )
					q = (AbstractDistribution) bn.get_prior(x);
				else if ( "posterior".equals(which) )
					q = (AbstractDistribution) bn.get_posterior(x);
				else if ( "pi".equals(which) )
					q = (AbstractDistribution) x.get_pi();
				else if ( "lambda".equals(which) )
					q = (AbstractDistribution) x.get_lambda();
				else if ( "pi-message".equals(which) )
					q = (AbstractDistribution) x.get_pi_messages()[which_index];
				else if ( "lambda-message".equals(which) )
					q = (AbstractDistribution) x.get_lambda_messages()[which_index];
				else
				{
					System.err.println( "which: "+which+" ???" );
					System.exit(1);
				}
			}

			TextRenderer tr = new TextRenderer();
			tr.npoints = npoints;
			tr.ps = System.out;

			tr.do_render(q);

			System.exit(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
