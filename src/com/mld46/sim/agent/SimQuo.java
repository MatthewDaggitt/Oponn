package com.mld46.sim.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Quo.java
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
 Quo is a Shaft who tries to obtain a card every turn.
 */

public class SimQuo extends SimShaft
{
	public String name()
	{
		return "Quo";
	}

	public String description()
	{
		return "Logical and deadly";
	}

	public void cardsPhase(Card[] cards)
	{
		super.cardsPhase(cards);
		cashCardsIfPossible(cards);
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

		attackForCard();
		attackHogWild();
		attackStalemate();
	}// End of attackPhase

	protected void setmoveInMemoryBeforeCardAttack(Country attacker)
	{
		moveInMemory = attacker.getArmies() / 2;
	}

	public String youWon()
	{
		String[] answers = new String[] {
				"You're not the boss of me",
				"It would be illogical to kill without reason",
				"Hmmm... Fascinating",
				"Logic is little tweeting bird chirping in meadow",
				"Random chance seems to have operated in my favor",
				"I'm frequently appalled by the \nlow regard you Earthmen have for life",
				"I object to intellect without discipline.\n I object to power without constructive purpose",
				"Madness has no purpose. Or reason. \nBut it may have a goal",
				"Superior ability breeds superior ambition",
				"Emotions are alien to me. \nI'm a scientist",
				"Death is welcome. \nThe murderers have won",
				"Pain is a thing of the mind. \nThe mind can be controlled",
				"In the strict scientific sense we all feed on death\n ... even vegetarians",
				"Military secrets are the most fleeting of all",
				"Violence in reality is quite different from theory",
				"You are intelligent, but inexperienced",
				"Your moves indicate two-dimensional thinking",
				"I'm not a pacifist",
				"Trust yourself, you know more than you think you do" };

		return answers[rand.nextInt(answers.length)];
	}
} // End of Cluster class
