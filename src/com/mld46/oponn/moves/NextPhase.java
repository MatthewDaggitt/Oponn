package com.mld46.oponn.moves;

import com.mld46.oponn.sim.boards.SimulationBoard.Phase;

public class NextPhase extends Move
{
	public final Phase nextPhase;
	public final Phase previousPhase;
	public final int player;
	
	public NextPhase(Phase nextPhase, Phase previousPhase, int player)
	{
		super(Phase.NEXT_PHASE);
		this.nextPhase = nextPhase;
		this.previousPhase = previousPhase;
		this.player = player;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof NextPhase)
		{
			NextPhase np = (NextPhase)o;
			return np.nextPhase == nextPhase &&
				   np.previousPhase == previousPhase &&
				   np.player == player;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "NextPhase [nextPhase=" + nextPhase + ", player=" + player + "]";
	}
}
