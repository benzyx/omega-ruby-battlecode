package examplefuncsplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
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
	public static final int RALLY_POINT = 10;
	public static final int CHANNEL_CONTROL_RADIUS = 12;
	public static final int CHANNEL_CALL_FOR_HELP = 13;
	public static final int CHANNEL_CALL_FOR_HELP_ROUND = 15;
	public static final int CHANNEL_HAPPY_PLACE = 16;
	public static final int CHANNEL_GARDENER_LOCATIONS = 200;
	public static final int CHANNEL_ATTACK = 999;
	public static final int CHANNEL_FIRST_SOLDIER_BLOCKED = 998;

	public static final float REPULSION_RANGE = 1.7f;

	static int topBound, leftBound, bottomBound, rightBound;
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
	static int lastGardenerHitRound;
	static boolean friendlyFireSpot;
	static boolean hasBeenThreatened;

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

		myID = rc.readBroadcast(myNumberOfChannel());
		rc.broadcast(myNumberOfChannel(), myID + 1);

		if (isSoldier && myID % 4 != 1)
		{
			freeRange = true;
		}
		if (isLumberjack && myID > 10 && myID % 2 == 1)
		{
			freeRange = true;
		}
		if (isScout)
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
				writePoint(RALLY_POINT, rally);
				writePoint(CHANNEL_HAPPY_PLACE, them);
				rc.broadcast(CHANNEL_MAP_TOP, 100000);
				rc.broadcast(CHANNEL_MAP_LEFT, 100000);
				rc.broadcast(CHANNEL_MAP_RIGHT, -100000);
				rc.broadcast(CHANNEL_MAP_BOTTOM, -100000);
			}
			else
			{
				//        		freeRange = true; // suicide mission
			}
		}
		destination = readPoint(RALLY_POINT);
		while (true)
		{
			try {
				round = rc.getRoundNum();

				onRoundBegin();
				destination = readPoint(RALLY_POINT);
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
				}

				if (freeRange)
				{
					if (retargetCount < theirSpawns.length)
					{
						currentTarget = theirSpawns[retargetCount];
						if (myLocation.distanceTo(currentTarget) < 4)
						{
							boolean anyGardener = false;
							for (RobotInfo info : nearbyEnemies)
							{
								if (info.getType() == RobotType.GARDENER)
								{
									anyGardener = true;
									break;
								}
							}
							if (!anyGardener)
							{
								++retargetCount;
								if (isScout)
								{
									retargetCount %= theirSpawns.length;
								}
							}
						}
					}
					else if (round % 40 == 0)
					{
						currentTarget = myLocation.add(randomDirection(), 100);
					}
				}
				else
				{
					if (currentTarget == null || round % 30 == 0)
					{
						currentTarget = myLocation.add(randomDirection(), 100);
					}        			
				}

				if (freeRange)
				{
					debug_line(myLocation, currentTarget, 100, 0, 100);
				}

				if (isScout){

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

					RobotInfo[] a = rc.senseNearbyRobots(3, myTeam);
					RobotInfo[] b = rc.senseNearbyRobots(3, myTeam.opponent());
					boolean wantStrike = a.length < b.length;
					for (RobotInfo info : b)
					{
						if (info.getType() == RobotType.SCOUT)
						{
							wantStrike = true;
						}
					}
					if (wantStrike)
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
						}
					}
				}
				else if (isSoldier || isTank || isScout)
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
						case TANK:
							req = 100;
						default:
							req = 6;
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
						System.out.println(onBegin - Clock.getBytecodesLeft());

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
		if (rc.canFirePentadShot() && enemyDistance < 4.2)
		{
			rc.firePentadShot(dir);
		}
		else if (rc.canFireTriadShot() && enemyDistance < 4.8)
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

	public static void archonSpecificLogic() throws GameActionException
	{
		getMacroStats();
		macro();
	}

	// When the type parameter is ARCHON, we build a TREE instead.
	public static boolean attemptBuild(int iter, RobotType type) throws GameActionException
	{
		System.out.println("Attempt build " + type);
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
				MapLocation spot = best.add(best.directionTo(myLocation), GameConstants.BULLET_TREE_RADIUS + myRadius);
				if (spot.distanceTo(myLocation) < myStride && rc.canMove(spot) && !rc.hasMoved())
				{
					rc.move(spot);
					myLocation = spot;
					Direction dir = myLocation.directionTo(best);
					if (rc.canPlantTree(dir))
					{
						rc.plantTree(dir);
						return true;
					}
					else
					{
						return false;
					}
				}
			}
			else if (neutralTrees.length >= 1 && (lumberjacks == 0 || soldiers > 0))
			{
				attemptBuild(10, RobotType.LUMBERJACK);
			}
			else if (soldiers == 0)
			{
				attemptBuild(10, RobotType.SOLDIER);
			}
			else if (gardeners == 1)
			{
				treeBuildTarget = myLocation.add(randomDirection(), 5);
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
				Direction dir = randomDirection();
				if (rc.canBuildRobot(type, dir)){
					rc.buildRobot(type, dir);
					return true;
				}
			}
		}

		return false;
	}

	static int gardeners, soldiers, trees, lumberjacks, scouts;
	static void getMacroStats() throws GameActionException
	{
		gardeners = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_GARDENERS));
		soldiers = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SOLDIERS));
		lumberjacks = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_LUMBERJACKS));
		scouts = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SCOUTS));
		trees = rc.getTreeCount();
		if (rc.readBroadcast(CHANNEL_FIRST_SOLDIER_BLOCKED) != 0)
		{
			soldiers += 2;
		}
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

	// What to build after our build order is done
	public static void macro() throws GameActionException
	{
		System.out.println(gardeners + "/" + soldiers + "/" + trees);

		boolean wantGardener = false;
		if (gardeners < trees / 5 + 1 || (rc.getTeamBullets() > 350 && gardeners < trees / 2)) // indicates some kind of blockage
		{
			if (gardeners > 0 || myID == 0)
			{
				wantGardener = true;
			}
		}
		if (getBuildOrderNext(rc.readBroadcast(CHANNEL_BUILD_INDEX)) == null &&
				gardeners <= 2 && rc.getTeamBullets() > 125 && gardeners + 1 < trees)
		{
			wantGardener = true;
		}

		if (isArchon)
		{
			if (wantGardener)
			{
				rc.setIndicatorDot(myLocation, 0, 0, 0);
				attemptBuild(10, RobotType.GARDENER);
			}
		}

		if (isGardener)
		{
			if (soldiers >= 2 && rc.getTeamBullets() >= 50 && (!wantGardener || gardeners > 5))
			{
				rc.setIndicatorDot(myLocation, 0, 255, 0);
				attemptBuild(10, RobotType.ARCHON); // plant a tree
			}
			if (scouts <= 1 || (trees >= 5 && scouts <= 3))
			{
				attemptBuild(10, RobotType.SCOUT);
			}
			if (aggro && rc.getTeamBullets() > 300)
			{
				if (rand() < 50)
				{
					attemptBuild(10, RobotType.LUMBERJACK);
				}
				else
				{
					attemptBuild(10, RobotType.SOLDIER);
				}
			}
			//    		if (neutralTrees.length >= 10 && lumberjacks < trees + 3)
			//    		{
			//    			attemptBuild(10, RobotType.LUMBERJACK);
			//    		}
			//    		if (lumberjacks < 2 && rc.getTeamBullets() > 150)
			//    		{
			//    			attemptBuild(10, RobotType.LUMBERJACK);
			//    		}
			if (soldiers < trees / 2 || soldiers < 2)
			{
				attemptBuild(10, RobotType.SOLDIER);
			}
		}

		if (trees >= 15)
		{
			//    		rc.broadcast(CHANNEL_ATTACK, 1); // kill 'em!
		}
	}

	// Determines the next object to build in the build order. 
	// !!!! If the return object is Archon, build a Tree. !!!!
	public static RobotType getBuildOrderNext(int index){
		RobotType[] buildOrder = 
			{
				RobotType.SCOUT,
				RobotType.ARCHON,
				RobotType.SOLDIER,
				RobotType.SCOUT,
				RobotType.ARCHON,
				null
			};
		return buildOrder[index];
	}

	public static long badness(MapLocation loc)
	{
		long ret = 0;

		// Scout code: Look for trees and shake 'em
		if (isScout || isLumberjack)
		{
			if (closestTree != null && (isScout || !freeRange)) {
				ret += closestTree.getLocation().distanceTo(loc) * 10000000;
			}
		}

		if (isSoldier || isLumberjack)
		{
			if (!bruteDefence)
			{
				float d = loc.distanceTo(destination);
				if (freeRange)
				{
					ret += 1000 * loc.distanceTo(currentTarget);
				}
				else
				{
					d -= controlRadius;
					if (d < -5)
					{
						ret += 3000 * loc.distanceTo(happyPlace);	    				
					}
					else
					{
						d *= d;
						ret += 1000 * d;
					}
				}
				// soldier could be kited by scout when gardener sees soldier but the soldier does not
				// see the scout.
				if (isSoldier && round - helpRound <= 1)
				{
					ret += 400000 * loc.distanceTo(helpLocation);
				}
			}
		}
		else if (isScout)
		{
			ret += 1000 * loc.distanceTo(currentTarget);
		}
		else if (isArchon && round < 40)
		{
			ret += 1000 * loc.distanceTo(destination);
		}
		else
		{
			ret += 1000 * loc.distanceTo(destination);
		}
		if (friendlyFireSpot){
			ret -= 3000 * loc.distanceTo(myLocation);
		}
		if (bruteDefence)
		{
			ret += 500 * loc.distanceTo(myLocation);
		}
		else
		{
			//    		int i = (round + repellers.length - 1) % repellers.length;
			//			if (repelWeight[i] > 0)
			//			{
			//				float d = loc.distanceTo(repellers[i]);
			//				ret -= Math.sqrt(d) * repelWeight[i];
			//			}
		}

		//    	if (!threatened)
		//    	{
		//    		if (leftBound < loc.x) ret -= 1000 * 500 * Math.min(5f, loc.x - leftBound);
		//    		if (topBound < loc.y) ret -= 1000 * 500 * Math.min(5f, loc.y - topBound);
		//    		if (rightBound > loc.x) ret -= 1000 * 500 * Math.min(5f, rightBound - loc.x);
		//    		if (bottomBound > loc.y) ret -= 1000 * 500 * Math.min(5f, bottomBound - loc.y);
		//    	}

		if (isScout)
		{
			if (reflection == null)
			{
				for (RobotInfo info : nearbyEnemies)
				{
					float d = loc.distanceTo(info.getLocation());
					switch (info.getType())
					{
					case GARDENER:
						ret += 1000 * d * d;
						break;
					case SOLDIER:
					case TANK:
					case LUMBERJACK:
					case SCOUT:
						if (round - lastGardenerHitRound < 80)
						{
							ret -= 50000 * Math.sqrt(d);    						
						}
					default:
					}
				}
			}
			for (RobotInfo info : nearbyFriends)
			{
				if (info.getType() == RobotType.SCOUT)
				{
					ret -= 1500 * loc.distanceTo(info.getLocation());
				}
			}
		}
		else if (!isGardener && !isArchon)
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
				if (isScout)
					ideal = 6;
				d -= ideal;
				d *= d;
				ret += (long) (d * getIdealDistanceMultiplier(info.getType()));
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

		if (!isGardener)
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
				ret += 15000 * Math.abs(loc.distanceTo(treeBuildTarget) - 2);
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

		if (reflection != null)
		{
			ret += 20000 * loc.distanceTo(reflection);
		}
		else if (!bruteDefence && !isArchon)
		{
			for (int i = 0; i < importantBulletIndex; i++)
			{
				BulletInfo bullet = nearbyBullets[i];
				// Get relevant bullet information
				Direction propagationDirection = bullet.dir;
				MapLocation bulletLocation = bullet.location;

				// Calculate bullet relations to this robot
				Direction directionToRobot = bulletLocation.directionTo(loc);
				float distToRobot = bulletLocation.distanceTo(loc);

				if (distToRobot < myRadius)
				{
					ret += 200000000;
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
						if (perpendicularDist < myRadius)
						{
							float alongDist = (float)Math.abs(distToRobot * Math.cos(theta)); // soh cah toa :)
							int roundsToHit = (int) (alongDist / bullet.speed);
							if (roundsToHit == 0)
							{
								ret += 200000000;
							}
							else
							{
								ret += 200000000 / distToRobot;
							}
						}
					}
				}
			}
		}

		return ret;
	}

	static int importantBulletIndex;

	public static void preprocessBullets()
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
		int lim = 5;
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
	}

	public static float getIdealDistanceMultiplier(RobotType t)
	{
		switch (t) {
		case LUMBERJACK:
			return 2000;
		case GARDENER:
			return 3000;
		case ARCHON:
			return theirSpawns.length == 1 ? 1500 : -100;
		case SOLDIER:
		case TANK:
			return 2500;
		case SCOUT:
			return freeRange ? 0 : 500;
		default:
			return 0;
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

	static MapLocation opti;

	public static void selectOptimalMove() throws GameActionException
	{
		debug_johnMadden();
		preprocessBullets();
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
		while (Clock.getBytecodesLeft() - longest > after && iterations < 100)
		{
			int t1 = Clock.getBytecodesLeft();
			float add;
			if (longest > 600 && nearbyBullets.length >= 5)
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
			if (rc.canMove(cand))
			{
				long b = badness(cand);
				if (best == null || b < bestVal)
				{
					best = cand;
					bestVal = b;
				}
			}
			else if (!rc.onTheMap(cand, myRadius))
			{
				if (cand.x < leftBound && !rc.onTheMap(new MapLocation(myLocation.x - myStride, myLocation.y), myRadius))
				{
					leftBound = (int) cand.x;
					rc.broadcast(CHANNEL_MAP_LEFT, leftBound);
				}
				if (cand.x > rightBound && !rc.onTheMap(new MapLocation(myLocation.x + myStride, myLocation.y), myRadius))
				{
					rightBound = (int) cand.x + 1;
					rc.broadcast(CHANNEL_MAP_RIGHT, rightBound);
				}
				if (cand.y < topBound && !rc.onTheMap(new MapLocation(myLocation.x, myLocation.y - myStride), myRadius))
				{
					topBound = (int) cand.y;
					rc.broadcast(CHANNEL_MAP_TOP, topBound);
				}
				if (cand.y > bottomBound && !rc.onTheMap(new MapLocation(myLocation.x, myLocation.y + myStride), myRadius))
				{
					bottomBound = (int) cand.y + 1;
					rc.broadcast(CHANNEL_MAP_BOTTOM, bottomBound);
				}
			}
			++iterations;
			int taken = t1 - Clock.getBytecodesLeft();
			longest = Math.max(longest, taken);
		}
		System.out.println(iterations + " iterations: " + bestVal + " (" + nearbyBullets.length + "/" + importantBulletIndex + ": " + longest + " bytecodes)");
		if (best != null)
			opti = best;
		else
			opti = myLocation;

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
	}

	public static void onRoundBegin() throws GameActionException
	{
		roam = false;
		nearbyFriends = rc.senseNearbyRobots(100, myTeam);
		nearbyEnemies = rc.senseNearbyRobots(100, myTeam.opponent());
		nearbyBullets = rc.senseNearbyBullets();
		if (isGardener || isArchon)
		{
			nearbyTrees = rc.senseNearbyTrees(-1, myTeam);
			neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
			treeBuildTarget = null;
		}
		else
		{
			nearbyTrees = rc.senseNearbyTrees();
		}
		leftBound = rc.readBroadcast(CHANNEL_MAP_LEFT);
		rightBound = rc.readBroadcast(CHANNEL_MAP_RIGHT);
		bottomBound = rc.readBroadcast(CHANNEL_MAP_BOTTOM);
		topBound = rc.readBroadcast(CHANNEL_MAP_TOP);
		myLocation = rc.getLocation();
		history[round  % history.length] = myLocation;
		controlRadius = rc.readBroadcast(CHANNEL_CONTROL_RADIUS) / 1000f;
		helpLocation = readPoint(CHANNEL_CALL_FOR_HELP);
		helpRound = rc.readBroadcast(CHANNEL_CALL_FOR_HELP_ROUND);
		oldGardenerLocChannel = CHANNEL_GARDENER_LOCATIONS + 100 * (round % 2);
		newGardenerLocChannel = CHANNEL_GARDENER_LOCATIONS + 100 * ((round + 1) % 2);
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
		}
		int myWrite = writeNumberChannel(numberOfChannel);
		rc.broadcast(myWrite, rc.readBroadcast(myWrite) + 1);

		if (isGardener)
		{
			gardenerLocsLen = rc.readBroadcast(oldGardenerLocChannel);
			if (gardenerLocsLen <= 10)
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
			writePoint(newGardenerLocChannel + 1 + clen * 2, myLocation);
			rc.broadcast(newGardenerLocChannel, clen + 1);
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
		if (isSoldier && threatened && rc.getHealth() > 10)
		{
			for (RobotInfo info : nearbyFriends)
			{
				if (info.getType() == RobotType.GARDENER)
				{
					bruteDefence = true;
					break;
				}
			}
		}
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
		}
		else if (isScout)
		{
			findReflection();
			trees = rc.getTreeCount();
		}
		if (rc.getRoundNum() + 2 >= rc.getRoundLimit() || rc.getTeamVictoryPoints() + rc.getTeamBullets() / 10 > 1000)
		{
			rc.donate(10 * (int) (rc.getTeamBullets() / 10f));
		}
		if (myID == 0 && freeRange && isSoldier && nearbyEnemies.length == 0)
		{
			if (checkBlocked())
			{
				rc.broadcast(CHANNEL_FIRST_SOLDIER_BLOCKED, 1);
				freeRange = false;
			}
		}
	}

	static boolean checkBlocked()
	{
		for (int i = 0; i < history.length; i++)
		{
			if (history[i] == null)
			{
				return false;
			}
			if (history[i].distanceTo(myLocation) > 1)
			{
				return false;
			}
		}
		return true;
	}
	static void findReflection() throws GameActionException
	{
		reflection = null;
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
					return;
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
					debug_line(a, c, 255, 255, 0);
					debug_line(myLocation, c, 127, 127, 0);
					return;
				}
			}
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
					System.out.println(info.getLocation().distanceTo(loc) + " " + info.getRadius() + 1);
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
		return new MapLocation(rc.readBroadcast(pos)/1000.0f, rc.readBroadcast(pos + 1)/1000.0f);
	}

	public static int writePoint(int pos, MapLocation val) throws GameActionException
	{
		rc.broadcast(pos++, (int) val.x*1000);
		rc.broadcast(pos++, (int) val.y*1000);
		return pos;
	}


}
