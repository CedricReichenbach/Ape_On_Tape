package server.model.entities.moving;

import server.model.Game;
import server.model.entities.Entity;

public class TurretBullet extends Bullet {

	protected float range;
	protected float travelDistance;
	
	public TurretBullet(Entity owner, float x, float y, float dirX, float dirY, float range) {
		super(owner, x, y, dirX, dirY);
		this.killOnWallHit = false;
		this.range = range;
		this.travelDistance = 0;
		this.tileCollision = false;
	}
	
	@Override
	public void brain(Game game){
		super.brain(game);
		this.travelDistance+=this.speed;
		if(travelDistance > range)
			game.removeEntity(this);
	}

}