package org.fullstack5.pacman.clients.teaminky.game;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Game;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Piece;
import org.fullstack5.pacman.api.models.Position;
import org.fullstack5.pacman.api.models.Result;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.MovingPiece;

import java.util.Optional;

public final class GameRunner {
    private final Game game;

    public GameRunner(final Game game) {
        this.game = game;
    }

    public Boolean isFinished() {
        return this.game.getResult().isPresent();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void performStep() {
        game.getPieces().stream()
                .filter(Piece::isActive)
                .forEach(piece ->
                        piece.setPosition(
                                determineNewPosition(piece.getPosition(), piece.getDirection())
                        )
                );

        updateTimers();

        if (game.getTicksVulnerable() <= 0) {
            game.getGhosts().forEach(ghost -> ghost.setVulnerable(false));
        }

        Piece pacman = game.getPacman();

        for (Piece ghost : game.getGhosts()) {
            if (collided(pacman, ghost)) {
                if (ghost.isVulnerable()) {
                    ghost.setPosition(getSpawnPosition(ghost));
                    ghost.setPreviousPosition(getSpawnPosition(ghost));
                    ghost.setTicksDisabled(5);
                    ghost.setVulnerable(false);
                } else {
                    game.setResult(Optional.of(Result.PACMAN_LOST));
                    return;
                }
            }
        }

        game.getRemainingPacdots().remove(pacman.getPosition());

        if (game.getRemainingPellets().remove(pacman.getPosition())) {
            game.setTicksVulnerable(20);
            game.getGhosts().forEach(ghost -> ghost.setVulnerable(true));
        }

        boolean allDotsEaten = game.getRemainingPacdots().isEmpty();
        boolean allPelletsEaten = game.getRemainingPellets().isEmpty();
        if (allDotsEaten && allPelletsEaten) {
            game.setResult(Optional.of(Result.PACMAN_WON));
        }
    }

    private boolean collided(Piece pacman, Piece ghost) {
        if (pacman.getPosition().equals(ghost.getPosition())) {
            return true;
        }

        return pacman.getPosition().equals(ghost.getPreviousPosition()) &&
                pacman.getPreviousPosition().equals(ghost.getPosition());
    }

    public final GameState createState() {
        return new GameState(
                game.getTime(),
                game.getResult(),
                game.getRemainingPacdots(),
                game.getRemainingPellets(),
                createMovingPiece(game.getPacman()),
                createMovingPiece(game.getBlinky()),
                createMovingPiece(game.getPinky()),
                createMovingPiece(game.getInky()),
                createMovingPiece(game.getClyde()),
                game.getPacmanScore(),
                game.getGhostsScore()
        );
    }

    private MovingPiece createMovingPiece(Piece piece) {
        return new MovingPiece(
                piece.getPreviousPosition(),
                piece.getPosition(),
                piece.getDirection(),
                piece.isVulnerable(),
                piece.isActive()
        );
    }

    public final void setDirection(final Direction direction, final Piece.Type type) {
        getPiece(type).setDirection(direction);
    }

    private void updateTimers() {
        game.setTime(game.getTime() + 1);
        game.getPieces().forEach(Piece::reduceTicksDisabled);
        game.reduceTicksVulnerable();
    }

    @SuppressWarnings("Duplicates")
    private Piece getPiece(final Piece.Type type) {
        switch (type) {
            case PACMAN:
                return game.getPacman();
            case BLINKY:
                return game.getBlinky();
            case PINKY:
                return game.getPinky();
            case INKY:
                return game.getInky();
            case CLYDE:
                return game.getClyde();
            default:
                throw new RuntimeException("getPiece called with a type that does not exist: " + type);
        }
    }

    @SuppressWarnings("Duplicates")
    private Position getSpawnPosition(Piece piece) {
        switch (piece.getType()) {
            case PACMAN:
                return game.getMaze().getPacmanSpawn();
            case BLINKY:
                return game.getMaze().getBlinkySpawn();
            case PINKY:
                return game.getMaze().getPinkySpawn();
            case INKY:
                return game.getMaze().getInkySpawn();
            case CLYDE:
                return game.getMaze().getClydeSpawn();
            default:
                throw new RuntimeException("getSpawnPosition called with a type that does not exist: " + piece.getType());
        }
    }

    @SuppressWarnings("Duplicates")
    private Position determineNewPosition(final Position position, final Direction direction) {
        if (position == null || direction == null) {
            return position;
        }
        final Maze maze = game.getMaze();
        int x = boundedMove(position.getX(), direction.getDeltaX(), maze.getWidth());
        int y = boundedMove(position.getY(), direction.getDeltaY(), maze.getHeight());

        if (maze.isWall(x, y)) {
            return position;
        }
        return new Position(x, y);
    }

    private int boundedMove(int position, int delta, int upperBound) {
        int result = (position + delta) % upperBound;
        if (result < 0) {
            result += upperBound;
        }
        return result;
    }
}
