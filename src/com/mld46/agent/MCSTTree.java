package com.mld46.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mld46.agent.Oponn.MoveSelectionPolicy;
import com.mld46.agent.moves.Attack;
import com.mld46.agent.moves.AttackOutcome;
import com.mld46.agent.moves.CountrySelection;
import com.mld46.agent.moves.Move;
import com.mld46.agent.moves.NextPhase;
import com.mld46.sim.board.ExploratorySimBoard;

public class MCSTTree
{
	private final int cores;
	private final int iterations;
	private final boolean retainRoot;
	private final boolean restoreTree;
	private final boolean attackAggressiveOptimisation;
	private final MoveSelectionPolicy moveSelectionPolicy;
	
	private final int agentID;
	private final int numberOfPlayers;
	
	private final ExploratorySimBoard [] simBoards;
	private SearchNode root;
	
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
		this.retainRoot = retainRoot;
		this.restoreTree = restoreTree;
		this.attackAggressiveOptimisation = attackAggressiveOptimisation;
		this.moveSelectionPolicy = moveSelectionPolicy;
		
		this.simBoards = simBoards;
	}
	
	public Move getCountrySelectionMove(BoardState boardState, int [] previousSelections)
	{
		updateCountrySelectionRoot(previousSelections);
		Move move = calculateMove(boardState);
		// Clear the root as other players will now play moves
		return move;
	}
	
	public Move getInitialPlacementMove(BoardState boardState)
	{
		// Clear root as we may be ending our initial placement turn.
		// Not much point in retaining it, even if we're not as there
		// will be loads of places to go initially.
		root = null;
		Move move = calculateMove(boardState);
		return move;
	}
	
	public Move getCardMove(BoardState boardState, boolean cardPhase)
	{
		if(cardPhase)
		{
			// Clear the root as we're at the start of a new turn
			root = null;
		}
		Move move = calculateMove(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getPlacementMove(BoardState boardState)
	{
		Move move = calculateMove(boardState);
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
		Move move = calculateMove(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getInvasionMove(BoardState boardState, AttackOutcome attackOutcome)
	{
		// As there is an invasion the AttackOutcome at the root of the tree must be removed.
		updateRoot(attackOutcome);
		Move move = calculateMove(boardState);
		updateRoot(move);
		return move;
	}
	
	public Move getFortificationMove(BoardState boardState)
	{
		Move move = calculateMove(boardState);
		updateRoot(move);
		return move;
	}
	
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
	
	private Move calculateMove(BoardState boardState)
	{
		Debug.output("Simulating " + iterations + " times",2);
		
		SearchNode currentNode, nextNode;
		List<SearchNode> path = new ArrayList<SearchNode>();
		boolean simOngoing;
		
		root = null;
		if(!retainRoot || root == null)
		{
			root = new SearchNode(agentID, numberOfPlayers);
		}
		else if(retainRoot && restoreTree)
		{
			restoreTree(boardState);
		}
		
		int loops = iterations/cores;
		for(int i = 0; i < loops; i++)
		{
			if(i == 1 && root.children.size() == 1)
			{
				// Then only one possible move so no need for further calculation
				break;
			}
			
			simBoards[0].setupState(boardState);
			
			currentNode = null;
			nextNode = root;
			simOngoing = true;
			path.clear();
			
			while(nextNode != null && simOngoing)
			{
				currentNode = nextNode;
				path.add(currentNode);
				nextNode = currentNode.select();
				
				if(nextNode != null)
				{
					simOngoing = simBoards[0].updateState(nextNode.move);
				}
			}
			
			if(!simOngoing)
			{
				path.add(nextNode);
			}
			else
			{
				List<Move> moves = simBoards[0].getPossibleMoves();
				currentNode.expand(moves);
				currentNode = currentNode.select();
				path.add(currentNode);
				
				runSimulations(boardState, path);
			}
			
			for(SearchNode node : path)
			{
				for(int j = 0; j < cores; j++)
				{
					node.updateStats(simBoards[j].getPlayerOutOnTurn(), simBoards[j].getSimTurnCount());
				}
			}
		}
		
		return selectMove();
	}
	
	private void runSimulations(final BoardState boardState, final List<SearchNode> path)
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
	
	private Move selectMove()
	{
		Move bestMove = null;
		
		switch(moveSelectionPolicy)
		{
			case MAX_CHILD:
				bestMove = selectMaxMove();
				break;
			
			case ROBUST_CHILD:
				bestMove = selectRobustMove();
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
					winString += (j == 0 ? "" : ", ") + String.format("%.3f", node.wins[j]/(double)sum);
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
	
	private Move selectRobustMove()
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
	
	private Move selectMaxMove()
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
		root.numberOfChildren = children.size();
	}
}
