package com.mld46.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mld46.agent.MCSTAgent.MoveSelectionPolicy;
import com.mld46.agent.moves.Attack;
import com.mld46.agent.moves.AttackOutcome;
import com.mld46.agent.moves.Move;
import com.mld46.agent.moves.NextPhase;
import com.mld46.sim.board.ExploratorySimBoard;

public class MCSTTree
{
	private final int iterations;
	private final boolean retainRoot;
	private final boolean restoreTree;
	private final boolean attackAggressiveOptimisation;
	private final boolean progressiveBias;
	private final int biasVisits;
	private final MoveSelectionPolicy moveSelectionPolicy;
	
	private final int agentID;
	private final int numberOfPlayers;
	
	private final ExploratorySimBoard simBoard;
	private SearchNode root;
	
	public MCSTTree(ExploratorySimBoard simBoard,
					int agentID,
					int iterations,
					boolean retainRoot, 
					boolean restoreTree,
					boolean attackAggressiveOptimisation,
					boolean progressiveBias,
					int biasVisits,
					MoveSelectionPolicy moveSelectionPolicy)
	{
		this.agentID = agentID;
		this.numberOfPlayers = simBoard.getNumberOfPlayers();
		
		this.iterations = iterations;
		this.retainRoot = retainRoot;
		this.restoreTree = restoreTree;
		this.attackAggressiveOptimisation = attackAggressiveOptimisation;
		this.progressiveBias = progressiveBias;
		this.biasVisits = biasVisits;
		this.moveSelectionPolicy = moveSelectionPolicy;
		
		this.simBoard = simBoard;
	}
	
	public Move getCountrySelectionMove(BoardState boardState)
	{
		Move move = calculateMove(boardState);
		// Clear the root as other players will now play moves
		root = null;
		return move;
	}
	
	public Move getInitialPlacementMove(BoardState boardState)
	{
		Move move = calculateMove(boardState);
		// Clear root as we may be ending our initial placement turn.
		// Not much point in retaining it, even if we're not as there
		// will be loads of places to go initially.
		root = null;
		return move;
	}
	
	public Move getCardMove(BoardState boardState)
	{
		// Clear the root as we're at the start of a new turn
		root = null;
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
		// Then there was not an invasion last turn, and the AttackOutcome at the root fo tree must be removed
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
	
	private Move calculateMove(BoardState boardState)
	{
		Debug.output("Simulating " + iterations + " times",2);
		
		long time = System.currentTimeMillis();
		
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

		for(int i = 0; i < iterations; i++)
		{
			if(i == 1 && root.children.size() == 1)
			{
				// Then only one possible move so no need for further calculation
				break;
			}
			
			simBoard.setupState(boardState);
			
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
					if(progressiveBias && currentNode.numberOfChildren == currentNode.visits && currentNode.numberOfChildren != 1)
					{
						ProgressiveBias.addProgressiveBias(currentNode, simBoard, biasVisits);
					}
					
					simOngoing = simBoard.updateState(nextNode.move);
				}
			}
			
			if(!simOngoing)
			{
				path.add(nextNode);
			}
			else
			{
				List<Move> moves = simBoard.getPossibleMoves();
				currentNode.expand(moves);
				currentNode = currentNode.select();
				path.add(currentNode);
				
				simBoard.updateState(currentNode.move);
				simBoard.simulate();
				/**
				try
				{
					
				}
				catch(IllegalArgumentException e)
				{
					simBoard.setupState(boardState);
					
					for(SearchNode node : path) 
					{
						if(node.move != null)
						{
							simBoard.updateState(node.move);
						}
					}
					simBoard.updateState(nextNode.move);
					simBoard.simulate();
				}**/
			}
			
			int [] turnsPlayersOutOn = simBoard.getPlayerOutOnTurn();
			int totalNumberOfTurns = simBoard.getSimTurnCount();
			for(SearchNode node : path)
			{
				node.updateStats(turnsPlayersOutOn, totalNumberOfTurns);
			}
		}
		
		System.out.println(System.currentTimeMillis() - time);
		return selectMove();
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
				Debug.output("Move " + i + " " + (root.children.get(i).restored ? "*" : "") + 
						"(" + Arrays.toString(root.children.get(i).wins) + ")," +
						" WL=" + df.format(root.children.get(i).averageWinLength[agentID]) + 
						" LL=" + df.format(root.children.get(i).averageLossLength[agentID]) + 
						" : " + root.children.get(i).move.toString(),3);
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
		simBoard.setupState(boardState);
		
		List<Move> moves = simBoard.getPossibleMoves();
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
	
	/**
	private int expandTree(BoardState bs, List<Move> path)
	{
		int size = 0;
		
		SimBoard board = new SimBoard(countries,bs);
		board.setupState(ID,bs,simAgents,cardManager);
		
		for(Move move : path)
		{
			board.updateState(move);
		}
		
		List<Move> moves = board.getAllPossibleMoves();
		int i = 0;
		int numberOfMoves = moves.size();
		List<Move> newPath;
		for(Move move : moves)
		{
			i++;
			size++;
			if(!(move instanceof NextPhase))
			{
				newPath = new ArrayList<Move>(path);
				newPath.add(move);
				
				size += expandTree(bs,newPath);
			}
			
			if(path.size() == 0)
			{
				Debug.output(i + "/" + numberOfMoves, 4);
			}
		}

		return size;
	}**/
}
