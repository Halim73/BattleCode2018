import bc.*;

public class Ranger {
    Unit unit;
    GameController controller;

    static final int NEUTRAL = 0;
    static final int RUSH = 1;
    static final int RETREAT = 2;
    static final int RANGED = 3;
    static final int HEAVY = 4;

    public Ranger(GameController gc, Unit unit){
        this.unit = unit;
        controller = gc;
    }
    public void run(Utility utility){
        if(utility.currentStrategy == NEUTRAL){
            runNeutral(utility);
        }
    }
    public void runNeutral(Utility utility){
        init(utility);

        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.opp);
            if(units.size() > 0){
                utility.tagEnemies(unit);
                Unit opp = utility.closestEnemy(unit);

                if(opp != null){
                    fight(opp,utility);
                }
            }else{
                if(unit.location().isOnPlanet(utility.earth.getPlanet())){
                    findNextMove(utility);
                }else{
                    utility.wander(unit);
                }
            }
        }
    }
    public void init(Utility utility) {
        if (unit.health() < unit.maxHealth() && unit.health() > unit.maxHealth() / 3) {
            float priority = utility.healDesire(unit, utility.myHealers.size());
            if (utility.healRequests.containsKey(unit.id())) {
                utility.healRequests.replace(unit.id(), priority);
            } else {
                utility.healRequests.putIfAbsent(unit.id(), priority);
            }
        } else {
            if (utility.healRequests.containsKey(unit.id())) {
                utility.healRequests.remove(unit.id());
            }
            if (utility.myRangers.containsKey(unit.id())) {
                utility.myRangers.remove(unit.id());
            }
        }
    }

    public void fight(Unit enemy,Utility utility){
        System.out.println("RANGER TRYING TO FIGHT");

        microMove(enemy,utility);

        if(unit.isAbilityUnlocked() == 0){
            if(unit.abilityHeat() < 10 && controller.isBeginSnipeReady(unit.id())){
                if(controller.canBeginSnipe(unit.id(),enemy.location().mapLocation())){
                    controller.beginSnipe(unit.id(),enemy.location().mapLocation());
                }
            }
        }
        if(controller.isAttackReady(unit.id())){
            if(controller.canAttack(unit.id(),enemy.id()) && unit.attackHeat() < 10){
                controller.attack(unit.id(),enemy.id());
                System.out.println("RANGER ATTACKED");
            }
        }
    }

    public void microMove(Unit enemy,Utility utility){
        long distance = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());

        Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
        MapLocation loc = unit.location().mapLocation();

        if(distance <= unit.rangerCannotAttackRange()+2){
            loc.addMultiple(bc.bcDirectionOpposite(bc.bcDirectionRotateLeft(dir)),2);
        }else{
            if(distance > unit.attackRange()){
                loc.addMultiple(bc.bcDirectionRotateRight(dir),2);
            }
        }

        utility.move(unit,loc);
    }
    public void findNextMove(Utility utility){
        if(utility.attackVectors.containsKey(unit.id())) {
            VecUnit rockets = controller.senseNearbyUnitsByType(unit.location().mapLocation(),unit.visionRange(),UnitType.Rocket);
            if(rockets.size() > 0){
                if(rockets.get(0).structureGarrison().size() != rockets.get(0).structureMaxCapacity()){
                    MapLocation away = rockets.get(0).location().mapLocation();
                    utility.move(unit,away);
                }else{
                    if(rockets.get(0).rocketIsUsed() != 0){
                        utility.move(unit,rockets.get(0).location().mapLocation());
                    }
                }
                return;
            }
            if (!utility.attackVectors.get(unit.id()).isEmpty()) {
                long elapsed = 0;
                if (utility.elapsedTime.containsKey(unit.id())) {
                    elapsed = utility.roundNum - utility.elapsedTime.get(unit.id());
                    //System.out.println("RANGER ELAPSED TIME IS "+elapsed);
                } else {
                    utility.elapsedTime.put(unit.id(), utility.roundNum);
                }

                if (unit.location().mapLocation().isWithinRange(unit.visionRange(), utility.attackVectors.get(unit.id()).peek())) {
                    utility.attackVectors.get(unit.id()).remove();
                    utility.elapsedTime.replace(unit.id(), utility.roundNum);
                } else {
                    if (elapsed > 200) {
                        utility.attackVectors.get(unit.id()).remove();
                        utility.elapsedTime.replace(unit.id(), utility.roundNum);
                        System.out.println("REASSIGNING VECTOR");
                    }
                    try {
                        MapLocation goal = utility.findClosestGoal(unit,utility.attackVectors.get(unit.id()));
                        utility.move(unit, utility.attackVectors.get(unit.id()).peek());

                    } catch (Exception | UnknownError e) {
                        return;
                    }
                }
            } else {
                for (int i = 0; i < utility.goals.size(); i++) {
                    utility.attackVectors.get(unit.id()).offer(utility.goals.get(i));
                }
                utility.elapsedTime.put(unit.id(), utility.roundNum);
            }
        }
    }
}
