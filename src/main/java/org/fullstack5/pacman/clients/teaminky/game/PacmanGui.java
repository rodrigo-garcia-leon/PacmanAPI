package org.fullstack5.pacman.clients.teaminky.game;

import org.fullstack5.pacman.api.models.Maze;
import org.fullstack5.pacman.api.models.Position;
import org.fullstack5.pacman.api.models.response.GameState;
import org.fullstack5.pacman.api.models.response.MovingPiece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.List;

public final class PacmanGui {

    private static final int GRID_WIDTH = 40;
    private static final int FRAMES_PER_TICK = 10;

    private final Maze maze;
    private final long msPerFrame;
    private GameState state;
    private int renderProgress = 0;

    public PacmanGui(final Maze maze, final Duration step) {
        this.maze = maze;
        long msPerTick = step.getNano() / 1000000;
        msPerFrame = msPerTick / FRAMES_PER_TICK;
    }

    private static int calcDrawX(final MovingPiece piece, final int renderProgress) {
        if (piece.getOldPosition() == null || piece.getOldPosition().equals(piece.getCurrentPosition())) {
            return GRID_WIDTH * piece.getCurrentPosition().getX();
        }
        return GRID_WIDTH * piece.getOldPosition().getX() + GRID_WIDTH * renderProgress * piece.getDirection().getDeltaX() / FRAMES_PER_TICK;
    }

    private static int calcDrawY(final MovingPiece piece, final int renderProgress) {
        if (piece.getOldPosition() == null || piece.getOldPosition().equals(piece.getCurrentPosition())) {
            return GRID_WIDTH * piece.getCurrentPosition().getY();
        }
        return GRID_WIDTH * piece.getOldPosition().getY() + GRID_WIDTH * renderProgress * piece.getDirection().getDeltaY() / FRAMES_PER_TICK;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public final void initialize() {
        final JFrame frame = new JFrame();
        final JPanel panel = new MyPanel();
        panel.setFocusable(true);
        panel.requestFocusInWindow();
        frame.add(panel);
        frame.pack();
        frame.setSize(maze.getWidth() * GRID_WIDTH + 16, maze.getHeight() * GRID_WIDTH + 38);
        frame.setLocationRelativeTo(null);
        frame.setTitle("Chapter Fullstack 5 Pacman Simulator");
        frame.setVisible(true);
        frame.addWindowListener(new PacmanWindowListener(frame));

        new Thread(new GuiRunner(frame)).start();
    }

    public void updateState(GameState state) {
        this.state = state;
        renderProgress = 0;
    }

    private class PacmanWindowListener extends WindowAdapter {

        private final JFrame frame;

        PacmanWindowListener(final JFrame frame) {
            this.frame = frame;
        }

        @Override
        public void windowClosed(final WindowEvent e) {
            frame.dispose();
        }
    }

    private class GuiRunner implements Runnable {

        private final JFrame frame;

        private GuiRunner(JFrame frame) {
            this.frame = frame;
        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while (true) {
                renderProgress++;
                frame.repaint();
                try {
                    Thread.sleep(msPerFrame);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private class MyPanel extends JPanel {

        @Override
        protected void paintComponent(final Graphics g) {
            renderMaze(g);
            //noinspection Duplicates
            if (state != null) {
                renderPacman(g);
                renderDots(g, state.getRemainingDots(), 8);
                renderDots(g, state.getRemainingPellets(), 2);
                renderGhost(g, state.getBlinky(), Color.RED);
                renderGhost(g, state.getPinky(), Color.PINK);
                renderGhost(g, state.getInky(), Color.CYAN);
                renderGhost(g, state.getClyde(), Color.ORANGE);
            }
        }

        private void renderDots(final Graphics g, final List<Position> dots, final int size) {
            try {
                g.setColor(Color.yellow);
                for (final Position dot : dots) {
                    g.fillOval(GRID_WIDTH * dot.getX() + GRID_WIDTH / 2 - GRID_WIDTH / size / 2,
                            GRID_WIDTH * dot.getY() + GRID_WIDTH / 2 - GRID_WIDTH / size / 2,
                            GRID_WIDTH / size, GRID_WIDTH / size);
                }
            } catch (Exception ignored) {

            }
        }

        @SuppressWarnings("Duplicates")
        private void renderPacman(final Graphics g) {
            final MovingPiece pacman = state.getPacman();
            int animProgress = (renderProgress + 5) % FRAMES_PER_TICK;
            if (animProgress > 6) {
                animProgress = FRAMES_PER_TICK - animProgress;
            }
            g.setColor(Color.yellow);
            final int startAngle = pacman.getDirection().getAngle();
            g.fillArc(
                    calcDrawX(pacman, renderProgress),
                    calcDrawY(pacman, renderProgress),
                    GRID_WIDTH - 1, GRID_WIDTH - 1, startAngle + 45 - animProgress * 9, 270 + animProgress * 18);
        }

        @SuppressWarnings("Duplicates")
        private void renderGhost(final Graphics g, final MovingPiece ghost, final Color color) {
            g.setColor(ghost.isVulnerable() ? Color.BLUE : color);
            final int drawX = calcDrawX(ghost, renderProgress);
            final int drawY = calcDrawY(ghost, renderProgress);
            g.fillArc(drawX, drawY, GRID_WIDTH - 1, (GRID_WIDTH) - 1, 0, 180);
            final int[] x = new int[]{drawX, drawX, drawX + GRID_WIDTH / 4, drawX + GRID_WIDTH / 2, drawX + GRID_WIDTH * 3 / 4, drawX + GRID_WIDTH, drawX + GRID_WIDTH};
            final int legsTop = drawY + GRID_WIDTH * 3 / 4;
            final int legsBottom = drawY + GRID_WIDTH - 1;
            final int[] y = new int[]{drawY + GRID_WIDTH / 2, legsBottom, legsTop, legsBottom, legsTop, legsBottom, drawY + GRID_WIDTH / 2};
            g.fillPolygon(x, y, x.length);
            g.setColor(Color.WHITE);
            g.fillOval(drawX + GRID_WIDTH / 8, drawY + GRID_WIDTH / 8, GRID_WIDTH / 4, GRID_WIDTH / 4);
            g.fillOval(drawX + GRID_WIDTH * 5 / 8, drawY + GRID_WIDTH / 8, GRID_WIDTH / 4, GRID_WIDTH / 4);
            if (!ghost.isVulnerable()) {
                g.setColor(Color.BLACK);
                g.drawOval(drawX + GRID_WIDTH / 8, drawY + GRID_WIDTH / 8, GRID_WIDTH / 4, GRID_WIDTH / 4);
                g.fillOval(drawX + GRID_WIDTH / 8 + (ghost.getDirection().getDeltaX() + 1) * GRID_WIDTH / 16, drawY + GRID_WIDTH / 8 + (ghost.getDirection().getDeltaY() + 1) * GRID_WIDTH / 16, GRID_WIDTH / 8, GRID_WIDTH / 8);
                g.drawOval(drawX + GRID_WIDTH * 5 / 8, drawY + GRID_WIDTH / 8, GRID_WIDTH / 4, GRID_WIDTH / 4);
                g.fillOval(drawX + GRID_WIDTH * 5 / 8 + (ghost.getDirection().getDeltaX() + 1) * GRID_WIDTH / 16, drawY + GRID_WIDTH / 8 + (ghost.getDirection().getDeltaY() + 1) * GRID_WIDTH / 16, GRID_WIDTH / 8, GRID_WIDTH / 8);
            }
        }

        private void renderMaze(final Graphics g) {
            final int width = maze.getWidth();
            final int height = maze.getHeight();
            //noinspection Duplicates
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (maze.isWall(x, y)) {
                        g.setColor(Color.blue);
                    } else {
                        g.setColor(Color.black);
                    }
                    g.fillRect(x * GRID_WIDTH, y * GRID_WIDTH, GRID_WIDTH - 1, GRID_WIDTH - 1);
                }
            }
        }
    }
}
