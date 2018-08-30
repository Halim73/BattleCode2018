import bc.*;
public class Factory {
    GameController controller;
    Unit unit;

    static final int NEUTRAL = 0;
    static final int RUSH = 1;
    static final int RETREAT = 2;
    static final int RANGED = 3;
    static final int HEAVY = 4;

    float[]priority;//0=KNIGHT,1=HEALER,2=MAGE,3=RANGER

    public Factory(GameController gc,Unit unit){
        controller = gc;
        this.unit = unit;
        priority = new float[4];
        for(int i=0;i<priority.length;i++){
            priority[i] = 1;
        }
    }

    public void run(Utility utility){
        //if(utility.currentStrategy == NEUTRAL){
            runNeutral(utility);
        //}
    }

    public void runNeutral(Utility utility){
        if(init(utility)){
            unload(utility);
        }
        //unload(utility);
    }
    public boolean init(Utility utility){
        if(unit.structureIsBuilt() == 0){
            System.out.println(unit.id()+" not built? "+unit.health());
            if(!utility.unbuilt.contains(unit.id())){
                utility.unbuilt.add(unit.id());
                utility.myFactories.put(unit.id(),false);
                return false;
            }
        }else{
            //System.out.println(unit.id() + " is built " + unit.health());
            if(unit.health() <= unit.maxHealth()/4){
                utility.myFactories.remove(unit.id());
            }
            if (utility.myFactories.containsKey(unit.id())) {
                utility.myFactories.replace(unit.id(), true);
            } else {
                utility.myFactories.put(unit.id(), true);
            }
            utility.unbuilt.remove(unit.location());
        }
        return true;
    }
    public void unload(Utility utility){
        VecUnitID units = unit.structureGarrison();
        //System.out.println(unit.id() + " number has " + units.size() + " number of units in garrison");

        if(units.size() > 0){
                try{
                    //MapLocation aux = utility.getOpen(unit);
                    MapLocation aux = utility.goals.get(utility.random.nextInt(utility.goals.size()));
                    if (unit == null) return;
                    Direction direction = unit.location().mapLocation().directionTo(aux);
                    if(direction == null){
                        direction = utility.compass[utility.random.nextInt(utility.compass.length)];
                    }

                    if(controller.canUnload(unit.id(),direction)){
                        controller.unload(unit.id(),direction);
                    }
                }catch(Exception | UnknownError e){
                    //System.out.println("problems unloading");
                }
        }else{
            if(utility.buildOrders.get(unit.id()).isEmpty()){
                //System.out.println(unit.id()+" trying to produce unit");
                if(utility.currentStrategy == NEUTRAL){
                    determineBuild(utility);
                }else if(utility.currentStrategy == RUSH){
                    utility.buildOrders.get(unit.id()).offer(UnitType.Mage);
                    utility.buildOrders.get(unit.id()).offer(UnitType.Ranger);
                    if(utility.myRangers.size() > 2 || utility.myMages.size() > 2){
                        utility.buildOrders.get(unit.id()).offer(UnitType.Healer);
                    }
                    //utility.buildOrders.get(unit.id()).offer(UnitType.Knight);
                    //utility.buildOrders.get(unit.id()).offer(UnitType.Ranger);
                    //utility.produceUnit(UnitType.Ranger,unit);
                }
            }
            utility.produceUnit(utility.buildOrders.get(unit.id()).remove(),unit);
        }
    }

    public void determineBuild(Utility utility){
        final int KNIGHT = 0;
        final int HEALER = 1;
        final int MAGE = 2;
        final int RANGER = 3;

        if(utility.buildOrders.containsKey(unit.id())){
            if(utility.myWorkers.size() == 0){
                utility.buildOrders.get(unit.id()).offer(UnitType.Worker);
                utility.buildOrders.get(unit.id()).offer(UnitType.Worker);
            }

            //while(utility.buildOrders.get(unit.id()).size() < 5){
                priority[KNIGHT] = utility.calculateKnightPriority();
                priority[HEALER] = utility.calculateHealPriority();
                priority[MAGE] = utility.calculateMagePriority();
                priority[RANGER] = utility.calculateRangerPriority();

                utility.normalize(priority);
                switch (utility.highestIndex(priority)){
                    case KNIGHT:
                        utility.buildOrders.get(unit.id()).offer(UnitType.Knight);
                        break;
                    case HEALER:
                        utility.buildOrders.get(unit.id()).offer(UnitType.Healer);
                        break;
                    case MAGE:
                        utility.buildOrders.get(unit.id()).offer(UnitType.Mage);
                        break;
                    case RANGER:
                        utility.buildOrders.get(unit.id()).offer(UnitType.Ranger);
                        break;
                }
            //}
        }
    }
}
