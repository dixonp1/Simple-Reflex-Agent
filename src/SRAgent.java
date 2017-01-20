import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.Random;

public class SRAgent {
	
	private static final int MAEDENPORT = 7237;
	private static final int CURRENT_COL = 2;//2
	private static final int CURRENT_ROW = 5;//1
	protected GridClient gc;
	
	
	public SRAgent(String h, int p){
		gc = new GridClient(h,p);
	}
	
	private void run(){
		String[] percept;
		State state;
		String action;
		
		while(true){
			state = getState();
			action = getAction(state);
			performAction(action);
		}		
	}
	private void performAction(String action){
		//used for DEBUGGIN
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
		//*****
		
		switch(action.charAt(0)){
			case 'f':
				//move forward
				gc.effectorSend("f");
				System.out.println("forward");
				break;
			case 'b':
				/*
				//move backwards
				gc.effectorSend("b");
				System.out.println("back");
				*/
				
				//turn around
				gc.effectorSend("l");
				System.out.println("turned left");
				break;
			case 'r':
				//more right
				gc.effectorSend("r");
				System.out.println("right");
				break;
			case 'l':
				//move left
				gc.effectorSend("l");
				System.out.println("left");
				break;
			case 'h':
				//cheese on current space
				//grab and use cheese
				gc.effectorSend("g +");
				System.out.println("found cheese");
				gc.effectorSend("u +");
				break;
			default:
				
		}
	}
	
	private String getAction(State state){
		String action;
		action = checkWall(state.visField, state.heading);
		return action;
	}
	
	private State getState(){
		SensoryPacket sp = gc.getSensoryPacket();
		String[] senses = sp.getRawSenseData();
		
		String heading = senses[0];
		
		//sent as continuous string containing inv items
		// '+' = cheese
		String inv = senses[1];
		
		sp.processRetinalField(senses[2]);
		//ArrayList<ArrayList<Vector<Character>>> visField = sp.getVisualArray();
		String[][] visField = processVisual(sp.getVisualArray());
		/*
		//declare local variables
		Random r = new Random();
		int rand = 0;
		String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
		String right = visField[CURRENT_ROW][CURRENT_COL + 1];
		String left = visField[CURRENT_ROW][CURRENT_COL - 1];
		String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
		String back = visField[CURRENT_ROW + 1][CURRENT_COL];
		boolean wholeInWall=false;
		System.out.println(senses[0]);
		if (senses[0].equals("f")) {
			if (forward != null) {
				if (forward.equals("*")) {
					for(int i = 0; i < 2; i++) {//search two visible spots to right and left of wall for opening
						if((visField[CURRENT_ROW - 1][(CURRENT_COL + (i + 1)) % 5]) == null) {
							System.out.println((CURRENT_COL + (i + 1)) % 5);
							System.out.println("Moving right from wall");
							wholeInWall=true;
							heading = "r"; //hole is right
							i = 2; //signal end of loop we’ve found nearest hole in wall
						}
						else if((visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]) == null) {
							System.out.println("Moving left from wall");
							wholeInWall=true;
							heading = "l"; //hole is left
							i = 2; //signal end of loop we’ve found nearest hole in wall
						}
					}
					if (wholeInWall==false) {//long wall encountered
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
		else if (senses[0].equals("l")) {//let's search for holes
			System.out.println("smell is left");
			System.out.println(forward + " = forward");
			System.out.println(left + " = left");
			if (left != null) {
				if (left.equals("*")) {//checks one space left
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
			else if (twoLeft != null) {//checks two spaces left
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
		else if (senses[0].equals("b")) {//let's search for holes
			System.out.println("smell is back");
			System.out.println(back + " = back");
			if (back != null) {
				if (back.equals("*")) {
					if (forward == null) {
						System.out.println("Left Smell instinct blocked with wall");
						heading = "f";
					}
					else if (forward.equals("*")) {
						System.out.print("In a corner, turning right!");
						heading = "l";
					}
				}
			}
			else if (visField[CURRENT_ROW - 1][CURRENT_COL + 1] != null) {//one back one left trap
				if (visField[CURRENT_ROW - 1][CURRENT_COL - 1].equals("*")) {
					heading = "l";
				}
			}
		}
		*/
		
		
		String groundContents = senses[3];
		String energy = senses[5];
		String lastAction = senses[6];
		
		return new State(heading, inv, groundContents, energy, lastAction, visField);
	}
	
	private String[][] processVisual(ArrayList<ArrayList<Vector<Character>>> visField){
		String[][] newVisField = new String[7][5];
		
		for( int i=0; i<visField.size(); i++){
			for ( int j=0; j<visField.get(i).size(); j++){
				for( Character item : visField.get(i).get(j)){
					if( item.charValue() != '"'){
						newVisField[i][j] = "" + item.charValue();
					}
				}
				System.out.println(newVisField[i][j] + " " + i + " " + j);//debugging
			}
			System.out.println();//debugging
		}
		
		return newVisField;
	}
	
	private String checkWall(String[][] visField, String smell) {
		//declare local variables
				Random r = new Random();
				int rand = 0;
				String forward = visField[CURRENT_ROW - 1][CURRENT_COL];
				String right = visField[CURRENT_ROW][CURRENT_COL + 1];
				String left = visField[CURRENT_ROW][CURRENT_COL - 1];
				String twoLeft = visField[CURRENT_ROW][CURRENT_COL - 2];
				String back = visField[CURRENT_ROW + 1][CURRENT_COL];
				String heading = smell;
				boolean wholeInWall=false;
				System.out.println(smell);
				if (smell.equals("f")) {
					if (forward != null) {
						if (forward.equals("*")) {
							for(int i = 0; i < 2; i++) {//search two visible spots to right and left of wall for opening
								if((visField[CURRENT_ROW - 1][(CURRENT_COL + (i + 1)) % 5]) == null) {
									System.out.println((CURRENT_COL + (i + 1)) % 5);
									System.out.println("Moving right from wall");
									wholeInWall=true;
									heading = "r"; //hole is right
									i = 2; //signal end of loop we’ve found nearest hole in wall
								}
								else if((visField[CURRENT_ROW - 1][(CURRENT_COL - (i + 1)) % 5]) == null) {
									System.out.println("Moving left from wall");
									wholeInWall=true;
									heading = "l"; //hole is left
									i = 2; //signal end of loop we’ve found nearest hole in wall
								}
							}
							if (wholeInWall==false) {//long wall encountered
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
				else if (smell.equals("l")) {//let's search for holes
					System.out.println("smell is left");
					System.out.println(forward + " = forward");
					System.out.println(left + " = left");
					if (left != null) {
						if (left.equals("*")) {//checks one space left
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
					else if (twoLeft != null) {//checks two spaces left
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
				else if (smell.equals("b")) {//let's search for holes
					System.out.println("smell is back");
					System.out.println(back + " = back");
					if (back != null) {
						if (back.equals("*")) {
							if (forward == null) {
								System.out.println("Left Smell instinct blocked with wall");
								heading = "f";
							}
							else if (forward.equals("*")) {
								System.out.print("In a corner, turning right!");
								heading = "l";
							}
						}
					}
					else if (visField[CURRENT_ROW - 1][CURRENT_COL + 1] != null) {//one back one left trap
						if (visField[CURRENT_ROW - 1][CURRENT_COL - 1].equals("*")) {
							heading = "l";
						}
					}
				}
		return heading;
	}
	
	public static void main(String[] args){
		SRAgent agent = new SRAgent("localhost", MAEDENPORT);
		agent.run();
	}
	
	private static class State{
		int orientation = 0;	
		String heading;
		String inv;
		String groundContents;
		String energy;
		String lastAction;
		String[][] visField;
		
		public State(String heading, String inv, String groundContents, 
				String energy, String lastAction, String[][] visField){
			this.heading = heading;
			this.inv = inv;
			this.groundContents = groundContents;
			this.energy = energy;
			this.lastAction = lastAction;
			this.visField = visField;
		}
	}
}
