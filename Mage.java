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
        if(utility.currentStrategy == NEUTRAL){
            runNeutral(utility);
        }
    }

    public void runNeutral(Utility utility){
        init(utility);

        if(!unit.location().isInGarrison() && !unit.location().isInSpace()){

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
            if (utility.myMages.containsKey(unit.id())) {
                utility.myMages.remove(unit.id());
            }
        }
    }
    public void microMove(Unit enemy,Utility utility){
        long distance = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());

        Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
        MapLocation loc = unit.location().mapLocation();

        if(distance <= enemy.attackRange()/2){
            loc.addMultiple(bc.bcDirectionOpposite(bc.bcDirectionRotateLeft(dir)),2);
        }else{
            if(distance > unit.attackRange()-2){
                loc.addMultiple(bc.bcDirectionRotateRight(dir),2);
            }else{
                loc.addMultiple(utility.compass[utility.random.nextInt(utility.compass.length)],2);
            }
        }

        utility.move(unit,loc);
    }
    public void fight(Unit enemy,Utility utility){
        if(unit.location().mapLocation().isWithinRange(unit.attackRange(),enemy.location().mapLocation())){
            if(!unit.location().mapLocation().isAdjacentTo(enemy.location().mapLocation())){
                if(controller.isAttackReady(unit.id()) && controller.canAttack(unit.id(),enemy.id())){
                    VecUnit allies = controller.senseNearbyUnitsByTeam(enemy.location().mapLocation(),2,utility.ally);

                    if(allies.size() > 3)return;

                    controller.attack(unit.id(),enemy.id());
                    System.out.println("MAGE ATTACKED");
                }
            }else{
                Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
                dir = bc.bcDirectionRotateRight(dir);
                MapLocation loc = unit.location().mapLocation().addMultiple(dir,3);
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
