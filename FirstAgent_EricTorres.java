package CardPickup;

import com.sun.org.apache.xerces.internal.xs.StringList;

import java.util.ArrayList;
import java.util.Random;

/**
 * Some important variables inherited from the Player Class:
 * protected Node[] graph; //Contains the entire graph
 * protected Hand hand; //Contains your current hand (Use the cardsHole array list)
 * protected int turnsRemaining; //Number of turns before the game ends
 * protected int currentNode; //Your current location
 * protected int oppNode; //Opponent's current position
 * protected Card oppLastCard;	//Opponent's last picked up card
 *
 * Important methods inherited from Player Class:
 * Method that is used to determine if a move is valid. This method should be used to help players
 * determine if their actions are valid. GameMaster has a local copy of this method and all the
 * required variables (such as the true graph), so manipulating the variables to turn a previously
 * invalid action in to a "valid" one will not help you as the GameMaster will still see the action
 * as invalid.
 * protected boolean isValidAction(Action a); //This method can be used to determine if an action is valid
 *
 * NOTE TO STUDENTS: The game master will only tell the player the results of your and your opponents actions.
 * It will not update your graph for you. That is something we left you to do so that you can update your
 * graphs, opponent hand, horoscope, etc. intelligently and however you like.

 *
 * @author Marcus Gutierrez
 * @version 04/15/2015
 */

/**
 * Modified by Eric on 4/27/2017.
 */
public class FirstAgent_EricTorres extends Player{
    protected final String newName = "NotDumb"; //Overwrite this variable in your player subclass

    //Keep the list of known cards
    ArrayList<Card> known;

    //Keep the list of possible cards left
    ArrayList<Card> unknown;

    /**Do not alter this constructor as nothing has been initialized yet. Please use initialize() instead*/
    public FirstAgent_EricTorres() {
        super();
        playerName = newName;
    }

    public void initialize() {
        //WRITE ANY INITIALIZATION COMPUTATIONS HERE

        /**First basic idea: Compare cards in hand to graph nodes; remove those cards as possibilities.
         * For this the agent must traverse the graph entirely - maybe multiple times.
         */

        //Set known
        known = new ArrayList<>();
        for(int i = 0; i < hand.getNumHole(); i++){
            known.add(hand.getHoleCard(i));
        }

        printIntel();

        updateKnownAndGraph();

        printIntel();
    }

    /**
     * THIS METHOD SHOULD BE OVERRIDDEN if you wish to make computations off of the opponent's moves.
     * GameMaster will call this to update your player on the opponent's actions. This method is called
     * after the opponent has made a move.
     *
     * @param opponentNode Opponent's current location
     * @param opponentPickedUp Notifies if the opponent picked up a card last turn
     * @param c The card that the opponent picked up, if any (null if the opponent did not pick up a card)
     */
    protected void opponentAction(int opponentNode, boolean opponentPickedUp, Card c){
        oppNode = opponentNode;
        if(opponentPickedUp)
            oppLastCard = c;
        else
            oppLastCard = null;
    }

    /**
     * THIS METHOD SHOULD BE OVERRIDDEN if you wish to make computations off of your results.
     * GameMaster will call this to update you on your actions.
     *
     * @param currentNode Opponent's current location
     * @param c The card that you picked up, if any (null if you did not pick up a card)
     */
    protected void actionResult(int currentNode, Card c){
        this.currentNode = currentNode;
        if(c!=null)
            addCardToHand(c);
    }

    /**
     * Player logic goes here
     */
    public Action makeAction() {
        printIntel();
        Random r = new Random();
        int neighbor;
        if (graph[currentNode].getNeighborAmount()==1)
            neighbor = graph[currentNode].getNeighbor(0).getNodeID();
        else
            neighbor = graph[currentNode].getNeighbor(r.nextInt(graph[currentNode].getNeighborAmount())).getNodeID();
        return new Action(ActionType.PICKUP, neighbor);
    }

    /**
     * Update the known card list, and the possible cards of each node
     */
    public void updateKnownAndGraph(){
        boolean reRun = false;

        //traverse entire graph, remove impossible cards from each node
        for(int nodeIndex = 0; nodeIndex < graph.length; nodeIndex++) {

            if(graph[nodeIndex].getPossibleCards().size() == 1) {
                //node is known, skip this node
                continue;
            }
            ArrayList<Card> cardsInNode = graph[nodeIndex].getPossibleCards();
            graph[nodeIndex].clearPossibleCards();

            //Get list containing cards in node that aren't known
            ArrayList<Card> unknownStill = containsKnown(known, cardsInNode);

            //Re-set the updated possible cards
            for (int i = 0; i < unknownStill.size(); i++) {
                graph[nodeIndex].addPossible(unknownStill.get(i));
            }

            //If there is only one card left, its certainty = 100%, so I know that card. Re-run this method.
            if (unknownStill.size() == 1) {
                known.add(unknownStill.get(0));
                reRun = true;
                break;
            }
        }

        if(reRun)
            updateKnownAndGraph();
    }

    /**
     * Returns anti-intersection of two Card ArrayLists (The cards that don't match)
     */
    public ArrayList<Card> containsKnown(ArrayList<Card> known, ArrayList<Card> node){
        ArrayList<Card> difference = (ArrayList<Card>) node.clone();

        //For every known card
        for(int i = 0; i < known.size(); i++){

            //for every possible card
            for(int j = 0; j < node.size(); j++){
                if(known.get(i).shortName() == node.get(j).shortName()){
                    //remove
                    difference.remove(j);
                    continue;
                }
                else{
//                    System.out.println(known.get(i).shortName() +"==" + node.get(j).shortName());
                }
            }
            /*
            if(!known.contains(node.get(i))){
                System.out.println("contained"+i);
                difference.add(node.get(i));
            }*/
        }

        return difference;
    }

    /**
     * Prints current player known cards and graph
     */
    public void printIntel(){
        //print known
        System.out.print("Known cards: ");
        for(int i = 0; i < known.size(); i++){
            System.out.print(known.get(i).shortName()+", ");
        }
        System.out.println("");

        //print graph
        for(int i = 0; i < graph.length; i++){
            System.out.print("Node: "+i+", ");

            ArrayList<Card> possible = graph[i].getPossibleCards();
            for(int j = 0; j < possible.size(); j++){
                System.out.print(possible.get(j).shortName()+", ");
            }

            System.out.println("");
        }

        System.out.println("");
    }
}