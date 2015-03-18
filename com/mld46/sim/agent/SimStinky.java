package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Stinky.java
//  Lux
//
//  Copyright (c) 2002-2008 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

/**
 A mostly random AI
 */

import java.util.Random;

public class SimStinky extends SimAgent
{
	// It is useful to have a random number generator for a couple of things
	protected Random rand;

	public SimStinky()
	{
		rand = new Random();
	}

	public String name()
	{
		return "Stinky";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "Stinky smells bad";
	}

	public int pickCountry()
	{
		return -1; // the world will give us a random unowned country
	}

	// Treat initial armies the same as normal armies
	public void placeInitialArmies(int numberOfArmies)
	{
		placeArmies(numberOfArmies);
	}

	// The game will automatically cash a random set of ours if we
	// return from this method and still have 5 or more cards.
	// For now just let that always happen
	public void cardsPhase(Card[] cards)
	{
	}

	public void placeArmies(int numberOfArmies)
	{
		// place on a random country that we own
		int test;
		do
		{
			test = rand.nextInt(countries.length);
		} while(countries[test].getOwner() != ID
				|| countries[test].getWeakestEnemyNeighbor() == null);

		makePlacement(numberOfArmies, test);
	}

	public void attackPhase()
	{
		attackPhase(true);

		// If we have tons of armies then attack more
		if(BoardHelper.getPlayerArmies(ID, countries) > 300)
			while(attackPhase(false))
			{
			}
	}

	public boolean attackPhase(boolean careAboutOdds)
	{
		boolean attacked = false;
		CountryIterator ours = new PlayerIterator(ID, countries);
		while(ours.hasNext())
		{
			Country us = ours.next();
			Country weak = us.getWeakestEnemyNeighbor();

			if(weak != null
					&& us.getArmies() > 1
					&& (us.getArmies() > weak.getArmies() * 1.5 || !careAboutOdds))
			{
				makeAttack(us.getCode(), weak.getCode(), true);
				attacked = true;
			}
		}
		return attacked;
	} // End of attackPhase

	public int moveArmiesIn(int cca, int ccd)
	{
		Country attackWeak = countries[cca].getWeakestEnemyNeighbor();
		Country defendWeak = countries[ccd].getWeakestEnemyNeighbor();

		if(attackWeak == null)
			return 1000000;
		if(defendWeak == null)
			return 0;

		if(attackWeak.getArmies() < defendWeak.getArmies())
			return 0;

		return 1000000;
	}

	public void fortifyPhase()
	{
	} // End of fortifyPhase() method

	// Oh boy. If this method ever gets called it is because we have won the
	// game.
	// Send back something witty to tell the user.
	public String youWon()
	{
		// For variety we store a bunch of answers and pick one at random to
		// return.
		String[] answers = new String[] { "poot", "Do you smell something?",
				"Deodorant is for losers" };

		return answers[rand.nextInt(answers.length)];
	}

	// This method isn't used for anything, but it is part of the interface.
	public String message(String message, Object data)
	{
		return null;
	}

} // End of Stinky class
