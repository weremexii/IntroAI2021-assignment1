package controllers.limitdepthfirst;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class Agent extends AbstractPlayer{

    final int limit = 5; // the depth limit
    Result lastResult = null; // to store the explored path generated from the depthlimit search
    Stack<StateObservation> closed = new Stack<>(); // the previous state

    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { // constructor
        ;
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
        this.closed.push(stateObs.copy());
        Types.ACTIONS action = null;

        // lastResult is empty and re-search for path
        if (this.lastResult == null || this.lastResult.path.size() == 0)
            this.lastResult = this.treeSearch(stateObs);

        // pop null action in lastResult
        while (this.lastResult.path.peek() == null) {
            this.lastResult.path.pop();
        }
        action = this.lastResult.path.pop();
        return action;
    }

    public Result treeSearch(StateObservation stateObs) { // call recursiveDLS
        Node root = new Node(null, stateObs, 0);
        Result searchResult = this.recursiveDLS(root, stateObs);
        return searchResult;
    }

    public Result recursiveDLS(Node node, StateObservation stateObs) {
        if (node.depth == this.limit) // reach the limit depth
            return new Result(node.action, node.score); // directly return the node
        else {
            ArrayList<Result> results = new ArrayList<>();
            for (Integer i: this.expand(stateObs)) { // expand the introducted node
                Types.ACTIONS action = stateObs.getAvailableActions().get(i);
                StateObservation stateCop = stateObs.copy();
                stateCop.advance(action);

                Node newNode = new Node(action, stateCop, node.depth+1);
                results.add(recursiveDLS(newNode, stateCop)); // recursively add results
            }
            if (results.isEmpty())
                return new Result(node.action, node.score);
            else {
                Result maxresult = results.stream().min(Comparator.comparing(Result::getMaxScore)).get(); // return the max score result
                maxresult.path.push(node.action);
                return maxresult;
            }
        }
    }

    public ArrayList<Integer> expand(StateObservation stateObs) {


        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        ArrayList<Integer> actual = new ArrayList<>(); // store the actions that actually make changes
        for(int i = 0; i < actions.size(); i++) {
            StateObservation stateCop = stateObs.copy();
            stateCop.advance(actions.get(i));
            //if(!stateCop.equalPosition(stateObs) && !this.oldState.contains(Arrays.deepHashCode(stateCop.getObservationGrid())) )
            if(!stateCop.equalPosition(stateObs) && !this.compareClosed(stateCop)) // compare to the origin state and the old states
                actual.add(i);
        }
        return actual;
    }

    public boolean compareClosed(StateObservation stateObs) { // as named
        for(StateObservation stateClosed: this.closed){
            if(stateClosed.equalPosition(stateObs))
                return true;
        }
        return false;
    }


}

class Node {
    // index table for itype
    // use Observation.itype the Agent can judge what's the wanted index of wanted Observation
    int indexGoal=-1;
    int indexKey=-1;
    int indexMush=-1;
    int indexHole=-1;
    int totalHole=-1;

    Types.ACTIONS action;
    int depth;
    float score;
    public Node(Types.ACTIONS action, StateObservation stateObs, int depth) {
        this.action = action;
        this.depth = depth;
        this.getIndex(stateObs); // init, use itype to get index
        this.score = this.calScore(stateObs); // construct to calculate score
    }

    private float calScore(StateObservation stateObs) { // make Avatar to find the key
        if(stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES)
            return 100000;
        else if(stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS)
            return 0;
        else {
            ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();
            Vector2d avatarpos = stateObs.getAvatarPosition();
            if(this.indexKey != -1){
                Vector2d keypos = movingPositions[this.indexKey].get(0).position;
                double distKey = Math.abs(keypos.copy().subtract(avatarpos).x)
                        + Math.abs(keypos.copy().subtract(avatarpos).y);
                distKey = distKey/50;
                return (float)distKey;
            }
            else {
                return 0.0F;
            }
        }
    }
    private void getIndex(StateObservation stateObs) { // get Observation object index
        // the fixed itype index
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
                    case goal -> this.indexGoal = i;
                    case hole -> {
                        this.indexHole = i;
                        this.totalHole = fixedPosition.size();
                    }
                    case mushroom -> this.indexMush = i;
                }
            }
        }
        for (int i = 0; i < movingPositions.length; i++) {
            ArrayList<Observation> movingPosition = movingPositions[i];
            if (!movingPosition.isEmpty()) {
                if (movingPosition.get(0).itype == key) {
                    this.indexKey = i;
                    break;
                }
            }
        }

    }
}

class Result { // a implement contains the generated path to get the max score
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
