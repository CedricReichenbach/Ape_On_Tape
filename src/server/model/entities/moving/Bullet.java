package server.model.entities.moving;

import java.util.List;

import server.model.Game;
import server.model.entities.Entity;
import server.network.GsonExclusionStrategy.noGson;
import server.util.Util;

public class Bullet extends Entity {

	@noGson
	protected float dirX;
	@noGson
	protected float dirY;
	@noGson
	protected Entity owner;
	@noGson
	protected float radius;
	@noGson
	protected boolean killOnWallHit;

	public Bullet(Entity owner, float x, float y, float dirX, float dirY) {
		super(x, y);
		this.owner = owner;
		this.dirX = dirX;
		this.dirY = dirY;
		this.type = "bullet";
		this.speed = 10;
		this.radius = 0;
		this.height = 1;
		this.width = 1;
		this.killOnWallHit = true;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public Entity getOwner() {
		return owner;
	}

	@Override
	public void brain(Game game) {

		this.moveInDirection(game);

		if (this.killOnWallHit && this.wallHit){
			this.doWallHit(game);
			return;
		}

		List<Entity> overlapping = Util.getEntitiesOverlapping(
				game.getAllEntites(), this);
		overlapping.remove(this.owner);
		boolean killMe = false;
		for (Entity entity : overlapping) {
			entity.hitByBullet(game, this);
			if(entity.collisionResolving)
				killMe = true;
		}
		if (killMe)
			this.doEntityWallHit(game, overlapping);
	}

	protected void moveInDirection(Game game) {
		float deltax = dirX * speed;
		float deltay = dirY * speed;
		if (deltax != 0 && deltay != 0) {
			deltax /= Math.sqrt(2);
			deltay /= Math.sqrt(2);
		}
		moveOnMap(game, deltax, deltay);
	}

	protected void doWallHit(Game game) {
		game.removeEntity(this);
	}

	protected void doEntityWallHit(Game game, List<Entity> entities) {
		if (this.killOnWallHit)
			game.removeEntity(this);
	}

	@Override
	public double getRadius() {
		return radius;
	}

	public boolean isKillOnWallHit() {
		return killOnWallHit;
	}

	public void setKillOnWallHit(boolean killOnWallHit) {
		this.killOnWallHit = killOnWallHit;
	}

	@Override
	public void hitByBullet(Game game, Bullet bullet) {
		// nothing happens
	}
}
