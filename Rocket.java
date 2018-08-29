import bc.*;
public class Rocket {
    GameController controller;
    Unit unit;

    public Rocket(GameController gc,Unit unit){
        controller = gc;
        this.unit = unit;
    }

    public void run(Utility utility){
        try{
            if(unit.location().isOnPlanet(utility.earth.getPlanet())){
                tryToLaunch(utility);
            }else{
                if(!unit.location().isInSpace()){
                    if(!utility.rocketsOnMars.contains(unit.id())){
                        utility.rocketsOnMars.add(unit.id());
                    }

                    if(unit.structureGarrison().size() == 0){
                        controller.disintegrateUnit(unit.id());
                        return;
                    }

                    try{
                        MapLocation aux = utility.getOpen(unit);

                        if (unit == null) return;
                        Direction direction = unit.location().mapLocation().directionTo(aux);
                        if(direction == null){
                            direction = utility.compass[utility.random.nextInt(utility.compass.length)];
                        }

                        if(controller.canUnload(unit.id(),direction)){
                            controller.unload(unit.id(),direction);
                        }
                    }catch(Exception | UnknownError e){
                        System.out.println("problems unloading");
                    }
                }
            }
        }catch(UnknownError | Exception e){
            return;
        }
    }

    public void tryToLaunch(Utility utility){
        VecUnit nearbyAllies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),6,utility.ally);
        if(unit.structureGarrison().size() == unit.structureMaxCapacity()){
            utility.myRockets.replace(unit.id(),true);
        }
        if(nearbyAllies.size() > 0){
            for(int i=0;i<nearbyAllies.size();i++){
                Unit ally = nearbyAllies.get(i);
                if(controller.canLoad(unit.id(),ally.id())){
                    controller.load(unit.id(),ally.id());
                    return;
                }
                //System.out.println("FAILED TO LOAD "+ally.id()+" UNIT SIZE IS "+unit.structureGarrison().size());
            }
        }

        if(!utility.isSafe(unit) || utility.roundNum > 325){
            if(unit.structureGarrison().size() > 0){
                MapLocation loc = utility.goodMarsLanding(unit);
                if(controller.canLaunchRocket(unit.id(),loc)){
                    controller.launchRocket(unit.id(),loc);
                    utility.myRockets.remove(unit.id());
                }
            }
        }else if(unit.structureGarrison().size() >= 3){
            MapLocation loc = utility.goodMarsLanding(unit);
            if(controller.canLaunchRocket(unit.id(),loc)){
                controller.launchRocket(unit.id(),loc);
                utility.myRockets.remove(unit.id());
            }
        }
    }
}
