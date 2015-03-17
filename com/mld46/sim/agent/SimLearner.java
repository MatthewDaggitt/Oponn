package com.mld46.sim.agent;

import java.util.HashMap;
import java.util.Map;

import com.mld46.sim.board.SimBoard;
import com.sillysoft.lux.Board;
import com.sillysoft.lux.Card;

public class SimLearner extends SimAgent
{
	private final String [] agentNames = new String[]
	{
		"angry", "stinky", "communist",
		"cluster", "yakool", "shaft", "pixie",
		"boscoe", "quo", "killbot",	"evilpixie"
	};
	
	private SimAgent backer;
	private Map<String, Double> distribution;
	private Map<String, SimAgent> backers;
	
	public SimLearner()
	{
		backers = new HashMap<String, SimAgent>();
		
		for(String agent : agentNames)
		{
			backers.put(agent, SimAgent.getSimAgent(agent, false));
		}
	}
	
	@Override
	public void setSimPrefs(int ID, SimBoard simBoard)
	{
		super.setSimPrefs(ID, simBoard);
		
		for(String name : agentNames)
		{
			setBackerPrefs(backers.get(name));
		}
	}

	public void selectBacker()
	{
		//System.out.print("Selecting: ");
		double r = Math.random();
		double cumulative = 0;
		
		for(String agent : agentNames)
		{
			cumulative += distribution.get(agent);
			if(r <= cumulative)
			{
				backer = backers.get(agent);
				//System.out.println(agent);
				return;
			}
		}
	}
	public void updateDistribution(Map<String, Double> distribution)
	{
		this.distribution = distribution;
		
		System.out.println("");
		System.out.println("Distribution updated for player " + ID);
		for(String agent : distribution.keySet())
		{
			System.out.println(agent + ": " + (distribution.get(agent)*100) + "%");
		}
		System.out.println("");
	}	
	
	@Override
	public void reset()
	{
		super.reset();
		
		if(backer != null)
		{
			backer.reset();
		}
		
		selectBacker();
	}
	
	
	@Override
	public int pickCountry()
	{
		return backer.pickCountry();
	}
	

	@Override
	public void placeInitialArmies(int numberOfArmies)
	{
		backer.placeInitialArmies(numberOfArmies);
	}

	@Override
	public void cardsPhase(Card[] cards)
	{
		backer.cardsPhase(cards);
	}

	@Override
	public void placeArmies(int numberOfArmies)
	{
		backer.placeArmies(numberOfArmies);
	}

	@Override
	public void attackPhase()
	{
		backer.attackPhase();
	}

	@Override
	public int moveArmiesIn(int countryCodeAttacker, int countryCodeDefender)
	{
		return backer.moveArmiesIn(countryCodeAttacker, countryCodeDefender);
	}

	@Override
	public void fortifyPhase()
	{
		backer.fortifyPhase();
	}

	@Override
	public String name()
	{
		return "SimLearner";
	}

	@Override
	public float version()
	{
		return 0;
	}

	@Override
	public String description()
	{
		return "Picks a backer in proportion to the likelihoods it is passed";
	}

	@Override
	public String youWon()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String message(String message, Object data)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
