package server.listeners;

import server.GameHandler;
import server.model.Game;
import server.model.entities.Entity;

public class RealCollisionListener implements CollisionListener {

	private GameHandler gameHander;

	public RealCollisionListener(GameHandler gameHandler) {
		this.gameHander = gameHandler;
	}

	@Override
	public void collisionOccured(Game game, Entity e) {
//		gameHander.collision(game, e);
	}

}
