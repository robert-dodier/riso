package riso.distributions;
import java.io.*;
import java.rmi.*;

/** Interface for so-called location and scale densities. These include
  * the Gaussian, in which case the location is the mean and the scale
  * is the covariance.
  */
public interface LocationScaleDensity extends Distribution
{
	public void set_location( double[] location ) throws RemoteException;

	public void set_scale( double[][] scale ) throws RemoteException;

	public double[] get_location() throws RemoteException;

	public double[][] get_scale() throws RemoteException;
}
