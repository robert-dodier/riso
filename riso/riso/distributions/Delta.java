package riso.distributions;
import java.rmi.*;

/** Each class whose density is a delta function is marked with this interface.
  */
public interface Delta extends Distribution
{
	/** Return the point on which the mass of this density is concentrated.
	  */
	double[] get_support() throws IllegalArgumentException;
}
