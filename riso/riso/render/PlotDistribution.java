/** <applet code="PlotDistribution" width=400 height=200>
  * <param name=beliefnet value="host/something">
  * <param name=variable value="somethingelse">
  * </applet>
  */
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.remote_data.*;

public class PlotDistribution extends Applet implements RemoteObserver
{
	AbstractVariable variable;
	Distribution p;
	int[] x, y;
	double xmin, xmax, xmean;
	double win_x0, win_y0, win_width, win_height;
	int vpt_x0, vpt_y0, vpt_width, vpt_height;
	Dimension size;

	public PlotDistribution() throws RemoteException
	{
		UnicastRemoteObject.exportObject( this );
	}

	/** Translate x from window coordinate system to viewport coordinates.
	  * <tt>(win_x0,win_y0) |-> (vpt_x0,vpt_y0)</tt>, and
	  * <tt>(win_x0+win_width,win_y0+win_height) |-> (vpt_x0+vpt_width,vpt_y0+vpt_height)</tt>.
	  */
	public int translate_x( double x )
	{
		double xscale = vpt_width/win_width;
		return vpt_x0 + (int) ((x-win_x0)*xscale);
	}

	/** Translate y from window coordinate system to viewport coordinates.
	  * <tt>(win_x0,win_y0) |-> (vpt_x0,vpt_y0)</tt>, and
	  * <tt>(win_x0+win_width,win_y0+win_height) |-> (vpt_x0+vpt_width,vpt_y0+vpt_height)</tt>.
	  */
	public int translate_y( double y )
	{
		double yscale = vpt_height/win_height;
		int yy = vpt_y0 + (int) ((y-win_y0)*yscale);
		return size.height - yy;
	}

	public void init()
	{
		String bn_name = getParameter( "beliefnet" );
		String variable_name = getParameter( "variable" );

		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			Remote bn = bnc.get_reference( NameInfo.parse_beliefnetwork(bn_name,null) );
			variable = (AbstractVariable) ((AbstractBeliefNetwork)bn).name_lookup( variable_name );
			((RemoteObservable)variable).add_observer( this, "posterior" );
			p = variable.get_posterior();
			set_geometry();
			this.addComponentListener( new PlotComponentListener() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException( "PlotDistribution.init: "+e );
		}
	}

	/** This method is called by the variable being watched after 
	  * the variable has changed. When that happens, update the display of
	  *  the current posterior distribution of the variable.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		try
		{
			String what = (String) of_interest;
System.err.println( "PlotDistribution: update "+what );
			p = (Distribution) arg;
			set_geometry();
			repaint();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException( "PlotDistribution.update: "+e );
		}
	}

	void set_geometry() throws Exception
	{
		size = getSize();
		vpt_x0 = 25;
		vpt_y0 = 25;
		vpt_width = size.width-2*25;
		vpt_height = size.height-2*25;

		if ( p == null ) return;
		double[] support = p.effective_support(1e-3);

		xmin = support[0];
		xmax = support[1];
		xmean = p.expected_value();

		double y_max = -1;

		// Store the points to be plotted in window coordinate;
		// translate to viewport coords after fudging window parameters.
		double[] win_x, win_y;

		if ( variable.is_discrete() )
		{
			y_max = 1; // set maximum for discrete probabilities
			int N = 1 + (int) support[1];
			win_x = new double[N];
			win_y = new double[N];
			double[] xi = new double[1];

			for ( int i = 0; i < N; i++ )
			{
				xi[0] = i;
				win_x[i] = xi[0];
				win_y[i] = p.p( xi );
			}
		}
		else
		{
			int N = 100;
			double x0 = support[0], x1 = support[1];
			double dx = (x1-x0)/N;

			win_x = new double[N];
			win_y = new double[N];
			double[] xi = new double[1];
			double yi;

			for ( int i = 0; i < N; i++ )
			{
				xi[0] = x0 + (i+0.5)*dx;
				yi = p.p( xi );
				if ( yi > y_max ) y_max = yi;
				win_x[i] = xi[0];
				win_y[i] = yi;
			}
		}

		win_x0 = xmin;
		win_y0 = 0;
		win_width = xmax-xmin;
		win_height = y_max;

		// Now fudge the window parameters so that plotted stuff
		// doesn't touch the edges of the viewport.

		double win_xmargin = win_width*0.1, win_ymargin = win_height*0.1;
		win_x0 -= win_xmargin;
		win_y0 -= win_ymargin;
		win_width += 2*win_xmargin;
		win_height += 2*win_ymargin;

		// Now translate window coords to viewport coords.

		x = new int[ win_x.length ];
		y = new int[ win_y.length ];

		for ( int i = 0; i < win_x.length; i++ )
		{
			x[i] = translate_x( win_x[i] );
			y[i] = translate_y( win_y[i] );
		}
	}

    public void paint(Graphics g)
	{
		try
		{
			String name = variable.get_fullname();
			FontMetrics fm = g.getFontMetrics();
			int swidth = fm.stringWidth( name );
			g.drawString( name, vpt_x0+vpt_width/2-swidth/2, vpt_y0-5 );
		}
		catch (RemoteException e)
		{
			System.err.println( "PlotDistribution.paint: "+e+"; stagger forward." );
		}

		if ( p == null ) // no data to plot yet
		{
			g.setColor(Color.gray);
			g.fillRect( vpt_x0, vpt_y0, vpt_width, vpt_height );
			return;
		}

        Dimension d = size();

		boolean is_discrete;
		try { is_discrete = variable.is_discrete(); }
		catch (RemoteException e) { throw new RuntimeException( "PlotDistribution.paint: "+e ); }

        g.setColor(Color.white);
		g.fillRect( vpt_x0, vpt_y0, vpt_width, vpt_height );

        g.setColor(Color.black);

		if ( is_discrete )
		{
			int y0 = translate_y(0);
			for ( int i = 0; i < x.length; i++ )
			{
				g.drawLine( x[i], y0, x[i], y[i] );
				g.fillOval( x[i]-2, y[i]-2, 5, 5 );
			}
		}
		else
		{
			g.drawPolyline( x, y, x.length );
		}

        g.setColor(Color.darkGray);
        g.drawRect( vpt_x0, vpt_y0, vpt_width, vpt_height );
		int y0 = translate_y(0), y1 = translate_y( -win_height/10.0/2 );
		g.drawLine( translate_x(xmin), y0, translate_x(xmax), y0 );

		g.drawLine( translate_x(xmin), y0, translate_x(xmin), y1 );
		g.drawLine( translate_x(xmax), y0, translate_x(xmax), y1 );
		g.drawLine( translate_x(xmean), y0, translate_x(xmean), y1 );
    }

	class PlotComponentListener implements ComponentListener
	{
		public void componentResized(ComponentEvent e)
		{
			try
			{
				set_geometry();
				repaint();
			}
			catch (Exception ee)
			{
				ee.printStackTrace();
				System.err.println( "componentResized: stagger forward." );
			}
		}

		public void componentMoved(ComponentEvent e) {}
		public void componentShown(ComponentEvent e) {}
		public void componentHidden(ComponentEvent e) {}
	}
}
