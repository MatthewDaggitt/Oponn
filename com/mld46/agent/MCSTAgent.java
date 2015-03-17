package com.mld46.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mld46.agent.moves.Attack;
import com.mld46.agent.moves.AttackOutcome;
import com.mld46.agent.moves.CardCash;
import com.mld46.agent.moves.Fortification;
import com.mld46.agent.moves.InitialPlacement;
import com.mld46.agent.moves.Invasion;
import com.mld46.agent.moves.Move;
import com.mld46.agent.moves.NextPhase;
import com.mld46.agent.moves.Placement;
import com.mld46.sim.agent.SimAgent;
import com.mld46.sim.agent.SimLearner;
import com.mld46.sim.agent.SimRandom;
import com.mld46.sim.board.ExploratorySimBoard;
import com.mld46.sim.board.ModellingSimBoard;
import com.mld46.sim.board.SimBoard;
import com.mld46.sim.board.SimBoard.AttackState;
import com.mld46.sim.board.SimBoard.Phase;
import com.mld46.tournament.SimTournament;
import com.sillysoft.lux.Board;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;

public class MCSTAgent extends SimAgent
{
	// Settings
	
	public enum MoveSelectionPolicy
	{
		MAX_CHILD,
		ROBUST_CHILD,
		MAX_ROBUST_CHILD,
		SECURE_CHILD
	}
	
	private static final boolean RETAIN_ROOT = true;
	private static final boolean RESTORE_TREE = RETAIN_ROOT && true;
	
	private static final boolean PROGRESSIVE_BIAS = false;
	private static final int BIAS_VISITS = 10;
	
	private static final boolean PLACEMENT_PARTIAL_ORDER_OPTIMISATION = true; 
	private static final boolean ATTACK_AGGRESSIVE_OPTIMISATION = true; 
	private static final boolean ATTACK_REPEAT_MOVES_OPTIMISATION = true; 
	private static final boolean ATTACK_PARTIAL_ORDER_OPTIMISATION = false;
	private static final boolean INVASION_OPTIMISATION = true;
	private static final boolean FORTIFICATION_CONTINENT_OPTIMISATION = true; 
	private static final boolean FORTIFICATION_REPEAT_MOVES_OPTIMISATION = true; 
	private static final boolean FORTIFICATION_PARTIAL_ORDER_OPTIMISATION = false;
	
	private static final boolean OPPONENT_MODELLING = true;
	private static final int MODELLING_TURN_LIMIT = 2;
	
	private static final MoveSelectionPolicy MOVE_SELECTION_POLICY = MoveSelectionPolicy.ROBUST_CHILD;
	
	private static final int ITERATIONS = 1000;
	
	// State
	
	private BoardState boardState;
	private CardManager cardManager;
	private MCSTTree tree;
	
	private Attack lastAttack = null;
	
	private SimLearner [] simLearners;
	
	public void setPrefs(int newID, Board board)
	{
		super.setPrefs(newID, board);
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
		
		//runTournaments();
		
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
		
		simLearners = new SimLearner[numberOfPlayers];
		setup();
	}
	
	private void setup()
	{
		if(!boardState.useCards)
		{
			throw new IllegalArgumentException("MCST agent must use cards!");
		}
		
		cardManager = new CardManager(countries, numberOfPlayers);
		SimAgent [] simAgents = createSimAgents();
		ExploratorySimBoard simBoard = new ModellingSimBoard(countries, boardState, simAgents, cardManager, MODELLING_TURN_LIMIT);
		
		tree = new MCSTTree(
			simBoard,
			ID,
			ITERATIONS,
			RETAIN_ROOT,
			RESTORE_TREE,
			ATTACK_AGGRESSIVE_OPTIMISATION,
			PROGRESSIVE_BIAS,
			BIAS_VISITS,
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
		
		Debug.output("exiting country selection phase!",0);
		return -1;
	}

	public void placeInitialArmies(int numberOfArmies)
	{
		Debug.output("",0);
		Debug.output("Placing initial armies",1);
		
		List<Country> owned = new ArrayList<Country>();
		for(Country c : countries)
		{
			if(c.getOwner() == ID)
			{
				owned.add(c);
			}
		}
	
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
		Debug.output("",0);
		Debug.output("starting turn " + getTurnCount(),0);
		
		Debug.output("",0);
		Debug.output("entering cards phase",1);
		
		cardManager.updateCardsHeld(cards);
		
		boardState.numberOfPlaceableArmies = 0;
		boardState.turnCount = getTurnCount();
		boardState.currentPhase = Phase.CARDS;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boardState.playerNumberOfCards[i] = getPlayerCards(i);
		}
		
		Move move = tree.getCardMove(boardState);
			
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

			move = tree.getCardMove(boardState);
		}
		
		Debug.output("exiting cards phase",1);
	}

	public void placeArmies(int numberOfArmies)
	{
		Debug.output("",0);
		Debug.output("entering placing phase",1);
		
		boardState.currentPhase = Phase.PLACEMENT;
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
				outcome = new AttackOutcome(lastAttack.attackingArmies-countries[lastAttack.attackerCC].getArmies(),
												  lastAttack.defendingArmies-countries[lastAttack.defenderCC].getArmies(),
												  Double.NaN,false);
			}
			else
			{
				// In this case the getInvasionMove() call will already have taken care of it
				outcome = null;
			}
			
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

		AttackOutcome outcome = new AttackOutcome(0, lastAttack.defendingArmies, Double.NaN, true);
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

	public String youWon()
	{ 
		cardManager.resetForNewGame();
		System.out.println("Game over - won!");
		
		// For variety we store a bunch of answers and pick one at random to return.
		String[] answers = new String[] {"Random!"};
		Random rand = new Random();
		
		return answers[rand.nextInt(answers.length)];
	}
	
	/***********/
	/** Other **/
	/***********/
	
	public String name()
	{
		return "MCST Agent";
	}

	public float version()
	{
		return 1.0f;
	}
	
	public String description()
	{
		return "An agent using the power of Monte Carlo Search Trees";
	}
	
	public String message(String message, Object data)
	{
		if(message.equals("youLose") || message.equals("youWin"))
		{
			setup();
			Debug.output("Game over - " + message, 1);
		}
		return null;
	}
}
