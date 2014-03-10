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
package riso.apps;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.general.*;

class RemoteQueryHostFrame extends Frame
{
	RemoteQueryOutputFrame rqof; // this frame is associated with the frame rqof

	TextField host_input = new TextField(80);
	List bn_list = new List(), var_list = new List();
	String variable_name; // most recently selected in the list of variables
	PopupMenu operations_menu = new PopupMenu( "Operations" );

	RemoteQueryHostFrame( RemoteQueryOutputFrame rqof, String title )
	{
		super(title);

		this.rqof = rqof;

		operations_menu.add( "Get Posterior" );
		operations_menu.add( "Compute Posterior" );
		operations_menu.add( "Get Pi" );
		operations_menu.add( "Get Lambda" );
		operations_menu.add( "Get Pi Messages" );
		operations_menu.add( "Get Lambda Messages" );
		operations_menu.add( "Get Parents" );
		operations_menu.add( "Get Children" );
		var_list.add(operations_menu);
System.err.println( "operations_menu: "+operations_menu );

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		setLayout(gbl);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		Insets label_insets = new Insets( 15, 15, 5, 15 ), component_insets = new Insets( 5, 15, 15, 15 );

		Label host_label = new Label( "Enter Host Name" );
		gbc.insets = label_insets;
		gbl.setConstraints( host_label, gbc );
		add( host_label );

		gbc.insets = component_insets;
		gbl.setConstraints( host_input, gbc );
		add(host_input);

		Label bn_label = new Label( "Belief Networks on Host" );
		gbc.insets = label_insets;
		gbl.setConstraints( bn_label, gbc );
		add( bn_label );

		gbc.insets = component_insets;
		gbl.setConstraints( bn_list, gbc );
		add(bn_list);

		Label var_label = new Label( "Variables in Belief Network" );
		gbc.insets = label_insets;
		gbl.setConstraints( var_label, gbc );
		add( var_label );

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;

		gbc.insets = component_insets;
		gbl.setConstraints( var_list, gbc );
		add(var_list);
	}

	public boolean keyDown( Event e, int key )
	{
		if ( key == Event.ENTER )
		{
			try
			{
				String host_name = host_input.getText();
				String[] names = Naming.list( "rmi://"+host_name );
				bn_list.removeAll();
				var_list.removeAll();

				for ( int i = 0; i < names.length; i++ )
				{
					try
					{
						Remote o = Naming.lookup( names[i] );
						if ( o instanceof AbstractBeliefNetwork )
							bn_list.add( ((AbstractBeliefNetwork)o).get_name() );
					}
					catch (RemoteException ex)
					{
						System.err.println( "RemoteQueryHostFrame.keyDown: "+ex+"; stagger forward." );
					}
				}
			}
			catch (Exception ex)
			{
				System.err.println( "RemoteQueryHostFrame.keyDown: "+ex+"; stagger forward." );
			}
		}

		return false;
	}
	
	public boolean action( Event evt, Object o )
	{
System.err.println( "action: evt: "+evt+", o: "+o );
		if ( evt.target == bn_list )
		{
			try
			{
				String bn_name = bn_list.getSelectedItem();
				String host_name = host_input.getText();
				RemoteQuery.bn = (AbstractBeliefNetwork) Naming.lookup( "rmi://"+host_name+"/"+bn_name );
				AbstractVariable[] av = RemoteQuery.bn.get_variables();
				var_list.removeAll();

				for ( int i = 0; i < av.length; i++ )
					try { var_list.add( av[i].get_name() ); }
					catch (RemoteException e) { System.err.println( "RemoteQueryHostFrame.action: "+e+"; stagger forward." ); }
			}
			catch (Exception e)
			{
				System.err.println( "RemoteQueryHostFrame.action: "+e+"; stagger forward." );
			}

			return false;
		}
		else if ( evt.target == var_list )
		{
			variable_name = (String) evt.arg;

			try { operations_menu.show( var_list, var_list.getSize().width, 0 ); }
			catch (Exception e)
			{
				System.err.println( "RemoteQueryHostFrame.action: "+e+"; stagger forward." );
			}
		}
		// ??? else if ( evt.target == operations_menu )
		else if ( evt.target instanceof MenuItem )
		{
			try
			{
				String cmd;

				if ( "Get Posterior".equals( (String)evt.arg ) )
					cmd = "get posterior "+variable_name;
				else if ( "Compute Posterior".equals( (String)evt.arg ) )
					cmd = variable_name+" ?";
				else if ( "Get Pi".equals( (String)evt.arg ) )
					cmd = "get pi "+variable_name;
				else if ( "Get Lambda".equals( (String)evt.arg ) )
					cmd = "get lambda "+variable_name;
				else if ( "Get Pi Messages".equals( (String)evt.arg ) )
					cmd = "get pi-messages "+variable_name;
				else if ( "Get Lambda Messages".equals( (String)evt.arg ) )
					cmd = "get lambda-messages "+variable_name;
				else if ( "Get Parents".equals( (String)evt.arg ) )
					cmd = "get parents "+variable_name;
				else if ( "Get Children".equals( (String)evt.arg ) )
					cmd = "get children "+variable_name;
				else
				{
					System.err.println( "RemoteQueryHostFrame.action: huh? evt.arg is "+evt.arg );
					return true;
				}

				SmarterTokenizer st = new SmarterTokenizer( new StringReader( cmd ) );
System.err.println( "cmd: "+cmd );
				rqof.textarea_pstream.println( "\n"+"INPUT: "+cmd+"\n"+"OUTPUT:" );
				try { riso.apps.RemoteQuery.parse_input( st, rqof.textarea_pstream ); }
				catch (Exception ex) { 
System.err.println( "FAILED: "+ex );
					rqof.textarea_pstream.println( "Failed: "+ex ); }
			}
			catch (Exception e)
			{
				System.err.println( "RemoteQueryHostFrame.action: "+e+"; stagger forward." );
			}
		}

		return true;
	}
}

public class RemoteQueryApplet extends Applet
{
	Button host_button = new Button( "Launch Host/BN Dialog" );
	Button output_button = new Button( "Launch RemoteQuery Output Frame" );
	TextArea console = new TextArea( "", 10, 128 );
	PrintStream console_pstream = new PrintStream( new TextAreaOutputStream( console ) );
	RemoteQueryOutputFrame most_recent_output_frame;

	public void init()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		setLayout(gbl);

		Insets label_insets = new Insets( 15, 15, 5, 15 ), component_insets = new Insets( 5, 15, 15, 15 );

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		gbc.insets = component_insets;
		gbl.setConstraints( output_button, gbc );
		add(output_button);

		gbc.insets = component_insets;
		gbl.setConstraints( host_button, gbc );
		add(host_button);

		console.setEditable(false);

		Label console_label = new Label( "Console Output" );
		gbc.insets = label_insets;
		gbl.setConstraints( console_label, gbc );
		add(console_label);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.insets = component_insets;
		gbl.setConstraints( console, gbc );
		add(console);
	}

	public boolean action( Event evt, Object o )
	{
System.err.println( "action: evt: "+evt+", o: "+o );
		if ( evt.target == output_button )
		{
			most_recent_output_frame = new RemoteQueryOutputFrame();
			most_recent_output_frame.setSize( 500, 500 );
			most_recent_output_frame.show();
		}
		else if ( evt.target == host_button )
		{
			Frame host_frame = new RemoteQueryHostFrame( most_recent_output_frame, "Select Host and Belief Network" );
			host_frame.setSize( 350, 500 );
			host_frame.show();
		}
else System.err.println( "action: unknown target; evt: "+evt );

		return true;
	}
}

class RemoteQueryOutputFrame extends Frame
{
	// REMOVE ??? Frame host_frame = new RemoteQueryHostFrame( this, "Select Host and Belief Network" );
	TextField input = new TextField(128);
	TextArea output = new TextArea( "", 10, 128 );
	PrintStream textarea_pstream = new PrintStream( new TextAreaOutputStream( output ) );

	public RemoteQueryOutputFrame()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		setLayout(gbl);

		Insets label_insets = new Insets( 15, 15, 5, 15 ), component_insets = new Insets( 5, 15, 15, 15 );

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;

		Label input_label = new Label( "Input Commands in RemoteQuery Format" );
		gbc.insets = label_insets;
		gbl.setConstraints( input_label, gbc );
		add(input_label);

		gbc.insets = component_insets;
		gbl.setConstraints( input, gbc );
		add(input);

		output.setEditable(false);

		Label output_label = new Label( "RemoteQuery Output" );
		gbc.insets = label_insets;
		gbl.setConstraints( output_label, gbc );
		add(output_label);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.insets = component_insets;
		gbl.setConstraints( output, gbc );
		add(output);

		textarea_pstream.println( "RISO Remote Query Applet." );

		// REMOVE ??? host_frame.setSize( new Dimension( 300, 400 ) );
		// REMOVE ??? host_frame.show();
	}

	public boolean keyDown( Event e, int key )
	{
		if ( key == Event.ENTER )
		{
			SmarterTokenizer st = new SmarterTokenizer( new StringReader( input.getText() ) );
			textarea_pstream.println( "\n"+"INPUT: "+input.getText()+"\n"+"OUTPUT:" );
			try { riso.apps.RemoteQuery.parse_input( st, textarea_pstream ); }
			catch (Exception ex) { textarea_pstream.println( "Failed: "+ex ); }
			input.setText("");
		}

		return false;
	}
}

class TextAreaOutputStream extends OutputStream
{
	TextArea textarea;

	public TextAreaOutputStream( TextArea a ) { textarea = a; }

	public void write( int b ) throws IOException
	{
		if ( b == (int) '\t' ) b = ' ';	// translate tab to space; probably should become multiple spaces.
		textarea.appendText( ""+(byte)b );
	}

	public void write(byte b[]) throws IOException
	{
		for ( int i = 0; i < b.length; i++ ) if ( b[i] == (byte) '\t' ) b[i] = (byte) ' ';
		textarea.appendText( ""+(new String(b)) );
	}

	public void write(byte b[], int off, int len) throws IOException
	{
		for ( int i = off; i < off+len; i++ ) if ( b[i] == (byte) '\t' ) b[i] = (byte) ' ';
		textarea.appendText( new String(b,off,len) );
	}
}
