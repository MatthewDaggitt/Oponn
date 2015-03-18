package com.mld46.agent;

import java.awt.Dimension;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Debug
{
	public final static boolean DEBUGGING = false;
	private final static boolean PAUSING = DEBUGGING && false;
	
	private static JTextArea area = new JTextArea();
	private static JFrame frame = new JFrame();
	private static Scanner scanner = new Scanner(System.in);
	static
	{
		frame.add(new JScrollPane(area));
		frame.setSize(new Dimension(600,300));
		area.setEditable(false);
	}
	
	public static void output(String s, int indent)
	{
		if(DEBUGGING)
		{
			String ind = "";
			for(int i = 0; i < indent; i++)
			{
				ind += "    ";
			}
			s = ind + s;
			
			frame.setVisible(true);
			area.append(s+"\n");
			frame.repaint();
			frame.revalidate();
			area.setCaretPosition(area.getDocument().getLength());
		}
	}
	
	public static void moveSelected()
	{
		if(PAUSING)
		{
			scanner.nextLine();
		}
	}
}
