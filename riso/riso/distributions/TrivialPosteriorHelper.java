package risotto.distributions;

public class TrivialPosteriorHelper implements PosteriorHelper
{
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		return pi;
	}
}
