package nobulletdodging;
import battlecode.common.*;

public strictfp class RobotPlayer {
	public static final int CHANNEL_NUMBER_OF_ARCHONS = 0;
	public static final int CHANNEL_NUMBER_OF_GARDENERS = 1;
	public static final int CHANNEL_NUMBER_OF_SOLDIERS = 2;
	public static final int CHANNEL_NUMBER_OF_LUMBERJACKS = 3;
	public static final int CHANNEL_NUMBER_OF_SCOUTS = 4;
	public static final int CHANNEL_BUILD_INDEX = 5;
	public static final int CHANNEL_MAP_TOP = 6;
	public static final int CHANNEL_MAP_BOTTOM = 7;
	public static final int CHANNEL_MAP_LEFT = 8;
	public static final int CHANNEL_MAP_RIGHT = 9;
	public static final int RALLY_POINT = 10;
	
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

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        myID = rc.readBroadcast(myNumberOfChannel());
        rc.broadcast(myNumberOfChannel(), myID + 1);
        if (rc.getType() == RobotType.ARCHON)
        {
        	writePoint(RALLY_POINT, rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
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
        		int round = rc.getRoundNum();
        		        		
            	onRoundBegin();
                if (nearbyEnemies.length > 0)
                {
                	writePoint(RALLY_POINT, nearbyEnemies[0].getLocation());
                }
                if (rc.getLocation().distanceTo(destination) < 10 || !rc.onTheMap(rc.getLocation().add(rc.getLocation().directionTo(destination), 3)))
                {
                	if (rand() < 60)
                	{
                		destination = rc.getLocation().add(randomDirection(), 100);
                	}
                	else
                	{
                		destination = readPoint(RALLY_POINT);
                	}
                }
            	switch (rc.getType())
            	{
            	case GARDENER:
            		gardenerSpecificLogic();
            		break;
            	case ARCHON:
            		archonSpecificLogic();
            		break;
            	}
            	MapLocation loc = selectOptimalMove();
            	if (rc.canMove(loc))
            	{
            		rc.move(loc);
            	}
	   
            	if (rc.getType() == RobotType.SCOUT){
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
	            	
            	long bestVal = 0;
            	Direction dir = null;
            	for (RobotInfo info : nearbyEnemies)
            	{
            		if (rc.getLocation().distanceTo(info.getLocation()) < 6)
            		{
            			long val = (long) getIdealDistanceMultiplier(info.getType());
            			if (dir == null || val > bestVal)
            			{
            				bestVal = val;
            				dir = rc.getLocation().directionTo(info.getLocation());	            				
            			}
            		}
            	}
            	if (rc.canFirePentadShot() && dir != null)
            	{
            		rc.firePentadShot(dir);
            	}
            	if (rc.canFireSingleShot() && dir != null)
            	{
            		rc.fireSingleShot(dir);
            	}
            	
            	onRoundEnd();
            	
            	if (round != rc.getRoundNum() || Clock.getBytecodesLeft() < 200)
            	{
            		System.out.println("TLE");
            		rc.disintegrate();
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
    	if (myID == 0)
    	{
    		boolean seesGardener = false;
	    	for (RobotInfo robot : nearbyFriends)
	    	{
	    		if (robot.getType() == RobotType.GARDENER)
	    		{
	    			seesGardener = true;
	    			break;
	    		}
	    	}
	    	if (!seesGardener || rc.getTeamBullets() > 300)
	    	{
	        	attemptBuild(10, RobotType.GARDENER);    		
	    	}
    	}
    	else{
    		if (rc.getTeamBullets() > 275)
	    	{
	        	attemptBuild(10, RobotType.GARDENER);    		
	    	}
    	}
    	randomWalk();
    }
    
    // When the type parameter is ARCHON, we build a TREE instead.
    public static boolean attemptBuild(int iter, RobotType type) throws GameActionException
    {
		if (type == RobotType.ARCHON){
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
    
    public static void gardenerSpecificLogic() throws GameActionException
    {
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
				if (info.health < lowestHP){
					lowestHP = info.health;
					bestTree = info;
				}
			}
		}
		if (bestTree != null){
			rc.water(bestTree.ID);
			rc.setIndicatorDot(bestTree.location, 0, 0, 255);
		}
		
		randomWalk();
		
    }
    
    // What to build after our build order is done
    public static void macro() throws GameActionException
    {
    	if (rc.getTeamBullets() > 150){
    		long r = rand();
    		if (r < 135){
    			attemptBuild(10, RobotType.ARCHON);
    		}
    		if (r < 270){
    			attemptBuild(10, RobotType.SOLDIER);
    		}
    		if (r < 360){
    			attemptBuild(10, RobotType.SCOUT);
    		}
    	}
    }
    
    // Determines the next object to build in the build order. 
    // !!!! If the return object is Archon, build a Tree. !!!!
    public static RobotType getBuildOrderNext(int index){
    	 RobotType[] buildOrder = 
    	{
    			RobotType.SCOUT,
    			RobotType.SOLDIER,
    			RobotType.ARCHON,
    			RobotType.ARCHON,
    			RobotType.SOLDIER,
    			RobotType.SOLDIER,
    			null
    	};
    	return buildOrder[index];
    }
    
    public static long badness(MapLocation loc)
    {
    	long ret = 0;
    	
    	if (rc.getType() != RobotType.ARCHON && rc.getType() != RobotType.GARDENER)
    	{
    		ret += (long) (destination.distanceTo(loc) * 1000);
	    	ret -= (long) (loc.distanceTo(repeller) * 700);
    	}
    
    	// Scout code: Look for trees and shake 'em
    	if (rc.getType() == RobotType.SCOUT)
    	{
    		if (closestTree != null){
    			ret += closestTree.getLocation().distanceTo(loc) * 5000;
    		}
    	}
    	else
    	{	    	
	    	for (RobotInfo info : nearbyEnemies)
	    	{
				float d = info.getLocation().distanceTo(loc);
				float ideal = getIdealDistance(info.getType());
				if (ideal < 0)
					continue;
				d -= ideal;
				d *= d;
				ret += (long) (d * getIdealDistanceMultiplier(info.getType()));
	    	}
    	}
    	
//    	for (int i = 0; i < importantBulletIndex; i++)
//    	{
//    		BulletInfo info = nearbyBullets[i];
//    		float d = info.getLocation().distanceTo(loc);
//    		MapLocation bullet = info.getLocation().add(info.dir, d);
//    		float margin = bullet.distanceTo(loc);
//    		float rad = rc.getType().bodyRadius + info.speed + 0.01f;
//    		float c = margin / rad;
//    		if (margin < rad)
//    		{
//    			ret += 2000000 * Math.sqrt(1 - c) / d;
//    		}
//    	}
    	
    	return ret;
    }
    
    static int importantBulletIndex;
    
    public static void preprocessBullets()
    {
    	importantBulletIndex = nearbyBullets.length;
    	for (int i = 0; i < importantBulletIndex; i++)
    	{
    		BulletInfo info = nearbyBullets[i];
    		float d = info.getLocation().distanceTo(rc.getLocation());
    		MapLocation bullet = info.getLocation().add(info.dir, d);
    		float margin = bullet.distanceTo(rc.getLocation());
    		float rad = rc.getType().bodyRadius + rc.getType().strideRadius + info.speed + 0.01f;
    		if (margin > rad)
    		{
    			--importantBulletIndex;
    			nearbyBullets[i] = nearbyBullets[importantBulletIndex];
    			nearbyBullets[importantBulletIndex] = info;
    		}
    	}
    }
    
    public static float getIdealDistanceMultiplier(RobotType t)
    {
    	switch (t) {
    	case LUMBERJACK:
    		return 100000;
    	case GARDENER:
    		return 1000;
    	case ARCHON:
    		return 200;
    	case SOLDIER:
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
    	default:
    		return 0;
    	}
    }
    
    public static MapLocation selectOptimalMove() throws GameActionException
    {
    	if (rc.getType() == RobotType.ARCHON)
    	{
    		return rc.getLocation();
    	}
    	preprocessBullets();
    	MapLocation best = null;
    	long bestVal = 0;
    	int iterations = 0;
    	for (RobotInfo info : nearbyEnemies)
    	{
			MapLocation them = info.getLocation();
			MapLocation us = rc.getLocation();
			float d = them.distanceTo(us);
			if (d < 10)
			{
				MapLocation cand = them.add(them.directionTo(us), getIdealDistance(info.getType()));
				if (rc.canMove(cand))
				{
					long b = badness(cand);
    				if (best == null || b < bestVal)
    				{
    					best = cand;
    					bestVal = b;
    				}
				}
			}
    	}
    	while (Clock.getBytecodesLeft() > 1500)
    	{
			MapLocation cand = rc.getLocation().add(randomDirection(), getStride(rc.getType()));
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
    		++iterations;
    	}
    	System.out.println(iterations + " iterations");
    	if (best != null)
    		return best;
    	else
    		return rc.getLocation();
    	
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
    	nearbyTrees = rc.senseNearbyTrees();
    	leftBound = rc.readBroadcast(CHANNEL_MAP_LEFT);
    	rightBound = rc.readBroadcast(CHANNEL_MAP_RIGHT);
    	bottomBound = rc.readBroadcast(CHANNEL_MAP_BOTTOM);
    	topBound = rc.readBroadcast(CHANNEL_MAP_TOP);
    	
    	System.out.println(leftBound + " " + topBound + " " + rightBound + " " + bottomBound);
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
        return new Direction((float) (rand() / Math.PI / 2));
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
    	switch (rc.getType())
    	{
    	case ARCHON: return CHANNEL_NUMBER_OF_ARCHONS;
    	case GARDENER: return CHANNEL_NUMBER_OF_GARDENERS;
    	case SOLDIER: return CHANNEL_NUMBER_OF_SOLDIERS;
    	case LUMBERJACK: return CHANNEL_NUMBER_OF_LUMBERJACKS;
    	case SCOUT: return CHANNEL_NUMBER_OF_SCOUTS;
    	}
    	throw new RuntimeException();
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
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
    
    public static void randomWalk() throws GameActionException{
    	if (true) return;
    	for(int i = 0; i<10; i++){
    		Direction dir = randomDirection();
    		if (rc.canMove(dir, getStride(rc.getType())))
    		{
    			rc.move(dir, getStride(rc.getType()));
    			break;
    		}
    	}
    }
    
}
