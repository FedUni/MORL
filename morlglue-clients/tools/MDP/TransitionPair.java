// Stores a state ID, and the probability of transitioning to that state
// Used as a bulding block for modelling MDPs
// Written by Peter Vamplew August 2017

package tools.MDP;

public class TransitionPair 
{
	private int state;
	private double probability;
	
	public TransitionPair(int _state, double _prob)
	{
		state = _state;
		probability = _prob;
	}
	
	public int getState()
	{
		return state;
	}
	
	public double getProbability()
	{
		return probability;
	}
	
	public void incrementProbability(double _extraProb)
	{
		probability += _extraProb;
	}
	
	public String toString()
	{
		return "(" + state + ", " + probability + ")";
	}

}
