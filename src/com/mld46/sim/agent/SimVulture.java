package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Vulture.java
//  Lux
//
//  Created by Dustin Sacks on 8/26/04.
//  Copyright (c) 2002-2008 Sillysoft Games. All rights reserved.
//

import java.util.*;

/**
 * Includes logic to fully eliminate weak players from the game.
 */

public class SimVulture extends SimSmartAgentBase
{
	// We use a backing agent to send many command to.
	// This is needed because java has no multiple inheritance,
	// and we want to add Vulture behaviour to different agent types.
	protected SimSmartAgentBase backer;

	// Routes can be expensive to calculate. So store any that we are going to
	// follow.
	// During attack phase check for stored routes and execute them.
	protected List<CountryRoute> attackRoutes = new ArrayList<CountryRoute>();

	public SimVulture()
	{
		backer = new SimCluster();
	}

	public void setPrefs(int ID, Board board)
	{
		backer.setPrefs(ID, board);
		super.setPrefs(ID, board);
	}

	// A bunch of methods we just let our backer take care of
	public int pickCountry()
	{
		return backer.pickCountry();
	}

	public void placeInitialArmies(int numberOfArmies)
	{
		backer.placeInitialArmies(numberOfArmies);
	}

	public int moveArmiesIn(int countryCodeAttacker, int countryCodeDefender)
	{
		return backer.moveArmiesIn(countryCodeAttacker, countryCodeDefender);
	}

	public void fortifyPhase()
	{
		backer.fortifyPhase();
	}

	public String message(String message, Object data)
	{
		return backer.message(message, data);
	}

	public String name()
	{
		return "Vulture";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "Vulture likes to pick at carcases.";
	}

	public void cardsPhase(Card[] cards)
	{
		cashCardsIfPossible(cards);
		backer.cardsPhase(cards);
	}

	protected int toKillPlayer = -1;
	protected boolean placedToKill = false; // did we place this round with the
											// intent to kill ?

	protected void setToKillPlayer(int numberOfArmies)
	{
		// calculate some stats about players:
		int cardsWorth = getNextCardSetValue();
		int lowestArmyCount = 1000000;
		int lowestArmyPlayer = -1;
		int numPlayers = numberOfPlayers;
		int[] armies = new int[numPlayers];
		int ourArmies = 0;
		for(int i = 0; i < numPlayers; i++)
		{
			armies[i] = BoardHelper.getPlayerArmies(i, countries);
			if(i == ID)
				ourArmies = armies[i];
			else
			{
				// Discount their armies based on the card reward we'll get for
				// killing them
				armies[i] -= cardsWorth * (getPlayerCards(i) / 3.0);
				// If they are alive AND the lowest so far AND within range of
				// us killing them remember them
				if(BoardHelper.getPlayerCountries(i, countries) > 0
						&& armies[i] < lowestArmyCount
						&& BoardHelper.getPlayersBiggestArmyWithEnemyNeighbor(
								ID, countries).getArmies()
								+ numberOfArmies > BoardHelper.getPlayerArmies(
								i, countries)
								+ BoardHelper.getPlayerCountries(i, countries))
				{
					lowestArmyCount = armies[i];
					lowestArmyPlayer = i;
				}
			}
		}
		// done getting stats

		toKillPlayer = -1;

		if(ourArmies > lowestArmyCount * 2)
		{
			toKillPlayer = lowestArmyPlayer;
		}
	}

	public void placeArmies(int numberOfArmies)
	{
		debug("Placing " + numberOfArmies + " armies...");
		// Look for a weak player to eliminate
		placedToKill = false;
		setToKillPlayer(numberOfArmies);

		if(toKillPlayer == -1)
		{
			backer.placeArmies(numberOfArmies);
			return;
		}

		placeToKill(numberOfArmies);
	}

	protected void placeToKill(int numberOfArmies)
	{
		// We will place with the intent of eliminating this player
		// Divide the to-kill countries into clusters
		CountryClusterSet clusters = CountryClusterSet.getAllCountriesOwnedBy(
				toKillPlayer, countries);
		if(clusters.numberOfClusters() == 1)
		{
			debug("They only have 1 cluster!");
			// a simpler case
			CountryCluster cluster = clusters.getCluster(0);
			CountryRoute clusterRoute = null;

			// Don't bother looking for a route in more then 20 countries, since
			// it can hang the
			// app on large search-spaces
			if(cluster.size() < 21)
				clusterRoute = cluster.getSimpleRoute(true, ID);

			if(clusterRoute != null)
			{
				debug("It has a simple path! " + clusterRoute);
				// So we just need to get to one end of the path and then follow
				// it
				Country start = clusterRoute.start();
				Country end = clusterRoute.end();

				CountryRoute routeToStart = new CountryRoute(
						BoardHelper.easyCostCountryWithOwner(start, ID,
								countries));
				CountryRoute routeToEnd = new CountryRoute(
						BoardHelper
								.easyCostCountryWithOwner(end, ID, countries));
				CountryRoute routeToCluster, attackRoute;

				if(routeToStart.costNotCountingPlayer(ID, toKillPlayer)
						- routeToStart.start().getArmies() < routeToEnd
						.costNotCountingPlayer(ID, toKillPlayer)
						- routeToEnd.start().getArmies())
				{
					routeToCluster = routeToStart;
					attackRoute = routeToStart.append(clusterRoute);
				} else
				{
					routeToCluster = routeToEnd;
					attackRoute = routeToEnd.append(clusterRoute.reverse());
				}

				Country placeOnCountry = routeToCluster.start();
				debug("Our closest country is " + placeOnCountry);

				debug("We have "
						+ placeOnCountry.getArmies()
						+ " + "
						+ numberOfArmies
						+ " armies. They have "
						+ clusterRoute.getArmies()
						+ " armies + "
						+ (routeToCluster.costNotCountingPlayer(ID) - routeToCluster
								.end().getArmies()) + " needed to get there.");

				// Do we have enough armies to take out this guy this turn?
				if(placeOnCountry.getArmies() + numberOfArmies > clusterRoute
						.getArmies()
						+ routeToCluster.costNotCountingPlayer(ID)
						- routeToCluster.end().getArmies())
				{
					placedToKill = true;
					debug("Placing all armies on " + placeOnCountry);
					makePlacement(numberOfArmies, placeOnCountry.getCode());
					attackRoutes.add(attackRoute);
					return;
				}
			}
		} else
			debug("They have " + clusters.numberOfClusters() + " clusters.");

		backer.placeArmies(numberOfArmies);
	}

	public void attackPhase()
	{
		if(!placedToKill)
		{
			backer.attackPhase();
			return;
		}

		// Otherwise we have a player in mind to kill.
		// Operate in a loop in case we eliminate for some cards and have the
		// chance to take out another player afterwards
		while(attackRoutes.size() > 0)
		{
			CountryRoute attackRoute = (CountryRoute) attackRoutes.remove(0);

			debug("We are attacking to kill off the player along the route: "
					+ attackRoute);
			attackAlongRoute(attackRoute);
		}

		attackHogWild(); // this only does anything if we outnumber everyone
	}

	protected boolean attackAlongRoute(CountryRoute route)
	{
		for(int i = 1; i < route.size(); i++)
		{
			backer.moveInMemory = 1000000;
			if(route.get(i - 1).getArmies() > 1
					&& route.get(i - 1).canGoto(route.get(i))
					&& route.get(i - 1).getOwner() == ID)
			{ // attack...
				if(makeAttack(route.get(i - 1).getCode(), route.get(i).getCode(), true) == 13)
				{
					// then we lost
					backer.moveInMemory = -1;
					return false;
				}
			}
		}
		backer.moveInMemory = -1;
		return true;
	}

	public String youWon()
	{
		return "Picked off";
	}

	@Override
	public void resetClusterManager()
	{
		super.resetClusterManager();
		backer.resetClusterManager();
	}
}
