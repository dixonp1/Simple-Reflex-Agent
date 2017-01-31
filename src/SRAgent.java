import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.Random;

/**
 * Simple-Reflex Agent to navigate the Maeden simulation environment
 * Designed to sequentially complete worlds by using next "best" action without keeping world state, using only senses
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
	 * as a String. actions are "flrbhgud"
	 */
	private void performAction(String action) {
		// used for DEBUGGING
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
		// *****
		switch (action.charAt(0)) {
		case 'f':
			// move forward
			gc.effectorSend("f");
			// debug ********************System.out.println("forward");
			break;
		case 'b':
			// turn around instead of going back to allow usage of the extra visual
			// cells the visual field displays in front of the agent
			gc.effectorSend("l");
			// debug ********************System.out.println("turned left");
			break;
		case 'r':
			// more right
			gc.effectorSend("r");
			// debug ********************System.out.println("right");
			break;
		case 'l':
			// move left
			gc.effectorSend("l");
			// debug ********************System.out.println("left");
			break;
		case 'h':
			// cheese on current space
			// grab and use cheese
			gc.effectorSend("g +");
			// debug ********************System.out.println("found cheese");
			gc.effectorSend("u +");
			break;
		case 'g':
			// hammer on current space
			// grab hammer
			gc.effectorSend("g");
			// debug ********************System.out.println("grabbing hammer");
			break;
		case 'u':
			// use weapon
			// grab and use cheese
			gc.effectorSend("u");
			// debug ********************System.out.println("using weapon");
			break;
		case 'd':
			// cheese on current space
			// grab and use cheese
			gc.effectorSend("d T");
			// debug ********************System.out.println("drop item");
			break;
		default:

		}
	}

	/**
	 * Decides on the "best" action to take based on current situation. Treats every move
	 * as if it is the first move the agent has made
	 * 
	 * When energy reaches a certain point, agent gets tired and hugs the wall
	 * either left or right depending on its position to agent. This fixes issues causing agent to
	 * repeat actions until death.
	 * 
	 * Actions are "decided" based on priority of the action: 
	 * find Cheese -> search for Narrows -> get items -> check for boulders/doors -> Agent gets tired twice and hugs wall -> 
	 * use item -> wall detector -> check for walls 
	 * @param state sensory information of the agent
	 * @return the action the agent decides to perform
	 */
	private String getAction(State state) {
		String action;
		//debug**************System.out.println("The energy is " + state.energy + "vs" + (STARTING_ENERGY * 0.9));
		//Check for cheese
		action = findCheese(state.visField, state.groundContents);
		//check for narrows
		if (action == null) {
			action = searchNarrows(state.visField, state.inv, state.groundContents);
			// debug ********************System.out.println("Narrows returned " + action);
		}
		//check for items
		if (action == null) {
			//debug***********************System.out.println("Calling grab item.");
			action = grabItem(state.visField, state.groundContents);
		}
		if (action == null && state.inv.contains("T")) {
			// debug ********************System.out.println("Going hulk hunting.");
			action = checkBoulder(state.visField);
		}
		if (action == null && state.inv.contains("K")) {
			// debug ********************System.out.println("Looking for door to use key!");
			action = checkDoor(state.visField);
			//debug***************************System.out.println("check door returned " + action);
		}
		//Agent gets tired
		//enter if energy is between 90% and 79%
		if (action == null && state.energy < (STARTING_ENERGY * (0.9)) && state.energy > (STARTING_ENERGY * (0.79))) {
			// debug ********************System.out.println("Agent tired looking for a wall to hold on to!");
			action = hugWall(state.visField, state.groundContents);
		}
		//Agent further tired
		//enter if energy between 55% and 40%
		if (action == null && state.energy < (STARTING_ENERGY * 0.55) && state.energy > (STARTING_ENERGY * .4)) {
			// debug ********************System.out.println("Agent very tired looking for a wall to hold on to!");
			action = hugWall(state.visField, state.groundContents);
		}
		//use item if in inventory
		if (action == null) {
			// debug ********************System.out.println("Looking to use weapon!");
			action = useWeapon(state.visField, state.inv);
		}
		if (action == null) {
			if (wallDetecter(state.visField) && (state.inv).contains("T")) {
				// debug ********************System.out.println("Found a wall so looking for boulder near by!");
				action = boulderDetecter(state.visField);
			}
		}
		//checks for walls in agents path
		if (action == null) {
			// debug ********************System.out.println("Checking for a wall!");
			action = checkWall(state.visField, state.heading);
			if (action == null) {
				// debug ********************System.out.println("Using sense of smell!");
			}
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
		sp.processRetinalField(senses[2]);//lets SensoryPacket process visual field and convert it to String[][]
		String[][] visField = processVisual(sp.getVisualArray());
		String groundContents = senses[3];
		String energy = senses[5];
		String lastAction = senses[6];

		return new State(heading, inv, groundContents, energy, lastAction, visField);
	}

	/**
	 * Converts the visual field received from the environment to a String[][]
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
				// debug ********************System.out.println(newVisField[i][j] + " " + i + " " + j);
			}
			// debug ********************System.out.println();
		}
		return newVisField;
	}
	/**
	 * 
	 * @param visField
	 * @param ground
	 * @return the direction the agent will move
	 * Bases move on if cheese is one space in any direction or if standing on cheese
	 */
	//Check if direct path to cheese one space away
	private String findCheese(String[][] visField, String ground) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String heading = null;
		if (forward != null) {
			if (forward.equals("+"))
				heading = "f";
		}
		if (left != null) {
			if (left.equals("+"))
				heading = "l";
		}
		if (right != null) {
			if (right.equals("+"))
				heading = "r";
		}
		if (ground.contains("+")) {
			heading = "h";
		}
		return heading;
	}

	/**
	 * 
	 * @param visField
	 * @param smell
	 * @return the direction the agent will move
	 * Priority is clear path to cheese then Bases decision on smell and obstacles presented
	 */
	private String checkWall(String[][] visField, String smell) {
		// declare local variables
		Random r = new Random();
		int rand = 0;
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String twoRight = visField[CURRENT_ROW][CURRENT_COL + 2]; 
		String leftFront = visField[CURRENT_ROW - 1][CURRENT_COL - 1]; 
		String rightFront = visField[CURRENT_ROW - 1][CURRENT_COL + 1];
		String leftRear = visField[CURRENT_ROW + 1][CURRENT_COL - 1];
		String rightRear = visField[CURRENT_ROW + 1][CURRENT_COL + 1];  
		String rightRightFront = visField[CURRENT_ROW - 1][CURRENT_COL + 2];
		String leftLeftFront = visField[CURRENT_ROW - 1][CURRENT_COL - 2];
		String fF = visField[CURRENT_ROW - 2][CURRENT_COL];
		String fFF = visField[CURRENT_ROW - 3][CURRENT_COL];
		String fFFF = visField[CURRENT_ROW - 4][CURRENT_COL];/*Changed from 3 to 4*/
		String lFF = visField[CURRENT_ROW - 2][CURRENT_COL - 1];
		String rFF = visField[CURRENT_ROW - 2][CURRENT_COL + 1];
		String heading = smell;
		boolean wholeInWall = false;
		boolean cheeseOverride = false;
		boolean freedomTurn = true;
		// debug ********************System.out.println(smell);
		/*After 29 remove boolean cheese override over whole block*/
		if (smell.equals("f")) {
			if (forward != null && fF != null) {
				if (forward.equals("Q") && fF.equals("+")) {
					heading = "f";
					cheeseOverride = true;
				}
			}
			else if (forward != null && fFF != null) {
				if (forward.equals("Q") && fFF.equals("+")) {
					heading = "f";
					cheeseOverride = true;
				}
			}
			else if (forward == null && fF == null && fFF != null) {
				if (fFF.equals("+")) {
					heading = "f";
					cheeseOverride = true;
				}
			}
		}
		// debug ********************System.out.println("Cheese override = " + cheeseOverride);
		if (!cheeseOverride) {
			if (smell.equals("f")) {
				// debug ********************System.out.println("Forward space contains " + forward);
				if (forward != null) {
					if (forward.equals("*") || forward.equals("@") || forward.equals("#") || forward.equals("Q")) {
						if (left != null) {
							if (left.equals("*") || left.equals("@") || left.equals("#") || left.equals("Q")) {//left is blocked
								heading = "r";
								freedomTurn = false;
							}
						}
						if (right != null) {
							if (right.equals("*") || right.equals("@") || right.equals("#") || right.equals("Q")) {//right is blocked
								heading = "l";
								freedomTurn = false;
							}
						} 
						if (freedomTurn == true){//both right and left options are open
							for (int i = 0; i < 2; i++) {// search four visible spots
								if (left != null) {
									heading = "r";
								}
								else if ((visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]) == null || (visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]).contains("+")) {
									// debug ********************System.out.println((CURRENT_COL - (i + 1)) % 5);
									// debug ********************System.out.println("Moving left from wall");
									wholeInWall = true;
									heading = "l"; // hole is left
									i = 2; // signal end of loop we’ve found nearest hole in wall
								} else if ((visField[CURRENT_ROW - 1][(CURRENT_COL + (i + 1)) % 5]) == null || (visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]).contains("+")) {
									if (right != null) {
										if (right.equals("*")) {
											heading = "l";
										}
									}
									/*end check*/
									wholeInWall = true;
									heading = "r"; // hole is right
									i = 2; // signal end of loop we’ve found nearest
											// hole in wall
								}
							}
							if (wholeInWall == false) {// long wall encountered
								// debug ********************System.out.println("There is no visible hole in wall");
								rand = r.nextInt(2) * 2 - 1;
								if (rand == -1) {
									// debug ********************System.out.println("Random generator chose left");
									heading = "l";
								} 
								else if (rand == 1) {
									// debug ********************System.out.println("Random generator chose right");
									heading = "r";
								}
							}
						}
					}
				}
				else if (fFF != null && fFFF != null && left != null && right == null) {//check if go right
					if (fFF.equals("*") && fFFF.equals("*") && (left.equals("*"))) {
							heading = "r";
					}
				} else if (fFF != null && fFFF != null && left == null && right != null) {//check if go left
					if (fFF.equals("*") && fFFF.equals("*") && right.equals("*")) {
						heading = "l";
					}
				}
				else if (fFF != null && fFFF != null && left != null && right != null) {
					if (fFF.equals("*") && fFFF.equals("*") && right.equals("*") && left.equals("*")) {
						heading = "f";
					}
				}
				else if (fFF != null && fFFF != null && left == null && right == null) {
					if (fFF.equals("*") && fFFF.equals("*")) {
						heading = "f";
					}
				}
			} 
			else if(smell.equals("l")) {
				// debug ********************// debug ********************System.out.println("smell is left");
				// debug ********************System.out.println(forward + " = forward");
				// debug ********************System.out.println(left + " = left");
				/*After 28 return else if to if*/
				if (leftFront != null && rightFront != null && rFF != null && lFF != null) {
					if (leftFront.equals("*") && rightFront.equals("*") && rFF.equals("*") && lFF.equals("*") && forward == null) {
						heading = "f";
					}
					else if (left == null)
						heading = "l";
				}
				// checks one space left then two spaces left 
				else if (left != null) {
					if (left.equals("*") || left.equals("@") || left.equals("#") || left.equals("Q")) {// checks one space left
						if (forward == null) {
							// debug ********************System.out.println("Left Smell instinct blocked with wall");
							heading = "f";
						}
						else if (forward.equals("*") || forward.equals("@") || forward.equals("#") || forward.equals("Q")) {
							// debug ********************System.out.print("In a corner, turning right!");
							heading = "r";
						}
					}
				}
				else if (twoLeft != null && leftRear != null && leftFront != null) {// checks two spaces left/rear 
					// debug ********************System.out.println("There is a wall two spots left and rearLeft and frontLeft");
					if ((twoLeft.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q")) && (leftRear.equals("*") || leftRear.equals("@") || leftRear.equals("#") || leftRear.equals("Q")) && (leftFront.equals("*") || leftFront.equals("@") || leftFront.equals("#") || leftFront.equals("Q"))) { 
						if (forward == null)
						{
							// debug ********************System.out.print("Three Sided trap!");
							heading = "f";
						}
					}
				}
				else if (twoLeft != null && leftRear != null) {// checks two spaces left/rear
					// debug ********************System.out.println("There is a wall two spots left and rearLeft");
					if (twoLeft.equals("*") && leftRear.equals("*")) {
						// debug ********************System.out.print("In a corner, turning left!");
						heading = "l";
					}
				}
				else if (twoLeft != null /*&& leftRear != null */&&  leftFront == null && leftLeftFront == null && forward == null) {// checks two spaces right/rear
					if ((twoLeft.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q"))/* && (leftRear.equals("*") || leftRear.equals("@") || leftRear.equals("#") || leftRear.equals("Q"))*/) {
						// debug ********************System.out.println("There is a wall two spots left but opening to left, can go forward");
						heading = "f";
					}
				}
				else if (twoLeft != null /*&& leftRear != null */&&  leftFront == null && leftLeftFront == null) {// checks two spaces right/rear
					if ((twoLeft.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q"))/* && (leftRear.equals("*") || leftRear.equals("@") || leftRear.equals("#") || leftRear.equals("Q"))*/) {
						// debug ********************System.out.println("There is a wall two spots left and rearLeft but opening to left");
						heading = "l";
					}
				}
				else if (twoLeft != null) {// checks two spaces left
					// debug ********************System.out.println("There is a wall two spots left");
					if (twoLeft.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q")) {
						if (forward == null) {
							// debug ********************System.out.println("Left Smell instinct blocked with wall");
							heading = "f";
						} 
						else if (forward.equals("*") || twoLeft.equals("@") || twoLeft.equals("#") || twoLeft.equals("Q")) {
							// debug ********************System.out.print("In a corner, turning right!");
							heading = "r";
						}
					}
				}
			} 
			else if(smell.equals("r")) {// let's search for holes
				// debug ********************System.out.println("smell is right");
				// debug ********************System.out.println(forward + " = forward");
				// debug ********************System.out.println(right + " = right");
				// checks one space right then two spaces right 
				if (right != null) {
					if (right.equals("*") || right.equals("@") || right.equals("#") || right.equals("Q")) {// checks one space right
						if (forward == null) {
							// debug ********************System.out.println("Right Smell instinct blocked with wall");
							heading = "f";
						} else if (forward.equals("*") || right.equals("@") || right.equals("#")) {
							// debug ********************System.out.print("In a corner, turning left!");
							heading = "l";
						}
					}
				}
				else if (twoRight != null && rightRear != null && rightFront != null) {// checks two spaces right/rear 
					// debug ********************System.out.println("There is a wall two spots right and rearRight and frontRight"); 
					if ((twoRight.equals("*") || twoRight.equals("@") || twoRight.equals("#") || twoRight.equals("Q")) && (rightRear.equals("*") || rightRear.equals("@") || rightRear.equals("#") || rightRear.equals("Q")) && (rightFront.equals("*") || rightFront.equals("@") || rightFront.equals("#") || rightFront.equals("Q"))) { 
						if (forward == null)
						{
							// debug ********************System.out.print("Three Sided trap!");
							heading = "f";
						}
					}
				}
				else if (twoRight != null /*&& rightRear != null */&&  rightFront == null && rightRightFront == null) {// checks two spaces right/rear
					// debug ********************System.out.println("There is a wall two spots right and rearRight but opening to right");
					if ((twoRight.equals("*") || twoRight.equals("@") || twoRight.equals("#") || twoRight.equals("Q"))/* && (rightRear.equals("*") || rightRear.equals("@") || rightRear.equals("#") || rightRear.equals("Q"))*/) {
						// debug ********************System.out.print("Right front Open!");
							heading = "r";
					}
				}
				else if (twoRight != null) {// checks two spaces right
					// debug ********************System.out.println("There is a wall two spots right");
					if (twoRight.equals("*") || twoRight.equals("@") || twoRight.equals("#") || twoRight.equals("Q")) {
						if (forward == null) {
							// debug ********************System.out.println("TwoRight heading forward");
							heading = "f";
						} 
						else if (forward.equals("*") || forward.equals("@") || forward.equals("#") || forward.equals("Q")) {
							// debug ********************System.out.print("In a corner, turning left!");
							heading = "l";
						}
					}
				}
			}
			else if (smell.equals("b")) {// let's search for holes
				// debug ********************System.out.println("smell is back");
				// debug ********************System.out.println(back + " = back");
				if (back != null) {
					if (back.equals("*") || back.equals("#") || back.equals("Q")) {
						if (forward == null) {
							// debug ********************System.out.println("Back Smell instinct blocked by obstacle");
							heading = "f";
						} else if (forward.equals("*")) {
							// debug ********************System.out.print("In a corner, turning right!");
							heading = "l";
						}
					}
				} else if (leftRear != null && !leftRear.equals("+") && !leftRear.equals("T") && !leftRear.equals("K")) { 
					// debug ********************System.out.println("leftRear View blocked"); 
					if ((leftRear.equals("*") || leftRear.equals("@") || leftRear.equals("#") || leftRear.equals("Q")) && left == null) {
						heading = "l";
					}
					else
						heading = "r";
				} else if (rightRear != null) { 
					// debug ********************System.out.println("rightRear View blocked"); 
					if ((rightRear.equals("*") || rightRear.equals("@") || rightRear.equals("#") || rightRear.equals("Q")) && right == null) { 
						heading = "r";
					}
					else
						heading = "l";
				}//end else if
			}//end else if
		}//end if
		return heading;
	}//end checkWall

	/**
	 * Searches the visual field for the hammer or the key.
	 * @param visField 
	 * visual field of the agent
	 * @param ground 
	 * ground contents of the agent location
	 * @return the direction of the item to the agent or "g" to grab if found
	 * Will prioritize moving forward towards item and then turning towards item, ignoring items one space behind.
	 */
	private String grabItem(String[][] visField, String ground) {
		String item = null;
		int i, j;
		for (i = 0; i < 7; i++) {
			for (j = 0; j < 5; j++) {
				if (visField[i][j] != null) {
					if (visField[i][j].equals("T") || visField[i][j].equals("K")) {
						// debug ********************System.out.println("Item Found");
						if (CURRENT_ROW - i > 0) {
							// debug ********************System.out.println("Item found ahead at " + i + " " + j);
							item = "f";
						} else if (CURRENT_ROW - i < 0) {
							// debug ********************System.out.println("Ignoring item behind.");
							item = "f";
						} else if (CURRENT_COL - j > 0) {
							// debug ********************System.out.println("Moving Left towards item.");
							item = "l";
						} else if (CURRENT_COL - j < 0) {
							// debug ********************System.out.println("Moving Right towards item.");
						}
						//Signal end of loop if already found an item
						i=7;
						j=5;
					} else if (ground != null) {
						if (ground.contains("T") || ground.contains("K")) {
							// debug ********************System.out.println("Grabbing item.");
							item = "g";
						}
					}
					// check wall for direction
					if (item != null) {
						if (!item.equals("g")) {
							// debug ********************System.out.println("Calling check wall from grab item method");
							item = checkWall(visField, item);
						}
					}
				}//end if
			}//end for
		}//end for
		return item;
	}// end grabItem
	
	/**
	 * Searches the visual field for narrows.
	 * @param visField 
	 * visual field of the agent
	 * @param inventory 
	 * inventory of the agent
	 * @param ground 
	 * ground contents on space of agent
	 * @return the direction of the narrows
	 * Look through entire visual field beginning with farthest forward and farthest left spaces visible.
	 * Priority to drop item if narrow ahead, else move forward, else turn direction of item right or left.
	 */
	private String searchNarrows(String[][] visField, String inventory, String ground) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String narrows = null;
		int i, j;
		for (i = 0; i < 7; i++) {
			for (j = 0; j < 5; j++) {
				if (visField[i][j] != null) {
					if (ground.contains("="));
					if (visField[i][j].equals("=")) {
						// debug ********************System.out.println("Narrows Found");
						// debug ********************System.out.println("Inventory Contains " + inventory);
						if (CURRENT_ROW - i > 0) {
							if (forward != null) {
								if (forward.equals("=") && inventory.contains("T")) {
									narrows = "d";
									// debug ********************System.out.println("Dropping hammer");
								}
								else if (forward.equals("="))
									narrows = "f";
								else if (forward.equals("*")) {
									if (j < CURRENT_COL) {
										// debug ********************System.out.println("Narrows to left, turn.");
										narrows = "l";
									} else if (j > CURRENT_COL) {
										// debug ********************System.out.println("Narrows to right, turn.");
										narrows = "r";
									}
								}
							}
							else {
								// debug ********************System.out.println("Moving Forward towards narrows.");
								narrows = "f";
							}
						} else if (CURRENT_ROW - i < 0) {//if narrows behind
							// debug ********************System.out.println("Narrows behind.");
							if (ground.contains("T") && back != null) {
								if (back.equals("=")) {
									narrows = "g";
								}
							}
							if (narrows == null) {
								if (forward == null) {
									narrows = "f";
								}
								else if (left == null) {
									narrows = "l";
								}
								else if (right == null) {
									narrows = "r";
								}
							}
						} else if (j == 0 && CURRENT_ROW == i) {
							// debug ********************System.out.println("Narrows two left, forward.");
							narrows = "f";
						} else if (j == 4 && CURRENT_ROW == i) {
							// debug ********************System.out.println("Narrows two right, forward.");
							narrows = "f";
						} else if (j < CURRENT_COL) {
							// debug ********************System.out.println("Narrows to left, turn.");
							narrows = "l";
						} else if (j > CURRENT_COL) {
							// debug ********************System.out.println("Narrows to right, turn.");
							narrows = "r";
						} else if (j == 0 && i == CURRENT_ROW) {
							// debug ********************System.out.println("Follow smell, narrows to left.");
							narrows = "f";
						} else if (j == 4 && i == CURRENT_ROW) {
							// debug ********************System.out.println("Follow smell narrows to right.");
							narrows = "f";
						} 
					}//end if
				}//end if
			}//end for
		}//end for
		return narrows;
	}//end searchNarrows
	
	/**
	 * Uses item in the agents inventory is object is not wall and agent has item
	 * @param visField 
	 * visual field of the agent
	 * @param inventory 
	 * inventory of the agent
	 * @return "u" to use item. NULL otherwise
	 * Checks for a boulder or door one space ahead if inventory is not empty.
	 */
	private String useWeapon(String[][] visField, String inventory) {
		// debug ********************System.out.println("Use Weapon inventory is ");
		// debug ********************System.out.println(inventory);
		String action = null;
		if (visField[CURRENT_ROW - 1][CURRENT_COL] != null && !inventory.equals("()")) {
			if (visField[CURRENT_ROW - 1][CURRENT_COL].equals("@") || visField[CURRENT_ROW - 1][CURRENT_COL].equals("#")) {
				// debug ********************System.out.println("Non-Wall Found");
				action = "u";
			}
		}
		return action;
	}//end useWeapon

	/**
	 * returns the direction of a wall or boulder so that the agent can hug the wall/boulder
	 * @param visField 
	 * visual field of the agent
	 * @return direction the agent should hug the wall. NULL if not next to a wall/boulder
	 * Agent will prioritize cheese if "on same space or one space forward" 
	 * then will hug the wall if there is a wall to left or right by moving forward
	 * but if forward is blocked will turn to a direction with no wall beside
	 */
	private String hugWall(String[][] visField, String ground) {
		// declare local variables
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String heading = null;
		if (forward != null && forward.contains("+")) {
			heading = "f";
		}
		else if (ground.contains("+")) {
			heading = "h";
		}
		else if (left != null) {
			//if wall or boulder is left of the agent, hug it
			if (left.equals("*") || left.equals("@") || left. equals("Q")) {// checks one space left
				// debug ********************System.out.println("Hug wall mode left");
				// debug ********************System.out.println(forward + " = forward");
				// debug ********************System.out.println(left + " = left");
				//if nothing in front of the agent move forward
				//else if in a corner turn right
				if (forward == null) {
					// debug ********************System.out.println("Let's hug wall moving forward");
					heading = "f";
				} else if (forward.equals("*") || left.equals("@") || forward.equals("@") || left.equals("*")) {
					// debug ********************System.out.print("In a corner, turning right!");
					heading = "r";
				}
			}
		} 
		else if (right != null) {
			//if wall or boulder is right of the agent, hug it
			if (right.equals("*") || right.equals("@") || right.equals("Q")) {// checks one space right
				// debug ********************System.out.println("Wall hugging is right");
				// debug ********************System.out.println(forward + " = forward");
				// debug ********************System.out.println(right + " = right");
				//if nothing in front of the agent move forward
				//else if in corner turn left
				if (forward == null) {
					// debug ********************System.out.println("Let's hug wall moving forwad");//debug*************
					heading = "f";
				} else if (forward.equals("*") || right.equals("@") || forward.equals("@") || right.equals("*")) {
					// debug ********************System.out.print("In a corner, turning left!");//debug****************8
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
	 * @param visField 
	 * visual field of the agent
	 * @return the direction of the boulder. NULL if none
	 */
	private static String checkBoulder(String visField[][]) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;
		
		//check is boulder is in front left right or behind agent and return direction
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
	}//end checkBoulder
	
	/**
	 * checks if there is a door adjacent to the agent 
	 * @param visField 
	 * visual field of the agent
	 * @return the direction of the door if adjacent to the agent. NULL if none
	 */
	private static String checkDoor(String visField[][]) {

		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;

		//check is door is in front left right or behind agent and return direction
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
	 * detects if there is a wall adjacent to the agent
	 * @param visField 
	 * visual field of the agent
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
	 * @param visField 
	 * the visual field of the agent
	 * @return direction of a boulder next to the agent, or to smash boulder. NULL if none
	 */
	private String boulderDetecter(String[][] visField) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
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