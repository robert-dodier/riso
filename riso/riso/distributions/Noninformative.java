package riso.distributions;
import java.rmi.*;

/** An item of this class represents a ``noninformative'' lambda
  * message or lambda function, that is, one for a variable which is
  * not evidence and for which there is no downstream evidence.
  */
public class Noninformative extends AbstractDistribution
{
	public Noninformative() throws RemoteException {}

	/** Since all <tt>Noninformative</tt> lambda messages are alike,
	  * this method simply returns <tt>this</tt>.
	  * This method doesn't make much sense, but it's harmless.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		return this;
	}

	/** Computes the density at the point <code>x</code>. 
	  * Always returns 1.
	  */
	public double p( double[] x ) throws RemoteException { return 1; }

	/** The support for a noninformative lambda or lambda message is unbounded.
	  * @throws SupportNotWellDefinedException Always thrown.
	  */
	public double[] effective_support( double epsilon ) throws RemoteException
	{
		throw new SupportNotWellDefinedException( "Noninformative: support is not well-defined." );
	}
}
