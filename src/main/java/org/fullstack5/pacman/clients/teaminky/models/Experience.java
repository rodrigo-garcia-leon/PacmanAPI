package org.fullstack5.pacman.clients.teaminky.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.fullstack5.pacman.api.models.Direction;

@AllArgsConstructor
@Getter
public class Experience {
    private DQNGameState previousState;
    private float reward;
    private Direction lastDirection;
    private DQNGameState currentState;
}
