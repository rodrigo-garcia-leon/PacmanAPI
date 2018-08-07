package org.fullstack5.pacman.clients.teaminky;

import org.fullstack5.pacman.api.models.GhostsRunner;
import org.fullstack5.pacman.api.models.PlayerType;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.PlayerRegistered;
import org.fullstack5.pacman.clients.teampacman.AI;
import org.fullstack5.pacman.clients.teampacman.RunnerThread;
import org.fullstack5.pacman.clients.teampacman.ServerComm;
import org.fullstack5.pacman.clients.teampacman.ghosts.AStarGhostAI;
import org.fullstack5.pacman.clients.teampacman.ghosts.RandomGhostAI;
import reactor.core.publisher.Flux;

@SuppressWarnings("Duplicates")
public final class TeamInkyAStarGhost implements Runnable {
    private final GhostsRunner ghostsRunner;
    private String gameId;

    private TeamInkyAStarGhost(String gameId, GhostsRunner ghostsRunner) {
        this.gameId = gameId;
        this.ghostsRunner = ghostsRunner;
    }

    public static void main(final String... args) {
        new TeamInkyAStarGhost(null, GhostsRunner.ASTAR).run();
    }

    @Override
    public final void run() {
        if (gameId == null) {
            gameId = ServerComm.startGame();
        }
        System.out.println(String.format("gameId: %s", gameId));
        final Flux<GameState> flux = ServerComm.establishGameStateFlux(gameId);

        RunnerThread thread;
        if (ghostsRunner != null) {
            final PlayerRegistered player = ServerComm.registerPlayer(gameId, PlayerType.GHOSTS);
            final AI ghostsAI;
            switch (ghostsRunner) {
                case RANDOM:
                    ghostsAI = new RandomGhostAI(gameId, player.getAuthId(), player.getMaze());
                    break;
                case ASTAR:
                    ghostsAI = new AStarGhostAI(gameId, player.getAuthId(), player.getMaze());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown ghosts runner");
            }
            thread = new RunnerThread(ghostsAI);
            flux.subscribe(thread::updateState);
        }
    }

}
