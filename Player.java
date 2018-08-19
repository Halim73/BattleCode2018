// import the API.
// See xxx for the javadocs.
import bc.*;

public class Player {
    public static void main(String[] args) {
        // One slightly weird thing: some methods are currently static methods on a static class called bc.
        // This will eventually be fixed :/
        System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));

        // Connect to the manager, starting the game
        GameController gc = new GameController();

        Utility utility = new Utility(gc);

        utility.setResearch();

        while (true) {
            System.out.println("Current round: "+gc.round());
            System.out.println("Total karbonite is "+utility.karbonite);
            // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
            utility.updateUtility();
            //utility.findUnBuiltFactories();
            //utility.assignUnbuiltFactories();

            VecUnit units = gc.myUnits();
            for (int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);

                switch (unit.unitType()){
                    case Worker:
                        Worker worker = new Worker(gc,unit);
                        worker.run(utility);
                        break;
                    case Ranger:
                        Ranger ranger = new Ranger(gc,unit);
                        ranger.run(utility);
                        break;
                    case Healer:
                        Healer healer = new Healer(gc,unit);
                        healer.run(utility);
                        break;
                    case Knight:
                        Knight knight = new Knight(gc,unit);
                        knight.run(utility);
                        break;
                    case Mage:
                        Mage mage = new Mage(gc,unit);
                        mage.run(utility);
                        break;
                    case Factory:
                        Factory factory = new Factory(gc,unit);
                        factory.run(utility);
                        break;
                    case Rocket:
                        Rocket rocket = new Rocket(gc,unit);
                        rocket.run(utility);
                        break;
                }
                utility.roundNum++;
                //clean up code?
            }
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
}