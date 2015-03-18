package com.mld46.agent.moves;

import com.mld46.sim.board.SimBoard.Phase;

public class Attack extends Move
{
	private final String attackerName;
	public final int attackerCC;
	public final int attackingArmies;
	private final String defenderName;
	public final int defenderCC;
	public final int defendingArmies;
	public final boolean attackUntilDead;
	
	public Attack(int attackerCC, int defenderCC, int attackerArmies, int defenderArmies, boolean attackUntilDead)
	{
		super(Phase.ATTACK);
		
		this.attackerCC = attackerCC;
		this.defenderCC = defenderCC;
		this.attackingArmies = attackerArmies;
		this.defendingArmies = defenderArmies;
		this.attackUntilDead = attackUntilDead;
		
		this.attackerName = countries[attackerCC].getName();
		this.defenderName = countries[defenderCC].getName();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Attack)
		{
			Attack a = (Attack)o; 
			return a.attackerCC == attackerCC &&
					a.defenderCC == defenderCC &&
					a.attackingArmies == attackingArmies &&
					a.defendingArmies == defendingArmies &&
					a.attackUntilDead == attackUntilDead;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "Attack [" + (attackerName != null ? "aName=" + attackerName + ", " : "") + (defenderName != null ? "dName=" + defenderName + ", " : "") + "attackUntilDead=" + attackUntilDead + "]";
	}
}
