
package com.mld46.agent;

import com.mld46.sim.board.SimBoard;
import com.mld46.sim.board.SimBoard.AttackState;
import com.mld46.sim.board.SimBoard.Phase;
import com.sillysoft.lux.Board;

public class BoardState
{
	// Immutable state
	
		// Game settings
	
		public int numberOfPlayers;
		public final int numberOfCountries;
		public final int numberOfContinents;
		public final int [] initialContinentBonuses;
		
		public String [] agentNames;
		
		public final boolean useCards;
		public final boolean transferCards;
		public final boolean immediateCash;
		public final int continentIncrease;
		public final String cardProgression;
	
		// Sim settings
		
		public final boolean placementPartialOrderOptimisation;
		public final boolean attackAggressiveOptimisation;
		public final boolean attackRepeatMovesOptimisation;
		public final boolean attackPartialOrderOptimisation;
		public final boolean invasionOptimisation;
		public final boolean fortificationContinentOptimisation;
		public final boolean fortificationRepeatMovesOptimisation;
		public final boolean fortificationPartialOrderOptimisation;
		
	// Mutable state
		
		public int currentPlayer;
		public Phase currentPhase;
		public int [] playerNumberOfCards;
		public int turnCount;
		
		// Initial placement phase
		
		public int initialPlacementRound;
		
		// Placement state
	
		public int numberOfPlaceableArmies;
		
		// Attacking state
	
		public boolean hasCapturedACountry;
		public AttackState attackState;
		public int attackingCountry;
		public int defendingCountry;
		public int defendingPlayer;
		
	public BoardState(
		Board board,
		int agentID,
		boolean placementPartialOrderOptimisation,
		boolean attackAggressiveOptimisation,
		boolean attackRepeatMovesOptimisation,
		boolean attackPartialOrderOptimisation,
		boolean invasionOptimisation,
		boolean fortificationContinentOptimisation,
		boolean fortificationRepeatMovesOptimisation,
		boolean fortificationPartialOrderOptimisation)
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
		
		currentPlayer = agentID;
		
		playerNumberOfCards = new int[numberOfPlayers];
		
		this.placementPartialOrderOptimisation = placementPartialOrderOptimisation;
		this.attackAggressiveOptimisation = attackAggressiveOptimisation;
		this.attackRepeatMovesOptimisation = attackRepeatMovesOptimisation;
		this.attackPartialOrderOptimisation = attackPartialOrderOptimisation;
		this.invasionOptimisation = invasionOptimisation;
		this.fortificationContinentOptimisation = fortificationContinentOptimisation;
		this.fortificationRepeatMovesOptimisation = fortificationRepeatMovesOptimisation;
		this.fortificationPartialOrderOptimisation = fortificationPartialOrderOptimisation;
	}
	
	public BoardState(
			SimBoard simBoard,
			int agentID,
			boolean placementPartialOrderOptimisation,
			boolean attackAggressiveOptimisation,
			boolean attackRepeatMovesOptimisation,
			boolean attackPartialOrderOptimisation,
			boolean invasionOptimisation,
			boolean fortificationContinentOptimisation,
			boolean fortificationRepeatMovesOptimisation,
			boolean fortificationPartialOrderOptimisation)
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
			
			currentPlayer = agentID;
			
			playerNumberOfCards = new int[numberOfPlayers];
			
			this.placementPartialOrderOptimisation = placementPartialOrderOptimisation;
			this.attackAggressiveOptimisation = attackAggressiveOptimisation;
			this.attackRepeatMovesOptimisation = attackRepeatMovesOptimisation;
			this.attackPartialOrderOptimisation = attackPartialOrderOptimisation;
			this.invasionOptimisation = invasionOptimisation;
			this.fortificationContinentOptimisation = fortificationContinentOptimisation;
			this.fortificationRepeatMovesOptimisation = fortificationRepeatMovesOptimisation;
			this.fortificationPartialOrderOptimisation = fortificationPartialOrderOptimisation;
		}
	
	public void transferMutableState(BoardState bs)
	{
		bs.numberOfPlaceableArmies = numberOfPlaceableArmies;
		bs.hasCapturedACountry = hasCapturedACountry;
		bs.attackState = attackState;
		bs.attackingCountry = attackingCountry;
		bs.defendingCountry = defendingCountry;
		bs.defendingPlayer = defendingPlayer;
		bs.playerNumberOfCards = playerNumberOfCards;
		bs.currentPhase = currentPhase;
		bs.turnCount = turnCount;
		bs.currentPlayer = currentPlayer;
	}
}
