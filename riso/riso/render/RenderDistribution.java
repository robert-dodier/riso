package riso.render;
import riso.distributions.*;

public interface RenderDistribution
{
	public void do_render( Distribution q ) throws Exception;
}
