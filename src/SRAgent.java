import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;


public class SRAgent {
	
	private static final int MAEDENPORT = 7237;
	
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
		return state.heading;
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
					newVisField[i][j] = "" + item.charValue();
				}
			}
		}
		
		return newVisField;
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
