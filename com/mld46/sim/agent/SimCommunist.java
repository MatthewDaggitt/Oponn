package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//	Communist.java
//	Lux
//
//  Copyright (c) 2002-2008 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

public class SimCommunist extends SimSmartAgentBase
{
	// The next country to expand from:
	protected int expando;
	protected int expandTo;

	public String name()
	{
		return "Communist";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "Communist is the worker's AI. All countries are equal. Knock them over like dominoes.";
	}

	public int pickCountry()
	{
		return pickCountryInSmallContinent();
	}

	// We place armies one at a time on the weakest country that we own.
	public void placeArmies(int numberOfArmies)
	{
		int leftToPlace = numberOfArmies;
		while(leftToPlace > 0)
		{
			int leastArmies = Integer.MAX_VALUE;
			CountryIterator ours = new PlayerIterator(ID, countries);
			while(ours.hasNext() && leftToPlace > 0)
			{
				Country us = ours.next();

				leastArmies = Math.min(leastArmies, us.getArmies());
			}

			// Now place an army on anything with less or equal to <leastArmies>
			CountryIterator placers = new ArmiesIterator(ID, -(leastArmies),
					countries);

			while(placers.hasNext() && leftToPlace > 0)
			{
				Country us = placers.next();
				makePlacement(1, us.getCode());
				leftToPlace -= 1;
			}
		}
	}

	// We pick expando as the country we own that has the weakest enemy country
	// beside it.
	protected void setExpandos()
	{
		int leastNeighborArmies = Integer.MAX_VALUE;
		expando = -1;
		expandTo = -1;

		for(int i = 0; i < numberOfCountries; i++)
		{
			if(countries[i].getOwner() == ID)
			{
				// This means this COULD be expando.

				// Get country[i]'s neighbours:
				Country[] neighbors = countries[i].getAdjoiningList();

				// Now loop through the neighbours and find the weakest:
				for(int j = 0; j < neighbors.length; j++)
				{
					if(neighbors[j].getOwner() != ID
							&& neighbors[j].getArmies() < leastNeighborArmies)
					{
						if(!getRealAgentName(neighbors[j].getOwner()).equals(name())
								|| communismWins())
						// don't attack other commies, until all the running
						// dogs are dead
						{
							leastNeighborArmies = neighbors[j].getArmies();
							expando = i;
							expandTo = neighbors[j].getCode();
						}
					}
				}
			}
		}
	}

	// returns true if every agent left is communist
	protected boolean communismWins()
	{
		for(int i = 0; i < numberOfPlayers; i++)
		{
			if(!getRealAgentName(i).equals(name())
					&& BoardHelper.playerIsStillInTheGame(i, countries))
			{
				// then a non-commie is still alive
				return false;
			}
		}
		// huzzah, the workers have won! now to kill off the other factions...
		debug("communism has won!");
		return true;
	}

	public void attackPhase()
	{
		setExpandos(); // this sets expando and expandTo

		if(expando == -1)
			return; // nowhere to go

		// Now we see if we have a good chance of taking the weakest link over:
		if(expandTo != -1
				&& countries[expando].getArmies() > countries[expandTo]
						.getArmies())
		{
			// We attack till dead, with max dice:
			makeAttack(expando, expandTo, true);
		}

		attackHogWild();
		attackStalemate();
	}

	// We want to divide the armies evenly between the two countries:
	public int moveArmiesIn(int countryCodeAttacker, int countryCodeDefender)
	{
		int totalArmies = countries[countryCodeAttacker].getArmies();

		return ((totalArmies + 1) / 2);
	}

	// To fortify, we cycle through all the countries.
	// If we own two touching continents we equalize the armies between them.
	// Hopefully this will propagate through to totally even out the armies in
	// the long run.
	public void fortifyPhase()
	{
		boolean changed = true;
		while(changed)
		{
			changed = false;
			for(int i = 0; i < numberOfCountries; i++)
			{
				if(countries[i].getOwner() == ID
						&& countries[i].getMoveableArmies() > 0)
				{
					// This means we COULD fortify out of this country if we
					// wanted to.

					// Get country[i]'s neighbours:
					Country[] neighbors = countries[i].getAdjoiningList();

					// Now loop through the neighbours and see if we own any of
					// them.
					for(int j = 0; j < neighbors.length
							&& countries[i].getMoveableArmies() > 0; j++)
					{
						if(neighbors[j].getOwner() == ID)
						{
							int difference = countries[i].getArmies()
									- neighbors[j].getArmies();
							// So we own a neighbor. Let's see if they have more
							// than one army difference:
							if(difference > 1)
							{
								// So we move half the difference:
								makeFortification(difference / 2, i, neighbors[j].getCode());
								changed = true;
								debug("fort");
							}
						}
					}
				}
			}
		}
	}

	public String youWon()
	{
		return "Welcome to the new world, Comrade.";
	}
}
