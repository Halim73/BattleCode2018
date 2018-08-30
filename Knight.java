import bc.*;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Stack;

public strictfp class Knight {
    GameController controller;
    Unit unit;
    Stack<MapLocation> basePath;
    PriorityQueue<Unit>priorityOpps;

    static final int NEUTRAL = 0;
    static final int RUSH = 1;
    static final int RETREAT = 2;
    static final int RANGED = 3;
    static final int HEAVY = 4;

    boolean hasPathed = false;
    public Knight(GameController gc, Unit unit){
        controller = gc;
        this.unit = unit;
        basePath = new Stack<>();
        priorityOpps = new PriorityQueue<>();
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
                break;
        }
    }

    public void runRush(Utility utility){
        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            init(utility);

            VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.opp);
            if(units.size() > 0){
                utility.tagEnemies(unit);
                Unit opp = utility.closestEnemy(unit);

                if(opp != null){
                    fight(opp,utility);
                }
            }else{
                if(unit.location().isOnPlanet(utility.earth.getPlanet())){
                    if(utility.attackVectors.containsKey(unit.id())){
                        MapLocation toGo = utility.attackVectors.get(unit.id()).peek();

                        if(utility.move(unit,toGo)){
                            if(unit.location().mapLocation().isWithinRange(20,toGo)){
                                utility.attackVectors.get(unit.id()).remove();
                            }
                        }else{
                            utility.wander(unit);
                        }
                    }
                }else{
                    utility.wander(unit);
                }
            }
        }
    }
    public void runNeutral(Utility utility){
        init(utility);

        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            if(utility.myKnights.containsKey(unit.id()) && utility.myKnights.get(unit.id()).isEmpty()){
                utility.enumerateEnemies(unit);
                //System.out.println("there are "+utility.myKnights.get(unit.id()).size()+" enemies in sight");
            }
            if(!utility.isSafe(unit)){
                utility.tagEnemies(unit);
                Unit opp = utility.closestEnemy(unit);
                fight(opp,utility);
            }else{
                if(unit.location().isOnPlanet(utility.earth.getPlanet())){
                    findNextMove(utility);
                }else{
                    utility.wander(unit);
                }
            }
        }
    }
    public void init(Utility utility){
        if(unit.health() < unit.maxHealth() && unit.health() > unit.maxHealth()/3){
            float priority = utility.healDesire(unit,utility.myHealers.size());
            if(utility.healRequests.containsKey(unit.id())){
                utility.healRequests.replace(unit.id(),priority);
            }else{
                utility.healRequests.putIfAbsent(unit.id(),priority);
            }
        }else{
            if(utility.healRequests.containsKey(unit.id())){
                utility.healRequests.remove(unit.id());
            }
            if(utility.myKnights.containsKey(unit.id())){
                utility.myKnights.remove(unit.id());
            }
        }
    }
    public boolean fight(Unit enemy,Utility utility){
        if(unit.location().mapLocation().isWithinRange(unit.attackRange(),enemy.location().mapLocation())){
            if(!javelin(enemy)){
                if(!melee(enemy)){
                    utility.move(unit,enemy.location().mapLocation());
                }
            }
        }else{
            utility.move(unit,enemy.location().mapLocation());
        }
        return false;
    }
    public boolean javelin(Unit enemy){
        if(controller.researchInfo().getLevel(unit.unitType()) == 0){
            return false;
        }
        if(controller.canAttack(unit.id(),enemy.id())){
            if(controller.isJavelinReady(unit.id()) && controller.canJavelin(unit.id(),enemy.id())){
                controller.javelin(unit.id(),enemy.id());
                return true;
            }
        }
        return false;
    }
    public void findNextMove(Utility utility){
        VecUnit rockets = controller.senseNearbyUnitsByType(unit.location().mapLocation(),unit.visionRange(),UnitType.Rocket);
        if(rockets.size() > 0){
            if(rockets.get(0).structureGarrison().size() != rockets.get(0).structureMaxCapacity()){
                MapLocation away = rockets.get(0).location().mapLocation();
                utility.move(unit,away);
            }else{
                utility.move(unit,rockets.get(0).location().mapLocation());
            }
            return;
        }
        if(utility.attackVectors.containsKey(unit.id())) {
            if (!utility.attackVectors.get(unit.id()).isEmpty()) {
                long elapsed = 0;
                if (utility.elapsedTime.containsKey(unit.id())) {
                    elapsed = utility.roundNum - utility.elapsedTime.get(unit.id());
                    //System.out.println("KNIGHTS ELAPSED TIME IS "+elapsed);
                } else {
                    utility.elapsedTime.put(unit.id(), utility.roundNum);
                }

                if (unit.location().mapLocation().isWithinRange(unit.visionRange()/2, utility.attackVectors.get(unit.id()).peek())) {
                    utility.attackVectors.get(unit.id()).remove();
                    utility.elapsedTime.replace(unit.id(), utility.roundNum);
                } else {
                    if (elapsed > 200) {
                        utility.attackVectors.get(unit.id()).remove();
                        utility.elapsedTime.replace(unit.id(), utility.roundNum);
                        System.out.println("REASSIGNING VECTOR");
                    }
                    try {
                        utility.move(unit, utility.attackVectors.get(unit.id()).peek());
                    } catch (Exception | UnknownError e) {
                        return;
                    }
                }
            } else {
                utility.attackVectors.get(unit.id()).addAll(utility.goals);
                utility.elapsedTime.put(unit.id(), utility.roundNum);
            }
        }
    }
    public boolean melee(Unit enemy){
        if(controller.isAttackReady(unit.id())){
            if(controller.canAttack(unit.id(),enemy.id())){
                if(enemy.location().mapLocation().isWithinRange(unit.attackRange(),unit.location().mapLocation())){
                    controller.attack(unit.id(),enemy.id());
                    return true;
                }
            }
        }
        return false;
    }
}
