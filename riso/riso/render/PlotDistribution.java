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
package riso.render;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.remote_data.*;
import riso.general.*;

/** This class implements an applet to display a plot of a density.
  * The applet tag specifies the name of the variable and the belief network 
  * which contains it. Here is an example:
  * <pre>
  * <applet code="PlotDistribution" width=400 height=200>
  * <param name=beliefnet value="host/something">
  * <param name=variable value="somethingelse">
  * </applet>
  * </pre>
  * If the variable and belief network are not specified, nothing is drawn.
  *
  * <p> The variable in question can be discrete, in which case the plot is a bar graph,
  * or continuous, in which case the plot is a line graph.
  *
  * <p> The applet shows a panel on which the plot is drawn. There is also a text box
  * in which the user can enter the name of a variable, like this:
  * <pre>
  *   host/beliefnetwork-name.variable-name  result-type#result-index
  * </pre>
  * The <tt>result-type</tt> is optional; it can be
  * <tt>posterior, pi, lamba, pi-message, lamba-message, pi-and-lambda</tt>.
  * The result type is assumed to be <tt>posterior</tt> if not specified.
  * The result index is optional; if the result type is <tt>pi-message</tt> or <tt>lambda-message</tt>
  * the result index specifies which one to plot.
  */
public class PlotDistribution extends Applet
{
	TextField input_text = new TextField(128);
	PlotPanel plot_panel = null;

	public void init()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		setLayout(gbl);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets( 4, 4, 4, 4 );
		gbl.setConstraints( input_text, gbc );
		add(input_text);

		String bn_name = getParameter( "beliefnet" );
		String variable_name = getParameter( "variable" );
		try { plot_panel = new PlotPanel( gbl, bn_name, variable_name ); }
		catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException( "OOPS: "+e ); }

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbl.setConstraints( plot_panel, gbc );
		add(plot_panel);

		Checkbox freeze_checkbox = new Checkbox( "Freeze window", null, false );
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.weighty = 0;
		gbl.setConstraints( freeze_checkbox, gbc );
		add( freeze_checkbox );

		freeze_checkbox.addItemListener( new CheckboxListener() );
	}

	public boolean keyDown( Event e, int key )
	{
		if ( key == Event.ENTER )
		{
			SmarterTokenizer st = new SmarterTokenizer( new StringReader( input_text.getText()) );

			try { st.nextToken(); }
			catch (Exception ex) { System.err.println( "NON-FATAL OOPS: "+ex ); }

			int slash_index = st.sval.indexOf("/");
			int dot_index = st.sval.substring(slash_index).indexOf(".");
			String bn_name = st.sval.substring(0,slash_index+dot_index);
			String variable_name = st.sval.substring(slash_index+dot_index+1);

			try { st.nextToken(); }
			catch (Exception ex) { System.err.println( "NON-FATAL OOPS: "+ex ); }

			String result_type = "posterior";
			int result_index = -1;
			
			if ( st.ttype != StreamTokenizer.TT_EOF )
			{
				int pound_index = st.sval.indexOf("#");
				if ( pound_index > 0 )
				{
					result_type = st.sval.substring(0,pound_index);
					result_index = Integer.parseInt( st.sval.substring(pound_index+1) );
				}
				else
					result_type = st.sval;
			}
			
System.err.println( "keyDown: bn_name, variable_name, result_type, result_index: "+bn_name+", "+variable_name+", "+result_type+", "+result_index );
			plot_panel.locate_variable( bn_name, variable_name, result_type, result_index );
			plot_panel.repaint();
		}

		return false;
	}

	class CheckboxListener implements ItemListener
	{
		public void itemStateChanged( ItemEvent e )
		{
			if ( e.getStateChange() == ItemEvent.SELECTED )
				plot_panel.freeze_window = true;
			else
				plot_panel.freeze_window = false;
		}
	}
}

class PlotPanel extends Panel implements RemoteObserver
{
	AbstractVariable variable;
	Distribution[] plist;
	Distribution p;
	int[] x;
	int[][] y;
	double xmin, xmax, xmean;
	double win_x0, win_y0, win_width, win_height;
	int vpt_x0, vpt_y0, vpt_width, vpt_height;
	Dimension size;
	boolean freeze_window = false, need_pi = false;

    String bn_name_cache = "localhost/bn-test";
    String variable_name_cache = "x";
    String result_type_cache = "posterior";
    int result_index_cache = -1;

	PlotPanel( LayoutManager lm, String bn_name, String variable_name ) throws RemoteException
	{
		super(lm);
		UnicastRemoteObject.exportObject( this );
		locate_variable( bn_name, variable_name, "posterior", -1 );
	}

	void locate_variable( String bn_name, String variable_name, String result_type, int result_index )
	{
        bn_name_cache = bn_name;
        variable_name_cache = variable_name;
        result_type_cache = result_type;
        result_index_cache = result_index;

		try
		{
            String url = "rmi://localhost/mycontext";   // AAAGH !!! NEED TO MAKE THIS A PARAMETER !!!
			AbstractBeliefNetworkContext bnc = (AbstractBeliefNetworkContext) Naming.lookup (url);
			Remote bn = bnc.get_reference( NameInfo.parse_beliefnetwork(bn_name,null) );

			if ( variable != null ) ((RemoteObservable)variable).delete_observer( this );
			variable = (AbstractVariable) ((AbstractBeliefNetwork)bn).name_lookup( variable_name );
			((RemoteObservable)variable).add_observer( this, result_type );

			need_pi = false;
			if ( result_type.equals("posterior") ) { plist = null; p = variable.get_posterior(); }
			else if ( result_type.equals("pi") ) { plist = null; p = variable.get_pi(); }
			else if ( result_type.equals("lambda") )
			{
				plist = null;
				p = variable.get_lambda();
				need_pi = true;
			}
			else if ( result_type.equals("pi-messages") )
			{
				plist = variable.get_pi_messages(); 
				if ( result_index > 0 ) p = plist[ result_index ];
				// MULTIPLE PI MESSAGES OUTPUT IS BROKEN; DO NOT ATTEMPT TO USE IT !!!
			}
			else if ( result_type.equals("lambda-messages") )
			{
				plist = variable.get_lambda_messages();
				if ( result_index > 0 ) p = plist[result_index];
				need_pi = true;
			}
			else if ( result_type.equals("pi-and-lambda") )
			{
				p = null;
				plist = new Distribution[2];
				plist[0] = variable.get_pi();
				plist[1] = variable.get_lambda();
				need_pi = true;		// well, this is a little strange, but necessary in current scheme. !!!
			}

			set_geometry();
			this.addComponentListener( new PlotComponentListener() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// throw new RuntimeException( "PlotDistribution.locate_variable: "+e );
			System.err.println( "PlotDistribution.locate_variable: stagger forward." );
		}
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
		return size.height - yy +16 +5;   // +16 to account for space left for title, +5 for the fun of it
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
			p = (Distribution) arg;

            if (p instanceof GaussianDelta)
            {
                // This is a work-around to make plot of delta look OK. !!!
                // It might be better to use a special plotter for delta fcns instead of the general plotter.
                System.err.println ("PlotDistribution.update: replace gaussian delta with gaussian mixture.");
                MixGaussians m = new MixGaussians (1, 2);
                m.components[0] = new Gaussian (p.expected_value(), 0.1);
                m.components[1] = new Gaussian (p.expected_value(), 10);
                p = m;
            }

			set_geometry();
			repaint();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println( "PlotDistribution.update: stagger forward." );
		}
	}

	void set_geometry() throws Exception
	{
		size = getSize();
		vpt_x0 = 5;
		vpt_y0 = 5+16;    // +16 to make room for title (variable name)
		vpt_width = size.width-2*5;
		vpt_height = size.height-2*5;

		if ( p == null && plist == null ) return;
		
		double[] support;

		if ( need_pi )
		{
System.err.println( "set_geometry: need to get pi." );
			Distribution pi = variable.get_pi();
			support = pi.effective_support(1e-3);
			xmean = pi.expected_value();
		}
		else
		{
			support = p.effective_support(1e-3);
			xmean = p.expected_value();
		}

		xmin = support[0];
		xmax = support[1];
System.err.println( "set_geometry: xmin, xmean, xmax: "+xmin+", "+xmean+", "+xmax );

		double y_max = -1;

		// Store the points to be plotted in window coordinate;
		// translate to viewport coords after fudging window parameters.
		double[] win_x;
		double[][] win_y;

		int ncurves = (p == null ? plist.length : 1);

		if ( variable.is_discrete() )
		{
			y_max = 1; // set maximum for discrete probabilities
			int N = 1 + (int) support[1];
			win_x = new double[N];
			win_y = new double[N][ ncurves ];
			double[] xi = new double[1];

			for ( int i = 0; i < N; i++ )
			{
				xi[0] = i;
				win_x[i] = xi[0];
				if ( p == null )
					for ( int j = 0; j < plist.length; j++ )
						win_y[i][j] = plist[j].p( xi );
				else
					win_y[i][0] = p.p( xi );
			}
		}
		else
		{
			int N;

			if ( vpt_width < 2 ) 
				N = 10; 			// this happens on the first call to this method
			else
				N = vpt_width/2;	// every other pixel; ought to look OK

			double x0 = support[0], x1 = support[1];
			double dx = (x1-x0)/N;

			win_x = new double[N];
			win_y = new double[N][ ncurves ];
			double[] xi = new double[1];
			double yi;

			for ( int i = 0; i < N; i++ )
			{
				xi[0] = x0 + (i+0.5)*dx;

				if ( p == null )
					for ( int j = 0; j < plist.length; j++ )
						win_y[i][j] = plist[j].p( xi );
				else
					win_y[i][0] = p.p( xi );

				win_x[i] = xi[0];
			}

			for ( int i = 0; i < win_y.length; i++ )
				for ( int j = 0; j < win_y[i].length; j++ )
					if ( win_y[i][j] > y_max ) y_max = win_y[i][j];
		}

		if ( ! freeze_window )
		{
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
		}

		// Now translate window coords to viewport coords.
		// If freeze_window == true, we re-use the window coords from previous drawing.

		x = new int[ win_x.length ];
		y = new int[ win_y.length ][ win_y[0].length ];

		for ( int i = 0; i < win_x.length; i++ )
		{
			x[i] = translate_x( win_x[i] );
			
			for ( int j = 0; j < win_y[i].length; j++ )
				y[i][j] = translate_y( win_y[i][j] );
		}
	}

    public void paint(Graphics g)
	{
		try
		{
			String name = variable.get_fullname();
			FontMetrics fm = g.getFontMetrics();
			int swidth = fm.stringWidth( name );

            // Print name right justified (so we see the tail of it).
			g.drawString( name, vpt_x0+vpt_width-swidth, vpt_y0-5 );
		}
        catch (StaleReferenceException e)
        {
            System.err.println ("PlotPanel.paint: variable has become stale; attempt to locate again.");

            AbstractVariable prev = variable;
            locate_variable (bn_name_cache, variable_name_cache, result_type_cache, result_index_cache);

            if (variable != null && variable != prev)
            {
                System.err.println ("PlotPanel.paint: apparently located again; re-execute paint.");
                paint (g);
            }
            else
                System.err.println ("PlotPanel.paint: failed to locate again; give up.");

            return;
        }
		catch (RemoteException e)
		{
			System.err.println( "PlotDistribution.paint: "+e+"; stagger forward." );
		}

		if ( p == null && plist == null ) // no data to plot yet
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


		if ( is_discrete )
		{
			int y0 = translate_y(0);
			int width = Math.max( 1, Math.min( vpt_width/(2*x.length), 20 ) );

			g.setColor(Color.red);

			for ( int i = 0; i < x.length; i++ )
			{
				for ( int j = 0; j < y[i].length; j++ )
				{
					// THIS SCHEME (WITH OVERLAPPING RECTANGLES OF SAME COLOR) MAKES LITTLE SENSE !!!
					// IF THERE IS MORE THAN ONE COLUMN OF y WHICH WE ARE PLOTTING !!!
					g.fillRect( x[i]-width/2, y[i][j], width, y0-y[i][j] );
					// g.drawLine( x[i], y0, x[i], y[i][j] );
					// g.fillOval( x[i]-2, y[i][j]-2, 5, 5 );
				}
			}
		}
		else
		{
			g.setColor(Color.red);
			int[] y1 = new int[ x.length ];
			
			for ( int j = 0; j < y[0].length; j++ )
			{
				for ( int i = 0; i < x.length; i++ ) y1[i] = y[i][j];
				g.drawPolyline( x, y1, x.length );
			}
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
