package numerical;

/** A wrapper for a callback to a function which takes a multi-dimensional
  * argument. There's probably a better way to do this!!!
  */
public interface Callback_nd
{
	public double f( double x[] ) throws Exception;
}
