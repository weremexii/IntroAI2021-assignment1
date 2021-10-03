package controllers.limitdepthfirst;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Stack;


import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;


public class Agent extends AbstractPlayer{
    /**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];
    protected int block_size;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();

    }



    public Types.ACTIONS act(StateObservation state, ElapsedCpuTimer elapsedTimer) {
        double avgTime = 0;
        double totalTime = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int iterTime = 0;
        int remainingLimit = 5;

        DepthLimitController controller = new DepthLimitController(state.copy());
        Types.ACTIONS action = null;
        while(remaining > 2*avgTime && remaining > remainingLimit) { // time limit code
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            controller.roll();
            action = controller.getAction();


            // time limit code
            iterTime++;
            totalTime += elapsedTimerIteration.elapsedMillis();
            avgTime = totalTime/iterTime;
            remaining = elapsedTimer.remainingTimeMillis();
        }
        return action;
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

class DepthLimitController {
    ArrayList<Types.ACTIONS> avails;
    ArrayList<Integer> actables = new ArrayList<>();
    ArrayList<StackTree> trees = new ArrayList<>();
    public DepthLimitController(StateObservation stateCopy) {
        this.getActables(stateCopy);
        for(int i = 0; i < actables.size(); i++) {
            StackTree tree = new StackTree(stateCopy, avails.get(actables.get(i)));
            trees.add(tree);
        }
    }
    public void roll() {
        for(int i = 0; i < trees.size(); i++) {
            StackTree tree = trees.get(i);
            tree.expand();
        }
    }
    public Types.ACTIONS getAction() {
        StackTree m = trees.stream().min(Comparator.comparing(StackTree::getPoint)).orElse(null);
        if (m == null) {
            return null;
        }
        else {
            return m.getAction();
        }
    }
    private void getActables(StateObservation state) {
        this.avails = state.getAvailableActions();

        for (int i = 0; i < avails.size(); i++) {
            StateObservation forTest = state.copy();
            forTest.advance(this.avails.get(i));
            if (!forTest.equalPosition(state)) {
                this.actables.add(i);
            }
        }
    }
}

class StackTree {
    Stack<Node> fringe = new Stack<>();
    StateObservation rootState;
    StateObservation inProcess;
    public StackTree(StateObservation stateCopyWithoutCand, Types.ACTIONS candidate) {
        this.rootState = stateCopyWithoutCand.copy();
        this.rootState.advance(candidate);
        this.inProcess = this.rootState.copy();

        Node rootNode = new Node(candidate, this.inProcess.copy(), null, 0);
        this.fringe.push(rootNode);
    }
    public void expand() {
        if(this.fringe.peek().isActable() && this.fringe.peek().depth < 5) {
            Node select = this.fringe.peek();
            Types.ACTIONS expandAction = select.getAction();
            this.inProcess.advance(expandAction);
            Node expandNode = new Node(expandAction, this.inProcess.copy(), this.fringe.get(0), select.depth+1);
            this.fringe.push(expandNode);
        }
        else {
            if(this.fringe.size() > 1) {
                this.fringe.pop();
                this.inProcess = this.rebuild();
                this.expand();
            }
            else { // expand all
                return;
            }
        }
    }
    public double getPoint() {
        return this.fringe.get(0).point;
    }
    public Types.ACTIONS getAction() {
        return this.fringe.get(0).action;
    }
    private StateObservation rebuild() {
        StateObservation result = this.rootState.copy();
        for(int i = 1; i < this.fringe.size(); i++) {
            result.advance(this.fringe.get(i).action);
        }
        return result;
    }
}

class Node {
    Types.ACTIONS action;
    ArrayList<Types.ACTIONS> avails;
    ArrayList<Integer> actables = new ArrayList<>();

    Node candidateParent = null;
    double value;
    int depth;
    // for candidate
    double point = 0.0;

    // stateCopy is a copy of state which has taken action
    public Node(Types.ACTIONS action, StateObservation stateCopy, Node candidate, int depth){
        this.action = action;
        this.getActables(stateCopy);
        //
        this.value = this.getValue(stateCopy);
        this.depth = depth;
        //
        this.candidateParent = candidate;
        this.passValue(stateCopy);
    }

    public Types.ACTIONS getAction() {
        if (this.actables.isEmpty()) {
            return null;
        }
        else {
            return this.avails.get(this.actables.remove(0));
        }
    }

    public boolean isActable() {
        return this.actables.size() != 0;
    }
    public boolean isCandidate() { return this.candidateParent == null; }

    private void getActables(StateObservation state) {
        this.avails = state.getAvailableActions();

        for (int i = 0; i < avails.size(); i++) {
            StateObservation forTest = state.copy();
            forTest.advance(this.avails.get(i));
            if (!forTest.equalPosition(state)) {
                this.actables.add(i);
            }
        }
    }
    private double getValue(StateObservation state) {
        ArrayList<Observation>[] fixedPositions = state.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = state.getMovablePositions();
        Vector2d goalpos = fixedPositions[1].get(0).position; //目标的坐标
        Vector2d keypos = movingPositions[0].get(0).position; //钥匙的坐标
        Vector2d avatarpos = state.getAvatarPosition();

        int measurement; // 1 for no key and 4 for having key
        if (state.getAvatarType() == 4) {measurement = 1;}
        else { measurement = 0; }

        double distKey = Math.abs(keypos.copy().subtract(avatarpos).x)
                        + Math.abs(keypos.copy().subtract(avatarpos).y);

        Vector2d target;
        if (measurement == 0) {target = keypos;}
        else {target = avatarpos;}
        double distGoal = Math.abs(goalpos.copy().subtract(target).x)
                + Math.abs(goalpos.copy().subtract(target).y);

        return (1 - measurement)*distKey + distGoal;

    }

    private void passValue(StateObservation state) {
        // for no-candidate
        if (this.candidateParent != null ) {
            if (state.getGameWinner() == Types.WINNER.PLAYER_WINS ) { this.candidateParent.point = Double.NEGATIVE_INFINITY; }
            else if (this.candidateParent.value > this.value) { this.candidateParent.point++; }
        }
        // for candidate
        else {
            if (state.getGameWinner() == Types.WINNER.PLAYER_WINS) { this.point = Double.NEGATIVE_INFINITY; }
        }
    }
}