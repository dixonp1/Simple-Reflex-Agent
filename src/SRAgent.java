import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.Random;

public class SRAgent {

	private static final int MAEDENPORT = 7237;
	private static final int CURRENT_COL = 2;// 2
	private static final int CURRENT_ROW = 5;// 1
	private static final double STARTING_ENERGY = 2000;
	protected GridClient gc;

	public SRAgent(String h, int p) {
		gc = new GridClient(h, p);
	}

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
			/*
			 * //move backwards gc.effectorSend("b");
			 * System.out.println("back");
			 */

			// turn around
			gc.effectorSend("l");
			System.out.println("turned left");
			break;
		case 'r':
			// more right
			gc.effectorSend("r");
			System.out.println("right");
			break;
		case 'l':
			// move left
			gc.effectorSend("l");
			System.out.println("left");
			break;
		case 'h':
			// cheese on current space
			// grab and use cheese
			gc.effectorSend("g +");
			System.out.println("found cheese");
			gc.effectorSend("u +");
			break;
		case 'g':
			// hammer on current space
			// grab hammer
			gc.effectorSend("g");
			System.out.println("grabbing hammer");
			break;
		case 'u':
			// use weapon
			// grab and use cheese
			gc.effectorSend("u");
			System.out.println("using weapon");
			break;
		default:

		}
	}

	private String getAction(State state) {
		String action;
		System.out.println("The energy is " + state.energy + "vs" + (STARTING_ENERGY * 0.9));
		action = grabItem(state.visField, state.groundContents);
		if (action == null && state.inv.contains("T")) {
			action = checkBoulder(state.visField);
		}
		if (action == null && state.inv.contains("K")) {
			action = checkKey(state.visField);
		}
		if (action == null && state.energy < (STARTING_ENERGY * (0.9)) && state.energy > (STARTING_ENERGY * (0.79))) {
			action = hugWall(state.visField);
		}
		if (action == null && state.energy < (STARTING_ENERGY * 0.55) && state.energy > (STARTING_ENERGY * .4)) {
			action = hugWall(state.visField);
		}
		if (action == null) {
			action = useWeapon(state.visField, state.inv);
		}
		if (action == null) {
			action = checkWall(state.visField, state.heading);
		}
		return action;
	}

	private State getState() {
		SensoryPacket sp = gc.getSensoryPacket();
		String[] senses = sp.getRawSenseData();

		String heading = senses[0];

		// sent as continuous string containing inv items
		// '+' = cheese
		String inv = senses[1];

		sp.processRetinalField(senses[2]);
		// ArrayList<ArrayList<Vector<Character>>> visField =
		// sp.getVisualArray();
		String[][] visField = processVisual(sp.getVisualArray());
		String groundContents = senses[3];
		String energy = senses[5];
		String lastAction = senses[6];

		return new State(heading, inv, groundContents, energy, lastAction, visField);
	}

	private String[][] processVisual(ArrayList<ArrayList<Vector<Character>>> visField) {
		String[][] newVisField = new String[7][5];

		for (int i = 0; i < visField.size(); i++) {
			for (int j = 0; j < visField.get(i).size(); j++) {
				for (Character item : visField.get(i).get(j)) {
					if (item.charValue() != '"') {
						newVisField[i][j] = "" + item.charValue();
					}
				}
				System.out.println(newVisField[i][j] + " " + i + " " + j);// debugging
			}
			System.out.println();// debugging
		}

		return newVisField;
	}

	private String checkWall(String[][] visField, String smell) {
		// declare local variables
		Random r = new Random();
		int rand = 0;
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String leftRear = visField[CURRENT_ROW + 1][CURRENT_COL - 1];
		String heading = smell;
		boolean wholeInWall = false;
		System.out.println(smell);
		if (smell.equals("f")) {
			if (forward != null) {
				if (forward.equals("*") || forward.equals("@") || forward.equals("#")) {
					for (int i = 0; i < 2; i++) {// search four visible spots
						if ((visField[CURRENT_ROW - 1][(CURRENT_COL + (i + 1)) % 5]) == null) {
							System.out.println((CURRENT_COL + (i + 1)) % 5);
							System.out.println("Moving right from wall");
							wholeInWall = true;
							heading = "r"; // hole is right
							i = 2; // signal end of loop we have found nearest hole in wall
						} else if ((visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]) == null) {
							System.out.println("Moving left from wall");
							wholeInWall = true;
							heading = "l"; // hole is left
							i = 2; // signal end of loop we have found nearest
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
							System.out.println("Random generator chose rigt");
							heading = "r";
						} 
						else {
							System.out.println("Random generator broken" + rand);
						}
					}
				}
			}
		} 
		else if(smell.equals("l")) {// let's search for holes
			System.out.println("smell is left");
			System.out.println(forward + " = forward");
			System.out.println(left + " = left");
			if (left != null) {
				if (left.equals("*")) {// checks one space left
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					}
					else if (forward.equals("*")) {
						System.out.print("In a corner, turning right!");
						heading = "r";
					}
				}
			}
			else if (twoLeft != null && leftRear != null) {// checks two spaces left/rear
				System.out.println("There is a wall two spots left and rearLeft");
				if (twoLeft.equals("*") && leftRear.equals("*")) {
					
						System.out.print("In a corner, turning right!");
						heading = "l";
				}
			}
			else if (twoLeft != null) {// checks two spaces left
				System.out.println("There is a wall two spots left");
				if (twoLeft.equals("*")) {
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					} 
					else if (forward.equals("*")) {
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
			if (right != null) {
				if (right.equals("*") || right.equals("@")) {// checks one space
																// right
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					} else if (forward.equals("*") || right.equals("@")) {
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
				if (back.equals("*")) {
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					} else if (forward.equals("*")) {
						System.out.print("In a corner, turning right!");
						heading = "l";
					}
				}
			} /*else if (visField[CURRENT_ROW - 1][CURRENT_COL + 1] != null) {
				System.out.println("Rear View blocked");
				if (visField[CURRENT_ROW - 1][CURRENT_COL - 1].equals("*")) {
					heading = "l";
				}
			}*/

		}
		return heading;
	}

	private String grabItem(String[][] visField, String ground) {
		String hammer = null;
		int i, j;
		for (i = 0; i < 7; i++) {
			for (j = 0; j < 5; j++) {
				if (visField[i][j] != null) {
					if (visField[i][j].equals("T") || visField[i][j].equals("K")) {
						System.out.println("Hammer Found");
						if (CURRENT_ROW - i > 0) {
							System.out.println("Moving Forward towards item.");
							hammer = "f";
						} else if (CURRENT_ROW - i < 0) {
							System.out.println("Moving Back towards item.");
							hammer = "b";
						} else if (CURRENT_COL - j > 0) {
							System.out.println("Moving Left towards item.");
							hammer = "l";
						} else if (CURRENT_COL - j < 0) {
							System.out.println("Moving Right towards item.");
							hammer = "r";
						}
					} else if (ground != null) {
						if (ground.contains("T") || ground.contains("K")) {
							System.out.println("Grabbing Item.");
							hammer = "g";
						}
					}
					// check wall for direction
					if (hammer != null) {
						if (!hammer.equals("g")) {
							System.out.println("Gets to item chack");
							hammer = checkWall(visField, hammer);
						}
					}
				}
			}
		}
		return hammer;
	}

	private String useWeapon(String[][] visField, String inventory) {
		System.out.println("Use Weapon inventory is ");
		System.out.println(inventory);
		String action = null;
		if (visField[CURRENT_ROW - 1][CURRENT_COL] != null && !inventory.equals("()")) {
			if (visField[CURRENT_ROW - 1][CURRENT_COL].equals("@") || visField[CURRENT_ROW - 1][CURRENT_COL].equals("#")) {
				System.out.println("Non-Wall Found");
				action = "u";
			}
		}
		return action;
	}

	private String hugWall(String[][] visField) {
		// declare local variables
		Random r = new Random();
		int rand = 0;
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;
		if (left != null) {
			if (left.equals("*") || left.equals("@") || left.equals("#")) {// checks one space left
				System.out.println("Wall hugging to left");
				System.out.println(forward + " = forward");
				System.out.println(left + " = left");
				if (forward == null) {
					System.out.println("Let's hug wall moving forward");
					heading = "f";
				} else if (forward.equals("*") || left.equals("@") || forward.equals("@") || left.equals("*") || forward.equals("#") || left.equals("#")) {
					System.out.print("In a corner, turning right!");
					heading = "r";
				}
			}
		}

		if (right != null) {
			if (right.equals("*") || right.equals("@") || right.equals("#")) {// checks one space
															// right
				System.out.println("Wall hugging is right");
				System.out.println(forward + " = forward");
				System.out.println(right + " = right");
				if (forward == null) {
					System.out.println("Let's hug wall moving forwad");
					heading = "f";
				} else if (forward.equals("*") || right.equals("@") || forward.equals("@") || right.equals("*") || forward.equals("#") || right.equals("#")) {
					System.out.print("In a corner, turning left!");
					heading = "l";
				}
			}
		}
		return heading;
	}

	private static String checkBoulder(String visField[][]) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;
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
	
	private static String checkKey(String visField[][]) {
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		String heading = null;
		if (forward != null) {
			if (forward.equals("K")) {
				heading = "u";
			}
		} else if (left != null) {
			if (left.equals("K")) {
				heading = "l";
			}
		} else if (right != null) {
			if (right.equals("K")) {
				heading = "r";
			}
		} else if (back != null) {
			if (back.equals("K")) {
				heading = "b";
			}
		}
		return heading;
	}

	public static void main(String[] args) {
		SRAgent agent = new SRAgent("localhost", MAEDENPORT);
		agent.run();
	}

	private static class State {
		int orientation = 0;
		String heading;
		String inv;
		String groundContents;
		int energy;
		String lastAction;
		String[][] visField;

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
