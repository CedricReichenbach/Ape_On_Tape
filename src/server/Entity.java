package server;

public class Entity {
	protected int id;
	float x;
	float y;
	float height = 20f;
	float width = 20f;
	float speed = 5;

	public Entity(int id, int x, int y) {
		this.id = id;
		this.x = x;
		this.y = y;
	}

	public void moveOnMap(TileMap map, int dirx, int diry) {
		float deltax = this.speed * dirx;
		float deltay = this.speed * diry;
		if (deltax != 0 && deltay != 0) {
			deltax /= Math.sqrt(2);
			deltay /= Math.sqrt(2);
		}
		this.x += deltax;
		this.y += deltay;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}
}