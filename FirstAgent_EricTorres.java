package CardPickup;

import com.sun.org.apache.xerces.internal.xs.StringList;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Some important variables inherited from the Player Class:
 * protected Node[] graph; //Contains the entire graph
 * protected Hand hand; //Contains your current hand (Use the cardsHole array list)
 * protected int turnsRemaining; //Number of turns before the game ends
 * protected int currentNode; //Your current location
 * protected int oppNode; //Opponent's current position
 * protected Card oppLastCard;	//Opponent's last picked up card
 * <p>
 * Important methods inherited from Player Class:
 * Method that is used to determine if a move is valid. This method should be used to help players
 * determine if their actions are valid. GameMaster has a local copy of this method and all the
 * required variables (such as the true graph), so manipulating the variables to turn a previously
 * invalid action in to a "valid" one will not help you as the GameMaster will still see the action
 * as invalid.
 * protected boolean isValidAction(Action a); //This method can be used to determine if an action is valid
 * <p>
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
public class FirstAgent_EricTorres extends Player {
    protected final String newName = "NotDumb"; //Overwrite this variable in your player subclass

    //Keep the list of known cards
    ArrayList<Card> known;

    //Keep the list of possible cards left
    float[] estValues;

    //Ordered nodes. Array of ordered nodes
    ArrayList<HeuristicNode> sorted;

    HandEvaluator evaluator = new HandEvaluator();

    /**
     * Do not alter this constructor as nothing has been initialized yet. Please use initialize() instead
     */
    public FirstAgent_EricTorres() {
        super();
        playerName = newName;
    }

    public void initialize() {
        //WRITE ANY INITIALIZATION COMPUTATIONS HERE

        /**First basic idea: Compare cards in hand to graph nodes; remove those cards as possibilities.
         * For this the agent must traverse the graph entirely - maybe multiple times.
         */

        //Initialize node values
        estValues = new float[graph.length];

        //Initialize sorted list of nodes by estimate value
        sorted = new ArrayList<>();

        //Set known
        known = new ArrayList<>();
        for (int i = 0; i < hand.getNumHole(); i++) {
            known.add(hand.getHoleCard(i));
        }

        updateKnownAndGraphAndValues();

//        printIntel();
    }

    /**
     * THIS METHOD SHOULD BE OVERRIDDEN if you wish to make computations off of the opponent's moves.
     * GameMaster will call this to update your player on the opponent's actions. This method is called
     * after the opponent has made a move.
     *
     * @param opponentNode     Opponent's current location
     * @param opponentPickedUp Notifies if the opponent picked up a card last turn
     * @param c                The card that the opponent picked up, if any (null if the opponent did not pick up a card)
     */
    protected void opponentAction(int opponentNode, boolean opponentPickedUp, Card c) {
        oppNode = opponentNode;
        if (opponentPickedUp)
            oppLastCard = c;
        else
            oppLastCard = null;

        //Add the picked up card to known and update graph and est values
        if (c != null) {
            known.add(c);
            graph[opponentNode].clearPossibleCards();
            updateKnownAndGraphAndValues();
        }
    }

    /**
     * THIS METHOD SHOULD BE OVERRIDDEN if you wish to make computations off of your results.
     * GameMaster will call this to update you on your actions.
     *
     * @param currentNode Opponent's current location
     * @param c           The card that you picked up, if any (null if you did not pick up a card)
     */
    protected void actionResult(int currentNode, Card c) {
        this.currentNode = currentNode;
        if (c != null)
            addCardToHand(c);

        //Add the picked up card to known and update graph and est values
        if (c != null) {
            known.add(c);
            graph[currentNode].clearPossibleCards();
            updateKnownAndGraphAndValues();
        }
    }

    /**
     * Player logic goes here
     */
    public Action makeAction() {

//        printIntel();

        //If running out of turns just pick up
        if (5 - hand.getNumHole() + hand.getNumUp() < turnsRemaining + 1) {
            //Card already picked, move instead
            if(estValues[currentNode] == 0.0)
                return new Action(ActionType.MOVE, randomNodeId());
            else
                return new Action(ActionType.PICKUP, graph[currentNode].getNodeID());
        }

        //If current node is within top estimate values, pick it up. Otherwise, move towards best.
        if (isInTop(graph[currentNode], 4)) {
            return new Action(ActionType.PICKUP, graph[currentNode].getNodeID());
        }

        //move to random node
        int indexOfBestNode = sorted.get(0).node.getNodeID();
        return new Action(ActionType.MOVE, randomNodeId());
    }

    /**
     * Update the known card list, and the possible cards of each node
     */
    public void updateKnownAndGraphAndValues() {
        boolean reRun = false;

        //traverse entire graph, remove impossible cards from each node
        for (int nodeIndex = 0; nodeIndex < graph.length; nodeIndex++) {
            ArrayList<Card> cardsInNode = graph[nodeIndex].getPossibleCards();

            //Update the estimated value of picking up this node
            if (cardsInNode.size() == 0) {
                //no value since can't pickup
                estValues[nodeIndex] = 0.0f;
            } else {
                float prob = 1.0f / cardsInNode.size();
                float value = 0.0f;
                Hand ghostHand = new Hand();

                for (int i = 0; i < cardsInNode.size(); i++) {

                    //Add hole cards on every estimation
                    for (int j = 0; j < hand.getNumHole(); j++) {
                        ghostHand.addHoleCard(hand.getHoleCard(j));
                    }

                    ghostHand.addUpCard(cardsInNode.get(i));
                    value += prob * evaluator.rankHand(ghostHand);

                    //Clear the hand per possible card
                    ghostHand.clearHand();
                }

                //Add probable value
                estValues[nodeIndex] = value;
            }

            if (cardsInNode.size() == 1) {
                //node is known, skip this node
                continue;
            }

            //Clear cards for update
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

        if (reRun)
            updateKnownAndGraphAndValues();

        //Sort the finished estimate values
        sorted.clear();
        for (int i = 0; i < estValues.length; i++) {
            sorted.add(new HeuristicNode(graph[i], estValues[i]));
        }
        Collections.sort(sorted, new HeuristicNode(null, 0));
    }

    /**
     * Returns anti-intersection of two Card ArrayLists (The cards that don't match)
     */
    public ArrayList<Card> containsKnown(ArrayList<Card> known, ArrayList<Card> node) {
        ArrayList<Card> difference = (ArrayList<Card>) node.clone();

        //For every known card
        for (int i = 0; i < known.size(); i++) {

            //for every possible card
            for (int j = 0; j < node.size(); j++) {
                if(j >= difference.size())
                    break;
                if (known.get(i).shortName().equalsIgnoreCase(node.get(j).shortName())) {
                    //remove
                    difference.remove(j);
                    continue;
                }
            }
        }

        return difference;
    }

    /**
     * Prints current player known cards and graph
     */
    public void printIntel() {
        //print known
        System.out.print("Known cards: ");
        for (int i = 0; i < known.size(); i++) {
            System.out.print(known.get(i).shortName() + ", ");
        }
        System.out.println("");

        //print graph
        for (int i = 0; i < graph.length; i++) {
            System.out.print("Node: " + i + "  Est. Val: " + estValues[i] + ", ");

            ArrayList<Card> possible = graph[i].getPossibleCards();
            for (int j = 0; j < possible.size(); j++) {
                System.out.print(possible.get(j).shortName() + ", ");
            }

            System.out.println("");
        }

        System.out.print("Sorted List: ");

        //print sorted
        for (HeuristicNode i : sorted) {
            System.out.print(i.node.getNodeID() + ", ");
        }
        System.out.println("\n");
    }

    /**
     * Returns if a node is within top estimated values (number given as parameter)
     */
    public boolean isInTop(Node any, int top) {
        for (HeuristicNode i : sorted) {
            if (top < 0)
                break;
            if (i.node.getNodeID() == any.getNodeID())
                return true;
            top--;
        }
        return false;
    }

    /**
     * Returns a random neighbor node id
     */
    public int randomNodeId() {
        Random r = new Random();

        ArrayList<Node> neighbors = graph[currentNode].getNeighborList();
        int rInd = r.nextInt(neighbors.size());

        return neighbors.get(rInd).getNodeID();
    }

    /**
     * TODO: Fix bugs to implement
     *
     * @param findMe: index of node to go to
     * @return nodeID of node to move to in order to get to wanted node.
     */
    public HeuristicNode findPathToNode(int findMe) {
        PriorityQueue visited = new PriorityQueue();
        PriorityQueue fringe = new PriorityQueue();
        HeuristicNode start = new HeuristicNode(graph[currentNode], 0);
        fringe.enqueue(start);
        visited.enqueue(start);

        HeuristicNode current;
        HeuristicNode solution = null;

        //while fringe isn't empty
        while (!fringe.isEmpty()) {
            current = fringe.dequeue();
            System.out.println("Current: " + current.node.getNodeID());
            System.out.println("Current: " + System.currentTimeMillis());

            //changed to 'continue if solution has higher cost than other evaluated solutions'
            //instead of just 'if goal node found, return it'
            if (current.node.getNodeID() == graph[findMe].getNodeID()) {
                if (current.val < fringe.peekEvaluation())
                    return current;
                else
                    solution = current;
            }

            //get children of the current node
            ArrayList<Node> ghostChildren = graph[currentNode].getNeighborList();

            //check current node's children and add to queue
            for (Node temp : ghostChildren) {
                HeuristicNode temp2 = new HeuristicNode(temp, 1);

                if (!visited.hasNode(temp2)) {

                    visited.enqueue(temp2);

                    temp2.parent = current;

                    //add up cost of nodes up to this node cumulatively
                    temp2.val += current.val;
                    //add heuristic to child Node before the priority enqueue
                    temp2.f = 0;

                    fringe.enqueue(temp2);
                }

            }
        }

        //no solution found
        return solution;
    }

    public class HeuristicNode implements Comparator<HeuristicNode> {
        //data
        Node node;
        float val; //cost of node
        double f; //heuristic estimate

        //fringe part
        HeuristicNode next;
        HeuristicNode parent;

        public HeuristicNode(Node node, float val) {
            this.node = node;
            this.val = val;
            this.f = 0;

            next = null;
            parent = null;
        }

        @Override
        public int compare(HeuristicNode x1, HeuristicNode x2) {
            return (int) x2.val - (int) x1.val;
        }
    }

    public class PriorityQueue {
        private HeuristicNode head; //head will always be smallest node

        public PriorityQueue() {
            head = null;
        }

        public double peekEvaluation() {
            if (isEmpty())
                return 0;
            return (head.val + head.f);
        }

        public void enqueue(HeuristicNode ghost) {
            if (isEmpty()) {
                head = ghost;
                return;
            }

            HeuristicNode temp = head;
            HeuristicNode previous = null;

            //if its smaller than the head
            if ((ghost.f + ghost.val) < (temp.val + temp.f)) {
                ghost.next = head;
                head = ghost;
                return;
            }

            //otherwise iterate until a spot for the Node is found
            while (temp.next != null) {
                previous = temp;
                temp = temp.next;

                if ((ghost.val + ghost.f) < (temp.val + temp.f)) {
                    previous.next = ghost;
                    ghost.next = temp;
                    return;
                }
            }

            temp.next = ghost;
        }

        public HeuristicNode dequeue() {
            if (isEmpty())
                return null;

            HeuristicNode temp = head;
            if (head.next == null) {
                head = null;
                return temp;
            }

            head = head.next;
            return temp;
        }

        public boolean isEmpty() {
            return head == null;
        }

        public void printQueue() {
            HeuristicNode temp = head;
            while (temp != null) {
                System.out.println("" + (temp.val + temp.f));
                temp = temp.next;
            }
        }

        public boolean hasNode(HeuristicNode ghost) {
            if (head == null)
                return false;

            HeuristicNode temp = head;
            while (temp != null) {
                if (temp.node.getNodeID() == ghost.node.getNodeID())
                    return true;
                temp = temp.next;
            }
            return false;
        }

        public int getSize() {
            if (isEmpty())
                return 0;

            HeuristicNode temp = head;
            int count = 0;
            while (temp != null) {
                count++;
                temp = temp.next;
            }

            return count;
        }
    }


}