import core.ArcadeMachine;
import java.util.Random;
import core.competition.CompetitionParameters;
public class Test_depthfirst {

    public static void main(String[] args) {
        String depthfirstController = "controllers.depthfirst.Agent";
        int seed = new Random().nextInt(); // seed for random
        /****** Task 1 ******/
        CompetitionParameters.ACTION_TIME = 10000; // set to the time that allow you to do the depth first search
        ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl0.txt", true, depthfirstController, null, seed, false);

    }
}
