import bc.*;
public strictfp class Mage {
    Unit unit;
    GameController controller;

    static final int NEUTRAL = 0;
    static final int RUSH = 1;
    static final int RETREAT = 2;
    static final int RANGED = 3;
    static final int HEAVY = 4;

    public Mage(GameController gc, Unit unit){
        this.unit = unit;
        controller = gc;
    }
    public void run(Utility utility){
        if(!unit.location().isInSpace() && !unit.location().isInGarrison()){
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
        }
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
        }catch(UnknownError e){
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
                        Unit enemy = utility.closestEnemy(unit);
                        if(enemy != null){
                            toGo = enemy.location().mapLocation();
                        }

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
            VecUnit allies = controller.senseNearbyUnitsByTeam(unit.location().mapLocation(),unit.visionRange(),utility.ally);

            if(allies.size() > 10)utility.spreadOut(unit);

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
                        for(int i=0;i<allies.size();i++){
                            Unit ally = allies.get(i);
                            if(ally.unitType() == UnitType.Factory){
                                waitingZone = ally.location().mapLocation();
                            }
                            if(ally.unitType() == UnitType.Worker || ally.unitType() == UnitType.Factory || ally.unitType() == UnitType.Rocket)continue;
                            numAllies++;
                        }

                        if(numAllies < 2){
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
                            }else{
                                //utility.wander(unit);
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
            if (utility.myMages.containsKey(unit.id())) {
                utility.myMages.remove(unit.id());
            }
        }
    }

    public void microMove(Unit enemy,Utility utility){
        long distance = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());

        Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
        MapLocation loc = unit.location().mapLocation();

        if(utility.currentStrategy == NEUTRAL){
            if(distance <= enemy.attackRange()/2){
                loc.addMultiple(bc.bcDirectionOpposite(bc.bcDirectionRotateLeft(dir)),2);
            }else{
                if(distance > unit.attackRange()-2){
                    loc.addMultiple(bc.bcDirectionRotateRight(dir),2);
                }else{
                    loc.addMultiple(utility.compass[utility.random.nextInt(utility.compass.length)],2);
                }
            }
        }else if(utility.currentStrategy == RUSH || utility.currentStrategy == HEAVY){
            long distanceToEnemy = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());

            if(distanceToEnemy > unit.attackRange()){
                loc = enemy.location().mapLocation();

            } else if(distanceToEnemy < unit.attackRange()){
                Direction direction = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
                loc = unit.location().mapLocation().add(bc.bcDirectionRotateLeft(direction));
            }
        }

        if(!utility.move(unit,loc)){
            utility.wander(unit);
        }
    }
    public void fight(Unit enemy,Utility utility){
        if(unit.location().mapLocation().isWithinRange(unit.attackRange(),enemy.location().mapLocation())){
            if(!unit.location().mapLocation().isAdjacentTo(enemy.location().mapLocation())){
                if(controller.isAttackReady(unit.id()) && controller.canAttack(unit.id(),enemy.id())){
                    VecUnit allies = controller.senseNearbyUnitsByTeam(enemy.location().mapLocation(),4,utility.ally);

                    if(allies.size() > 3)return;

                    controller.attack(unit.id(),enemy.id());
                    System.out.println("MAGE ATTACKED");
                }
            }else{
                Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
                dir = bc.bcDirectionRotateRight(dir);
                MapLocation loc = unit.location().mapLocation().addMultiple(dir,25);
                if(!blink(loc)){
                    microMove(enemy,utility);
                }
            }
        }
    }
    public boolean blink(MapLocation  location){
        if(controller.researchInfo().getLevel(unit.unitType()) != 4){
            //System.out.println("MAGE LEVEL "+controller.researchInfo().getLevel(unit.unitType()));
            return false;
        }
        if(controller.isBlinkReady(unit.id())){
            if(controller.canBlink(unit.id(),location)){
                controller.blink(unit.id(),location);
                System.out.println("BLINKED");
                return true;
            }
        }
        return false;
    }

    public void findNextMove(Utility utility){
        if(utility.attackVectors.containsKey(unit.id())) {
            if (!utility.attackVectors.get(unit.id()).isEmpty()) {
                long elapsed = 0;
                if (utility.elapsedTime.containsKey(unit.id())) {
                    elapsed = utility.roundNum - utility.elapsedTime.get(unit.id());
                    //System.out.println("MAGE ELAPSED TIME IS "+elapsed);
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
                        if(!blink(utility.attackVectors.get(unit.id()).peek())){
                            utility.move(unit, utility.attackVectors.get(unit.id()).peek());
                        }
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
