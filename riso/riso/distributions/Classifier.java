package riso.distributions;

/** An instance of this class represents a classification model.
  * Subclasses provide particular implementations of classification schemes.
  * A classifier is the conditional distribution of a discrete child with
  * discrete or continuous parents.
  */
public abstract class Classifier extends AbstractConditionalDistribution
{
	abstract public int ncategories();
}
