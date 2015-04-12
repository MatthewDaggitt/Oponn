package com.mld46.oponn.sim.boards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mld46.oponn.BattleSim;
import com.mld46.oponn.BoardState;
import com.mld46.oponn.CardManager;
import com.mld46.oponn.moves.*;
import com.mld46.oponn.sim.agents.SimAgent;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;

public class SimulationBoard
{
	// Constants
	public enum Phase { 
		COUNTRY_SELECTION,
		INITIAL_PLACEMENT, 
		CARDS, 
		PLACEMENT, 
		ATTACK,
		FORTIFICATION,
		NEXT_PHASE
	}
	
	public enum AttackState {
		START,
		EXECUTE,
		INVADE,
		CASH,
		PLACE,
	}
	
	public static final int DEFENDERS_DESTROYED = 7;
	public static final int ATTACK_INCONCLUSIVE = 0;
	public static final int ATTACKERS_DESTROYED = 13;
	
	private final int coreID;
	
	protected final int numberOfCountries;
	protected final int numberOfContinents;
	protected final int numberOfPlayers;
	
	protected final String [] agentNames;
	
	protected final Country [] countries;
	private final int [] initialContinentBonuses;
	
	// Game settings
		
	private final boolean transferCards;
	private final boolean immediateCash;
	private final boolean useCards;
	private final int continentIncrease;
	private final String cardProgression;
	
	// Game state
	
	private final int [] playerCountries;
	protected final int [][] countriesPlayerMissingInContinent;
	private int [] currentContinentBonuses;
	protected SimAgent [] simAgents;
	private CardManager cardManager;
	protected List<List<Card>> playerCards;
	
	protected int remainingPlayers;

	protected int currentPlayer;
	protected Phase currentPhase;
	protected int simStartTurn;
	protected int simTurnCount;
	private final int [] playerOutOnTurn;
	private final int [] playerPositions;
	
	// Country selection phase
	protected int remainingCountries;
	
	// Initial placement phase
	protected int [] initialArmiesLeftToPlace;
	
	// Placement phase
	protected int numberOfPlaceableArmies;
	
	// Attack phase
	protected boolean hasInvadedACountry;
	protected int attackingCC;
	protected int defendingCC;
	protected int attackingPlayer;
	protected int defendingPlayer;
	protected boolean attackUntilDead;
	protected int attackerLosses;
	protected int defenderLosses;
	protected AttackState attackState;
	
	/*****************/
	/** Constructor **/
	/*****************/
	
	public SimulationBoard(BoardState boardState, SimAgent [] simAgents, CardManager cardManager, int coreID)
	{
		/*****************/
		/** Board state **/
		/*****************/
		
		this.coreID = coreID;
		this.cardManager = cardManager;
		this.simAgents = simAgents;
		
		numberOfPlayers = boardState.numberOfPlayers;
		numberOfCountries = boardState.numberOfCountries;
		numberOfContinents = boardState.numberOfContinents;
		initialContinentBonuses = boardState.initialContinentBonuses;
		
		agentNames = boardState.agentNames;
		
		useCards = boardState.useCards;
		transferCards = boardState.transferCards;
		immediateCash = boardState.immediateCash;
		continentIncrease = boardState.continentIncrease;
		cardProgression = boardState.cardProgression;
		
		countries = BoardState.cloneCountries(boardState.countries);
		currentContinentBonuses = new int[numberOfContinents];
		playerOutOnTurn = new int[numberOfPlayers];
		playerPositions = new int[numberOfPlayers];
		playerCountries = new int [numberOfPlayers];
		countriesPlayerMissingInContinent = new int[numberOfPlayers][numberOfContinents];
		
		initialArmiesLeftToPlace = new int[numberOfPlayers];
				
		playerCards = new ArrayList<List<Card>>(numberOfPlayers);
		for(int player = 0; player < numberOfPlayers; player++)
		{
			playerCards.add(new ArrayList<Card>(5));
		}
	}
	
	public void setupNewGame(SimAgent [] simAgents)
	{
		this.simAgents = simAgents;
		
		// Randomly reassign countries
		List<Integer> ccs = new ArrayList<Integer>();
		for(int i = 0; i < numberOfCountries; i++)
		{
			ccs.add(i);
		}
		Collections.shuffle(ccs);
		int player = 0;
		for(int cc : ccs)
		{
			countries[cc].setOwner(player, null);
			countries[cc].setArmies(1, null);
			player = (player + 1) % numberOfPlayers;
		}
		
		// Reset other state
		simStartTurn = 0;
		currentPlayer = 0;
		
		recalculateContinentBonuses();
		setupGeneralGameState();
		setupCardState(new int[numberOfPlayers]);
		
		initialArmiesLeftToPlace = calculateStartingArmies();
		for(int i = 0; i < numberOfPlayers; i++)
		{
			initialArmiesLeftToPlace[i] -= playerCountries[i];
		}
		
		currentPhase = Phase.INITIAL_PLACEMENT;
		setupInitialPlacementPhase();
	}
	
	public void setupState(BoardState boardState)
	{
		simStartTurn = boardState.turnCount;
		currentPlayer = boardState.currentPlayer;
		currentPhase = boardState.currentPhase;
		
		setupCountries(boardState.countries);
		setupCardState(boardState.playerNumberOfCards);
		setupGeneralGameState();
		setupPhaseGameState(boardState);
		
		recalculateContinentBonuses();
	}
	
	protected void setupCountries(Country [] oldCountries)
	{
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			Country newCountry = countries[cc];
			Country oldCountry = oldCountries[cc];
			
			newCountry.setArmies(oldCountry.getArmies(), null);
			newCountry.setOwner(oldCountry.getOwner(), null);
			newCountry.setMoveableArmies(oldCountry.getMoveableArmies(), null);
		}
	}
	
	protected void setupCardState(int [] numberOfPlayerCards)
	{
		cardManager.simReset(coreID);
		
		List<Card> playersCards;
		for(int player = 0; player < numberOfPlayers; player++)
		{
			// Cards state
			playersCards = playerCards.get(player);
			playersCards.clear();
			if(player == currentPlayer)
			{
				playerCards.get(currentPlayer).addAll(cardManager.getCardsHeld());
			}
			else
			{
				for(int j = 0; j < numberOfPlayerCards[player]; j++)
				{
					playersCards.add(cardManager.simDeal(coreID));
				}
			}
		}
	}
	
	protected void setupGeneralGameState() 
	{
		remainingPlayers = 0;
		simTurnCount = 0;
		
		for(int player = 0; player < numberOfPlayers; player++)
		{
			playerOutOnTurn[player] = 0;
			playerCountries[player] = 0;
			Arrays.fill(countriesPlayerMissingInContinent[player],0);
			simAgents[player].setSimPrefs(player, this);
		}

		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			Country country = countries[cc];
			int owner = country.getOwner();
			int continent = country.getContinent();
			
			for(int player = 0; player < numberOfPlayers; player++)
			{
				if(player != owner)
				{
					countriesPlayerMissingInContinent[player][continent]++;
				}
			}
			
			if(!(currentPhase == Phase.COUNTRY_SELECTION && owner == -1))
			{
				playerCountries[owner]++;
			}
		}
		
		if(currentPhase == Phase.COUNTRY_SELECTION)
		{
			remainingPlayers = numberOfPlayers;
		}
		else
		{
			for(int player = 0; player < numberOfPlayers; player++)
			{
				if(playerCountries[player] > 0)
				{
					remainingPlayers++;
				}
			}
		}
	}
	
	protected void setupPhaseGameState(BoardState boardState)
	{
		switch(currentPhase) 
		{
			case COUNTRY_SELECTION:
				initialArmiesLeftToPlace = calculateStartingArmies();
				remainingCountries = 0;
				for(int cc = 0; cc < numberOfCountries; cc++)
				{
					int owner = countries[cc].getOwner();
					if(owner == -1)
					{
						remainingCountries++;
					}
					else
					{
						initialArmiesLeftToPlace[owner]--;
					}
				}
				break;
				
			case INITIAL_PLACEMENT:
				int [] playerArmies = new int[numberOfPlayers];
				for(Country country : countries)
				{
					playerArmies[country.getOwner()] += country.getArmies();
				}
				
				initialArmiesLeftToPlace = calculateStartingArmies();
				for(int i = 0; i < numberOfPlayers; i++)
				{
					initialArmiesLeftToPlace[i] -= playerArmies[i];
				}
				
				setupInitialPlacementPhase();
				numberOfPlaceableArmies = boardState.numberOfPlaceableArmies;
				break;
				
			case CARDS: 
				setupCardsPhase();
				numberOfPlaceableArmies = boardState.numberOfPlaceableArmies;
				break;
				
			case PLACEMENT:
				setupPlacementPhase();
				numberOfPlaceableArmies = boardState.numberOfPlaceableArmies;
				break;
				
			case ATTACK:
				setupAttackPhase();
				hasInvadedACountry = boardState.hasInvadedACountry;
				attackState = boardState.attackState;
				
				attackingCC = boardState.attackingCC;
				defendingCC = boardState.defendingCC;
				if(attackingCC != -1)
				{
					attackingPlayer = countries[attackingCC].getOwner();
					defendingPlayer = boardState.defendingPlayer;
				}
				
				if(boardState.attackState == AttackState.INVADE && playerCountries[defendingPlayer] == 0)
				{
					remainingPlayers++;
				}
				else if(boardState.attackState == AttackState.CASH)
				{
					numberOfPlaceableArmies = boardState.numberOfPlaceableArmies;
				}
				else if(boardState.attackState == AttackState.PLACE)
				{
					numberOfPlaceableArmies = boardState.numberOfPlaceableArmies;
				}
				break;
				
			case FORTIFICATION:
				break;
				
			default:
				throw new IllegalArgumentException("BoardState does not hold a valid phase");
		}
	}
	
	/****************/
	/** Simulation **/
	/****************/
	
	public int [] simulate()
	{
		for(SimAgent agent : simAgents)
		{
			agent.reset();
		}
		
		// Catch up to the start of the next phase
		switch(currentPhase)
		{	
			case COUNTRY_SELECTION:
				tearDownCountrySelectionPhase();
				break;
				
			case INITIAL_PLACEMENT:
				if(numberOfPlaceableArmies > 0)
				{
					simAgents[currentPlayer].placeInitialArmies(numberOfPlaceableArmies);
				}
				tearDownInitialPlacementPhase();
				break;
				
			case CARDS: 			
				List<Card> cards = playerCards.get(currentPlayer);
				simAgents[currentPlayer].cardsPhase(cards.toArray(new Card[cards.size()]));
				tearDownCardsPhase();
				break;
										
			case PLACEMENT:
				if(numberOfPlaceableArmies > 0)
				{
					simAgents[currentPlayer].placeArmies(numberOfPlaceableArmies);
				}
				tearDownPlacementPhase();
				break;
										
			case ATTACK: 			
				if(attackState == AttackState.EXECUTE)
				{
					executeAttack();
					finishAttack();
				}
				if(attackState == AttackState.INVADE)
				{
					executeInvasion(simAgents[currentPlayer].moveArmiesIn(attackingCC,defendingCC));
					finishInvasion();
				}
				if(attackState == AttackState.CASH)
				{
					executeAttackCash();
				}
				if(attackState == AttackState.PLACE)
				{
					executeAttackPlacement();
				}
				simAgents[currentPlayer].attackPhase();
				tearDownAttackPhase();
				break;
										
			case FORTIFICATION:
				simAgents[currentPlayer].fortifyPhase();
				tearDownFortificationPhase();
				break;
				
			case NEXT_PHASE:
				throw new IllegalArgumentException("Cannot be in the NEXT_PHASE phase during simulation");
		}
		
		
		// Play out simulation
		while(remainingPlayers > 1)
		{
			switch(currentPhase)
			{
				case COUNTRY_SELECTION:
					selectCountry(simAgents[currentPlayer].pickCountry());
					tearDownCountrySelectionPhase();
					break;
					
				case INITIAL_PLACEMENT:
					setupInitialPlacementPhase();
					simAgents[currentPlayer].placeInitialArmies(numberOfPlaceableArmies);
					tearDownInitialPlacementPhase();
					break;
					
				case CARDS:
					setupCardsPhase();
					List<Card> cardsList = playerCards.get(currentPlayer);
					Card [] cards = cardsList.toArray(new Card[cardsList.size()]);
					simAgents[currentPlayer].cardsPhase(cards);
					tearDownCardsPhase();
					break;
					
				case PLACEMENT:
					setupPlacementPhase();
					simAgents[currentPlayer].placeArmies(numberOfPlaceableArmies);
					tearDownPlacementPhase();
					break;
					
				case ATTACK:
					setupAttackPhase();
					simAgents[currentPlayer].attackPhase();
					tearDownAttackPhase();
					break;
					
				case FORTIFICATION:
					setupFortificationPhase();
					simAgents[currentPlayer].fortifyPhase();
					tearDownFortificationPhase();
					break;
				
				case NEXT_PHASE:
					throw new IllegalArgumentException("Cannot be in phase NEXT_PHASE whilst simulating");
			}
		}
		return playerOutOnTurn;
	}
	
	/** Country selection phase **/
	
	protected void selectCountry(int cc)
	{
		if(cc < 0)
		{
			System.out.println("Err...");
		}
		
		if(countries[cc].getOwner() != -1)
		{
			throw new IllegalArgumentException("Cannot pick " + countries[cc].getName() + " as it is already owned!");
		}
		
		countries[cc].setOwner(currentPlayer, null);
		
		playerCountries[currentPlayer]++;
		initialArmiesLeftToPlace[currentPlayer]--;
		remainingCountries--;
		countriesPlayerMissingInContinent[currentPlayer][countries[cc].getContinent()]--;
		
		currentPlayer = (currentPlayer + 1) % numberOfPlayers;
	}
	
	protected void tearDownCountrySelectionPhase()
	{
		if(remainingCountries == 0)
		{
			currentPhase = Phase.INITIAL_PLACEMENT;
			currentPlayer = 0;
		}
	}
	
	/** Initial placement phase **/
	
	protected int [] calculateStartingArmies()
	{
	    double f1 = numberOfCountries/42.0;
	    f1 -= 1.0;
	    f1 /= 2.0;
	    f1 += 1.0;
	    int base = (int) Math.round(f1 * (50 - numberOfPlayers*5));
	    
	    int [] armies = new int[numberOfPlayers];
	    for(int i = 0; i < numberOfPlayers; i++)
	    {
	    	armies[i] = base + (i > 1 ? i : 0);
	    }
	    
	    return armies;
	}
	
	protected void setupInitialPlacementPhase()
	{
		int armies = initialArmiesLeftToPlace[currentPlayer];
		numberOfPlaceableArmies = armies == 5 ? 5 : Math.min(4, armies);
	}
	
	protected void tearDownInitialPlacementPhase()
	{
		if(numberOfPlaceableArmies > 0)
		{
			System.out.println("Error! Agent " + simAgents[currentPlayer].name() + " did not place all their initial armies");
		}
		
		int nextPlayer = (currentPlayer + 1) % numberOfPlayers;
		while(nextPlayer != currentPlayer && initialArmiesLeftToPlace[nextPlayer] == 0)
		{
			nextPlayer = (nextPlayer + 1) % numberOfPlayers;
		}
		
		if(nextPlayer == currentPlayer)
		{
			currentPlayer = 0;
			currentPhase = Phase.CARDS;
			for(int i = 0; i < numberOfPlayers; i++)
			{
				simAgents[i].resetClusterManager();
			}
			simTurnCount++;
		}
		else
		{
			currentPlayer = nextPlayer;
		}
	}
	
	/** Card phase **/
	
	protected void setupCardsPhase()
	{
		numberOfPlaceableArmies = 0;
	}
	
	public boolean cashCards(Card card1, Card card2, Card card3)
	{
		if(currentPhase != Phase.CARDS && (currentPhase != Phase.ATTACK || attackState != AttackState.CASH))
		{
			System.out.println("Wrong phase!");
			return false;
		}
		else if(!Card.isASet(card1,card2,card3))
		{
			System.out.println("Cards are not a set!");
			return false;
		}
		
		List<Card> currentCards = playerCards.get(currentPlayer);
		if(!(currentCards.contains(card1) && currentCards.contains(card2) && currentCards.contains(card3)))
		{
			System.out.println("Player does not own the provided cards: " + card1 + " " + card2 + " " + card3);
			return false;
		}
		
		currentCards.remove(card1);
		currentCards.remove(card2);
		currentCards.remove(card3);
		
		int armies = cardManager.simCash(card1, card2, card3, coreID);
		
		numberOfPlaceableArmies += armies;
		
		Country c;
		for(Card card : new Card[]{card1, card2, card3})
		{
			if(card.getCode() != -1)
			{
				c = countries[card.getCode()];
				if(c.getOwner() == currentPlayer)
				{
					c.setArmies(c.getArmies()+2,null);
				}
			}
		}
		return true;
	}
	
	protected void tearDownCardsPhase()
	{
		if(playerCards.get(currentPlayer).size() > 4)
		{
			List<Card> cardsList = playerCards.get(currentPlayer);
			Card [] cards = cardsList.toArray(new Card[cardsList.size()]);
			
			while(cards.length > 4) 
			{
				Card [] toCash = Card.getRandomSet(cards);
				cashCards(toCash[0], toCash[1], toCash[2]);
				
				cardsList = playerCards.get(currentPlayer);
				cards = cardsList.toArray(new Card[cardsList.size()]);
			}
		}
		
		currentPhase = Phase.PLACEMENT;
	}
	
	/** Placement phase **/
	
	protected void setupPlacementPhase()
	{
		numberOfPlaceableArmies += getPlayerIncome(currentPlayer);
	}
	
	public void placeArmies(int numberOfArmies, int cc)
	{
		Country country = countries[cc];
		if(currentPhase != Phase.PLACEMENT && currentPhase != Phase.INITIAL_PLACEMENT 
				&& (currentPhase != Phase.ATTACK || attackState != AttackState.PLACE))
		{
			throw new IllegalArgumentException("Player " + agentNames[currentPlayer] + 
												" (" + currentPlayer + ") cannot place during " +
												" phase " + currentPhase);
		}
		else if(numberOfArmies > numberOfPlaceableArmies)
		{
			throw new IllegalArgumentException("Player " + agentNames[currentPlayer] +
												" does not have " + numberOfArmies + " to place, " +
												" only has " + numberOfPlaceableArmies);
		}
		else if(country.getOwner() != currentPlayer)
		{
			throw new IllegalArgumentException("Player " + agentNames[currentPlayer] + 
					" (" + currentPlayer + ") " + " does not own " + 
					country.getName() + " (" + cc +
					") and therefore cannot reinforce it.");
		}
		else if(numberOfArmies < 0)
		{
			throw new IllegalArgumentException("Player " + agentNames[currentPlayer] + 
					" (" + currentPlayer + ") " + " is trying to subtract armies from " + 
					country.getName());
		}
		
		country.setArmies(country.getArmies() + numberOfArmies, null);
		numberOfPlaceableArmies -= numberOfArmies;
		
		if(currentPhase == Phase.INITIAL_PLACEMENT)
		{
			initialArmiesLeftToPlace[currentPlayer] -= numberOfArmies;
		}
	}

	protected void tearDownPlacementPhase()
	{
		if(numberOfPlaceableArmies > 0)
		{
			System.out.println("Error! Agent " + simAgents[currentPlayer].name() + " did not place all their armies");
		}
		
		currentPhase = Phase.ATTACK;
	}
	
	/** Attack phase **/
	
	protected void setupAttackPhase()
	{
		hasInvadedACountry = false;
		attackingCC = -1;
		defendingCC = -1;
		attackingPlayer = -1;
		defendingPlayer = -1;

		attackState = AttackState.START;
	}
	
	/**
	 * @return Returns according to the silly Sillysoft.Lux.Board.attack() convention, 7 for invasion,
	 * 13 if attackers left with 1 army and 0 otherwise.
	 */
	public int attack(int attackingCC, int defendingCC, boolean attackUntilDead)
	{
		setupAttack(attackingCC, defendingCC, attackUntilDead);
		executeAttack();
		finishAttack();
		
		if(attackState == AttackState.INVADE)
		{
			setupInvasion();
			executeInvasion(simAgents[currentPlayer].moveArmiesIn(attackingCC,defendingCC));
			finishInvasion();
			
			if(attackState == AttackState.CASH)
			{
				numberOfPlaceableArmies = 0;
				List<Card> cardsList = playerCards.get(currentPlayer);
				Card [] cards = cardsList.toArray(new Card[cardsList.size()]);
				
				simAgents[currentPlayer].cardsPhase(cards);
				simAgents[currentPlayer].placeArmies(numberOfPlaceableArmies);
				
				if(numberOfPlaceableArmies > 0)
				{
					System.out.println("Error! Agent " + simAgents[currentPlayer].name() + " did not place all their armies");
				}
			}
			
			return DEFENDERS_DESTROYED;
		}
		
		return countries[attackingCC].getArmies() == 1 ? ATTACKERS_DESTROYED : ATTACK_INCONCLUSIVE;
	}
	
	protected void setupAttack(int attackingCC, int defendingCC, boolean attackUntilDead)
	{
		this.attackerLosses = 0;
		this.defenderLosses = 0;
		this.attackingCC = attackingCC;
		this.defendingCC = defendingCC;
		this.attackingPlayer = countries[attackingCC].getOwner();
		this.defendingPlayer = countries[defendingCC].getOwner();
		this.attackUntilDead = attackUntilDead;
		this.attackState = AttackState.EXECUTE;
	}
	
	protected void executeAttack()
	{
		int aArmies = countries[attackingCC].getArmies()-1;
		int dArmies = countries[defendingCC].getArmies();
		
		if(attackUntilDead)
		{
			AttackOutcome ao = BattleSim.simulateBattle(aArmies,dArmies);
			attackerLosses = ao.attackerLosses;
			defenderLosses = ao.defenderLosses;
		}
		else
		{
			int participatingAttackers = Math.min(aArmies,3);
			int participatingDefenders = Math.min(dArmies,2);
			attackerLosses = BattleSim.simulateIndividualBattle(participatingAttackers,participatingDefenders);
			defenderLosses = Math.min(participatingAttackers,participatingDefenders) - attackerLosses;
		}
		if(attackerLosses < 0 || defenderLosses < 0)
		{
			System.out.println("Hmmm");
		}
	}
	
	protected void finishAttack()
	{
		Country attackingCountry = countries[attackingCC];
		Country defendingCountry = countries[defendingCC];
		
		if(attackingCountry.getArmies()-attackerLosses < 1)
		{
			System.out.println("Hmm");
		}
		attackingCountry.setArmies(attackingCountry.getArmies()-attackerLosses,null);
		defendingCountry.setArmies(defendingCountry.getArmies()-defenderLosses,null);
		
		attackState = defendingCountry.getArmies() == 0 ? AttackState.INVADE : AttackState.START;
	}
	
	protected void setupInvasion()
	{
		Country attackingCountry = countries[attackingCC];
		Country defendingCountry = countries[defendingCC];
		int continent = defendingCountry.getContinent();
		
		attackingPlayer = attackingCountry.getOwner();
		defendingPlayer = defendingCountry.getOwner();
		
		playerCountries[attackingPlayer]++;
		countriesPlayerMissingInContinent[attackingPlayer][continent]--;
		playerCountries[defendingPlayer]--;
		countriesPlayerMissingInContinent[defendingPlayer][continent]++;
		
		defendingCountry.setOwner(attackingPlayer,null);
	}
	
	protected void executeInvasion(int armies)
	{
		Country attackingCountry = countries[attackingCC];
		Country defendingCountry = countries[defendingCC];
		
		int minInvadingArmies = Math.min(attackingCountry.getArmies()-1,3);
		int maxInvadingArmies = attackingCountry.getArmies()-1;
		int invadingArmies = Math.min(Math.max(armies, minInvadingArmies), maxInvadingArmies);
		
		defendingCountry.setArmies(invadingArmies,null);
		attackingCountry.setArmies(attackingCountry.getArmies() - invadingArmies,null);
	}
	
	protected void finishInvasion()
	{
		if(playerCountries[defendingPlayer] == 0)
		{
			playerDefeated(defendingPlayer,attackingPlayer);
		}
		
		hasInvadedACountry = true;
		attackState = AttackState.START;
		
		attackingCC = -1;
		defendingCC = -1;
		attackingPlayer = -1;
		defendingPlayer = -1;
	}
	
	protected void playerDefeated(int player, int conquerer)
	{
		remainingPlayers--;
		playerOutOnTurn[player] = simTurnCount;
		playerPositions[remainingPlayers] = player;
		
		if(transferCards)
		{
			playerCards.get(conquerer).addAll(playerCards.get(player));
			if(immediateCash && playerCards.get(conquerer).size() >= 5)
			{
				//attackState = AttackState.CASH;
			}
		}
		else
		{
			cardManager.simReturnToDeck(playerCards.get(player), coreID);
		}
		
		if(remainingPlayers == 1)
		{
			playerOutOnTurn[currentPlayer] = -1;
			playerPositions[0] = currentPlayer;
		}
	}
	
	protected void executeAttackCash()
	{
		numberOfPlaceableArmies = 0;
		List<Card> cardsList = playerCards.get(currentPlayer);
		simAgents[currentPlayer].cardsPhase(cardsList.toArray(new Card[cardsList.size()]));
		
		attackState = AttackState.PLACE;
	}

	protected void executeAttackPlacement()
	{
		simAgents[currentPlayer].placeArmies(numberOfPlaceableArmies);
		attackState = AttackState.START;
	}
	
	protected void tearDownAttackPhase()
	{
		if(hasInvadedACountry && useCards)
		{
			Card card = cardManager.simDeal(coreID);
			if(card != null)
			{
				playerCards.get(currentPlayer).add(card);
			}
		}
		
		currentPhase = Phase.FORTIFICATION;
	}

	/** Fortification phase **/
	
	protected void setupFortificationPhase()
	{
		for(Country c : countries)
		{
			c.setMoveableArmies(c.getOwner() == currentPlayer ? c.getArmies() : 0, null);
		}
	}

	public int fortifyArmies(int numberOfArmies, int sourceCC, int destinationCC)
	{
		Country sourceCountry = countries[sourceCC];
		Country destinationCountry = countries[destinationCC];
		int sourceArmies = sourceCountry.getArmies();
		int moveableArmies = countries[sourceCC].getMoveableArmies();
		
		if(currentPhase != Phase.FORTIFICATION)
		{
			throw new IllegalArgumentException("Player " + agentNames[currentPlayer] + 
												" (" + currentPlayer + ") cannot fortify during " +
												" phase " + currentPhase);
		}
		else if(sourceCountry.getOwner() != currentPlayer || destinationCountry.getOwner() != currentPlayer)
		{
			System.out.println(
				"Player " + agentNames[currentPlayer] + 
				" (" + currentPlayer + ") " + " does not own one of " + 
				sourceCountry.getName() + " (" + sourceCC + ") or " +
				destinationCountry.getName() + " (" + destinationCC + 
				") and therefore cannot transfer armies between the two."
			);
			return -1;
		}
		else if(numberOfArmies < 0)
		{
			System.out.println(
				"Player " + agentNames[currentPlayer] + " (" + currentPlayer + ")" + 
				" tried to fortify a negative number of armies " +
				" from " + sourceCountry.getName() + " (" + sourceCC + ")" +
				" to " + destinationCountry.getName() + " (" + destinationCC + ")."
			);
			return -1;
		}
		
		int armiesToFortify = Math.min(numberOfArmies, Math.min(moveableArmies, sourceArmies-1));	
		if(armiesToFortify == 0)
		{
			return 0;
		}

		sourceCountry.setArmies(sourceArmies - armiesToFortify, null);
		sourceCountry.setMoveableArmies(moveableArmies - armiesToFortify, null);
		destinationCountry.setArmies(destinationCountry.getArmies() + armiesToFortify,null);
		
		return 1;
	}
	
	protected void recalculateContinentBonuses()
	{
		double factor = Math.pow(1+continentIncrease/100.0 , Math.max(simStartTurn + simTurnCount - 1, 0));
		for(int i = 0; i < numberOfContinents; i++)
		{
			currentContinentBonuses[i] = (int) Math.floor(initialContinentBonuses[i]*factor);
		}
	}
	
	protected void tearDownFortificationPhase()
	{
		for(Country country : countries)
		{
			country.setMoveableArmies(0, null);
		}
		
		int nextPlayer = getNextPlayer();
		if(nextPlayer < currentPlayer)
		{
			simTurnCount++;
			recalculateContinentBonuses();
		}
		currentPlayer = nextPlayer;
		currentPhase = Phase.CARDS;
		
		checkForOverflow();
	}
	
	protected void checkForOverflow()
	{
		int [] armyTotals = new int[numberOfPlayers];
		for(Country c : countries)
		{
			armyTotals[c.getOwner()] += c.getArmies();
		}
		
		int totalArmies = 0;
		int maxArmies = 0;
		int maxPlayer = -1;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			totalArmies += armyTotals[i];
			if(armyTotals[i] > maxArmies)
			{
				maxPlayer = i;
				maxArmies = armyTotals[i];
			}
		}
		
		if(totalArmies > 100000)
		{
			for(int i = 0; i < numberOfPlayers; i++)
			{
				if(playerCountries[i] > 0 && i != maxPlayer)
				{
					playerDefeated(i, maxPlayer);
				}
			}
		}
	}
	
	/*************************/
	/** Information methods **/
	/*************************/
	
	public int getCurrentPlayer()
	{
		return currentPlayer;
	}
	
	public Phase getCurrentPhase()
	{
		return currentPhase;
	}
	
	public int getSimTurnCount()
	{
		return simTurnCount;
	}
	
	public int getTurnCount()
	{
		return simTurnCount + simStartTurn;
	}
	
	public int [] getPlayerOutOnTurn()
	{
		return playerOutOnTurn;
	}
	
	public int [] getPlayerPositions()
	{
		return playerPositions;
	}
	
	public Country [] getCountries()
	{
		return countries;
	}
	
	public int getNumberOfCountries()
	{
		return countries.length;
	}
	
	public int getNumberOfContinents()
	{
		return numberOfContinents;
	}
	
	public int getNumberOfPlayers()
	{
		return numberOfPlayers;
	}
	
	public int getNumberOfPlayersLeft()
	{
		return remainingPlayers;
	}
	
	public int getPlayerIncome(int player)
	{
		if(playerCountries[player] == 0) return 0;
		
		int income = Math.max(playerCountries[currentPlayer]/3,3);
		for(int i = 0; i < numberOfContinents; i++)
		{
			if(countriesPlayerMissingInContinent[currentPlayer][i] == 0)
			{
				income += getContinentBonus(i);
			}
		}
		
		return income;
	}
	
	public int getPlayerCards(int player)
	{
		return playerCards.get(player).size();
	}
	
	public int getNextCardSetValue()
	{
		return 5;
	}
	
	public int getNextPlayer()
	{
		int nextPlayer = currentPlayer;
		do
		{
			nextPlayer = (nextPlayer+1) % numberOfPlayers;
		}
		while(playerCountries[nextPlayer] == 0);
		return nextPlayer;
	}
	
	public int getContinentOwner(int continent)
	{
		for(int player = 0; player < numberOfPlayers; player++)
		{
			if(countriesPlayerMissingInContinent[player][continent] == 0)
			{
				return player;
			}
		}
		return -1;
	}
	
	public int getNumberOfPlaceableArmies()
	{
		if(currentPhase != Phase.PLACEMENT)
		{
			throw new IllegalStateException("Wrong phase to request placements");
		}
		return numberOfPlaceableArmies;
	}
	
	public int getAttackingCountry()
	{
		if(currentPhase != Phase.ATTACK || attackState != AttackState.INVADE)
		{
			throw new IllegalStateException("Wrong phase to request attacking country");
		}
		return attackingCC;
	}
	
	public int getDefendingCountry()
	{
		if(currentPhase != Phase.ATTACK || attackState != AttackState.INVADE)
		{
			throw new IllegalStateException("Wrong phase to request defending country");
		}
		return defendingCC;
	}
	
	public boolean canFortifyFrom(int cc, int player)
	{
		Country country = countries[cc];
		return 	country.getOwner() == player &&
				country.getMoveableArmies() > 0 &&
				country.getArmies() > 1; 
	}
	
	public boolean playerOwnsContinent(int player, int continent)
	{
		return countriesPlayerMissingInContinent[player][continent] == 0;
	}
	
	public int getContinentBonus(int continent)
	{
		return currentContinentBonuses[continent];
	}
	
	public boolean useCards()
	{
		return useCards;
	}
	
	public boolean transferCards() 
	{
		return transferCards;
	}
	
	public boolean immediateCash()
	{
		return immediateCash;
	}
	
	public int getContinentIncrease()
	{
		return continentIncrease;
	}
	
	public String getCardProgression()
	{
		return cardProgression;
	}
	
	public String getRealAgentName(int player)
	{
		return agentNames[player];
	}
	
	public String getSimAgentName(int player)
	{
		return simAgents[player].name();
	}
	
	public String getAgentPath()
	{
		throw new IllegalArgumentException("SimBoard does not implement the method 'getAgentPath'");
	}
	
	public boolean hasInvadedACountry()
	{
		return hasInvadedACountry;
	}
}
