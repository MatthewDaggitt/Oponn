package com.mld46.tournament;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RealTournament extends Tournament
{
	@SuppressWarnings("serial")
	private final HashMap<String, Integer> menuIndices = new HashMap<String, Integer>() 
	{{
        put("angry", 			1);
        put("communist",		2);
        put("stinky", 			3);
        put("cluster", 			5);
        put("pixie", 			6);
        put("shaft",			7);
        put("yakool", 			8);
        put("boscoe",			10);
        put("evilpixie",		11);
        put("killbot",			12);
        put("quo",	 			13);
        put("modellingmcst",	15);
        put("normalmcst",		16);
        put("reaper", 			17);  
    }};
	
	private final int launchX = 450;//540;
	private final int launchY = 55;
	
	private final int firstSuperfastY = 105;
	private final int secondSuperfastY = 80;
	
	private final int luxX = 700;
	private final int luxY = 247;
	
	private final int playerNameStartX = 150 + luxX;
	private final int playerAgentStartX = 250 + luxX;
	private final int playerStartY = 273 + luxY;
	private final int playerHeight = 35;
	
	private final int menuStartY = -326;
	private final int menuItemHeight = 16;
	
	private final int startButtonX = 500 + luxX;
	private final int startButtonY = 413 + luxY;
	
	private final LuxLogReader reader;
	
	private Robot robot;
	private boolean firstTime = true;
	private boolean terminated = false;
	
	public RealTournament(String [] entrants, int playersPerGame, int gamesPerMatch)
	{
		super(entrants, playersPerGame, gamesPerMatch);
		
		try
		{
			robot = new Robot();
		}
		catch(AWTException e)
		{
			e.printStackTrace();
		}
		
		reader = new LuxLogReader(ROOT, players);
	}
	
	@Override
	public List<String> playMatch(List<String> agents)
	{
		List<String> log = null;
		
		robot.mouseMove(launchX,launchY);
		leftClick();
		
		robot.mouseMove(launchX, firstTime ? firstSuperfastY : secondSuperfastY);
		leftClick();
		
		Debug.front();
		
		wait(10000);
		
		if(terminated) 
		{
			wait(50000);
			terminated = false;
		}
		
		for(int i = 0; i < agents.size(); i++)
		{
			enterAgent(i, agents.get(i));
		}
		
		robot.mouseMove(startButtonX,startButtonY);
		leftClick();
		
		Debug.front();
		
		log = waitForGamesToFinish();
		
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Q);
		robot.keyRelease(KeyEvent.VK_Q);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		
		Debug.front();
		
		if(log.isEmpty()) 
		{
			wait(60000);
		}
		
		firstTime = false;
		
		wait(5000);
		return log;
	}
	
	private void enterAgent(int player, String agentName)
	{
		robot.mouseMove(playerNameStartX,playerStartY+playerHeight*player);
		
		leftClick();
		leftClick();
		
		wait(200);
		
		for(char c : agentName.toUpperCase().toCharArray())
		{
			robot.keyPress(c);
			robot.keyRelease(c);
		}
		
		int playerY = playerStartY + playerHeight*player;
		robot.mouseMove(playerAgentStartX, playerY); 
		leftClick();
		
		wait(200);
		
		int baseMenuItemHeight = menuStartY + menuItemHeight*menuIndices.get(agentName);
		int itemHeight = baseMenuItemHeight - (player == 0 ? 0 : (player == 1 ? 1 : 2))*menuItemHeight;
		
		robot.mouseMove(playerAgentStartX, playerY + itemHeight);
		leftClick();
	}
	
	public List<String> waitForGamesToFinish()
	{
		List<String> log = new ArrayList<String>();
		
		int i = 0;
		int lastLogSize = 0;
		while(log.size() < gamesPerMatch)
		{
			 log = reader.readLog();
			 System.out.println(log.size());
			 wait(1000);
			 i++;
			 
			 if(log.size() > lastLogSize)
			 {
				 i = 0;
				 lastLogSize = log.size();
			 }
			 
			 
			 if(i % 50 == 0)
			 {
				 robot.mouseMove((int)(Math.random()*1000), 300);
			 }
			 
			 if(i > 1800)
			 {
				 terminated = true;
				 System.out.println("Terminated at " + new Date().toString());
				 return new ArrayList<String>();
			 }
		}
		
		while(log.size() > gamesPerMatch)
		{
			log.remove(0);
		}
		
		return log;
	}

	public void leftClick()
	{
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		wait(50);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}
	
	public void wait(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}