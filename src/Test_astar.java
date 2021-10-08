import core.ArcadeMachine;
import core.competition.CompetitionParameters;

import java.util.Random;

public class Test_astar {

    public static void main(String[] args) {
        String AstarController = "controllers.Astar.Agent";
        int seed = new Random().nextInt(); // seed for random
        /****** Task 1 ******/
        CompetitionParameters.ACTION_TIME = 1000; // set to the time that allow you to do the depth first search
        //ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl0.txt", true, AstarController, null, seed, false);
        ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl3.txt", true, AstarController, null, seed, false);

    }
}
