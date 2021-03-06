package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class StudentPlayerBestFit extends PylosPlayer {
    PylosLocation lastLocation = null;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        List<PylosLocation> usableLocations = new ArrayList<>();

        for (PylosLocation location : board.getLocations()) {
            if (location.isUsable()) {
                usableLocations.add(location);
            }
        }

        lastLocation = makeSquareLocation(board, game, usableLocations);

        if (lastLocation != null){
            PylosSphere toMove = board.getReserve(this);
            game.moveSphere(toMove, lastLocation);
            return;
        }

        List<PylosLocation> topLocations = new ArrayList<>();

        // move to higher level
        for (PylosSquare square : board.getAllSquares()) {

            // if square is filled and has empty location above
            if (square.isSquare() && square.getTopLocation().isUsable()) {
                topLocations.add(square.getTopLocation());
            }
        }

        if (!topLocations.isEmpty()) {
            lastLocation = makeSquareLocation(board, game, topLocations);
            if (lastLocation != null) {
                PylosSphere sphere = board.getReserve(this);
                game.moveSphere(sphere, lastLocation);
                return;
            } else {
                PylosLocation location = topLocations.get(0);
                PylosSphere sphere = getMoveableSphere(board, game, location);
                lastLocation = location;
                game.moveSphere(sphere, location);
                return;
            }
        }

        // block other user if there is a square with 2 his/here spheres
        lastLocation = getUsableLocation(board, game);

        // choose a random location
        if (lastLocation == null){
            lastLocation = usableLocations.get(this.getRandom().nextInt(usableLocations.size()));
        }

        PylosSphere toMove = board.getReserve(this);
        game.moveSphere(toMove, lastLocation);

    }

    public PylosLocation getUsableLocation(PylosBoard board, PylosGameIF game){

        PylosLocation usableLocation = null;
        for (PylosSquare square : board.getAllSquares()){
            if (square.getInSquare(this.OTHER) == 2 && square.getInSquare(this) == 0){
                for(PylosLocation location :square.getLocations()){
                    if (location.isUsable()){
                        return location;
                    }
                }
            }
        }
        return usableLocation;
    }

    public PylosSphere getMoveableSphere(PylosBoard board, PylosGameIF game, PylosLocation location) {
        PylosSphere moveableSphere = null;
        for (PylosSphere sphere : board.getSpheres(this)) {
            if (sphere.canMoveTo(location)) {
                if (sphere.isReserve() && moveableSphere == null) {
                    moveableSphere = sphere;

                    // TODO check if reserve sphere yields the best solution
                } else {
                    moveableSphere = sphere;
                    break;
                }
            }

        }

        return moveableSphere;
    }

    public PylosLocation makeSquareLocation(PylosBoard board, PylosGameIF game, List<PylosLocation> locations) {

        PylosLocation makeSquareLocation = null;
        for (PylosLocation location : locations) {
            for (PylosSquare square : location.getSquares()) {

                // make a square, block the other player
                int spheresOther = square.getInSquare(this.OTHER);
                if (spheresOther == 3) {
                    return location;
                }

                // make a square, make a filled square
                int spheresSelf = square.getInSquare(this);
                if (spheresSelf == 3) {
                    makeSquareLocation = location;
                }
            }
        }
        return makeSquareLocation;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

        //TODO: take the most optimal usable sphere out of the square
        PylosSphere sphere = lastLocation.getSphere();
        game.removeSphere(sphere);

    }

    public List<PylosSphere> getRemovableSpheres(PylosGameIF game, PylosBoard board){

        //zoek alle moveable spheres
        ArrayList<PylosSphere> moveableSpheres = new ArrayList<>();
        for (PylosSphere sphere : board.getSpheres(this)) {
            if (!sphere.isReserve() && !sphere.getLocation().hasAbove()) {
                moveableSpheres.add(sphere);
            }
        }

        Iterator<PylosSphere> iterator = moveableSpheres.iterator();
        while (iterator.hasNext()){
            PylosSphere sphere = iterator.next();
            for (PylosSquare square : sphere.getLocation().getSquares()){

                // geef andere speler geen mogelijkheid om een square te maken
                if (square.getInSquare(this.OTHER) == 3){
                    iterator.remove();
                }

            }
        }


        return moveableSpheres;
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

        /***
         * probeer eerst altijd een bol te verwijderen van het bord
         * pass indien er geen verwijderbare bollen zijn
         */

        List<PylosSphere> moveableSpheres = getRemovableSpheres(game, board);

        if (moveableSpheres.isEmpty()) {
            game.pass();
        }

        //selecteer de te verwijderen sphere
        int index = this.getRandom().nextInt(moveableSpheres.size());
        game.removeSphere(moveableSpheres.get(index));
        moveableSpheres.remove(index);
    }
}
