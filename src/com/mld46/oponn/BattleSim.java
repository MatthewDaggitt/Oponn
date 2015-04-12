package com.mld46.oponn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.mld46.oponn.moves.AttackOutcome;

public class BattleSim
{
	private static final Random random = new Random(System.nanoTime());
	
	public static final float a1_d1_al0 = 0.4167f;
	public static final float a1_d1_al1 = 0.5833f;
	
	public static final float a1_d2_al0 = 0.2546f;
	public static final float a1_d2_al1 = 0.7454f;
	
	public static final float a2_d1_al0 = 0.5787f;
	public static final float a2_d1_al1 = 0.4213f;
	
	public static final float a2_d2_al0 = 0.2276f;
	public static final float a2_d2_al1 = 0.3241f;
	public static final float a2_d2_al2 = 0.4483f;
	
	public static final float a3_d1_al0 = 0.6597f;
	public static final float a3_d1_al1 = 0.3403f;
	
	public static final float a3_d2_al0 = 0.3717f;
	public static final float a3_d2_al1 = 0.3358f;
	public static final float a3_d2_al2 = 0.2926f;
	
	// array[i] = probability attackers lose less than or equal to i armies
	private final static double [][] outcomes = new double[][]
	{
		new double[]{a1_d1_al0,	1.0,					1.0},
		new double[]{a1_d2_al0,	1.0,					1.0},
		new double[]{a2_d1_al0,	1.0,					1.0},
		new double[]{a2_d2_al0,	a2_d2_al0 + a2_d2_al1,	1.0},
		new double[]{a3_d1_al0,	1.0,					1.0},
		new double[]{a3_d2_al0,	a3_d2_al0 + a3_d2_al1,	1.0},
	};
	
	private final static int LIMIT = 50;
	private static HashMap<Integer,AttackOutcome[]> attackOutcomes;
	static
	{
		attackOutcomes = new HashMap<Integer,AttackOutcome[]>();
		
		for(int a = 1; a < LIMIT; a++)
		{
			for(int d = 1; d < LIMIT; d++)
			{
				attackOutcomes.put(szudzikMapping(a, d), generateAttackOutcomes(a,d));
			}
		}
	}
	
	
	////////////////////
	// Public methods //
	////////////////////
	
	public static AttackOutcome simulateBattle(int attackers, int defenders)
	{
		int attackerLosses = 0;
		int defenderLosses = 0;
				
		if(attackers > 500 && defenders > 500)
		{
			int matchedAttackers = (int) (0.922*defenders);
			
			if(matchedAttackers > attackers)
			{
				attackerLosses = attackers;
				
				int variance = (int) (random.nextGaussian()*Math.sqrt(attackers));
				defenderLosses = (int)Math.min(variance + attackers/0.922, defenders-1); 
			}
			else
			{
				defenderLosses = defenders;
				
				int variance = (int) (random.nextGaussian()*Math.sqrt(defenders));
				attackerLosses = Math.min(variance + matchedAttackers, attackers-1);
			}
			if(attackerLosses < 0 || defenderLosses < 0)
			{
				System.out.println(attackers + " attackers (lost " + attackerLosses + ") against " + defenders + " defenders (lost " + defenderLosses + ")");
			}	
		}
		else
		{
			int remainingAttackers = attackers;
			int remainingDefenders = defenders;
			
			if(remainingAttackers > 100 || remainingDefenders > 100)
			{
				int participatingAttackers, participatingDefenders;
				int attackerLoss, defenderLoss;
				
				while((remainingAttackers > 100 || remainingDefenders > 100) && !(remainingAttackers == 0 || remainingDefenders == 0))
				{
					participatingAttackers = Math.min(remainingAttackers,3);
					participatingDefenders = Math.min(remainingDefenders,2);
					attackerLoss = simulateIndividualBattle(participatingAttackers,participatingDefenders);
					defenderLoss = Math.min(participatingAttackers,participatingDefenders) - attackerLoss;
					
					attackerLosses += attackerLoss;
					defenderLosses += defenderLoss;
					remainingAttackers -= attackerLoss;
					remainingDefenders -= defenderLoss;
				}
			}
			
			if(remainingAttackers != 0 && remainingDefenders != 0)
			{
				int key = szudzikMapping(remainingAttackers, remainingDefenders);
						
				AttackOutcome [] possibleResults = attackOutcomes.get(key);
				if(possibleResults == null)
				{
					possibleResults = generateAttackOutcomes(remainingAttackers,remainingDefenders);
					attackOutcomes.put(szudzikMapping(remainingAttackers, remainingDefenders), possibleResults);
				}
				
				double r = random.nextDouble();
				double total = 0;
				for(AttackOutcome ao : possibleResults)
				{
					total += ao.probability;
					if(r <= total)
					{
						attackerLosses += ao.attackerLosses;
						defenderLosses += ao.defenderLosses;
						break;
					}
				}
			}
		}
		
		return new AttackOutcome(attackerLosses,defenderLosses,Float.NaN,false);
	}
	
	/**
	 * @param attackers - number of attacking armies (1-3)
	 * @param defenders - number of defending armies (1-2)
	 * @return the number of armies that the attackers lost
	 */
	public static int simulateIndividualBattle(int attackers, int defenders)
	{
		if(attackers > 3 || defenders > 2 || attackers < 1 || defenders < 1)
		{
			throw new IllegalArgumentException("Error: " + attackers + ":" + defenders);
		}
		
		double r = random.nextDouble();
		double [] distribution = outcomes[(attackers-1)*2 + (defenders-1)];
		return (r <= distribution[0] ? 0 : (r <= distribution[1] ? 1 : 2));
	}
	
	public static boolean favourableForAttacker(int freeAttackers, int defenders)
	{
		return (freeAttackers >= 3) || (freeAttackers == 3 && defenders == 1);
	}
	
	public static boolean unfavourableForAttacker(int freeAttackers, int defenders)
	{
		return freeAttackers == 1 && defenders >= 2;
	}
	
	public static AttackOutcome [] getAttackOutcomes(int attackers, int defenders)
	{
		int key = szudzikMapping(attackers, defenders);
		AttackOutcome [] outcomes = attackOutcomes.get(key);
		if(outcomes == null)
		{
			outcomes = generateAttackOutcomes(attackers,defenders);
			attackOutcomes.put(key, outcomes);
		}
		return outcomes;
	}
	
	/////////////////////
	// Private methods //
	/////////////////////
	
	private static float [] generateProbabilities(int attackers, int defenders)
	{
		int rowLength = defenders+1;
		int colLength = attackers+1;
		
		float [] lastRow = new float[colLength*rowLength];
		lastRow[attackers*rowLength+defenders] = 1.0f;
		
		boolean allAbsorbed = false;
		
		float [] newRow;

		while(!allAbsorbed)
		{
			newRow = new float[colLength*rowLength];
			allAbsorbed = true;
			
			for(int a = 0; a < colLength; a++)
			{
				for(int d = 0; d < rowLength; d++)
				{
					if(lastRow[a*rowLength+d] != 0)
					{
						if(a == 0 || d == 0)
						{
							newRow[a*rowLength+d] += lastRow[a*rowLength+d];
						}
						else if(a == 1 && d == 1)
						{
							newRow[1*rowLength+0] += lastRow[1*rowLength+1]*a1_d1_al0;
							newRow[0*rowLength+1] += lastRow[1*rowLength+1]*a1_d1_al1;
							allAbsorbed = false;
						}
						else if(a == 2 && d == 1)
						{
							newRow[2*rowLength+0] += lastRow[2*rowLength+1]*a2_d1_al0;
							newRow[1*rowLength+1] += lastRow[2*rowLength+1]*a2_d1_al1;
							allAbsorbed = false;
						}
						else if(a == 1)
						{
							newRow[1*rowLength+(d-1)] += lastRow[1*rowLength+d]*a1_d2_al0;
							newRow[0*rowLength+(d-0)] += lastRow[1*rowLength+d]*a1_d2_al1;
							allAbsorbed = false;
						}
						else if(a == 2)
						{
							newRow[2*rowLength+(d-2)] += lastRow[2*rowLength+d]*a2_d2_al0;
							newRow[1*rowLength+(d-1)] += lastRow[2*rowLength+d]*a2_d2_al1;
							newRow[0*rowLength+(d-0)] += lastRow[2*rowLength+d]*a2_d2_al2;
							allAbsorbed = false;
						}
						else if(d == 1)
						{
							newRow[(a-0)*rowLength+(1-1)] += lastRow[a*rowLength+1]*a3_d1_al0;
							newRow[(a-1)*rowLength+(1-0)] += lastRow[a*rowLength+1]*a3_d1_al1;
							allAbsorbed = false;
						}
						else
						{
							newRow[(a-0)*rowLength+(d-2)] += lastRow[a*rowLength+d]*a3_d2_al0;
							newRow[(a-1)*rowLength+(d-1)] += lastRow[a*rowLength+d]*a3_d2_al1;
							newRow[(a-2)*rowLength+(d-0)] += lastRow[a*rowLength+d]*a3_d2_al2;
							allAbsorbed = false;
						}
					}
				}
			}
			lastRow = newRow;
		}
	
		return lastRow;
	}

	private static AttackOutcome [] generateAttackOutcomes(int a, int d)
	{
		List<AttackOutcome> attackOutcomes = new ArrayList<AttackOutcome>(a*d + a + d);
		
		float [] probabilities = generateProbabilities(a,d);
		float probability;
		
		for(int remA = 0; remA <= a; remA++)
		{
			for(int remD = 0; remD <= d; remD++)
			{
				probability = probabilities[remA*(d+1) + remD];
				
				if(probability != 0.0)
				{
					attackOutcomes.add(new AttackOutcome(a-remA, d-remD, probability, remD == 0));
				}
			}
		}
		Collections.sort(attackOutcomes, Collections.reverseOrder());
		
		return attackOutcomes.toArray(new AttackOutcome[attackOutcomes.size()]);
	}
	
	private static int szudzikMapping(int a, int b)
	{
		return a >= b ? (a * a + a + b) : (a + b * b);
	}
}
