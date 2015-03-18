package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.List;
import java.util.ArrayList;

//
//  BetterPixie.java
//	Lux
//
//  Copyright (c) 2002-2008 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

//
//	BetterPixie builds a concrete agent out of SmartAgentBase to do the following...
//
//	She picks countries in continents that have the fewest border points
//
//	At the start of each turn (inside placeArmies) she decides what continents she will expend effort on.
//  Then she will place and attack from those continents.
//
// 	Also she tries to get a card every turn
//	She also runs attackHogWild()
//
//	When fortifying she will fort to the borders of continents that are on the front lines.
//

public class SimBetterPixie extends SimSmartAgentBase
{
	float outnumberBy = 1; // for individual country attacks
	protected int borderForce = 20;
	boolean[] ourConts; // whether we will spend efforts taking/holding each
						// continent

	public String name()
	{
		return "BetterPixie";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "BetterPixie is a lovable little sprite. She enjoys kicking your ass.";
	}

	public void cardsPhase(Card[] cards)
	{
		super.cardsPhase(cards);
		cashCardsIfPossible(cards);
	}

	public int pickCountry()
	{
		// our first choice is the continent with the least # of borders that is
		// totally empty
		if(goalCont == -1)
		{
			setGoalToLeastBordersCont();
		}

		// so now we have picked a cont...
		return pickCountryInContinent(goalCont);
	}

	// This method is a hook that EvilPixie uses to place better during hogwild
	boolean placeHogWild(int numberOfArmies)
	{
		return false;
	}

	// returns true if we want at least one continent
	boolean setupOurConts(int numberOfArmies)
	{
		if(ourConts == null)
			ourConts = new boolean[numberOfContinents];

		// calculate the armies needed to conquer each continent
		int[] neededForCont = new int[numberOfContinents];
		for(int i = 0; i < numberOfContinents; i++)
		{
			neededForCont[i] = BoardHelper.getEnemyArmiesInContinent(ID, i,
					countries); // enemies in the cont
			neededForCont[i] *= 1.3; // add a multiple for losses

			int ourArmiesNearCont = BoardHelper.getPlayerArmiesInContinent(ID,
					i, countries)
					+ BoardHelper.getPlayerArmiesAdjoiningContinent(ID, i,
							countries);

			// If we have a big group near the continent then consider that as
			// well
			CountryRoute bestRoute = new CountryRoute(
					BoardHelper.cheapestRouteFromOwnerToCont(ID, i, countries),
					countries);
			int ourArmiesFartherAway = bestRoute.start().getArmies()
					- (int) (bestRoute.costNotCountingPlayer(ID) * 1.2);

			neededForCont[i] -= Math.max(ourArmiesNearCont,
					ourArmiesFartherAway);
		}

		// We will only concentrate on the easiest continent to take when we own
		// none
		boolean ownNoContinents = !mapHelper.playerOwnsAnyPositiveContinent(ID);
		int lowestArmiesNeededToTake = 1000000;
		int targetCont = -1;

		boolean wantACont = false; // if we think we can take/hold any
									// continents
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(ownNoContinents)
			{
				if(neededForCont[i] < lowestArmiesNeededToTake
						&& getContinentBonus(i) > 0)
				{
					lowestArmiesNeededToTake = neededForCont[i];
					targetCont = i;
				}
				ourConts[i] = false;
			}
			// say we can give at most numberOfArmies/(numContinents/4) armies
			// to each continent.
			else if(neededForCont[i] < numberOfArmies / (numberOfContinents / 4.0)
					&& getContinentBonus(i) > 0)
			{
				ourConts[i] = true;
				wantACont = true;
			} else
				ourConts[i] = false;
		}

		if(ownNoContinents)
		{
			if(targetCont == -1)
				return false;

			ourConts[targetCont] = true;
			return true;
		}

		return wantACont;
	}

	public void placeArmies(int numberOfArmies)
	{
		if(placeHogWild(numberOfArmies))
			return;

		// Calculate what continents we can take/hold
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

		// We place the dregs if all our borders are above borderforce.
		if(armiesPlaced < numberOfArmies)
			placeRemainder(numberOfArmies - armiesPlaced);
	}

	protected void placeRemainder(int numberOfArmies)
	{
		placeNearEnemies(numberOfArmies, true);
	}

	/**
	 * Place our armies so they are next to enemy clusters. If 'minimumToWin' is
	 * true then place the minimum number of armies needed to conquer each
	 * cluster (starting at the smallest). If 'minimumToWin' is false then place
	 * armies evenly for each cluster.
	 */
	protected void placeNearEnemies(int numberOfArmies, boolean minimumToWin)
	{
		// Divide all enemy countries into clusters:
		CountryClusterSet clusters = CountryClusterSet.getAllCountriesNotOwnedBy(ID, countries);

		if(minimumToWin)
			clusters.orderWeakestFirst();

		// Now place beside each enemy cluster
		for(int i = 0; i < clusters.size() && numberOfArmies > 0; i++)
		{
			CountryCluster cluster = clusters.getCluster(i);

			Country placeOn = cluster.getStrongestNeighborOwnedBy(ID);
			if(placeOn != null)
			{
				int numberToPlace = numberOfArmies / clusters.size();
				if(minimumToWin)
				{
					numberToPlace = cluster.estimatedNumberOfArmiesNeededToConquer() - placeOn.getArmies();
				}
				
				numberToPlace = Math.max(Math.min(numberOfArmies, numberToPlace),0);
				if(numberToPlace < 0)
				{
					System.out.println(placeOn.getName() + ":" + placeOn.getArmies());
				}
				makePlacement(numberToPlace, placeOn.getCode());
				if(numberToPlace < 0)
				{
					System.out.println(placeOn.getName() + ":" + placeOn.getArmies());
				}
				numberOfArmies -= numberToPlace;
			}
		}

		if(numberOfArmies > 0)
		{
			// we still have some left ?
			// This method is AWFUL!!!
			System.out.println("BetterPixie still has " + numberOfArmies
					+ " left to place in a really bad manner");
			// Thread.dumpStack();
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
	}

	boolean borderCountryNeedsHelp(Country border)
	{
		return border.getArmies() <= borderForce && !weOwnContsArround(border);
	}

	// a test of whether or not we should send some armies this cont's way
	protected boolean continentNeedsHelp(int cont)
	{
		// if we don't own it then it definitely needs some help
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
		// Try and take over all the continents we want
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(ourConts[i])
				attackInContinent(i);
		}

		// Take out any badly defended conts next to us
		for(int i = 0; i < numberOfContinents; i++)
			takeOutContinentCheck(i);

		attackForCard();
		attackHogWild();
		attackStalemate();
	}// End of attackPhase

	// If we think we can take over this continent then execute lots of attacks
	protected void attackInContinent(int cont)
	{
		// Count the enemies and friendlies:
		int enemyCount = BoardHelper.getEnemyArmiesInContinent(ID, cont,
				countries); // enemies in the cont
		// add a multiple for losses and for the # of enemy countries
		enemyCount *= 1.3;
		CountryIterator continent = new ContinentIterator(cont, countries);
		while(continent.hasNext())
			if(continent.next().getOwner() != ID)
				enemyCount++;

		// Count the friendlies (our armies in the cont and our armies in
		// countries neighboring the cont)
		int friendlyCount = BoardHelper.getPlayerArmiesInContinent(ID, cont,
				countries);
		friendlyCount += BoardHelper.getPlayerArmiesAdjoiningContinent(ID,
				cont, countries);

		if(enemyCount > friendlyCount)
			return;

		boolean attackMade = true;
		while(attackMade)
		{
			// We cycle through the continent 2 seperate times...

			// Start by only attacking from our countries that have 1 enemy
			// country
			while(attackMade)
			{
				attackMade = false;

				continent = new ContinentIterator(cont, countries);
				while(continent.hasNext())
				{
					Country c = continent.next();
					if(c.getOwner() == ID
							&& getNumberOfEnemyNeighborsInOurConts(c) == 1
							&& c.getArmies() > 1)
					{
						// This country can only attack 1 good destination, so
						// attack it
						Country[] adjoining = c.getAdjoiningList();
						for(int i = 0; i < adjoining.length; i++)
							if(adjoining[i].getOwner() != ID
									&& ourConts[adjoining[i].getContinent()]
									&& c.getArmies() > adjoining[i].getArmies()
											* outnumberBy)
							{
								makeAttack(c.getCode(), adjoining[i].getCode(), true);
								attackMade = true;
							}
					}
				}
			}

			// Now make any good attacks we can...
			continent = new ContinentIterator(cont, countries);
			while(continent.hasNext())
			{
				Country c = continent.next();
				if(c.getOwner() != ID)
				{
					// try and find a neighbor that we own, and attack this
					// country
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
							attackMade = true;
						}
					}
				}
			}
		}
	}

	// A check to see if someone else owns this continent. If they do then we
	// try to kill it
	protected void takeOutContinentCheck(int cont)
	{
		if(BoardHelper.anyPlayerOwnsContinent(cont, countries)
				&& getContinentBonus(cont) > 0)
		{
			if(countries[BoardHelper.getCountryInContinent(cont, countries)]
					.getOwner() != ID)
			{
				debug("enemy owns continent " + cont);
				// then an enemy owns this continent.
				// calculate if it's worth it to hit the continent
				/*
				 * int continentBonus = board.getContinentBonus(cont); int[]
				 * path = BoardHelper.cheapestRouteFromOwnerToCont(ID, cont,
				 * countries); if (path != null) { int costToHit =
				 * pathCost(path); if (costToHit < continentBonus) {
				 * System.out.println(board.getPlayerName(ID)+
				 * " thinks it is worth it to take out continent "
				 * +board.getContinentName
				 * (cont)+" from player "+board.getPlayerName
				 * (countries[BoardHelper.getCountryInContinent(cont,
				 * countries)]
				 * .getOwner())+" ("+continentBonus+" bonus vs "+costToHit
				 * +" costToHit)"); } }
				 */

				// Check all of it's borders for a weak spot
				int[] borders = BoardHelper
						.getContinentBorders(cont, countries);
				for(int b = 0; b < borders.length; b++)
				{
					Country[] neigbors = countries[borders[b]]
							.getAdjoiningList();
					for(int n = 0; n < neigbors.length; n++)
					{
						if(neigbors[n].getOwner() == ID
								&& neigbors[n].getArmies() > countries[borders[b]]
										.getArmies() * 2
								&& neigbors[n].canGoto(countries[borders[b]]))
						{
							// kill him
							debug("attacking to take out continent " + cont);
							if(makeAttack(neigbors[n].getCode(), countries[borders[b]].getCode(),
									true) > 0)
								return;
						}
					}
				}
			}
		}
	}

	protected int getNumberOfEnemyNeighborsInOurConts(Country c)
	{
		int result = 0;

		Country[] adjoining = c.getAdjoiningList();
		for(int i = 0; i < adjoining.length; i++)
			if(adjoining[i].getOwner() != ID
					&& ourConts[adjoining[i].getContinent()])
				result++;

		return result;
	}

	public int moveArmiesIn(int cca, int ccd)
	{
		int testCode = obviousMoveArmiesInTest(cca, ccd);
		if(testCode != -1)
			return testCode;

		testCode = memoryMoveArmiesInTest(cca, ccd);
		if(testCode != -1)
			return testCode;

		// test if they border any enemies at all:
		int attackerEnemies = countries[cca].getNumberEnemyNeighbors();
		int defenderEnemies = countries[ccd].getNumberEnemyNeighbors();

		if(attackerEnemies == 0 && defenderEnemies != 0)
			return 1000000;
		else if(attackerEnemies != 0 && defenderEnemies == 0)
			return 0;

		// Possibly they both have 0 enemies:
		else if(defenderEnemies == 0)
			return countries[cca].getArmies() / 2;

		// OK, so they both have some enemies. Look again only considering the
		// enemies that are in conts we care about.
		// (And make a note of the enemies for later)
		List<Country> attackerEnemyList = new ArrayList<Country>();
		List<Country> defenderEnemyList = new ArrayList<Country>();
		attackerEnemies = 0;
		defenderEnemies = 0;
		Country[] adjoining = countries[cca].getAdjoiningList();
		for(int i = 0; i < adjoining.length; i++)
			if(adjoining[i].getOwner() != ID
					&& ourConts[adjoining[i].getContinent()])
				attackerEnemyList.add(adjoining[i]);
		adjoining = countries[ccd].getAdjoiningList();
		for(int i = 0; i < adjoining.length; i++)
			if(adjoining[i].getOwner() != ID
					&& ourConts[adjoining[i].getContinent()])
				defenderEnemyList.add(adjoining[i]);

		if(attackerEnemyList.size() == 0 && defenderEnemyList.size() != 0)
			return 1000000;
		else if(attackerEnemyList.size() != 0 && defenderEnemyList.size() == 0)
			return 0;

		// Possibly they both have 0 enemies in conts we want:
		else if(attackerEnemyList.size() == 0)
			return countries[cca].getArmies() / 2;

		// OK, so they both have some enemies in continents we care about. Do
		// they connect?
		List<Country> allEnemies = new ArrayList<Country>(attackerEnemyList);
		for(int i = 0; i < defenderEnemyList.size(); i++)
			if(!allEnemies.contains(defenderEnemyList.get(i)))
				allEnemies.add(defenderEnemyList.get(i));
		CountryClusterSet enemySet = CountryClusterSet.getHostileCountries(ID,
				allEnemies);

		// If they all connect then move everyone in
		if(enemySet.numberOfClusters() == 1)
			return 1000000;

		return countries[cca].getArmies() / 2;
	}

	public void fortifyPhase()
	{
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(BoardHelper.playerOwnsContinent(ID, i, countries))
			{
				while(fortifyContinent(i))
				{
					
				}
			} else
			{
				fortifyContinentScraps(i);
			}
		}
	} // End of fortifyPhase() method

	protected boolean fortifyContinent(int cont)
	{
		boolean fortifiedSomething = false;

		// We work from the borders back, fortifying closer.
		// Start out by getting a List of the cont's borders:
		int [] borders = BoardHelper.getContinentBorders(cont, countries);
		List<Country> cluster = new ArrayList<Country>();
		for(int i = 0; i < borders.length; i++)
		{
			cluster.add(countries[borders[i]]);
		}

		// So now the cluster borders are in <cluster>. fill it up while
		// fortifying towards the borders.
		for(int i = 0; i < cluster.size(); i++)
		{
			CountryIterator neighbors = new NeighborIterator(cluster.get(i));
			
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() == ID && !cluster.contains(neighbor)
						&& neighbor.getContinent() == cont
						&& neighbor.getMoveableArmies() > 0)
				{
					debug(" -> fortify " + neighbor.getMoveableArmies()
							+ " armies from " + neighbor + " to "
							+ cluster.get(i));
					
					// Then <neighbour> is part of the cluster. fortify any
					// armies back and add to the List
					if(makeFortification(neighbor.getMoveableArmies(),
							neighbor.getCode(), cluster.get(i).getCode()) == 1)
					{
						fortifiedSomething = true;
					}
					cluster.add(neighbor);
				}
			}
		}

		return fortifiedSomething;
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
						makeFortification(c.getMoveableArmies(), c.getCode(),
								weakestLink.getCode());
				}
			}
		}
	}

	public String youWon()
	{
		String[] answers = new String[] { "Poof! I win" };

		return answers[rand.nextInt(answers.length)];
	}
} // End of Pixie class
