package com.mld46.sim.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mld46.agent.BattleSim;
import com.mld46.agent.BoardState;
import com.mld46.agent.CardManager;
import com.mld46.agent.moves.*;
import com.mld46.sim.agent.SimAgent;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;

public class SimBoard
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
	
	protected final Country [] countries;
	private final Country [] originalCountries;
	private final int [] initialContinentBonuses;
	private final int [] currentContinentBonuses;
	
	protected static String [] realAgentNames;
	protected static String [] simAgentNames;
	
	protected final int numberOfCountries;
	protected final int numberOfContinents;
	protected final int numberOfPlayers;
	
	// Game settings
		
	private final boolean transferCards;
	private final boolean immediateCash;
	private final boolean useCards;
	private final int continentIncrease;
	private final String cardProgression;
	
	// Game state
	
	private final int [] playerCountries;
	protected final int [][] countriesPlayerMissingInContinent;
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
	
	// Initial placement phase
	protected int initialPlacementRound;
	protected int [] initialArmiesLeftToPlace;
	
	// Placement phase
	public int numberOfPlaceableArmies;
	
	// Attack phase
	private boolean hasInvadedACountry;
	protected Country attackingCountry;
	protected Country defendingCountry;
	protected int attackingPlayer;
	protected int defendingPlayer;
	protected boolean attackUntilDead;
	protected int attackerLosses;
	protected int defenderLosses;
	protected AttackState attackState;
	
	/*****************/
	/** Constructor **/
	/*****************/
	
	public SimBoard(Country [] originalCountries, BoardState boardState, SimAgent [] simAgents, CardManager cardManager, int coreID)
	{
		Move.countries = originalCountries;
		
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
		
		realAgentNames = boardState.agentNames;
		
		useCards = boardState.useCards;
		transferCards = boardState.transferCards;
		immediateCash = boardState.immediateCash;
		continentIncrease = boardState.continentIncrease;
		cardProgression = boardState.cardProgression;
		
		currentContinentBonuses = new int[numberOfContinents];
		playerOutOnTurn = new int[numberOfPlayers];
		playerPositions = new int[numberOfPlayers];
		playerCountries = new int [numberOfPlayers];
		countriesPlayerMissingInContinent = new int[numberOfPlayers][numberOfContinents];
		
		int startingArmies = calculateStartingArmies();
		initialArmiesLeftToPlace = new int[numberOfPlayers];
		for(int i = 0; i < numberOfPlayers; i++)
		{
			initialArmiesLeftToPlace[i] = startingArmies;
		}
		
		
		this.originalCountries = originalCountries;
		countries = new Country[numberOfCountries];
		Country originalCountry;
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			originalCountry = originalCountries[cc];
			countries[cc] = new Country(cc,originalCountry.getContinent(),null);
			countries[cc].setName(originalCountry.getName(), null);
		}
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
	        int [] neighbourCCs = originalCountries[cc].getAdjoiningCodeList();
	        for(int neighbourCC : neighbourCCs)
	        {
	            countries[cc].addToAdjoiningList(countries[neighbourCC], null);
	        }
		}
				
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
		
		int startingArmies = calculateStartingArmies();
		for(int i = 0; i < numberOfPlayers; i++)
		{
			initialArmiesLeftToPlace[i] = startingArmies - playerCountries[i];
		}
		
		currentPhase = Phase.INITIAL_PLACEMENT;
		setupInitialPlacementPhase();
	}
	
	public void setupState(BoardState boardState)
	{
		simStartTurn = boardState.turnCount;
		currentPlayer = boardState.currentPlayer;
		
		setupCountries();
		setupCardState(boardState.playerNumberOfCards);
		setupGeneralGameState();
		setupPhaseGameState(boardState);
		
		recalculateContinentBonuses();
	}
	
	protected void setupCountries()
	{
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			Country country = countries[cc];
			Country originalCountry = originalCountries[cc];
			
			int owner = originalCountry.getOwner();
			country.setOwner(owner,null);
			country.setArmies(originalCountry.getArmies(),null);
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
			
			playerCountries[owner]++;
			
			for(int player = 0; player < numberOfPlayers; player++)
			{
				if(player != owner)
				{
					countriesPlayerMissingInContinent[player][continent]++;
				}
			}
		}
		
		for(int player = 0; player < numberOfPlayers; player++)
		{
			if(playerCountries[player] > 0)
			{
				remainingPlayers++;
			}
		}
	}
	
	protected void setupPhaseGameState(BoardState boardState)
	{
		currentPhase = boardState.currentPhase;
		
		switch(currentPhase) 
		{
			case COUNTRY_SELECTION:
				throw new IllegalArgumentException("COUNTRY SELECTION TO DO!");
				
			case INITIAL_PLACEMENT:
				initialPlacementRound = boardState.initialPlacementRound;
				int startingArmies = calculateStartingArmies();
				for(int i = 0; i < numberOfPlayers; i++)
				{
					initialArmiesLeftToPlace[i] = Math.max(startingArmies - playerCountries[i] - 4*initialPlacementRound, 0);
				}
				
				setupInitialPlacementPhase();
				numberOfPlaceableArmies = boardState.numberOfPlaceableArmies;
				// TO-DO fix when find out how are initial armies are calculated
				initialArmiesLeftToPlace[currentPlayer] = Math.min(initialArmiesLeftToPlace[currentPlayer], numberOfPlaceableArmies);
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
				hasInvadedACountry = boardState.hasCapturedACountry;
				attackState = boardState.attackState;
				
				if(boardState.attackState == AttackState.INVADE)
				{
					attackingCountry = countries[boardState.attackingCountry];
					defendingCountry = countries[boardState.defendingCountry];
					attackingPlayer = attackingCountry.getOwner();
					defendingPlayer = boardState.defendingPlayer;
					
					if(playerCountries[defendingPlayer] == 0)
					{
						remainingPlayers++;
					}
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
				for(int i = 0; i < numberOfCountries; i++)
				{
					countries[i].setMoveableArmies(originalCountries[i].getMoveableArmies(), null);
				}
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
					int attackerCC = attackingCountry.getCode();
					int defenderCC = defendingCountry.getCode();
					
					executeInvasion(simAgents[currentPlayer].moveArmiesIn(attackerCC,defenderCC));
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
				
			default:
				throw new IllegalArgumentException("Cannot be in the NEXT_PHASE phase during simulation");
		}
		
		
		// Play out simulation
		while(remainingPlayers > 1)
		{
			switch(currentPhase)
			{
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
				
				default:
					throw new IllegalArgumentException("Cannot be in phase " + currentPhase + " whilst simulating");
			}
		}
		return playerOutOnTurn;
	}
	
	/** Initial placement phase **/
	
	private int calculateStartingArmies()
	{
	    double f1 = numberOfCountries/42.0;
	    f1 -= 1.0;
	    f1 /= 2.0;
	    f1 += 1.0;
	    
	    switch(numberOfPlayers)
	    {
	    	case 2: return (int) Math.round(f1 * 40.0);
	    	case 3: return (int) Math.round(f1 * 35.0);
	    	case 4: return (int) Math.round(f1 * 30.0);
	    	case 5: return (int) Math.round(f1 * 25.0);
	    	case 6: return (int) Math.round(f1 * 20.0);
	    	default: return -1;
	    }
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
			System.out.println("Error! Agent " + simAgents[currentPlayer].name() + " did not place all their armies");
		}
		
		int nextPlayer = getNextPlayer();
		if(nextPlayer == 0)
		{
			initialPlacementRound++;
			if(initialArmiesLeftToPlace[numberOfPlayers-1] == 0)
			{
				currentPhase = Phase.CARDS;
				for(int i = 0; i < numberOfPlayers; i++)
				{
					simAgents[i].resetClusterManager();
				}
				simTurnCount++;
			}
		}
		currentPlayer = nextPlayer;
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
			throw new IllegalArgumentException("Player " + realAgentNames[currentPlayer] + 
												" (" + currentPlayer + ") cannot place during " +
												" phase " + currentPhase);
		}
		else if(numberOfArmies > numberOfPlaceableArmies)
		{
			throw new IllegalArgumentException("Player " + realAgentNames[currentPlayer] +
												" does not have " + numberOfArmies + " to place, " +
												" only has " + numberOfPlaceableArmies);
		}
		else if(country.getOwner() != currentPlayer)
		{
			throw new IllegalArgumentException("Player " + realAgentNames[currentPlayer] + 
					" (" + currentPlayer + ") " + " does not own " + 
					country.getName() + " (" + cc +
					") and therefore cannot reinforce it.");
		}
		else if(numberOfArmies < 0)
		{
			throw new IllegalArgumentException("Player " + realAgentNames[currentPlayer] + 
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
		attackingCountry = null;
		defendingCountry = null;

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
		
		return attackingCountry.getArmies() == 1 ? ATTACKERS_DESTROYED : ATTACK_INCONCLUSIVE;
	}
	
	protected void setupAttack(int attackerCode, int defenderCode, boolean attackUntilDead)
	{
		Country attackingCountry = countries[attackerCode];
		Country defendingCountry = countries[defenderCode];
		
		attackerLosses = 0;
		defenderLosses = 0;
		this.attackingCountry = attackingCountry;
		this.defendingCountry = defendingCountry;
		this.attackUntilDead = attackUntilDead;
		attackState = AttackState.EXECUTE;
	}
	
	protected void executeAttack()
	{
		int aArmies = attackingCountry.getArmies()-1;
		int dArmies = defendingCountry.getArmies();
		
		if(!attackUntilDead)
		{
			int participatingAttackers = Math.min(aArmies-attackerLosses,3);
			int participatingDefenders = Math.min(dArmies-defenderLosses,2);
			attackerLosses = BattleSim.simulateIndividualBattle(participatingAttackers,participatingDefenders);
			defenderLosses = Math.min(participatingAttackers,participatingDefenders) - attackerLosses;
		}
		
		AttackOutcome ao = BattleSim.simulateBattle(aArmies,dArmies);
		attackerLosses = ao.attackerLosses;
		defenderLosses = ao.defenderLosses;
	}
	
	protected void finishAttack()
	{
		attackingCountry.setArmies(attackingCountry.getArmies()-attackerLosses,null);
		defendingCountry.setArmies(defendingCountry.getArmies()-defenderLosses,null);
		
		attackState = defendingCountry.getArmies() == 0 ? AttackState.INVADE : AttackState.START;
	}
	
	protected void setupInvasion()
	{
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
		int minInvadingArmies = Math.min(attackingCountry.getArmies()-1,3);
		int maxInvadingArmies = attackingCountry.getArmies()-1;
		int invadingArmies = Math.min(Math.max(armies, minInvadingArmies), maxInvadingArmies);
		
		defendingCountry.setArmies(invadingArmies,null);
		attackingCountry.setArmies(attackingCountry.getArmies() - invadingArmies,null);
	}
	
	protected void finishInvasion()
	{
		hasInvadedACountry = true;
		attackState = AttackState.START;
		
		if(playerCountries[defendingPlayer] == 0)
		{
			playerDefeated(defendingPlayer,attackingPlayer);
		}
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
			throw new IllegalArgumentException("Player " + realAgentNames[currentPlayer] + 
												" (" + currentPlayer + ") cannot fortify during " +
												" phase " + currentPhase);
		}
		else if(sourceCountry.getOwner() != currentPlayer || destinationCountry.getOwner() != currentPlayer)
		{
			System.out.println(
				"Player " + realAgentNames[currentPlayer] + 
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
				"Player " + realAgentNames[currentPlayer] + " (" + currentPlayer + ")" + 
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
		int nextPlayer = getNextPlayer();
		if(nextPlayer < currentPlayer)
		{
			simTurnCount++;
			recalculateContinentBonuses();
		}
		currentPlayer = nextPlayer;
		currentPhase = Phase.CARDS;
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
		return attackingCountry.getCode();
	}
	
	public int getDefendingCountry()
	{
		if(currentPhase != Phase.ATTACK || attackState != AttackState.INVADE)
		{
			throw new IllegalStateException("Wrong phase to request defending country");
		}
		return defendingCountry.getCode();
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
		return realAgentNames[player];
	}
	
	public String getSimAgentName(int player)
	{
		return simAgents[player].name();
	}
	
	public boolean hasInvadedACountry()
	{
		return hasInvadedACountry;
	}
}
