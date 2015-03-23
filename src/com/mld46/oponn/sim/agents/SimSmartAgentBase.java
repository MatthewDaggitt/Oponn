package com.mld46.oponn.sim.agents;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  SmartAgentBase.java
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
 An abstract agent class containing a variety of utility methods for subclasses to use.
 */

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public abstract class SimSmartAgentBase extends SimAgent
{
	// Sometimes in the attack phase we know if we want to move in armies or
	// not.
	// Store the value in this var. It will be used as long as it is not equal
	// to -1.
	protected int moveInMemory;

	// Sometimes it is useful to remember which continent we are spending our
	// efforts on.
	// When not in use set to -1.
	protected int goalCont;

	// Since all subclasses need a random number generator (for you-won strings)
	// we keep one.
	protected Random rand;

	public SimSmartAgentBase()
	{
		rand = new Random();
		goalCont = -1;
		moveInMemory = -1;
	}

	// I have yet to develope any card-smarts. The game-world will automatically
	// cash in our best set if we return from this method and still have five or
	// more cards.
	public void cardsPhase(Card[] cards)
	{
		mustKillPlayer = -1; // just in case it was set last turn.
		// xxagentxx SmartAgentBase cards phase
	}

	public void cashCardsIfPossible(Card[] cards)
	{
		if(Card.containsASet(cards))
		{
			Card[] set = Card.getBestSet(cards, ID, countries);
			makeCardCash(set[0], set[1], set[2]);
		}
	}

	// This method sets the class-variable goalCont to the continent with the
	// least number of borders.
	// First all totally non-occupied conts will be searched, then partially
	// ocupied ones.
	// Conts worth 0 or less are not considered.
	protected void setGoalToLeastBordersCont()
	{
		goalCont = -1;

		int[] borderSizes = new int[numberOfContinents];
		int smallBorders = 1000000; // the size of the smallest borders cont

		// first loop through and find the smallest totally empty cont
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(getContinentBonus(i) > 0)
			{
				borderSizes[i] = BoardHelper.getContinentSize(i, countries);
				if(borderSizes[i] < smallBorders
						&& BoardHelper.playerOwnsContinent(-1, i, countries))
				{
					smallBorders = borderSizes[i];
					goalCont = i;
				}
			}
		}

		// if there were no empty conts then next we would like the cont with
		// the least # of borders that is partially empty
		if(goalCont == -1)
		{
			smallBorders = 1000000;
			for(int i = 0; i < numberOfContinents; i++)
			{
				if(getContinentBonus(i) > 0)
				{
					if(borderSizes[i] < smallBorders
							&& BoardHelper.playerOwnsContinentCountry(-1, i,
									countries))
					{
						smallBorders = borderSizes[i];
						goalCont = i;
					}
				}
			}
		}
		// There is the possibility that no cont will be chosen, if all the
		// continents with
		// open countries are worth 0 or less income
	}

	// If goalCont is set then return a country-code of the country we should
	// choose in that cont. If goalCont is unset then set it to the smallest
	// empty/open cont.
	protected int pickCountryInSmallContinent()
	{
		if(goalCont == -1
				|| !BoardHelper.playerOwnsContinentCountry(-1, goalCont,
						countries))
		// then we don't have a target cont yet
		{
			goalCont = -1;
			goalCont = mapHelper.getSmallestPositiveEmptyContinent();

			if(goalCont == -1) // oops, there are no unowned conts
				goalCont = mapHelper.getSmallestPositiveOpenContinent();
		}

		// if we are here then we DO have a target cont.
		return pickCountryInContinent(goalCont);
	}

	// return an unowned country-code in <continent>, preferably near others we
	// own
	// If there are no countries left in the given continent then pick a country
	// touching us.
	protected int pickCountryInContinent(int continent)
	{
		CountryIterator continentIter = new ContinentIterator(continent,
				countries);
		while(continentIter.hasNext())
		{
			Country c = continentIter.next();
			if(c.getOwner() == -1 && c.getNumberPlayerNeighbors(ID) > 0)
				return c.getCode();
		}

		// we neighbor none of them, so pick the open country with the fewest
		// neighbors
		continentIter = new ContinentIterator(continent, countries);
		int bestCode = -1;
		int fewestNeib = 1000000;
		while(continentIter.hasNext())
		{
			Country c = continentIter.next();
			if(c.getOwner() == -1 && c.getNumberNeighbors() < fewestNeib)
			{
				bestCode = c.getCode();
				fewestNeib = c.getNumberNeighbors();
			}
		}

		if(bestCode == -1)
		{
			// There are no unowned countries in this continent.
			return pickCountryTouchingUs();
		}

		return bestCode;
	}

	/** Pick the open country that touches us the most. */
	protected int pickCountryTouchingUs()
	{
		int maxTouches = -1;
		int maxCode = -1; // the country code of the best place so far
		// Loop through all the unowned countries
		CountryIterator ci = new PlayerIterator(-1, countries);
		while(ci.hasNext())
		{
			Country open = ci.next();
			if(open.getNumberPlayerNeighbors(ID) > maxTouches
					&& getContinentBonus(open.getContinent()) >= 0)
			{
				maxTouches = open.getNumberPlayerNeighbors(ID);
				maxCode = open.getCode();
			}
		}

		if(maxTouches < 1)
		{
			// Then no open countries touch any of our countries directly.
			// Do a search outwards to find the closest unowned country to us.

			List<Country> ourBorderCountries = new ArrayList<Country>();
			ci = new PlayerIterator(ID, countries);
			while(ci.hasNext())
			{
				Country open = ci.next();
				if(open.getNumberNotPlayerNeighbors(ID) > 0)
				{
					ourBorderCountries.add(open);
				}
			}

			return BoardHelper.closestCountryWithOwner(ourBorderCountries, -1,
					countries);
		}

		return maxCode;
	}

	// returns the country-code of the nearest unowned country to the cluster
	// starting at <root>
	// NOTE, this method is inferior to pickCountryTouchingUs().
	protected int pickCountryNearCluster(Country root)
	{
		// do a breadth first search outwards starting with this cluster's
		// borders
		// return as soon as we find an unowned country
		List<Country> borders = getClusterBorders(root.getCode());

		for(Country border : borders)
		{
			CountryIterator neighbors = new NeighborIterator(border);
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() == -1)
					return neighbor.getCode();
				else if(!borders.contains(neighbor))
				{
					// Then we add it to the List. in time its neighbors will
					// get expanded
					borders.add(neighbor);
				}
			}
		}

		// we should never get here.
		System.out
				.println("ERROR in smartbase.pickCountryNearCluster() 65465477");
		return -1;
	}

	public void placeInitialArmies(int numberOfArmies)
	{
		placeArmies(numberOfArmies);
	}

	// a simplistic method to get the easiest continent for us to take over
	protected int getEasiestContToTake()
	{
		// For each continent we calculate the ratio of (our armies):(enemy
		// armies)
		// The biggest one wins.
		float easiestContRatio = -1;
		int easiestCont = -1;
		for(int cont = 0; cont < numberOfContinents; cont++)
		{
			int enemies = BoardHelper.getEnemyArmiesInContinent(ID, cont,
					countries);
			int ours = BoardHelper.getPlayerArmiesInContinent(ID, cont,
					countries);
			float newratio = (float) ours / (float) enemies;
			if(newratio > easiestContRatio && getContinentBonus(cont) > 0)
			{
				easiestCont = cont;
				easiestContRatio = newratio;
			}
		}

		return easiestCont;
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

	// should be called within placeArmies()
	// will place them at the start of the cheapest path to <wantCont>
	protected void placeArmiesToTakeCont(int numberOfArmies, int wantCont)
	{
		if(wantCont == -1)
		{ // we weren't given a continent index. maybe there is only 1
			// continent. fallback to another method
			placeArmiesOnClusterBorder(numberOfArmies,
					BoardHelper.getPlayersBiggestArmy(ID, countries));
			return;
		}

		// we want to place our armies strategically, in order to conquer
		// <wantCont>
		if(BoardHelper.playerOwnsContinent(ID, wantCont, countries))
		{
			// then we already own it, place on the weakest borders that we
			// don't envelope
			int[] borders = BoardHelper
					.getContinentBorders(wantCont, countries);
			int placed = 0;
			while(placed < numberOfArmies)
			{
				int leastArmies = 1000000, leastID = -1;
				for(int i = 0; i < borders.length; i++)
				{
					if(countries[borders[i]].getArmies() < leastArmies
							&& !weOwnContsArround(countries[borders[i]]))
					{
						leastArmies = countries[borders[i]].getArmies();
						leastID = borders[i];
					}
				}
				if(leastID == -1)
				{ // this can happen when the entire map is one continent. thus
					// it has no borders
					leastID = borders[rand.nextInt(borders.length)];
				}
				makePlacement(1, leastID);
				placed++;
			}
			return;
		}

		// if we own any countries in <wantCont>, then place the armies on the
		// one with the most enemies inside the continent.
		CountryIterator want = new ContinentIterator(wantCont, countries);
		Country bestPlace = null;
		int mostEnemies = 0;
		while(want.hasNext())
		{
			Country us = want.next();
			if(us.getOwner() == ID)
			{
				// count its enemy neighbors inside wantCont:
				int enemyNeighbors = 0;
				CountryIterator neighbors = new NeighborIterator(us);
				while(neighbors.hasNext())
				{
					Country neighbor = neighbors.next();
					if(neighbor.getOwner() != ID
							&& neighbor.getContinent() == wantCont)
						enemyNeighbors++;
				}

				if(enemyNeighbors > mostEnemies)
				{
					mostEnemies = enemyNeighbors;
					bestPlace = us;
				}
			}
		}

		// if we found anyplace at all, do it
		if(bestPlace != null)
		{
			makePlacement(numberOfArmies, bestPlace.getCode());
			return;
		}

		// If we got here then we own zero countries inside <wantCont>
		// we place our armies in the country we own with the cheapest route to
		// <wantCont>
		int[] route = BoardHelper.cheapestRouteFromOwnerToCont(ID, wantCont,
				countries);
		debug("BoardHelper.cheapestRouteFromOwnerToCont(" + ID + ", "
				+ wantCont + ") = " + new CountryRoute(route, countries));
		int placer = route[0];
		makePlacement(numberOfArmies, placer);
	}

	// this method places armies one at a time on the weakest border surrounding
	// <root>
	// it will call itself untill all armies have been placed.
	protected void placeArmiesOnClusterBorder(int numberOfArmies, Country root)
	{
		if(root == null)
			System.out
					.println("SmartBase.placeArmiesOnClusterBorder() -> the cluster root==null. 654213465");

		List<Country> borders = getClusterBorders(root.getCode());

		// Find the weakest of the cluster borders:
		Country weakest = null;
		int weakestArmies = 1000000;
		for(Country border : borders)
		{
			if(border.getArmies() < weakestArmies)
			{
				weakestArmies = border.getArmies();
				weakest = border;
			}
		}

		if(weakest == null)
		{
			System.out
					.println("SmartBase.placeArmiesOnClusterBorder() -> weakest==null. 7404524");
			makePlacement(numberOfArmies, root.getCode());
			return;
		}

		int numberToPlace = Math.min(numberOfArmies,
				Math.max(1, numberOfArmies / 100));

		makePlacement(numberToPlace, weakest.getCode());
		if(numberOfArmies > numberToPlace)
			placeArmiesOnClusterBorder(numberOfArmies - numberToPlace, root);
	}

	/***********
	 * The actual attack methods are below.
	 * 
	 * Different skill level of agents can use different combinations of them.
	 ************/

	// If any of our border countries around <root>'s cluster have only one
	// enemy then attack it
	// (if they have some chance of winning)
	// return true if we won at least one attack
	protected boolean attackEasyExpand(Country root)
	{
		// get the borders of the cluster centered on <root>:
		List<Country> borders = getClusterBorders(root.getCode());
		
		if(borders == null)
		{
			System.out.println("eerr..");
		}
		
		boolean wonAttack = false;
		for(Country border : borders)
		{
			CountryIterator neighbors = new NeighborIterator(border);
			int enemies = 0;
			Country enemy = null;
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() != ID)
				{
					enemies++;
					enemy = neighbor;
				}
			}
			if(enemies == 1 && border.getArmies() > enemy.getArmies())
			{
				// then we will attack that one country and move everything in,
				// thus expanding our borders.
				moveInMemory = 1000000;
				if(makeAttack(border.getCode(), enemy.getCode(), true) > 0)
					wonAttack = true;
				moveInMemory = -1;
			}
		}
		return wonAttack;
	}

	// Attack any countries next to <root>'s cluster that has zero
	// not-owned-by-us neighbors
	// This kills little islands to fill out our territory.
	// return true if we won at least one attack
	protected boolean attackFillOut(Country root)
	{
		boolean wonAttack = false;
		List<Country> borders = getClusterBorders(root.getCode());
		for(Country border : borders)
		{
			CountryIterator neighbors = new NeighborIterator(border);
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() != ID
						&& neighbor.getNumberNotPlayerNeighbors(ID) == 0)
				{
					// attack it
					if(neighbor.getArmies() < border.getArmies())
					{
						moveInMemory = 0; // since we are attacking from a
											// border we remember to move zero
											// armies in
						if(makeAttack(border.getCode(), neighbor.getCode(),
								true) == 7)
							wonAttack = true;
						moveInMemory = -1;
					}
				}
			}
		}
		return wonAttack;
	}

	// If we can, we consolidate our borders, by attacking from two or more
	// borderCountries into a common enemy
	// return true if we won at least one attack
	protected boolean attackConsolidate(Country root)
	{
		List<Country> borders = getClusterBorders(root.getCode());
		boolean wonAttack = false;

		for(Country border : borders)
		{
			CountryIterator neighbors = new NeighborIterator(border);
			int enemies = 0;
			Country enemy = null;
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() != ID)
				{
					enemies++;
					enemy = neighbor;
				}
			}
			if(enemies == 1 && enemy.getNumberPlayerNeighbors(ID) > 1)
			{
				// then this enemy could be a good point to consolidate.
				// look for other border countries to consolidate into enemy...
				List<Country> ours = new ArrayList<Country>(); // this will
																// store all the
																// countries
																// that will
																// participate
																// in the
																// attack.
				CountryIterator possibles = new NeighborIterator(enemy);
				while(possibles.hasNext())
				{
					Country poss = possibles.next();
					if(poss.getOwner() == ID
							&& poss.getNumberEnemyNeighbors() == 1)
					{
						// then <poss> will join in the merge into <enemy>
						ours.add(poss);
					}
				}
				// now we attack if the odds are good.
				int ourArmies = 0;
				for(int i = 0; i < ours.size(); i++)
					ourArmies += (ours.get(i)).getArmies();
				if(ourArmies > enemy.getArmies())
				{
					// AAaaaaaaaaaeeeeeeeeeiiiiiiiii! Attack him from all our
					// countries
					for(int i = 0; i < ours.size() && enemy.getOwner() != ID; i++)
					{
						if((ours.get(i)).getArmies() > 1
								&& (ours.get(i)).canGoto(enemy))
						{
							moveInMemory = 1000000;
							if(makeAttack(ours.get(i).getCode(),
									enemy.getCode(), true) > 0)
								wonAttack = true;
						}
					}
					moveInMemory = -1;
				}
			}
		}
		return wonAttack;
	}

	// wherever we have a border that outnumbers an enemy, we attack the enemy.
	// let moveIn decide what to do if we win.
	// return true if we won at least one attack
	protected boolean attackSplitOff(Country root)
	{
		moveInMemory = -1;
		List<Country> borders = getClusterBorders(root.getCode());
		boolean wonAttack = false;

		for(Country border : borders)
		{
			CountryIterator neighbors = new NeighborIterator(border);
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() != ID
						&& neighbor.getArmies() < border.getArmies())
				{
					if(makeAttack(border.getCode(), neighbor.getCode(), true) > 0)
						wonAttack = true;
				}
			}
		}
		return wonAttack;
	}

	// for each border of <root>'s cluster, we split up our border country into
	// its ememy countries.
	// but only when (our armies) > (enemy armies)*attackRatio.
	// An attack ratio of 1.0 is when we at least tie them
	// return true if we won at least one attack
	protected boolean attackSplitUp(Country root, float attackRatio)
	{
		/**** STAGE 4 ATTACK ****/
		// Now the third stage. If it leeds to a good chance of more land, we
		// split our borders into two or more armie groups.
		List<Country> borders = getClusterBorders(root.getCode());
		boolean wonAttack = false;

		for(Country border : borders)
		{
			CountryIterator neighbors = new NeighborIterator(border);
			int enemies = 0;
			int enemiesArmies = 0;
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() != ID)
				{
					enemies++;
					enemiesArmies += neighbor.getArmies();
				}
			}

			// We only perform this operation when we far outnumber them.
			if(border.getArmies() > enemiesArmies * attackRatio)
			{
				int armiesPer = border.getArmies() / Math.max(enemies, 1); // use
																			// the
																			// max
																			// function
																			// to
																			// avoid
																			// divide
																			// by
																			// zero
				// then we will attack from this border to all of its enemy
				// neighbors.
				neighbors = new NeighborIterator(border);
				while(neighbors.hasNext() && border.getArmies() > 1)
				{
					Country neighbor = neighbors.next();
					if(neighbor.getOwner() != ID)
					{ // then we kill this enemy with 1/<enemies>
						moveInMemory = armiesPer;
						if(makeAttack(border.getCode(), neighbor.getCode(),
								true) > 0)
							wonAttack = true;
						moveInMemory = -1;
						// xxagentxx: if we lose lots of armies in the first
						// attacks, the last attacks might not happen because we
						// are out of armies. This is a bug, but not very
						// serious.
						wonAttack = true;
					}
				}
			}
		}
		return wonAttack;
	}

	// do a combination of the three almost always helpful attacks
	// return true if we won at least one attack
	protected boolean tripleAttackPack(Country root)
	{
		boolean won = false;
		while(attackEasyExpand(root))
		{
			won = true;
		}
		attackFillOut(root);
		while(attackConsolidate(root))
		{
			won = true;
		}
		return won;
	}

	// This method calculates the total number of armies owned by each player.
	// If we outnumber all the other players combined then go HOGWILD!
	protected boolean hogWildCheck()
	{
		// calculate some stats about player armies:
		int numPlayers = numberOfPlayers;
		int[] armies = new int[numPlayers];
		int enemyArmies = 0;
		for(int i = 0; i < numPlayers; i++)
		{
			armies[i] = BoardHelper.getPlayerArmies(i, countries);
			if(i != ID)
			{
				enemyArmies += armies[i];
			}
		}
		// done getting stats

		return (armies[ID] > enemyArmies);
	}

	// sets off as much attacking as possible if hogWild conditions are met
	protected void attackHogWild()
	{
		if(hogWildCheck())
		{
			attackAsMuchAsPossible();
		}
	}

	// If we have tons of armies then the game has probably hit a stalemate.
	// Shake things up
	protected void attackStalemate()
	{
		if(BoardHelper.getPlayerArmies(ID, countries) > 1500)
			attackAsMuchAsPossible();
	}

	protected void attackAsMuchAsPossible()
	{
		boolean attacked = true;
		CountryIterator e;
		while(attacked)
		{
			attacked = false;
			e = new PlayerIterator(ID, countries);
			while(e.hasNext())
			{
				Country c = e.next();
				while(tripleAttackPack(c))
				{
					attacked = true;
				}
				while(attackSplitUp(c, 0.01f))
				{
					attacked = true;
				}
			}
		}
	}

	// This method first checks to see if we can still aquire a card by
	// conquering a country.
	// If so, it compares all the possible attack match-ups we have and executes
	// the best one
	// if (ourArmies > theirArmies*ratio). i.e. ratio of 1 is us > them. ratio
	// of 2 is us double them
	protected void attackForCard()
	{
		attackForCard(1);
	}

	protected void attackForCard(int outnumberTimes)
	{
		if(!useCards || hasInvadedACountry())
		{
			return;
		}

		// find the best matchup of all our enemies
		float bestRatio = 0;
		Country bestUs = null;
		Country bestThem = null;
		CountryIterator ours = new PlayerIterator(ID, countries);
		while(ours.hasNext())
		{
			Country us = ours.next();
			CountryIterator thems = new NeighborIterator(us);
			while(thems.hasNext())
			{
				Country them = thems.next();
				if(them.getOwner() != ID
						&& (us.getArmies() * 1.0 / them.getArmies()) > bestRatio
						&& us.canGoto(them))
				{
					bestRatio = (us.getArmies() * 1.0f / them.getArmies());
					bestUs = us;
					bestThem = them;
				}
			}
		}
		// if we have a good matchup take it:
		if(bestUs != null
				&& bestUs.getArmies() > bestThem.getArmies() * outnumberTimes)
		{
			debug("executing an attackForCard attack");
			setmoveInMemoryBeforeCardAttack(bestUs); // boscoe and Yakool
														// implement this
														// differently
			makeAttack(bestUs.getCode(), bestThem.getCode(), true);
			moveInMemory = -1;
		}
	} // end of attackForCard()

	// to have rational behaviour when using attackForCard subclasses must imp
	// this
	protected void setmoveInMemoryBeforeCardAttack(Country attacker)
	{
	}

	/**
	 * If either country only borders 1 country then stay away from that
	 * country. Return -1 if both countries have more then 1 neighbor, otherwise
	 * return the number that should be returned from moveArmiesIn().
	 */
	protected int obviousMoveArmiesInTest(int cca, int ccd)
	{
		if(countries[cca].getNumberNeighbors() == 1)
		{
			moveInMemory = -1; // just in case it was something
			return 1000000;
		}
		if(countries[ccd].getNumberNeighbors() == 1)
		{
			moveInMemory = -1; // just in case it was something
			return 0;
		}
		return -1;
	}

	/**
	 * Check to see if moveInMemory has been set and if so obey it. Return -1 if
	 * not set, otherwise return the number that should be returned from
	 * moveArmiesIn().
	 */
	protected int memoryMoveArmiesInTest(int cca, int ccd)
	{
		// Now see if the agent has set the memory
		if(moveInMemory != -1)
		{
			int temp;
			if(moveInMemory == -2) // then move in half
				temp = countries[cca].getArmies() / 2;
			else
				temp = moveInMemory;
			moveInMemory = -1;
			return temp;
		}

		return -1;
	}

	// fortify armies outwards towards the borders of the cluster
	protected void fortifyCluster(Country root)
	{
		// We work from our borders back, fortifying closer.
		// Start out by getting a List of the cluster's borders:
		List<Country> borders = getClusterBorders(root.getCode());

		// So now the cluster borders are in <cluster>. fill it up while
		// fortifying towards the borders.
		for(int i = 0; i < borders.size(); i++)
		{
			Country border = borders.get(i);
			CountryIterator neighbors = new NeighborIterator(border);
			while(neighbors.hasNext())
			{
				Country neighbor = neighbors.next();
				if(neighbor.getOwner() == ID && !borders.contains(neighbor))
				{
					// Then <neighbour> is part of the cluster. fortify any
					// armies back and add to the List
					if(neighbor.canGoto(border))
					{ // this if statement should only return false when their
						// are single-way borders on the map
						makeFortification(neighbor.getMoveableArmies(),
								neighbor.getCode(), border.getCode());
					}
					borders.add(neighbor);
				}
			}
		}
	}

	/**
	 * Returns the contcode of the continent that we own with the most countries
	 * in it.
	 */
	protected int getBiggestOwnedCont()
	{
		int bestCont = -1;
		int bestContSize = -1;
		for(int i = 0; i < numberOfContinents; i++)
			if(BoardHelper.playerOwnsContinent(ID, i, countries)
					&& BoardHelper.getContinentSize(i, countries) > bestContSize)
			{
				bestCont = i;
				bestContSize = BoardHelper.getContinentSize(i, countries);
			}
		return bestCont;
	}

	/**
	 * Returns the contcode of the continent that we own with the largest
	 * positive bonus.
	 */
	protected int getMostValuablePositiveOwnedCont()
	{
		int bestCont = -1;
		int bestContBonus = -1;
		for(int i = 0; i < numberOfContinents; i++)
			if(BoardHelper.playerOwnsContinent(ID, i, countries)
					&& getContinentBonus(i) > bestContBonus)
			{
				bestCont = i;
				bestContBonus = getContinentBonus(i);
			}
		return bestCont;
	}

	// This method isn't used for anything, but it is part of the interface.
	public String message(String message, Object data)
	{
		return null;
	}

	// These methods used to be part of Yakool, but they were moved here so
	// EvilPixie could take advantage of them

	protected int mustKillPlayer;
	protected boolean[] mustKillPlayerOwnsCont;

	protected boolean placeArmiesToKillDominantPlayer(int numberOfArmies)
	{
		// calculate some stats about player incomes and armies:
		int numPlayers = numberOfPlayers;
		int[] incomes = new int[numPlayers];
		int[] armies = new int[numPlayers];
		int[] ownedCountries = new int[numPlayers];
		int totalArmies = 0, totalIncome = 0;
		for(int i = 0; i < numPlayers; i++)
		{
			armies[i] = BoardHelper.getPlayerArmies(i, countries);
			incomes[i] = getPlayerIncome(i);
			ownedCountries[i] = BoardHelper.getPlayerCountries(i, countries);
			totalArmies += armies[i];
			totalIncome += incomes[i];
		}
		// done getting stats

		// if an enemy player has half of all armies or half of all income
		// then he MUST be stopped.
		int playerToAttack = -1;
		for(int i = 0; i < numPlayers; i++)
		{
			if(i != ID
					&& (armies[i] >= totalArmies * 0.5
							|| incomes[i] >= totalIncome * 0.5 || ownedCountries[i] >= numberOfCountries * 0.5))
				playerToAttack = i;
		}

		if(playerToAttack != -1)
		{
			mustKillPlayer = playerToAttack;
			if(placeArmiesToKillPlayer(numberOfArmies, playerToAttack))
				return true; // cuz we successfully placed all the armies
		}
		return false;
	}

	protected boolean placeArmiesToKillPlayer(int numberOfArmies,
			int playerToAttack)
	{
		// try and take out his biggest continents:
		mustKillPlayerOwnsCont = new boolean[numberOfContinents];
		for(int i = 0; i < numberOfContinents; i++)
		{
			mustKillPlayerOwnsCont[i] = false;
			if(BoardHelper.playerOwnsContinent(playerToAttack, i, countries))
				mustKillPlayerOwnsCont[i] = true;
		}

		// now what?
		// place armies to take out the easiest continent

		// find the cost of taking out each one he owns
		int[] cost = new int[numberOfContinents];
		int smallestCost = 1000000;
		int placer = -1;
		for(int i = 0; i < numberOfContinents; i++)
		{
			cost[i] = -1;
			if(mustKillPlayerOwnsCont[i])
			{
				int[] path = BoardHelper.cheapestRouteFromOwnerToCont(ID, i,
						countries);
				if(path != null)
				{
					cost[i] = pathCost(path);

					if(cost[i] < smallestCost)
					{
						smallestCost = cost[i];
						placer = path[0];
					}
				}
			}
		}

		if(placer == -1)
			return false;

		makePlacement(numberOfArmies, placer);
		return true;
	}

	protected void attackToKillPlayer(int player)
	{
		debug("starting attackToKillPlayer -> " + player);
		// get the bonus values of all the conts they own:
		int[] ownContValue = new int[numberOfContinents];
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(mustKillPlayerOwnsCont[i])
				ownContValue[i] = getContinentBonus(i);
			else
				ownContValue[i] = -1;
		}

		// now try to kill the continents, starting with the biggest
		int biggestCont = 0;
		int biggestContValue = 0;
		while(biggestCont != -1)
		{
			biggestCont = -1;
			biggestContValue = 0;

			for(int i = 0; i < numberOfContinents; i++)
			{
				if(ownContValue[i] > biggestContValue)
				{
					biggestContValue = ownContValue[i];
					biggestCont = i;
				}
			}

			if(biggestCont != -1)
			{
				// kill it if possible
				attackToKillContinent(biggestCont);
				ownContValue[biggestCont] = -1; // so we don't try and attack it
												// again
			}
		}
		debug("ending attackToKillPlayer -> " + player);
	}

	protected boolean attackToKillContinent(int cont)
	{
		debug("starting attackToKillContinent -> " + cont);
		CountryIterator armies = new ArmiesIterator(ID, 2, countries);
		while(armies.hasNext())
		{
			Country us = armies.next();
			int[] path = BoardHelper.easyCostFromCountryToContinent(
					us.getCode(), cont, countries);

			if(path != null && pathCost(path) < us.getArmies())
			{
				if(attackAlongPath(path))
				{
					// we succeeded in killing the continent. our job is done.
					return true;
				}
			}
		}
		return false;
	}

	/** Calculate the sum of all enemy armies in <path> */
	protected int pathCost(int[] path)
	{
		if(path == null)
		{
			System.out
					.println("SmartAgentBase.pathCost() -> the path was null!");
			return 1000000; // return a high number so Yakool doesn't try to
							// follow it
		}
		int cost = 0;
		for(int p = 0; p < path.length; p++)
		{
			if(countries[path[p]].getOwner() != ID)
				cost += countries[path[p]].getArmies();
		}
		return cost;
	}

	// returns true if we successfully took over the whole path.
	// otherwise false
	protected boolean attackAlongPath(int[] path)
	{
		for(int i = 1; i < path.length; i++)
		{
			moveInMemory = 1000000;
			if(countries[path[i - 1]].getArmies() == 1
					|| makeAttack(path[i - 1], path[i], true) == 13)
			{
				// then we lost
				moveInMemory = -1;
				return false;
			}
		}
		moveInMemory = -1;
		return true;
	}

	public void debug(Object text)
	{
		// System.out.println(board.getPlayerName(ID)+" says: "+text);
		// System.out.flush();
	}

} // End of SmartAgentBase class
