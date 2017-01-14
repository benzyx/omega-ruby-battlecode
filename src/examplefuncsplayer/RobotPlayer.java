package examplefuncsplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
	public static final int CHANNEL_CURRENT_ROUND = 0;
	public static final int CHANNEL_NUMBER_OF_ARCHONS = 100;
	public static final int CHANNEL_NUMBER_OF_GARDENERS = 102;
	public static final int CHANNEL_NUMBER_OF_SOLDIERS = 104;
	public static final int CHANNEL_NUMBER_OF_LUMBERJACKS = 106;
	public static final int CHANNEL_NUMBER_OF_SCOUTS = 108;
	public static final int CHANNEL_NUMBER_OF_TANKS = 110;
	public static final int CHANNEL_BUILD_INDEX = 5;
	public static final int CHANNEL_MAP_TOP = 6;
	public static final int CHANNEL_MAP_BOTTOM = 7;
	public static final int CHANNEL_MAP_LEFT = 8;
	public static final int CHANNEL_MAP_RIGHT = 9;
	public static final int RALLY_POINT = 10;
	
	public static final float REPULSION_RANGE = 1.7f;
	
	static int topBound, leftBound, bottomBound, rightBound;
    static RobotController rc;
    static int myID;
    static MapLocation destination = null;
//    static Direction prevDirection = randomDirection();
    static RobotInfo[] nearbyEnemies;
    static RobotInfo[] nearbyFriends;
    static BulletInfo[] nearbyBullets;
    static TreeInfo[] nearbyTrees;
    static MapLocation repeller = null;
    static MapLocation[] history = new MapLocation[10];
    static TreeInfo closestTree = null;	// scouts use this to shake trees
    static int numberOfChannel;
    static RobotType myType;
    static boolean isScout, isArchon, isGardener, isSoldier, isTank, isLumberjack;
    static float myStride;
    static float myRadius;
    static MapLocation myLocation;
    static int round;

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
        numberOfChannel = myNumberOfChannel();
        myStride = myType.strideRadius;
        myRadius = myType.bodyRadius;
        isScout = myType == RobotType.SCOUT;
        isArchon = myType == RobotType.ARCHON;
        isGardener = myType == RobotType.GARDENER;
        isSoldier = myType == RobotType.SOLDIER;
        isTank = myType == RobotType.TANK;
        isLumberjack = myType == RobotType.LUMBERJACK;

        myID = rc.readBroadcast(myNumberOfChannel());
        rc.broadcast(myNumberOfChannel(), myID + 1);
        if (myType == RobotType.ARCHON)
        {
        	MapLocation them = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
        	MapLocation us = rc.getLocation();
//        	MapLocation rally = new MapLocation((us.x + us.x + them.x) / 3, (us.y + us.y + them.y) / 3);
        	MapLocation rally = us.add(us.directionTo(them), 7);
        	writePoint(RALLY_POINT, rally);
        	if (myID == 0)
        	{
        		rc.broadcast(CHANNEL_MAP_TOP, 100000);
        		rc.broadcast(CHANNEL_MAP_LEFT, 100000);
        		rc.broadcast(CHANNEL_MAP_RIGHT, -100000);
        		rc.broadcast(CHANNEL_MAP_BOTTOM, -100000);
        	}
        }
        destination = readPoint(RALLY_POINT);
        while (true)
        {
        	try {
        		round = rc.getRoundNum();
        		
            	onRoundBegin();
                destination = readPoint(RALLY_POINT);
            	switch (myType)
            	{
            	case GARDENER:
            		gardenerSpecificLogic();
            		break;
            	case ARCHON:
            		archonSpecificLogic();
            		break;
            	}
            	
            	if (myType == RobotType.SCOUT){
            		float minDist = 99999999;
            		closestTree = null;
            		for (TreeInfo info : nearbyTrees)
            		{
            			if(info.containedBullets > 0 && info.getLocation().distanceTo(rc.getLocation()) < minDist)
            			{
            				minDist = info.getLocation().distanceTo(rc.getLocation());
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
            	
            	selectOptimalMove();
            	MapLocation loc = opti;
            	rc.setIndicatorLine(rc.getLocation(), loc, 255, 0, 255);
//            	rc.setIndicatorLine(loc, destination, 255, 255, 255);
            	
            	if (rc.canMove(loc) && !rc.hasMoved())
            	{
            		rc.move(loc);
            		myLocation = loc;
            	}     	
	            	
            	long bestVal = 0;
            	Direction dir = null;
            	RobotType enemyType = null;
            	float enemyDistance = 0;
            	for (RobotInfo info : nearbyEnemies)
            	{
            		float dist = rc.getLocation().distanceTo(info.getLocation());
            		if (dist < 6)
            		{
            			long val = (long) getIdealDistanceMultiplier(info.getType());
            			if (dir == null || val > bestVal)
            			{
            				bestVal = val;
            				dir = rc.getLocation().directionTo(info.getLocation());
            				enemyType = info.getType();
            				enemyDistance = dist;
            			}
            		}
            	}
            	if (dir != null && rc.canFirePentadShot() && enemyType != RobotType.ARCHON && enemyDistance < 4.2 )
            	{
            		rc.firePentadShot(dir);
            	}
            	else if (rc.canFireSingleShot() && dir != null)
            	{
            		rc.fireSingleShot(dir);
            	}
            	
            	onRoundEnd();
            	
            	if (round != rc.getRoundNum() || Clock.getBytecodesLeft() < 200)
            	{
            		System.out.println("TLE");
            		rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
            	}
            	
        		Clock.yield();
        	}
        	catch (Exception e)
        	{
        		System.out.println("Exception in robot loop");
        		e.printStackTrace();
        	}
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
			if (gardeners == 1 && myLocation.distanceTo(destination) > 3)
			{
				return false;
			}
			for (TreeInfo info : nearbyTrees)
			{
				if (info.getLocation().distanceTo(myLocation) < 2.5)
				{
					return false;
				}
			}
			for (int i = 0; i < iter; i++)
	    	{
	    		Direction dir = randomDirection();
	    		if (rc.canPlantTree(dir)){
	    			rc.plantTree(dir);
	    			return true;
	    		}
	    	}
		}
		else
		{
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
    
    static int gardeners, soldiers, trees;
    static void getMacroStats() throws GameActionException
    {
    	gardeners = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_GARDENERS));
    	soldiers = rc.readBroadcast(readNumberChannel(CHANNEL_NUMBER_OF_SOLDIERS));
    	trees = rc.getTreeCount();
    }
    
    public static void gardenerSpecificLogic() throws GameActionException
    {
    	getMacroStats();
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
				if (info.health < lowestHP && info.team == rc.getTeam()){
					lowestHP = info.health;
					bestTree = info;
				}
			}
		}
		if (bestTree != null){
			rc.water(bestTree.ID);
			rc.setIndicatorDot(bestTree.location, 0, 0, 255);
		}
    }
    
    // What to build after our build order is done
    public static void macro() throws GameActionException
    {
    	System.out.println(gardeners + "/" + soldiers + "/" + trees);
    	
    	if (isArchon)
    	{
    		if (gardeners < trees / 6 + 1 || rc.getTeamBullets() > 350) // indicates some kind of blockage
    		{
    			attemptBuild(10, RobotType.GARDENER);
    		}
    	}
    	
    	if (isGardener)
    	{
    		if (soldiers < trees / 4 || soldiers < 2)
    		{
    			attemptBuild(10, RobotType.SOLDIER);
    		}
    		if (soldiers >= 2)
    		{
    			attemptBuild(10, RobotType.ARCHON);
    		}
    	}
    	
    	if (trees >= 25 && rc.getTeamBullets() > 200)
    	{
    		rc.donate(10);
    	}
    	if (rc.getRoundNum() + 2 >= rc.getRoundLimit() || rc.getTeamVictoryPoints() + rc.getTeamBullets() / 10 > 1000)
    	{
    		rc.donate(10 * ((int) rc.getTeamBullets()));
    	}
    }
    
    // Determines the next object to build in the build order. 
    // !!!! If the return object is Archon, build a Tree. !!!!
    public static RobotType getBuildOrderNext(int index){
    	 RobotType[] buildOrder = 
    	{
    			RobotType.SOLDIER,
    			RobotType.ARCHON,
    			RobotType.ARCHON,
    			null
    	};
    	return buildOrder[index];
    }
    
    public static long badness(MapLocation loc)
    {
    	long ret = 0;
    
    	// Scout code: Look for trees and shake 'em
    	if (isScout)
    	{
    		if (closestTree != null){
    			ret += closestTree.getLocation().distanceTo(loc) * 5000;
    		}
    	}
    	
    	ret += 1000 * loc.distanceTo(destination);
    	ret -= 500 * loc.distanceTo(myLocation);
    	
    	if (nearbyEnemies.length == 0)
    	{
//    		if (leftBound < loc.x) ret += 1000 * 500 * Math.max(0f, 3 - (loc.x - leftBound));
//    		if (topBound < loc.y) ret += 1000 * 500 * Math.max(0f, 3 - (loc.y - topBound));
//    		if (rightBound > loc.x) ret += 1000 * 500 * Math.max(0f, 3 - (rightBound - loc.x));
//    		if (bottomBound > loc.y) ret += 1000 * 500 * Math.max(0f, 3 - (bottomBound - loc.y));
    	}
    		    	
    	if (!isGardener && !isArchon)
    	{
	    	for (RobotInfo info : nearbyEnemies)
	    	{
				float d = info.getLocation().distanceTo(loc);
				float ideal = getIdealDistance(info.getType());
				if (ideal < 0)
					continue;
				if (isSoldier)
					ideal = 0;
				d -= ideal;
				d *= d;
				ret += (long) (d * getIdealDistanceMultiplier(info.getType()));
	    	}
    	}
    	
    	if (!isGardener)
    	for (RobotInfo info : nearbyFriends)
    	{
    		float d = info.getLocation().distanceTo(loc) - myRadius - info.getType().bodyRadius;
    		if (d < REPULSION_RANGE)
    		{
    			ret += 1000 * (1 / (0.01f + d / REPULSION_RANGE));
    		}
    	}
    	
    	if (isGardener)
    	{
    		for (TreeInfo info : nearbyTrees)
    		{
    			float damage = info.maxHealth - info.health;
    			if (damage < 10)
    			{
    				float d = info.location.distanceTo(loc);
    				if (d < 5)
    				{
    					ret -= 2000 * d;
    				}
    			}
    			else
    			{
    				ret += 1000 * 20 * damage * damage * info.location.distanceTo(loc);
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

            MapLocation loc = rc.getLocation();
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
    				float d = nearbyBullets[j].getLocation().distanceTo(rc.getLocation()); 
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
    		return 5000;
    	case GARDENER:
    		return 1000;
    	case ARCHON:
    		return 200;
    	case SOLDIER:
    		return 5000;
    	case SCOUT:
    		return 5000;
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
    	switch (t) {
    	case LUMBERJACK:
    		return 5; // 1.5 (lumberjack stride) + 1 radius (them) + 1 radius (us) + 1 (lumberjack attack range) + 0.5 (safety first kids)
    	case GARDENER:
    		return 2.1f;
    	case ARCHON:
    		return 3.1f;
    	case SOLDIER:
    		return 5;
    	case SCOUT:
    		return 2.1f;
    	default:
    		return 0;
    	}
    }
    
    static MapLocation opti;
    
    public static void selectOptimalMove() throws GameActionException
    {
    	preprocessBullets();
    	MapLocation best = null;
    	long bestVal = 0;
    	int iterations = 0;
    	int longest = 0;
    	while (Clock.getBytecodesLeft() - longest > 500 && iterations < 100)
    	{
    		int t1 = Clock.getBytecodesLeft();
			MapLocation cand = rc.getLocation().add(randomDirection(), myStride);
			if (iterations == 0)
			{
				cand = rc.getLocation();
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
				if (cand.x < leftBound)
				{
					leftBound = (int) cand.x;
					rc.broadcast(CHANNEL_MAP_LEFT, leftBound);
				}
				if (cand.x > rightBound)
				{
					rightBound = (int) cand.x + 1;
					rc.broadcast(CHANNEL_MAP_RIGHT, rightBound);
				}
				if (cand.y < topBound)
				{
					topBound = (int) cand.y;
					rc.broadcast(CHANNEL_MAP_TOP, topBound);
				}
				if (cand.y > bottomBound)
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
    		opti = rc.getLocation();
    	
    }
    
    public static void onRoundEnd()
    {
    	
    }
    
    public static void onRoundBegin() throws GameActionException
    {
    	MapLocation old = history[rc.getRoundNum() % history.length];
    	if (old != null && rc.getLocation().distanceTo(old) < 5)
    	{
    		repeller = old;
    	}
		history[rc.getRoundNum() % history.length] = rc.getLocation();
    	nearbyFriends = rc.senseNearbyRobots(100, rc.getTeam());
    	nearbyEnemies = rc.senseNearbyRobots(100, rc.getTeam().opponent());
    	nearbyBullets = rc.senseNearbyBullets();
    	if (isGardener || isArchon)
    	{
    		nearbyTrees = rc.senseNearbyTrees(-1, rc.getTeam());
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
    	
    	if (rc.readBroadcast(CHANNEL_CURRENT_ROUND) != round)
    	{
    		rc.broadcast(CHANNEL_CURRENT_ROUND, round);
    		rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_ARCHONS), 0);
    		rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_GARDENERS), 0);
    		rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_SOLDIERS), 0);
    		rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_TANKS), 0);
    		rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_LUMBERJACKS), 0);
    		rc.broadcast(writeNumberChannel(CHANNEL_NUMBER_OF_SCOUTS), 0);
    	}
    	int myWrite = writeNumberChannel(numberOfChannel);
    	rc.broadcast(myWrite, rc.readBroadcast(myWrite) + 1);
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
    	return x + round % 2;
    }
    
    static int readNumberChannel(int x)
    {
    	return x + (round + 1) % 2;
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
