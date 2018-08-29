import bc.*;
import java.util.*;

public strictfp class Utility {
    boolean[][] passables;
    boolean[][]marsPassables;
    boolean hasLaunched;

    long[][]earthKarboniteLoc;
    long[][]marsKarboniteLoc;

    public long karbonite;

    public long roundNum;
    public int currentStrategy;

    public static int knights = 0;

    public int workers = 0;
    public int factories = 0;

    public static final int NEUTRAL = 0;
    public static final int RUSH = 1;
    public static final int RETREAT = 2;
    public static final int RANGED = 3;
    public static final int HEAVY = 4;

    public LinkedList<Integer> unbuilt;
    public LinkedList<Integer>rocketsOnMars;

    public Queue<MapLocation>priorityMines;

    static PriorityQueue<Unit>priorityHeals;
    static PriorityQueue<Unit>priorityTargets;

    public HashMap<Integer,Queue<MapLocation>>myWorkers;
    public HashMap<Integer,Queue<MapLocation>>workerBuilds;

    public HashMap<Integer,Queue<Integer>>myKnights;
    public HashMap<Integer,LinkedList<MapLocation>>knightPaths;

    public HashMap<Integer,Queue<Integer>>myMages;

    public HashMap<Integer,Queue<Integer>>myRangers;

    public HashMap<Integer,Queue<MapLocation>>attackVectors;

    public HashMap<Integer,Queue<MapLocation>>myHealers;
    public HashMap<Integer,Queue<MapLocation>>overcharges;
    public HashMap<Integer,Queue<MapLocation>>healVectors;

    public LinkedList<Unit>overchargeRequests;
    public HashMap<Integer,Float>healRequests;

    public HashMap<Integer,Queue<UnitType>>buildOrders;

    public HashMap<Integer,Boolean>myFactories;
    public HashMap<Integer,Boolean>myRockets;

    public HashSet<MapLocation>claimedMines;

    public HashMap<Integer,Long>elapsedTime;

    public HashMap<Integer,Direction>lastWorkingDirection;

    public Grid grid;

    Direction[] compass;

    GameController controller;

    PlanetMap earth;
    PlanetMap mars;

    AsteroidPattern marsAstroids;
    AsteroidPattern earthAstroids;

    MapLocation allyStart;
    MapLocation oppStart;

    LinkedList<MapLocation> goals;
    LinkedList<MapLocation>marsGoals;

    Team ally;
    Team opp;

    Random random = new Random();

    LinkedList<Integer>enemies;

    public Utility(GameController gc){
        controller = gc;

        roundNum = controller.round();

        earth = controller.startingMap(Planet.Earth);
        mars = controller.startingMap(Planet.Mars);

        //earthAstroids = new AsteroidPattern(0,earth);
        marsAstroids = new AsteroidPattern(0,mars);

        myWorkers = new HashMap<>();
        workerBuilds = new HashMap<>();

        myFactories = new HashMap<>();
        myRockets = new HashMap<>();

        myKnights = new HashMap<>();
        knightPaths = new HashMap<>();

        myMages = new HashMap<>();

        myRangers = new HashMap<>();

        attackVectors = new HashMap<>();

        myHealers = new HashMap<>();
        healRequests = new HashMap<>();
        healVectors = new HashMap<>();
        overcharges = new HashMap<>();
        overchargeRequests = new LinkedList<>();

        buildOrders = new HashMap<>();

        karbonite = controller.karbonite();

        goals = new LinkedList<>();
        marsGoals = new LinkedList<>();

        passables = new boolean[(int)earth.getWidth()][(int)earth.getHeight()];
        earthKarboniteLoc = new long[(int)earth.getWidth()][(int)earth.getHeight()];

        enemies = new LinkedList<>();
        rocketsOnMars = new LinkedList<>();

        elapsedTime = new HashMap<>();

        lastWorkingDirection = new HashMap<>();

        for(int i=0;i<earth.getWidth();i++){
            for(int j=0;j<earth.getHeight();j++){
                MapLocation loc = new MapLocation(earth.getPlanet(),i,j);
                if(!earth.onMap(loc)){
                    passables[i][j] = false;
                }
                passables[i][j] = earth.isPassableTerrainAt(loc) != 0;
                earthKarboniteLoc[i][j] = earth.initialKarboniteAt(loc);
                //System.out.println(passables[i][j]);
            }
        }

        marsPassables = new boolean[(int)mars.getWidth()][(int)mars.getHeight()];
        marsKarboniteLoc = new long[(int)mars.getWidth()][(int)mars.getHeight()];
        for(int i=0;i<mars.getWidth();i++){
            for(int j=0;j<mars.getHeight();j++){
                MapLocation loc = new MapLocation(mars.getPlanet(),i,j);
                if(!mars.onMap(loc)){
                    marsPassables[i][j] = false;
                }
                marsPassables[i][j] = mars.isPassableTerrainAt(loc) != 0;
                marsKarboniteLoc[i][j] = mars.initialKarboniteAt(loc);
            }
        }

        ally = controller.team();
        opp = ally == Team.Red?Team.Blue:Team.Red;

        compass = Direction.values();

        VecUnit temp = earth.getInitial_units();
        for(int i=0;i<temp.size();i++){
            if(temp.get(i).team() == ally){
                allyStart = temp.get(i).location().mapLocation();
            }else{
                oppStart = temp.get(i).location().mapLocation();
                goals.add(temp.get(i).location().mapLocation());
            }
        }
        //grid = new Grid(controller,allyStart,this);
        //grid.runBfs(controller,goals.getFirst(),this);

        claimedMines = new HashSet<>();
        unbuilt = new LinkedList<>();

        setStrategy();
    }
    public void updateUtility(){
        VecUnit units = controller.units();

        karbonite = controller.karbonite();
        roundNum = controller.round();
        getAsteroidLocations();

        for(int i=0;i<units.size();i++){
            switch(units.get(i).unitType()){
                case Worker:
                    if(!myWorkers.containsKey(units.get(i).id())){
                        Queue<MapLocation>mines = new LinkedList<>();
                        myWorkers.put(units.get(i).id(),mines);
                    }
                    if(!workerBuilds.containsKey(units.get(i).id())){
                        Queue<MapLocation>toBuild = new LinkedList<>();
                        workerBuilds.put(units.get(i).id(),toBuild);
                    }
                    break;
                case Factory:
                    if(!buildOrders.containsKey(units.get(i).id())){
                        Queue<UnitType>toBuild = new LinkedList<>();
                        buildOrders.put(units.get(i).id(),toBuild);
                        factories++;
                    }
                    break;
                case Rocket:
                    if(!myRockets.containsKey(units.get(i).id())){
                        myRockets.put(units.get(i).id(),false);
                    }
                    break;
                case Mage:
                    if(!myMages.containsKey(units.get(i).id())){
                        Queue<Integer>toKill = new LinkedList<>();
                        myMages.put(units.get(i).id(),toKill);
                    }
                    if(!attackVectors.containsKey(units.get(i).id())){
                        Queue<MapLocation>vectors = new LinkedList<>();
                        attackVectors.put(units.get(i).id(),vectors);

                        for(int j=0;j<goals.size();j++){
                            attackVectors.get(units.get(i).id()).offer(goals.get(j));
                        }
                    }
                    break;
                case Ranger:
                    if(!myRangers.containsKey(units.get(i).id())){
                        Queue<Integer>toKill = new LinkedList<>();
                        myRangers.put(units.get(i).id(),toKill);
                    }
                    if(!attackVectors.containsKey(units.get(i).id())){
                        Queue<MapLocation>vectors = new LinkedList<>();
                        attackVectors.put(units.get(i).id(),vectors);

                        for(int j=0;j<goals.size();j++){
                            attackVectors.get(units.get(i).id()).offer(goals.get(j));
                        }
                    }
                    break;
                case Healer:
                    if(!myHealers.containsKey(units.get(i).id())){
                        Queue<MapLocation>toHeal = new LinkedList<>();
                        myHealers.put(units.get(i).id(),toHeal);
                    }
                    if(!healVectors.containsKey(units.get(i).id())){
                        Queue<MapLocation>vectors = new LinkedList<>();
                        healVectors.put(units.get(i).id(),vectors);
                    }
                    break;
                case Knight:
                    if(!myKnights.containsKey(units.get(i).id())){
                        Queue<Integer>toKill = new LinkedList<>();
                        myKnights.put(units.get(i).id(),toKill);
                    }
                    if(!attackVectors.containsKey(units.get(i).id())){
                        Queue<MapLocation>vectors = new LinkedList<>();
                        attackVectors.put(units.get(i).id(),vectors);

                        for(int j=0;j<goals.size();j++){
                            attackVectors.get(units.get(i).id()).offer(goals.get(j));
                        }
                    }
                    break;
            }
        }
        //System.out.println("utility update complete");
    }
    public MapLocation getOpen(Unit unit){
        VecMapLocation nearby = controller.allLocationsWithin(unit.location().mapLocation(),unit.visionRange()/2);

        for(int i=0;i<nearby.size();i++){
            if(nearby.get(i).isAdjacentTo(unit.location().mapLocation())){
                if(isPassable(nearby.get(i))){
                    return nearby.get(i);
                }
            }
        }
        return null;
    }

    public void scannedArea(){

    }
    public Stack<MapLocation> path(MapLocation to,MapLocation from){
        Queue<MapLocation>aux  = new LinkedList<>();
        Stack<MapLocation>path  = new Stack<>();

        if(earth.getPlanet() == to.getPlanet() && earth.getPlanet() == from.getPlanet()){
            if(earth.onMap(to) && earth.onMap(from)){
                MapLocation current = from;
                while(!current.isAdjacentTo(to)){
                    VecMapLocation adj = controller.allLocationsWithin(current,30);
                    MapLocation best = adj.get(0);
                    for(int i=0;i<adj.size();i++){
                        MapLocation loc = adj.get(i);

                        if(earth.isPassableTerrainAt(loc) == 0)continue;

                        if(loc.distanceSquaredTo(to) < best.distanceSquaredTo(to)
                                && !aux.contains(loc)){
                            aux.offer(loc);
                            best = loc;
                        }
                    }
                    current = aux.poll();
                }
                while(!aux.isEmpty()){
                    path.push(aux.remove());
                }
            }
        }
        return path;
    }

    public float healDesire(Unit self,int healerCount){
        float base = (self.maxHealth()-self.health());
       // float expDen = (long)Math.pow((healerCount+1),4);
       // float exp = (long)((1f-expDen)*.25f);
       // float desire = (long)(Math.pow(base,exp));
       // System.out.println("HEAL DESIRE "+base);
        return base;
    }
    public float[] normalize(float[]data){
        float[]auxData = new float[data.length];

        float max = 0;
        float min = 0;

        for(Float num:data){
            max = Math.max(num,max);
            min = Math.min(num,min);
        }
        for(int i=0;i<auxData.length;i++){
            auxData[i] = (data[i]-min)/(max-min);
        }
        return auxData;
    }
    public float sigmoid(float x,int lim){//hard = 1,medium = 2,soft = 4
        float y = (float)(1/(1+Math.pow(Math.E,lim*x)));
        return y;
    }
    public float linear(float x,float m,float c){
        float y = m*x+c;
        return y;
    }
    public float stepFunction(float x,float lim ){//on off switch
        float y = 0;
        if(x > lim) {
            y = 1;
        }
        return y;
    }
    public float decreasingIncrease(float x,double lim){//hard = .25,medium = .5,soft = .75
        float y = (float)Math.pow(x,lim);
        return y;
    }
    public float exponentialIncrease(float x,int step){//hard = 2,medium = 3, soft = 4
        float y = (float)Math.pow(x,step);
        return y;
    }
    public float exponentialDecay(float x,double lim){//hard = .1,medium = .5,soft = .9
        float y = (float)Math.pow(lim,x);
        return y;
    }
    public long threatLevel(Unit self,Unit enemy){
        if(enemy.unitType() != UnitType.Factory || enemy.unitType() != UnitType.Rocket){
            return 1;
        }
        long threat = Math.min((enemy.damage()/self.health()),1);
        return threat;
    }
    public double attackDesire(Unit self,Unit enemy,float combatScore){
        double a = self.health()-enemy.damage();
        double b = (1-(a/enemy.damage()))*(1-combatScore)+combatScore;
        double desire = Math.max(Math.min(b,1f),0);
        return desire;
    }
    public int highestIndex(float[]data){
        float highest = data[0];
        int index = 0;
        for(int i=0;i<data.length;i++){
            if(data[i] > highest){
                highest = data[i];
                index = i;
            }
        }
        return index;
    }

    public float calculateMagePriority(){
        float need = -.5f;
        need += exponentialDecay(myMages.size(),.1);
        need += exponentialIncrease(enemies.size(),4);
        return need;
    }
    public float calculateKnightPriority(){
        float need = -.5f;
        need += exponentialDecay(myKnights.size(),.1);
        need += exponentialIncrease(enemies.size(),4);

        //if(controller.researchInfo().getLevel(UnitType.Mage) == 4)need = 0;

        return need;
    }
    public float calculateHealPriority(){
        float need = -.1f;
        need += sigmoid(enemies.size(),4);
        need += exponentialDecay(myHealers.size(),.5);
        need += decreasingIncrease(healRequests.size(),.75f);
        return need;
    }
    public float calculateRangerPriority(){
        float need = .5f;
        need += exponentialDecay(myRangers.size(),.1);
        need += exponentialIncrease(enemies.size(),4);
        return need;
    }
    public MapLocation bestHealLocation(){
        Set<Integer> set = healRequests.keySet();
        Collection<Float> desire = healRequests.values();
        float best = 0;
        Unit ret = null;
        for(Integer id:set){
            if(!checkUnitIDExistence(id)){
                //healRequests.remove(id);
                continue;
            }
            Unit unit = controller.unit(id);
            for(Float des:desire){
                if(des > best){
                    best = des;
                    ret = unit;
                }
            }
        }
        if(ret != null){
            MapLocation loc = ret.location().mapLocation();
            System.out.println("HEALER FOUND BEST HEAL");
            return loc;
        }
        return null;
    }

    public MapLocation goodMarsLanding(Unit unit){
        MapLocation best = null;
        for(int i=0;i<20;i++){
            int x = random.nextInt((int)mars.getWidth());
            int y = random.nextInt((int)mars.getHeight());
            if(marsPassables[x][y]){
                best = new MapLocation(mars.getPlanet(),x,y);
                System.out.println("FOUND MARS LANDING "+best);
                return best;
            }
        }
        System.out.println("MARS LANDING FAILED "+best);
        return best;
    }
    public MapLocation goodEarthLanding(Unit unit){
        MapLocation best = null;
        for(int i=0;i<20;i++){
            int x = random.nextInt((int)earth.getWidth());
            int y = random.nextInt((int)earth.getHeight());
            if(passables[x][y]){
                best = new MapLocation(earth.getPlanet(),x,y);
                System.out.println("FOUND EARTH LANDING "+best);
                return best;
            }
        }
        System.out.println("EARTH LANDING FAILED "+best);
        return best;
    }
    public void getAsteroidLocations(){
        if(marsAstroids.hasAsteroid(roundNum)){
            marsGoals.add(marsAstroids.asteroid(roundNum).getLocation());
        }
    }

    public MapLocation findClosestGoal(Unit unit,Collection<MapLocation>goals){
        long closest = 99999;
        MapLocation best = null;
        for(MapLocation loc: goals){
            //if(!loc.isWithinRange(unit.visionRange(),unit.location().mapLocation()))continue;
            long distance = unit.location().mapLocation().distanceSquaredTo(loc);
            if(distance <= closest){
                closest = distance;
                best = loc;
            }
        }
        //System.out.println("FOUND CLOSEST GOAL "+best);
        return best;
    }
    public void enumerateOvercharges(Unit unit){
        if(overcharges.containsKey(unit.id())){
            for(Unit ally:overchargeRequests){
                if(ally.location().mapLocation().isWithinRange(unit.abilityRange(),unit.location().mapLocation())){
                    overcharges.get(unit.id()).offer(ally.location().mapLocation());
                    overchargeRequests.remove(ally);
                }
            }
        }
    }
    public void enumerateHeals(Unit unit){
        if(unit.location().isInSpace() || unit.location().isInGarrison())return;

        Set<Integer>set = healRequests.keySet();
        float best = -1111f;
        Unit toAdd = null;
        for(Integer x:set){
            if(best == -1111f){
                best = healRequests.get(x);
            }
            if(checkUnitIDExistence(x)){
                Unit toHeal = controller.unit(x);
                if(toHeal.location().isInGarrison() || toHeal.location().isInGarrison())continue;

                if(toHeal.location().mapLocation().isWithinRange(unit.visionRange(),unit.location().mapLocation())){
                    if(healRequests.get(x) > best){
                        best = healRequests.get(x);
                        toAdd = toHeal;
                    }
                }
            }
        }
        if(toAdd != null){
            myHealers.get(unit.id()).offer(toAdd.location().mapLocation());
        }

        VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),ally);
        for(int i=0;i<units.size();i++){
            if(units.get(i).unitType() == UnitType.Factory)continue;

            if(myHealers.containsKey(unit.id())){
                if(healDesire(units.get(i),myHealers.size()) > 0){
                    System.out.println("ADDING HEALER HEALS");
                    myHealers.get(unit.id()).offer(units.get(i).location().mapLocation());
                }
            }
        }
    }
    public void enumerateEnemies(Unit unit){
        VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),opp);

        for(int i=0;i<units.size();i++){
            if(myKnights.containsKey(unit.id())){
                myKnights.get(unit.id()).offer(units.get(i).id());
                if(units.get(i).unitType() == UnitType.Factory
                        && !goals.contains(units.get(i).location().mapLocation())){
                    goals.add(units.get(i).location().mapLocation());
                }
            }
        }
    }
    public Queue<MapLocation>getBestKarbonite(Unit unit){
        VecMapLocation adj = controller.allLocationsWithin(unit.location().mapLocation(),unit.visionRange());
        Queue<MapLocation>ret = new LinkedList<>();

        long min = 0;
        for(int i=0;i<adj.size();i++){
            if(claimedMines.contains(adj.get(i)))continue;
            if(controller.karboniteAt(adj.get(i)) == 0)continue;

            if(unit.location().isOnPlanet(earth.getPlanet())){
                if(earth.initialKarboniteAt(adj.get(i)) > min && unit.location().mapLocation().isWithinRange(4,adj.get(i))){
                    //System.out.println("added karbo loc");
                    ret.offer(adj.get(i));
                    claimedMines.add(adj.get(i));
                }
            }else{
                if(mars.initialKarboniteAt(adj.get(i)) > min && unit.location().mapLocation().isWithinRange(4,adj.get(i))){
                    //System.out.println("added karbo loc");
                    ret.offer(adj.get(i));
                    claimedMines.add(adj.get(i));
                }
            }
            if(claimedMines.size() > 20)claimedMines.clear();
            if(ret.size() > 5){
                break;
            }
        }
        return ret;
    }
    public boolean isSafe(Unit unit){
        VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),opp);
        for(int i=0;i<units.size();i++){
            if(units.get(i).location().mapLocation().isWithinRange(unit.visionRange(),unit.location().mapLocation())){
                return false;
            }
        }
        return true;
    }

    public void setResearch(){
        controller.queueResearch(UnitType.Ranger);

        controller.queueResearch(UnitType.Knight);
        controller.queueResearch(UnitType.Ranger);
        controller.queueResearch(UnitType.Rocket);

        controller.queueResearch(UnitType.Knight);
        controller.queueResearch(UnitType.Healer);
        controller.queueResearch(UnitType.Ranger);
    }
    public void tagEnemies(Unit unit){
        VecUnit seen = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),opp);

        for(int i=0;i<seen.size();i++){
            if(!enemies.contains(seen.get(i).id())){
                enemies.add(seen.get(i).id());
                if(seen.get(i).unitType() == UnitType.Factory){
                    goals.add(seen.get(i).location().mapLocation());
                }
            }
        }
        int size = enemies.size();
        LinkedList<Integer>toRemove = new LinkedList<>();

        for(int i=0;i<size;i++){
            int id = enemies.get(i);
            if(!checkUnitIDExistence(id)){
               toRemove.add(id);
            }
        }

        enemies.removeAll(toRemove);
    }
    public Unit closestEnemy(Unit unit){
        long closest = unit.visionRange();
        Unit best;

        long range = unit.visionRange();
        if(unit.unitType() == UnitType.Worker){
            range /= 2;
        }
        VecUnit enemies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),range,opp);

        if(enemies.size() == 0)return null;

        best = enemies.get(0);

        for(int i=0;i<enemies.size();i++){
            long distance = unit.location().mapLocation().distanceSquaredTo(enemies.get(i).location().mapLocation());
            if(distance <= closest){
                best = enemies.get(i);
                closest = distance;
            }
        }
        return best;
    }
    public Unit closesAlly(Unit unit){
        long closest = unit.visionRange();
        Unit best;

        VecUnit allies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),ally);

        if(allies.size() == 0)return null;

        best = allies.get(0);

        for(int i=0;i<allies.size();i++){
            long distance = unit.location().mapLocation().distanceSquaredTo(allies.get(i).location().mapLocation());
            if(distance <= closest){
                best = allies.get(i);
                closest = distance;
            }
        }
        return best;
    }

    public boolean isPassable(MapLocation loc){
        if(loc.getPlanet() == mars.getPlanet()){
            return marsPassables[loc.getX()][loc.getY()];
        }
        return passables[loc.getX()][loc.getY()];
    }
    public boolean move(Unit unit, MapLocation loc){
        try{
            if(unit == null)return false;
            Direction dir = unit.location().mapLocation().directionTo(loc);

            if(controller.isMoveReady(unit.id()) && unit.movementHeat() < 10) {
                int check = 0;
                if(!lastWorkingDirection.containsKey(unit.id())){
                    lastWorkingDirection.put(unit.id(),dir);
                    //System.out.println("added to list of working direction "+lastWorkingDirection.get(unit.id()));
                }
                if(!controller.canMove(unit.id(),dir)){
                    if(!controller.canMove(unit.id(),lastWorkingDirection.get(unit.id()))){
                        while (!controller.canMove(unit.id(),dir)) {
                            if (check%2 == 0) {
                                dir = bc.bcDirectionRotateLeft(dir);
                            } else {
                                dir = bc.bcDirectionRotateRight(dir);
                            }
                            lastWorkingDirection.replace(unit.id(),dir);
                            if (check++ == 8) {
                                //dir = lastWorkingDirection.get(unit.id());
                                return false;
                            }
                        }
                        if(controller.isOccupiable(unit.location().mapLocation().add(dir)) != 0){
                            controller.moveRobot(unit.id(),dir);
                            return true;
                        }
                    }else{
                        if(controller.isOccupiable(unit.location().mapLocation().add(lastWorkingDirection.get(unit.id()))) != 0){
                            controller.moveRobot(unit.id(),lastWorkingDirection.get(unit.id()));
                            return true;
                        }
                    }
                }else{
                    if(controller.isOccupiable(unit.location().mapLocation().add(dir)) != 0){
                        controller.moveRobot(unit.id(),dir);
                        return true;
                    }
                }
            }
        }catch(Exception e){
            return false;
        }
        return false;
    }
    public void dodge(Unit unit,Unit enemy){
        Direction to = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
        Direction from = enemy.location().mapLocation().directionTo(unit.location().mapLocation());

        if(enemy.unitType() == UnitType.Factory)return;
        if(enemy.unitType() == UnitType.Worker)return;
        if(enemy.unitType() != UnitType.Worker && enemy.damage() == 0)return;

        VecMapLocation nearbyDodge = controller.allLocationsWithin(unit.location().mapLocation(),unit.visionRange()/2);

        long furthest = -1;
        MapLocation best = nearbyDodge.get(0);

        for(int i=0;i<nearbyDodge.size();i++){
            long distance = nearbyDodge.get(i).distanceSquaredTo(best);
            if(distance > furthest){
                furthest = distance;
                best = nearbyDodge.get(i);
            }
        }

        if(best != null){
            if(!move(unit,best)){
                wander(unit);
            }
        }
    }
    public boolean wander(Unit unit){
        Direction dir = compass[random.nextInt(7)];
        int checks = 0;

        MapLocation check = unit.location().mapLocation().add(dir);

        do{
            dir = compass[random.nextInt(7)];
            check = unit.location().mapLocation().add(dir);

            if(checks++ == 10)break;

            if(unit.location().isOnPlanet(earth.getPlanet()) && !earth.onMap(check))return false;
            if(unit.location().isOnPlanet(mars.getPlanet()) && !mars.onMap(check))return false;

        }while(controller.isOccupiable(check) == 0);

        if(checks >= 10)return false;

        if(controller.isMoveReady(unit.id()) && controller.canMove(unit.id(),dir)){
            controller.moveRobot(unit.id(),dir);
            return true;
        }
        return false;
    }

    public boolean build(Unit unit,Integer id){
        Unit structure = null;

        if(checkUnitIDExistence(id)){
            structure = controller.unit(id);
        }

        if(structure != null && (structure.unitType() == UnitType.Factory || structure.unitType() == UnitType.Rocket)){
            if(!unit.location().mapLocation().isAdjacentTo(structure.location().mapLocation())){
                move(unit,structure.location().mapLocation());
            }
            if(controller.canBuild(unit.id(),structure.id())){
                controller.build(unit.id(),structure.id());
                /*if(structure.structureIsBuilt() == 0){
                    if(!unbuilt.contains(structure.location().mapLocation())){
                        unbuilt.offer(structure.location().mapLocation());
                    }
                }*/
                System.out.println("BUILD STRUCTURE TRUE");
                return true;
            }
        }
        return false;
    }

    public boolean checkUnitIDExistence(Integer x){
        try{
            Unit check = controller.unit(x);
        }catch(Exception e){
            return false;
        }
        return true;
    }
    public boolean checkUnitExistence(MapLocation loc){
        try{
            controller.senseUnitAtLocation(loc);
        }catch(Exception | UnknownError e){
            System.out.println("Unit doesn't exists at location "+loc);
            return false;
        }
        return true;
    }
    public boolean checkUnitCanSee(Integer id){
        try{
            return controller.canSenseUnit(id);
        }catch(Exception | UnknownError e){
            System.out.println(id+" unit doesnt exist");
            return false;
        }
    }
    public boolean bestRepair(Unit unit,UnitType structure){
        VecUnit structures = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),ally);

        if(structures.size() > 0){
            for(int i=0;i<structures.size();i++){
                Unit toRepair = structures.get(i);
                if(toRepair.unitType() != structure)continue;
                if(toRepair.health() == toRepair.maxHealth())continue;

                if(toRepair.structureIsBuilt() == 0){
                   return build(unit,toRepair.id());
                }

                Direction dir = compass[random.nextInt(compass.length)];
                MapLocation adj = toRepair.location().mapLocation().subtract(dir);

                if(!unit.location().mapLocation().isAdjacentTo(toRepair.location().mapLocation())){
                    move(unit,adj);
                }

                if(controller.canRepair(unit.id(),toRepair.id())){
                    controller.repair(unit.id(),toRepair.id());
                    System.out.println(unit.id()+" repaired "+toRepair.id());
                    return true;
                }
            }
        }
        return false;
    }
    public boolean workerShouldBuild(Unit unit){
        Set<Integer>set = myFactories.keySet();
        for(Integer x: set){
            if(!myFactories.get(x)){
                if(checkUnitIDExistence(x)){
                    Unit toBuild = controller.unit(x);
                    if(unit.location().mapLocation().isWithinRange(unit.visionRange(),toBuild.location().mapLocation())){
                        //System.out.println("need to build");
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean workerBuildOrMine(Unit unit){//true to build false to mine
        switch (currentStrategy){
            case NEUTRAL:
                if(unit.location().isOnPlanet(earth.getPlanet())){
                    if(roundNum > 200 && rocketsOnMars.isEmpty())return true;

                    if(myFactories.size() < myWorkers.size()/4){
                        //System.out.println("should build");
                        return true;
                    }

                    /*VecUnit workers = controller.senseNearbyUnitsByType(unit.location().mapLocation(),5,UnitType.Worker);
                    if(karbonite > 200 && workers.size() > 3){
                        return true;
                    }*/

                    if(karbonite < 200 ){
                        if(roundNum > 200 && karbonite >= bc.bcUnitTypeBlueprintCost(UnitType.Rocket))return true;

                        return false;
                    }
                    if(!workerShouldBuild(unit)){
                        return myFactories.size() <= myWorkers.size()/4;
                    }
                    return true;
                }
            case RUSH:
                break;
            case RANGED:
                break;
            case RETREAT:
                break;
        }
        return false;
    }
    public void setStrategy(){
        /*if(earth.getWidth() < 25 || earth.getHeight() < 25){
            currentStrategy = RUSH;
        }*/
        //if(controller.units().size() < 10){
            currentStrategy = NEUTRAL;
        //}
    }

    public void produceUnit(UnitType toProduce,Unit factory){
        if(controller.canProduceRobot(factory.id(),toProduce)){
            //if(karbonite >= bc.bcUnitTypeFactoryCost(toProduce)){
                controller.produceRobot(factory.id(),toProduce);
                karbonite = controller.karbonite();
                System.out.println(factory.id()+" produced a "+toProduce.name());
            //}
        }
    }
    public boolean blueprint(Unit unit,UnitType structure,MapLocation location){
        //if(isSafe(unit)){
            if(earth.onMap(location) || mars.onMap(location)){
                Direction dir =  unit.location().mapLocation().directionTo(location);

                if(controller.canBlueprint(unit.id(),structure,dir)){
                    MapLocation loc = unit.location().mapLocation().add(dir);

                    controller.blueprint(unit.id(),structure,dir);
                    karbonite = controller.karbonite();

                    if(workerBuilds.containsKey(unit.id())){
                        if(!workerBuilds.get(unit.id()).contains(loc)){
                            workerBuilds.get(unit.id()).offer(loc);
                            System.out.println("added to worker builds");
                        }
                    }else{
                        Queue<MapLocation>queue = new LinkedList<>();
                        workerBuilds.putIfAbsent(unit.id(),queue);
                        workerBuilds.get(unit.id()).offer(loc);
                    }
                    if(checkUnitExistence(location)){
                        unbuilt.offer(controller.senseUnitAtLocation(location).id());
                    }
                    System.out.println(unit.id()+" blueprinted at "+loc);
                    return true;
                }
            }
        //}
        return false;
    }

    public boolean harvest(Unit unit,MapLocation location){
        if(isSafe(unit)){
            Direction dir =  unit.location().mapLocation().directionTo(location);
            if(controller.canHarvest(unit.id(),dir)){
                controller.harvest(unit.id(),dir);
                karbonite = controller.karbonite();

                try{
                    if(location.getPlanet() == earth.getPlanet()){
                        if(earth.onMap(location)){
                            earthKarboniteLoc[location.getY()][location.getY()] = controller.karboniteAt(location);
                        }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    return true;
                }
                //System.out.println("trying to harvest");
                return true;
            }else{
                if(!claimedMines.contains(location)){
                    claimedMines.add(location);
                }
            }
        }
        return false;
    }
    public boolean heal(Unit unit,MapLocation location){
        if(checkUnitExistence(location)){
            Unit toHeal = controller.senseUnitAtLocation(location);
            if(controller.isHealReady(unit.id())){
                if(controller.canHeal(unit.id(),toHeal.id())){
                    controller.heal(unit.id(),toHeal.id());
                    //System.out.println("HEALER HEALED "+toHeal.id());
                    return true;
                }
            }
        }
        //System.out.println("HEALER HEAL FAILED");
        return false;
    }
}
