package controllers.Astar;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class Agent extends AbstractPlayer{

    final int limit = 4; // depth limit for indicate push boxes
    Stack<StateObservation> closed = new Stack<>();

    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        ;
    } // empty constructor

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        // record old state
        this.closed.push(stateObs.copy());

        Types.ACTIONS action = null;
        Result result = this.treeSearch(stateObs);
        // pop null
        while (!result.path.isEmpty() && result.path.peek() == null) {
            result.path.pop();
        }
        if (result.path.isEmpty()) {
            return null;
        }
        else {
            action = result.path.pop();
        }
        return action;
    }

    public Result treeSearch(StateObservation stateObs) {
        Node root = new Node(null, stateObs, 0);
        return this.Astar(root, stateObs);
    }

    public Result Astar(Node node, StateObservation stateObs) {
        if (node.depth == this.limit) {
            return new Result(node.action, node.score);
        }
        else {
            ArrayList<Result> results = new ArrayList<>();
            for (Integer i: this.expand(stateObs)) {
                Types.ACTIONS action = stateObs.getAvailableActions().get(i);
                StateObservation stateCop = stateObs.copy();
                stateCop.advance(action);

                Node newNode = new Node(action, stateCop, node.depth+1);
                results.add(this.Astar(newNode, stateCop)); // recursive call Astar
            }
            if (results.isEmpty())
                return new Result(node.action, node.score);
            else {
                Result maxresult = results.stream().min(Comparator.comparing(Result::getMaxScore)).get();
                maxresult.path.push(node.action);
                return maxresult;
            }
        }
    }

    public ArrayList<Integer> expand(StateObservation stateObs) {


        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        ArrayList<Integer> actual = new ArrayList<>();
        for(int i = 0; i < actions.size(); i++) {
            StateObservation stateCop = stateObs.copy();
            stateCop.advance(actions.get(i));
            if(!stateCop.equalPosition(stateObs) && !this.compareClosed(stateCop)) // check for repeated state
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
}

class Node {
    Types.ACTIONS action;
    int depth;
    double score;
    // index table
    int indexGoal=-1;
    int indexKey=-1;
    int indexMush=-1;
    int indexHole=-1;
    int indexBox=-1;
    int totalHole=-1;

    public Node(Types.ACTIONS action, StateObservation stateObs, int depth) {
        this.action = action;
        this.depth = depth;
        this.getIndex(stateObs);
        this.score = this.calScore(stateObs);
    }

    private double calScore(StateObservation stateObs) {
        if(stateObs.getGameWinner() == Types.WINNER.PLAYER_LOSES)
            return 100000;
        else if(stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS)
            return -100000;
        else {
            ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions();
            ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions();

            double score = 0;
            Vector2d avatarpos = stateObs.getAvatarPosition();
            // obstacle: key or goal
            if(this.indexKey == -1) {
                score -= 2000; // getKey bonus
                score += this.calManhattan(fixedPositions[this.indexGoal].get(0).position, avatarpos)*8;
            }
            else {
                score += this.calManhattan(fixedPositions[this.indexGoal].get(0).position, avatarpos)*0.5;

                score += this.calManhattan(movingPositions[this.indexKey].get(0).position, avatarpos)*0.5;
                // avoid box on the key
                if (this.indexBox != -1) {
                    for(Observation box: movingPositions[this.indexBox]) {
                        if (this.calManhattan(box.position, movingPositions[this.indexKey].get(0).position) == 0) {
                            score = 100000;break;
                        }
                    }
                }

            }
            // obstacle: box and their hole
            if (this.indexHole != -1){
                Vector2d hole = fixedPositions[this.indexHole].get(0).position;
                for(Observation box: movingPositions[this.indexBox]){
                    score += this.calManhattan(hole, box.position)*16 + this.calManhattan(box.position, avatarpos);
                }
            }
            return score;
        }
    }

    private double calManhattan(Vector2d a, Vector2d b) {
       return (Math.abs(a.copy().subtract(b).x) + Math.abs(a.copy().subtract(b).y));
    }
    private void getIndex(StateObservation stateObs) {

        this.indexGoal=-1;
        this.indexKey=-1;
        this.indexMush=-1;
        this.indexHole=-1;
        this.indexBox=-1;
        this.totalHole=-1;

        final int goal = 7;
        final int key = 6;
        final int mushroom = 5;
        final int hole = 2;
        final int box = 8;
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
        if (movingPositions != null) {
            for (int i = 0; i < movingPositions.length; i++) {
                ArrayList<Observation> movingPosition = movingPositions[i];
                if (!movingPosition.isEmpty()) {
                    switch (movingPosition.get(0).itype) {
                        case box -> this.indexBox = i;
                        case key -> this.indexKey = i;
                    }
                }
            }
        }
    }
}

class Result {
    Stack<Types.ACTIONS> path = new Stack<>();
    double maxScore;
    public Result(Types.ACTIONS action, double score) {
        this.path.push(action);
        this.maxScore = score;
    }
    public double getMaxScore() {
        return maxScore;
    }
}