package com.mld46.oponn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Random;

import com.mld46.oponn.moves.Attack;
import com.mld46.oponn.moves.AttackOutcome;
import com.mld46.oponn.moves.CardCash;
import com.mld46.oponn.moves.CountrySelection;
import com.mld46.oponn.moves.Fortification;
import com.mld46.oponn.moves.InitialPlacement;
import com.mld46.oponn.moves.Invasion;
import com.mld46.oponn.moves.Move;
import com.mld46.oponn.moves.NextPhase;
import com.mld46.oponn.moves.Placement;
import com.mld46.oponn.sim.agents.SimAgent;
import com.mld46.oponn.sim.agents.SimRandom;
import com.mld46.oponn.sim.boards.ExploratorySimBoard;
import com.mld46.oponn.sim.boards.ModellingSimBoard;
import com.mld46.oponn.sim.boards.SimBoard;
import com.mld46.oponn.sim.boards.SimBoard.AttackState;
import com.mld46.oponn.sim.boards.SimBoard.Phase;
import com.sillysoft.lux.Board;
import com.sillysoft.lux.Card;

public class Oponn extends SimAgent
{
	/**************/
	/** Settings **/
	/**************/
	
	public enum MoveSelectionPolicy
	{
		MAX_CHILD,
		ROBUST_CHILD,
		MAX_ROBUST_CHILD,
		SECURE_CHILD
	}
	
	private final boolean RETAIN_ROOT = true;
	private final boolean RESTORE_TREE = RETAIN_ROOT && true;
	
	private final boolean PLACEMENT_PARTIAL_ORDER_OPTIMISATION = true; 
	private final boolean ATTACK_AGGRESSIVE_OPTIMISATION = true; 
	private final boolean ATTACK_REPEAT_MOVES_OPTIMISATION = true; 
	private final boolean ATTACK_PARTIAL_ORDER_OPTIMISATION = false;
	private final boolean INVASION_OPTIMISATION = true;
	private final boolean FORTIFICATION_CONTINENT_OPTIMISATION = true; 
	private final boolean FORTIFICATION_REPEAT_MOVES_OPTIMISATION = true; 
	private final boolean FORTIFICATION_PARTIAL_ORDER_OPTIMISATION = false;
	
	private boolean OPPONENT_MODELLING;
	private final int MODELLING_TURN_LIMIT = 2;
	
	private final MoveSelectionPolicy MOVE_SELECTION_POLICY = MoveSelectionPolicy.ROBUST_CHILD;
	
	private int ITERATIONS;
	private int CORES;
	
	/***********/
	/** State **/
	/***********/
	
	private BoardState boardState;
	private CardManager cardManager;
	private MCSTTree tree;
	
	private Attack lastAttack = null;
	private boolean [] playersEliminated;
	private int [] countryOwners;
	
	/********************/
	/** Initialisation **/
	/********************/
	
	public void setPrefs(int newID, Board board)
	{
		super.setPrefs(newID, board);
		
		readSettings();
		
		boardState = new BoardState(
			board,
			ID,
			PLACEMENT_PARTIAL_ORDER_OPTIMISATION,
			ATTACK_AGGRESSIVE_OPTIMISATION,
			ATTACK_REPEAT_MOVES_OPTIMISATION,
			ATTACK_PARTIAL_ORDER_OPTIMISATION,
			INVASION_OPTIMISATION,
			FORTIFICATION_CONTINENT_OPTIMISATION,
			FORTIFICATION_REPEAT_MOVES_OPTIMISATION,
			FORTIFICATION_PARTIAL_ORDER_OPTIMISATION
		);
		
		Debug.output("Agent created (ID = " + newID + ")",0);
		setup();
	}
	
	@Override
	public void setSimPrefs(int newID, SimBoard simBoard)
	{
		super.setSimPrefs(newID, simBoard);
		
		boardState = new BoardState(
			simBoard,
			ID,
			PLACEMENT_PARTIAL_ORDER_OPTIMISATION,
			ATTACK_AGGRESSIVE_OPTIMISATION,
			ATTACK_REPEAT_MOVES_OPTIMISATION,
			ATTACK_PARTIAL_ORDER_OPTIMISATION,
			INVASION_OPTIMISATION,
			FORTIFICATION_CONTINENT_OPTIMISATION,
			FORTIFICATION_REPEAT_MOVES_OPTIMISATION,
			FORTIFICATION_PARTIAL_ORDER_OPTIMISATION
		);
		
		setup();
	}
	
	private void setup()
	{
		cardManager = new CardManager(countries, numberOfPlayers, ID, CORES, cardProgression, boardState.transferCards, boardState.immediateCash);
		
		SimAgent [] simAgents = createSimAgents();
		
		ExploratorySimBoard [] simBoards = new ExploratorySimBoard[CORES];
		for(int i = 0; i < CORES; i++)
		{
			simBoards[i] = new ModellingSimBoard(countries, boardState, simAgents, cardManager, i, MODELLING_TURN_LIMIT);
		}
		
		playersEliminated = new boolean [numberOfPlayers];
		countryOwners = new int[numberOfCountries];
		Arrays.fill(countryOwners, -1);
		
		tree = new MCSTTree(
			simBoards,
			ID,
			CORES,
			ITERATIONS,
			RETAIN_ROOT,
			RESTORE_TREE,
			ATTACK_AGGRESSIVE_OPTIMISATION,
			MOVE_SELECTION_POLICY
		);
	}
	
	private SimAgent [] createSimAgents()
	{
		SimAgent [] simAgents = new SimAgent[numberOfPlayers];
		for(int i = 0; i < numberOfPlayers; i++)
		{
			if(OPPONENT_MODELLING && i != ID)
			{
				simAgents[i] = SimAgent.getSimAgent(getRealAgentName(i), true);
			}
			else 
			{
				simAgents[i] = new SimRandom();
				Debug.output("Using SimRandom",1);
			}
		}
		return simAgents;
	}
	
	/*********************/
	/** Playing methods **/
	/*********************/
	
	public int pickCountry()
	{
		Debug.output("",0);
		Debug.output("country selection phase",1);
		
		boardState.turnCount = getTurnCount();
		boardState.currentPhase = Phase.COUNTRY_SELECTION;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boardState.playerNumberOfCards[i] = 0;
		}
		
		int [] selections;
		if(RETAIN_ROOT)
		{
			selections = new int[numberOfPlayers];
			for(int cc = 0; cc < numberOfCountries; cc++)
			{
				int owner = countries[cc].getOwner();
				if(owner != countryOwners[cc])
				{
					selections[owner] = cc;
					countryOwners[cc] = owner;
				}
			}
		}
		CountrySelection move = (CountrySelection) tree.getCountrySelectionMove(boardState, selections);
		
		Debug.output("exiting country selection phase!",0);
		return move.cc;
	}

	public void placeInitialArmies(int numberOfArmies)
	{
		Debug.output("",0);
		Debug.output("Placing initial armies",1);

		boardState.turnCount = getTurnCount();
		boardState.currentPhase = Phase.INITIAL_PLACEMENT;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boardState.playerNumberOfCards[i] = 0;
		}
		
		while(numberOfArmies != 0)
		{
			boardState.numberOfPlaceableArmies = numberOfArmies;
			
			InitialPlacement placement = (InitialPlacement) tree.getInitialPlacementMove(boardState);
			
			makePlacement(placement.armies, placement.cc);
			numberOfArmies -= placement.armies;
		}

		boardState.initialPlacementRound += 1;
		
		Debug.output("Exiting initial placement",1);
	}

	public void cardsPhase(Card [] cards)
	{
		cardManager.updateCardsHeld(cards);
		boolean cardPhase = lastAttack == null;
		
		if(cardPhase)
		{
			// Normal card phase
			
			Debug.output("",0);
			Debug.output("starting turn " + getTurnCount(),0);
			
			Debug.output("",0);
			Debug.output("entering cards phase",1);
			
			int [] cardNumbers = new int[numberOfPlayers];
			boolean [] playersEliminatedLastTurn = new boolean[numberOfPlayers];
			for(int i = 0; i < numberOfPlayers; i++)
			{
				cardNumbers[i] = getPlayerCards(i);
				if((getPlayerIncome(i) == 0) != playersEliminated[i])
				{
					playersEliminatedLastTurn[i] = true;
					playersEliminated[i] = true;
				}
			}
			cardManager.updateCardProgressionPosition(getNextCardSetValue(), cardNumbers, playersEliminatedLastTurn);
			
			boardState.currentPhase = Phase.CARDS;
			boardState.turnCount = getTurnCount();
		}
		else
		{
			// We've come here after stealing cards from an eliminated player
			boardState.currentPhase = Phase.ATTACK;
			boardState.attackState = AttackState.CASH;
		}
		
		boardState.numberOfPlaceableArmies = 0;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boardState.playerNumberOfCards[i] = getPlayerCards(i);
		}
		
		Move move = tree.getCardMove(boardState, cardPhase);
		while(move instanceof CardCash)
		{
			CardCash cc = (CardCash) move;
			boolean cashSucceeded = makeCardCash(cc.card1,cc.card2,cc.card3);
			
			if(!cashSucceeded)
			{
				String errorMessage = "Invalid cards!";
				errorMessage += " Tried to cash " + cc.card1 + ", " + cc.card2 + ", " + cc.card3;
				errorMessage += " but only has ";
				for(int i = 0 ; i < cards.length; i++)
				{
					errorMessage += cards[i] + ", ";
				}
				throw new IllegalArgumentException(errorMessage);
			}
			
			cardManager.cashCards(cc.card1,cc.card2,cc.card3);
			boardState.playerNumberOfCards[ID] -= 3;
			boardState.numberOfPlaceableArmies += getNextCardSetValue();
			
			move = tree.getCardMove(boardState, false);
		}
		
		Debug.output("exiting cards phase",1);
	}

	public void placeArmies(int numberOfArmies)
	{
		if(lastAttack == null)
		{
			Debug.output("",0);
			Debug.output("entering placing phase",1);
			
			boardState.currentPhase = Phase.PLACEMENT;
		}
		else
		{
			// We've come here after stealing cards from an eliminated player
			boardState.currentPhase = Phase.ATTACK;
			boardState.attackState = AttackState.PLACE;
		}
		
		boardState.numberOfPlaceableArmies = numberOfArmies;
		
		Move move = tree.getPlacementMove(boardState);
		
		while(move instanceof Placement)
		{
			Placement placement = (Placement)move;
			makePlacement(placement.armies,placement.cc);
			boardState.numberOfPlaceableArmies -= placement.armies;
			
			move = tree.getPlacementMove(boardState);
		}
		
		Debug.output("exiting placing phase",1);
	}

	public void attackPhase()
	{
		Debug.output("",0);
		Debug.output("entering attack phase",1);
		
		boardState.currentPhase = Phase.ATTACK;
		boardState.hasCapturedACountry = false;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boardState.playerNumberOfCards[i] = getPlayerCards(i);
		}
		boardState.attackState = AttackState.START;
		
		Move move = tree.getAttackMove(boardState, null);
		AttackOutcome outcome;
		
		while(move instanceof Attack)
		{
			lastAttack = (Attack) move;
			boardState.defendingPlayer = countries[lastAttack.defenderCC].getOwner();
			int result = makeAttack(lastAttack.attackerCC, lastAttack.defenderCC, lastAttack.attackUntilDead);
			if(RETAIN_ROOT && result != SimBoard.DEFENDERS_DESTROYED)
			{
				// Then there is an outstanding attackOutcome sitting at the top of the tree which needs to be removed.
				outcome = new AttackOutcome((short)(lastAttack.attackingArmies-countries[lastAttack.attackerCC].getArmies()),
											(short)(lastAttack.defendingArmies-countries[lastAttack.defenderCC].getArmies()),
											Float.NaN,
											false);
			}
			else
			{
				// In this case the getInvasionMove() call will already have taken care of it
				outcome = null;
			}
			
			if(result == SimBoard.DEFENDERS_DESTROYED && getPlayerIncome(boardState.defendingPlayer) == 0)
			{
				playersEliminated[boardState.defendingPlayer] = true;
			}
			
			lastAttack = null;
			boardState.attackState = AttackState.START;
			move = tree.getAttackMove(boardState, outcome);
		}
		
		
		
		Debug.output("exiting attack phase",1);
	}
	
	public int moveArmiesIn(int invadingCountry, int invadedCountry)
	{
		boardState.attackState = AttackState.INVADE;
		boardState.defendingCountry = invadedCountry;
		boardState.attackingCountry = invadingCountry;
		boardState.hasCapturedACountry = true;

		AttackOutcome outcome = new AttackOutcome((short)0, lastAttack.defendingArmies, Float.NaN, true);
		Invasion invasion = (Invasion) tree.getInvasionMove(boardState, outcome);
		
		return invasion.armies;
	}

	public void fortifyPhase()
	{
		Debug.output("entering fortification phase",1);
		
		boardState.currentPhase = Phase.FORTIFICATION;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boardState.playerNumberOfCards[i] = getPlayerCards(i);
		}
		
		Move move = tree.getFortificationMove(boardState);
		
		while(!(move instanceof NextPhase))
		{
			Fortification f = (Fortification) move;
			makeFortification(f.armies,f.sourceCC,f.destinationCC);
			
			move = tree.getFortificationMove(boardState);
		}
		
		Debug.output("exiting fortification phase",1);
	}

	/***********/
	/** Other **/
	/***********/
	
	public String name()
	{
		return "Oponn";
	}

	public float version()
	{
		return 1.0f;
	}
	
	public String description()
	{
		return "An agent using the power of Monte Carlo Search Trees";
	}
	
	public String youWon()
	{ 
		System.out.println("Game over - won!");
		
		setup();
		
		// For variety we store a bunch of answers and pick one at random to return.
		String[] answers = new String[] {"Random!"};
		Random rand = new Random();
		
		return answers[rand.nextInt(answers.length)];
	}
	
	public String message(String message, Object data)
	{
		if(message.equals("youLose") || message.equals("youWin"))
		{
			setup();
			Debug.output("Game over - " + message, 1);
		}
		
		System.out.println("Message: " + message);
		return null;
	}
	
	public void readSettings()
	{
		ITERATIONS = 1000;
		OPPONENT_MODELLING = true;
		Debug.DEBUGGING = false;
		CORES = Runtime.getRuntime().availableProcessors();
		
		BufferedReader br;
		try
		{
			Debug.output("Settings detected", 0);
			
			br = new BufferedReader(new FileReader(new File("Oponn.txt")));
			
			String line = br.readLine();
			while(line != null)
			{	
				String value = null;
				if(line.contains("="))
				{
					value = line.substring(line.indexOf("=")+1).replace(" ", "");
				}
				
				if(value != null)
				{
					if(line.startsWith("lux_agent_modelling"))
					{
						OPPONENT_MODELLING = !value.equals("false");
						Debug.output("lux_agent_modelling=" + (OPPONENT_MODELLING ? "true" : "false"), 0);
					}
					else if(line.startsWith("iterations"))
					{
						try
						{
							ITERATIONS = Integer.valueOf(value);
						}
						catch(NumberFormatException nfe){}
						
						Debug.output("debug=" + (Debug.DEBUGGING ? "true" : "false"), 0);
					}
					else if(line.startsWith("cores"))
					{
						if(!value.equals("n"))
						{
							try
							{
								int cores = Integer.valueOf(value);
								CORES = Math.max(1,Math.min(cores, CORES));
							}
							catch(NumberFormatException nfe){}
						}
						Debug.output("cores=" + CORES, 0);
					}
					else if(line.startsWith("debug"))
					{
						Debug.DEBUGGING = value.equals("true");
						Debug.output("iterations=" + ITERATIONS,0);
					}
				}
				
				line = br.readLine();
			}
			br.close();
		} 
		catch(Exception e)
		{
			System.out.println("Error reading settings file");
			e.printStackTrace();
		}
	}
}
