package com.mld46.oponn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mld46.oponn.Oponn.MoveSelectionPolicy;
import com.mld46.oponn.moves.Attack;
import com.mld46.oponn.moves.AttackOutcome;
import com.mld46.oponn.moves.CountrySelection;
import com.mld46.oponn.moves.Move;
import com.mld46.oponn.moves.NextPhase;
import com.mld46.oponn.sim.boards.ExploratorySimBoard;
import com.mld46.oponn.sim.boards.SimBoard.Phase;

public class MCSTTree
{
	// Exploration constant
	private final double C = 1;
	// Exploitation constant balancing winPercentage with length of games 
	private final double A = 0.05;
	// Randomness constant
	private final double EPSILON = 1e-10;
	
	private final int cores;
	private final int iterations;
	private final int loops;
	
	private final boolean retainRoot;
	private final boolean restoreTree;
	private final boolean attackAggressiveOptimisation;
	private final MoveSelectionPolicy moveSelectionPolicy;
	
	private final int agentID;
	private final int numberOfPlayers;
	
	private final ExploratorySimBoard [] simBoards;
	private SearchNode root;
	
	/*****************/
	/** Constructor **/
	/*****************/
	
	public MCSTTree(ExploratorySimBoard [] simBoards,
					int agentID,
					int cores,
					int iterations,
					boolean retainRoot, 
					boolean restoreTree,
					boolean attackAggressiveOptimisation,
					MoveSelectionPolicy moveSelectionPolicy)
	{
		this.agentID = agentID;
		this.numberOfPlayers = simBoards[0].getNumberOfPlayers();
		
		this.cores = cores;
		this.iterations = iterations;
		this.loops = iterations/cores;
		
		this.retainRoot = retainRoot;
		this.restoreTree = restoreTree;
		this.attackAggressiveOptimisation = attackAggressiveOptimisation;
		this.moveSelectionPolicy = moveSelectionPolicy;
		
		this.simBoards = simBoards;
	}
	
	/******************/
	/** Move getters **/
	/******************/
	
	public Move getCountrySelectionMove(BoardState boardState, int [] previousSelections)
	{
		updateCountrySelectionRoot(previousSelections);
		Move move = runMCTS(boardState);
		// Clear the root as other players will now play moves
		return move;
	}
	
	public Move getInitialPlacementMove(BoardState boardState)
	{
		// Clear root as we may be ending our initial placement turn.
		// Not much point in retaining it, even if we're not as there
		// will be loads of places to go initially.
		root = null;
		Move move = runMCTS(boardState);
		return move;
	}
	
	public Move getCardMove(BoardState boardState, boolean cardPhase)
	{
		if(cardPhase)
		{
			// Clear the root as we're at the start of a new turn
			root = null;
		}
		Move move = runMCTS(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getPlacementMove(BoardState boardState)
	{
		Move move = runMCTS(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getAttackMove(BoardState boardState, AttackOutcome attackOutcome)
	{
		// Then there was not an invasion last turn, and the AttackOutcome at the root of tree must be removed
		if(attackOutcome != null)
		{
			updateRoot(attackOutcome);
		}
		Move move = runMCTS(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getInvasionMove(BoardState boardState, AttackOutcome attackOutcome)
	{
		// As there is an invasion the AttackOutcome at the root of the tree must be removed.
		updateRoot(attackOutcome);
		Move move = runMCTS(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getFortificationMove(BoardState boardState)
	{
		Move move = runMCTS(boardState);
		updateRoot(move);
		return move;
	}
	
	/********************/
	/** MCTS algorithm **/
	/********************/
	
	private Move runMCTS(BoardState boardState)
	{
		Debug.output("Simulating " + iterations + " times",2);
		
		root = new SearchNode(agentID, numberOfPlayers);
		/**
		root = null;
		if(!retainRoot || root == null)
		{
			root = new SearchNode(agentID, numberOfPlayers);
		}
		else if(retainRoot && restoreTree)
		{
			restoreTree(boardState);
		}
		**/
		
		for(int i = 0; i < loops && root.children.size() != 1; i++)
		{
			SearchNode currentNode = null;
			SearchNode nextNode = root;
			boolean simOngoing = true;
			List<SearchNode> path = new ArrayList<SearchNode>();
			
			// Selection phase
			path.add(root);
			simBoards[0].setupState(boardState);
			while(nextNode != null && simOngoing)
			{
				currentNode = nextNode;
				nextNode = selection(currentNode, path);
				
				if(nextNode != null)
				{
					simOngoing = simBoards[0].updateState(nextNode.move);
				}
			}
			
			if(simOngoing)
			{
				expansion(currentNode, simBoards[0].getPossibleMoves());
				simulation(boardState, path);
			}
			
			backpropagation(path);
		}
		
		return chooseMove();
	}
	
	// Selection
	
	private SearchNode selection(SearchNode node, List<SearchNode> path)
	{
		if(node.children.isEmpty())
		{
			return null;
		}
		
		SearchNode selected = node.move instanceof Attack ? stochasticSelection(node) : deterministicSelection(node);
		path.add(selected);
		return selected;
	}
	
	private SearchNode stochasticSelection(SearchNode node)
	{
		// Choose move according to the probability distribution of outcomes
		double currentScore;
		double bestScore = Integer.MIN_VALUE;
		SearchNode bestChild = null;
		for(SearchNode child : node.children)
		{
			currentScore = ((AttackOutcome)child.move).probability;
			if(node.visits != 0)
			{
				 currentScore -= child.visits/(double)node.visits;
			}
			
			if(currentScore > bestScore)
			{
				bestChild = child;
				bestScore = currentScore;
			}
		}
		return bestChild;
	}
	
	private SearchNode deterministicSelection(SearchNode node)
	{
		double logVisits = Math.log(node.visits+1);
		double maxAverageWinLength = Double.NEGATIVE_INFINITY;
		double minAverageWinLength = Double.POSITIVE_INFINITY;
		double maxAverageLossLength = Double.NEGATIVE_INFINITY;
		double minAverageLossLength = Double.POSITIVE_INFINITY;
		
		// Establish average win and loss lengths
		for(SearchNode child : node.children)
		{
			double averageLength = child.averageWinLength[child.currentPlayer];
			if(averageLength > maxAverageWinLength)
			{
				maxAverageWinLength = averageLength;
			}
			if(averageLength < minAverageWinLength)
			{
				minAverageWinLength = averageLength;
			}
		
			averageLength = child.averageLossLength[child.currentPlayer];
			if(averageLength > maxAverageLossLength)
			{
				maxAverageLossLength = averageLength;
			}
			if(averageLength < minAverageLossLength)
			{
				minAverageLossLength = averageLength;
			}
		}
		
		double deltaAverageWinLength = maxAverageWinLength - minAverageWinLength;
		double deltaAverageLossLength = maxAverageLossLength - minAverageLossLength;
		
		double bestScore = Double.NEGATIVE_INFINITY;
		SearchNode bestChild = null;
		
		double a = A*Math.min(Math.max(node.children.size()-2,0),100)/100.0;
				
		// Find the highest scoring child
		for(SearchNode child : node.children)
		{
			if(child.visits == 0)
			{
				// Then its never been visited before and hence prioritise it
				return child;
			}
			
			double winPercentage = child.wins[child.currentPlayer]/(double)child.visits;
			double winScore = (1-a)*winPercentage;
			
			// Now factor in the length of the games
			double winLengthScore = 0;
			double lossLengthScore = 0;
			if(winPercentage != 0.0)
			{
				// Its base contribution of the time taken to win. The higher the 
				// win percentage the more we care about about how long it takes to win
				winLengthScore = a*winPercentage;
				if(deltaAverageWinLength != 0.0)
				{
					winLengthScore *= (maxAverageWinLength-child.averageWinLength[child.currentPlayer])/deltaAverageWinLength;
				}
			}
			if(winPercentage != 1.0)
			{
				lossLengthScore = a*(1-winPercentage);
				if(deltaAverageLossLength != 0.0)
				{
					lossLengthScore *= (child.averageLossLength[child.currentPlayer]-minAverageLossLength)/deltaAverageLossLength;
				}
			}
			
			// Exploration term
			double score = C*Math.sqrt(logVisits/(child.visits+1));
			// Exploitation term
			score += winScore + winLengthScore + lossLengthScore;
			// Randomness
			score += Math.random()*EPSILON;
			
			if(score > bestScore)
			{
				bestScore = score;
				bestChild = child;
			}
		}
		
		return bestChild;
	}
	
	// Expansion 
	
	private void expansion(SearchNode node, List<Move> nextMoves)
	{
		if(nextMoves.isEmpty())
		{
			throw new IllegalArgumentException("Cannot expand a node with no moves");
		}
		
		for(Move move : nextMoves)
		{
			node.unvisitedChildren.add(new SearchNode(node, move, false));
		}
		node.children.addAll(node.unvisitedChildren);
	}
	
	// Simulation
	
	private void simulation(final BoardState boardState, final List<SearchNode> path)
	{
		class Simulation extends Thread
		{
			private final int id;
			public Simulation(int id)
			{
				this.id = id;
			}
			
			@Override
			public void run()
			{
				simBoards[id].setupState(boardState);
				for(int i = 1; i < path.size(); i++)
				{
					simBoards[id].updateState(path.get(i).move);
				}
				simBoards[id].simulate();
			}
		}
	
		Simulation [] sims = new Simulation[cores];
		for(int i = 0; i < cores; i++)
		{
			sims[i] = new Simulation(i);
			sims[i].run();
		}
		
		for(int i = 0; i < cores; i++)
		{
			try
			{
				sims[i].join();
			} 
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	// Backpropagation
	
	private void backpropagation(List<SearchNode> path)
	{
		for(int j = 0; j < cores; j++)
		{
			int [] playerOutOnTurn = simBoards[j].getPlayerOutOnTurn();
			int finalTurn = simBoards[j].getSimTurnCount();
			
			for(SearchNode node : path)
			{
				for(int i = 0; i < numberOfPlayers; i++)
				{
					if(playerOutOnTurn[i] == -1)
					{
						node.averageWinLength[i] = (finalTurn+node.wins[i]*node.averageWinLength[i])/(node.wins[i]+1);
						node.wins[i]++;
					}
					else
					{
						node.averageLossLength[i] = (finalTurn+node.losses[i]*node.averageLossLength[i])/(node.losses[i]+1);
						node.losses[i]++;
					}
				}
				node.visits++;
			}
		}
	}
	
	/**************************/
	/** Other helper methods **/
	/**************************/
	
	private void updateRoot(Move movePlayed)
	{
		if(!retainRoot || root.children.isEmpty())
		{
			return;
		}
		
		for(SearchNode child : root.children)
		{
			if(child.move.equals(movePlayed))
			{
				root = child;
				return;
			}
		}
		
		throw new IllegalStateException("The following move cannot be found amongst <root>'s children: " + movePlayed.toString());
	}
	
	private void updateCountrySelectionRoot(int [] previousSelections)
	{
		if(!retainRoot || root == null)
		{
			root = null;
			return;
		}
		
		for(int i = 0; i < numberOfPlayers; i++)
		{
			boolean found = false;
			
			for(SearchNode child : root.children)
			{
				if(((CountrySelection)child.move).cc == previousSelections[i])
				{
					root = child;
					found = true;
				}
			}
			
			if(!found)
			{
				root = null;
				return;
			}
		}
	}
	
	private Move chooseMove()
	{
		Move bestMove = null;
		
		switch(moveSelectionPolicy)
		{
			case MAX_CHILD:
				bestMove = chooseMaxMove();
				break;
			
			case ROBUST_CHILD:
				bestMove = chooseRobustMove();
				break;
		
			default:
				throw new IllegalStateException("Illegal final move selection policy specified!");
		}
		
		if(Debug.DEBUGGING)
		{
			DecimalFormat df = new DecimalFormat("#.##");
			Debug.output("Selecting move from:",2);
			
			for(int i = 0; i < root.children.size(); i++)
			{
				SearchNode node = root.children.get(i);
				int sum = 0;
				for(int v : node.wins)
				{
					sum += v;
				}
				
				String winString = "[";
				for(int j = 0; j < node.wins.length; j++)
				{
					winString += (j == 0 ? "" : ", ") + (j == agentID ? "#" : "") + String.format("%.3f", node.wins[j]/(double)sum);
				}
				winString += "]";
				
				Debug.output("Move " + i + " " + (node.restored ? "*" : "") + 
						"(" + winString + "," +
						" G=" + sum + 
						" WL=" + df.format(node.averageWinLength[agentID]) + 
						" LL=" + df.format(node.averageLossLength[agentID]) + 
						" : " + root.children.get(i).move.toString() + ")",3);
			}
			Debug.output("Selected: " + bestMove.toString(),2);
			Debug.output("Current (" + Arrays.toString(root.wins) + ")," +
					" WL=" + df.format(root.averageWinLength[agentID]) + 
					" LL=" + df.format(root.averageLossLength[agentID]),2);
			Debug.moveSelected();
		}
		
		return bestMove;
	}
	
	private Move chooseRobustMove()
	{
		int bestVisitCount = Integer.MIN_VALUE;
		double bestWinLength = Integer.MAX_VALUE;
		
		List<Move> bestMoves = new ArrayList<Move>();
		
		for(SearchNode child : root.children)
		{
			if(child.visits > bestVisitCount || (child.visits == bestVisitCount && child.averageWinLength[agentID] < bestWinLength))
			{
				bestVisitCount = child.visits;
				bestWinLength = child.averageWinLength[agentID];
				bestMoves.clear();
				bestMoves.add(child.move);
			}
			else if(child.visits == bestVisitCount && child.averageWinLength[agentID] == bestWinLength)
			{
				bestMoves.add(child.move);
			}
		}
		
		return bestMoves.get((int)(Math.random()*bestMoves.size()));
	}
	
	private Move chooseMaxMove()
	{
		double bestWinRate = Double.MIN_VALUE;
		double bestWinLength = Integer.MAX_VALUE;
		double winRate;
		
		List<Move> bestMoves = new ArrayList<Move>();
		
		for(SearchNode child : root.children)
		{
			winRate = ((double)child.wins[agentID])/child.visits;
			if(winRate > bestWinRate || (winRate == bestWinRate && child.averageWinLength[agentID] < bestWinLength))
			{
				bestWinRate = winRate;
				bestWinLength = child.averageWinLength[agentID];
				bestMoves.clear();
				bestMoves.add(child.move);
			}
			else if(winRate == bestWinRate && child.averageWinLength[agentID] == bestWinLength)
			{
				bestMoves.add(child.move);
			}
		}
		
		return bestMoves.get((int)(Math.random()*bestMoves.size()));
	}
	
	private void restoreTree(BoardState boardState)
	{
		simBoards[0].setupState(boardState);
		
		List<Move> moves = simBoards[0].getPossibleMoves();
		List<SearchNode> children = root.children;
		
		int childIndex = 0;
		for(Move move : moves)
		{
			if(childIndex == children.size() || !children.get(childIndex).move.equals(move))
			{
				children.add(childIndex,new SearchNode(root,move,true));
			}
			childIndex++;
		}
		
		if(attackAggressiveOptimisation && children.size() == moves.size()+1 && moves.get(0) instanceof Attack && children.get(children.size()-1).move instanceof NextPhase)
		{
			children.remove(children.size()-1);
		}
	}
}

class SearchNode implements Comparable<SearchNode>
{
	public int depth;
	public int currentPlayer;
	public int numberOfPlayers;
	public boolean restored;
	public Move move;
	
	public int visits = 0;
	public int [] wins;
	public int [] losses;
	public double [] averageWinLength;
	public double [] averageLossLength;
	
	public List<SearchNode> children = new ArrayList<SearchNode>();
	public List<SearchNode> unvisitedChildren = new ArrayList<SearchNode>();
	
	public SearchNode(int currentPlayer, int numberOfPlayers)
	{
		setup(numberOfPlayers, 0, currentPlayer, null, false);
	}
	
	public SearchNode(SearchNode parent, Move move, boolean restored)
	{
		int currentPlayer = move.phase == Phase.NEXT_PHASE ? ((NextPhase)move).player : parent.currentPlayer;
		setup(parent.numberOfPlayers, parent.depth+1, currentPlayer, move, restored);
	}
	
	private void setup(int numberOfPlayers, int depth, int currentPlayer, Move move, boolean restored)
	{
		this.numberOfPlayers = numberOfPlayers;
		this.depth = depth;
		this.currentPlayer = currentPlayer;
		this.move = move;
		this.restored = restored;
		
		wins = new int[numberOfPlayers];
		losses = new int[numberOfPlayers];
		averageWinLength = new double[numberOfPlayers];
		averageLossLength = new double[numberOfPlayers];
	}
	
	@Override
	public String toString()
	{
		return "SearchNode [visits=" + visits + ", " + (wins != null ? "estimatedValues=" + Arrays.toString(wins) + ", " : "") + "currentPlayer=" + currentPlayer + (move != null ? ", move=" + move : "") + "]";
	}

	@Override
	public int compareTo(SearchNode other)
	{
		return wins[currentPlayer] > other.wins[currentPlayer] ? 1 : (wins[currentPlayer] == other.wins[currentPlayer] ? 0 : -1);
	}
}

