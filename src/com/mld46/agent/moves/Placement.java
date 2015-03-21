package com.mld46.agent.moves;

import com.mld46.sim.board.SimBoard.Phase;

public class Placement extends Move implements Comparable<Placement>
{
	public final int cc;
	public final int armies;
	
	public Placement(int cc, int armies, Phase phase)
	{
		super(phase);
		
		this.cc = cc;
		this.armies = armies;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Placement)
		{
			Placement p = (Placement)o;
			return p.cc == cc && p.armies == armies;
		}
		return false;
	}

	
	@Override
	public String toString()
	{
		return "Placement [" + countries[cc].getName() + ", " + armies + "]";
	}

	@Override
	public int compareTo(Placement other)
	{
		return cc < other.cc ? -1 : (cc == other.cc ? 0 : 1);
	}
}
