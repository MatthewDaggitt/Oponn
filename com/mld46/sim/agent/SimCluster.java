package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Cluster.java
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
 * Cluster expands from his biggest cluster of countries. In particlular...
 * 
 * He picks countries in continents decided by the pickGoalContChoosing()
 * method. This method chooses medium sized continents, starting with fully
 * unowned ones.
 * 
 * He places armies evenly along the border of the cluster centered on the
 * biggest owned continent or centered on the his biggest army if no continents
 * are owned
 * 
 * He runs attackFromCluster() on the biggest cluster with reckless abandon
 * 
 * Goes Hog-Wild attacking everywhere when he outnumbers everyone
 * 
 * Uses the moveInMemory variable in various places to control army advances
 * 
 * Fortifies to the borders of the biggest cluster
 * 
 * Cluster doesn't really care about continents. He only thinks about them when
 * choosing his initial countries. He doesn't try to take them over or defend
 * them.
 */

public class SimCluster extends SimSmartAgentBase
{
	public String name()
	{
		return "Cluster";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "Cluster is a radical dude. He enjoys starting a party and then spreading it.";
	}

	// Use the pickGoalContChoosing() method to choose a continent.
	// Then use the SmartAgentBase method pickCountryInContinent() to select the
	// best country.
	public int pickCountry()
	{
		// If we have yet to choose a goal or our goal is fully owned then
		// choose a goal
		if(goalCont == -1
				|| !BoardHelper.playerOwnsContinentCountry(-1, goalCont,
						countries))
		{
			goalCont = pickGoalContChoosing();
		}

		return pickCountryInContinent(goalCont);
	}

	/*
	 * pickGoalContChoosing()... This method examines all the continents to see
	 * which one we should try and take over at the very beginning of the game.
	 * 
	 * It returns the continentCode of the continent it decides on.
	 * 
	 * For now it picks the continent that has the most countries but not over
	 * <max>, where <max> is dependant on the number of countries and players.
	 * It checks for fully unowned conts first, then any goes on to any conts
	 */
	protected int pickGoalContChoosing()
	{
		int max = (int) Math.ceil((numberOfCountries * 1.25)
				/ numberOfPlayers);

		// First of all we look at all the continents, and choose the biggest
		// one (but not over <max>)
		// that is FULLY empty
		int bigUnownedContSize = 0;
		int bigUnownedCont = -1;
		for(int cont = 0; cont < numberOfContinents; cont++)
		{
			if(getContinentBonus(cont) > 0)
			{
				int size = BoardHelper.getContinentSize(cont, countries);
				if(size < max && size > bigUnownedContSize
						&& BoardHelper.playerOwnsContinent(-1, cont, countries))
				{
					bigUnownedContSize = size;
					bigUnownedCont = cont;
				}
			}
		}

		if(bigUnownedCont != -1)
			return bigUnownedCont;

		// OK So if we get here then there are no conts that are FULLY unowned.
		// We choose the biggest continent under <max> that has ANYTHING
		// unowned.
		bigUnownedContSize = 0;
		bigUnownedCont = -1;
		for(int cont = 0; cont < numberOfContinents; cont++)
		{
			if(getContinentBonus(cont) > 0)
			{
				int size = BoardHelper.getContinentSize(cont, countries);
				if(size < max
						&& size > bigUnownedContSize
						&& BoardHelper.playerOwnsContinentCountry(-1, cont,
								countries))
				{
					bigUnownedContSize = size;
					bigUnownedCont = cont;
				}
			}
		}

		if(bigUnownedCont != -1)
			return bigUnownedCont;

		// We only get to here if all the continents that are left with less
		// than <max> countries are all taken.
		// Or all the other continents are worth 0 or less.

		// start by looking for continents worth 0 (better then negative)
		for(int cont = 0; cont < numberOfContinents; cont++)
			if(getContinentBonus(cont) == 0)
				return cont;

		// we pick the smallest one that has something to pick:
		int smallestOpenCont = -1;
		int smallestSize = 1000;
		for(int cont = 0; cont < numberOfContinents; cont++)
		{
			int size = BoardHelper.getContinentSize(cont, countries);
			if(size >= max
					&& size < smallestSize
					&& BoardHelper.playerOwnsContinentCountry(-1, cont,
							countries))
			{
				smallestOpenCont = cont;
				smallestSize = size;
			}
		}

		if(smallestOpenCont != -1)
			return smallestOpenCont;

		// we should never get here.
		System.out.println("ERROR in Cluster.pickGoalContChoosing  323230032");
		return -1;
	}

	// Use the SmartAgentBase method placeArmiesOnClusterBorder() to place our
	// armies evenly along a cluster.
	public void placeArmies(int numberOfArmies)
	{
		if(mapHelper.playerOwnsAnyPositiveContinent(ID))
		{
			// Center the cluster on the biggest continent we own
			int ownCont = getMostValuablePositiveOwnedCont();
			placeArmiesOnClusterBorder(numberOfArmies,
					countries[BoardHelper.getCountryInContinent(ownCont,
							countries)]);
		} else
		{
			// Center the cluster on the easiest continent to take
			int wantCont = getEasiestContToTake(); // getEasiestContToTake() is
													// a SmartAgentBase method
			placeArmiesToTakeCont(numberOfArmies, wantCont);
		}
	}

	public void attackPhase()
	{
		if(mapHelper.playerOwnsAnyPositiveContinent(ID))
		{
			int ownCont = getMostValuablePositiveOwnedCont();
			Country root = countries[BoardHelper.getCountryInContinent(ownCont,
					countries)];
			attackFromCluster(root);
		} else
		{
			// get our biggest army group:
			Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
			attackFromCluster(root);
		}

		attackHogWild(); // this only does anything if we outnumber everyone
		attackStalemate();
	}

	protected void attackFromCluster(Country root)
	{
		// now run some attack methods for the cluster centered around root:
		if(root != null)
		{
			// expand as long as possible the easyist ways
			while(attackEasyExpand(root))
			{
			}

			attackFillOut(root);

			while(attackEasyExpand(root))
			{
			}

			while(attackConsolidate(root))
			{
			}

			while(attackSplitUp(root, 1.2f))
			{
			}
		}
	}

	public int moveArmiesIn(int cca, int ccd)
	{
		int test = obviousMoveArmiesInTest(cca, ccd);
		if(test != -1)
			return test;

		test = memoryMoveArmiesInTest(cca, ccd);
		if(test != -1)
			return test;

		Country aweakest = countries[cca]
				.getWeakestEnemyNeighborInContinent(goalCont);
		Country dweakest = countries[ccd]
				.getWeakestEnemyNeighborInContinent(goalCont);

		if(dweakest == null
				|| (aweakest != null && aweakest.getArmies() < dweakest
						.getArmies()))
			// attacking country has a weaker neighbor. leave armies there
			return 0;

		return 1000000;
	}

	public void fortifyPhase()
	{
		if(mapHelper.playerOwnsAnyPositiveContinent(ID))
		{
			int ownCont = getMostValuablePositiveOwnedCont();
			fortifyCluster(countries[BoardHelper.getCountryInContinent(ownCont,
					countries)]);
		} else
		{
			Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
			fortifyCluster(root);
		}
	} // End of fortifyPhase() method

	public String youWon()
	{
		String[] answers = new String[] { "Sweetness...",
				"Milkshakes for everyone",
				"Time for the global underground \nto rock the overground",
				"Silly little muggles", "My middle name is Beowulf",
				"Clusta da Busta", "Cluster knows how to muster his troops",
				"I declare today a global day of funk", "Dude! That was sweet!" };

		return answers[rand.nextInt(answers.length)];
	}
} // End of Cluster class
