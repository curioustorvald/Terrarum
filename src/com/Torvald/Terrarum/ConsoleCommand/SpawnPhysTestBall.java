package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.Terrarum.Actors.Actor;
import com.Torvald.Terrarum.Actors.ActorWithBody;
import com.Torvald.Terrarum.Actors.PhysTestBall;
import com.Torvald.Terrarum.MapDrawer.MapCamera;
import com.Torvald.Terrarum.Terrarum;

/**
 * Created by minjaesong on 16-03-05.
 */
public class SpawnPhysTestBall implements ConsoleCommand {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 2) {
            int mouseX = Terrarum.appgc.getInput().getMouseX();
            int mouseY = Terrarum.appgc.getInput().getMouseY();

            float elasticity = new Float(args[1]);

            ActorWithBody ball = new PhysTestBall();
            ball.setPosition(mouseX + MapCamera.getCameraX()
                    , mouseY + MapCamera.getCameraY());
            ball.setElasticity(elasticity);

            Terrarum.game.addActor(ball);
        }
        else {
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        new Echo().execute("usage: spawnball [elasticity]");
    }
}
