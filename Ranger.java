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
        try{
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
        }catch(Exception | UnknownError e){
            return;
        }
    }

    public void runRush(Utility utility){
        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            init(utility);

            VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.opp);
            VecUnit allies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.ally);

            if(allies.size() > 10)utility.spreadOut(unit);

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
            VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.opp);
            VecUnit allies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange()/2,utility.ally);

            if(allies.size() > 10)utility.spreadOut(unit);

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
    public void runHeavy(Utility utility){
        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){
            init(utility);

            VecUnit units = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.opp);
            VecUnit allies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.ally);

            if(allies.size() > 10)utility.spreadOut(unit);

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
                        int numAllies = 0;

                        MapLocation waitingZone = unit.location().mapLocation();
                        Unit squadMate = utility.closesAlly(unit);
                        if(squadMate != null){
                            waitingZone = squadMate.location().mapLocation();
                        }

                        for(int i=0;i<allies.size();i++){
                            Unit ally = allies.get(i);
                            if(ally.unitType() == UnitType.Factory){
                                waitingZone = ally.location().mapLocation();
                            }
                            if(ally.unitType() == UnitType.Worker || ally.unitType() == UnitType.Factory || ally.unitType() == UnitType.Rocket)continue;
                            numAllies++;
                        }

                        if(numAllies < 1){
                            Direction toWait = bc.bcDirectionRotateLeft(waitingZone.directionTo(unit.location().mapLocation()));
                            waitingZone.addMultiple(toWait,9);
                            if(!utility.move(unit,waitingZone)){

                            }
                        }else{
                            if(utility.move(unit,toGo)){
                                if(unit.location().mapLocation().isWithinRange(20,toGo)){
                                    if(utility.attackVectors.get(unit.id()).contains(toGo)){
                                        utility.attackVectors.get(unit.id()).remove();
                                    }
                                }
                            }
                        }
                    }
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

            Unit healer = utility.closestAlly(unit,UnitType.Healer);
            if(healer != null){
                utility.move(unit,healer.location().mapLocation());
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
        //System.out.println("RANGER TRYING TO FIGHT");
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
                //System.out.println("RANGER ATTACKED");
            }
        }
    }

    public void microMove(Unit enemy,Utility utility){
        long distance = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());

        Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
        MapLocation loc = unit.location().mapLocation();

        if(utility.currentStrategy == NEUTRAL){
            if(distance <= unit.rangerCannotAttackRange()){
                loc.addMultiple(bc.bcDirectionOpposite(bc.bcDirectionRotateLeft(dir)),2);
            }else{
                if(distance > unit.attackRange()-2){
                    loc.addMultiple(bc.bcDirectionRotateRight(dir),2);
                }else{
                    Unit ally = utility.closesAlly(unit);
                    if(ally != null){
                        loc.addMultiple(ally.location().mapLocation().directionTo(unit.location().mapLocation()),3);
                        //loc = loc.subtract(utility.compass[utility.random.nextInt(utility.compass.length-1)]);
                    }
                }
            }
        }else if(utility.currentStrategy == RUSH || utility.currentStrategy == HEAVY){
            long distanceToEnemy = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());

            if(distanceToEnemy > unit.attackRange()){
                loc = enemy.location().mapLocation();

            } else if(distanceToEnemy < unit.attackRange()){
                Direction direction = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
                loc = unit.location().mapLocation().subtract(bc.bcDirectionRotateRight(direction));
            }
        }

        if(!utility.move(unit,loc)){
            utility.wander(unit);
        }
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
                MapLocation goal = utility.findClosestGoal(unit,utility.attackVectors.get(unit.id()));

                if (unit.location().mapLocation().isWithinRange(unit.visionRange(), goal)) {
                    utility.attackVectors.get(unit.id()).remove(goal);
                    utility.elapsedTime.replace(unit.id(), utility.roundNum);
                } else {
                    if (elapsed > 200) {
                        utility.attackVectors.get(unit.id()).remove();
                        utility.elapsedTime.replace(unit.id(), utility.roundNum);
                        System.out.println("REASSIGNING VECTOR");
                    }
                    try {
                        if(!utility.move(unit,goal)){
                            utility.wander(unit);
                        }
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
}
