/* Copyright (c) 1997 Robert Dodier and the Joint Center for Energy Management,
 * University of Colorado at Boulder. All Rights Reserved.
 *
 * By copying this software, you agree to the following:
 *  1. This software is distributed for non-commercial use only.
 *     (For a commercial license, contact the copyright holders.)
 *  2. This software can be re-distributed at no charge so long as
 *     this copyright statement remains intact.
 *
 * ROBERT DODIER AND THE JOINT CENTER FOR ENERGY MANAGEMENT MAKE NO
 * REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.
 * ROBERT DODIER AND THE JOINT CENTER FOR ENERGY MANAGEMENT SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY YOU AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package distributions;
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
