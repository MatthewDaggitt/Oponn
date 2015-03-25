package com.mld46.oponn.sim.boards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.mld46.oponn.BattleSim;
import com.mld46.oponn.BoardState;
import com.mld46.oponn.CardManager;
import com.mld46.oponn.MapStats;
import com.mld46.oponn.moves.*;
import com.mld46.oponn.sim.agents.SimAgent;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;

public class ExploratorySimBoard extends SimBoard
{
	// Optimisations
	
	private final boolean placementPartialOrderOptimisation;
	private final boolean attackAggressiveOptimisation;
	private final boolean attackRepeatMovesOptimisation;
	private final boolean attackPartialOrderOptimisation;
	private final boolean attackUntilDeadOptimisation;
	private final boolean invasionOptimisation;
	private final boolean fortificationContinentOptimisation;
	private final boolean fortificationRepeatMovesOptimisation;
	private final boolean fortificationPartialOrderOptimisation;
	
	// Partial order variables
	
	private int minPlacementCC;
	private int minAttackSourceCC;
	private int minAttackDestinationCC;
	private boolean initiatedWithInvasion;
	private int minFortificationSourceCC;
	private int minFortificationDestinationCC;
	
	// Attack phase
	
	private boolean [][] attacksMade;
	private boolean [][] fortificationsMade;
	
	/*****************/
	/** Constructor **/
	/*****************/
	
	public ExploratorySimBoard(Country [] originalCountries, BoardState boardState, SimAgent [] simAgents, CardManager cardManager, int coreID)
	{
		super(originalCountries, boardState, simAgents, cardManager, coreID);
		
		/*****************/
		/** Board state **/
		/*****************/
		
		placementPartialOrderOptimisation = boardState.placementPartialOrderOptimisation;
		attackAggressiveOptimisation = boardState.attackAggressiveOptimisation;
		attackRepeatMovesOptimisation = boardState.attackRepeatMovesOptimisation;
		attackPartialOrderOptimisation = boardState.attackPartialOrderOptimisation;
		attackUntilDeadOptimisation = boardState.attackUntilDeadOptimisation;
		invasionOptimisation = boardState.invasionOptimisation;
		fortificationContinentOptimisation = boardState.fortificationContinentOptimisation;
		fortificationRepeatMovesOptimisation = boardState.fortificationRepeatMovesOptimisation;
		fortificationPartialOrderOptimisation = boardState.fortificationPartialOrderOptimisation;
		
		fortificationsMade = new boolean[numberOfCountries][numberOfCountries];
		attacksMade = new boolean[numberOfCountries][numberOfCountries];
	}
	
	@Override
	@SuppressWarnings("incomplete-switch")
	protected void setupPhaseGameState(BoardState boardState)
	{
		super.setupPhaseGameState(boardState);
		
		switch(currentPhase) 
		{
			case INITIAL_PLACEMENT:
				minPlacementCC = 0;
				break;
				
			case PLACEMENT:
				minPlacementCC = 0;
				break;
				
			case ATTACK:
				if(attackRepeatMovesOptimisation)
				{
					for(int cc1 = 0; cc1 < numberOfCountries; cc1++)
					{
						for(int cc2 = 0; cc2 < numberOfCountries; cc2++)
						{
							attacksMade[cc1][cc2] = false;
						}
					}
				}
				initiatedWithInvasion = boardState.attackState == AttackState.INVADE;
				
				minPlacementCC = 0;
				minAttackSourceCC = 0;
				minAttackDestinationCC = 0;
				break;
				
			case FORTIFICATION:
				if(fortificationRepeatMovesOptimisation)
				{
					for(int cc1 = 0; cc1 < numberOfCountries; cc1++)
					{
						for(int cc2 = 0; cc2 < numberOfCountries; cc2++)
						{
							fortificationsMade[cc1][cc2] = false;
						}
					}
				}
				
				minFortificationSourceCC = 0;
				minFortificationDestinationCC = 0;
				break;
		}
	}
	
	public boolean updateState(Move move)
	{
		if(move.phase != currentPhase && move.phase != Phase.NEXT_PHASE)
		{
			throw new IllegalArgumentException("Trying to simulate move of phase " + move.phase + " in phase " + currentPhase);
		}
		else if(move instanceof CountrySelection)
		{
			CountrySelection cs = (CountrySelection) move;
			selectCountry(cs.cc);
		}
		else if(move instanceof InitialPlacement)
		{
			InitialPlacement ip = (InitialPlacement) move;
			placeArmies(ip.armies, ip.cc);
			
			if(placementPartialOrderOptimisation)
			{
				minPlacementCC = ip.cc+1;
			}
		}
		else if(move instanceof CardCash)
		{
			CardCash cc = (CardCash) move;
			cashCards(cc.card1,cc.card2,cc.card3);
		}
		else if(move instanceof Placement)
		{
			Placement p = (Placement) move;
			placeArmies(p.armies,p.cc);
				
			if(placementPartialOrderOptimisation)
			{
				minPlacementCC = p.cc+1;
			}
		}
		else if(move instanceof Attack)
		{
			Attack a = (Attack) move;
			
			if(attackRepeatMovesOptimisation && attackingCountry != null && defendingCountry != null && (attackingCountry.getCode() != a.attackerCC || defendingCountry.getCode() != a.defenderCC))
			{
				// If we've switched attacking countries then we may not go back to the previous attack
				attacksMade[attackingCountry.getCode()][defendingCountry.getCode()] = true;
			}
			
			minAttackSourceCC = a.attackerCC;
			minAttackDestinationCC = a.defenderCC;
			
			setupAttack(a.attackerCC, a.defenderCC, a.attackUntilDead);
		}
		else if(move instanceof AttackOutcome)
		{
			AttackOutcome ao = (AttackOutcome) move;
			attackerLosses = ao.attackerLosses;
			defenderLosses = ao.defenderLosses;
			finishAttack();
			
			if(ao.invading)
			{
				setupInvasion();
			}
		}
		else if(move instanceof Invasion)
		{
			Invasion i = (Invasion) move;
			executeInvasion(i.armies);
			finishInvasion();
			
			if(attackPartialOrderOptimisation)
			{
				if(initiatedWithInvasion)
				{
					initiatedWithInvasion = false;
				}
				else
				{
					minAttackSourceCC = attackingCountry.getCode();
					
					int nc;
					for(Country neighbour : defendingCountry.getAdjoiningList())
					{
						nc = neighbour.getCode();
						if(neighbour.getOwner() != currentPlayer && nc < minAttackSourceCC)
						{
							minAttackSourceCC = nc;
						}
					}
					
					if(minAttackSourceCC == attackingCountry.getCode())
					{
						minAttackDestinationCC = defendingCountry.getCode();
					}
					else
					{
						minAttackDestinationCC = 0;
					}
				}
			}
		}
		else if(move instanceof Fortification)
		{
			Fortification f = (Fortification) move;
			fortifyArmies(f.armies,f.sourceCC,f.destinationCC);
			
			if(fortificationRepeatMovesOptimisation)
			{
				fortificationsMade[f.sourceCC][f.destinationCC] = true;
				fortificationsMade[f.destinationCC][f.sourceCC] = true;
			}
			
			if(fortificationPartialOrderOptimisation)
			{
				if(f.continentOptimisationFortification)
				{
					minFortificationSourceCC = 0;
					minFortificationDestinationCC = 0;		
				}
				else
				{
					minFortificationSourceCC = f.sourceCC;
					minFortificationDestinationCC = f.destinationCC;
				}
			}
		}
		else if(move instanceof NextPhase)
		{
			NextPhase np = (NextPhase) move;
			
			switch(np.nextPhase)
			{
				case INITIAL_PLACEMENT:
					if(np.previousPhase == Phase.COUNTRY_SELECTION)
					{
						tearDownCountrySelectionPhase();
					}
					else
					{
						tearDownInitialPlacementPhase();
					}
					setupInitialPlacementPhase();
					break;
					
				case CARDS:
					if(np.previousPhase == Phase.INITIAL_PLACEMENT)
					{
						tearDownInitialPlacementPhase();
					}
					else
					{
						tearDownFortificationPhase();
					}
					setupCardsPhase();
					break;
				
				case PLACEMENT:
					tearDownCardsPhase();
					setupPlacementPhase();
					break;
					
				case ATTACK:
					tearDownPlacementPhase();
					setupAttackPhase();
					break;
				
				case FORTIFICATION:
					tearDownAttackPhase();
					setupFortificationPhase();
					break;
					
				default:
					throw new IllegalArgumentException("Next phase cannot be of type " + np.nextPhase);
			}
		}
		
		return remainingPlayers != 1;
	}
	
	/********************/
	/** Tree expansion **/
	/********************/

	public List<Move> getPossibleMoves()
	{
		switch(currentPhase)
		{
			case COUNTRY_SELECTION:
				return getCountrySelectionMoves();
			case INITIAL_PLACEMENT:
				return getInitialPlacementMoves();
			case CARDS:
				return getCardMoves(Phase.CARDS);
			case PLACEMENT:
				return getPlacementMoves(Phase.PLACEMENT);
			case ATTACK:
				return getAttackMoves();
			case FORTIFICATION:
				return getFortificationMoves();
			default:
				throw new IllegalArgumentException("Cannot provide moves for phase " + currentPhase);
		}
	}
	
	private List<Move> getCountrySelectionMoves()
	{
		List<Move> moves = new ArrayList<Move>();
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			if(countries[cc].getOwner() == -1)
			{
				moves.add(new CountrySelection(cc));
			}
		}
		
		if(moves.size() == 0)
		{
			moves.add(new NextPhase(Phase.INITIAL_PLACEMENT, Phase.COUNTRY_SELECTION, 0));
		}
		return moves;
	}
	
	private List<Move> getCardMoves(Phase phase)
	{
		Card [] cards = new Card[playerCards.get(currentPlayer).size()];
		cards = playerCards.get(currentPlayer).toArray(cards);
		
		List<Move> moves = new ArrayList<Move>();
		
		if(cards.length >= 3)
		{
			//TODO Need to change!
			Card [] bestSet = Card.getBestSet(cards,currentPlayer,countries);
			if(bestSet != null)
			{
				moves.add(new CardCash(bestSet[0], bestSet[1], bestSet[2], phase));
			}
		}
		
		if(cards.length < 5)
		{
			moves.add(new NextPhase(Phase.PLACEMENT, Phase.CARDS, currentPlayer));
		}
		return moves;
	}
	
	private List<Move> getInitialPlacementMoves()
	{
		List<Move> moves = new ArrayList<Move>();
		
		if(numberOfPlaceableArmies == 0)
		{
			int nextPlayer = (currentPlayer + 1) % numberOfPlayers;
			while(nextPlayer != currentPlayer && initialArmiesLeftToPlace[nextPlayer] == 0)
			{
				nextPlayer = (nextPlayer + 1) % numberOfPlayers;
			}
			
			if(nextPlayer == currentPlayer)
			{
				// Then go to the start of the cards phase
				moves.add(new NextPhase(Phase.CARDS, Phase.INITIAL_PLACEMENT, 0));
			}
			else
			{
				moves.add(new NextPhase(Phase.INITIAL_PLACEMENT, Phase.INITIAL_PLACEMENT, nextPlayer));
			}
		}
		else
		{
			List<Country> possiblePlacements = new ArrayList<Country>();
			Country country;
			int startIndex = placementPartialOrderOptimisation ? minPlacementCC : 0;
			
			for(int cc = startIndex; cc < numberOfCountries; cc++)
			{
				country = countries[cc];
				if(country.getOwner() == currentPlayer && country.getNumberEnemyNeighbors() > 0)
				{
					possiblePlacements.add(country);
				}
			}
			
			int numberOfPlacements = possiblePlacements.size();
			
			if(placementPartialOrderOptimisation)
			{
				int cc;
				for(int i = 0; i < numberOfPlacements-1; i++)
				{
					cc = possiblePlacements.get(i).getCode();
					for(int j = 1; j <= numberOfPlaceableArmies; j++)
					{
						moves.add(new InitialPlacement(cc,j));
					}
				}
				
				moves.add(new InitialPlacement(possiblePlacements.get(numberOfPlacements-1).getCode(),numberOfPlaceableArmies));
			}
			else
			{
				for(Country targetCountry : possiblePlacements)
				{
					for(int j = 1; j <= numberOfPlaceableArmies; j++)
					{
						moves.add(new InitialPlacement(targetCountry.getCode(),j));
					}
				}
			}
		}
		
		return moves;
	}
	
	private List<Move> getPlacementMoves(Phase phase)
	{
		List<Move> moves = new ArrayList<Move>();
		
		if(numberOfPlaceableArmies == 0)
		{
			moves.add(new NextPhase(Phase.ATTACK, Phase.PLACEMENT, currentPlayer));
		}
		else
		{
			List<Country> possiblePlacements = new ArrayList<Country>();
			Country country;
			int startIndex = placementPartialOrderOptimisation ? minPlacementCC : 0;
			
			for(int cc = startIndex; cc < numberOfCountries; cc++)
			{
				country = countries[cc];
				if(country.getOwner() == currentPlayer && country.getNumberEnemyNeighbors() > 0)
				{
					possiblePlacements.add(country);
				}
			}
			
			int numberOfPlacements = possiblePlacements.size();
			
			if(placementPartialOrderOptimisation)
			{
				int cc;
				for(int i = 0; i < numberOfPlacements-1; i++)
				{
					cc = possiblePlacements.get(i).getCode();
					for(int j = 1; j <= numberOfPlaceableArmies; j++)
					{
						moves.add(new Placement(cc,j,phase));
					}
				}
				
				moves.add(new Placement(possiblePlacements.get(numberOfPlacements-1).getCode(), numberOfPlaceableArmies, phase));
			}
			else
			{
				for(Country targetCountry : possiblePlacements)
				{
					for(int j = 1; j <= numberOfPlaceableArmies; j++)
					{
						moves.add(new Placement(targetCountry.getCode(), j, phase));
					}
				}
			}
		}
		
		return moves;
	}
	
	private List<Move> getAttackMoves()
	{
		List<Move> moves = new ArrayList<Move>();
		int attackers, defenders;
		int attackerCC, defenderCC;
		
		if(attackState == AttackState.START)
		{
			Country attackingCountry;
			int startSourceIndex = attackPartialOrderOptimisation ? minAttackSourceCC : 0;
			
			for(attackerCC = startSourceIndex; attackerCC < numberOfCountries; attackerCC++)
			{
				attackingCountry = countries[attackerCC];
				attackers = attackingCountry.getArmies();
				
				if(attackingCountry.getOwner() == currentPlayer && attackers > 1)
				{
					int startDestinationIndex = attackPartialOrderOptimisation ? minAttackDestinationCC : 0;
					
					for(Country defendingCountry : attackingCountry.getAdjoiningList())
					{
						defenderCC = defendingCountry.getCode();
						defenders = defendingCountry.getArmies();
						
						if(defendingCountry.getOwner() != currentPlayer && defenderCC >= startDestinationIndex && 
								!(attackRepeatMovesOptimisation && attacksMade[attackerCC][defenderCC]) &&
								!BattleSim.unfavourableForAttacker(attackers-1,defenders))
						{
							moves.add(new Attack(attackerCC,defenderCC,attackers,defenders, false));
							if(attackUntilDeadOptimisation && attackers > 2)
							{
								moves.add(new Attack(attackerCC,defenderCC,attackers,defenders, true));
							}
						}
					}
				}
			}
			
			if(attackAggressiveOptimisation && remainingPlayers == 2)
			{
				Attack a;
				boolean optimalAttacksExhausted = true;
				
				for(Move m : moves)
				{
					a = (Attack) m;
					if(BattleSim.favourableForAttacker(a.attackingArmies-1,a.defendingArmies))
					{
						optimalAttacksExhausted = false;
					}
				}
				
				if(optimalAttacksExhausted)
				{
					moves.add(new NextPhase(Phase.FORTIFICATION, Phase.ATTACK, currentPlayer));
				}
			}
			else
			{
				moves.add(new NextPhase(Phase.FORTIFICATION, Phase.ATTACK, currentPlayer));
			}
		}
		else if(attackState == AttackState.EXECUTE)
		{
			attackers = attackingCountry.getArmies();
			defenders = defendingCountry.getArmies();
			
			if(attackUntilDeadOptimisation && attackUntilDead)
			{
				for(AttackOutcome outcome : BattleSim.getAttackOutcomes(attackers-1, defenders))
				{
					moves.add(outcome);
				}
			}
			else
			{
				if(attackers == 2 && defenders == 1)
				{
					moves.add(new AttackOutcome(0,1,BattleSim.a1_d1_al0,true));
					moves.add(new AttackOutcome(1,0,BattleSim.a1_d1_al1,false));
				}
				else if(attackers == 3 && defenders == 1)
				{
					moves.add(new AttackOutcome(0,1,BattleSim.a2_d1_al0,true));
					moves.add(new AttackOutcome(1,0,BattleSim.a2_d1_al1,false));
				}
				else if(attackers >= 4 && defenders == 1)
				{		
					moves.add(new AttackOutcome(0,1,BattleSim.a3_d1_al0,true));
					moves.add(new AttackOutcome(1,0,BattleSim.a3_d1_al1,false));
				}
				else if(attackers == 2 && defenders >= 2)
				{
					moves.add(new AttackOutcome(0,1,BattleSim.a1_d2_al0,false));
					moves.add(new AttackOutcome(1,0,BattleSim.a1_d2_al1,false));
				}
				else if(attackers == 3 && defenders >= 2)
				{
					moves.add(new AttackOutcome(0,2,BattleSim.a2_d2_al0,defenders == 2));
					moves.add(new AttackOutcome(1,1,BattleSim.a2_d2_al1,false));
					moves.add(new AttackOutcome(2,0,BattleSim.a2_d2_al2,false));
				}
				else if(attackers >= 4 && defenders >= 2)
				{
					moves.add(new AttackOutcome(0,2,BattleSim.a3_d2_al0,defenders == 2));
					moves.add(new AttackOutcome(1,1,BattleSim.a3_d2_al1,false));
					moves.add(new AttackOutcome(2,0,BattleSim.a3_d2_al2,false));
				}
			}
		}
		else if(attackState == AttackState.INVADE)
		{
			boolean onlyLargest = false;
			
			if(invasionOptimisation)
			{
				List<Country> attackerHostileNeighbours = new ArrayList<Country>();
				List<Country> defenderHostileNeighbours = new ArrayList<Country>();
				
				for(Country neighbour : attackingCountry.getAdjoiningList())
				{
					if(neighbour.getOwner() != currentPlayer)
					{
						attackerHostileNeighbours.add(neighbour);
					}
				}
				
				for(Country neighbour : defendingCountry.getAdjoiningList())
				{
					if(neighbour.getOwner() != currentPlayer)
					{
						defenderHostileNeighbours.add(neighbour);
					}
				}
				
				onlyLargest = defenderHostileNeighbours.containsAll(attackerHostileNeighbours);
			}
			
			if(onlyLargest)
			{
				moves.add(new Invasion(attackingCountry.getCode(),defendingCountry.getCode(),attackingCountry.getArmies()-1));
			}
			else
			{
				attackers = attackingCountry.getArmies();
				for(int i = Math.min(attackers-1,3); i < attackers; i++)
				{
					moves.add(new Invasion(attackingCountry.getCode(),defendingCountry.getCode(),i));
				}
			}
		}
		else if(attackState == AttackState.CASH)
		{
			return getCardMoves(Phase.ATTACK);
		}
		else if(attackState == AttackState.PLACE) 
		{
			return getPlacementMoves(Phase.ATTACK);
		}
		
		return moves;
	}
	
	private List<Move> getFortificationMoves()
	{
		List<Move> moves = new ArrayList<Move>();
		
		if(fortificationContinentOptimisation)
		{
			// For each continent owned, check for optimal reinforcement moves
			for(int continent = 0; continent < numberOfContinents; continent++)
			{
				if(countriesPlayerMissingInContinent[currentPlayer][continent] == 0)
				{
					getCompulsoryInternalContinentFortifications(moves,continent);
					
					if(!moves.isEmpty())
					{
						return moves;
					}
					
					getOtherInternalContinentFortifications(moves,continent);
					
					if(!moves.isEmpty())
					{
						return moves;
					}
				}
			}
		}
		
		// Otherwise consider all moves
		getOtherFortificationMoves(moves);
		
		return moves;
	}
	
	private void getCompulsoryInternalContinentFortifications(List<Move> moves, int continent)
	{
		Country sourceCountry;
		int sourceCC;
		int moveableArmies;
		
		List<Entry<Integer,List<Integer>>> compulsoryMoves;
		List<Integer> destinationCCs;
		List<Integer> validDestinationCCs;
		Entry<Integer,List<Integer>> moveSet;
		
		int compulsoryMovesSize;
		
		compulsoryMoves = MapStats.getCompulsoryFortifications(continent);
		compulsoryMovesSize = compulsoryMoves.size();
		
		for(int i = 0; i < compulsoryMovesSize; i++)
		{
			moveSet = compulsoryMoves.get(i);
			sourceCC = moveSet.getKey();
			sourceCountry = countries[sourceCC];
			moveableArmies = Math.min(sourceCountry.getMoveableArmies(),sourceCountry.getArmies()-1);
			
			if(moveableArmies > 0)
			{
				destinationCCs = moveSet.getValue();
				
				if(fortificationRepeatMovesOptimisation)
				{
					validDestinationCCs = new ArrayList<Integer>();
					for(int destinationCC : destinationCCs)
					{
						if(!fortificationsMade[sourceCC][destinationCC])
						{
							validDestinationCCs.add(destinationCC);
						}
					}
				}
				else
				{
					validDestinationCCs = destinationCCs;
				}
				
				if(validDestinationCCs.size() == 1)
				{
					moves.add(new Fortification(moveableArmies,sourceCC,validDestinationCCs.get(0),true));
				}
				else
				{
					for(int destinationCC : validDestinationCCs)
					{
						for(int armies = 1; armies <= moveableArmies; armies++)
						{
							moves.add(new Fortification(armies,sourceCC,destinationCC,true));
						}
					}
				}
				
				return;
			}
		}
	}
	
	private void getOtherInternalContinentFortifications(List<Move> moves, int continent)
	{
		int moveableArmies;		
		Country sourceCountry;
		List<Integer> validDestinationCCs;
		
		List<Integer> internalCountries = MapStats.getContinentInternalCountries(continent);
		
		for(int sourceCC : internalCountries)
		{
			sourceCountry = countries[sourceCC];
			moveableArmies = Math.min(sourceCountry.getMoveableArmies(),sourceCountry.getArmies()-1);
			
			if(moveableArmies > 0)
			{
				validDestinationCCs = new ArrayList<Integer>();
				for(int destinationCC : sourceCountry.getAdjoiningCodeList())
				{
					if(!(fortificationRepeatMovesOptimisation && fortificationsMade[sourceCC][destinationCC]) &&
						MapStats.isValidFortificationMoveInOwnedContinent(sourceCC,destinationCC))
					{
						validDestinationCCs.add(destinationCC);
					}
				}
				
				if(validDestinationCCs.size() == 1)
				{
					moves.add(new Fortification(moveableArmies,sourceCC,validDestinationCCs.get(0),true));
				}
				else
				{
					for(int destinationCC : validDestinationCCs)
					{
						for(int armies = 1; armies <= moveableArmies; armies++)
						{
							moves.add(new Fortification(armies,sourceCC,destinationCC,true));
						}
					}
				}
				
				return;
			}
		}
	}
	
	private void getOtherFortificationMoves(List<Move> moves)
	{
		Country sourceCountry;
		int continent;
		int moveableArmies;
		int destinationCC;
		int destinationStartCode;
		
		int sourceStartCode = fortificationPartialOrderOptimisation ? minFortificationSourceCC : 0;
		for(int sourceCC = sourceStartCode; sourceCC < numberOfCountries; sourceCC++)
		{
			sourceCountry = countries[sourceCC];
			
			if(sourceCountry.getOwner() == currentPlayer)
			{
				continent = sourceCountry.getContinent();
				moveableArmies = Math.min(sourceCountry.getMoveableArmies(), sourceCountry.getArmies()-1);
				
				if(moveableArmies > 0)
				{
					destinationStartCode = fortificationPartialOrderOptimisation ? minFortificationDestinationCC : 0;
					for(Country destination : sourceCountry.getAdjoiningList())
					{
						destinationCC = destination.getCode();
						if(destinationCC >= destinationStartCode &&
							destination.getOwner() == currentPlayer && 
							!(fortificationRepeatMovesOptimisation && fortificationsMade[sourceCC][destinationCC]) &&
							!(destination.getContinent() == continent && countriesPlayerMissingInContinent[currentPlayer][continent] == 0 && !MapStats.isValidFortificationMoveInOwnedContinent(sourceCC,destinationCC)))
						{
							for(int i = 1; i <= moveableArmies; i++)
							{
								moves.add(new Fortification(i,sourceCC,destinationCC,false));
							}
						}
					}
				}
			}
		}
		moves.add(new NextPhase(Phase.CARDS, Phase.FORTIFICATION, getNextPlayer()));
	}
	

	/****************/
	/** Simulation **/
	/****************/

	@Override
	protected void setupInitialPlacementPhase()
	{
		super.setupInitialPlacementPhase();
		minPlacementCC = 0;
	}

	@Override
	protected void setupPlacementPhase()
	{
		super.setupPlacementPhase();
		minPlacementCC = 0;
	}
	
	@Override
	protected void setupAttackPhase()
	{
		super.setupAttackPhase();
		minAttackSourceCC = 0;
		minAttackDestinationCC = 0;

		if(attackRepeatMovesOptimisation)
		{
			for(boolean [] row : attacksMade)
			{
				Arrays.fill(row, false);
			}
		}
	}

	@Override
	protected void setupFortificationPhase()
	{
		super.setupFortificationPhase();
		
		minFortificationSourceCC = 0;
		minFortificationDestinationCC = 0;
		
		if(fortificationRepeatMovesOptimisation)
		{
			for(boolean [] row : fortificationsMade)
			{
				Arrays.fill(row, false);
			}
		}
	}

	@Override
	protected void executeAttackPlacement()
	{
		minAttackSourceCC = 0;
		minAttackDestinationCC = 0;
		minPlacementCC = 0;
		super.executeAttackPlacement();
	}
}
