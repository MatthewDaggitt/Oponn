
package com.mld46.oponn;

import com.mld46.oponn.sim.boards.SimulationBoard;
import com.mld46.oponn.sim.boards.SimulationBoard.AttackState;
import com.mld46.oponn.sim.boards.SimulationBoard.Phase;
import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;

public class BoardState
{
	// Immutable state
	
		// Game settings
	
		public final int numberOfPlayers;
		public final String [] agentNames;
		public final int numberOfCountries;
		public final int numberOfContinents;
		public final int [] initialContinentBonuses;
		
		public final boolean useCards;
		public final boolean transferCards;
		public final boolean immediateCash;
		public final int continentIncrease;
		public final String cardProgression;
	
	// Mutable state

		public int turnCount;
		public int currentPlayer;
		public Phase currentPhase;
		
		public Country [] countries;
		
		// Card state
		
		public int cardProgressionPos = 0;
		public int [] playerNumberOfCards;
		
		// Placement state
	
		public int numberOfPlaceableArmies;
		
		// Attacking state
	
		public boolean hasInvadedACountry;
		public AttackState attackState;
		public int attackingCC;
		public int defendingCC;
		public int defendingPlayer;
		
		// Fortification phase
		
		public boolean [] fortifiedCountries;
		
	public BoardState(Board board, int currentPlayer)
	{
		numberOfPlayers = board.getNumberOfPlayers();
		numberOfCountries = board.getNumberOfCountries();
		numberOfContinents = board.getNumberOfContinents();
		initialContinentBonuses = new int[numberOfContinents];
		for(int i = 0; i < numberOfContinents; i++)
		{
			initialContinentBonuses[i] = board.getContinentBonus(i);
		}
		
		agentNames = new String[numberOfPlayers];
		for(int i = 0; i < numberOfPlayers; i++)
		{
			agentNames[i] = board.getAgentName(i);
		}
		
		useCards = board.useCards();
		transferCards = board.transferCards();
		immediateCash = board.immediateCash();
		continentIncrease = board.getContinentIncrease();
		cardProgression = board.getCardProgression();
		
		countries = board.getCountries();
		fortifiedCountries = new boolean[numberOfCountries];
		playerNumberOfCards = new int[numberOfPlayers];
		
		this.currentPlayer = currentPlayer;
	}
	
	public BoardState(SimulationBoard simBoard, int currentPlayer)
	{		
		numberOfPlayers = simBoard.getNumberOfPlayers();
		numberOfCountries = simBoard.getNumberOfCountries();
		numberOfContinents = simBoard.getNumberOfContinents();
		initialContinentBonuses = new int[numberOfContinents];
		for(int i = 0; i < numberOfContinents; i++)
		{
			initialContinentBonuses[i] = simBoard.getContinentBonus(i);
		}
		
		agentNames = new String[numberOfPlayers];
		for(int i = 0; i < numberOfPlayers; i++)
		{
			agentNames[i] = simBoard.getRealAgentName(i);
		}
				
		useCards = simBoard.useCards();
		transferCards = simBoard.transferCards();
		immediateCash = simBoard.immediateCash();
		continentIncrease = simBoard.getContinentIncrease();
		cardProgression = simBoard.getCardProgression();
		
		countries = simBoard.getCountries();
		playerNumberOfCards = new int[numberOfPlayers];
		fortifiedCountries = new boolean[numberOfCountries];
		
		this.currentPlayer = currentPlayer;
	}

	
	public static Country [] cloneCountries(Country [] originalCountries)
	{
		int numberOfCountries = originalCountries.length;
				
		Country [] countries = new Country[numberOfCountries];
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			Country originalCountry = originalCountries[cc];
			countries[cc] = new Country(cc,originalCountry.getContinent(),null);
			countries[cc].setName(originalCountry.getName(), null);
		}
		
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
	        for(int nc : originalCountries[cc].getAdjoiningCodeList())
	        {
	            countries[cc].addToAdjoiningList(countries[nc], null);
	        }
		}
		
		return countries;
	}
}
