package riso.numerical;

/** A wrapper for a callback to a function which takes a 1-dimensional
  * argument. Parameters can be made available to <tt>f</tt> by creating
  * a class which implements this interface and includes the needed 
  * parameters as instance data.
  */
public interface Callback_1d
{
	public double f( double x ) throws Exception;
}
