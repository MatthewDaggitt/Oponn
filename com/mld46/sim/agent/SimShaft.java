package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Shaft.java
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
 Shaft uses a sweep-attack look-ahead to collapse into as few borders as possible.

 Shaft picks countries in continents that have the fewest border points  

 Shaft also has a slow-acting attackFromCluster() that includes a primitive 
 sweepBordersForward method.
 */

import java.util.List;
import java.util.ArrayList;

public class SimShaft extends SimCluster
{
	public String name()
	{
		return "Shaft";
	}

	public String description()
	{
		return "Shaft is a bad mutha.";
	}

	// We are going to try and take over lots of continents.
	// It's much easier if they are easy to defend continents
	// pick the ones with least # of border points
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

	protected void attackFromCluster(Country root)
	{
		// now run some attack methods for the cluster centered around root:
		if(root != null)
		{
			// do one simple attack
			if(!attackEasyExpand(root))
			{
				attackFillOut(root);
			}

			// and consolidate our borders as much as possible
			while(attackConsolidate(root))
			{
			}

			if(!hasInvadedACountry())
			{
				// try and sweep forward our borders
				CountryIterator borders = new ClusterBorderIterator(root);
				while(borders.hasNext())
				{
					sweepForwardBorder(borders.next());
				}
			}
		}
	}

	// tries to take over a couple of countries while keeping to one border
	// country
	// also only attacks with winnable odds
	protected void sweepForwardBorder(Country sweep)
	{
		// FIRST --> test if the sweep is worthwhile

		// say that we have seen this border's enemies
		CountryIterator neib = new NeighborIterator(sweep);
		List<Country> q = new ArrayList<Country>();
		List<Country> seen = new ArrayList<Country>();
		while(neib.hasNext())
		{
			Country n = neib.next();
			if(n.getOwner() != ID)
			{
				seen.add(n);
			}
		}

		// run a simulation advance...
		startSweep(sweep, q, seen, false);
		while(advanceSweep(q, seen, false))
		{
		}

		if(q.size() == 1)
		{
			// then we should totally follow this sweep plan
			// reset the q's
			neib = new NeighborIterator(sweep);
			q = new ArrayList<Country>();
			seen = new ArrayList<Country>();
			while(neib.hasNext())
			{
				Country n = neib.next();
				if(n.getOwner() != ID)
				{
					seen.add(n);
				}
			}

			// and do it for real
			startSweep(sweep, q, seen, true);
			while(advanceSweep(q, seen, true))
			{
			}
		}
	}

	protected int countUnseenEnemies(Country c, List<Country> seen)
	{
		int enemies = 0;
		// Country e = null;
		CountryIterator neib = new NeighborIterator(c);
		while(neib.hasNext())
		{
			Country n = neib.next();
			if(n.getOwner() != ID && !seen.contains(n))
			{
				enemies++;
				// e = n;
			}
		}
		return enemies;
	}

	// xxagentxx what happens if we lose the forReal attack? we still enque...
	protected void startSweep(Country from, List<Country> q, List<Country> seen, boolean forReal)
	{
		if(forReal)
		{
			q.add(seen.get(0));
			debug("startSweep");
			if(from.getArmies() > 1)
				makeAttack(from.getCode(), seen.get(0).getCode(), true);
		} else
		{
			for(int i = 0; i < seen.size(); i++)
			{
				q.add(seen.get(i));
			}
		}
	}																	

	// q is the collection of countries that we don't own but are examining,
	// to see if they can be compressed into a smaller border
	// if shouldAttack is true then the sweep will actually be done.
	// otherwise the q's will be updated but no attack will occur
	protected boolean advanceSweep(List<Country> q, List<Country> seen, boolean forReal)
	{
		// to make sure that we take over the borders as soon as possible, we
		// check here.
		// any 'seen' countries that we don't own will be crushed!!!
		if(forReal)
			takeOverEnveloped(seen);

		boolean swept = false;
		for(int i = 0; i < q.size(); i++)
		{
			// count the enemies we have not seen yet
			int enemies = 0;
			Country e = null;
			CountryIterator neib = new NeighborIterator(q.get(i));
			while(neib.hasNext())
			{
				Country n = neib.next();
				if(n.getOwner() != ID && !seen.contains(n))
				{
					enemies++;
					e = n;
				}
			}
			// if there is only one enemy then advance the q to it
			if(enemies == 0)
				q.remove(q.get(i));
			else if(enemies == 1)
			{
				if(forReal)
				{
					debug("advanceSweep");
					Country from = q.get(i);
					if(from.getOwner() == ID && from.getArmies() > 1)
						makeAttack(from.getCode(), e.getCode(), true);
				}
				q.remove(q.get(i));
				q.add(e);
				seen.add(e);
				swept = true;
			}
		}
		return swept;
	}

	protected void takeOverEnveloped(List<Country> seen)
	{
		for(int i = 0; i < seen.size(); i++)
		{
			if((seen.get(i)).getOwner() != ID)
				takeCountry(seen.get(i));
		}
	}

	protected void takeCountry(Country into)
	{
		// try and find a neighbor that we own, and attack this country
		CountryIterator neighbors = new NeighborIterator(into);
		while(neighbors.hasNext() && into.getOwner() != ID)
		{
			Country possAttack = neighbors.next();
			if(possAttack.getOwner() == ID
					&& possAttack.getArmies() > into.getArmies()
					&& possAttack.canGoto(into))
			{

				debug("take country");
				makeAttack(possAttack.getCode(), into.getCode(), true);
			}
		}
	}

	public String youWon()
	{
		String[] answers = new String[] { "Can you dig it?", "Damn right",
				"You sure got a lotta mouth on you", "c'mere, baby!",
				"Why don't you stop playin' with yourself",
				"You a cagey spook, Bumpy",
				"You know me. \nIt's my duty to please that booty",
				"You're too hot, man. \nYou gotta step off a bit",
				"Chicks dig me because I rarely wear underwear",
				"If brains were dynamite \nyou couldn't blow your nose" };

		return answers[rand.nextInt(answers.length)];
	}
} // End of Cluster class
