package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class AttackOutcome extends Move implements Comparable<AttackOutcome>
{
	public final float probability;
	public final short attackerLosses;
	public final short defenderLosses;
	public final boolean invading;
	
	public AttackOutcome(int attackerLosses, int defenderLosses, float probability, boolean invading)
	{
		super(Phase.ATTACK);
		this.probability = probability;
		this.attackerLosses = (short) attackerLosses;
		this.defenderLosses = (short) defenderLosses;
		this.invading = invading;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof AttackOutcome)
		{
			AttackOutcome a = (AttackOutcome)o; 
			return a.attackerLosses == attackerLosses &&
					a.defenderLosses == defenderLosses &&
					a.invading == invading;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "AttackOutcome [probability=" + probability + ", attackerLosses=" + attackerLosses + ", defenderLosses=" + defenderLosses + ", invading=" + invading + "]";
	}

	
	@Override
	public int compareTo(AttackOutcome ao)
	{
		return probability > ao.probability ? 1 : (probability == ao.probability ? 0 : -1);
	}
}
