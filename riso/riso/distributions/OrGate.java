package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** An object of this class represents an or gate. The output is 1 if any input is 1,
  * and the output is 0 if all inputs are 0. This class is implemented as a special
  * case of <tt>NoisyOrGate</tt> with the leak probability equal to 0.
  */
public class OrGate extends NoisyOrGate 
{
	/** Default constructor for an or gate.
	  */
	public OrGate() throws RemoteException { p_leak = 0; }

	/** This constructor specifies the number of inputs for the or gate.
	  */
	public OrGate( int ninputs_in ) throws RemoteException { ninputs = ninputs_in; }

	/** Return a deep copy of this object. If this object is remote,
	  * <tt>remote_clone</tt> will create a new remote object.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		OrGate copy = new OrGate();
		copy.associated_variable = this.associated_variable;
		copy.ninputs = this.ninputs;
		return copy;
	}

	/** Create a description of this or gate as a string.
	  * If this distribution is associated with a variable, the number of inputs
	  * is not into the output string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		String result = this.getClass()+" { ";
		if ( associated_variable == null )
			result += "ninputs "+ninputs+" ";
		result += "}\n";
		return result;
	}
}
