package controllers.depthfirst;

import java.util.ArrayList;
import java.util.Stack;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class Agent extends AbstractPlayer{
    protected StackTree tree; //LIFO implement

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        tree = new StackTree(so.copy());
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        return this.tree.act();
    }

}

class StackTree {
    Stack<Node> fringe = new Stack<>();
    StateObservation rootState; // backup the state for later use
    StateObservation inProcess; // a state used for expanding nodes
    boolean success = false;
    int actPointer = 0; // a index to indicate what's the next action for Agent.act()
    public StackTree(StateObservation state) { // constructor
        this.rootState = state.copy();
        this.inProcess = state.copy();
        Node rootNode = new Node(null, state.copy()); // copy a state to node for judging whether it is actable
        if (rootNode.isActable()) {
            this.fringe.push(rootNode);
        }
        this.success = this.Search();
    }

    public boolean Search() {
        if (this.fringe.isEmpty()) return false;
        else {
            // main loop to search the winning state
            while(this.inProcess.getGameWinner() != Types.WINNER.PLAYER_WINS && this.fringe.size() > 0) {
                Node select = this.fringe.peek();
                //if (select.isActable() && this.inProcess.getGameTick() < 100) {
                //    Types.ACTIONS action = select.getAction();
                //    this.inProcess.advance(action);
                //    this.fringe.push(new Node(action, this.inProcess.copy()));
                //}
                if (select.isActable()) {
                    Types.ACTIONS action = select.getAction();
                    StateObservation forCheck = this.inProcess.copy(); // copy a state to check whether this action gets a repeat state
                    forCheck.advance(action);
                    if (!this.checkRepeat(forCheck)) {
                        this.inProcess.advance(action);
                        this.fringe.push(new Node(action, this.inProcess.copy()));

                    }
                }
                else { // the peak node can't expand, pop it the rebuild the inProcess state
                    // go back
                    this.fringe.pop();
                    this.inProcess = this.rebuild();
                }
            }

            if (this.fringe.isEmpty() || this.inProcess.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
                //System.out.println("false");
                return false;
            }
            else {
                //System.out.println("true, the step is" + (this.fringe.size() - 1));
                return true;
            }
        }
    }

    public Types.ACTIONS act() { // for Agent.act()
        actPointer++;
        //System.out.println(actPointer);
        if (actPointer < this.fringe.size()) {
            return this.fringe.get(actPointer).action;
        }
        else {
            return null;
        }
    }
    private StateObservation rebuild() { // to rebuild the inProcess state
        StateObservation result = this.rootState.copy();
        for(int i = 1; i < this.fringe.size(); i++) {
            result.advance(this.fringe.get(i).action);
        }
        return result;
    }
    private boolean checkRepeat(StateObservation advanceState) {
        boolean result = false;
        StateObservation forCheck = this.rootState.copy();
        for(int i = 1; i < this.fringe.size() - 1; i++) {
            forCheck.advance(this.fringe.get(i).action);
            if (forCheck.equalPosition(advanceState)) {
                result = true;
                break;
            }
        }
        return result;
    }
}

class Node {
    //static Random randomGenerator = new Random();
    Types.ACTIONS action;
    ArrayList<Types.ACTIONS> avails; // to store the Types.ACTION instances
    ArrayList<Integer> actables = new ArrayList<>(); // to indicate which ACTION is actable
    public Node(Types.ACTIONS action, StateObservation stateCopy){ // constructor
        this.action = action;
        this.getActables(stateCopy);
    }

    public Types.ACTIONS getAction() {
        if (this.actables.isEmpty()) {
            return null;
        }
        else {
            //int index = randomGenerator.nextInt(this.actables.size());
            return this.avails.get(this.actables.remove(0));
        }
    }

    public boolean isActable() {
        return this.actables.size() != 0;
    }

    private void getActables(StateObservation state) { // use the introducted state to judge the actable actions
        this.avails = state.getAvailableActions();

        for(int i = 0; i < avails.size(); i++) {
            StateObservation forTest = state.copy();
            forTest.advance(this.avails.get(i));
            if (!forTest.equalPosition(state)) {
                this.actables.add(i);
            }
        }
    }
}

