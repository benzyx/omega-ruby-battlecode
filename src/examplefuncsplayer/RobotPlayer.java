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
	public static final int CHANNEL_ARCHON_CRAMP_RECORDING = 132;
	public static final int CHANNEL_EVER_FOUND_GARDENER = 135;
	public static final int CHANNEL_LAST_ANYTHING_BUILD_TIME = 136;
	public static final int CHANNEL_FIRST_BUILD_DIRECTION = 137;
	public static final int CHANNEL_LEADER_ID = 138;
	public static final int CHANNEL_LEADER_ROUND_TIMESTAMP = 139;
	public static final int CHANNEL_THEIRBASE_IS_GARDENER = 140;
	public static final int CHANNEL_OUR_BUILDS = 141;
	public static final int MAX_OUR_BUILD_LEN = 20;
	public static final int ____ = 182;
	
	// Channels 1500 - 1550 are reserved for storing hex locations
	public static final int CHANNEL_HEX_LOCATIONS = 5500;
	public static final int CHANNEL_START_LOCATION = 5552;
	public static final int CHANNEL_END_LOCATION = 5554;
	public static final int CHANNEL_TARGET_DIRECTIONS = 5556;
	public static final int CHANNEL_HEX_SIZE = 5557;
	public static final int CHANNEL_ROW_SPACING = 5558;
	public static final int CHANNEL_HEX_OFFSET_X = 5559;
	public static final int CHANNEL_HEX_OFFSET_Y = 5560;
	public static final int HEX_TORUS_SIZE = 32;

	public static final float REPULSION_RANGE = 1.7f;

	static float topBound, leftBound, bottomBound, rightBound;
	static RobotController rc;
	static int myID;
	static MapLocation centreOfBase = null;
	static MapLocation currentTarget = null;
	//    static Direction prevDirection = randomDirection();
	static RobotInfo[] nearbyEnemies;
	static RobotInfo[] nearbyFriends;
	static BulletInfo[] nearbyBullets;
	static TreeInfo[] nearbyTrees;
	static TreeInfo[] neutralTrees;
//	static MapLocation[] repellers = new MapLocation[25];
//	static int[] repelWeight = new int[25];
    static MapLocation[] history = new MapLocation[30];
	static TreeInfo closestTree = null;	// scouts use this to shake trees
	static int numberOfChannel;
	static RobotType myType;
	static Team myTeam;
	static boolean isScout, isArchon, isGardener, isSoldier, isTank, isLumberjack;
	static float myStride;
	static float myRadius;
	static MapLocation myLocation;
	static MapLocation myOriginalLocation;
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
	static MapLocation closestThreat;
	static boolean canSeeThreat;
	static MapLocation[] ourSpawns;
	static boolean isLeader;
	

	static MapLocation targetHex;
	static int targetDirections;
	static boolean inHex = false;
	static int retHelper1, retHelper2;

	public static float hexSize = 9f; // 8f
	public static float rowSpacing = (float) Math.sqrt(3) / 2.0f * hexSize;
	public static float offsetHexX, offsetHexY;

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
		myLocation = myOriginalLocation = rc.getLocation();
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
		if (isArchon)
		{
			ourSpawns = rc.getInitialArchonLocations(myTeam);
			sortByDistanceFromMe(ourSpawns);
			if (myID == 0)
			{
				becomeLeader();
			}
		}
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
				writePoint(CHANNEL_START_LOCATION, rally);
				writePoint(CHANNEL_END_LOCATION, them);
				writePoint(CHANNEL_CHOPPABLE_TREE, new MapLocation(-1, -1));
				rc.broadcastFloat(CHANNEL_MAP_TOP, -INFINITY);
				rc.broadcastFloat(CHANNEL_MAP_LEFT, -INFINITY);
				rc.broadcastFloat(CHANNEL_MAP_RIGHT, INFINITY);
				rc.broadcastFloat(CHANNEL_MAP_BOTTOM, INFINITY);
				rc.broadcast(CHANNEL_INITIAL_ARCHON_BLOCKED, 0);
				rc.broadcastInt(CHANNEL_MAP_START_X, -INFINITY);
				rc.broadcastInt(CHANNEL_MAP_START_Y, -INFINITY);
				rc.broadcast(CHANNEL_LAST_ANYTHING_BUILD_TIME, -100);
			}
			else
			{
				//        		freeRange = true; // suicide mission
			}
		}


		MapLocation start = readPoint(CHANNEL_START_LOCATION);
		MapLocation end = readPoint(CHANNEL_END_LOCATION);

		int[] td = new int[6];
		
		for (int i = 0; i < 6; i++)
			td[i] = i;
		
		int bitstring = 0;
		
		for (int i = 0; i < 5; i++) {
			int minIndex = i;
			MapLocation minLocation = start.add(new Direction(td[i] * 60/57.2957795131f), 2.0f);
			for (int j = i + 1; j < 6; j++) {
				MapLocation nextLocation = start.add(new Direction(td[j] * 60/57.2957795131f), 2.0f);
				if (nextLocation.distanceTo(end) < minLocation.distanceTo(end)) {
					minLocation = nextLocation;
					minIndex = j;
				}
			}
			int temp = td[i];
			td[i] = td[minIndex];
			td[minIndex] = temp;
			bitstring |= 1 << td[i];
		}
		
		targetDirections = bitstring;
		/*
		switch (bitstring)
		{ // 3 to 5 if tanks
			case 0b111100:
			case 0b011110:
			case 0b100111:
			case 0b110011:
				hexSize = 5 + 2 + 1 + 1;
				rowSpacing = 5 + 3 + 2;
				break;
			
			case 0b111001:
			case 0b001111:
				hexSize = 5 + 3 + 3;
				rowSpacing = 5 + 1 + 1 + 2;
		}
		*/
		
		hexSize = 3 + 2 + 2 + 1;
		rowSpacing = 3 + 3 + 2;
		
		if (isGardener && trees == 0 && gardeners == 0) {
			rc.broadcastFloat(CHANNEL_HEX_OFFSET_X, rc.getLocation().x);
			rc.broadcastFloat(CHANNEL_HEX_OFFSET_Y, rc.getLocation().y);
		} else {
			freeRange = true;
		}
		
		centreOfBase = readPoint(CHANNEL_RALLY_POINT);
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
				centreOfBase = readPoint(CHANNEL_RALLY_POINT);
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

					if (rc.canMove(loc) && (!isGardener || !inHex))
					{
						rc.move(loc);
						myLocation = loc;
					}
				}

				if (isLumberjack)
				{
					closestTree = null;
					TreeInfo myClosestTree = null;
					TreeInfo myClosestChopTree = null;
					float minDist = 1e9f;
					float chopMinDist = 1e9f; 
					for (TreeInfo info : nearbyTrees)
					{
						if (info.getTeam() == myTeam)
						{
							continue;
						}
						float d = reverseValueOfTree(info);
						if (d < minDist)
						{
							minDist = d;
							myClosestTree = info;
						}
						if (rc.canChop(info.ID))
						{
							if (d < chopMinDist)
							{
								chopMinDist = d;
								myClosestChopTree = info;
							}
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
					}
					if (myClosestChopTree != null)
					{
						if (rc.canShake(myClosestChopTree.ID))
						{
							rc.setIndicatorDot(myClosestChopTree.location, 0, 255, 0);
							rc.shake(myClosestChopTree.ID);
						}
						if (rc.canChop(myClosestChopTree.ID))
						{
							rc.chop(myClosestChopTree.ID);
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
				if (!rc.hasAttacked() && (isSoldier || isTank))
				{
					trees = rc.getTreeCount();
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
									(info.getType() == RobotType.GARDENER && trees >= 4)))
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

					smartShot(dir, enemyType, enemyDistance);

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
	
	private static float reverseValueOfTree(TreeInfo info)
	{
		float ret = myLocation.distanceTo(info.location);
		if (info.containedRobot != null)
		{
			switch (info.containedRobot)
			{
			case GARDENER:
				ret -= 10;
				break;
			case LUMBERJACK:
				ret -= 200;
				break;
			case SOLDIER:
				ret -= 150;
				break;
			case TANK:
				ret -= 250;
				break;
			case ARCHON:
				break;
			case SCOUT:
				ret -= 20;
				break;
			}
		}
		return ret;
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
			default:
				;
			}
		}
		return false;
	}
	
	public static void smartShot(Direction dir, RobotType enemyType, float enemyDistance) throws GameActionException{
		if (dir == null) return;

		for(RobotInfo info : nearbyFriends){
			if (info.getLocation().distanceTo(myLocation) < enemyDistance && willCollideWithTarget(myLocation, dir, info)){
				friendlyFireSpot = true;
//				rc.setIndicatorDot(myLocation, 255, 255, 0);
				return; // friendly fire straight up
			}

			//if (Math.abs(myLocation.directionTo(info.getLocation()).degreesBetween(dir)) < 15) badTriad++;
			//if (Math.abs(myLocation.directionTo(info.getLocation()).degreesBetween(dir)) < 30) badPentad++;
		}
		if (enemyType == RobotType.SCOUT && enemyDistance > 3.1f)
		{
			return;
		}
		if (enemyType == RobotType.ARCHON && trees < 3 && round < 750 && rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_ARCHONS)) > 0)
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
	
	static void becomeLeader() throws GameActionException
	{
		isLeader = true;
		rc.broadcast(CHANNEL_LEADER_ID, rc.getID());
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
	
	static int smallestCramp() throws GameActionException
	{
		int ans = Integer.MAX_VALUE;
		for (int i = 0; i < ourSpawns.length; i++)
		{
			ans = Math.min(ans, rc.readBroadcastInt(CHANNEL_ARCHON_CRAMP_RECORDING + i));
		}
		return ans;
	}
	
	static void round1Planning() throws GameActionException
	{
		int cramp = computeCramp();
		rc.broadcastInt(CHANNEL_ARCHON_CRAMP_RECORDING + myID, cramp);
		int best = smallestCramp();
		System.out.println("Cramp = " + cramp + "; best = " + best);
		if (cramp / 100 == best / 100 && cramp % 100 < best % 100 * 1.05f)
		{
			executeBuildPlan();
		}
	}
	
	static Direction[] buildPlan = new Direction[3];
	static int buildPlanLen = 0;
	
	static void executeBuildPlan() throws GameActionException
	{
		if (buildPlanLen >= 2 || (buildPlanLen == 1 && round >= 30))
		{
			becomeLeader();
			executeBuild(buildPlan[0], RobotType.GARDENER);
			if (buildPlanLen >= 2)
			{
				rc.broadcastFloat(CHANNEL_FIRST_BUILD_DIRECTION, buildPlan[1].radians);
			}
			if (buildPlanLen == 1)
			{
				rc.disintegrate();
			}
		}
	}
	
	static void planFirstBuilds() throws GameActionException
	{
		buildPlanLen = 0;
		for (int i = 0; i < 100 && buildPlanLen < 3; i++)
		{
			attemptBuildPlan();
		}
	}
	
	static final float spawnDist = 1 + GameConstants.GENERAL_SPAWN_OFFSET;
	
	static void attemptBuildPlan() throws GameActionException
	{
		MapLocation a = myLocation;
		Direction d1 = randomDirection();
		if (!rc.canBuildRobot(RobotType.GARDENER, d1))
		{
			return;
		}
		MapLocation b = a.add(d1, myRadius + spawnDist);
		if (buildPlanLen < 1)
		{
			buildPlan[0] = d1;
			buildPlanLen = 1;
		}
		Direction d2 = randomDirection();
		MapLocation c = b.add(d2, 1 + spawnDist);
		if (rc.isCircleOccupied(c, 1) || !rc.onTheMap(c, 1))
		{
			return;
		}
		if (buildPlanLen < 2)
		{
			buildPlan[0] = d1;
			buildPlan[1] = d2;
			buildPlanLen = 2;
		}
		Direction d3 = randomDirection();
		MapLocation d = b.add(d3, 1 + spawnDist);
		if (rc.isCircleOccupied(d, 1) || !rc.onTheMap(d, 1))
		{
			return;
		}
		buildPlan[0] = d1;
		buildPlan[1] = d2;
		buildPlan[2] = d3;
		buildPlanLen = 3;
	}
	
	static int computeCramp() throws GameActionException
	{
		int ret = 0;
		for (int i = 0; i < 100; i++)
		{
			MapLocation loc = randomPointWithin(myType.sensorRadius - 1.1f);
			rc.setIndicatorDot(loc, 255, 255, 255);
			if (rc.isCircleOccupied(loc, 1) || !rc.onTheMap(loc, 1))
			{
				++ret;
			}
		}
		planFirstBuilds();
		ret += 200 * (3 - buildPlanLen);
		return ret;
	}
	
	static boolean meetsInitialConditions() throws GameActionException
	{
		if (rc.getTeamBullets() < 100)
		{
			return false;
		}
		if (gardeners > 0)
		{
			return false;
		}
		if (trees > 0)
		{
			return false;
		}
		if (round - rc.readBroadcast(CHANNEL_LAST_ANYTHING_BUILD_TIME) < 5)
		{
			return false;
		}
		if (canSeeThreat)
		{
			return false;
		}
		return true;
	}

	public static void archonSpecificLogic() throws GameActionException
	{
		getMacroStats();
		archonIsSoldierNear = closestEnemyOfType(RobotType.SOLDIER) != null;
		broadcastSensingMagic();
		if (meetsInitialConditions())
		{
			round1Planning();
		}
		else
		{
			macro();
		}
		burnCycles(Clock.getBytecodesLeft() / 2 - 500);
	}
	
	static MapLocation[] prevBroadcasts;
	static int prevRound = -1;
	static void broadcastSensingMagic() throws GameActionException
	{
		MapLocation[] broadcasts = rc.senseBroadcastingRobotLocations();
		MapLocation[] ourBuilds = readPointArray(CHANNEL_OUR_BUILDS);
		if (round == prevRound + 1)
		{
			if (broadcasts.length * (prevBroadcasts.length + ourBuilds.length) < 400)
			{
				loop:
				for (MapLocation loc : broadcasts)
				{
					for (MapLocation us : ourBuilds)
					{
						if (loc.distanceTo(us) < 5)
						{
							continue loop;
						}
					}
					float d = 1e9f;
					for (MapLocation oth : prevBroadcasts)
					{
						d = Math.min(d, loc.distanceTo(oth));
					}
					if (d > 1.75f)
					{
						debug_line(myLocation, loc, 255, 0, 255);
						theyHaveGardenerAt(loc);
						break;
					}
				}
			}
		}
		prevRound = round;
		prevBroadcasts = broadcasts;
	}

	// When the type parameter is ARCHON, we build a TREE instead.
	public static boolean attemptBuild(RobotType type) throws GameActionException
	{
		if (type == RobotType.ARCHON){
			System.out.println("BUILDING A TREE " + inHex);
			if (rc.getTeamBullets() < 50 || !rc.hasTreeBuildRequirements())
			{
				return false;
			}
			if (inHex) {
				int canBuild = 0;
				for (int i = 0; i < 6; i++) {
					float theta = i * 60;
					Direction dir = new Direction(theta/57.2957795131f);
					if (rc.canPlantTree(dir))
						canBuild++;
					
				}
				System.out.println("CAN BUILD IS " + canBuild);
				for (int i = 0; i < 6; i++)
				{
					float theta = i * 60;
					if ((targetDirections & 1 << i) == 0)
						continue;
					Direction dir = new Direction(theta/57.2957795131f);
//						rc.setIndicatorDot(myLocation.add(dir, 2.1f), 200, 200, 50);
					if (rc.canPlantTree(dir) && (canBuild >= 2 || gardeners >= 2))
					{
						rc.plantTree(dir);
						increment(CHANNEL_THING_BUILD_COUNT);
						rc.broadcast(CHANNEL_LAST_ANYTHING_BUILD_TIME, round);
						writePoint(CHANNEL_RALLY_POINT, myLocation);
						inHex = true;
						return true;
					}
				}
				return onTreeBuildFail();
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
			float bestValue = 0;
			Direction bestDir = null;
			float base;
			if (rc.readBroadcast(CHANNEL_FIRST_BUILD_DIRECTION) != 0)
			{
				base = rc.readBroadcastFloat(CHANNEL_FIRST_BUILD_DIRECTION);
			}
			else
			{
				base = rand01() * 6.283185307179586476925286766559f;
			}
			for (int i = 0; i < 12; i++)
			{
				Direction dir = new Direction(base + i * 6.283185307179586476925286766559f / 12);
				if (rc.canBuildRobot(type, dir))
				{
					MapLocation loc = myLocation.add(dir, myRadius + type.bodyRadius + GameConstants.GENERAL_SPAWN_OFFSET);
					float v = evaluateBuildGoodness(loc);
					if (bestDir == null || v > bestValue)
					{
						bestValue = v;
						bestDir = dir;
					}
				}
			}
			if (bestDir == null)
			{
				return false;
			}
			else
			{
				System.out.println(bestValue);
				executeBuild(bestDir, type);
				return true;
			}
		}
	}
	
	private static float evaluateBuildGoodness(MapLocation loc) throws GameActionException
	{
		float ret = 0;
		
		if (canSeeThreat)
		{
			ret += loc.distanceTo(closestThreat);
		}
		else
		{
			ret -= loc.distanceTo(closestThreat);
		}
		
		return ret;
	}
	
	private static void executeBuild(Direction dir, RobotType type) throws GameActionException
	{
		rc.buildRobot(type, dir);
		if (type == RobotType.SCOUT)
		{
			rc.broadcast(CHANNEL_LAST_SCOUT_BUILD_TIME, round);
		}
		if (type == RobotType.GARDENER && gardeners == 0)
		{
			writePoint(CHANNEL_RALLY_POINT, myLocation);
		}
		rc.broadcast(CHANNEL_LAST_ANYTHING_BUILD_TIME, round);
		increment(CHANNEL_THING_BUILD_COUNT);
		rc.broadcast(CHANNEL_FIRST_BUILD_DIRECTION, 0);
		pushToPointArray(CHANNEL_OUR_BUILDS, myLocation, MAX_OUR_BUILD_LEN);
	}
	
	private static RobotInfo closestEnemyOfType(RobotType t)
	{
		for (RobotInfo info : nearbyEnemies)
		{
			if (info.getType() == t)
			{
				return info;
			}
		}
		return null;
	}
	
	private static RobotInfo closestFriendOfType(RobotType t)
	{
		for (RobotInfo info : nearbyFriends)
		{
			if (info.getType() == t)
			{
				return info;
			}
		}
		return null;
	}

	private static boolean onTreeBuildFail() throws GameActionException
	{
		if (neutralTrees.length >= 1)
		{
			return attemptBuild(RobotType.LUMBERJACK);
		}
		else if (gardeners == 1)
		{
			treeBuildTarget = myLocation.add(randomDirection(), 5);
		}
		return false;
	}

	static int gardeners, soldiers, trees, lumberjacks, scouts, archons, tanks;
	static void getMacroStats() throws GameActionException
	{
		gardeners = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_GARDENERS));
		soldiers = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SOLDIERS));
		lumberjacks = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_LUMBERJACKS));
		scouts = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SCOUTS));
		tanks = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_TANKS));
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

	
	// donates as much bullets it can until bullets
	static void donate (int bullets) throws GameActionException
	{
		rc.donate((int) (bullets / rc.getVictoryPointCost()) * rc.getVictoryPointCost());
	}
	
	static boolean archonIsSoldierNear;
	static boolean gardenerIsProtectedByArchon;
	
	static MapLocation findTarget()
	{
		if (closestTree != null && (isScout || isLumberjack || (isArchon && round < 50)))
		{
			return closestTree.location;
		}
		if (isArchon)
		{
			RobotInfo gardener = closestFriendOfType(RobotType.GARDENER);
			if (gardener != null)
			{
				RobotInfo soldier = null;
				float d = 0;
				for (RobotInfo info : nearbyEnemies)
				{
					if (info.type == RobotType.SOLDIER)
					{
						float td = info.location.distanceTo(gardener.location);
						if (soldier == null || td < d)
						{
							d = td;
							soldier = info;
						}
					}
				}
				if (soldier != null)
				{
					ignoreFriendRepulsion = true;
					MapLocation a = soldier.getLocation();
					MapLocation b = gardener.getLocation();
					return a.add(
							a.directionTo(b),
							soldier.type.bodyRadius + myRadius + 0.1f); 
									
				}
			}
			return null;
		}
		if (isGardener)
		{
			gardenerIsProtectedByArchon = false;
			RobotInfo soldier = closestEnemyOfType(RobotType.SOLDIER);
			RobotInfo archon = closestFriendOfType(RobotType.ARCHON);
			if (archon != null)
			{
				ignoreFriendRepulsion = true;
				MapLocation a;
				if (soldier != null)
				{
					gardenerIsProtectedByArchon = true;
					a = soldier.getLocation();
				}
				else if (rc.senseNearbyTrees(-1, myTeam).length == 0)
				{
					a = theirSpawns[0];
				}
				else
				{
					return centreOfBase;
				}
				MapLocation b = archon.getLocation();
				MapLocation c = myLocation;
				MapLocation result = b.add(
						a.directionTo(b),
						archon.type.bodyRadius + myRadius + 0.2f + archon.type.strideRadius);
				if (result.x < leftBound + 4 ||
						result.y < topBound + 4 ||
						result.x > rightBound - 4 ||
						result.y > bottomBound - 4){
					return centreOfBase;
				}
				else
				{
					return result;
				}
			}
			else
			{
				if (targetHex != null) currentTarget = targetHex;
			}
		}
		return freeRange ? currentTarget : centreOfBase;
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
			if (attemptBuild(next)){
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
		if (gardeners < trees / 4 + 1 || (rc.getTeamBullets() > 350 && gardeners < trees / 2)) // indicates some kind of blockage
		{
			if (gardeners > 0 || (rc.getTeamBullets() >= 300 && round == 1))
			{
				wantGardener = true;
			}
		}
		
		// TODO make sure this still makes sense given our new intial condition logic
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
		if (rc.readBroadcastBoolean(CHANNEL_CRAMPED) && lumberjacks < 3)
		{
			wantLumberjack = true;
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
				if (!attemptBuild(RobotType.GARDENER) && gardeners == 0)
					rc.broadcast(CHANNEL_INITIAL_ARCHON_BLOCKED, 1);
			}
		}

		if (isGardener)
		{
			if (rc.readBroadcastInt(CHANNEL_THING_BUILD_COUNT) == 2 && isRobotInNearbyTree()){
				attemptBuild(RobotType.LUMBERJACK);
			}
			if (rc.readBroadcastBoolean(CHANNEL_IS_SCOUT_USEFUL) && rc.readBroadcast(CHANNEL_LAST_SCOUT_BUILD_TIME) == 0)
			{
				attemptBuild(RobotType.SCOUT);
			}
			if (rc.getTeamBullets() >= 50 && (!wantGardener || gardeners > 5) && ((!wantSoldier && !wantLumberjack) || trees >= 5))
			{
				rc.setIndicatorDot(myLocation, 0, 255, 0);
				attemptBuild(RobotType.ARCHON); // plant a tree
			}
			if (wantLumberjack)
			{
				attemptBuild(RobotType.LUMBERJACK);
			}
			if (wantSoldier)
			{
				attemptBuild(RobotType.SOLDIER);
			}
			if (neutralTrees.length != 0)
			{
				attemptBuild(RobotType.LUMBERJACK);
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
		if (isArchon && meetsInitialConditions())
		{
			switch (round / 6 % 4)
			{
			case 0: return (long) (1000 * loc.x);
			case 1: return (long) (1000 * loc.y);
			case 2: return (long) (1000 * -loc.x);
			case 3: return (long) (1000 * -loc.y);
			}
		}
		
		long ret = 0;

//		if (beaconLen != 0)
//		{
//			ret += 1000 * loc.distanceTo(beacons[beaconLen - 1]);			
//		}
//		if (attractor != null)
//		{
//			ret += 1200 * loc.distanceTo(attractor);
//		}
		
		if (cachedTarget != null)
		{
			if (isScout && closestTree != null)
			{
				ret += 100000 * loc.distanceTo(cachedTarget);
			}
			else if (isLumberjack && closestTree != null)
			{
				debug_checkClosestTreeEqualToCachedTarget();
				ret += 100000 * Math.max(0f, loc.distanceTo(cachedTarget) - closestTree.radius - 1 - myRadius);
			}
			else if (gardenerIsProtectedByArchon)
			{
				ret += 200000 * loc.distanceTo(cachedTarget);
			}
			else if (bugMode && bugDestination != null)
			{
				ret += 10000 * loc.distanceTo(bugDestination);
			}
			else
			{
				ret += 1000 * loc.distanceTo(cachedTarget);
			}
		}
		else if (isArchon)
		{
			ret += 1000 * loc.distanceTo(closestThreat);
			if (loc.distanceTo(centreOfBase) > 6)
			{
				ret += 1500 * loc.distanceTo(centreOfBase);
			}
			if (trees == 0 && !archonIsSoldierNear)
			{
				float a = theirSpawns[0].directionTo(myOriginalLocation).radiansBetween(theirSpawns[0].directionTo(loc));
				float s = Math.abs((float) Math.sin(a));
				float d = loc.distanceTo(theirSpawns[0]) * s;
				ret -= 300000 * Math.min(d, 12);
			}
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
				switch (info.getType())
				{
				case SOLDIER:
				case LUMBERJACK:
				case TANK:
					ret -= 20 * info.getLocation().distanceTo(loc) * getIdealDistanceMultiplier(info.getType());
					break;
				default:
					;
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
					if (isSoldier && info.type == RobotType.SOLDIER)
					{
						ret += 4000 * Math.abs(d - 7);
					}
					else
					{
						if (isLumberjack)
							ideal = 0;
						d -= ideal;
						d *= d;
						ret += (long) (d * getIdealDistanceMultiplier(info.getType()));
					}
				}
			}
		}

		if (isGardener) {
			if (targetHex != null && myLocation.distanceTo(targetHex) < 0.1) {
				System.out.println("ROBOT IS IN THE HEX");
				inHex = true;
				freeRange = false;
			}
			if (targetHex != null) {
				rc.setIndicatorLine(myLocation, targetHex, 0, 255, 255);
				ret += loc.distanceTo(targetHex) * 1000000;
			}
			if (!inHex)
				ret -= loc.distanceTo(myLocation) * 2000000;
		}
		

		if (isGardener && !ignoreFriendRepulsion)
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
			for (RobotInfo ri : nearbyFriends) {
				if (ri.type == RobotType.GARDENER && ri.ID < rc.getID()) {
					float range = 10;
					float d = ri.location.distanceTo(loc) - myRadius * 2;
					ret += 1000 * (1 / (0.01f + d / range));
				}
			}
			if (count > 5)
			{
				ret -= 10000 * loc.distanceTo(centreOfBase);
			}
			else
			{
				ret += tot;
			}
		}

		if (isArchon && !ignoreFriendRepulsion)
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
		else if (!ignoreFriendRepulsion && !canBug())
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
		if (isArchon && !ignoreFriendRepulsion)
		{
			for (TreeInfo info : nearbyTrees)
			{
				float d = info.getLocation().distanceTo(loc) - myRadius - info.radius;
				if (d < REPULSION_RANGE)
				{
					ret += 1000 * (1 / (0.01f + d / REPULSION_RANGE));
				}
				if (d - 1 > REPULSION_RANGE)
				{
					break;
				}
			}    		
		}
		
		if (!isArchon)
		{
			ret += bulletDodgeWeight(loc);
		}
		
		if (isSoldier)
		{
			if (round - lastAttackRound < 5)
			{
				for (TreeInfo info : nearbyTrees)
				{
					float d = loc.distanceTo(info.location) - myRadius - info.radius;
					if (d < 3)
					{
						ret += 2000 * (3 - d);
					}
					if (d - 1 > 3)
					{
						break;
					}
				}
			}
		}
		
		return ret;
	}
	
	private static void debug_checkClosestTreeEqualToCachedTarget() {
		if (cachedTarget.distanceTo(closestTree.location) > 0.001f)
		{
			throw new RuntimeException();
		}
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
			else
			{
				b = a.add(dir, 2 * bullet.speed);
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
				if (d < myRadius - myStride)
				{
					ret += BULLET_HIT_WEIGHT;
				}
			}
		}
		return ret;
	}
	
	static MapLocation toward(MapLocation a, MapLocation b, float d)
	{
		if (a.distanceTo(b) <= d)
		{
			return b;
		}
		else
		{
			return a.add(a.directionTo(b), d);
		}
	}

	static int importantBulletIndex;

	public static void preprocessBullets() throws GameActionException
	{
		importantBulletIndex = nearbyBullets.length;
		for (int i = 0; i < importantBulletIndex; i++)
		{
			BulletInfo bullet = nearbyBullets[i];
			MapLocation a = bullet.location;
			Direction dir = bullet.dir;
			MapLocation b = a.add(dir, bullet.speed * 2);
			Direction toRobot = a.directionTo(myLocation);
			
			float angle = Math.abs(dir.radiansBetween(toRobot)); 
			float d;
			if (angle > HALFPI)
			{
				d = a.distanceTo(myLocation);
			}
			else if (Math.abs(dir.radiansBetween(b.directionTo(myLocation))) < HALFPI)
			{
				d = b.distanceTo(myLocation);
			}
			else
			{
				d = (float) (a.distanceTo(myLocation) * Math.sin(angle));
			}
			if (d > myRadius + myStride)
			{
				--importantBulletIndex;
				nearbyBullets[i] = nearbyBullets[importantBulletIndex];
				--i;
			}
		}
//		int lim = 10;
//		if (importantBulletIndex > lim)
//		{
//			for (int i = 0; i < lim; i++)
//			{
//				float best = 1e9f;
//				int idx = i;
//				for (int j = i+1; j < importantBulletIndex; j++)
//				{
//					float d = nearbyBullets[j].getLocation().distanceTo(myLocation); 
//					if (d < best)
//					{
//						best = d;
//						idx = j;
//					}
//				}
//				BulletInfo swp = nearbyBullets[i];
//				nearbyBullets[i] = nearbyBullets[idx];
//				nearbyBullets[idx] = swp;
//			}
//			importantBulletIndex = lim;
//		}
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
			return 2.1f;
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
//		rc.setIndicatorLine(myLocation, beacons[beaconLen], 0, 255, 0);
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
		MapLocation them = cachedTarget;
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
			beacons[0] = cachedTarget;
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
		if (cachedTarget == null)
		{
			return;
		}
		ignoreFriendRepulsion = ignoreFriendRepulsion || beaconLen > 1;
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
			if (theirBaseFound() && readConsumerBFSDistance(myX, myY) < INFINITY && cachedTarget.distanceTo(theirBase) < 4)
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
				beacons[0] = cachedTarget;
			}
			beaconLen = 1;
		}
//		rc.setIndicatorLine(myLocation, beacons[beaconLen - 1], 255, 127, 0);
	}
	
	static boolean goingTowardMe(BulletInfo info)
	{
		return Math.abs(info.dir.degreesBetween(info.location.directionTo(myLocation))) < 75;
	}
	
	static MapLocation findMidpoint()
	{
		BulletInfo a = null, b = null;
		for (BulletInfo info : nearbyBullets)
		{
			if (goingTowardMe(info))
			{
				if (a == null)
				{
					a = info;
				}
				else
				{
					b = info;
					break;
				}
			}
		}
		if (b == null)
		{
			return null;
		}
		MapLocation q = a.location.add(a.dir, a.location.distanceTo(myLocation));
		MapLocation r = b.location.add(b.dir, b.location.distanceTo(myLocation));
		return new MapLocation((q.x + r.x) / 2, (q.y + r.y) / 2);
	}
	

	// Bug Algorithm
	static boolean bugMode = false;
	static MapLocation bugBeginLocation;
	static MapLocation bugDestination;
	static MapLocation savedDestination;
	static MapLocation bodyCentre;
	static float bodyRadius;
	static MapLocation myBugLocation;
	static float bugTurnDir;
	static Direction justGoThisWay = null;
	
	static void snap(MapLocation loc)
	{
		float bestD = 1e9f;
		for (TreeInfo info : rc.senseNearbyTrees(loc, myRadius + 1, null))
		{
			float d = loc.distanceTo(info.location) - info.getRadius() - myRadius;
			if (d < bestD)
			{
				bodyCentre = info.location;
				bodyRadius = info.getRadius();
				bestD = d;
				justGoThisWay = null;
			}
		}
		for (RobotInfo info : rc.senseNearbyRobots(loc, myRadius + 1, null))
		{
			float d = loc.distanceTo(info.location) - info.getRadius() - myRadius;
			if (d < bestD)
			{
				bodyCentre = info.location;
				bodyRadius = info.getRadius();
				bestD = d;
				justGoThisWay = null;
			}
		}
//		TreeInfo[] edgeTrees = new TreeInfo[] {
//				new TreeInfo(-1, null, new MapLocation(leftBound, (int) loc.y), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation(leftBound, (int) loc.y + 1), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation(rightBound, (int) loc.y), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation(rightBound, (int) loc.y + 1), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation((int) loc.x, topBound), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation((int) loc.x + 1, topBound), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation((int) loc.x, bottomBound), 0.5f, 0, 0, null),
//				new TreeInfo(-1, null, new MapLocation((int) loc.x + 1, bottomBound), 0.5f, 0, 0, null)
//		};
//		for (TreeInfo info : edgeTrees)
//		{
//			float d = loc.distanceTo(info.location) - info.getRadius();
//			if (d < bestD)
//			{
//				bodyCentre = info.location;
//				bodyRadius = info.getRadius();
//				bestD = d;
//			}
//		}
		System.out.println("d = " + bestD);
		float far = 1;
		float margin = 0;
		float bugTurnDir;
		float topDist = Math.max(0, loc.y - myRadius - (topBound + margin));
		float leftDist = Math.max(0, loc.x - myRadius - (leftBound + margin));
		float rightDist = Math.max(0, (rightBound - margin) - loc.x - myRadius);
		float bottomDist = Math.max(0, (bottomBound - margin) - loc.y - myRadius);
		if (topDist < bestD)
		{
			bestD = topDist;
			bodyCentre = new MapLocation(loc.x, (topBound + margin) - far);
			bodyRadius = far;
			justGoThisWay = Direction.EAST;
		}
		if (leftDist < bestD)
		{
			bestD = leftDist;
			bodyCentre = new MapLocation((leftBound + margin) - far, loc.y);
			bodyRadius = far;
			justGoThisWay = Direction.SOUTH;
		}
		if (bottomDist < bestD)
		{
			bestD = bottomDist;
			bodyCentre = new MapLocation(loc.x, (bottomBound - margin) + far);
			bodyRadius = far;
			justGoThisWay = Direction.WEST;
		}
		if (rightDist < bestD)
		{
			bestD = rightDist;
			bodyCentre = new MapLocation((rightBound - margin) + far, loc.y);
			bodyRadius = far;
			justGoThisWay = Direction.NORTH;
		}
		System.out.println("d = " + bestD);
	}
	
	static MapLocation advanceBy(float stride)
	{
		if (justGoThisWay != null)
		{
			return myBugLocation.add(justGoThisWay, bugTurnDir * stride);
		}
		else
		{
			float r = bodyRadius + myRadius + 0.001f;
			return bodyCentre.add(bodyCentre.directionTo(myBugLocation).rotateRightRads(bugTurnDir * stride / r), r);
		}
	}
	
	static final float PI = 3.1415926535897932384626433832795f;
	static final float TAU = 6.283185307179586476925286766559f;
	
	static void bugAlgorithm() throws GameActionException {
		if (bugMode){
			if (cachedTarget != null && savedDestination.distanceTo(cachedTarget) > 4)
			{
				System.out.println("Destination changed");
				bugMode = false;
			}
			MapLocation cen = toward(myLocation, bodyCentre, myType.sensorRadius - 0.1f);
			if (justGoThisWay != null)
			{
				; // edge of map
			}
//			else if (!rc.canSenseLocation(cen))
//			{
//				System.out.println("Can't sense");
//				bugMode = false;
//			}
			else if (!rc.isLocationOccupied(cen))
			{
				System.out.println("Vacant");
				bugMode = false;
			}
		}
		if (bugMode)
		{
			rc.setIndicatorLine(myLocation, bodyCentre, 255, 0, 255);
			followWall();
		}
		if (!bugMode && cachedTarget != null)
		{
			MapLocation wantMoveTo = myLocation.add(myLocation.directionTo(cachedTarget), 0.125f);
			if (!rc.canMove(wantMoveTo))
			{
				System.out.println("ENTERING BUG MODE:");
				bugMode = true;
				bugBeginLocation = myLocation;
				savedDestination = cachedTarget;
				snap(myLocation);
				myBugLocation = myLocation;
				bugTurnDir = 1;
//				MapLocation a = advanceBy(0.1f);
//				MapLocation b = advanceBy(-0.1f);
//				if (a.distanceTo(cachedTarget) < b.distanceTo(cachedTarget))
//				{
//					bugTurnDir = 1;
//				}
//				else
//				{
//					bugTurnDir = -1;
//				}
				bugDestination = advanceBy(0);
				resetHistory(); 
			}
		}
		if (!bugMode)
		{
			bugDestination = null;
		}
		else
		{
			// debug point
			rc.setIndicatorDot(bugDestination, 0, 255, 255);
			rc.setIndicatorLine(myLocation, bodyCentre, 0, 0, 255);
			rc.setIndicatorLine(myLocation, bugDestination, 255, 127, 0);
			
			System.out.println("Current spot = " + myLocation);
			System.out.println("Final spot = " + bugDestination.x + " " + bugDestination.y);
			System.out.println("d = " + myLocation.distanceTo(bugDestination));
		}
	}
	
	static void followWall(){
		
		// Didn't follow wall properly last turn
		if (bugDestination != null && !myLocation.equals(bugDestination))
		{
			bugMode = false;
			System.out.println("BROKE OUT CUZ NOT SAME BUGDEST");
			rc.setIndicatorLine(myLocation, theirSpawns[0], 255, 127, 0);
			return;
		}
		
		float stride = RobotPlayer.myStride - 0.01f;

		myBugLocation = myLocation;
		MapLocation ideal = advanceBy(stride);
		if (rc.canMove(ideal))
		{
			bugDestination = ideal;
		}
		else
		{
			final int LIM = 1024;
			int lo = 0;
			int hi = LIM - 1;
			while (lo != hi)
			{
				int mid = (lo + hi + 1) / 2;
				float amount = stride * mid / LIM;
				if (rc.canMove(advanceBy(amount)))
				{
					lo = mid;
				}
				else
				{
					hi = mid - 1;
				}
			}
			float a = stride * lo / LIM;
			float b = stride * (lo + 1) / LIM;
			myBugLocation = bugDestination = advanceBy(a);
			snap(advanceBy(b));
			System.out.println("Binary search yields " + lo + " [" + a + "]");
		}
		
		if (bugDestination.distanceTo(cachedTarget) > myLocation.distanceTo(cachedTarget) && myLocation.distanceTo(cachedTarget) < bugBeginLocation.distanceTo(cachedTarget) - 1)
		{
			System.out.println("BugMode broken by worse bugDestination");
			bugMode = false;
		}
	}

	static MapLocation opti;
	static MapLocation cachedTarget;
	
	static float[] preprocessDirections = {
		0.0000000000f,
		3.1415926536f,
		1.5707963268f,
		4.7123889804f,
		0.7853981634f,
		3.9269908170f,
		2.3561944902f,
		5.4977871438f,
		0.3926990817f,
		3.5342917353f,
		1.9634954085f,
		5.1050880621f,
		1.1780972451f,
		4.3196898987f,
		2.7488935719f,
		5.8904862255f
	};
	
	static boolean canBug()
	{
		return freeRange && !isScout;
	}

	public static void selectOptimalMove() throws GameActionException
	{
		System.out.println(Clock.getBytecodesLeft() + " left");
		ignoreFriendRepulsion = false;
		cachedTarget = findTarget();
		if (cachedTarget != null)
		{
			rc.setIndicatorLine(myLocation, cachedTarget, 255, 255, 255);			
		}
		debug_johnMadden();
		preprocessBullets();
//		findBeacon();
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
		int iterlim = 50;
		if (isScout && nearbyBullets.length == 0 && freeRange && nearbyEnemies.length == 0)
		{
			iterlim = 5;
		}
		else if (isGardener || isArchon)
		{
			iterlim = 16;
		}
		if (canBug())
		{
			bugAlgorithm();
			if (bugMode && bugDestination != null)
			{
				best = bugDestination;
				bestVal = badness(best);
			}
		}
		if (best == null && cachedTarget != null)
		{
			MapLocation cand = toward(myLocation, cachedTarget, myStride);
			if (rc.canMove(cand))
			{
				best = cand;
				bestVal = badness(best);				
			}
		}
		System.out.println(Clock.getBytecodesLeft() + " left");
		float base = rand01() * 6.283185307179586476925286766559f;
		while (Clock.getBytecodesLeft() - longest > after && iterations < iterlim)
		{
			int t1 = Clock.getBytecodesLeft();
			float add;
			MapLocation cand;
			if (longest < 500 || nearbyBullets.length == 0)
			{
				add = myStride * rand01();
			}
			else
			{
				add = myStride;
			}
			if (iterations < preprocessDirections.length)
			{
				cand = myLocation.add(new Direction(base + preprocessDirections[iterations]), add);
			}
			else
			{
				cand = myLocation.add(randomDirection(), add);
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
		if (best != null)
			opti = best;
		else
			opti = myLocation;
		debug_printAfterMovementLoop(iterations, longest);
	}
	
	static void debug_printAfterMovementLoop(int iterations, int longest) throws GameActionException
	{
		long x = badness(opti);
		System.out.println(
				iterations + " iterations; the longest one cost " +
				longest + "; " + 
				nearbyBullets.length + "/" + importantBulletIndex + "; " +
				x + "; ignore = " +
				ignoreFriendRepulsion);
		System.out.println(Clock.getBytecodesLeft() + " left");
	}

	public static void onRoundEnd() throws GameActionException
	{
		if (isGardener)
		{
			int dist = (int) ((myLocation.distanceTo(centreOfBase) + 7) * 1000);
			if (dist > rc.readBroadcast(CHANNEL_CONTROL_RADIUS))
			{
				rc.broadcast(CHANNEL_CONTROL_RADIUS, dist);
			}
		}
		if (rc.hasAttacked())
		{
			lastAttackRound = round;
		}
		if (isLeader)
		{
			rc.broadcast(CHANNEL_LEADER_ROUND_TIMESTAMP, round);
		}
		burnCycles(Clock.getBytecodesLeft() - 500);
	}
	
	static void debug_resignOver1000()
	{
		if (round > 1000)
		{
			rc.resign();
		}
	}

	public static void onRoundBegin() throws GameActionException
	{
//		debug_resignOver1000();
		debug_highlightHexes();
		roam = false;
		theirBase = readPoint(CHANNEL_THEIR_BASE);
		nearbyFriends = rc.senseNearbyRobots(100, myTeam);
		nearbyEnemies = rc.senseNearbyRobots(100, myTeam.opponent());
		nearbyBullets = rc.senseNearbyBullets(myRadius + myStride + 4);
		isLeader = rc.readBroadcast(CHANNEL_LEADER_ID) == rc.getID();
		offsetHexX = rc.readBroadcastFloat(CHANNEL_HEX_OFFSET_X);
		offsetHexY = rc.readBroadcastFloat(CHANNEL_HEX_OFFSET_Y);
		if (round - rc.readBroadcast(CHANNEL_LEADER_ROUND_TIMESTAMP) > 15)
		{
			becomeLeader();
		}
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

		float d = myLocation.distanceTo(centreOfBase);
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
			for (int i = 0; i < 32; i++)
				rc.broadcast(CHANNEL_HEX_LOCATIONS + i, 0);
			if (theirBaseFound())
			{
				rc.setIndicatorDot(theirBase, 127, 255, 255);
			}
		}

		if (isGardener && inHex) {
			findHex(myLocation);
			freeRange = false;
			int tx = retHelper1;
			int ty = retHelper2;
			int bit = (1 << ty);
			writeHexPoint(CHANNEL_HEX_LOCATIONS + tx	% HEX_TORUS_SIZE, bit);
		}

		int myWrite = writeNumberChannel(numberOfChannel);
		rc.broadcast(myWrite, rc.readBroadcast(myWrite) + 1);

		if (isGardener)
		{
			if (!inHex) {
				System.out.println("TRYING TO FIND TARGET HEX");
				findHex(myLocation);
				findClosestHex(retHelper1, retHelper2);
				if (targetHex != null) {
					System.out.println("HEX FOUND");
					rc.setIndicatorLine(myLocation, targetHex, 0, 255, 255);
				}
			}
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
			if (nearbyTrees.length > 40)
			{
				TreeInfo[] arr = new TreeInfo[40];
				System.arraycopy(nearbyTrees, 0, arr, 0, 40);
				nearbyTrees = arr;
			}
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
		if (rc.getTeamBullets() >= 2500) {
			donate((int)(rc.getTeamBullets() - 2500));
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
		if (isLeader)
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
		loop:
		for (RobotInfo info : nearbyEnemies)
		{
			switch (info.getType())
			{
			case GARDENER:
				theyHaveGardenerAt(info.location);
				break loop;
			case SOLDIER:
			case LUMBERJACK:
				if (rc.readBroadcastBoolean(CHANNEL_EVER_FOUND_GARDENER) && !theirBaseFound())
				{
					writePoint(CHANNEL_THEIR_BASE, info.getLocation());
					rc.broadcastBoolean(CHANNEL_THEIRBASE_IS_GARDENER, false);
				}
				break; // switch
			default:
				;
			}
		}
		if (myLocation.distanceTo(theirBase) < 3 && !canSeeValuableTargets())
		{
			rc.broadcastInt(CHANNEL_THEIR_BASE, 0);
		}
		archons = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_ARCHONS));
		if (round > 400 && archons == 0 && rc.getTeamVictoryPoints() <= rc.getOpponentVictoryPoints() + 2 && rc.getTreeCount() == 0)
		{
			donate();
		}
		closestThreat = null;
		canSeeThreat =  false;
		loop:
		for (RobotInfo info : nearbyEnemies)
		{
			switch (info.getType())
			{
			case SOLDIER:
			case LUMBERJACK:
			case TANK:
				closestThreat = info.location;
				canSeeThreat = true;
				break loop;
			default: ;
			}
		}
		if (closestThreat == null)
		{
			closestThreat = theirSpawns[0];
		}
		if (!isArchon && !isGardener && nearbyEnemies.length == 0 && nearbyBullets.length == 0)
		{
			burnCycles(Clock.getBytecodesLeft() / 4);
		}
	}
	
	private static void theyHaveGardenerAt(MapLocation loc) throws GameActionException
	{
		writePoint(CHANNEL_THEIR_BASE, loc);
		rc.broadcastBoolean(CHANNEL_EVER_FOUND_GARDENER, true);
		rc.broadcastBoolean(CHANNEL_THEIRBASE_IS_GARDENER, true);
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
		float r;
		if (bugMode)
		{
			r = myRadius + 0.001f;
		}
		else
		{
			r = myType.sensorRadius - 0.1f;
		}
		float eps = 0.001f;
		float lx = Math.max(leftBound + eps, myLocation.x - r);
		float rx = Math.min(rightBound - eps, myLocation.x + r);
		float ty = Math.max(topBound + eps, myLocation.y - r);
		float by = Math.min(bottomBound - eps, myLocation.y + r);
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
	
	static boolean checkBugBlocked()
	{
		for (int i = 0; i < history.length; i++)
		{
			if (history[i] == null)
			{
				return false;
			}
			if (history[i].distanceTo(myLocation) > 2 * (myRadius + bodyRadius + 0.2f))
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

	static MapLocation hexToCartesian(int x, int y)
	{
		return new MapLocation(
				hexSize * (x + y % 2 * 0.5f),
				y * rowSpacing);
	}

	static void findHex(MapLocation loc)
	{
		retHelper2 = (int) ((loc.y) / rowSpacing + 0.5f);
		retHelper1 = (int) (((loc.x) / hexSize - retHelper2 % 2 * 0.5f) + 0.5f);
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
						if (rc.canSenseAllOfCircle(loc, 1))
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
						if (rc.canSenseAllOfCircle(loc, 1))
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
						if (rc.canSenseAllOfCircle(loc, 1))
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
						if (rc.canSenseAllOfCircle(loc, 1))
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


	static boolean checkLeft, checkRight, checkTop, checkBottom;
	
	static MapLocation shiftInBounds (int x, int y, MapLocation initial, int k) {
		MapLocation curr = hexToCartesian(x, y);
		while (curr.x < initial.x - 2.5 * k * hexSize) {
			x += 32;
			curr = hexToCartesian(x, y);
		}
		while (curr.y < initial.y - 2.5 * k * hexSize) {
			y += 32;
			curr = hexToCartesian(x, y);
		}
		rc.setIndicatorDot(curr, 255, 0, 0);
//		rc.setIndicatorLine(new MapLocation(leftBound, 0), new MapLocation(leftBound, 1 << 30), 255, 0, 0);
//		rc.setIndicatorLine(new MapLocation(rightBound, 0), new MapLocation(rightBound, 1 << 30), 255, 0, 0);
//		rc.setIndicatorLine(new MapLocation(0, topBound), new MapLocation(1 << 30, topBound), 255, 0, 0);
//		rc.setIndicatorLine(new MapLocation(0, bottomBound), new MapLocation(1 << 30, bottomBound), 255, 0, 0);
		if (round > 50) {
			boolean valid = true;
			if (leftBound != 100000 && curr.x < leftBound + 1.1) {
				checkLeft = false;
				valid = false;
			}
			if (topBound != 100000 && curr.y < topBound + 1.1) {
				checkTop = false;
				valid = false;
			}
			if (rightBound != -100000 && curr.x > rightBound - 1.1) {
				checkRight = false;
				valid = false;
			}
			if (bottomBound != -100000 && curr.y > bottomBound - 1.1) {
				checkBottom = false;
				valid = false;
			}
			if (!valid)
				return null;
		}
		
		return curr;
	}
	
	static int mod (int val, int mod) {
		return (val % mod + mod) % mod;
	}
	
	static void findClosestHex (int x, int y) throws GameActionException {
		targetHex = null;
		int[] channels = new int[32];
		for (int i = 0; i < 32; i++)
			channels[i] = rc.readBroadcast(CHANNEL_HEX_LOCATIONS + i);
		checkLeft = true;
		checkRight = true;
		checkTop = true;
		checkBottom = true;
		main : for (int k = 0; k <= 10; k++) {
			System.out.println("CHECKING " + k);
			// checking top and bottom border
			for (int i = - k; i <= k; i++) {
				int curr = Clock.getBytecodeNum();
				int nx = mod(x + k, 32);
				int ny = mod(y + i, 32);
				if (checkTop && (channels[nx] & 1 << ny) == 0) {
					targetHex = shiftInBounds(nx, ny, myLocation, k + 1);
					if (targetHex != null)
						break main;
				}
				nx = mod(x - k, 32);
				if (checkBottom && (channels[nx] & 1 << ny) == 0) {
					targetHex = shiftInBounds(nx, ny, myLocation, k + 1);
					if (targetHex != null)
						break main;
				}

				nx = mod(x + i, 32);
				ny = mod(y + k, 32);
				if (checkRight && (channels[nx] & 1 << ny) == 0) {
					targetHex = shiftInBounds(nx, ny, myLocation, k + 1);
					if (targetHex != null)
						break main;
				}
				ny = mod(y - k, 32);

				if (checkLeft && (channels[nx] & 1 << ny) == 0) {
					targetHex = shiftInBounds(nx, ny, myLocation, k + 1);
					if (targetHex != null)
						break main;
				}
				System.out.println(Clock.getBytecodeNum() - curr + " " + checkLeft + " " + checkRight + " " + checkTop + " " + checkBottom);
			}
		}
		System.out.println("FINISHED CHECKING FOUND " + targetHex);
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

	 static Direction randomDirection() {
		return new Direction(rand() / 57.295779513082320876798154814105f);
	}

	static Direction randomDirection(int deg) {
		return new Direction((float) ((rand()/deg*deg) / 57.295779513082320876798154814105f));
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
	
	public static void pushToPointArray(int channel, MapLocation val, int maxLen) throws GameActionException
	{
		int v = rc.readBroadcastInt(channel);
		if (v == maxLen)
		{
			return;
		}
		rc.broadcastInt(channel, v + 1);
		writePoint(channel + 1 + v * 2, val);
	}
	
	public static MapLocation[] readPointArray(int channel) throws GameActionException
	{
		int len = rc.readBroadcastInt(channel);
		MapLocation[] ret = new MapLocation[len];
		for (int i = 0; i < len; i++)
		{
			ret[i] = readPoint(channel + 1 + 2 * i);
		}
		return ret;
	}

	public static void writeHexPoint(int rowchannel, int bitstring) throws GameActionException
	{
		rc.broadcast(rowchannel, rc.readBroadcast(rowchannel) | bitstring);
	}
}
