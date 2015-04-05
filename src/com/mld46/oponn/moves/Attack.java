package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class Attack extends Move
{
	private final String attackerName;
	public final int attackingCC;
	public final short attackingArmies;
	private final String defenderName;
	public final int defendingCC;
	public final short defendingArmies;
	public final boolean attackUntilDead;
	
	public Attack(int attackerCC, int defenderCC, int attackerArmies, int defenderArmies, boolean attackUntilDead)
	{
		super(Phase.ATTACK);
		
		if(attackerArmies > Short.MAX_VALUE || defenderArmies > Short.MAX_VALUE)
		{
			throw new IllegalArgumentException("More than " + Short.MAX_VALUE + " armies on a country - Oponn not equipped to handle this situation!");
		}
		
		this.attackingCC = attackerCC;
		this.defendingCC = defenderCC;
		this.attackingArmies = (short)attackerArmies;
		this.defendingArmies = (short)defenderArmies;
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
