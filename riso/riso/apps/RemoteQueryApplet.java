package riso.apps;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import SmarterTokenizer;

public class RemoteQueryApplet extends Applet implements KeyListener
{
	TextField input = new TextField(128);
	TextArea output = new TextArea( "", 80, 80 );
	PrintStream textarea_pstream = new PrintStream( new TextAreaOutputStream( output ) );

	public void init()
	{
		setLayout(null);

		output.setEditable(false);

		add(input);
		add(output);

		input.reshape( 50, 30, 300, 32 );
		input.addKeyListener(this);

		output.reshape( 50, 100, 200, 300 );
		output.appendText( "Hello, World!" );
	}

	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void keyPressed(KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			SmarterTokenizer st = new SmarterTokenizer( new StringReader( input.getText() ) );
			output.setText("");
			textarea_pstream.println( "Input: "+input.getText()+"\n"+"Output:" );
			try { riso.apps.RemoteQuery.parse_input( st, textarea_pstream ); }
			catch (Exception ex) { textarea_pstream.println( "Failed: "+ex ); }
		}
	}
}

class TextAreaOutputStream extends OutputStream
{
	TextArea textarea;

	public TextAreaOutputStream( TextArea a ) { textarea = a; }

	public void write( int b ) throws IOException
	{
System.err.println( "write(b) called: "+(byte)b );
		textarea.appendText( ""+(byte)b );
	}

	public void write(byte b[]) throws IOException
	{
System.err.println( "write(b[]) called: "+(new String(b)) );
		textarea.appendText( ""+(new String(b)) );
	}

	public void write(byte b[], int off, int len) throws IOException
	{
System.err.println( "write(b[],off,len) called: "+(new String(b,off,len)) );
		textarea.appendText( new String(b,off,len) );
	}
}
