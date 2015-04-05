package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class InitialPlacement extends Move implements Comparable<InitialPlacement>
{
	public final int cc;
	public final int armies;
	
	public InitialPlacement(int cc, int armies)
	{
		super(Phase.INITIAL_PLACEMENT);
		
		this.cc = cc;
		this.armies = armies;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof InitialPlacement)
		{
			InitialPlacement p = (InitialPlacement)o;
			return p.cc == cc && p.armies == armies;
		}
		return false;
	}

	
	@Override
	public String toString()
	{
		return "InitialPlacement [" + countries[cc].getName() + ", " + armies + "]";
	}

	@Override
	public int compareTo(InitialPlacement other)
	{
		return cc < other.cc ? -1 : (cc == other.cc ? 0 : 1);
	}
}
