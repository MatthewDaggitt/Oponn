package com.mld46.oponn.sim.boards;

import com.mld46.oponn.BoardState;
import com.mld46.oponn.CardManager;
import com.mld46.oponn.sim.agents.SimAgent;
import com.mld46.oponn.sim.agents.SimRandom;
import com.sillysoft.lux.Country;

public class ModellingSimBoard extends ExploratorySimBoard
{
	private final SimAgent [] initialSimAgents;
	private final int modellingTurnLimit;
	
	public ModellingSimBoard(Country[] originalCountries,
			BoardState boardState, SimAgent[] simAgents, CardManager cardManager, int coreID, int modellingTurnLimit)
	{
		super(originalCountries, boardState, simAgents, cardManager, coreID);
	
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
