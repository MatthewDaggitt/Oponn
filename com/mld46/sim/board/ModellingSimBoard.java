package com.mld46.sim.board;

import com.mld46.agent.BoardState;
import com.mld46.agent.CardManager;
import com.mld46.sim.agent.SimAgent;
import com.mld46.sim.agent.SimRandom;
import com.sillysoft.lux.Country;

public class ModellingSimBoard extends ExploratorySimBoard
{
	private final SimAgent [] initialSimAgents;
	private final int modellingTurnLimit;
	
	public ModellingSimBoard(Country[] originalCountries,
			BoardState boardState, SimAgent[] simAgents, CardManager cardManager, int modellingTurnLimit)
	{
		super(originalCountries, boardState, simAgents, cardManager);
	
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
