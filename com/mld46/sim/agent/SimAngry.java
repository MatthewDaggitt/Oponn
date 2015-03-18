package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.Random;

//
//  Angry.java
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
 * Angry is a simplistic agent that attacks lots.
 */

public class SimAngry extends SimAgent
{
	// It is useful to have a random number generator for a couple of things
	protected Random rand;

	public SimAngry()
	{
		rand = new Random();
	}

	public String name()
	{
		return "Angry";
	}

	public float version()
	{
		return 1.0f;
	}

	public String description()
	{
		return "Angry is an AI that likes to attack.";
	}

	// Because Angry doesn't ever consern himself with continents,
	// we will try to get some continents at the beginning,
	// by choosing countries in the smallest continents.
	public int pickCountry()
	{
		int goalCont = mapHelper.getSmallestPositiveEmptyContinent();

		if(goalCont == -1) // oops, there are no unowned conts
			goalCont = mapHelper.getSmallestPositiveOpenContinent();

		// So now pick a country in the desired continent
		return pickCountryInContinent(goalCont);
	}

	// Picks a country in <cont>, starting with countries that have neighbors
	// that we own.
	// If there are none of those then pick the country with the fewest
	// neighbors total.
	protected int pickCountryInContinent(int cont)
	{
		// Cycle through the continent looking for unowned countries that have
		// neighbors we own
		CountryIterator continent = new ContinentIterator(cont, countries);
		while(continent.hasNext())
		{
			Country c = continent.next();
			if(c.getOwner() == -1 && c.getNumberPlayerNeighbors(ID) > 0)
			{
				// We found one, so pick it
				return c.getCode();
			}
		}

		// we neighbor none of them, so pick the open country with the fewest
		// neighbors
		continent = new ContinentIterator(cont, countries);
		int bestCode = -1;
		int fewestNeib = 1000000;
		while(continent.hasNext())
		{
			Country c = continent.next();
			if(c.getOwner() == -1 && c.getNumberNeighbors() < fewestNeib)
			{
				bestCode = c.getCode();
				fewestNeib = c.getNumberNeighbors();
			}
		}

		if(bestCode == -1)
		{
			// We should never get here, so print an alert if we do
			System.out
					.println("ERROR in Angry.pickCountryInContinent() -> there are no open countries");
		}

		return bestCode;
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
		// xxagentxx Angry cards phase
	}

	// Angry's thought process when placing his armies is simple.
	// He puts all of his armies where they can attack the most countries.
	// Thus we will cycle through all the countries that we own remembering
	// the one with the most enemy countries beside it.

	// Cycle through all our countries placing <num> armies on each country that
	// has at least <numberOfArmies> armies
	public void placeArmies(int numberOfArmies)
	{
		int mostEnemies = -1;
		Country placeOn = null;
		int subTotalEnemies = 0;
		
		// Use a PlayerIterator to cycle through all the countries that we own.
		CountryIterator own = new PlayerIterator(ID, countries);
		while(own.hasNext())
		{
			Country us = own.next();
			subTotalEnemies = us.getNumberEnemyNeighbors();

			// If it's the best so far store it
			if(subTotalEnemies > mostEnemies)
			{
				mostEnemies = subTotalEnemies;
				placeOn = us;
			}
		}

		// So now placeOn is the country that we own with the most enemies.
		// Tell the board to place all of our armies there
		makePlacement(numberOfArmies, placeOn.getCode());
	}

	// During the attack phase, Angry has a clear goal:
	// Take over as much land as possible.
	// Therefore he performs every available attack that he thinks he can win,
	// starting with the best matchups
	public void attackPhase()
	{
		// Keep cycling until we make no attacks
		boolean madeAttack = true;
		while(madeAttack)
		{
			madeAttack = false; // reset it. if we win an attack somewhere we
								// set it to true.

			// cycle through all of the countries that we have 2 or more armies
			// on
			CountryIterator armies = new ArmiesIterator(ID, 2, countries);
			while(armies.hasNext())
			{
				Country us = armies.next();

				// Find its weakest neighbor
				Country weakestNeighbor = us.getWeakestEnemyNeighbor();

				// So we have found the best matchup for Country <us>. (if there
				// are any enemies)

				// Even though this agent is a little angry, he is still
				// consious of the odds.
				// He will only attack if it is a good-chance of winning.
				if(weakestNeighbor != null
						&& us.getArmies() > weakestNeighbor.getArmies())
				{
					// Angry is a proud dude, and doesn't like to retreat.
					// So he will always attack till death.
					makeAttack(us.getCode(), weakestNeighbor.getCode(), true);

					// Set madeAttack to true, so that we loop through all our
					// armies again
					madeAttack = true;
				}
			}
		}
	} // End of attackPhase

	// When deciding how many armies to move in after a successful attack,
	// Angry will just put them all into the country with more enemies
	public int moveArmiesIn(int cca, int ccd)
	{
		int Aenemies = countries[cca].getNumberEnemyNeighbors();
		int Denemies = countries[ccd].getNumberEnemyNeighbors();

		// If the attacking country had more enemies, then we leave all possible
		// armies in the country they attacked from (thus we move in 0):
		if(Aenemies > Denemies)
			return 0;

		// Otherwise the defending country has more neighboring enemies, move in
		// everyone:
		return countries[cca].getArmies() - 1;
	}

	public void fortifyPhase()
	{
		// Cycle through all the countries and find countries that we could move
		// from:
		for(int i = 0; i < numberOfCountries; i++)
		{
			if(countries[i].getOwner() == ID
					&& countries[i].getMoveableArmies() > 0)
			{
				// This means we've found a country of ours that we can move
				// from if we want to.

				// We determine the best country by counting the enemy neighbors
				// it has.
				// The most enemy neighbors is where we move. Also, if there are
				// 0 enemy
				// neighbors where the armies are on now, we move to a random
				// neighbor (in
				// the hopes we'll find an enemy eventually).

				// To cycle through the neighbors we could use a
				// NeighborIterator,
				// but we can also directly use the country's AdjoingingList
				// array.
				// Let's use the array...
				Country[] neighbors = countries[i].getAdjoiningList();
				int countryCodeBestProspect = -1;
				int bestEnemyNeighbors = 0;
				int enemyNeighbors = 0;

				for(int j = 0; j < neighbors.length; j++)
				{
					if(neighbors[j].getOwner() == ID)
					{
						enemyNeighbors = neighbors[j].getNumberEnemyNeighbors();

						if(enemyNeighbors > bestEnemyNeighbors)
						{
							// Then so far this is the best country to move to:
							countryCodeBestProspect = neighbors[j].getCode();
							bestEnemyNeighbors = enemyNeighbors;
						}
					}
				}
				// Now let's calculate the number of enemies of the country
				// where the armies
				// already are, to see if they should stay here:
				enemyNeighbors = countries[i].getNumberEnemyNeighbors();

				// If there's a better country to move to, move:
				if(bestEnemyNeighbors > enemyNeighbors)
				{
					// Then the armies should move:
					// So now the country that had the best ratio should be
					// moved to:

					makeFortification(countries[i].getMoveableArmies(), i, countryCodeBestProspect);
				}
				// If there are no good places to move to, move to a random
				// place:
				else if(enemyNeighbors == 0)
				{
					// We choose an int from [0, neighbors.length]:
					int randCC = rand.nextInt(neighbors.length);
					makeFortification(countries[i].getMoveableArmies(), i,
							neighbors[randCC].getCode());
				}
			}
		}
	} // End of fortifyPhase() method

	// Oh boy. If this method ever gets called it is because we have won the
	// game.
	// Send back something witty to tell the user.
	public String youWon()
	{
		// For variety we store a bunch of answers and pick one at random to
		// return.
		String[] answers = new String[] {
				"Muh-Ha-Ha-Ha\nAngry now very happy!",
				"Your skull is squishy and mellon-like", "ME STILL ANGRY!!!" };

		return answers[rand.nextInt(answers.length)];
	}

	/**
	 * We get notified through this methos when certain things happen. Angry
	 * parses out all the different messages and does nothing with them.
	 */
	public String message(String message, Object data)
	{
		return null;
	}

} // End of Angry class
