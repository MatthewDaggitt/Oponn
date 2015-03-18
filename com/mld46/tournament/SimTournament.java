package com.mld46.tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.mld46.agent.BoardState;
import com.mld46.agent.CardManager;
import com.mld46.sim.agent.SimAgent;
import com.mld46.sim.board.SimBoard;
import com.sillysoft.lux.Country;

public class SimTournament extends Tournament
{
	private final BoardState boardState;
	private final Country [] countries;
	private final CardManager cardManager;
	
	public SimTournament(String [] entrants, int playersPerGame, int gamesPerMatch, Country [] countries, BoardState boardState)
	{
		super(entrants, playersPerGame, gamesPerMatch);
		
		this.countries = countries;
		this.boardState = boardState;
		this.boardState.numberOfPlayers = playersPerGame;
		this.boardState.agentNames = new String[playersPerGame];
		
		this.cardManager = new CardManager(countries, playersPerGame);
	}
	
	@Override
	public List<String> playMatch(List<String> playerNames)
	{
		SimAgent [] agents = new SimAgent[players];
		List<String> results = new ArrayList<String>(gamesPerMatch);
		for(int i = 0; i < gamesPerMatch; i++)
		{
			Collections.shuffle(playerNames);
			System.out.println(playerNames);
			for(int j = 0; j < playerNames.size(); j++)
			{
				agents[j] = SimAgent.getSimAgent(playerNames.get(j), false);
				boardState.agentNames[j] = playerNames.get(j);
			}
			
			SimBoard simBoard = new SimBoard(countries, boardState, agents, cardManager);
			simBoard.setupNewGame(agents);
			simBoard.simulate();
			
			String result = "";
			for(int player : simBoard.getPlayerPositions())
			{
				result = playerNames.get(player) + " " + result;
			}
			results.add(result);
			
			Debug.outputGameNumber(" " + (i+1) + "/" + gamesPerMatch + " " + new Date().toString().substring(11,19));
		}
		return results;
	}
}
