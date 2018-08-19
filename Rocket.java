import bc.*;
public class Rocket {
    GameController controller;
    Unit unit;

    public Rocket(GameController gc,Unit unit){
        controller = gc;
        this.unit = unit;
    }

    public void run(Utility utility){
        if(unit.location().mapLocation().getPlanet() == utility.earth.getPlanet()){
            tryToLaunch(utility);
        }else{
            if(!unit.location().isInSpace()){
                if(unit.structureGarrison().size() > 0){
                    for(int i=0;i<unit.structureGarrison().size();i++){
                        if(controller.canLoad(unit.id(),unit.structureGarrison().get(i))){
                            MapLocation open = utility.getOpen(unit);
                            Direction toOpen = unit.location().mapLocation().directionTo(open);
                            controller.unload(unit.id(),toOpen);
                        }
                    }
                }
            }
        }
    }

    public void tryToLaunch(Utility utility){
        VecUnit nearbyAllies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),5,utility.ally);
        if(unit.structureGarrison().size() == unit.structureMaxCapacity()){
            utility.myRockets.replace(unit.id(),true);
        }
        if(nearbyAllies.size() > 0){
            for(int i=0;i<nearbyAllies.size();i++){
                if(unit.structureGarrison().size() == unit.structureMaxCapacity())break;

                Unit ally = nearbyAllies.get(i);
                if(controller.canLoad(unit.id(),ally.id())){
                    controller.load(unit.id(),ally.id());
                }
            }

        }
        if(utility.roundNum > 400){
            if(unit.structureGarrison().size() > 0){
                MapLocation loc = utility.goodMarsLanding(unit);
                if(controller.canLaunchRocket(unit.id(),loc)){
                    controller.launchRocket(unit.id(),loc);
                }
            }
        }else if(unit.structureGarrison().size() == unit.structureMaxCapacity()){
            MapLocation loc = utility.goodMarsLanding(unit);
            if(controller.canLaunchRocket(unit.id(),loc)){
                controller.launchRocket(unit.id(),loc);
            }
        }
    }
}
