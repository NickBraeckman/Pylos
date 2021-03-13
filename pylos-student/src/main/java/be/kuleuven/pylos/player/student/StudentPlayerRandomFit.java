package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;

public class StudentPlayerRandomFit extends PylosPlayer{
    PylosLocation lastLocation = null;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
		/* add a reserve sphere to a feasible random location */
        /* board methods
         * 	PylosLocation[] allLocations = board.getLocations();
         * 	PylosSphere[] allSpheres = board.getSpheres();
         * 	PylosSphere[] mySpheres = board.getSpheres(this);
         * 	PylosSphere myReserveSphere = board.getReserve(this); */

        /* game methods
         * game.moveSphere(myReserveSphere, allLocations[0]); */
        PylosLocation[] locations = board.getLocations();
        ArrayList<PylosLocation> possibleLocations = new ArrayList<>();

        for (PylosLocation location : locations) {
            if (location.isUsable()) {
                possibleLocations.add(location);
            }
        }

        lastLocation = possibleLocations.get(this.getRandom().nextInt(possibleLocations.size()));
        PylosSphere toMove = board.getReserve(this);

        game.moveSphere(toMove, lastLocation);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

        //zoek alle moveable spheres
        PylosSphere[] allSpheres = board.getSpheres(this);
        ArrayList<PylosSphere> moveableSpheres = new ArrayList<>();
        for(PylosSphere sphere : allSpheres){
            if(!sphere.isReserve() && !sphere.getLocation().hasAbove()){
                moveableSpheres.add(sphere);
            }
        }

        if(moveableSpheres.isEmpty()){
            System.err.println("error: no removeable spheres");
            System.exit(1);
        }

        //select de te verwijderen sphere
        int index = this.getRandom().nextInt(moveableSpheres.size());
        game.removeSphere(moveableSpheres.get(index));
        moveableSpheres.remove(index);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
		/* always pass */
        if(this.getRandom().nextBoolean()){
            this.doRemove(game, board);
        } else {
            game.pass();
        }
    }
}
