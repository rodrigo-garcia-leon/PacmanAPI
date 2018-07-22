package org.fullstack5.pacman.clients.teaminky.ghosts;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.response.GameState;

public class DQNPacmanAI {
    public Direction runAI(GameState gameState) {
        return Direction.random();
    }
}
