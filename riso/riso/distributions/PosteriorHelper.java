package riso.distributions;

public interface PosteriorHelper
{
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception;
}
