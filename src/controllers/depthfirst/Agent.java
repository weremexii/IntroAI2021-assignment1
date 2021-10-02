package controllers.depthfirst;

import java.awt.Graphics2D;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import core.game.Observation;
import core.game.StateObservation;
import core.competition.CompetitionParameters;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

public class Agent extends AbstractPlayer{
    /**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];
    protected int block_size;
    protected StackTree tree;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();
        // construct tree
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

    /**
     * Prints the number of different types of sprites available in the "positions" array.
     * Between brackets, the number of observations of each type.
     * @param positions array with observations.
     * @param str identifier to print
     */
    private void printDebug(ArrayList<Observation>[] positions, String str)
    {
        if(positions != null){
            System.out.print(str + ":" + positions.length + "(");
            for (int i = 0; i < positions.length; i++) {
                System.out.print(positions[i].size() + ",");
            }
            System.out.print("); ");
        }else System.out.print(str + ": 0; ");
    }

    /**
     * Gets the player the control to draw something on the screen.
     * It can be used for debug purposes.
     * @param g Graphics device to draw to.
     */
    public void draw(Graphics2D g)
    {
        int half_block = (int) (block_size*0.5);
        for(int j = 0; j < grid[0].length; ++j)
        {
            for(int i = 0; i < grid.length; ++i)
            {
                if(grid[i][j].size() > 0)
                {
                    Observation firstObs = grid[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    int print = firstObs.category; //firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i*block_size+half_block,j*block_size+half_block);
                }
            }
        }
    }
}

class StackTree {
    Stack<Node> fringe = new Stack<>();
    StateObservation rootState;
    StateObservation inProcess;
    boolean success = false;
    int actPointer = 0;
    public StackTree(StateObservation state) {
        this.rootState = state.copy();
        this.inProcess = state.copy();
        Node rootNode = new Node(null, state.copy());
        if (rootNode.isActable()) {
            this.fringe.push(rootNode);
        }
        this.success = this.Search();
    }

    public boolean Search() {
        if (this.fringe.isEmpty()) return false;
        else {

            while(this.inProcess.getGameWinner() != Types.WINNER.PLAYER_WINS && this.fringe.size() > 0) {
                Node select = this.fringe.peek();
                //if (select.isActable() && this.inProcess.getGameTick() < 100) {
                //    Types.ACTIONS action = select.getAction();
                //    this.inProcess.advance(action);
                //    this.fringe.push(new Node(action, this.inProcess.copy()));
                //}
                if (select.isActable()) {
                    Types.ACTIONS action = select.getAction();
                    StateObservation forCheck = this.inProcess.copy();
                    forCheck.advance(action);
                    if (!this.checkRepeat(forCheck)) {
                        this.inProcess.advance(action);
                        this.fringe.push(new Node(action, this.inProcess.copy()));

                    }
                }
                else {
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

    public Types.ACTIONS act() {
        actPointer++;
        //System.out.println(actPointer);
        if (actPointer < this.fringe.size()) {
            return this.fringe.get(actPointer).action;
        }
        else {
            return null;
        }
    }
    private StateObservation rebuild() {
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
    ArrayList<Types.ACTIONS> avails;
    ArrayList<Integer> actables = new ArrayList<>();
    public Node(Types.ACTIONS action, StateObservation stateCopy){
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

    private void getActables(StateObservation state) {
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

