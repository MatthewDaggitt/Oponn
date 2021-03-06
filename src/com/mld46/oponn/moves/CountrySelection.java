package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class CountrySelection extends Move
{
	public final int cc;
	private final String name;
	
	public CountrySelection(int cc)
	{
		super(Phase.COUNTRY_SELECTION);
		this.cc = cc;
		this.name = countries[cc].getName();
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof CountrySelection)
		{
			CountrySelection sc = (CountrySelection)o; 
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
