// Stores a list of all possible transitions which can result from a particular state-action pair.
// Used as a bulding block for modelling MDPs
// Written by Peter Vamplew August 2017
package tools.MDP;

import java.util.ArrayList;
import java.util.Random;

public class TransitionList 
{
	ArrayList<TransitionPair> list; 
	
	public TransitionList()
	{
		list = new ArrayList<TransitionPair>();
	}
	
	// if there is not an entry matching _state, create it and set it to the provided probability
	// if there is, then increment the current probability for that state by the provided value
	public void add(int _state, double _probability)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).getState()==_state)
			{
				list.get(i).incrementProbability(_probability);
				return;
			}
		}
		// not found, so add to the list
		list.add(new TransitionPair(_state,_probability));
	}
	
	// Returns the probability of the provided state being the successor state
	public double getProbability(int _state)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).getState()==_state)
			{
				return list.get(i).getProbability();
			}
		}
		// not found, so return 0
		return 0.0;	
	}
	
	// Stochastically selects a successor state from the list, and returns its ID
	public int getNextState(Random r)
	{
		double d=r.nextDouble();
		double summedProbability = 0.0;
		for (int i=0; i<list.size()-1; i++)
		{
			summedProbability += list.get(i).getProbability();
			if (d<summedProbability)
			{
				return list.get(i).getState();
			}
		}
		// if we reach the end of the list, just return the state of the last entry
		// written this way to avoid any issues with rounding errors
		return list.get(list.size()-1).getState();
	}
	
	public String toString()
	{
		String s = "";
		for (int i=0; i<list.size(); i++)
		{
			s += list.get(i);
		}
		return s;
	}

}
