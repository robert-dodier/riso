package risotto.distributions;
import java.io.*;

/** Interface for so-called location and scale densities. These include
  * the Gaussian, in which case the location is the mean and the scale
  * is the covariance.
  */
public interface LocationScaleDensity extends Distribution
{
	public void set_location( double[] location );

	public void set_scale( double[][] scale );

	public double[] get_location();

	public double[][] get_scale();
}
