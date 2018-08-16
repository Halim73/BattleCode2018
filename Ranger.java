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

            if(!utility.isSafe(unit)){
                utility.tagEnemies(unit);
                Unit opp = utility.closestEnemy(unit);
                fight(opp,utility);
            }else{
                findNextMove(utility);
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
    public void fight(Unit enemy,Utility utility){
        if(unit.location().mapLocation().isWithinRange(unit.attackRange(),enemy.location().mapLocation())){
            if(!unit.location().mapLocation().isWithinRange(10,enemy.location().mapLocation())){
                if(controller.canBeginSnipe(unit.id(),enemy.location().mapLocation())){
                    controller.beginSnipe(unit.id(),enemy.location().mapLocation());
                }
                if(controller.isAttackReady(unit.id()) && controller.canAttack(unit.id(),enemy.id())){
                    controller.attack(unit.id(),enemy.id());
                    System.out.println("RANGER ATTACKED");
                }
            }else{
                Direction dir = unit.location().mapLocation().directionTo(enemy.location().mapLocation());
                dir = bc.bcDirectionOpposite(dir);
                MapLocation loc = unit.location().mapLocation().add(dir);
                utility.move(unit,loc);
            }
        }
    }
    public void findNextMove(Utility utility){
        if(utility.attackVectors.containsKey(unit.id())) {
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
