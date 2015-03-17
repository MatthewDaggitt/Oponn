package com.mld46.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mld46.agent.moves.Attack;
import com.mld46.agent.moves.AttackOutcome;
import com.mld46.agent.moves.Move;
import com.mld46.agent.moves.NextPhase;
import com.mld46.sim.board.SimBoard.Phase;

public class SearchNode implements Comparable<SearchNode>
{
	// Exploration constant
	protected static final double C = 1;
	// Exploitation constant balancing winPercentage with length of games 
	protected static final double A = 0.05;
	// Randomness constant
	protected static final double EPSILON = 1e-10;
	
	public int visits = 0;
	public final int [] wins;
	public final int [] losses;
	public final double [] averageWinLength;
	public final double [] averageLossLength;
	
	public final int depth;
	public int currentPlayer;
	public final int numberOfPlayers;
	public List<SearchNode> children;
	public int numberOfChildren;
	public Move move;
	
	public boolean biased;
	public double bias;
	public int biasVisits;
	
	public final boolean restored;
	
	public SearchNode(int currentPlayer, int numberOfPlayers)
	{
		this.depth = 0;
		this.numberOfPlayers = numberOfPlayers;
		this.currentPlayer = currentPlayer;
		move = null;
		
		wins = new int[numberOfPlayers];
		losses = new int[numberOfPlayers];
		averageWinLength = new double[numberOfPlayers];
		averageLossLength = new double[numberOfPlayers];
		children = new ArrayList<SearchNode>();
		biased = false;
		
		restored = false;
	}
	
	public SearchNode(SearchNode parent, Move move, boolean restored)
	{
		this.depth = parent.depth+1;
		this.numberOfPlayers = parent.numberOfPlayers;
		this.move = move;
		this.currentPlayer = move.phase == Phase.NEXT_PHASE ? ((NextPhase)move).player : parent.currentPlayer;
		
		wins = new int[numberOfPlayers];
		losses = new int[numberOfPlayers];
		averageWinLength = new double[numberOfPlayers];
		averageLossLength = new double[numberOfPlayers];
		children = new ArrayList<SearchNode>();
		biased = false;
		
		this.restored = restored;
	}
	
	public void expand(List<Move> nextMoves)
	{
		/**
		if(nextMoves.size() == 1)
		{
			moves.add(nextMoves.get(0));
		}
		else 
		**/
		if(nextMoves.size() > 0)
		{
			for(Move m : nextMoves)
			{
				children.add(new SearchNode(this, m, false));
			}
			numberOfChildren = nextMoves.size();
		}
	}
	
	public SearchNode select()
	{
		if(children.isEmpty())
		{
			return null;
		}
		
		if(move instanceof Attack)
		{
			// Choose move according to the probability distribution of outcomes
			return selectAttackOutcome();
		}
		
		return selectHighestPriorityMove();
	}
	
	protected SearchNode selectAttackOutcome()
	{
		double currentScore;
		double bestScore = Integer.MIN_VALUE;
		SearchNode bestChild = null;
		for(SearchNode child : children)
		{
			currentScore = ((AttackOutcome)child.move).probability;
			if(visits != 0)
			{
				 currentScore -= child.visits/(double)visits;
			}
			
			if(currentScore > bestScore)
			{
				bestChild = child;
				bestScore = currentScore;
			}
		}
		return bestChild;
	}

	protected SearchNode selectHighestPriorityMove()
	{
		double logVisits = Math.log(visits+1);
		double maxAverageWinLength = Double.NEGATIVE_INFINITY;
		double minAverageWinLength = Double.POSITIVE_INFINITY;
		double maxAverageLossLength = Double.NEGATIVE_INFINITY;
		double minAverageLossLength = Double.POSITIVE_INFINITY;
		
		double averageLength;
		
		// Establish average win and loss lengths
		for(SearchNode child : children)
		{
			averageLength = child.averageWinLength[child.currentPlayer];
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
		
		double score;
		double bestScore = Double.NEGATIVE_INFINITY;
		SearchNode bestChild = null;
		
		double winPercentage;
		double winScore;
		double winLengthScore;
		double lossLengthScore;
		
		double a = A*Math.min(Math.max(numberOfChildren-2,0),100)/100.0;
				
		// Find the highest scoring child
		for(SearchNode child : children)
		{
			if(child.visits == 0)
			{
				// Then its never been visited before and hence prioritise it
				return child;
			}
			
			winPercentage = child.wins[child.currentPlayer]/(double)child.visits;
			winScore = (1-a)*winPercentage;
			
			// Now factor in the length of the games
			winLengthScore = 0;
			lossLengthScore = 0;
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
			
			// Exploration term with progressive bias
			score = C*Math.sqrt(logVisits/(child.visits+1));
			if(child.biased)
			{
				if(child.visits-1 == child.biasVisits)
				{
					child.biased = false;
				}
				else
				{
					int biasFactor = 1-((child.visits-1)/child.biasVisits);
					score = score*(1-biasFactor) + biasFactor*child.bias;
				}
			}
			
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
	
	public void updateStats(int [] playerOutOnTurn, int finalTurn)
	{	
		for(int i = 0; i < numberOfPlayers; i++)
		{
			if(playerOutOnTurn[i] == -1)
			{
				averageWinLength[i] = (finalTurn+wins[i]*averageWinLength[i])/(wins[i]+1);
				wins[i]++;
			}
			else
			{
				averageLossLength[i] = (finalTurn+losses[i]*averageLossLength[i])/(losses[i]+1);
				losses[i]++;
			}
		}
		visits++;
	}
	
	public void setBias(double bias, int biasVisits)
	{
		biased = true;
		this.bias = bias;
		this.biasVisits = biasVisits;
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
