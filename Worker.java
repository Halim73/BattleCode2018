import bc.*;

import javax.rmi.CORBA.Util;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public strictfp class Worker {
    GameController controller;

    Unit unit;

    static final int NEUTRAL = 0;
    static final int RUSH = 1;
    static final int RETREAT = 2;
    static final int RANGED = 3;
    static final int HEAVY = 4;

    static boolean hasReplicated;
    static boolean hasSearchedBuilt;

    public Worker(GameController gc, Unit unit){
        controller = gc;
        this.unit = unit;

        hasReplicated = false;
        hasSearchedBuilt = false;

    }

    public void run(Utility utility){
        switch(utility.currentStrategy){
            case NEUTRAL:
                runNeutral(utility);
                break;
            case RUSH:
                runRush(utility);
                break;
            case HEAVY:
                runHeavy(utility);
                break;
        }
    }

    public void runNeutral(Utility utility){
        if(!unit.location().isInSpace() && !unit.location().isInGarrison()){
            init(utility);

            if(unit.location().isOnPlanet(utility.earth.getPlanet())){
                if(utility.roundNum <= 25){
                    if(!buildLogic(utility)){
                        if(!spontaneousReplicate(utility)){
                            if(!mineLogic(utility)){
                                utility.wander(unit);
                            }
                        }
                    }
                    return;
                }

                if(!utility.workerBuildOrMine(unit)){
                    if(!conservativeMine(utility)){
                        //if(!spontaneousReplicate(utility,10)){
                            utility.wander(unit);
                        //}
                    }
                }else{
                    if(!utility.bestRepair(unit,UnitType.Factory)){
                        if(!buildLogic(utility)){
                            if(!conservativeMine(utility)){
                                //if(!spontaneousReplicate(utility,15)){
                                    utility.wander(unit);
                                //}
                            }
                        }
                    }
                }
            }else{
                if(!mineLogic(utility)){
                    //if(!spontaneousReplicate(utility));
                    utility.wander(unit);
                }
            }
        }
    }
    public void runRush(Utility utility){
        if(!unit.location().isInSpace() && !unit.location().isInGarrison()){
            init(utility);

            if(utility.allyStartPos.size() > 1){
                boolean amIMiner = unit.id()%2 == 0;
                boolean amIAlone = (controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange()/2,utility.ally).size() < 10);

                if(!amIAlone){
                    VecUnit factories = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Factory);
                    if(factories.size() == 0){
                        if(!rushBuild(utility)){
                            greedyMine(utility);
                        }
                    }else{
                        if(!greedyMine(utility)){
                            if(!rushBuild(utility)){
                                utility.wander(unit);
                            }
                        }
                    }
                }else{
                    if(!rushBuild(utility)){
                        utility.wander(unit);
                    }
                }
            }else{
                if(!rushBuild(utility)){
                    //System.out.println("FAILED TO BLUEPRINT OR BUILD");
                    if(!greedyMine(utility)){
                        if(!spontaneousReplicate(utility,10)){
                            utility.wander(unit);
                        }
                    }
                }
            }
        }
    }
    public void runHeavy(Utility utility){
        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            init(utility);
            long cost = bc.bcUnitTypeBlueprintCost(UnitType.Factory);

            boolean handledUnbuilt;
            boolean costEffective = cost <= utility.karbonite;

            handledUnbuilt = handleUnbuilts(utility);

            if(!handledUnbuilt && costEffective){
                if(!turtleBuild(utility)){
                    conservativeMine(utility);
                }
            }else{
                greedyMine(utility);
            }
        }
    }

    public boolean buildLogic(Utility utility){
        VecUnit units = controller.senseNearbyUnitsByType(unit.location().mapLocation(),15,UnitType.Factory);
        VecUnit rockets = controller.senseNearbyUnitsByType(unit.location().mapLocation(),15,UnitType.Rocket);
        VecUnit nearbyWorkers = controller.senseNearbyUnitsByType(unit.location().mapLocation(),25,UnitType.Worker);

        if(utility.roundNum > 300){
            for(int i=0;i<rockets.size();i++){
                if(rockets.get(i).structureIsBuilt() != 0)continue;
                if(utility.build(unit,rockets.get(i).id())){
                    if(nearbyWorkers.size() < 2){
                        replicateNear(utility,rockets.get(i).location().mapLocation());
                    }
                    return true;
                }
            }

            for(int i=0;i<units.size();i++){
                if(units.get(i).structureIsBuilt() != 0)continue;
                if(utility.build(unit,units.get(i).id())){
                    if(nearbyWorkers.size() < 2){
                        replicateNear(utility,units.get(i).location().mapLocation());
                    }
                    return true;
                }
            }
        }else{
            for(int i=0;i<units.size();i++){
                if(units.get(i).structureIsBuilt() != 0)continue;
                if(utility.build(unit,units.get(i).id())){
                    if(nearbyWorkers.size() < 2){
                        replicateNear(utility,units.get(i).location().mapLocation());
                    }
                    return true;
                }
            }

            for(int i=0;i<rockets.size();i++){
                if(rockets.get(i).structureIsBuilt() != 0)continue;
                if(utility.build(unit,rockets.get(i).id())){
                    if(nearbyWorkers.size() < 2){
                        replicateNear(utility,rockets.get(i).location().mapLocation());
                    }
                    return true;
                }
            }
        }

        MapLocation spot = utility.getOpen(unit);

        if (spot != null) {
            if(utility.roundNum > 300){
                utility.blueprint(unit, UnitType.Rocket, spot);
            }else{
                if(utility.factories < utility.workers/2){
                    utility.blueprint(unit, UnitType.Factory, spot);
                }else{
                    if(utility.rocketsOnMars.size() > 4){
                        utility.blueprint(unit, UnitType.Factory, spot);
                    }else{
                        utility.blueprint(unit, UnitType.Rocket, spot);
                    }
                }
            }
        }
        return false;
    }
    public boolean rushBuild(Utility utility){
        VecUnit factories = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Factory);
        VecUnit rockets = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Rocket);
        VecUnit nearbyWorkers = controller.senseNearbyUnitsByType(unit.location().mapLocation(),25,UnitType.Worker);

        int myWorkers = 0;
        if(nearbyWorkers.size() > 0){
            for(int i=0;i<nearbyWorkers.size();i++){
                if(nearbyWorkers.get(i).team() == utility.ally)myWorkers++;
            }
        }

        MapLocation open = utility.getOpen(unit);

        if(utility.roundNum < 200){
            if(factories.size() > 0){
                for(int i=0;i<factories.size();i++){
                    if(factories.get(i).structureIsBuilt() != 0)continue;
                    if(utility.build(unit,factories.get(i).id())){
                        if(myWorkers < 3){
                            replicateNear(utility,factories.get(i).location().mapLocation());
                        }
                        return true;
                    }
                }
            }

            if(open != null){
                return utility.blueprint(unit,UnitType.Factory,open);
            }
        }else{
            if(rockets.size() > 0){
                for(int i=0;i<rockets.size();i++){
                    if(utility.build(unit,rockets.get(i).id())){
                        if(myWorkers < 3){
                            replicateNear(utility,factories.get(i).location().mapLocation());
                        }
                        return true;
                    }
                }
            }
            if(factories.size() > 0){
                for(int i=0;i<factories.size();i++){
                    if(utility.build(unit,factories.get(i).id())){
                        if(myWorkers < 3){
                            replicateNear(utility,factories.get(i).location().mapLocation());
                        }
                        return true;
                    }
                }
            }
            if(utility.roundNum > 320){
                return utility.blueprint(unit, UnitType.Rocket, open);
            }
            if(utility.factories <= utility.workers/4){
                return utility.blueprint(unit, UnitType.Factory, open);
            }else{
                if(utility.rocketsOnMars.size() > 6){
                    return utility.blueprint(unit, UnitType.Factory, open);
                }else{
                    return utility.blueprint(unit, UnitType.Rocket, open);
                }
            }
        }
        return false;
    }
    public boolean turtleBuild(Utility utility){
        VecUnit factories = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Factory);
        VecUnit rockets = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Rocket);
        VecUnit nearbyWorkers = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Worker);

        if(nearbyWorkers.size() > 6){
            Unit ally = utility.closesAlly(unit);
            if(ally != null){
                Direction away = unit.location().mapLocation().directionTo(ally.location().mapLocation());
                MapLocation spreadOut = unit.location().mapLocation().subtract(bc.bcDirectionRotateLeft(away));
                utility.move(unit,spreadOut);
            }
        }

        MapLocation open = utility.getOpen(unit);
        try{
            if(utility.turtleBase.isEmpty())utility.planTurtleBase(unit);

            if(!utility.turtleBase.isEmpty()){
                open = utility.getBaseLoc(unit);

                if(controller.hasUnitAtLocation(open)){
                    Unit toCheck = controller.senseUnitAtLocation(open);
                    if(toCheck.unitType() == UnitType.Factory){
                       return false;
                    }

                    System.out.println("HAVE A "+toCheck.unitType()+" AT BASE LOCATION");
                    if(unit.location().mapLocation().equals(open)){
                        return false;
                    }
                }else{
                    open = utility.getBaseLoc(unit);
                }
                utility.cleanTurtleBase();
            }
        }catch(UnknownError e){
            return false;
        }

        boolean hasSetNextBuild = false;

        if(utility.roundNum > 300 || rockets.size() > 0){
            if(rockets.size() > 0){
                for(int i=0;i<rockets.size();i++){
                    if(utility.build(unit,rockets.get(i).id())){
                        if(nearbyWorkers.size() < 3){
                            replicateNear(utility,factories.get(i).location().mapLocation());
                        }
                        return true;
                    }
                }
            }
            if(factories.size() > 0){
                for(int i=0;i<factories.size();i++){
                    if(utility.build(unit,factories.get(i).id())){
                        if(nearbyWorkers.size() < 3){
                            replicateNear(utility,factories.get(i).location().mapLocation());
                        }
                        return true;
                    }
                }
            }
        }

        if(factories.size() > 0){
            for(int i=0;i<factories.size();i++){
                if(factories.get(i).structureIsBuilt() != 0){
                    continue;
                }
                if(utility.build(unit,factories.get(i).id())){
                    if(nearbyWorkers.size() < 2){
                        replicateNear(utility,factories.get(i).location().mapLocation());
                    }
                    return true;
                }
            }
        }

        if(open == null){
            open = unit.location().mapLocation().add(utility.getRandomDirection());
        }

        if(utility.roundNum > 300 && utility.factories > 4){
            return utility.blueprint(unit,UnitType.Rocket,open) == false?utility.blueprint(unit,UnitType.Factory,open):true;
        }

        return utility.blueprint(unit,UnitType.Factory,open);
    }
    public boolean handleUnbuilts(Utility utility){
        VecUnit nearbyFactories = (controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Factory));
        VecUnit nearbyRockets = (controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Rocket));

        boolean handledUnbuilt = false;

        if(nearbyFactories.size() > 0){
            for(int i=0;i<nearbyFactories.size();i++){
                if(nearbyFactories.get(i).structureIsBuilt() != 0)continue;
                if(utility.unbuilt.contains(nearbyFactories.get(i).id())){
                    handledUnbuilt = turtleBuild(utility);
                    System.out.println("HANDLING UNBUILT FACTORIES");
                }
            }
        }

        if(nearbyRockets.size() > 0){
            for(int i=0;i<nearbyRockets.size();i++){
                if(nearbyRockets.get(i).structureIsBuilt() != 0)continue;
                if(utility.unbuilt.contains(nearbyRockets.get(i).id())){
                    handledUnbuilt = turtleBuild(utility);
                    System.out.println("HANDLING UNBUILT ROCKETS");
                }
            }
        }
        return handledUnbuilt;
    }

    public boolean mineLogic(Utility utility){
        if(unit.location().isOnPlanet(utility.earth.getPlanet())){
            if(utility.myWorkers.containsKey(unit.id())){
                if(utility.myWorkers.get(unit.id()).isEmpty()){
                    utility.myWorkers.replace(unit.id(),utility.getBestKarbonite(unit));
                }
            }else{
                utility.myWorkers.put(unit.id(),utility.getBestKarbonite(unit));
            }

            MapLocation toHarvest = utility.myWorkers.get(unit.id()).poll();

            if(toHarvest != null && unit.location().mapLocation().isWithinRange(unit.visionRange(),toHarvest)){
                if(!unit.location().mapLocation().isAdjacentTo(toHarvest) && !unit.location().mapLocation().equals(toHarvest)){
                    utility.move(unit,toHarvest);
                    //System.out.println("moving to harvest");
                }
                return utility.harvest(unit,toHarvest);
            }
        }else{
            MapLocation toHarvest = utility.findClosestGoal(unit,utility.marsGoals);
            if(utility.marsGoals.size() > 0){
                toHarvest = utility.marsGoals.get(0);
            }
            if(toHarvest != null && unit.location().mapLocation().isWithinRange(unit.visionRange(),toHarvest)){
                if(!unit.location().mapLocation().isAdjacentTo(toHarvest) && !unit.location().mapLocation().equals(toHarvest)){
                    utility.move(unit,toHarvest);
                    //System.out.println("moving to harvest");
                }
                if(utility.harvest(unit,toHarvest)){
                    utility.marsGoals.remove(toHarvest);
                    return true;
                }
            }
        }
        return false;
    }
    public boolean greedyMine(Utility utility){
        MapLocation toMine = utility.priorityMine(unit);
        boolean worthIt = unit.location().isOnPlanet(utility.earth.getPlanet())?utility.earthKarboniteLoc[toMine.getX()][toMine.getY()] > 0:utility.marsKarboniteLoc[toMine.getX()][toMine.getY()] > 0;
        if(worthIt){
            if(!unit.location().mapLocation().isAdjacentTo(toMine)){
                utility.move(unit,toMine);
            }
            if(!utility.harvest(unit,toMine)){
                return spontaneousReplicate(utility,5);
            }
            if(toMine.getPlanet() == utility.earth.getPlanet()){
                utility.earthKarboniteLoc[toMine.getX()][toMine.getY()] = controller.karboniteAt(toMine);
            }else{
                utility.marsKarboniteLoc[toMine.getX()][toMine.getY()] = controller.karboniteAt(toMine);
            }
        }else{
            if(!spontaneousReplicate(utility,5)){
                utility.wander(unit);
                return false;
            }
        }
        return true;
    }
    public boolean conservativeMine(Utility utility){
        MapLocation toMine = utility.priorityMine(unit);
        if(controller.hasUnitAtLocation(toMine)){
            return false;
        }

        boolean worthIt = unit.location().isOnPlanet(utility.earth.getPlanet())?utility.earthKarboniteLoc[toMine.getX()][toMine.getY()] > 0:utility.marsKarboniteLoc[toMine.getX()][toMine.getY()] > 0;
        if(worthIt){
            if(!unit.location().mapLocation().isAdjacentTo(toMine)){
                utility.move(unit,toMine);
            }
            if(!utility.harvest(unit,toMine)){
                return spontaneousReplicate(utility,20);
            }
        }else{
            if(!spontaneousReplicate(utility,30)){
                utility.wander(unit);
                return false;
            }
        }
        return true;
    }

    public void replicateNear(Utility utility,MapLocation location){
        VecMapLocation adj = controller.allLocationsWithin(location,9);
        for(int i=0;i<adj.size();i++){
            if(adj.get(i).isAdjacentTo(location)){
                Direction dir = unit.location().mapLocation().directionTo(adj.get(i));

                if(replicate(utility,dir))break;
            }
        }
    }
    public boolean replicate(Utility utility,Direction direction){
        //if(utility.isSafe(unit)){
            if(utility.karbonite >= bc.bcUnitTypeReplicateCost(unit.unitType())){
                int count = 0;
                while(!controller.canReplicate(unit.id(),direction)){
                    direction = utility.compass[utility.random.nextInt(utility.compass.length)];
                    if(count++ == 8){
                        break;
                    }
                }
                if(controller.canReplicate(unit.id(),direction)){
                    controller.replicate(unit.id(),direction);
                    utility.karbonite -= bc.bcUnitTypeReplicateCost(unit.unitType());

                    utility.workers++;
                    hasReplicated = true;
                    return true;
                }
            }
        //}
        return false;
    }
    public boolean spontaneousReplicate(Utility utility){
        boolean isStart = utility.roundNum < 25;

        boolean couldReplicateAgain = (utility.karbonite >= bc.bcUnitTypeReplicateCost(UnitType.Worker))?(utility.myFactories.size() > 0):true;
        boolean shouldReplicateAgain = (utility.factories > utility.workers/2);

        if(utility.workers > 15)return false;

        if(unit.location().isOnPlanet(utility.earth.getPlanet())){
            if(isStart || (couldReplicateAgain && shouldReplicateAgain)){
                MapLocation loc = utility.getOpen(unit);

                if(loc != null){
                    if(!replicate(utility,unit.location().mapLocation().directionTo(loc))){
                        //utility.wander(unit);
                    }else{
                        return true;
                    }
                }
            }
        }else{
            MapLocation loc = utility.getOpen(unit);

            if(loc != null){
                if(!replicate(utility,unit.location().mapLocation().directionTo(loc))){
                    //utility.wander(unit);
                }else{
                    return true;
                }
            }
        }

        return false;
    }
    public boolean spontaneousReplicate(Utility utility,int limiter){
        boolean isStart = utility.roundNum < 25;
        VecUnit nearbyWorkers = controller.senseNearbyUnitsByType(unit.location().mapLocation(),16,UnitType.Worker);

        boolean couldReplicateAgain = (utility.karbonite >= limiter*bc.bcUnitTypeReplicateCost(UnitType.Worker))?(utility.myFactories.size() > 0):true;
        //boolean shouldReplicateAgain = (utility.factories > utility.workers/2);

        if(utility.workers > 15)return false;
        if(nearbyWorkers.size() > 4)return false;
        if(utility.factories < utility.workers/3)return false;

        if(unit.location().isOnPlanet(utility.earth.getPlanet())){
            if(isStart || (couldReplicateAgain /*&& shouldReplicateAgain*/)){
                MapLocation loc = utility.getOpen(unit);

                if(loc != null){
                    return replicate(utility,unit.location().mapLocation().directionTo(loc));
                }
            }
        }else{
            MapLocation loc = utility.getOpen(unit);

            if(loc != null){
                return replicate(utility,unit.location().mapLocation().directionTo(loc));
            }
        }

        return false;
    }

    public void init(Utility utility){
        if(unit.location().isInGarrison())return;
        if(unit.health() <= 0)return;

        if(unit.health() < unit.maxHealth() && unit.health() > unit.maxHealth()/3){
            float priority = utility.healDesire(unit,utility.myHealers.size());
            if(utility.healRequests.containsKey(unit.id())){
                utility.healRequests.replace(unit.id(),priority);
            }else{
                utility.healRequests.put(unit.id(),priority);
            }
        }else{
            if(utility.healRequests.containsKey(unit.id())){
                utility.healRequests.remove(unit.id());
            }
            if(utility.myWorkers.containsKey(unit.id())){
                utility.myWorkers.remove(unit.id());
            }
        }

        if(!utility.isSafe(unit)){
            utility.tagEnemies(unit);
            Unit enemy = utility.closestEnemy(unit);
            if(enemy != null){
                utility.dodge(unit,enemy);
                utility.goals.add(enemy.location().mapLocation());
            }
        }

        if(!utility.elapsedTime.containsKey(unit.id())){
            utility.elapsedTime.put(unit.id(),utility.roundNum);
        }
    }
}
