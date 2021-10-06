package controllers.Astar;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class Agent extends AbstractPlayer{
    //boolean init = false;
    static int indexGoal=0;
    static int indexKey=0;
    static int indexMush=0;
    static int indexHole=0;
    static int totalHole=0;

    //final int limit = 10;
    //Result lastResult = null;
    HashSet<Integer> oldState = new HashSet<>();
    Stack<StateObservation> closed = new Stack<>();
    //HashSet<Integer> explorer = new HashSet<>();

    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // init
        this.getIndex(stateObs);
    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        // init
        //if(!this.init) {this.getIndex(stateObs);this.init=true;}
        this.oldState.add(Arrays.deepHashCode(stateObs.getObservationGrid()));
        this.closed.push(stateObs.copy());
        //if(this.lastResult == null || this.lastResult.path.size() == 0) {
        //    this.lastResult = this.treeSearch(stateObs);
        //}
        Types.ACTIONS action = null;
        //this.explorer.addAll(this.oldState);
        Result newResult = this.treeSearch(stateObs);
        // pop null
        while (newResult.path.peek() == null) {
            newResult.path.pop();
        }
        action = newResult.path.pop();
        return action;
    }

    public Result treeSearch(StateObservation stateObs) {
        Node root = new Node(null, stateObs, 0);
        Result searchResult = this.Astar(root, stateObs);
        //assert(searchResult.path.pop() == null); // pop the root
        return searchResult;
    }

    public Result Astar(Node node, StateObservation stateObs) {
            // goal-test
            ;
            ArrayList<Result> results = new ArrayList<>();
            for (Integer i: this.expand(stateObs)) {
                Types.ACTIONS action = stateObs.getAvailableActions().get(i);
                StateObservation stateCop = stateObs.copy();
                stateCop.advance(action);

                //this.explorer.add(Arrays.deepHashCode(stateCop.getObservationGrid()));

                Node newNode = new Node(action, stateCop, node.depth+1);
                results.add(new Result(newNode.action, newNode.score));
            }
            if (results.isEmpty())
                return new Result(node.action, node.score);
            else {
                Result maxresult = results.stream().min(Comparator.comparing(Result::getMaxScore)).get();
                return maxresult;
            }

    }

    public ArrayList<Integer> expand(StateObservation stateObs) {


        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        ArrayList<Integer> actual = new ArrayList<>();
        for(int i = 0; i < actions.size(); i++) {
            StateObservation stateCop = stateObs.copy();
            stateCop.advance(actions.get(i));
            //if(!stateCop.equalPosition(stateObs) && !this.oldState.contains(Arrays.deepHashCode(stateCop.getObservationGrid())) )
            if(!stateCop.equalPosition(stateObs) && !this.compareClosed(stateCop))
                actual.add(i);
        }
        return actual;
    }

    public boolean compareClosed(StateObservation stateObs) {
        for(StateObservation stateClosed: this.closed){
            if(stateClosed.equalPosition(stateObs))
                return true;
        }
        return false;
    }

    public void getIndex(StateObservation stateObs) {
        final int goal = 7;
        final int key = 6;
        final int mushroom = 5;
        final int hole = 2;
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        for (int i = 0; i < fixedPositions.length; i++) {
            ArrayList<Observation> fixedPosition = fixedPositions[i];
            if (!fixedPosition.isEmpty()) {
                switch (fixedPosition.get(0).itype) {
                    case goal -> Agent.indexGoal = i;
                    case hole -> {
                        Agent.indexHole = i;
                        Agent.totalHole = fixedPosition.size();
                    }
                    case mushroom -> Agent.indexMush = i;
                }
            }
        }
        for (int i = 0; i < movingPositions.length; i++) {
            ArrayList<Observation> movingPosition = movingPositions[i];
            if (!movingPosition.isEmpty()) {
                if (movingPosition.get(0).itype == key) {
                    Agent.indexKey = i;
                    break;
                }
            }
        }

    }
}

class Node {
    Types.ACTIONS action;
    int depth;
    float score;
    public Node(Types.ACTIONS action, StateObservation stateObs, int depth) {
        this.action = action;
        this.depth = depth;
        this.score = this.calScore(stateObs);
    }

    private float calScore(StateObservation stateObs) {
        if(stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES)
            return 100000;
        else if(stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS)
            return 0;
        else {
            ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
            ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
            Vector2d avatarpos = stateObs.getAvatarPosition();
            if(!movingPositions[Agent.indexKey].isEmpty()){
                Vector2d keypos = movingPositions[Agent.indexKey].get(0).position;
                double distKey = Math.abs(keypos.copy().subtract(avatarpos).x)
                        + Math.abs(keypos.copy().subtract(avatarpos).y);
                distKey = distKey/50;
                return this.depth + this.calH(stateObs) + (float)distKey;
            }
            else {
                return 0.0F;
            }
        }
    }

    private float calH(StateObservation stateObs) {
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
        final float base = 100.0F;
        int burdenKey = -100; //default value for non-hole
        float payoffHole = 0.0F;
        if (Agent.indexHole != 0 && Agent.totalHole != 0) {
            payoffHole = (float) ((burdenKey/2)/Agent.totalHole);
            burdenKey = burdenKey/2;
        }
        if (!movingPositions[Agent.indexKey].isEmpty() ) { // key exists
            if (Agent.indexHole != 0)
                return base + burdenKey + payoffHole*fixedPositions[Agent.indexHole].size();
            else
                return base + burdenKey;
        }
        else
            return 0.0F;
    }
}

class Result {
    Stack<Types.ACTIONS> path = new Stack<>();
    float maxScore;
    public Result(Types.ACTIONS action, float score) {
        this.path.push(action);
        this.maxScore = score;
    }

    public float getMaxScore() {
        return maxScore;
    }
}