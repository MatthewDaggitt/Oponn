package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class Invasion extends Move
{
	public final int attackerCC;
	public final int defenderCC;
	public final int armies;
	
	public Invasion(int attackerCC, int defenderCC, int armies)
	{
		super(Phase.ATTACK);
		this.attackerCC = attackerCC;
		this.defenderCC = defenderCC;
		this.armies = armies;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Invasion)
		{
			Invasion i = (Invasion)o; 
			return i.armies == armies &&
					i.attackerCC == attackerCC &&
					i.defenderCC == defenderCC;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "Invasion [invader=" + attackerCC + ", invaded=" + defenderCC + ", numberOfArmies=" + armies + "]";
	}
}
