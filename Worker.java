import bc.*;

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
        if(utility.currentStrategy == NEUTRAL){
            runNeutral(utility);
        }
    }
    public void runNeutral(Utility utility){
        if(!unit.location().isInSpace() && !unit.location().isInGarrison()){
            init(utility);

            if(!utility.workerBuildOrMine(unit)){
                if(!mineLogic(utility)){
                    utility.wander(unit);
                }
            }else{
                if(!utility.bestRepair(unit,UnitType.Factory)){
                    if(!buildLogic(utility)){
                        if(!mineLogic(utility)){
                            utility.wander(unit);
                        }
                    }
                }
            }
        }
    }

    public boolean buildLogic(Utility utility){
        MapLocation spot = utility.getOpen(unit);

        if (spot != null) {
            if(utility.roundNum > 200){
                utility.blueprint(unit, UnitType.Rocket, spot);
            }else{
                utility.blueprint(unit, UnitType.Factory, spot);
            }
        }

        VecUnit units = controller.senseNearbyUnitsByType(unit.location().mapLocation(),2,UnitType.Factory);
        VecUnit rockets = controller.senseNearbyUnitsByType(unit.location().mapLocation(),2,UnitType.Rocket);

        if(utility.roundNum > 200){
            for(int i=0;i<rockets.size();i++){
                if(utility.build(unit,rockets.get(i).id())){
                    replicateNear(utility,rockets.get(i).location().mapLocation());
                    return true;
                }
            }

            for(int i=0;i<units.size();i++){
                if(utility.build(unit,units.get(i).id())){
                    replicateNear(utility,units.get(i).location().mapLocation());
                    return true;
                }
            }
        }else{
            for(int i=0;i<units.size();i++){
                if(utility.build(unit,units.get(i).id())){
                    replicateNear(utility,units.get(i).location().mapLocation());
                    return true;
                }
            }

            for(int i=0;i<rockets.size();i++){
                if(utility.build(unit,rockets.get(i).id())){
                    replicateNear(utility,rockets.get(i).location().mapLocation());
                    return true;
                }
            }
        }

        return false;
    }
    public boolean mineLogic(Utility utility){
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
        return false;
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

    public void init(Utility utility){
        if(unit.location().isInGarrison())return;
        if(unit.health() <= 0)return;

        if(unit.health() <= unit.maxHealth()/4){
            utility.myWorkers.remove(unit.id());
        }

        if(!utility.isSafe(unit)){
            utility.tagEnemies(unit);
            Unit enemy = utility.closestEnemy(unit);
            utility.dodge(unit,enemy);
            utility.goals.add(enemy.location().mapLocation());
            //return;
        }
        if(/*utility.roundNum > 0 && utility.roundNum < 200 &&*/ utility.random.nextFloat() < .1){
            //if(utility.factories == 0)return;

            boolean condition1 = (utility.roundNum > 50)?(utility.myFactories.size() > 0):true;
            //boolean condition2 = utility.karbonite > bc.bcUnitTypeReplicateCost(UnitType.Worker);
            if(utility.workers < 4){
                MapLocation loc = utility.getOpen(unit);

                if(loc != null){
                    if(!replicate(utility,unit.location().mapLocation().directionTo(loc))){
                        //utility.wander(unit);
                    }
                }
            }
        }
    }
}
