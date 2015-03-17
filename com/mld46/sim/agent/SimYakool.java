package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Yakool.java
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
//  Yakool builds upon Cluster by adding the following...
//		if an enemy looks too strong all our efforts are put into killing him
//		slows down attackFromCluster()
//		tries to get a card every turn
//
//	Agents subclassing Yakool should consider overriding the method setMoveInMemoryBeforeCardAttack(),
//	which is called right before the attack for a card is made
//
// Or override attackFromCluster() to get a cluster with mustKill and card attacks.
//

public class SimYakool extends SimCluster
{
	public SimYakool()
	{
		mustKillPlayer = -1;
	}

	public String name()
	{
		return "Yakool";
	}

	public String description()
	{
		return "Yakool is wary of those who get to strong.";
	}

	public void placeArmies(int numberOfArmies)
	{
		if(placeArmiesToKillDominantPlayer(numberOfArmies))
			return;

		if(mapHelper.playerOwnsAnyPositiveContinent(ID))
		{
			int ownCont = getMostValuablePositiveOwnedCont();
			placeArmiesOnClusterBorder(numberOfArmies,
					countries[BoardHelper.getCountryInContinent(ownCont,
							countries)]);
		} else
		{
			int wantCont = getEasiestContToTake();
			placeArmiesToTakeCont(numberOfArmies, wantCont);
		}
	}

	public void attackPhase()
	{
		if(mustKillPlayer != -1)
		{
			// do our best to take out this guy
			attackToKillPlayer(mustKillPlayer);
		}

		// but do other attacks also...
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

		attackForCard();
		attackHogWild();
		attackStalemate();
	}// End of attackPhase

	protected void attackFromCluster(Country root)
	{
		// now run some attack methods for the cluster centered around root:
		if(root == null)
		{
			System.out
					.println("ERROR in Yakool.attackFromCluster(). root==null 654978789654");
			return;
		}

		// expand as long as possible the easyist ways
		while(tripleAttackPack(root))
		{
		}

		// calculate some stats about player incomes and armies:
		int numPlayers = numberOfPlayers;
		int[] incomes = new int[numPlayers];
		int[] armies = new int[numPlayers];
		int maxEnemyIncome = 0, aveEnemyIncome = 0, enemyArmies = 0;
		for(int i = 0; i < numPlayers; i++)
		{
			incomes[i] = getPlayerIncome(i);
			armies[i] = BoardHelper.getPlayerArmies(i, countries);
			if(i != ID)
			{
				enemyArmies += armies[i];
				aveEnemyIncome += incomes[i];
				if(incomes[i] > maxEnemyIncome)
					maxEnemyIncome = incomes[i];
			}
		}
		aveEnemyIncome = aveEnemyIncome / numPlayers;
		// done getting stats

		// there are 4 conditions upon which we expand:
		// 1. if we own zero continents (hopefully we will take over a cont
		// then)
		// 2. our income is below average
		// 3. our income is the highest (hopefully we will take over the world
		// then)
		// 4. we outnumber all the other player combined (hopefully we will take
		// over the world then)
		if(!mapHelper.playerOwnsAnyPositiveContinent(ID)
				|| incomes[ID] < aveEnemyIncome || incomes[ID] > maxEnemyIncome
				|| armies[ID] > enemyArmies)
		{
			// then we want to attack more
			debug("Yakool has decided to do the worse attacks");
			while(attackSplitUp(root, 1.2f))
			{
			}
		}
	} // end of Yakool.attackFromCluster

	protected void setmoveInMemoryBeforeCardAttack(Country attacker)
	{
	}

	public String youWon()
	{
		String[] answers = new String[] {
				"All I want is for the village and forest \nto live together in peace",
				"Gone fishing",
				"Red Elk Rule",
				"Good Morrow",
				"Ah...\nTime to relax and smell the flowers",
				"Loyalty to a good master is paramount",
				"Go away",
				"Here pixie, pixie, pixie...",
				"I fight for peace, \nbut war is all I ever see!",
				"When the power of love overcomes the love of power \nthe world will know peace",
				"I must see with eyes unclouded by hate",
				"Look, everyone! \nThis is what hatred looks like!",
				"It's eating me alive, \nand very soon now it will kill me! \nFear and anger only make it grow faster!" };

		return answers[rand.nextInt(answers.length)];
	}

} // End of Yakool class
