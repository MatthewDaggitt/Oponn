package com.mld46.sim.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mld46.sim.board.SimBoard;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;

public class SimRandom extends SimAgent
{
	protected final Random random = new Random(System.nanoTime());

	@Override
	public int pickCountry()
	{
		throw new IllegalStateException("SimAgent::pickCountry() not implemented");
	}
	
	@Override
	public void placeInitialArmies(int numberOfArmies)
	{
		place(numberOfArmies);
	}
	
	@Override
	public void cardsPhase(Card [] cards)
	{
		if(cards.length < 3 || !Card.containsASet(cards))
		{
			return;
		}
		
		double r = random.nextDouble();
		if((cards.length == 3 && r < 0.5) || (cards.length == 4 && r < 0.75) || cards.length >= 5)
		{
			Card [] chosen = Card.getRandomSet(cards);
			makeCardCash(chosen[0],chosen[1],chosen[2]);
		}
	}
	
	@Override
	public void placeArmies(int numberOfArmies)
	{
		place(numberOfArmies);
	}
	
	@Override
	public void attackPhase()
	{
		/**
		 * debug("");
		 * debug("Starting attack phase");
		 */
		
		List<Country> possibleAttackers = new ArrayList<Country>(countries.length);
		List<Country> possibleDefenders = new ArrayList<Country>(10);
		
		for(Country country : countries)
		{
			if(country.getOwner() == ID && country.getArmies() > 1)
			{
				for(Country neighbour : country.getAdjoiningList())
				{
					if(neighbour.getOwner() != ID)
					{
						possibleAttackers.add(country);
						//debug("Adding: " + country.getName());
						break;
					}
				}
			}
		}
		
		Country attacker = null, defender = null;
		int attackerCode, defenderCode;
		int attackerIndex;
		
		while(possibleAttackers.size() > 0)
		{
			attackerIndex = random.nextInt(possibleAttackers.size());
			attacker = possibleAttackers.get(attackerIndex);
			attackerCode = attacker.getCode();
				
			possibleDefenders.clear();
			for(Country n : attacker.getAdjoiningList())
			{
				if(n.getOwner() != ID)
				{
					possibleDefenders.add(n);
				}
			}
			
			if(possibleDefenders.size() == 0)
			{
				possibleAttackers.remove(attackerIndex);
			}
			else
			{
				defender = possibleDefenders.get(random.nextInt(possibleDefenders.size()));
				defenderCode = defender.getCode();
				
				int result = makeAttack(attackerCode,defenderCode,true);
				
				if(result == SimBoard.ATTACKERS_DESTROYED)
				{
					possibleAttackers.remove(attackerIndex);
				}
				else if(result == SimBoard.DEFENDERS_DESTROYED)
				{
					if(attacker.getArmies() == 1)
					{
						possibleAttackers.remove(attackerIndex);
					}
					if(defender.getArmies() > 1)
					{
						possibleAttackers.add(defender);
					}
				}
			}
		}
	}
	
	@Override
	public int moveArmiesIn(int countryCodeAttacker, int countryCodeDefender)
	{
		int available = countries[countryCodeAttacker].getArmies()-1;
		if(available == 0)
		{
			throw new IllegalArgumentException("Error, there are zero available attackers!");
		}
		
		int min = Math.min(available,3);
		int number =  min + (min == available ? 0 : random.nextInt(available-min));
		return number;
	}
	
	@Override
	public void fortifyPhase()
	{
		return;
		
		/**
		List<Country> fortifications = new ArrayList<Country>();
		//PriorityQueue<EntityArmyPair> fortificationQueue = new PriorityQueue<EntitryArmyPair>()
		for(Country country : countries)
		{
			if(country.getOwner() == ID && country.getArmies() > 2)
			{
				fortifications.add(country);
			}
		}
		
		Country sourceCountry, destinationCountry;
		List<Country> ownedNeighbours;
		int numberOfArmies;
		int armiesPresent;
		
		while(fortifications.size() > 0 && random.nextFloat() > 0.2f)
		{
			do
			{
				sourceCountry = fortifications.remove(random.nextInt(fortifications.size()));
				
				ownedNeighbours = new ArrayList<Country>();
				for(Country neighbour : sourceCountry.getAdjoiningList())
				{
					if(neighbour.getOwner() == ID)
					{
						ownedNeighbours.add(neighbour);
					}
				}
			}
			while(ownedNeighbours.size() == 0 && fortifications.size() > 0);
			
			
			destinationCountry = ownedNeighbours.remove(random.nextInt(ownedNeighbours.size()));
			
			armiesPresent = sourceCountry.getArmies();
			if(armiesPresent == 2)
			{
				numberOfArmies = 1;
			}
			else
			{
				if(armiesPresent == 1)
				{
					System.out.println(armiesPresent);
				}
				numberOfArmies = random.nextInt(armiesPresent-2)+2;
			}
			
			makeFortification(numberOfArmies,sourceCountry.getCode(),destinationCountry.getCode());
		}
		**/
	}
	
	@Override
	public String name()
	{
		return "SimRandom";
	}
	
	@Override
	public float version()
	{
		return 1.0f;
	}
	
	@Override
	public String description()
	{
		return "Simulation agent for MCTS";
	}
	
	@Override
	public String youWon()
	{
		return "SimAgent wins again!";
	}
	
	@Override
	public String message(String message, Object data)
	{
		return null;
	}
	
	public void place(int numberOfAvailableArmies)
	{
		List<Country> candidateCountries = new ArrayList<Country>();
		for(Country c : countries)
		{
			if(c.getOwner() == ID)
			{
				for(Country n : c.getAdjoiningList())
				{
					if(n.getOwner() != ID)
					{
						candidateCountries.add(c);
						break;
					}
				}
			}
		}
		
		int armies;
		Country country;
		
		while(numberOfAvailableArmies > 0)
		{
			armies = 1+random.nextInt(numberOfAvailableArmies);
			country = candidateCountries.get(random.nextInt(candidateCountries.size()));

			makePlacement(armies,country.getCode());
			
			numberOfAvailableArmies -= armies;
		}
	}
}
