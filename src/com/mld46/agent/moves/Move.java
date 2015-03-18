package com.mld46.agent.moves;

import com.mld46.sim.board.SimBoard.Phase;
import com.sillysoft.lux.Country;

public abstract class Move
{
	public static Country [] countries;
	public final Phase phase;
	
	public Move(Phase phase)
	{
		this.phase = phase;
	}
	
	abstract public boolean equals(Object o);
	
	abstract public String toString();
}
