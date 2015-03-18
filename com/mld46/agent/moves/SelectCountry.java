package com.mld46.agent.moves;

import com.mld46.sim.board.SimBoard.Phase;

public class SelectCountry extends Move
{
	public final int cc;
	private final String name;
	
	public SelectCountry(int cc)
	{
		super(Phase.COUNTRY_SELECTION);
		this.cc = cc;
		this.name = countries[cc].getName();
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof SelectCountry)
		{
			SelectCountry sc = (SelectCountry)o; 
			return sc.cc == cc;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "SelectCountry [name=" + name + "]";
	}
}
