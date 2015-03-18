package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.List;
import java.util.ArrayList;

//
//  Pixie.java
//	Lux
//
//  Copyright (c) 2002-2008 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

/**
 * Pixie examines each continent and focuses on promising ones.
 * 
 * She picks countries in continents that have the fewest border points
 * 
 * At the start of each turn (inside placeArmies) she decides what continents
 * she will expend effort on. Then she will place and attack from those
 * continents.
 * 
 * Also she tries to get a card every turn She also runs attackHogWild()
 * 
 * When fortifying she will fort to the borders of continents that are on the
 * front lines.
 */

public class SimPixie extends SimSmartAgentBase
{
	protected float outnumberBy = 1; // for individual country attacks
	protected int borderForce = 20;
	protected boolean[] ourConts; // whether we will spend efforts taking/holding each
						// continent

	public String name()
	{
		return "Pixie";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "Pixie is a lovable little sprite. She enjoys kicking your ass.";
	}

	public int pickCountry()
	{
		// our first choice is the continent with the least # of borders that is
		// totally empty
		if(goalCont == -1
				|| !BoardHelper.playerOwnsContinentCountry(-1, goalCont,
						countries))
		{
			setGoalToLeastBordersCont();
		}

		// so now we have picked a cont...
		return pickCountryInContinent(goalCont);
	}

	// This method is a hook that EvilPixie uses to place better during hogwild
	protected boolean placeHogWild(int numberOfArmies)
	{
		return false;
	}

	// returns true if we want at least one continent
	protected boolean setupOurConts(int numberOfArmies)
	{
		if(ourConts == null)
			ourConts = new boolean[numberOfContinents];

		// calculate the armies needed to conquer each continent
		int[] neededForCont = new int[numberOfContinents];
		for(int i = 0; i < numberOfContinents; i++)
		{
			neededForCont[i] = BoardHelper.getEnemyArmiesInContinent(ID, i,
					countries); // enemies in the cont
			neededForCont[i] -= BoardHelper.getPlayerArmiesInContinent(ID, i,
					countries); // minus our armies in the cont
			// also minus our armies in countries neighboring the cont
			neededForCont[i] -= BoardHelper.getPlayerArmiesAdjoiningContinent(
					ID, i, countries);
		}

		// say we can give at most (1/numberOfContinents)*numberOfArmies armies to
		// each continent.
		boolean wantACont = false; // if we think we can take/hold any
									// continents
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(neededForCont[i] < (1.0 / numberOfContinents) * numberOfArmies
					&& getContinentBonus(i) > 0)
			{
				ourConts[i] = true;
				wantACont = true;
			} else
				ourConts[i] = false;
		}
		return wantACont;
	}

	public void placeArmies(int numberOfArmies)
	{
		if(placeHogWild(numberOfArmies))
			return;

		if(!setupOurConts(numberOfArmies))
		{
			// then we don't think we can take/hold any continents
			placeArmiesToTakeCont(numberOfArmies, getEasiestContToTake());
			return;
		}

		// divide our armies amongst the conts we want
		int armiesPlaced = 0;
		boolean oneNeedsHelp = true;
		while(armiesPlaced < numberOfArmies && oneNeedsHelp)
		{
			oneNeedsHelp = false;
			for(int c = 0; c < numberOfContinents && armiesPlaced < numberOfArmies; c++)
			{
				if(ourConts[c] && continentNeedsHelp(c))
				{
					placeArmiesToTakeCont(1, c);
					armiesPlaced++;
					oneNeedsHelp = true;
				}
			}
		}

		// We get here if all our borders are above borderforce.
		placeRemainder(numberOfArmies - armiesPlaced);
	}

	protected void placeRemainder(int numberOfArmies)
	{
		placeNearEnemies(numberOfArmies);
	}

	// place evenly amongst our countries that have enemies
	protected void placeNearEnemies(int numberOfArmies)
	{
		int i = 0;
		while(numberOfArmies > 0)
		{
			if(countries[i].getOwner() == ID
					&& countries[i].getNumberEnemyNeighbors() > 0)
			{
				makePlacement(1, i);
				numberOfArmies--;
			}
			i = (i + 1) % numberOfCountries;
		}
	}

	// Do we own all of the continents that this country borders?
	// NOTE: This will not check countries that are in the same continent as
	// 'center'
	protected boolean weOwnContsArround(Country center)
	{
		int cont = center.getContinent();
		CountryIterator n = new NeighborIterator(center);
		while(n.hasNext())
		{
			Country neib = n.next();
			if(neib.getContinent() != cont
					&& !BoardHelper.playerOwnsContinent(ID,
							neib.getContinent(), countries))
			{
				return false;
			}
		}
		return true;
	}

	protected boolean borderCountryNeedsHelp(Country border)
	{
		return border.getArmies() <= borderForce;
	}

	// a test of whether or not we should send some armies this cont's way
	protected boolean continentNeedsHelp(int cont)
	{
		// if we don't own it then it deffinately needs some help
		if(!BoardHelper.playerOwnsContinent(ID, cont, countries))
			return true;

		// otherwise we own it.
		// check each border
		int[] borders = BoardHelper.getContinentBorders(cont, countries);
		for(int i = 0; i < borders.length; i++)
		{
			if(borderCountryNeedsHelp(countries[borders[i]]))
				return true;
		}

		return false;
	}

	public void attackPhase()
	{
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(ourConts[i])
			{
				attackInContinent(i);
			}
		}

		attackForCard();
		attackHogWild();
		attackStalemate();
	}// End of attackPhase

	// Execute all the attacks possible in this continent where we outnumber the
	// enemy
	protected void attackInContinent(int cont)
	{
		CountryIterator continent = new ContinentIterator(cont, countries);
		while(continent.hasNext())
		{
			Country c = continent.next();
			if(c.getOwner() != ID)
			{
				// try and find a neighbor that we own, and attack this country
				CountryIterator neighbors = new NeighborIterator(c);
				while(neighbors.hasNext())
				{
					Country possAttack = neighbors.next();
					if(possAttack.getOwner() == ID
							&& possAttack.getArmies() > c.getArmies()
									* outnumberBy && c.getOwner() != ID
							&& possAttack.canGoto(c))
					{
						makeAttack(possAttack.getCode(), c.getCode(), true);
					}
				}
			}
		}
	}

	public int moveArmiesIn(int cca, int ccd)
	{
		// test if they border any enemies at all:
		int attackerEnemies = countries[cca].getNumberEnemyNeighbors();
		int defenderEnemies = countries[ccd].getNumberEnemyNeighbors();

		if(attackerEnemies > defenderEnemies)
			return 0;
		else if(defenderEnemies > attackerEnemies)
			return 1000000;
		else if(attackerEnemies > 0) // then they are tied at above 0
			return countries[cca].getArmies() / 2;

		// move our armies into continents that we want.
		if(ourConts[countries[cca].getContinent()]
				&& ourConts[countries[ccd].getContinent()])
		{ // we want both
			return countries[cca].getArmies() / 2;
		} else if(ourConts[countries[cca].getContinent()])
		{
			return 0; // leave them in attacker
		} else if(ourConts[countries[ccd].getContinent()])
		{
			return 1000000; // send to defender
		}

		// so now we want none of them
		// see if either of them border something we want
		int attackerEnemiesWanted = 0, defenderEnemiesWanted = 0;
		CountryIterator e = new NeighborIterator(countries[cca]);
		while(e.hasNext())
		{
			Country test = e.next();
			if(test.getOwner() != ID && ourConts[test.getContinent()])
			{
				attackerEnemiesWanted++;
			}
		}
		e = new NeighborIterator(countries[ccd]);
		while(e.hasNext())
		{
			Country test = e.next();
			if(test.getOwner() != ID && ourConts[test.getContinent()])
			{
				defenderEnemiesWanted++;
			}
		}

		if(attackerEnemiesWanted > defenderEnemiesWanted)
			return 0;
		else if(defenderEnemiesWanted > attackerEnemiesWanted)
			return 1000000;
		else if(attackerEnemiesWanted > 0) // then they are tied at above 0
			return countries[cca].getArmies() / 2;

		// So if we get here then they both border zero countries that we want.

		// Now if we are here then neither have any enemies.
		// we won't be able to use them to attack this turn

		// now just move in xxagentxx
		debug("Pixie moveArmiesIn not fully imped");
		return countries[cca].getArmies() / 2;
	}

	public void fortifyPhase()
	{
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(BoardHelper.playerOwnsContinent(ID, i, countries))
			{
				fortifyContinent(i);
			} else
			{
				fortifyContinentScraps(i);
			}
		}
	} // End of fortifyPhase() method

	protected void fortifyContinent(int cont)
	{
		// We work from the borders back, fortifying closer.
		// Start out by getting a List of the cont's borders:
		int[] borders = BoardHelper.getContinentBorders(cont, countries);
		List<Country> cluster = new ArrayList<Country>();
		for(int i = 0; i < borders.length; i++)
		{
			cluster.add(countries[borders[i]]);
		}

		// So now the cluster borders are in <cluster>. fill it up while
		// fortifying towards the borders.
		for(int i = 0; i < cluster.size(); i++)
		{
			CountryIterator neighbors = new NeighborIterator(
					(Country) cluster.get(i));
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() == ID && !cluster.contains(neighbor)
						&& neighbor.getContinent() == cont)
				{
					// Then <neighbor> is part of the cluster. fortify any
					// armies back and add to the List
					makeFortification(neighbor.getMoveableArmies(), neighbor.getCode(), cluster.get(i).getCode());
					cluster.add(neighbor);
				}
			}
		}
	}

	// called on continents that we don't own.
	// fortify our guys towards weak enemy countries.
	protected void fortifyContinentScraps(int cont)
	{
		CountryIterator e = new ContinentIterator(cont, countries);
		while(e.hasNext())
		{
			Country c = e.next();
			if(c.getOwner() == ID && c.getMoveableArmies() > 0)
			{
				// we COULD move armies from 'c'
				int weakestArmies = 1000000;
				Country weakestLink = null;
				// if it has a neighbor with a weaker enemy then move there
				CountryIterator n = new NeighborIterator(c);
				while(n.hasNext())
				{
					Country possMoveTo = n.next();
					if(possMoveTo.getOwner() == ID)
					{
						Country themWeak = possMoveTo.getWeakestEnemyNeighbor();
						if(themWeak != null
								&& themWeak.getArmies() < weakestArmies)
						{
							weakestArmies = possMoveTo
									.getWeakestEnemyNeighbor().getArmies();
							weakestLink = possMoveTo;
						}
					}
				}
				Country hereWeakest = c.getWeakestEnemyNeighbor();
				// if a neighbor has a weaker country then we do here move our
				// armies
				if(hereWeakest == null
						|| weakestArmies < hereWeakest.getArmies())
				{
					if(weakestLink != null)
						makeFortification(c.getMoveableArmies(), c.getCode(), weakestLink.getCode());
				}
			}
		}
	}

	public String youWon()
	{
		String[] answers = new String[] { "Eat my dust",
				"You didn't clap loud enough", "Make a wish",
				"Now fetch me Yakool", "Careful what you wish for",
				"Love one another, and be happy",
				"You can come out now Satyrs, \nit's pixie party time",
				"I'm the strongest woman in the world",
				"I want to be the girl with the most bracelets",
				"I'm not just cute! \nI am a serious fighter!",
				"All this bloodshed,\nI think I'm going to be sick..." };

		return answers[rand.nextInt(answers.length)];
	}
} // End of Pixie class
