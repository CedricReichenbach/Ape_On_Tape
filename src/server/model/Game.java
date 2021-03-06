package server.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import server.GameHandler;
import server.listeners.CollisionListener;
import server.model.ServerEvents.EndGameEvent;
import server.model.ServerEvents.GameStartEvent;
import server.model.ServerEvents.MapChangeEvent;
import server.model.ServerEvents.RoundEndEvent;
import server.model.ServerEvents.ServerEvent;
import server.model.entities.Barrier;
import server.model.entities.Entity;
import server.model.entities.moving.Player;
import server.model.map.MapInfo;
import server.model.map.PositionType;
import server.model.map.TileMap;
import server.properties.ApeProperties;

public class Game {

	// store players separatly.
	private volatile Map<Integer, Player> players;
	private volatile Map<Integer, Entity> entities;
	private List<ServerEvent> serverEvents;
	private List<ServerEvent> removeServerEvents;
	private List<ServerEvent> addServerEvents;
	private Map<Player, Integer> winners;
	private static final int MAP_COUNT_DOWN = Integer.parseInt(ApeProperties
			.getProperty("firstMapCountDown"));
	private static final int WAIT_AFTER_WIN = Integer.parseInt(ApeProperties
			.getProperty("waitAfterWin"));
	private static final int WAIT_AFTER_END_ROUND = Integer.parseInt(ApeProperties
			.getProperty("waitAfterEndRound"));
	private static final int FIRST_WINNER_POINTS = Integer.parseInt(ApeProperties
			.getProperty("firstWinnerPoints"));
	private static final int LATER_WINNER_POINTS = Integer.parseInt(ApeProperties
			.getProperty("laterWinnerPoints"));
	private static final int NUMBER_OF_LEVELS = Integer.parseInt(ApeProperties
			.getProperty("numberOfLevels"));
	private static final int FASTER_SHOOT_BULLETS = 50;
	private List<String> levels;

	private boolean finishRound;
	private String roomName;
	private GameHandler gameHandler;
	TileMap map;
	protected String mapName;
	protected int currentLevel;
	int width, height; // TODO: unused? (also in constructor)
	private Collection<CollisionListener> collisionListeners;
	private boolean started;

	/**
	 * True if game has already started, false if waiting for
	 * <code>start()</code> signal
	 */
	private boolean running;

	public Game(GameHandler gameHandler, String roomName, int width, int height) {
		this.players = new HashMap<Integer, Player>();
		this.collisionListeners = new LinkedList<CollisionListener>();
		this.width = width;
		this.roomName = roomName;
		this.height = height;
		this.gameHandler = gameHandler;
		this.finishRound = false;
		this.started = false;
		this.winners = new HashMap<Player, Integer>();
		this.serverEvents = new LinkedList<ServerEvent>();
		this.currentLevel = 0;
		this.loadLevels();
		this.loadMap(this.levels.get(this.currentLevel));
	}
	
	private void loadLevels(){
		this.levels = new LinkedList<String>();
		for(int i = 0; i < NUMBER_OF_LEVELS; i++){
			this.levels.add(ApeProperties.getProperty("level"+i));
		}
	}

	private void initEntities(MapInfo mapInfo) {
		this.entities = new HashMap<Integer, Entity>();
		for (Entity e : mapInfo.createEntities(this.getMap()))
			this.addEntity(e);
	}

	/**
	 * Launch this game
	 */
	public synchronized void start() {
		this.running = true;
		for (Entity e : entities.values()) {
			if (e instanceof Barrier) {
				((Barrier) e).open();
				e.type = "barrier_open";
			}
		}
	}

	public void addPlayer(int playerId, String playerName) {
		float[] start = map.getFirstTileXY(PositionType.PlayerStart);
		start[0] += this.players.size();
		Player player = new Player(playerId, start[0], start[1], playerName);
		player.setId(playerId);
		this.players.put(player.getId(), player);
	}

	public void removePlayer(int playerId) {
		this.players.remove(playerId);
	}

	public Map<Integer, Player> getPlayers() {
		return this.players;
	}

	public List<Player> getPlayersList() {
		return new LinkedList<Player>(this.getPlayers().values());
	}

	public void setPlayerKeys(int playerId, List<Integer> keys) {
		this.players.get(playerId).setKeysPressed(keys);
	}

	public Map<Integer, Player> getPlayersAsMap() {
		return this.players;
	}

	public TileMap getMap() {
		return map;
	}

	public void update() {
		for (Entity entity : this.getAllEntites())
			entity.brain(this);
		removeServerEvents = new LinkedList<ServerEvent>();
		addServerEvents = new LinkedList<ServerEvent>();

		for (ServerEvent event : this.serverEvents)
			event.work();

		this.serverEvents.removeAll(removeServerEvents);
		this.serverEvents.addAll(addServerEvents);
	}

	public boolean hasPlayerWithId(int id) {
		return this.players.containsKey(id);
	}

	/**
	 * Returns true if this game (room) is empty.
	 */
	public boolean noPlayers() {
		return this.players.isEmpty();
	}

	public void addCollisionListener(CollisionListener listener) {
		this.collisionListeners.add(listener);
	}

	public void collision(Entity e) {
		if (e.collisionState())
			return;

		// TODO: save (and read) collision state dependent on side of collision
		e.setCollisionState(true);
		for (CollisionListener listener : collisionListeners)
			listener.collisionOccured(this, e);
		// this.soundEvents.add("wall-collision");
	}

	public void addEntity(Entity e) {
		this.entities.put(e.getId(), e);
	}

	public void removeEntity(Entity e) {
		this.entities.remove(e.getId());
	}

	/**
	 * 
	 * @return all entities of this game INCLUDING the players
	 */
	public List<Entity> getAllEntites() {
		List<Entity> list = new LinkedList<Entity>(this.entities.values());
		list.addAll(this.getPlayersList());
		return list;
	}

	public List<Entity> getEntities() {
		return new LinkedList<Entity>(this.entities.values());
	}

	public void noCollision(Entity e) {
		e.setCollisionState(false);
	}

	public Map<Integer, Entity> getAllEntitiesMap() {
		Map<Integer, Entity> e = new HashMap<Integer, Entity>(this.players);
		e.putAll(this.entities);
		return e;
	}

	public GameEvent[] popEvents() {
		Collection<GameEvent> result = new ArrayList<GameEvent>(EventHandler
				.getInstance().popEvents());
		return result.toArray(new GameEvent[0]);
	}

	public void playerHit(Player player) {
		EventHandler.getInstance().addEvent(
				new GameEvent(GameEvent.Type.SOUND, "kill"));
	}

	public boolean isRunning() {
		return running;
	}

	public void playerFinished(Player player) {
		player.win();
		EventHandler.getInstance().addEvent(
				new GameEvent(GameEvent.Type.SOUND, "win"));
		if(winners.isEmpty()){
			EventHandler.getInstance().addEvent(
					new PushMessageEvent(GameEvent.Type.PUSH_MESSAGE, "Hurry up! "+WAIT_AFTER_WIN+" Seconds left!", 2 * 1000));
			this.addServerEvent(new RoundEndEvent(this, WAIT_AFTER_WIN * GameHandler.GAME_RATE));
			player.addPoints(FIRST_WINNER_POINTS);
			winners.put(player, FIRST_WINNER_POINTS);
		}else if(!finishRound){
			player.addPoints(LATER_WINNER_POINTS);
			winners.put(player, LATER_WINNER_POINTS);
		}
			
	}
	
	public void finishRound(){
		List<Player> winners = new LinkedList<Player>(this.winners.keySet());
		String message = "Winners of this round:";
		for(Player winner : winners){
			message += "\n "+winner.getName()+" : "+this.winners.get(winner)+" points";
		}
		EventHandler.getInstance().addEvent(
				new PushMessageEvent(GameEvent.Type.PUSH_MESSAGE, message, WAIT_AFTER_END_ROUND * 1000));
		if(this.currentLevel < this.levels.size() - 1){
			this.currentLevel++;
			this.addFutureServerEvent(new MapChangeEvent(this, GameHandler.GAME_RATE * WAIT_AFTER_END_ROUND,
				this.levels.get(this.currentLevel)));
		}
		else{
			this.addFutureServerEvent(new EndGameEvent(this, GameHandler.GAME_RATE * WAIT_AFTER_END_ROUND));
		}
		this.finishRound = true;
	}

	public void changeMap(String map) {
		this.loadMap(map);
		this.movePlayersToStartingPosition();
		this.finishRound = false;
		EventHandler.getInstance().addEvent(GameEvent.Type.MAPCHANGE, mapName);
		this.addFutureServerEvent(new GameStartEvent(this,
				GameHandler.GAME_RATE * MAP_COUNT_DOWN ));
	}

	private void loadMap(String mapName) {
		this.mapName = mapName;
		String mapPath = GameHandler.getWebRoot() + File.separator + "maps"
				+ File.separator + mapName;
		MapInfo mapInfo = MapInfo.fromJSON(mapPath);
		this.map = new TileMap(mapInfo);
		this.initEntities(mapInfo);

	}

	/**
	 * moves players back to start and "unwins" them.
	 */
	public void movePlayersToStartingPosition() {
		int i = 0;
		float[] start = this.map.getFirstTileXY(PositionType.PlayerStart);
		this.winners.clear();
		for (Player player : this.getPlayersList()) {
			player.setX(start[0] + i);
			player.setY(start[1]);
			i++;
			player.unwin();
		}
	}

	public String getMapName() {
		return this.mapName;
	}

	public void removeServerEvent(ServerEvent serverEvent) {
		this.removeServerEvents.add(serverEvent);
	}

	/**
	 * This is used if you want to add an event within a server event. It will be added at the end of the iteration over the events. So we don't get a concurrentModificationException
	 * @param serverEvent
	 */
	public void addFutureServerEvent(ServerEvent serverEvent) {
		this.addServerEvents.add(serverEvent);
	}

	public void addServerEvent(ServerEvent event) {
		this.serverEvents.add(event);
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public void cloudPenaltyFor(Player player) {
		if(!player.isWinner())
		EventHandler.getInstance().addEvent(GameEvent.Type.CLOUD_PENALTY,
				player.getId() + "");
	}

	public void endGame() {
		this.gameHandler.endGame(this);
	}

	public String getRoomName() {
		return roomName;
	}

	public void fasterShoot(Player player) {
		player.fasterShoot(FASTER_SHOOT_BULLETS);
	}

	
}
