package com.mld46.oponn.sim.boards;

import com.mld46.oponn.BoardState;
import com.mld46.oponn.CardManager;
import com.mld46.oponn.sim.agents.SimAgent;
import com.mld46.oponn.sim.agents.SimRandom;

public class ModellingBoard extends SimulationBoard
{
	private final SimAgent [] initialSimAgents;
	private final int modellingTurnLimit;
	
	public ModellingBoard(BoardState bs, SimAgent[] sas, CardManager cm, int cid, int modellingTurnLimit)
	{
		super(bs,sas,cm,cid);
	
		this.modellingTurnLimit = modellingTurnLimit;
		this.initialSimAgents = simAgents;
	}

	@Override
	protected void setupGeneralGameState()
	{
		simAgents = initialSimAgents;

		super.setupGeneralGameState();
	}
	
	@Override
	protected void tearDownFortificationPhase()
	{
		int nextPlayer = getNextPlayer();
		if(nextPlayer < currentPlayer)
		{
			if(simTurnCount + 1 == modellingTurnLimit)
			{
				simAgents = new SimAgent[numberOfPlayers];
				for(int i = 0; i < numberOfPlayers; i++)
				{
					simAgents[i] = new SimRandom();
					simAgents[i].setSimPrefs(i, this);
				}
			}
		}
		
		super.tearDownFortificationPhase();
	}
}
