package risotto.distributions;
import java.rmi.*;

/** An item of this class represents a ``noninformative'' lambda
  * message or lambda function, that is, one for a variable which is
  * not evidence and for which there is no downstream evidence.
  */
public class Noninformative extends AbstractDistribution
{
	public Noninformative() throws RemoteException {}

	/** Computes the density at the point <code>x</code>. 
	  * Always returns 1.
	  */
	public double p( double[] x ) throws RemoteException { return 1; }
}
