package riso.belief_nets;

/** This exception is thrown when a belief network cannot be located.
  * This may mean either that the network cannot be loaded from the
  * local disk, or that a remote reference cannot be obtained; the
  * latter may occur under many circumstances.
  */
public class UnknownNetworkException extends java.rmi.RemoteException
{
	public UnknownNetworkException() { super(); }
	public UnknownNetworkException(String s) { super(s); }
}
