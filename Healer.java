import bc.*;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Healer {
    GameController controller;
    Unit unit;

    static final int NEUTRAL = 0;
    static final int RUSH = 1;
    static final int RETREAT = 2;
    static final int RANGED = 3;
    static final int HEAVY = 4;

    public Healer(GameController gc,Unit unit){
        this.controller = gc;
        this.unit = unit;
    }

    public void run(Utility utility){
        if(utility.currentStrategy == NEUTRAL){
            runNeutral(utility);
        }
    }

    public void runNeutral(Utility utility){
        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            if(utility.myHealers.get(unit.id()).isEmpty()){
                utility.enumerateHeals(unit);
                utility.enumerateOvercharges(unit);
            }

            //if(!vectorHeal(utility)){
                if(!generalHeal(utility)){
                    if(!utility.move(unit,utility.goals.getFirst())){
                        utility.wander(unit);
                    }
                }
            //}
        }
    }

    public boolean generalHeal(Utility utility){
        if(utility.myHealers.containsKey(unit.id())){
            if(!utility.myHealers.get(unit.id()).isEmpty()) {
                MapLocation toMove = utility.myHealers.get(unit.id()).peek();

                if (toMove != null) {
                    if (utility.heal(unit, toMove)) {
                        utility.myHealers.get(unit.id()).remove();
                        System.out.println("HEALER found nearby heal");
                    } else {
                        if(!utility.move(unit, toMove)){
                            return false;
                        }
                    }
                    return true;
                }
            }
        }else{
            Queue<MapLocation> toHeal = new LinkedList<>();
            utility.myHealers.putIfAbsent(unit.id(),toHeal);
        }
        return false;
    }
    public boolean vectorHeal(Utility utility) {
        if (utility.healVectors.containsKey(unit.id())) {
            MapLocation bestHeal = utility.bestHealLocation();

            if (bestHeal != null) {
                utility.healVectors.get(unit.id()).offer(bestHeal);
                System.out.println("HEALER found best heal");
            }
            MapLocation toMove = utility.healVectors.get(unit.id()).peek();

            if (toMove != null) {
                if (utility.heal(unit, toMove)) {
                    utility.healVectors.get(unit.id()).remove();
                    System.out.println("HEALER moving toward best heal");
                } else {
                    if(!utility.move(unit, toMove)){
                        return false;
                    }
                }
                return true;
            }
        }else{
            Queue<MapLocation> toHeal = new LinkedList<>();
            utility.healVectors.putIfAbsent(unit.id(),toHeal);
        }
        return false;
    }
}
