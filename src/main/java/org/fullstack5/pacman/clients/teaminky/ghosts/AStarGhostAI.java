package org.fullstack5.pacman.clients.teaminky.ghosts;

import org.fullstack5.pacman.api.models.Direction;
import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Position;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.MovingPiece;
import org.fullstack5.pacman.clients.teampacman.ClientUtils;
import org.fullstack5.pacman.clients.teampacman.models.WeightedPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public final class AStarGhostAI {

    private static final int GHOST_COUNT = 4;

    private final Maze maze;

    public AStarGhostAI(Maze maze) {
        this.maze = maze;
    }

    public final List<Direction> runAI(final GameState state) {

        List<Direction> directions = new ArrayList<>();
        for (int i = 0; i < GHOST_COUNT; i++) {
            final MovingPiece ghost = ClientUtils.getGhost(state, i);
            final MovingPiece pacman = state.getPacman();
            try {
                final List<WeightedPosition> path = calculateAStarPath(maze, ghost, pacman);
                directions.add(path.get(0).getDirectionToPosition());
            } catch (Exception ignored) {
                directions.add(Direction.random());
            }
        }
        return directions;
    }

    @SuppressWarnings("Duplicates")
    private List<WeightedPosition> calculateAStarPath(final Maze maze, final MovingPiece origin, final MovingPiece target) {
        final Collection<WeightedPosition> neighbouring = findNeighbouring(maze, origin.getCurrentPosition(), target.getCurrentPosition());
        final TreeSet<WeightedPosition> options = new TreeSet<>(neighbouring);


        final List<WeightedPosition> doneList = new ArrayList<>();
        while (!options.isEmpty()) {
            final WeightedPosition next = options.first();
            if (next.getEstimatedDistanceToTarget() == 0) {
                break;
            }
            options.remove(next);
            doneList.add(next);
            addAllNonPresent(options, doneList, findNeighbouring(maze, next, target.getCurrentPosition(), next));
        }
        final List<WeightedPosition> result = new ArrayList<>();

        WeightedPosition foundSolution = options.first();
        while (foundSolution != null) {
            result.add(0, foundSolution);
            foundSolution = foundSolution.getPrevious();
        }
        return result;
    }


    @SuppressWarnings("Duplicates")
    private void addAllNonPresent(final Collection<WeightedPosition> target, final Collection<WeightedPosition> doneList, final Collection<WeightedPosition> newPoss) {
        for (final WeightedPosition newPos : newPoss) {
            boolean found = false;
            for (final WeightedPosition existing : target) {
                if (newPos.getPosition().equals(existing.getPosition())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }
            for (final WeightedPosition existing : doneList) {
                if (newPos.getPosition().equals(existing.getPosition())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }
            target.add(newPos);
        }
    }

    private Collection<WeightedPosition> findNeighbouring(final Maze maze, final Position current, final Position target) {
        final List<WeightedPosition> result = new ArrayList<>(4);
        for (final Direction direction : ClientUtils.randomize(Direction.values())) {
            final Position newPos = ClientUtils.getPosition(maze, current, direction);
            if (maze.isWall(newPos)) {
                continue;
            }
            result.add(new WeightedPosition(newPos, null, direction, 0, estimateDistance(newPos, target)));
        }
        return result;
    }

    private Collection<WeightedPosition> findNeighbouring(final Maze maze, final WeightedPosition current, final Position target, final WeightedPosition parent) {
        final List<WeightedPosition> result = new ArrayList<>(4);
        for (final Direction direction : ClientUtils.randomize(Direction.values())) {
            final Position newPos = ClientUtils.getPosition(maze, current.getPosition(), direction);
            if (!maze.isWall(newPos)) {
                result.add(new WeightedPosition(newPos, parent, direction, current.getDistanceFromSource() + 1, estimateDistance(newPos, target)));
            }
        }
        return result;
    }


    private int estimateDistance(final Position from, final Position to) {
        return Math.abs(from.getX() - to.getX()) + Math.abs(from.getY() - to.getY());
    }
}
