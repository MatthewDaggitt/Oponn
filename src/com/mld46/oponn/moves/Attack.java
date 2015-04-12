package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class Attack extends Move
{
	private final String attackerName;
	public final int attackingCC;
	public final int attackingArmies;
	private final String defenderName;
	public final int defendingCC;
	public final int defendingArmies;
	public final boolean attackUntilDead;
	
	public Attack(int attackerCC, int defenderCC, int attackerArmies, int defenderArmies, boolean attackUntilDead)
	{
		super(Phase.ATTACK);
		
		this.attackingCC = attackerCC;
		this.defendingCC = defenderCC;
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
			return a.attackingCC == attackingCC &&
					a.defendingCC == defendingCC &&
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
