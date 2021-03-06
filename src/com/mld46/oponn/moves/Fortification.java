package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class Fortification extends Move
{
	public final int armies;
	public final int sourceCC;
	public final String sourceName;
	public final int destinationCC;
	public final String destinationName;
	
	public final boolean continentOptimisationFortification;
	
	public Fortification(int armies, int sourceCC, int destinationCC, boolean continentOptimisationFortification)
	{
		super(Phase.FORTIFICATION);
		
		this.armies = armies;
		this.sourceCC = sourceCC;
		this.destinationCC = destinationCC;
		
		this.sourceName = Move.countries[sourceCC].getName();
		this.destinationName = destinationCC != -1 ? Move.countries[destinationCC].getName() : "N/A";
		
		this.continentOptimisationFortification = continentOptimisationFortification;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Fortification)
		{
			Fortification f = (Fortification)o; 
			return f.armies == armies &&
					f.sourceCC == sourceCC &&
					f.destinationCC == destinationCC;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "Fortification [armies=" + armies + ", " + (sourceName != null ? "sourceName=" + sourceName + ", " : "") + (destinationName != null ? "destinationName=" + destinationName : "") + "]";
	}
}
