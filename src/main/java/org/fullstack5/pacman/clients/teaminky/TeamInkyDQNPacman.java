package org.fullstack5.pacman.clients.teaminky;

import org.fullstack5.pacman.api.models.PlayerType;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.PlayerRegistered;
import org.fullstack5.pacman.clients.teaminky.pacman.DQNPacmanRunner;
import org.fullstack5.pacman.clients.teampacman.AI;
import org.fullstack5.pacman.clients.teampacman.RunnerThread;
import org.fullstack5.pacman.clients.teampacman.ServerComm;
import reactor.core.publisher.Flux;

public final class TeamInkyDQNPacman implements Runnable {
    private final String gameId;

    private TeamInkyDQNPacman(String gameId) {
        this.gameId = gameId;
    }

    public static void main(final String... args) {
        new TeamInkyDQNPacman(args[0]).run();
    }

    @Override
    public final void run() {
        final Flux<GameState> flux = ServerComm.establishGameStateFlux(gameId);

        RunnerThread thread;
        final PlayerRegistered player = ServerComm.registerPlayer(gameId, PlayerType.PACMAN);
        final AI pacmanAI = new DQNPacmanRunner(gameId, player.getAuthId(), player.getMaze());
        thread = new RunnerThread(pacmanAI);
        flux.subscribe(thread::updateState);
    }

}
