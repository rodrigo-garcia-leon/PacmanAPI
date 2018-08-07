package org.fullstack5.pacman.clients.teaminky.pacman;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Piece;
import org.fullstack5.pacman.api.models.request.MoveRequest;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.clients.teaminky.models.DQN;
import org.fullstack5.pacman.clients.teaminky.models.DQNGameState;
import org.fullstack5.pacman.clients.teampacman.AI;
import org.fullstack5.pacman.clients.teampacman.ServerComm;

public class DQNPacmanRunner implements AI {
    private final String gameId;
    private final String authId;
    private final Maze maze;
    private final DQN dqn;

    public DQNPacmanRunner(String gameId, String authId, Maze maze) {
        this.gameId = gameId;
        this.authId = authId;
        this.maze = maze;
        dqn = new DQN(maze.getWidth(), maze.getHeight());
    }

    @Override
    public void runAI(GameState gameState) {
        DQNGameState state = DQNGameState.createState(maze, gameState);
        Direction move = getMove(state);
        performMove(move);
    }

    private Direction getMove(DQNGameState state) {
        return dqn.getMove(state);
    }

    private void performMove(final Direction direction) {
        final MoveRequest request = new MoveRequest(gameId, authId, direction, Piece.Type.PACMAN);
        ServerComm.performMove(request);
    }
}
