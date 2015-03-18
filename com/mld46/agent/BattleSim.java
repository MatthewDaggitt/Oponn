package com.mld46.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.mld46.agent.moves.AttackOutcome;

public class BattleSim
{
	private static final Random random = new Random(System.nanoTime());
	
	public static final double a1_d1_al0 = 0.4167;
	public static final double a1_d1_al1 = 0.5833;
	
	public static final double a1_d2_al0 = 0.2546;
	public static final double a1_d2_al1 = 0.7454;
	
	public static final double a2_d1_al0 = 0.5787;
	public static final double a2_d1_al1 = 0.4213;
	
	public static final double a2_d2_al0 = 0.2276;
	public static final double a2_d2_al1 = 0.3241;
	public static final double a2_d2_al2 = 0.4483;
	
	public static final double a3_d1_al0 = 0.6597;
	public static final double a3_d1_al1 = 0.3403;
	
	public static final double a3_d2_al0 = 0.3717;
	public static final double a3_d2_al1 = 0.3358;
	public static final double a3_d2_al2 = 0.2926;
	
	// array[i] = probability attackers lose less than or equal to i armies
	private static double [][] outcomes = new double[6][3];
	
	private final static int LIMIT = 50;
	private static HashMap<Integer,List<AttackOutcome>> attackOutcomes;
	
	static
	{
		outcomes[0] = new double[]{a1_d1_al0,	1.0,					1.0};
		outcomes[1] = new double[]{a1_d2_al0,	1.0,					1.0};
		outcomes[2] = new double[]{a2_d1_al0,	1.0,					1.0};
		outcomes[3] = new double[]{a2_d2_al0,	a2_d2_al0 + a2_d2_al1,	1.0};
		outcomes[4] = new double[]{a3_d1_al0,	1.0,					1.0};
		outcomes[5] = new double[]{a3_d2_al0,	a3_d2_al0 + a3_d2_al1,	1.0};
	}
	
	static
	{
		attackOutcomes = new HashMap<Integer,List<AttackOutcome>>();
		
		for(int a = 1; a < LIMIT; a++)
		{
			for(int d = 1; d < LIMIT; d++)
			{
				addBattleToHashmap(a,d);
			}
		}
	}
	
	/**
	 * @param attackingArmies - number of attacking armies (1-3)
	 * @param defendingArmies - number of defending armies (1-2)
	 * @return the number of armies that the attackers lost
	 */
	public static int simulateIndividualBattle(int attackingArmies, int defendingArmies)
	{
		double [] distribution = null;
		
		if(attackingArmies == 3 && defendingArmies == 2)
		{
			distribution = outcomes[5];
		}
		else if(attackingArmies == 3 && defendingArmies == 1)
		{
			distribution = outcomes[4];
		}
		else if(attackingArmies == 2 && defendingArmies == 2)
		{
			distribution = outcomes[3];
		}
		else if(attackingArmies == 2 && defendingArmies == 1)
		{
			distribution = outcomes[2];
		}
		else if(attackingArmies == 1 && defendingArmies == 2)
		{
			distribution = outcomes[1];
		}
		else if(attackingArmies == 1 && defendingArmies == 1)
		{
			distribution = outcomes[0];
		}
		else
		{
			throw new IllegalArgumentException("Error: " + attackingArmies + ":" + defendingArmies);
		}
		
		double r = random.nextDouble();
		return r <= distribution[0] ? 0 : (r <= distribution[1] ? 1 : 2);
	}
	
	public static AttackOutcome simulateBattle(int attackers, int defenders)
	{
		int attackerLosses = 0;
		int defenderLosses = 0;
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
			int key = remainingAttackers >= remainingDefenders ? (remainingAttackers * remainingAttackers + remainingAttackers + remainingDefenders) : (remainingAttackers + remainingDefenders * remainingDefenders);
			
			List<AttackOutcome> possibleResults = attackOutcomes.get(key);
			
			if(possibleResults == null)
			{
				addBattleToHashmap(remainingAttackers,remainingDefenders);
				possibleResults = attackOutcomes.get(key);
			}
			
			double r = random.nextDouble();
			
			for(AttackOutcome ao : possibleResults)
			{
				if(r <= ao.probability)
				{
					attackerLosses += ao.attackerLosses;
					defenderLosses += ao.defenderLosses;
					break;
				}
			}
		}
		
		return new AttackOutcome(attackerLosses,defenderLosses,Double.NaN,false);
	}
	
	public static double [] generateProbabilities(int attackers, int defenders)
	{
		int rowLength = defenders+1;
		int colLength = attackers+1;
		
		double [] lastRow = new double[colLength*rowLength];
		lastRow[attackers*rowLength+defenders] = 1.0;
		
		boolean allAbsorbed = false;
		
		double [] newRow;

		while(!allAbsorbed)
		{
			newRow = new double[colLength*rowLength];
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

	public static boolean favourableForAttacker(int freeAttackers, int defenders)
	{
		return (freeAttackers >= 3) || (freeAttackers == 3 && defenders == 1);
	}
	
	public static boolean unfavourableForAttacker(int freeAttackers, int defenders)
	{
		return freeAttackers == 1 && defenders >= 2;
	}
	
	public static void addBattleToHashmap(int a, int d)
	{
		List<AttackOutcome> singularAttackOutcomes;
		List<AttackOutcome> cumulativeAttackOutcomes;
		
		double [] probabilities;
		double probability;
		
		probabilities = generateProbabilities(a,d);
		
		singularAttackOutcomes = new ArrayList<AttackOutcome>();
		
		for(int a2 = 0; a2 <= a; a2++)
		{
			for(int d2 = 0; d2 <= d; d2++)
			{
				probability = probabilities[a2*(d+1) + d2];
				
				if(probability != 0.0)
				{
					singularAttackOutcomes.add(new AttackOutcome(a-a2,d-d2,probability,false));
				}
			}
		}
		
		Collections.sort(singularAttackOutcomes);
		
		cumulativeAttackOutcomes = new ArrayList<AttackOutcome>(singularAttackOutcomes.size());
		probability = 0;
		for(AttackOutcome ao : singularAttackOutcomes)
		{
			probability += ao.probability;
			cumulativeAttackOutcomes.add(new AttackOutcome(ao.attackerLosses,ao.defenderLosses,probability,false));
		}
		
		if(attackOutcomes.put(a >= d ? (a * a + a + d) : (a + d * d),cumulativeAttackOutcomes) != null)
		{
			throw new IllegalStateException("Szudzik mapping is not bijective!");
		}
	}
	
	public static void main(String [] args)
	{
		int attackers, defenders;
		int attackerLosses, defenderLosses;
		int participatingAttackers, participatingDefenders;
		int attackerLoss;
		
		int iterations = 1000000;
		
		double [] oldProb;
		double [] newProb;
		AttackOutcome ao;
		
		for(int a = 0; a < 10; a++)
		{
			attackers = 1;//random.nextInt(200);
			defenders = 20;//random.nextInt(200);
			
			oldProb = new double[attackers+defenders+2];
			newProb = new double[attackers+defenders+2];
			
			for(int j = 0; j < iterations; j++)
			{
				attackerLosses = 0;
				defenderLosses = 0;
				while(attackerLosses != attackers && defenderLosses != defenders)
				{
					participatingAttackers = Math.min(attackers-attackerLosses,3);
					participatingDefenders = Math.min(defenders-defenderLosses,2);
					attackerLoss = simulateIndividualBattle(participatingAttackers,participatingDefenders);
					attackerLosses += attackerLoss;
					defenderLosses += Math.min(participatingAttackers,participatingDefenders) - attackerLoss;
				}
				
				ao = simulateBattle(attackers,defenders);
				
				oldProb[attackers == attackerLosses ? (attackers+1) + (defenders-defenderLosses) : attackers-attackerLosses]++;
				newProb[attackers == ao.attackerLosses ? (attackers+1) + (defenders-ao.defenderLosses) : attackers-ao.attackerLosses]++;
			}
			
			double error = 0;
			for(int j = 0; j < attackers+defenders+2; j++)
			{
				error += ((oldProb[j]-newProb[j])/iterations)*((oldProb[j]-newProb[j])/iterations);
			}
			System.out.println(attackers + "," + defenders + ": " + error);
		}
	}
}
