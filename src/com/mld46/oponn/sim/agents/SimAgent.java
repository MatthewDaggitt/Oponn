package com.mld46.oponn.sim.agents;

import java.util.List;

import com.mld46.oponn.Debug;
import com.mld46.oponn.MapStats;
import com.mld46.oponn.sim.boards.SimulationBoard;
import com.sillysoft.lux.Board;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;
import com.sillysoft.lux.agent.LuxAgent;

public abstract class SimAgent implements LuxAgent
{
	private boolean simulating;
	private Board board;
	private SimulationBoard simBoard;
	
	protected int ID;
	protected Country [] countries;
	
	protected int numberOfContinents;
	protected int numberOfPlayers;
	protected int numberOfCountries;
	
	protected boolean transferCards;
	protected boolean immediateCash;
	protected boolean useCards;
	protected int continentIncrease;
	protected String cardProgression;
	
	protected MapHelper mapHelper;
	
	protected ClusterManager clusterManager;
	protected int clusterLastRequested = -1;
	
	public void setPrefs(int ID, Board board) {
		this.ID = ID;
		setBoard(board, null);
	}
	
	public void setSimPrefs(int ID, SimulationBoard simBoard) {
		this.ID = ID;
		setBoard(null, simBoard);
	}
	
	private void setBoard(Board board, SimulationBoard simBoard)
	{
		this.board = board;
		this.simBoard = simBoard;
		
		
		if(board == null && simBoard != null)
		{
			countries = simBoard.getCountries();
			
			this.numberOfContinents = simBoard.getNumberOfContinents();
			this.numberOfCountries = countries.length;
			this.numberOfPlayers = simBoard.getNumberOfPlayers();
			
			useCards = simBoard.useCards();
			transferCards = simBoard.transferCards();
			immediateCash = simBoard.immediateCash();
			continentIncrease = simBoard.getContinentIncrease();
			cardProgression = simBoard.getCardProgression();
			
			simulating = true;
		}
		else if(board != null && simBoard == null)
		{
			countries = board.getCountries();
			
			this.numberOfContinents = board.getNumberOfContinents();
			this.numberOfCountries = countries.length;
			this.numberOfPlayers = board.getNumberOfPlayers();
			
			useCards = board.useCards();
			transferCards = board.transferCards();
			immediateCash = board.immediateCash();
			continentIncrease = board.getContinentIncrease();
			cardProgression = board.getCardProgression();
			
			simulating = false;
		}
		else
		{
			throw new IllegalArgumentException("SimAgent cannot be both playing for real and simulating");
		}
		
		mapHelper = new MapHelper(this);
	}
	
	public void reset()
	{
		resetClusterManager();
	}
	
	protected boolean makeCardCash(Card card1, Card card2, Card card3)
	{
		if(simulating)
		{
			return simBoard.cashCards(card1,card2,card3);
		}
		else
		{
			return board.cashCards(card1,card2,card3);
		}
	}
	
	protected void makePlacement(int numberOfArmies, int country)
	{
		if(simulating)
		{
			simBoard.placeArmies(numberOfArmies,country);
		}
		else
		{
			board.placeArmies(numberOfArmies,country);
		}
	}
	
	protected int makeAttack(int attackerCode, int defenderCode, boolean tillTheDeath)
	{
		int result = simulating ? simBoard.attack(attackerCode,defenderCode,tillTheDeath) : board.attack(attackerCode,defenderCode,tillTheDeath);
		
		if(result == 7)
		{
			updateClusterManager(countries[defenderCode]);
		}
		return result;
	}
	
	protected int makeFortification(int numberOfArmies, int sourceCountry, int destinationCountry)
	{
		if(simulating)
		{
			return simBoard.fortifyArmies(numberOfArmies,sourceCountry,destinationCountry);
		}
		else
		{
			return board.fortifyArmies(numberOfArmies,sourceCountry,destinationCountry);
		}
	}

	protected int getPlayerIncome(int player)
	{
		return simulating ? simBoard.getPlayerIncome(player) : board.getPlayerIncome(player);
	}
	
	protected int getPlayerCards(int player)
	{
		return simulating ? simBoard.getPlayerCards(player) : board.getPlayerCards(player);
	}
	
	protected int getContinentBonus(int continent)
	{
		return simulating ? simBoard.getContinentBonus(continent) : board.getContinentBonus(continent);
	}

	protected int getNextCardSetValue()
	{
		return simulating ? simBoard.getNextCardSetValue() : board.getNextCardSetValue();
	}
	
	protected boolean hasInvadedACountry()
	{
		return simulating ? simBoard.hasInvadedACountry() : board.tookOverACountry();
	}
	
	protected String getRealAgentName(int player)
	{
		return simulating ? simBoard.getSimAgentName(player) : board.getAgentName(player);
	}
	
	protected int getNumberOfPlayersLeft()
	{
		return simulating ? simBoard.getNumberOfPlayersLeft() : board.getNumberOfPlayersLeft();
	}
	
	protected int getTurnCount()
	{
		return simulating ? simBoard.getTurnCount() : board.getTurnCount();
	}
		
	protected void setBackerPrefs(SimAgent simAgent)
	{
		if(simulating)
		{
			simAgent.setSimPrefs(ID, simBoard);
		}
		else
		{
			simAgent.setPrefs(ID, board);
		}
	}

	/*********************/
	/** Cluster methods **/
	/*********************/
	
	protected void updateClusterManager(Country conquered)
	{
		if(clusterManager != null)
		{
			if(clusterLastRequested != getTurnCount())
			{
				clusterManager = null;
			}
			else
			{
				clusterManager.countryConquered(conquered);
			}
		}
	}
	
	protected List<Country> getClusterBorders(int cc)
	{
		int turnCount = getTurnCount();
		if(clusterManager == null || clusterLastRequested != turnCount) 
		{
			clusterManager = new ClusterManager(countries, ID);
		}
		clusterLastRequested = turnCount;
		
		return clusterManager.getClusterBorders(cc);
		
		/**
		CountryIterator borders = new ClusterBorderIterator(countries[cc]);
		List<Country> cluster = new ArrayList<Country>();
		while (borders.hasNext())
		{
			cluster.add(borders.next());
		}
		return cluster;**/
	}
	
	public void resetClusterManager()
	{
		clusterManager = null;
		clusterLastRequested = -1;
	}
	
	public static SimAgent getSimAgent(String name, boolean amMCST)
	{
		SimAgent simAgent;
		
		name = name.toLowerCase();
		if(name.equals("cluster"))
		{
			simAgent = new SimCluster();
		}
		else if(name.equals("shaft"))
		{
			simAgent = new SimShaft();
		}
		else if(name.equals("angry"))
		{
			simAgent = new SimAngry();
		}
		else if(name.equals("boscoe"))
		{
			simAgent = new SimBoscoe();
		}
		else if(name.equals("communist"))
		{
			simAgent = new SimCommunist();
		}
		else if(name.equals("evilpixie"))
		{
			simAgent = new SimEvilPixie();
		}
		else if(name.equals("killbot"))
		{
			simAgent = new SimKillbot();
		}
		else if(name.equals("pixie"))
		{
			simAgent = new SimPixie();
		}
		else if(name.equals("quo"))
		{
			simAgent = new SimQuo();
		}
		else if(name.equals("stinky"))
		{
			simAgent = new SimStinky();
		}
		else if(name.equals("yakool"))
		{
			simAgent = new SimYakool();
		}
		else
		{
			simAgent = new SimRandom();
		}
		
		if(amMCST)
		{
			Debug.output("Using " + simAgent.name(),1);
		}
		return simAgent;
	}
}
