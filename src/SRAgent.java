import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.Random;

/**
 * Simple-Reflex Agent to navigate the Maeden simulation environment
 * Designed to complete worlds with next "best" action without keeping world state
 * 
 * @author Patrick Dixon
 * @author Austen Herrick
 * @author Londynn Metten
 *
 */
public class SRAgent {

	private static final int MAEDENPORT = 7237;
	private static final int CURRENT_COL = 2;// 2
	private static final int CURRENT_ROW = 5;// 1
	private static final double STARTING_ENERGY = 2000;
	
	protected GridClient gc;

	/**
	 * construct agent object and GridClient object with hostname and port number
	 * passed to constructor
	 * @param h host name
	 * @param p socket port to connect to maeden
	 */
	public SRAgent(String h, int p) {
		gc = new GridClient(h, p);
	}

	/**
	 * runs the agent. loops through "turns" where the agent retrieves senses as "state" 
	 * of agent, uses that state to decide on action and performs that action.
	 * ends when maeden environment closes connection
	 */
	private void run() {
		String[] percept;
		State state;
		String action;

		while (true) {
			state = getState();
			action = getAction(state);
			performAction(action);
		}
	}

	/**
	 * sends the corresponding action that the agent has decided to perform
	 * @param action action to perform received as a single character represented
	 * as a String. actions are "flrbhgu"
	 */
	private void performAction(String action) {
		
		// used for DEBUGGIN
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
		// *****
		
		switch (action.charAt(0)) {
		case 'f':
			// move forward
			gc.effectorSend("f");
			System.out.println("forward");
			break;
		case 'b':
			// turn around instead of going back to allow usage of the extra visual
			// cells the visual field displays in front of the agent
			
			gc.effectorSend("l"); // debug ********************
			
			System.out.println("turned left");
			break;
		case 'r':
			// more right
			gc.effectorSend("r");
			
			System.out.println("right"); //debug *********************88
			
			break;
		case 'l':
			// move left
			gc.effectorSend("l");
			
			System.out.println("left"); //debug *********************88
			
			break;
		case 'h':
			// cheese on current space
			// grab and use cheese
			gc.effectorSend("g +");
			
			System.out.println("found cheese"); //debug**********************
			
			gc.effectorSend("u +");
			break;
		case 'g':
			// hammer on current space
			// grab hammer
			gc.effectorSend("g");
			
			System.out.println("grabbing hammer");//debug ********************888
			
			break;
		case 'u':
			// use weapon
			// grab and use cheese
			gc.effectorSend("u");
			
			System.out.println("using weapon");//debug ******************************8
			
			break;
		default:

		}
	}

	/**
	 * decides on the "best" action to take based on current situation. treats every move
	 * as if it is the first move the agent has made
	 * 
	 * when energy reaches a certain point, agent go into "panic mode" and hugs the wall
	 * either left or right depending on if position to agent. this is used to determine if
	 * agent is "stuck"
	 * 
	 * actions are "decided" based on priority of the action: 
	 * get items -> check for boulders/doors -> 2 panic periods -> use item -> check for walls 
	 * @param state sensory information of the agent
	 * @return the action the agent decides to perform
	 */
	private String getAction(State state) {
		String action;
		
		//debug**************
		System.out.println("The energy is " + state.energy + "vs" + (STARTING_ENERGY * 0.9));
		
		//check for items
		action = grabItem(state.visField, state.groundContents);
		//debug***********************
		System.out.println("grabItem returned " + action);
		System.out.println("Inventory contains " + state.inv);
		//******************************8
		
		if (action == null && state.inv.contains("T")) {
			action = checkBoulder(state.visField);
		}
		if (action == null && state.inv.contains("K")) {
			action = checkDoor(state.visField);
			//debug***************************
			System.out.println("check door returned " + action);
		}
		//panic mode
		//enter if energy is between 90% and 79%
		if (action == null && state.energy < (STARTING_ENERGY * (0.9)) && state.energy > (STARTING_ENERGY * (0.79))) {
			action = hugWall(state.visField);
		}
		//second panic mode
		//enter if energy between 55% and 40%
		if (action == null && state.energy < (STARTING_ENERGY * 0.55) && state.energy > (STARTING_ENERGY * .4)) {
			action = hugWall(state.visField);
		}
		//use item if in inventory
		if (action == null) {
			action = useWeapon(state.visField, state.inv);
		}
		if (action == null) {
			if (wallDetecter(state.visField) && (state.inv).contains("T")) {
				action = boulderDetecter(state.visField);
			}
		}
		//checks for walls in agents path
		if (action == null) {
			action = checkWall(state.visField, state.heading);
		}
		return action;
	}

	/**
	 * retrieves the senses from the environment and creates a State object with them
	 * @return State object holding the senses information
	 */
	private State getState() {
		//get sensory info form SensoryPacket
		SensoryPacket sp = gc.getSensoryPacket();
		String[] senses = sp.getRawSenseData();

		String heading = senses[0];
		String inv = senses[1];

		//lets SensoryPacket process visual field and convert it to String[][]
		sp.processRetinalField(senses[2]);
		String[][] visField = processVisual(sp.getVisualArray());
		
		String groundContents = senses[3];
		String energy = senses[5];
		String lastAction = senses[6];

		return new State(heading, inv, groundContents, energy, lastAction, visField);
	}

	/**
	 * converts the visual field received from the environment to a String[][]
	 * to be more manageable
	 * @param visField visual field received from the environment
	 * @return visual field as a new structure
	 */
	private String[][] processVisual(ArrayList<ArrayList<Vector<Character>>> visField) {
		String[][] newVisField = new String[7][5];

		for (int i = 0; i < visField.size(); i++) {
			for (int j = 0; j < visField.get(i).size(); j++) {
				for (Character item : visField.get(i).get(j)) {
					if (item.charValue() != '"') {
						newVisField[i][j] = "" + item.charValue();
					}
				}
				
				System.out.println(newVisField[i][j] + " " + i + " " + j);// debugging******
			}
			
			System.out.println();// debugging****************************
		}

		return newVisField;
	}

	/**
	 * 
	 * @param visField
	 * @param smell
	 * @return
	 */
	private String checkWall(String[][] visField, String smell) {
		// declare local variables
		Random r = new Random();
		int rand = 0;
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String twoRight = visField[CURRENT_ROW][CURRENT_COL + 2]; 
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String leftRear = visField[CURRENT_ROW + 1][CURRENT_COL - 1];
		String rightRear = visField[CURRENT_ROW + 1][CURRENT_COL + 1]; 
		String leftFront = visField[CURRENT_ROW - 1][CURRENT_COL - 1]; 
		String rightFront = visField[CURRENT_ROW - 1][CURRENT_COL + 1]; 
		String rightRightFront = visField[CURRENT_ROW - 1][CURRENT_COL + 2]; 
		String heading = smell;
		boolean wholeInWall = false;
		System.out.println(smell);
		if (smell.equals("f")) {
			System.out.println("Forward space contains " + forward);
			if (forward != null) {
				if (forward.equals("*") || forward.equals("@") || forward.equals("#") || forward.equals("Q")) {
					for (int i = 0; i < 2; i++) {// search four visible spots
						if ((visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]) == null) {
							System.out.println((CURRENT_COL - (i + 1)) % 5);
							System.out.println("Moving left from wall");
							wholeInWall = true;
							heading = "l"; // hole is left
							i = 2; // signal end of loop we’ve found nearest hole in wall
						} else if ((visField[CURRENT_ROW - 1][(CURRENT_COL + (i + 1)) % 5]) == null) {
							System.out.println("Moving right from wall");
							wholeInWall = true;
							heading = "r"; // hole is right
							i = 2; // signal end of loop we’ve found nearest
									// hole in wall
						}
					}
					if (wholeInWall == false) {// long wall encountered
						System.out.println("There is no visible hole in wall");
						rand = r.nextInt(2) * 2 - 1;
						if (rand == -1) {
							System.out.println("Random generator chose left");
							heading = "l";
						} 
						else if (rand == 1) {
							System.out.println("Random generator chose right");
							heading = "r";
						} 
						else {
							System.out.println("Random generator broken" + rand);
						}
					}
				}
			}
		} 
		else if(smell.equals("l")) {
			System.out.println("smell is left");
			System.out.println(forward + " = forward");
			System.out.println(left + " = left");
			// checks one space left then two spaces left 
			if (left != null) {
				if (left.equals("*") || left.equals("@") || left.equals("#") || left.equals("Q")) {// checks one space left
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					}
					else if (forward.equals("*") || forward.equals("@") || forward.equals("#") || forward.equals("Q")) {
						System.out.print("In a corner, turning right!");
						heading = "r";
					}
				}
			}
			else if (twoLeft != null && leftRear != null && leftFront != null) {// checks two spaces left/rear 
				System.out.println("There is a wall two spots left and rearLeft and frontLeft"); 
				if ((twoLeft.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q")) && (leftRear.equals("*") || leftRear.equals("@") || leftRear.equals("#") || leftRear.equals("Q")) && (leftFront.equals("*") || leftFront.equals("@") || leftFront.equals("#") || leftFront.equals("Q"))) { 
					if (forward == null)
					{
						System.out.print("Three Sided trap!");
						heading = "f";
					}
				}
			}
			else if (twoLeft != null && leftRear != null) {// checks two spaces left/rear
				System.out.println("There is a wall two spots left and rearLeft");
				if (twoLeft.equals("*") && leftRear.equals("*")) {
					
						System.out.print("In a corner, turning left!");
						heading = "l";
				}
			}
			else if (twoLeft != null) {// checks two spaces left
				System.out.println("There is a wall two spots left");
				if (twoLeft.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q")) {
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					} 
					else if (forward.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q")) {
						System.out.print("In a corner, turning right!");
						heading = "r";
					}
				}
			}
		} 
		else if(smell.equals("r")) {// let's search for holes
			System.out.println("smell is right");
			System.out.println(forward + " = forward");
			System.out.println(right + " = right");
			// checks one space right then two spaces right 
			if (right != null) {
				if (right.equals("*") || right.equals("@") || right.equals("#") || right.equals("Q")) {// checks one space right
					if (forward == null) {
						System.out.println("Right Smell instinct blocked with wall");
						heading = "f";
					} else if (forward.equals("*") || right.equals("@") || right.equals("#")) {
						System.out.print("In a corner, turning left!");
						heading = "l";
					}
				}
			}
			else if (twoRight != null && rightRear != null && rightFront != null) {// checks two spaces right/rear 
				System.out.println("There is a wall two spots right and rearRight and frontRight"); 
				if ((twoRight.equals("*") || twoRight.equals("@") || twoRight.equals("#") || twoRight.equals("Q")) && (rightRear.equals("*") || rightRear.equals("@") || rightRear.equals("#") || rightRear.equals("Q")) && (rightFront.equals("*") || rightFront.equals("@") || rightFront.equals("#") || rightFront.equals("Q"))) { 
					if (forward == null)
					{
						System.out.print("Three Sided trap!");
						heading = "f";
					}
				}
			}
			else if (twoRight != null && rightRear != null &&  rightFront == null && rightRightFront == null) {// checks two spaces right/rear
				System.out.println("There is a wall two spots right and rearRight and rightFront and rightRightFront");
				if ((twoRight.equals("*") || twoRight.equals("@") || twoRight.equals("#") || twoRight.equals("Q")) && (rightRear.equals("*") || rightRear.equals("@") || rightRear.equals("#") || rightRear.equals("Q"))) {
						System.out.print("Right front Open!");
						heading = "r";
				}
			}
			else if (twoRight != null) {// checks two spaces right
				System.out.println("There is a wall two spots right");
				if (twoRight.equals("*") || twoRight.equals("@") || twoRight.equals("#") || twoRight.equals("Q")) {
					if (forward == null) {
						System.out.println("TwoRight heading forward");
						heading = "f";
					} 
					else if (forward.equals("*") || forward.equals("@") || forward.equals("#") || forward.equals("Q")) {
						System.out.print("In a corner, turning left!");
						heading = "l";
					}
				}
			}
		}
		else if (smell.equals("b")) {// let's search for holes
			System.out.println("smell is back");
			System.out.println(back + " = back");
			if (back != null) {
				if (back.equals("*") || back.equals("#") || back.equals("Q")) {
					if (forward == null) {
						System.out.println("Back Smell instinct blocked by obstacle");
						heading = "f";
					} else if (forward.equals("*")) {
						System.out.print("In a corner, turning right!");
						heading = "l";
					}
				}
			} else if (leftRear != null && !leftRear.equals("+") && !leftRear.equals("T") && !leftRear.equals("K")) { 
				System.out.println("leftRear View blocked"); 
				if ((leftRear.equals("*") || leftRear.equals("@") || leftRear.equals("#") || leftRear.equals("Q")) && left == null) {
					heading = "l";
				}
				else
					heading = "r";
			} else if (rightRear != null) { 
				System.out.println("rightRear View blocked"); 
				if ((rightRear.equals("*") || rightRear.equals("@") || rightRear.equals("#") || rightRear.equals("Q")) && right == null) { 
					heading = "r";
				}
				else
					heading = "l";
			}
		}
		return heading;
	}

	/**
	 * searches the visual field for the hammer or the key.
	 * @param visField visual field of the agent
	 * @param ground ground contents of the agent location
	 * @return the direction of the item to the agent or "g" to grab if found
	 */
	private String grabItem(String[][] visField, String ground) {
		String item = null;
		int i, j;
		for (i = 0; i < 7; i++) {
			for (j = 0; j < 5; j++) {
				if (visField[i][j] != null) {
					if (visField[i][j].equals("T") || visField[i][j].equals("K")) {
						
						System.out.println("Item Found");//debug************
						
						if (CURRENT_ROW - i > 0) {
							
							System.out.println("Moving Forward towards hammer.");//debug******8
							
							item = "f";
						} else if (CURRENT_ROW - i < 0) {
							
							System.out.println("Moving Back towards hammer.");//debug****8
							
							item = "b";
						} else if (CURRENT_COL - j > 0) {
							
							System.out.println("Moving Left towards hammer.");//debug*******
							
							item = "l";
						} else if (CURRENT_COL - j < 0) {
							
							System.out.println("Moving Right towards hammer.");//debug*****
							
							item = "r";
						}
					} else if (ground != null) {
						if (ground.contains("T") || ground.contains("K")) {
							
							System.out.println("Grabbing hammer.");//debug**********
							
							item = "g";
						}
					}
					// check wall for direction
					if (item != null) {
						if (!item.equals("g")) {
							
							System.out.println("Gets to hammer check");//debug*************
							
							item = checkWall(visField, item);
						}
					}
				}
			}
		}
		return item;
	}
	
	/**
	 * uses item in the agents inventory is object is not wall and agent has item
	 * @param visField visual field of the agent
	 * @param inventory inventory of the agent
	 * @return "u" to use item. NULL otherwise
	 */
	private String useWeapon(String[][] visField, String inventory) {
		
		//debug*************************
		System.out.println("Use Weapon inventory is ");
		System.out.println(inventory);
		//***********************88888
		
		String action = null;
		if (visField[CURRENT_ROW - 1][CURRENT_COL] != null && !inventory.equals("()")) {
			if (visField[CURRENT_ROW - 1][CURRENT_COL].equals("@") || visField[CURRENT_ROW - 1][CURRENT_COL].equals("#")) {
				
				System.out.println("Non-Wall Found");//debug************************
				
				action = "u";
			}
		}
		return action;
	}

	/**
	 * returns the direction of a wall or boulder so that the agent can hug the wall/boulder
	 * @param visField visual field of the agent
	 * @return direction the agent should hug the wall. NULL if not next to a wall/boulder
	 */
	private String hugWall(String[][] visField) {
		// declare local variables
		Random r = new Random();
		int rand = 0;
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String leftRear = visField[CURRENT_ROW + 1][CURRENT_COL - 1]; 
		String rightRear = visField[CURRENT_ROW + 1][CURRENT_COL + 1]; 
		String heading = null;
		if (left != null) {
			//if wall or boulder is left of the agent, hug it
			if (left.equals("*") || left.equals("@") || left. equals("Q")) {// checks one space left
				
				//debug*******************************
				System.out.println("Hug wall mode left");
				System.out.println(forward + " = forward");
				System.out.println(left + " = left");
				//**************************
				
				//if nothing in front of the agent move forward
				//else if in a corner turn right
				if (forward == null) {
					
					System.out.println("Let's hug wall moving forward");//debug******************8
					
					heading = "f";
				} else if (forward.equals("*") || left.equals("@") || forward.equals("@") || left.equals("*")) {
					
					System.out.print("In a corner, turning right!");//debug**********8
					
					heading = "r";
				}
			}
		} 
		else if (right != null) {
			//if wall or boulder is right of the agent, hug it
			if (right.equals("*") || right.equals("@") || right.equals("Q")) {// checks one space
															// right
				//debug***************************
				System.out.println("Wall hugging is right");
				System.out.println(forward + " = forward");
				System.out.println(right + " = right");
				//******************************888
				
				//if nothing in front of the agent move forward
				//else if in corner turn left
				if (forward == null) {
					
					System.out.println("Let's hug wall moving forwad");//debug*************
					
					heading = "f";
				} else if (forward.equals("*") || right.equals("@") || forward.equals("@") || right.equals("*")) {
					
					System.out.print("In a corner, turning left!");//debug****************8
					
					heading = "l";
				}
			}
		}/*
		else if (rightRear != null) {
			System.out.println("No wall beside, but right rear so going right"); 
			heading = "r"; 
		}
		*/
		/*
		else if (leftRear != null) {
			System.out.println("No wall beside, but left rear so going left"); 
			heading = "l"; 
		}
		*/
		return heading;
	}

	/**
	 * checks if there is a boulder adjacent to the agent
	 * @param visField visual field of the agent
	 * @return the direction of the boulder. NULL if none
	 */
	private static String checkBoulder(String visField[][]) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;
		
		//check is boulder is in front
		// left
		//right
		//or behind agent and return direction
		if (forward != null) {
			if (forward.equals("@")) {
				heading = "u";
			}
		} else if (left != null) {
			if (left.equals("@")) {
				heading = "l";
			}
		} else if (right != null) {
			if (right.equals("@")) {
				heading = "r";
			}
		} else if (back != null) {
			if (back.equals("@")) {
				heading = "b";
			}
		}
		return heading;
	}
	
	/**
	 * checks if there is a door adjacent to the agent 
	 * @param visField visual field of the agent
	 * @return the direction of the door if adjacent to the agent. NULL if none
	 */
	private static String checkDoor(String visField[][]) {

		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;

		//check is door is in front
		// left
		//right
		//or behind agent and return direction
		if (forward != null) {
			if (forward.equals("#")) {
				heading = "u";
			}
		} else if (left != null) {
			if (left.equals("#")) {
				heading = "l";
			}
		} else if (right != null) {
			if (right.equals("#")) {
				heading = "r";
			}
		} else if (back != null) {
			if (back.equals("#")) {
				heading = "b";
			}
		}
		return heading;
	}

	/**
	 * detects if there is a wall around the agent so it can decide to go
	 * around, if that is best option
	 * @param visField visual field of the agent
	 * @return true if there is a wall adjacent to the agent
	 */
	private boolean wallDetecter(String[][] visField) {
		boolean wall = false;
		int i, j;
		for (i = CURRENT_ROW - 1; i <= CURRENT_ROW + 1; i++) {
			for (j = CURRENT_COL; j < CURRENT_COL + 3; j++) {
				if (j == CURRENT_COL + 2)
					j = CURRENT_COL - 1;
				if (visField[i][j] != null) {
					if (visField[i][j].equals("*")) {
						wall = true;
						i = CURRENT_ROW + 2;
						j = CURRENT_COL + 2;
						wall =true;
					}
				}
				if (j == CURRENT_COL - 1)
					j = CURRENT_COL + 3;
			}
		}
		return wall;
	}
	
	/**
	 *  checks if there is a boulder adjacent to the agents
	 *  front, left, or right. that could be in the way of the path
	 *  to the cheese
	 * @param visField the visual field of the agent
	 * @return direction of a boulder next to the agent. NULL if none
	 */
	private String boulderDetecter(String[][] visField) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;
		boolean boulder = false;
		int rIndex = 0;
		int cIndex = 0;
		int i, j;
		for (i = CURRENT_ROW - 1; i <= CURRENT_ROW + 1; i++) {
			for (j = CURRENT_COL; j < CURRENT_COL + 3; j++) {
				if (j == CURRENT_COL + 2)
					j = CURRENT_COL - 1;
				if (visField[i][j] != null) {
					if (visField[i][j].equals("@")) {
						boulder = true;
						rIndex = i;
						cIndex = j;
						i = CURRENT_ROW + 2;
						j = CURRENT_COL + 2;
					}
				}
				if (j == CURRENT_COL - 1)
					j = CURRENT_COL + 3;
			}
		}

		//if boulder found
		//sets direction of boulder or "u" to use item if directly in front of agent
		if (boulder == true) {
			if (rIndex < CURRENT_ROW && cIndex == CURRENT_COL) {
					heading = "u";
			} else if (rIndex < CURRENT_ROW && forward == null) {
					heading = "f";
			} else if (cIndex < CURRENT_COL) {
					heading = "l";
			} else if (cIndex > CURRENT_COL) {
				heading = "r";
			} else if (rIndex > CURRENT_ROW && cIndex == CURRENT_COL) {
				heading = "l";
			}
		}
		
		return heading;
	}

	/**
	 * starts the agent program to run in the maeden environment
	 * @param args
	 */
	public static void main(String[] args) {
		SRAgent agent = new SRAgent("localhost", MAEDENPORT);
		agent.run();
	}

	/**
	 * object to store the sense the agent receives from the environment 
	 * to be easily accessed
	 *
	 */
	private static class State {
		String heading;
		String inv;
		String groundContents;
		int energy;
		String lastAction;
		String[][] visField;

		/**
		 * constructor that stores sensory information received to variables
		 * @param heading	direction of cheese
		 * @param inv		inventory of agent
		 * @param groundContents	contents on ground at agents location
		 * @param energy	remaining energy of the agent
		 * @param lastAction	last action status "fail" or "ok"
		 * @param visField	visual field of the agent
		 */
		public State(String heading, String inv, String groundContents, String energy, String lastAction,
				String[][] visField) {
			this.heading = heading;
			this.inv = inv;
			this.groundContents = groundContents;
			this.energy = Integer.parseInt(energy);
			this.lastAction = lastAction;
			this.visField = visField;
		}
	}
}