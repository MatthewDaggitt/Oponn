package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;
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
