package examplefuncsplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
	public static final int INFINITY = 100000;
	
	public static final int CHANNEL_CURRENT_ROUND = 0;
	public static final int CHANNEL_NUMBER_OF_ARCHONS = 100;
	public static final int CHANNEL_NUMBER_OF_GARDENERS = 103;
	public static final int CHANNEL_NUMBER_OF_SOLDIERS = 106;
	public static final int CHANNEL_NUMBER_OF_LUMBERJACKS = 109;
	public static final int CHANNEL_NUMBER_OF_SCOUTS = 112;
	public static final int CHANNEL_NUMBER_OF_TANKS = 115;
	public static final int CHANNEL_BUILD_INDEX = 5;
	public static final int CHANNEL_MAP_TOP = 6;
	public static final int CHANNEL_MAP_BOTTOM = 7;
	public static final int CHANNEL_MAP_LEFT = 8;
	public static final int CHANNEL_MAP_RIGHT = 9;
	public static final int CHANNEL_RALLY_POINT = 10;
	public static final int CHANNEL_CONTROL_RADIUS = 12;
	public static final int CHANNEL_CALL_FOR_HELP = 13;
	public static final int CHANNEL_CALL_FOR_HELP_ROUND = 15;
	public static final int CHANNEL_HAPPY_PLACE = 16;
	public static final int CHANNEL_ATTACK = 18;
	public static final int CHANNEL_CRAMPED = 19;
	public static final int CHANNEL_GARDENER_LOCATIONS = 20;
	public static final int GARDENER_LOC_LIMIT = 10;
	// Channels 20-120 reserved for gardeners
	public static final int CHANNEL_LAST_SCOUT_BUILD_TIME = 121;
	public static final int SCOUT_BUILD_INTERVAL = 80;
	public static final int CHANNEL_CHOPPABLE_TREE = 122;
	public static final int CHANNEL_INITIAL_ARCHON_BLOCKED = 124;
	public static final int CHANNEL_THEIR_BASE = 125;
	public static final int CHANNEL_MAP_START_X = 127;
	public static final int CHANNEL_MAP_START_Y = 128;
	public static final int CHANNEL_VP_WIN = 129;
	public static final int CHANNEL_IS_SCOUT_USEFUL = 130;
	public static final int CHANNEL_THING_BUILD_COUNT = 131;
	
	public static final float REPULSION_RANGE = 1.7f;

	static float topBound, leftBound, bottomBound, rightBound;
	static RobotController rc;
	static int myID;
	static MapLocation destination = null;
	static MapLocation currentTarget = null;
	//    static Direction prevDirection = randomDirection();
	static RobotInfo[] nearbyEnemies;
	static RobotInfo[] nearbyFriends;
	static BulletInfo[] nearbyBullets;
	static TreeInfo[] nearbyTrees;
	static TreeInfo[] neutralTrees;
//	static MapLocation[] repellers = new MapLocation[25];
//	static int[] repelWeight = new int[25];
    static MapLocation[] history = new MapLocation[10];
	static TreeInfo closestTree = null;	// scouts use this to shake trees
	static int numberOfChannel;
	static RobotType myType;
	static Team myTeam;
	static boolean isScout, isArchon, isGardener, isSoldier, isTank, isLumberjack;
	static float myStride;
	static float myRadius;
	static MapLocation myLocation;
	static int round;
	static MapLocation treeBuildTarget;
	static float controlRadius;
	static MapLocation helpLocation;
	static int helpRound;
	static int oldGardenerLocChannel, newGardenerLocChannel;
	static MapLocation[] gardenerLocs = new MapLocation[30];
	static int gardenerLocsLen = 0;
	static boolean bruteDefence; // disable complicated dodging when defending a gardener
	static boolean freeRange;
	static MapLocation[] theirSpawns;
	static int retargetCount;
	static boolean aggro;
	static boolean threatened;
	static MapLocation happyPlace;
	static MapLocation[] hexes = new MapLocation[15];
	static int hexLen;
	static boolean roam; // for gardeners
	static MapLocation reflection;
	static RobotInfo reflectionTarget;
	static int lastGardenerHitRound;
	static boolean friendlyFireSpot;
	static boolean hasBeenThreatened;
	static int[] dominationTable;
	static int spawnRound;
	static RobotInfo dominated;
	static int lastScoutBuildTime;
	static boolean skipToNextRound;
	static MapLocation choppableTree;
	static MapLocation theirBase;
	static MapLocation attractor;
	static int lastAttackRound;
	
	static int retHelper1, retHelper2;

	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {

		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;

		myType = rc.getType();		
		myTeam = rc.getTeam();
		numberOfChannel = myNumberOfChannel();
		myStride = myType.strideRadius;
		myRadius = myType.bodyRadius;
		myLocation = rc.getLocation();
		isScout = myType == RobotType.SCOUT;
		isArchon = myType == RobotType.ARCHON;
		isGardener = myType == RobotType.GARDENER;
		isSoldier = myType == RobotType.SOLDIER;
		isTank = myType == RobotType.TANK;
		isLumberjack = myType == RobotType.LUMBERJACK;
		spawnRound = rc.getRoundNum();

		myID = rc.readBroadcast(myNumberOfChannel());
		rc.broadcast(myNumberOfChannel(), myID + 1);

		if (isSoldier)
		{
			freeRange = true;
		}
		if (isScout || isTank)
		{
			freeRange = true;
		}
		theirSpawns = rc.getInitialArchonLocations(myTeam.opponent());
		sortByDistanceFromMe(theirSpawns);
		if (myType == RobotType.ARCHON)
		{
			if (myID == 0)
			{
				MapLocation them = theirSpawns[0];
				MapLocation us = myLocation;
				//            	MapLocation rally = new MapLocation((us.x + us.x + them.x) / 3, (us.y + us.y + them.y) / 3);
				//            	MapLocation rally = us.add(us.directionTo(them), 12);
				MapLocation rally = us;
				writePoint(CHANNEL_RALLY_POINT, rally);
				writePoint(CHANNEL_HAPPY_PLACE, them);
				writePoint(CHANNEL_CHOPPABLE_TREE, new MapLocation(-1, -1));
				rc.broadcastFloat(CHANNEL_MAP_TOP, -INFINITY);
				rc.broadcastFloat(CHANNEL_MAP_LEFT, -INFINITY);
				rc.broadcastFloat(CHANNEL_MAP_RIGHT, INFINITY);
				rc.broadcastFloat(CHANNEL_MAP_BOTTOM, INFINITY);
				rc.broadcast(CHANNEL_INITIAL_ARCHON_BLOCKED, 0);
				rc.broadcastInt(CHANNEL_MAP_START_X, -INFINITY);
				rc.broadcastInt(CHANNEL_MAP_START_Y, -INFINITY);
			}
			else
			{
				//        		freeRange = true; // suicide mission
			}
		}
		destination = readPoint(CHANNEL_RALLY_POINT);
		while (true)
		{
			try {
				round = rc.getRoundNum();

				onRoundBegin();
				if (skipToNextRound)
				{
					skipToNextRound = false;
					continue;
				}
				destination = readPoint(CHANNEL_RALLY_POINT);
				if (freeRange && theirSpawns == null)
				{
					theirSpawns = rc.getInitialArchonLocations(myTeam.opponent());
				}
				switch (myType)
				{
				case GARDENER:
					gardenerSpecificLogic();
					break;
				case ARCHON:
					archonSpecificLogic();
					break;
				case SCOUT:
					scoutSpecificLogic();
					break;
				}

				if (freeRange)
				{
					if (theirBaseFound())
					{
						currentTarget = theirBase;
					}
					else if (retargetCount < 15)
					{
						currentTarget = theirSpawns[retargetCount % theirSpawns.length];
						if (myLocation.distanceTo(currentTarget) < 4)
						{
							if (!canSeeValuableTargets())
							{
								++retargetCount;
							}
						}
					}
					else if (round % 40 == 0)
					{
						currentTarget = myLocation.add(randomDirection(), 100);
					}
				}

//				if (freeRange)
//				{
//					debug_line(myLocation, currentTarget, 100, 0, 100);
//				}

				if (isScout || isArchon) {
					float minDist = 99999999;
					closestTree = null;
					for (TreeInfo info : nearbyTrees)
					{
						if(info.containedBullets > 0 && info.getLocation().distanceTo(myLocation) < minDist)
						{
							minDist = info.getLocation().distanceTo(myLocation);
							closestTree = info;
						}
					}

					if (closestTree != null){
						rc.setIndicatorDot(closestTree.location, 255, 0, 0);
						if (rc.canShake(closestTree.ID)){
							rc.setIndicatorDot(closestTree.location, 0, 255, 0);
							rc.shake(closestTree.ID);
						}
					}
				}
				
				if (dominated != null && myLocation.distanceTo(dominated.getLocation()) < myRadius + dominated.type.bodyRadius + GameConstants.BULLET_SPAWN_OFFSET)
				{
					if (rc.canFireSingleShot())
					{
						rc.fireSingleShot(myLocation.directionTo(dominated.getLocation()));
					}
				}

				if (!rc.hasMoved())
				{
					selectOptimalMove();
					MapLocation loc = opti;

					if (rc.canMove(loc))
					{
						rc.move(loc);
						myLocation = loc;
					}
				}

				if (isLumberjack)
				{
					float minDist = 99999999;
					closestTree = null;
					//            		for (TreeInfo info : nearbyTrees)
						//            		{
						//            			if (info.getTeam() == myTeam)
							//            			{
							//            				continue;
							//            			}
						//            			float d = info.getLocation().distanceTo(destination);
						//            			if (d < minDist)
							//            			{
							//            				minDist = d;
							//            				closestTree = info;
							//            			}
						//            		}
					TreeInfo myClosestTree = null;
					minDist = 99999999;
					for (TreeInfo info : nearbyTrees)
					{
						if (info.getTeam() == myTeam)
						{
							continue;
						}
						float d = info.getLocation().distanceTo(myLocation);
						if (d < minDist)
						{
							minDist = d;
							myClosestTree = info;
						}
					}

					if (nearbyEnemies.length != 0 &&
							myLocation.distanceTo(nearbyEnemies[0].getLocation())
							< myRadius + GameConstants.INTERACTION_DIST_FROM_EDGE + nearbyEnemies[0].getType().bodyRadius &&
							nearbyEnemies[0].getType() != RobotType.ARCHON)
					{
						rc.strike();
					}
					if (myClosestTree != null)
					{
						closestTree = myClosestTree;
						rc.setIndicatorDot(myClosestTree.location, 255, 0, 0);
						if (rc.canShake(myClosestTree.ID))
						{
							rc.setIndicatorDot(myClosestTree.location, 0, 255, 0);
							rc.shake(myClosestTree.ID);
						}
						if (rc.canChop(myClosestTree.ID))
						{
							rc.chop(myClosestTree.ID);
							resetHistory();
						}
					}
				}
				if (isScout)
				{
					if (dominated != null && !rc.hasAttacked())
					{
						MapLocation them = dominated.getLocation();
						Direction dir = myLocation.directionTo(them);
						MapLocation a = myLocation.add(dir, myRadius + GameConstants.BULLET_SPAWN_OFFSET);
						boolean ok = false;
						if (a.distanceTo(them) < dominated.type.bodyRadius)
						{
							ok = true;
						}
						else
						{
							MapLocation b = them.subtract(dir, dominated.getType().bodyRadius);
							float d = a.distanceTo(b);
							if (d < 1 && rc.senseTreeAtLocation(a.add(dir, d / 2)) == null)
							{
								ok = true;
							}
						}
						if (ok)
						{
							if (rc.canFireSingleShot())
							{
								rc.fireSingleShot(dir);
							}
						}
					}
				}
				if (!rc.hasAttacked() && (isSoldier || isTank || isScout))
				{
					friendlyFireSpot = false;
					long bestVal = 0;
					Direction dir = null;
					RobotType enemyType = null;
					float enemyDistance = 0;
					for (RobotInfo info : nearbyEnemies)
					{
						float dist = myLocation.distanceTo(info.getLocation());
						float req;
						switch (info.getType())
						{
						case SCOUT:
							req = 2.3f;
							break;
						default:
							req = 10;
						}
						MapLocation enemyLocation = info.getLocation();
						Direction currDirection = myLocation.directionTo(enemyLocation);

						boolean isNeutralTree = false;
						boolean isEnemyTree = false;

						int onBegin = Clock.getBytecodesLeft();
						MapLocation currLocation = myLocation.add(currDirection, 1.001f);
						float per = (dist - 1.003f - info.getRadius()) / 4;
						for (int i = 0; i < 5; i++, currLocation = currLocation.add(currDirection, per)) 
						{
							if (!rc.canSenseLocation(currLocation))
							{
								continue;
							}
							TreeInfo ti = rc.senseTreeAtLocation(currLocation);
							if (ti == null)
							{
								continue;
							}
							if (ti.team == myTeam || ti.team == Team.NEUTRAL)
							{
								isNeutralTree = true;
							}
							else
							{
								isEnemyTree = true;
							}
						}

						if (dist < 3 && info.getType() == RobotType.LUMBERJACK) 
						{
							dir = myLocation.directionTo(enemyLocation);
							enemyType = info.getType();
							enemyDistance = dist;
							break;
						} 

						if (
								dist < req &&
								!isNeutralTree &&
								(!isEnemyTree ||
									(info.getType() == RobotType.GARDENER && !isScout && trees >= 6)))
						{
							long val = (long) getIdealDistanceMultiplier(info.getType());
							if (dir == null || val > bestVal)
							{
								bestVal = val;
								dir = myLocation.directionTo(info.getLocation());
								enemyType = info.getType();
								enemyDistance = dist;
							}
						}
					}

					if (!isScout || enemyType == RobotType.GARDENER || trees >= 4) {
						smartShot(dir, enemyType, enemyDistance);
					}

					// pink line showing who i wanna shoot
					rc.setIndicatorLine(myLocation, myLocation.add(dir, enemyDistance),255,182,193);
				}

				onRoundEnd();

				if (round != rc.getRoundNum() || Clock.getBytecodesLeft() < 20)
				{
					System.out.println("TLE");
					rc.setIndicatorLine(myLocation, theirSpawns[0], 255, 0, 0);
				}

				Clock.yield();
			}
			catch (Exception e)
			{
				System.out.println("Exception in robot loop");
				e.printStackTrace();
				rc.setIndicatorLine(myLocation, theirSpawns[0], 255, 0, 0);
			}
		}
	}
	
	private static boolean canSeeValuableTargets()
	{
		for (RobotInfo info : nearbyEnemies)
		{
			switch (info.getType())
			{
			case GARDENER:
			case SOLDIER:
			case LUMBERJACK:
			case TANK:
			case ARCHON:
				return true;
			}
		}
		return false;
	}
	
	public static void smartShot(Direction dir, RobotType enemyType, float enemyDistance) throws GameActionException{
		if (dir == null) return;

		for(RobotInfo info : nearbyFriends){
			if (info.getLocation().distanceTo(myLocation) < enemyDistance && willCollideWithTarget(myLocation, dir, info)){
				friendlyFireSpot = true;
				rc.setIndicatorDot(myLocation, 255, 255, 0);
				return; // friendly fire straight up
			}

			//if (Math.abs(myLocation.directionTo(info.getLocation()).degreesBetween(dir)) < 15) badTriad++;
			//if (Math.abs(myLocation.directionTo(info.getLocation()).degreesBetween(dir)) < 30) badPentad++;
		}
		if (enemyType == RobotType.SCOUT && enemyDistance > 3.1f)
		{
			return;
		}
		int trees = rc.getTreeCount();
		if (enemyType == RobotType.ARCHON && trees < 3 && round < 750)
		{
			return;
		}
		if (rc.canFirePentadShot() && (enemyDistance < 4.2f || trees >= 5 || enemyType == RobotType.SOLDIER))
		{
			rc.firePentadShot(dir);
		}
		else if (rc.canFireTriadShot() && enemyDistance < 4.8f)
		{
			rc.fireTriadShot(dir);
		}
		else if (rc.canFireSingleShot() && (!isScout || enemyType == RobotType.GARDENER))
		{
			rc.fireSingleShot(dir);
		}
	}

	static boolean willCollideWithTarget(MapLocation origin, Direction dir, RobotInfo target) {
		MapLocation loc = target.getLocation();

		// Calculate bullet relations to this robot
		Direction directionToRobot = origin.directionTo(loc);
		float distToRobot = origin.distanceTo(loc);
		float theta = dir.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and we can break early
		if (Math.abs(theta) > 1.57079633) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)


		return (perpendicularDist <= target.getType().bodyRadius);
	}

	private static void sortByDistanceFromMe(MapLocation[] pts)
	{
		for (int i = 0; i < pts.length; i++)
		{
			int idx = -1;
			float minDist = 99999999;
			for (int j = i; j < pts.length; j++)
			{
				float d = pts[j].distanceTo(myLocation);
				if (d < minDist)
				{
					minDist = d;
					idx = j;
				}
			}
			MapLocation swp = pts[i];
			pts[i] = pts[idx];
			pts[idx] = swp;
		}
	}
	
	public static boolean theirBaseFound() throws GameActionException
	{
		return rc.readBroadcastInt(CHANNEL_THEIR_BASE) != 0;
	}
	
	public static MapLocation randomPointWithin(float radius)
	{
		radius *= Math.sqrt(rand01());
		return myLocation.add(randomDirection(), radius);
	}
	
	static void relayInfo(int allowed) throws GameActionException
	{
		int start = Clock.getBytecodeNum();
		while (Clock.getBytecodeNum() - start < allowed && Clock.getBytecodesLeft() > 1000)
		{
			float collideRadius = 0.8f;
			MapLocation pt = randomPointWithin(myType.sensorRadius - collideRadius - 1.1f);
			int x = (int) (pt.x + 0.5f);
			int y = (int) (pt.y + 0.5f);
			pt = new MapLocation(x, y);
			if (!rc.onTheMap(pt, collideRadius))
			{
				writeStatus(x, y, STATUS_IMPASSABLE);
			}
			else
			{
				TreeInfo[] arr = rc.senseNearbyTrees(pt, collideRadius, null);
				if (arr.length == 0)
				{
					writeStatus(x, y, STATUS_PASSABLE);
				}
				else
				{
					TreeInfo info = arr[0];
					if (pt.distanceTo(info.getLocation()) - collideRadius - info.getRadius() <= 0)
					{
						writeStatus(x, y, STATUS_IMPASSABLE);
					}
					else
					{
						writeStatus(x, y, STATUS_PASSABLE);
					}
				}
			}
		}
		System.out.println("Spent " + (Clock.getBytecodeNum() - start));
	}
	
	public static void burnCycles(int allowed) throws GameActionException
	{
		System.out.println("Burn " + allowed);
		if (!isBFSActive())
		{
			relayInfo(allowed);
		}
		else if (isArchon)
		{
			doBFS(allowed);
		}
		else if (isScout)
		{
			relayInfo(allowed);
		}
		else
		{
			doBFS(allowed / 2);
			relayInfo(allowed / 2);
		}
	}
	
	public static void scoutSpecificLogic() throws GameActionException
	{
	}

	public static void archonSpecificLogic() throws GameActionException
	{
		getMacroStats();
		macro();
	}

	// When the type parameter is ARCHON, we build a TREE instead.
	public static boolean attemptBuild(int iter, RobotType type) throws GameActionException
	{
		if (type == RobotType.ARCHON){
			if (rc.getTeamBullets() < 50 || !rc.hasTreeBuildRequirements())
			{
				return false;
			}
			float minDist = 99999999;
			MapLocation best = null;
			for (int i = 0; i < hexLen; i++)
			{
				MapLocation cand = hexes[i];
				if (rc.canSenseAllOfCircle(cand, 1) && (rc.isCircleOccupiedExceptByThisRobot(cand, GameConstants.BULLET_TREE_RADIUS) || !rc.onTheMap(cand, 1)))
				{
					continue;
				}
				float d = cand.distanceTo(destination);
				if (d < minDist)
				{
					best = cand;
					minDist = d;
				}
			}
			if (best != null)
			{
				treeBuildTarget = best;
				float offs = GameConstants.BULLET_TREE_RADIUS + myRadius + GameConstants.GENERAL_SPAWN_OFFSET;
				MapLocation spot = best.add(best.directionTo(myLocation), offs);
				if (spot.distanceTo(myLocation) < myStride && rc.canMove(spot) && !rc.hasMoved())
				{
					rc.move(spot);
					myLocation = spot;
					Direction dir = myLocation.directionTo(best);
					if (rc.canPlantTree(dir))
					{
						rc.plantTree(dir);
						increment(CHANNEL_THING_BUILD_COUNT);
						return true;
					}
					else
					{
						return false;
					}
				}
				if (spot.distanceTo(myLocation) < myStride || myLocation.distanceTo(best) < offs)
				{
					return onTreeBuildFail();
				}
			}
			else
			{
				return onTreeBuildFail();
			}
		}
		else
		{
			if (!rc.hasRobotBuildRequirements(type))
			{
				return false;
			}
			for (int i = 0; i < iter; i++)
			{
				Direction dir;
				if (i == 0)
				{
					dir = myLocation.directionTo(theirSpawns[0]);
				}
				else
				{
					dir = randomDirection();
				}
				if (rc.canBuildRobot(type, dir)){
					rc.buildRobot(type, dir);
					if (type == RobotType.SCOUT)
					{
						rc.broadcast(CHANNEL_LAST_SCOUT_BUILD_TIME, round);
					}
					if (type == RobotType.GARDENER && gardeners == 0)
					{
						writePoint(CHANNEL_RALLY_POINT, myLocation);
					}
					increment(CHANNEL_THING_BUILD_COUNT);
					return true;
				}
			}
		}

		return false;
	}

	private static boolean onTreeBuildFail() throws GameActionException
	{
		if (neutralTrees.length >= 1)
		{
			return attemptBuild(10, RobotType.LUMBERJACK);
		}
		else if (gardeners == 1)
		{
			treeBuildTarget = myLocation.add(randomDirection(), 5);
		}
		return false;
	}

	static int gardeners, soldiers, trees, lumberjacks, scouts, archons;
	static void getMacroStats() throws GameActionException
	{
		gardeners = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_GARDENERS));
		soldiers = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SOLDIERS));
		lumberjacks = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_LUMBERJACKS));
		scouts = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SCOUTS));
		trees = rc.getTreeCount();
	}

	public static void debug_highlightClosestGardener() throws GameActionException
	{
		float d = 1e8f;
		MapLocation pos = null;
		for (int i = 0; i < gardenerLocsLen; i++)
		{
			MapLocation oth = gardenerLocs[i];
			float td = oth.distanceTo(myLocation);
			if (td > 2)
			{
				if (td < d)
				{
					d = td;
					pos = oth;
				}
			}
		}
		if (pos != null)
		{
			rc.setIndicatorLine(myLocation, pos, 0, 255, 0);
		}
	}
	
	static void donate() throws GameActionException
	{
		rc.donate((int) (rc.getTeamBullets() / rc.getVictoryPointCost()) * rc.getVictoryPointCost());
	}
	
	static MapLocation myTarget()
	{
		return freeRange ? currentTarget : destination;
	}

	public static void gardenerSpecificLogic() throws GameActionException
	{
		getMacroStats();

		debug_highlightClosestGardener();
		if (threatened)
		{
			boolean anySoldier = false;
			for (RobotInfo info : nearbyFriends)
			{
				if (info.getType() == RobotType.SOLDIER)
				{
					anySoldier = true;
					break;
				}
			}
			if (!anySoldier)
			{
				writePoint(CHANNEL_CALL_FOR_HELP, myLocation);
				rc.broadcast(CHANNEL_CALL_FOR_HELP_ROUND, round);
			}
		}

		int buildIndex = rc.readBroadcast(CHANNEL_BUILD_INDEX);

		RobotType next = getBuildOrderNext(buildIndex);
		if (next != null){
			if (attemptBuild(10, next)){
				rc.broadcast(CHANNEL_BUILD_INDEX, buildIndex+1);
			}
		}
		else{
			macro();
		}

		float lowestHP = 99999;
		TreeInfo bestTree = null;
		for (TreeInfo info : nearbyTrees)
		{
			if (rc.canWater(info.ID)){
				if (info.health < lowestHP && info.team == myTeam){
					lowestHP = info.health;
					bestTree = info;
				}
			}
		}
		if (bestTree != null){
			rc.water(bestTree.ID);
			rc.setIndicatorLine(myLocation, bestTree.location, 0, 0, 255);
		}
	}
	
	static void debug_printMacroStats()
	{
		System.out.println(gardeners + "/" + soldiers + "/" + scouts + "/" + trees);
	}

	// What to build after our build order is done
	public static void macro() throws GameActionException
	{
		debug_printMacroStats();

		boolean wantGardener = false;
		if (gardeners < trees / 5 + 1 || (rc.getTeamBullets() > 350 && gardeners < trees / 2)) // indicates some kind of blockage
		{
			if (gardeners > 0 || (rc.getTeamBullets() >= 300 && round == 1))
			{
				wantGardener = true;
			}
		}
		if (getBuildOrderNext(rc.readBroadcast(CHANNEL_BUILD_INDEX)) == null &&
				gardeners <= 2 && rc.getTeamBullets() > 125 && gardeners < round / 50)
		{
			wantGardener = true;
		}
		
		if (rc.getTeamVictoryPoints() +
				(rc.getTeamBullets() + trees * 200) / rc.getVictoryPointCost()
				> GameConstants.VICTORY_POINTS_TO_WIN)
		{
			rc.broadcastBoolean(CHANNEL_VP_WIN, true);
		}
		if (rc.readBroadcastBoolean(CHANNEL_VP_WIN))
		{
			donate();
		}

		boolean wantLumberjack = false;
		boolean wantSoldier = false;
		if (rc.readBroadcastBoolean(CHANNEL_CRAMPED))
		{
			if (lumberjacks < 2 && lumberjacks < trees)
			{
				wantLumberjack = true;
			}
		}
		else
		{
			if (soldiers < 2 || soldiers < trees / 2)
			{
				wantSoldier = true;
			}
		}
		if (nearbyEnemies.length > 0)
		{
			if (!anyFriendHasType(RobotType.SOLDIER))
			{
				wantSoldier = true;
			}
		}

		if (isArchon)
		{
			if (wantGardener)
			{
				rc.setIndicatorDot(myLocation, 0, 0, 0);
				if (!attemptBuild(10, RobotType.GARDENER) && gardeners == 0)
					rc.broadcast(CHANNEL_INITIAL_ARCHON_BLOCKED, 1);
			}
		}

		if (isGardener)
		{
			if (rc.readBroadcastInt(CHANNEL_THING_BUILD_COUNT) == 2 && isRobotInNearbyTree()){
				attemptBuild(10, RobotType.LUMBERJACK);
			}
			if (rc.readBroadcastBoolean(CHANNEL_IS_SCOUT_USEFUL) && rc.readBroadcast(CHANNEL_LAST_SCOUT_BUILD_TIME) == 0)
			{
				attemptBuild(10, RobotType.SCOUT);
			}
			if (rc.getTeamBullets() >= 50 && (!wantGardener || gardeners > 5) && ((!wantSoldier && !wantLumberjack) || trees >= 5))
			{
				rc.setIndicatorDot(myLocation, 0, 255, 0);
				attemptBuild(10, RobotType.ARCHON); // plant a tree
			}
			if (wantSoldier)
			{
				attemptBuild(10, RobotType.SOLDIER);
			}
			if (wantLumberjack)
			{
				attemptBuild(10, RobotType.LUMBERJACK);
			}
		}
	}
	
	private static void debug_ensureGardener()
	{
		if (!isGardener)
		{
			throw new RuntimeException();
		}
	}

	private static boolean isRobotInNearbyTree()
	{
		debug_ensureGardener();
		for (TreeInfo info : neutralTrees)
		{
			if (info.containedRobot != null)
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean anyFriendHasType(RobotType t)
	{
		for (RobotInfo o : nearbyFriends)
		{
			if (o.getType() == t)
			{
				return true;
			}
		}
		return false;
	}
	
	// Determines the next object to build in the build order. 
	// !!!! If the return object is Archon, build a Tree. !!!!
	public static RobotType getBuildOrderNext(int index){
		return null;
//		RobotType[] buildOrder = 
//			{
//				RobotType.SCOUT,
//			};
//		if (index >= buildOrder.length)
//		{
//			return null;
//		}
//		else
//		{
//			return buildOrder[index];
//		}
	}

	public static long badness(MapLocation loc) throws GameActionException
	{
		long ret = 0;

		// Scout code: Look for trees and shake 'em
		switch (myType)
		{
		case SCOUT:
		case LUMBERJACK:
			if (closestTree != null)
			{
				if (!isLumberjack || (!freeRange && nearbyEnemies.length == 0))
				{
					ret += closestTree.getLocation().distanceTo(loc) * 10000000;
				}
			}
			break;
		case ARCHON:
			if (closestTree != null && round < 50)
			{
				ret += closestTree.getLocation().distanceTo(loc) * 10000;
			}
		default:;
		}

		if (beaconLen != 0)
		{
			ret += 1000 * loc.distanceTo(beacons[beaconLen - 1]);			
		}
		if (attractor != null)
		{
			ret += 1200 * loc.distanceTo(attractor);
		}
		
		if (isScout)
		{
			if (topBound == -INFINITY)
			{
				ret += 20000 * loc.y;
			}
			else if (leftBound == -INFINITY)
			{
				ret += 20000 * loc.x;
			}
			else if (bottomBound == INFINITY)
			{
				ret -= 20000 * loc.y;
			}
			else if (rightBound == INFINITY)
			{
				ret -= 20000 * loc.x;
			}
		}

		if (isScout)
		{
			for (RobotInfo info : nearbyEnemies)
			{
				float d = info.getLocation().distanceTo(loc);
				if (d < 7)
				{
					ret -= 2000 * (long) d * getIdealDistanceMultiplier(info.getType());
				}
			}
		}
		else if (!isGardener && !isArchon)
		{
			if (!(isSoldier && round - lastAttackRound > 10))
			{
				for (RobotInfo info : nearbyEnemies)
				{
					float d = info.getLocation().distanceTo(loc);
					float ideal = getIdealDistance(info.getType());
					if (ideal < 0)
						continue;
					if (isSoldier && bruteDefence)
						ideal = 0;
					if (isLumberjack)
						ideal = 0;
					d -= ideal;
					d *= d;
					ret += (long) (d * getIdealDistanceMultiplier(info.getType()));
				}
			}
		}

		if (isGardener)
		{
			long count = 0;
			long tot = 0;
			for (int i = 0; i < gardenerLocsLen; i++)
			{
				MapLocation oth = gardenerLocs[i];
				if (oth.distanceTo(myLocation) < 2)
				{
					continue;
				}
				float range = 10;
				float d = oth.distanceTo(loc) - myRadius * 2;
				if (d < range)
				{
					++count;
					tot += 1000 * (1 / (0.01f + d / range));
				}
			}
			if (count > 5)
			{
				ret -= 10000 * loc.distanceTo(destination);
			}
			else
			{
				ret += tot;
			}
		}

		if (isArchon)
		{
			for (RobotInfo info : nearbyFriends)
			{
				float range = REPULSION_RANGE;
				float d = info.getLocation().distanceTo(loc) - myRadius - info.getType().bodyRadius;
				if (d < range)
				{
					ret += 100000 * (1 / (0.01f + d / range));
				}
			}
		}
		else if (!ignoreFriendRepulsion)
		{
			for (RobotInfo info : nearbyFriends)
			{
				float range = REPULSION_RANGE;
				float d = info.getLocation().distanceTo(loc) - myRadius - info.getType().bodyRadius;
				if (d < range)
				{
					ret += 1000 * (1 / (0.01f + d / range));
				}
			}
		}

		if (isGardener)
		{
			if (treeBuildTarget != null)
			{
				ret += 150000 * Math.abs(loc.distanceTo(treeBuildTarget) - 2);
			}
			else
			{
				for (TreeInfo info : nearbyTrees)
				{
					float damage = info.maxHealth - info.health;
					if (damage > 5)
					{
						ret += 1000 * 20 * damage * damage * info.location.distanceTo(loc);
					}
				}
			}
		}
		if (isArchon)
		{
			for (TreeInfo info : nearbyTrees)
			{
				float d = info.getLocation().distanceTo(loc) - myRadius - info.radius;
				if (d < REPULSION_RANGE)
				{
					ret += 1000 * (1 / (0.01f + d / REPULSION_RANGE));
				}
			}    		
		}
//		if (reflection != null)
//		{
//			ret += 20000 * loc.distanceTo(reflection);
//		}
		
		if (!bruteDefence && !isArchon)
		{
			ret += bulletDodgeWeight(loc);
		}

		return ret;
	}
	
	private static final float BULLET_HIT_WEIGHT = 200000000;
	private static final float HALFPI = 1.5707963267948966192313216916398f;

	private static float bulletDodgeWeight(MapLocation loc)
	{
		float ret = 0;
		for (int i = 0; i < importantBulletIndex; i++)
		{
			BulletInfo bullet = nearbyBullets[i];
			MapLocation a = bullet.location;
			if (reflection != null && a.distanceTo(reflectionTarget.location) < a.distanceTo(loc))
			{
				continue;
			}
			Direction dir = bullet.dir;
			MapLocation b = a.add(dir, bullet.speed);
			Direction toRobot = a.directionTo(loc);
			
			float angle = Math.abs(dir.radiansBetween(toRobot)); 
			float d;
			if (angle > HALFPI)
			{
				d = a.distanceTo(loc);
			}
			else if (Math.abs(dir.radiansBetween(b.directionTo(loc))) < HALFPI)
			{
				d = b.distanceTo(loc);
			}
			else
			{
				d = (float) (a.distanceTo(loc) * Math.sin(angle));
			}
			if (d < myRadius)
			{
				ret += BULLET_HIT_WEIGHT;
			}
		}
		return ret;
	}

	static int importantBulletIndex;

	public static void preprocessBullets() throws GameActionException
	{
		importantBulletIndex = nearbyBullets.length;
		for (int i = 0; i < importantBulletIndex; i++)
		{
			BulletInfo bullet = nearbyBullets[i];
			// Get relevant bullet information
			Direction propagationDirection = bullet.dir;
			MapLocation bulletLocation = bullet.location;

			MapLocation loc = myLocation;
			// Calculate bullet relations to this robot
			Direction directionToRobot = bulletLocation.directionTo(loc);
			float distToRobot = bulletLocation.distanceTo(loc);

			boolean important = false;
			if (distToRobot < myRadius + myStride)
			{
				important = true;
			}
			else
			{
				float theta = propagationDirection.radiansBetween(directionToRobot);

				if (theta < Math.PI / 2)
				{
					// distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
					// This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
					// This corresponds to the smallest radius circle centered at our location that would intersect with the
					// line that is the path of the bullet.
					float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)
					if (perpendicularDist < myRadius + myStride)
					{
						float alongDist = (float)Math.abs(distToRobot * Math.cos(theta)); // soh cah toa :)
						int roundsToHit = (int) (alongDist / bullet.speed);
						if (roundsToHit <= 3)
							important = true;
					}
				}
			}
			if (!important)
			{
				--importantBulletIndex;
				nearbyBullets[i] = nearbyBullets[importantBulletIndex];
				nearbyBullets[importantBulletIndex] = bullet;
				--i;
			}
		}
		int lim = 10;
		if (importantBulletIndex > lim)
		{
			for (int i = 0; i < lim; i++)
			{
				float best = 1e9f;
				int idx = i;
				for (int j = i+1; j < importantBulletIndex; j++)
				{
					float d = nearbyBullets[j].getLocation().distanceTo(myLocation); 
					if (d < best)
					{
						best = d;
						idx = j;
					}
				}
				BulletInfo swp = nearbyBullets[i];
				nearbyBullets[i] = nearbyBullets[idx];
				nearbyBullets[idx] = swp;
			}
			importantBulletIndex = lim;
		}
		debug_highlightImportantBullets();
	}

	private static void debug_highlightImportantBullets() throws GameActionException
	{
		for (int i = 0; i < importantBulletIndex; i++)
		{
			BulletInfo bullet = nearbyBullets[i];
			rc.setIndicatorLine(bullet.location, bullet.location.add(bullet.dir, 0.3f), 255, 255, 0);
		}
	}
	
	public static float getIdealDistanceMultiplier(RobotType t)
	{
		if (isScout)
		{
			switch (t) {
			case LUMBERJACK:
			case SOLDIER:
			case TANK:
				return 3000;
			case SCOUT:
				return 50;
			default:
				return 0;
			}
		}
		else
		{
			switch (t) {
			case LUMBERJACK:
				return 2000;
			case GARDENER:
				return 3000;
			case ARCHON:
				return theirSpawns.length == 1 ? 500 : -100;
			case SOLDIER:
			case TANK:
				return 2500;
			case SCOUT:
				return freeRange ? 0 : 200;
			default:
				return 0;
			}
		}
	}

	public static float getStride(RobotType t)
	{
		return t.strideRadius;
	}

	public static float getIdealDistance(RobotType t)
	{
		if (isLumberjack) return 0;

		switch (t) {
		case LUMBERJACK:
			return 5; // 1.5 (lumberjack stride) + 1 radius (them) + 1 radius (us) + 1 (lumberjack attack range) + 0.5 (safety first kids)
		case GARDENER:
			return 2.1f;
		case ARCHON:
			return 3.1f;
		case SOLDIER:
		case TANK:
			return 5;
		case SCOUT:
			return 2.1f;
		default:
			return 0;
		}
	}

	static void debug_johnMadden() throws GameActionException
	{
		if (treeBuildTarget != null)
		{
			rc.setIndicatorLine(myLocation, treeBuildTarget, 0, 100, 0);
		}
		if (isGardener)
		{
			float minDamage = 9999999;
			MapLocation loc = null;
			for (TreeInfo info : nearbyTrees)
			{
				MapLocation oth = info.location;
				float d = info.maxHealth - info.health;
				if (d < minDamage)
				{
					loc = oth;
					minDamage = d;
				}
			}
			if (loc != null)
			{
				rc.setIndicatorLine(myLocation, loc, 0, 0, 100);
			}
		}
	}
	
	static void resetHistory()
	{
		history = new MapLocation[history.length];
	}
	
	static MapLocation[] beacons = new MapLocation[400];
	static boolean ignoreFriendRepulsion;
	static int beaconLen = 0;
	static int destX, destY;
	static int srcX, srcY;
	static MapLocation ttarg;
	static String s;
	
	static boolean dfs(int x, int y) throws GameActionException
	{
		if (rc.getRoundNum() - round > 20)
		{
			return false;
		}
		if (x == destX && y == destY)
		{
			return true;
		}
		if ((x != srcX || y != srcY) && getStatusWithBounds(x, y) == STATUS_IMPASSABLE)
		{
			return false;
		}
		String k = x + "," + y;
		if (s.contains(k))
		{
			return false;
		}
		s += " ";
		s += k;
		beacons[beaconLen] = new MapLocation(x, y);
		rc.setIndicatorLine(myLocation, beacons[beaconLen], 0, 255, 0);
		++beaconLen;
		float[] d = new float[4];
		boolean[] done = new boolean[4];
		for (int i = 0; i < 4; i++)
		{
			int nx = x + dx[i];
			int ny = y + dy[i];
			d[i] = new MapLocation(nx, ny).distanceTo(ttarg);
		}
		for (int i = 0; i < 4; i++)
		{
			int best = -1;
			for (int j = 0; j < 4; j++)
			{
				if (!done[j] && (best == -1 || d[j] < d[best]))
				{
					best = j;
				}
			}
			if (dfs(x + dx[best], y + dy[best]))
			{
				return true;
			}
			done[best] = true;
		}
		--beaconLen;
		return false;
	}
	
	static boolean dfsWrapped(int fromX, int fromY, int toX, int toY) throws GameActionException
	{
		destX = toX;
		destY = toY;
		ttarg = new MapLocation(destX, destY);
		s = "";
		beaconLen = 0;
		srcX = fromX;
		srcY = fromY;
		return dfs(fromX, fromY);
	}
	
	static void adHocPathfind(int myX, int myY) throws GameActionException
	{
		relayInfo(2000);
		MapLocation them = myTarget();
		resetHistory();
		attractor = null;
		if (dfsWrapped(myX, myY, (int) them.x, (int) them.y))
		{
			for (int a = 0, b = beaconLen - 1; a < b; a++, b--)
			{
				MapLocation swp = beacons[a];
				beacons[a] = beacons[b];
				beacons[b] = swp;
			}
		}
		else
		{
			attractor = myLocation.add(randomDirection(), 100);
			beacons[0] = myTarget();
			beaconLen = 1;
		}
		if (Clock.getBytecodesLeft() < 2000 || rc.getRoundNum() != round)
		{
			throw new RuntimeException();
		}
	}
	
	static boolean wasBFSAvailable = false;
	
	static void findBeacon() throws GameActionException
	{
		ignoreFriendRepulsion = beaconLen > 1;
		if (beaconLen > beacons.length)
		{
			beaconLen = 10;
			
			for (int a = 0, b = beaconLen - 1; a < b; a++, b--)
			{
				MapLocation swp = beacons[a];
				beacons[a] = beacons[b];
				beacons[b] = swp;
			}
		}
		while (beaconLen > 1 && myLocation.distanceTo(beacons[beaconLen - 1]) < 2.5f)
		{
			--beaconLen;
		}
		if (beaconLen > 1 && !isArchon)
		{
			ignoreFriendRepulsion = true;
		}
//		for (int i = 0; i + 1 < beaconLen; i++)
//		{
//			rc.setIndicatorLine(beacons[i], beacons[i+1], 255, 255, 255);
//		}
		if (isScout)
		{
			beaconLen = 1;
			beacons[0] = currentTarget;
		}
		else if (checkBlocked() && freeRange && round - lastAttackRound > 10)
		{
			int myX = (int) (0.5f + myLocation.x);
			int myY = (int) (0.5f + myLocation.y);
			adHocPathfind(myX, myY);
		}
		else if (beaconLen <= 1)
		{
			MapLocation best = myLocation;
			int bestDist = INFINITY;
			int myX = (int) (0.5f + myLocation.x);
			int myY = (int) (0.5f + myLocation.y);
			if (theirBaseFound() && readConsumerBFSDistance(myX, myY) < INFINITY && myTarget().distanceTo(theirBase) < 4)
			{
				for (int i = 0; i < 8; i++)
				{
					int nx = myX + dx[i];
					int ny = myY + dy[i];
					int d = readConsumerBFSDistance(nx, ny);
					if (d < bestDist)
					{
						bestDist = d;
						best = new MapLocation(nx, ny);
					}
				}
			}

			if (best.distanceTo(myLocation) > 1)
			{
				beacons[0] = best;
			}
			else
			{
				beacons[0] = myTarget();
			}
			beaconLen = 1;
		}
//		rc.setIndicatorLine(myLocation, beacons[beaconLen - 1], 255, 127, 0);
	}

	static MapLocation opti;

	public static void selectOptimalMove() throws GameActionException
	{
		debug_johnMadden();
		preprocessBullets();
		findBeacon();
		MapLocation best = null;
		long bestVal = 0;
		int iterations = 0;
		int longest = 0;
		int after;
		if (isLumberjack)
		{
			after = 3000;
		}
		else if (isSoldier || isTank){
			after = 400 + nearbyFriends.length*106;
		}
		else
		{
			after = 500;
		}
		if (isSoldier || isTank || isScout)
		{
			after += 284 * (nearbyEnemies.length + 1);
		}
		int iterlim = 100;
		if (isScout && nearbyBullets.length == 0 && freeRange && isScout && nearbyEnemies.length == 0)
		{
			iterlim = 5;
		}
		else if (isGardener || isArchon)
		{
			iterlim = 12;
		}
		while (Clock.getBytecodesLeft() - longest > after && iterations < iterlim)
		{
			int t1 = Clock.getBytecodesLeft();
			float add;
			if (longest > 500 && nearbyBullets.length >= 5)
			{
				add = myStride;
			}
			else
			{
				add = myStride * rand() / 360;
			}
			MapLocation cand = myLocation.add(randomDirection(), add);
			if (iterations == 0)
			{
				if (reflection != null)
				{
					if (myLocation.distanceTo(reflection) < myStride)
					{
						cand = reflection;
					}
					else
					{
						cand = myLocation.add(myLocation.directionTo(reflection), myStride);
					}
				}
				else
				{
					cand = myLocation;
				}
			}
			else if (iterations == 1 && nearbyBullets.length > 0)
			{
				cand = myLocation.add(nearbyBullets[0].dir, myStride);
			}
			else if (dominated != null)
			{
				MapLocation them = dominated.getLocation();
				switch (iterations)
				{
				case 4:
					MapLocation ncand = them.add(them.directionTo(myLocation), dominated.type.bodyRadius + myRadius + 0.001f);
					if (myLocation.distanceTo(ncand) < myStride)
					{
						cand = ncand;
					}
					else
					{
						cand = myLocation.add(myLocation.directionTo(them), myStride);
					}
					break;
				case 2:
				case 3:
					float a = myStride - 0.001f;
					float b = myRadius + dominated.type.bodyRadius + 0.001f;
					float c = myLocation.distanceTo(dominated.getLocation());
					float q = (c * c + a * a - b * b) / (2 * c * a);
					if (Math.abs(q) <= 1)
					{
						float angle = (float) Math.acos(q);
						if (iterations == 2)
						{
							cand = myLocation.add(myLocation.directionTo(dominated.getLocation()).rotateLeftRads(angle), a);
						}
						else
						{
							cand = myLocation.add(myLocation.directionTo(dominated.getLocation()).rotateLeftRads(-angle), a);							
						}
					}
					break;
				}
			}
			else if (iterations == 2 && currentTarget != null)
			{
				if (isScout && closestTree != null)
				{
					cand = myLocation.add(myLocation.directionTo(closestTree.getLocation()), myStride);
				}
				else
				{
					cand = myLocation.add(myLocation.directionTo(currentTarget), myStride);
				}
			}
			if (rc.canMove(cand))
			{
				long b = badness(cand);
				if (best == null || b < bestVal)
				{
					best = cand;
					bestVal = b;
				}
			}
			++iterations;
			int taken = t1 - Clock.getBytecodesLeft();
			longest = Math.max(longest, taken);
		}
		debug_printAfterMovementLoop(iterations, longest);
		if (best != null)
			opti = best;
		else
			opti = myLocation;

	}
	
	static void debug_printAfterMovementLoop(int iterations, int longest)
	{
		System.out.println(iterations + " iterations; the longest one cost " + longest + "; " + nearbyBullets.length + "/" + importantBulletIndex);
	}

	public static void onRoundEnd() throws GameActionException
	{
		if (isGardener)
		{
			int dist = (int) ((myLocation.distanceTo(destination) + 7) * 1000);
			if (dist > rc.readBroadcast(CHANNEL_CONTROL_RADIUS))
			{
				rc.broadcast(CHANNEL_CONTROL_RADIUS, dist);
			}
		}
		if (closestTree != null)
		{
			rc.setIndicatorLine(myLocation, closestTree.getLocation(), 255, 255, 255);
		}
		if (rc.hasAttacked())
		{
			lastAttackRound = round;
		}
		burnCycles(Clock.getBytecodesLeft() - 500);
	}

	public static void onRoundBegin() throws GameActionException
	{
		roam = false;
		theirBase = readPoint(CHANNEL_THEIR_BASE);
		nearbyFriends = rc.senseNearbyRobots(100, myTeam);
		nearbyEnemies = rc.senseNearbyRobots(100, myTeam.opponent());
		nearbyBullets = rc.senseNearbyBullets();
		if (isGardener)
		{
			nearbyTrees = rc.senseNearbyTrees(-1, myTeam);
			neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
			treeBuildTarget = null;
		}
		else
		{
			nearbyTrees = rc.senseNearbyTrees();
		}
		for (TreeInfo ti : nearbyTrees)
		{
//			if (ti.getContainedRobot() != null) {
//				writePoint(CHANNEL_CHOPPABLE_TREE, ti.getLocation());
//			}
			if (ti.containedBullets > 0)
			{
				rc.broadcastBoolean(CHANNEL_IS_SCOUT_USEFUL, true);
			}
		}
		leftBound = rc.readBroadcastFloat(CHANNEL_MAP_LEFT);
		rightBound = rc.readBroadcastFloat(CHANNEL_MAP_RIGHT);
		bottomBound = rc.readBroadcastFloat(CHANNEL_MAP_BOTTOM);
		topBound = rc.readBroadcastFloat(CHANNEL_MAP_TOP);
		debug_printBounds();
		myLocation = rc.getLocation();
		history[round % history.length] = myLocation;
		controlRadius = rc.readBroadcast(CHANNEL_CONTROL_RADIUS) / 1000f;
		helpLocation = readPoint(CHANNEL_CALL_FOR_HELP);
		helpRound = rc.readBroadcast(CHANNEL_CALL_FOR_HELP_ROUND);
		oldGardenerLocChannel = CHANNEL_GARDENER_LOCATIONS + 50 * (round % 2);
		newGardenerLocChannel = CHANNEL_GARDENER_LOCATIONS + 50 * ((round + 1) % 2);
		aggro = rc.readBroadcast(CHANNEL_ATTACK) != 0;
		happyPlace = readPoint(CHANNEL_HAPPY_PLACE);

		float d = myLocation.distanceTo(destination);
		if (controlRadius - 1 < d && d < controlRadius + 1)
		{
			writePoint(CHANNEL_HAPPY_PLACE, myLocation);
		}

		if (rc.readBroadcast(CHANNEL_CURRENT_ROUND) != round)
		{
			if (round % 10 == 0)
			{
				rc.broadcast(CHANNEL_CONTROL_RADIUS, 0);
			}
			rc.broadcast(CHANNEL_CURRENT_ROUND, round);
			rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_ARCHONS), 0);
			rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_GARDENERS), 0);
			rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_SOLDIERS), 0);
			rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_TANKS), 0);
			rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_LUMBERJACKS), 0);
			rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_SCOUTS), 0);
			rc.broadcast(newGardenerLocChannel, 0);
			if (theirBaseFound())
			{
				rc.setIndicatorDot(theirBase, 127, 255, 255);
			}
		}
		int myWrite = writeNumberChannel(numberOfChannel);
		rc.broadcast(myWrite, rc.readBroadcast(myWrite) + 1);

		if (isGardener)
		{
			gardenerLocsLen = rc.readBroadcast(oldGardenerLocChannel);
			if (gardenerLocsLen <= GARDENER_LOC_LIMIT)
			{
				for (int i = 0; i < gardenerLocsLen; i++)
				{
					gardenerLocs[i] = readPoint(oldGardenerLocChannel + 1 + i * 2);
				}
			}
			else
			{
				gardenerLocsLen = 0;
				for (RobotInfo info : nearbyFriends)
				{
					if (info.getType() == RobotType.GARDENER)
					{
						gardenerLocs[gardenerLocsLen++] = info.getLocation();
					}
				}
			}
			int clen = rc.readBroadcast(newGardenerLocChannel);
			if (clen <= GARDENER_LOC_LIMIT)
			{
				writePoint(newGardenerLocChannel + 1 + clen * 2, myLocation);
				rc.broadcast(newGardenerLocChannel, clen + 1);
			}
		}

		threatened = false;
		loop:
			for (RobotInfo info : nearbyEnemies)
			{
				switch (info.getType())
				{
				case LUMBERJACK:
				case SOLDIER:
				case TANK:
				case SCOUT:
					threatened = true;
					if (!hasBeenThreatened)
					{
						hasBeenThreatened = true;
						lastGardenerHitRound = round;
					}
					break loop;
				default:
					;
				}
			}

		bruteDefence = false;
//		if (isSoldier && threatened && rc.getHealth() > 10)
//		{
//			for (RobotInfo info : nearbyFriends)
//			{
//				if (info.getType() == RobotType.GARDENER)
//				{
//					bruteDefence = true;
//					break;
//				}
//			}
//		}
		if (aggro)
		{
			bruteDefence = false;
			controlRadius = Math.max(GameConstants.MAP_MAX_HEIGHT, GameConstants.MAP_MAX_WIDTH);
			if (isSoldier || isLumberjack || isTank)
			{
				freeRange = true;
			}
		}
		if (bruteDefence)
		{
			rc.setIndicatorDot(myLocation, 0, 0, 0);
		}

		if (isGardener)
		{
			initHexes();
			lastScoutBuildTime = rc.readBroadcast(CHANNEL_LAST_SCOUT_BUILD_TIME);
		}
		else if (isScout)
		{
			findEasyTargets();
			trees = rc.getTreeCount();
		}
		else if (isLumberjack)
		{
			if (choppableTree != null && rc.canSenseLocation(choppableTree) && !rc.isLocationOccupiedByTree(choppableTree)) {
				rc.setIndicatorDot(choppableTree, 255, 0, 0);
				choppableTree = null;
			}

			MapLocation tree = readPoint(CHANNEL_CHOPPABLE_TREE);
			if (tree.x >= 0 && (choppableTree == null || myLocation.distanceTo(tree) < myLocation.distanceTo(choppableTree)))
				choppableTree = tree;
		}
		if (rc.getRoundNum() + 2 >= rc.getRoundLimit() || rc.getTeamVictoryPoints() + rc.getTeamBullets() / rc.getVictoryPointCost() > 1000)
		{
			donate();
		}
		if (rc.getRoundNum() >= 2750 || (rc.getRoundNum() >= 2500 && rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_ARCHONS)) == 0))
		{
			donate();
		}
		findBounds();
		if (isLumberjack && !freeRange)
		{
			if (checkBlocked() || noNeutralTrees())
			{
				freeRange = true;
			}
		}
		if (isLumberjack && freeRange && !noNeutralTrees())
		{
			freeRange = false;
		}
		debug_highlightGrid();
		if (isArchon && myID == 0)
		{
			if (round == 8 || (round % 100 == 0 && rc.readBroadcastBoolean(CHANNEL_CRAMPED)))
			{
				if (round > 200)
				{
					rc.broadcastBoolean(CHANNEL_CRAMPED, false);
				}
				else
				{
					boolean b = dfsWrapped(
							(int) myLocation.x,
							(int) myLocation.y,
							(int) theirSpawns[0].x,
							(int) theirSpawns[0].y);
					rc.broadcastBoolean(CHANNEL_CRAMPED, !b);
					if (b)
					{
						rc.setIndicatorLine(myLocation, theirSpawns[0], 0, 0, 0);
					}
					System.out.println("CRAMPED = " + !b);
				}
			}
		}
		if (!theirBaseFound())
		{
			for (RobotInfo info : nearbyEnemies)
			{
				if (info.getType() == RobotType.GARDENER)
				{
					writePoint(CHANNEL_THEIR_BASE, info.getLocation());
					break;
				}
			}
		}
		else if (myLocation.distanceTo(theirBase) < 3 && !canSeeValuableTargets())
		{
			rc.broadcastInt(CHANNEL_THEIR_BASE, 0);
		}
		archons = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_ARCHONS));
		if (round > 400 && archons == 0 && rc.getTeamVictoryPoints() <= rc.getOpponentVictoryPoints() + 2 && rc.getTreeCount() == 0)
		{
			donate();
		}
		if (isArchon)
		{
			burnCycles(Clock.getBytecodesLeft() / 2 - 500);
		}
		else if (!isGardener && nearbyEnemies.length == 0 && nearbyBullets.length == 0)
		{
			burnCycles(Clock.getBytecodesLeft() / 4);
		}
	}
	
	static boolean noNeutralTrees()
	{
		for (TreeInfo info : nearbyTrees)
		{
			if (info.getTeam() == Team.NEUTRAL)
			{
				return false;
			}
		}
		return true;
	}
	
	static void findBounds() throws GameActionException
	{
		float r = myType.sensorRadius - 0.1f;
		float lx = myLocation.x - r;
		float rx = myLocation.x + r;
		float ty = myLocation.y - r;
		float by = myLocation.y + r;
		if (lx > leftBound && !rc.onTheMap(new MapLocation(lx, myLocation.y)))
		{
			rc.broadcastFloat(CHANNEL_MAP_LEFT, lx);
		}
		if (rx < rightBound && !rc.onTheMap(new MapLocation(rx, myLocation.y)))
		{
			rc.broadcastFloat(CHANNEL_MAP_RIGHT, rx);
		}
		if (ty > topBound && !rc.onTheMap(new MapLocation(myLocation.x, ty)))
		{
			rc.broadcastFloat(CHANNEL_MAP_TOP, ty);
		}
		if (by < bottomBound && !rc.onTheMap(new MapLocation(myLocation.x, by)))
		{
			rc.broadcastFloat(CHANNEL_MAP_BOTTOM, by);
		}
	}
	
	private static void debug_printBounds() {
		System.out.println("[" + leftBound + " " + topBound + " -- " + rightBound + " " + bottomBound + "]");
	}
	
	static final int STATUS_IMPASSABLE = 1;
	static final int STATUS_PASSABLE = 0;
	
	static final int TORUS_SIZE = 104;
	
	static final int CHANNEL_BFS_KEY = 999;
	static final int CHANNEL_MAP = 1000;
	static final int CHANNEL_DIST_INFO = CHANNEL_MAP + TORUS_SIZE * TORUS_SIZE / 32 + 1;
	static final int DIST_BITS = 16;
	static final int DIST_DPART = 14;
	static final int CHANNEL_QUEUE_LENGTH = CHANNEL_DIST_INFO + TORUS_SIZE * TORUS_SIZE * DIST_BITS / 32 + 1;
	static final int CHANNEL_QUEUE_BEGIN_POS = CHANNEL_QUEUE_LENGTH + 1;
	static final int CHANNEL_QUEUE = CHANNEL_QUEUE_BEGIN_POS + 1;
	static final int QUEUE_MAX_LEN = GameConstants.BROADCAST_MAX_CHANNELS - CHANNEL_QUEUE;
	
	static int getDistInfo(int x, int y) throws GameActionException
	{
		x %= TORUS_SIZE;
		y %= TORUS_SIZE;
		x += y * TORUS_SIZE;
		return rc.readBroadcastInt(CHANNEL_DIST_INFO + x / 2) >> (x % 2 * DIST_BITS);
	}
	
	static int popQueue() throws GameActionException
	{
		int len = rc.readBroadcast(CHANNEL_QUEUE_LENGTH);
		if (len == 0)
		{
			return -1;
		}
		else
		{
			rc.broadcast(CHANNEL_QUEUE_LENGTH, len - 1);
			int begin = rc.readBroadcast(CHANNEL_QUEUE_BEGIN_POS);
			rc.broadcast(CHANNEL_QUEUE_BEGIN_POS, begin + 1);
			return rc.readBroadcast(CHANNEL_QUEUE + begin % QUEUE_MAX_LEN);
		}
	}
	
	static int currentBFSKey;
	static final int BFS_KEY_COUNT = 4;
	
	static int packCoordinates(int x, int y)
	{
		return (x << 16) | y;
	}
	
	static void pushQueue(int x) throws GameActionException
	{
		int len = rc.readBroadcast(CHANNEL_QUEUE_LENGTH);
		rc.broadcast(CHANNEL_QUEUE_LENGTH, len + 1);
		int begin = rc.readBroadcast(CHANNEL_QUEUE_BEGIN_POS);
		rc.broadcast(CHANNEL_QUEUE + (begin + len) % QUEUE_MAX_LEN, x);
	}
	
	static void initBFS() throws GameActionException
	{
		++currentBFSKey;
		currentBFSKey %= BFS_KEY_COUNT;
		rc.broadcast(CHANNEL_BFS_KEY, currentBFSKey);
		rc.broadcast(CHANNEL_QUEUE_LENGTH, 0);
		int x = (int) theirBase.x;
		int y = (int) theirBase.y;
		System.out.println("BFS INIT");
		setBFSDistance(x, y, 0);
		pushQueue(packCoordinates(x, y));
	}
	
	static void setBFSDistance(int x, int y, int d) throws GameActionException
	{
		x %= TORUS_SIZE;
		y %= TORUS_SIZE;
		x *= TORUS_SIZE;
		x += y;
		int shift = x % 2 * 16;
		int channel = CHANNEL_DIST_INFO + x / 2;
		int val = rc.readBroadcast(channel);
		d |= currentBFSKey << 14; 
		d <<= shift;
		val &= ~(65535 << shift);
		rc.broadcast(channel, val | d);
	}
	
	static int readBFSDistance(int x, int y) throws GameActionException
	{
		x %= TORUS_SIZE;
		y %= TORUS_SIZE;
		x *= TORUS_SIZE;
		x += y;
		int shift = x % 2 * 16;
		int channel = CHANNEL_DIST_INFO + x / 2;
		int q = 65535 & (rc.readBroadcast(channel) >> shift);
		if ((q >> 14) != currentBFSKey)
		{
			return -1;
		}
		else
		{
			return q & 16383;
		}
	}
	
	static int readConsumerBFSDistance(int x, int y) throws GameActionException
	{
		x %= TORUS_SIZE;
		y %= TORUS_SIZE;
		x *= TORUS_SIZE;
		x += y;
		int shift = x % 2 * 16;
		int channel = CHANNEL_DIST_INFO + x / 2;
		int q = 16383 & (rc.readBroadcast(channel) >> shift);
		if (q == 0)
		{
			return INFINITY;
		}
		else
		{
			return q;
		}
	}
	
	static final int[] dx = {-1, 0, 1, 0, -1, 1, 1, -1};
	static final int[] dy = {0, 1, 0, -1, -1, -1, 1, 1};
	
	static void debug_bfs_dot(int x, int y)
	{
		switch (currentBFSKey)
		{
		case 0: rc.setIndicatorDot(new MapLocation(x, y), 255, 255, 0); break;
		case 1: rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 0); break;
		case 2: rc.setIndicatorDot(new MapLocation(x, y), 0, 0, 255); break;
		case 3: rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 255); break;
		}
	}
	
	static boolean isBFSActive()
	{
		return round >= 300;
	}
	
	static void bfsStep() throws GameActionException
	{
		if (!isBFSActive())
		{
			return;
		}
		currentBFSKey = rc.readBroadcast(CHANNEL_BFS_KEY);
		int q = popQueue();
		if (q == -1)
		{
			initBFS();
		}
		else
		{
			int x = q >> 16;
			int y = q & 65535;
			debug_bfs_dot(x, y);
			int d = readBFSDistance(x, y);
			for (int i = 0; i < 4 && Clock.getBytecodesLeft() > 500; i++)
			{
				int nx = x + dx[i];
				int ny = y + dy[i];
				if (readBFSDistance(nx, ny) == -1)
				{
					if (getStatusWithBounds(nx, ny) == STATUS_PASSABLE)
					{
						setBFSDistance(nx, ny, d + 1);
						pushQueue(packCoordinates(nx, ny));
					}
					else
					{
						setBFSDistance(nx, ny, INFINITY);
					}
				}
			}
		}
	}
	
	static void doBFS(int allowed) throws GameActionException
	{
		int num = Clock.getBytecodeNum();
		while (Clock.getBytecodeNum() - num < allowed - 1500 && Clock.getBytecodesLeft() > 2000 && rc.getRoundNum() == round)
		{
			bfsStep();
		}
	}
	
	static int getStatus(int x, int y) throws GameActionException
	{
		x %= TORUS_SIZE;
		y %= TORUS_SIZE;
		x += y * TORUS_SIZE;
		return 1 & (rc.readBroadcastInt(CHANNEL_MAP + x / 32) >> (x % 32));
	}
	
	static int getStatusWithBounds(int x, int y) throws GameActionException
	{
		if (x < leftBound)
		{
			return STATUS_IMPASSABLE;
		}
		if (x > rightBound)
		{
			return STATUS_IMPASSABLE;
		}
		if (y < topBound)
		{
			return  STATUS_IMPASSABLE;
		}
		if (y > bottomBound)
		{
			return STATUS_IMPASSABLE;
		}
		return getStatus(x, y);
	}
	
	static void writeStatus(int x, int y, int status) throws GameActionException
	{
		x %= TORUS_SIZE;
		y %= TORUS_SIZE;
		x += y * TORUS_SIZE;
		int idx = CHANNEL_MAP + x / 32;
		int shift = x % 32;
		int v = rc.readBroadcastInt(idx);
		v &= ~(1 << (shift));
		v |= status << shift;
		rc.broadcastInt(idx, v);
	}
	
	private static void debug_highlightGrid() throws GameActionException
	{
		if (!isArchon || myID != 0)
		{
			return;
		}
		if (theirBaseFound())
		{
			rc.setIndicatorDot(theirBase, 255, 255, 255);
		}

		if (round % 100 != 9)
		{
			return; // this code lags the client
		}
		int l = (int) myLocation.x - 100;
		int r = l + 200;
		int t = (int) myLocation.y - 100;
		int b = t + 200;
//		for (int i = l; i <= r; i++)
//		{
//			rc.setIndicatorLine(new MapLocation(i, t), new MapLocation(i, b), 0, 0, 0);
//		}
//		for (int j = t; j <= b; j++)
//		{
//			rc.setIndicatorLine(new MapLocation(l, j), new MapLocation(r, j), 0, 0, 0);
//		}
		for (int i = l; i <= r; i++)
		{
			for (int j = t; j <= b; j++)
			{
				switch (getStatusWithBounds(i, j))
				{
				case STATUS_PASSABLE:
//					rc.setIndicatorLine(
//							new MapLocation(i - .1f, j - .1f),
//							new MapLocation(i + .1f, j + .1f),
//							0, 255, 0);
					break;
				case STATUS_IMPASSABLE:
					rc.setIndicatorDot(new MapLocation(i, j), 0, 0, 255);
//					rc.setIndicatorLine(
//							new MapLocation(i - .1f, j - .1f),
//							new MapLocation(i + .1f, j + .1f),
//							255, 0, 0);
					break;
				}
			}
		}
	}
	
	static void debug_printTimeTaken(String cause, int original)
	{
		System.out.println(cause + " took " + (original - Clock.getBytecodesLeft()) + " bytecodes");
	}

	static boolean checkBlocked()
	{
		for (int i = 0; i < history.length; i++)
		{
			if (history[i] == null)
			{
				return false;
			}
			if (history[i].distanceTo(myLocation) > 2)
			{
				return false;
			}
		}
		return true;
	}
	
	static boolean findReflection() throws GameActionException
	{
		RobotInfo gardener = null;
		RobotInfo shooter = null;
		for (int i = nearbyEnemies.length - 1; i >= 0; i--)
		{
			RobotInfo enemy = nearbyEnemies[i];
			switch (enemy.getType())
			{
			case GARDENER:
				gardener = enemy;
				if (myLocation.distanceTo(enemy.getLocation()) < 4)
				{
					lastGardenerHitRound = round;
				}
				break;
			case SCOUT:
			case TANK:
			case SOLDIER:
			case LUMBERJACK:
				if (shooter == null)
				{
					shooter = enemy;
				}
				else
				{
					return false;
				}
			}
		}
		if (gardener != null)
		{
			MapLocation b = gardener.getLocation();
			MapLocation a;
			if (shooter == null)
			{
				a = b.add(myLocation.directionTo(b));
			}
			else
			{
				a = shooter.getLocation();	    		
			}
			for (float f = 2.001f; f < 6; f += 1.8f)
			{
				MapLocation c = b.add(a.directionTo(b), f);
				if (canGoTo(c))
				{
					reflection = c;
					reflectionTarget = gardener;
					debug_line(a, c, 255, 255, 0);
					debug_line(myLocation, c, 127, 127, 0);
					return true;
				}
			}
		}
		return false;
	}
	
	static void findEasyTargets() throws GameActionException
	{
		reflection = null;
		dominated = null;
		reflectionTarget = null;
		if (findReflection())
		{
			return;
		}
	}

	private static void debug_highlightDominated() throws GameActionException
	{
		if (dominated != null)
		{
			rc.setIndicatorLine(myLocation, dominated.getLocation(), 200, 100, 0);
		}
	}
	static void debug_line(MapLocation p1, MapLocation p2, int r, int g, int b) throws GameActionException
	{
		rc.setIndicatorLine(p1, p2, r, g, b);
	}

	static boolean canGoTo(MapLocation loc) throws GameActionException
	{
		if (isScout)
		{
			if (!rc.canSenseAllOfCircle(loc, myRadius) || !rc.onTheMap(loc, myRadius))
			{
				return false;
			}
			for (RobotInfo info : nearbyEnemies)
			{
				if (info.getLocation().distanceTo(loc) < info.getRadius() + 1)
				{
					return false;
				}
			}
			//    		for (RobotInfo info : nearbyFriends)
				//    		{
				//    			if (info.getLocation().distanceTo(loc) < info.getRadius() + 1)
					//    			{
					//    				return false;
					//    			}
				//    		}
			return true;
		}
		else
		{
			return
					rc.canSenseAllOfCircle(loc, myRadius) &&
					!rc.isCircleOccupiedExceptByThisRobot(loc, myRadius) &&
					rc.onTheMap(loc, myRadius);
		}
	}

	public static final float hexSize = 4.2f;
	public static final float rowSpacing = (float) Math.sqrt(3) / 1.75f * hexSize;

	static MapLocation hexToCartesian(int x, int y)
	{
		return new MapLocation(
				hexSize * (x + y % 2 * 0.5f),
				y * rowSpacing);
	}

	static void findHex(MapLocation loc)
	{
		retHelper2 = (int) (loc.y / rowSpacing + 0.5f);
		retHelper1 = (int) ((loc.x / hexSize - retHelper2 % 2 * 0.5f) + 0.5f);
	}

	static void initHexes() throws GameActionException
	{
		findHex(myLocation);
		int bx = retHelper1;
		int by = retHelper2;
		hexes[0] = hexToCartesian(bx, by);
		hexLen = 1;
		{
			outer:
				for (int i = 0; ; i++)
				{
					for (int j = 0; ; j++)
					{
						MapLocation loc = hexToCartesian(bx + i, by + j);
						if (rc.canSenseAllOfCircle(loc, 1) && rc.onTheMap(loc, 1))
						{
							hexes[hexLen++] = loc;
						}
						else if (j == 0)
						{
							break outer;
						}
						else
						{
							break;
						}
					}
					for (int j = -1; ; j--)
					{
						MapLocation loc = hexToCartesian(bx + i, by + j);
						if (rc.canSenseAllOfCircle(loc, 1) && rc.onTheMap(loc, 1))
						{
							hexes[hexLen++] = loc;
						}
						else
						{
							break;
						}
					}
				}
		}
		{
			outer:
				for (int i = -1; ; i--)
				{
					for (int j = 0; ; j++)
					{
						MapLocation loc = hexToCartesian(bx + i, by + j);
						if (rc.canSenseAllOfCircle(loc, 1) && rc.onTheMap(loc, 1))
						{
							hexes[hexLen++] = loc;
						}
						else if (j == 0)
						{
							break outer;
						}
						else
						{
							break;
						}
					}
					for (int j = -1; ; j--)
					{
						MapLocation loc = hexToCartesian(bx + i, by + j);
						if (rc.canSenseAllOfCircle(loc, 1) && rc.onTheMap(loc, 1))
						{
							hexes[hexLen++] = loc;
						}
						else
						{
							break;
						}
					}
				}
		}
		debug_highlightHexes();
	}

	static void debug_highlightHexes() throws GameActionException
	{
		for (int i = 0; i < hexLen; i++)
		{
			rc.setIndicatorDot(hexes[i], 0, 0, 0);
		}
	}

	static long crnt = 17;

	static long rand()
	{
		crnt *= 349820432;
		crnt += 543857345;
		crnt %= 1000000007;
		return crnt % 360;
	}
	
	static float rand01()
	{
		return rand() / 360f;
	}

	/**
	 * Returns a random Direction
	 * @return a random Direction
	 */
	 static Direction randomDirection() {
		return new Direction((float) (rand() / 6.283185307179586476925286766559));
	}

	static Direction randomDirection(int deg) {
		return new Direction((float) ((rand()/deg*deg) / 6.283185307179586476925286766559));
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		return tryMove(dir,20,3);
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
	 *
	 * @param dir The intended direction of movement
	 * @param degreeOffset Spacing between checked directions (degrees)
	 * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

		// First, try intended direction
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
		boolean moved = false;
		int currentCheck = 1;

		while(currentCheck<=checksPerSide) {
			// Try the offset of the left side
			if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
				rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
				return true;
			}
			// Try the offset on the right side
			if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
				rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}
	
	static int myNumberOfChannel()
	{
		switch (myType)
		{
		case ARCHON: return CHANNEL_NUMBER_OF_ARCHONS;
		case GARDENER: return CHANNEL_NUMBER_OF_GARDENERS;
		case SOLDIER: return CHANNEL_NUMBER_OF_SOLDIERS;
		case LUMBERJACK: return CHANNEL_NUMBER_OF_LUMBERJACKS;
		case SCOUT: return CHANNEL_NUMBER_OF_SCOUTS;
		case TANK: return CHANNEL_NUMBER_OF_TANKS;
		}
		throw new RuntimeException();
	}

	static int writeNumberChannel(int x)
	{
		return x + 1 + round % 2;
	}

	static int readNumberChannel(int x)
	{
		return x + 1 + (round + 1) % 2;
	}

	public static MapLocation readPoint(int pos) throws GameActionException
	{
		return new MapLocation(rc.readBroadcastFloat(pos), rc.readBroadcastFloat(pos + 1));
	}

	public static void writePoint(int pos, MapLocation val) throws GameActionException
	{
		rc.broadcastFloat(pos, val.x);
		rc.broadcastFloat(++pos, val.y);
	}

	public static void increment(int channel) throws GameActionException
	{
		rc.broadcastInt(channel, rc.readBroadcastInt(channel) + 1);
	}

}
