package com.mld46.agent;

import com.mld46.sim.board.SimBoard;

public class ProgressiveBias
{
	public static void addProgressiveBias(SearchNode node, SimBoard simBoard, int BIAS_VISITS)
	{
		throw new IllegalArgumentException("Progressive bias is currently not operational");
		/**
		Move parentMove = node.move;
		if  (parentMove instanceof Placement ||
			(parentMove instanceof NextPhase && ((NextPhase)parentMove).nextPhase == Phase.PLACEMENT))
		{
			int numberOfPlaceableArmies = simBoard.getNumberOfPlaceableArmies();
			List<Placement> suggestedPlacements = epc.placeArmies(numberOfPlaceableArmies);
			
			if(PLACEMENT_PARTIAL_ORDER_OPTIMISATION)
			{
				Collections.sort(suggestedPlacements);
				
				for(Placement placement : suggestedPlacements)
				{
					for(SearchNode child : node.children)
					{
						if(child.move.equals(placement))
						{
							child.setBias(1.0,BIAS_VISITS);
							return;
						}
					}
				}
			}
			else
			{
				int index;
				for(SearchNode child : node.children)
				{
					index = suggestedPlacements.indexOf(child.move);
					
					if(index != -1)
					{
						child.setBias(1.0,BIAS_VISITS);
						suggestedPlacements.remove(index);
					}
				}
			}
		}
		else if (parentMove instanceof Invasion || 
				(parentMove instanceof AttackOutcome && !((AttackOutcome)parentMove).invading) ||
				(parentMove instanceof NextPhase && ((NextPhase)parentMove).nextPhase == Phase.ATTACK))
		{
			Move suggestedMove = epc.attackPhase();
			
			for(SearchNode child : node.children)
			{
				if(child.move.equals(suggestedMove))
				{
					child.setBias(1.0,BIAS_VISITS);
				}
			}
		}
		else if (parentMove instanceof AttackOutcome && ((AttackOutcome)parentMove).invading)
		{
			Invasion invasion = epc.moveArmiesIn(simBoard.getAttackingCountry(),simBoard.getDefendingCountry());
			
			for(SearchNode child : node.children)
			{
				if(child.move.equals(invasion))
				{
					child.setBias(1.0,BIAS_VISITS);
					return;
				}
			}
			
			System.out.println("Hmm invasions should always be present!");
		}
		else if (parentMove instanceof Fortification ||
				(parentMove instanceof NextPhase && ((NextPhase)parentMove).nextPhase == Phase.FORTIFICATION))
		{
			Move suggestedMove = epc.fortifyPhase();
			
			for(SearchNode child : node.children)
			{
				if(child.move.equals(suggestedMove))
				{
					child.setBias(1.0,BIAS_VISITS);
				}
			}
		}
		**/
	}
}
