package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.List;


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

        if (isFilledSquaredOther(board, game, usableLocations)) {
            return;
        }

        List<PylosLocation> topLocations = new ArrayList<>();

        // move to higher level
        for (PylosLocation location : board.getLocations()) {
            if (location.isUsable()) {
                for (PylosSquare square : location.getSquares()) {
                    if (square.isSquare() && square.getTopLocation().isUsable()) {
                        topLocations.add(square.getTopLocation());
                    }
                }
            }
        }
        if (!topLocations.isEmpty()) {
            if (isFilledSquaredOther(board, game, topLocations)) {
                return;
            } else {
                PylosLocation location = topLocations.get(0);
                PylosSphere sphere = board.getReserve(this);
                board.move(sphere, location);
            }
        }


        lastLocation = usableLocations.get(this.getRandom().nextInt(usableLocations.size()));
        PylosSphere toMove = board.getReserve(this);

        game.moveSphere(toMove, lastLocation);
    }

    public boolean isFilledSquaredOther(PylosBoard board, PylosGameIF game, List<PylosLocation> locations) {

        PylosLocation makeSquareLocation = null;
        for (PylosLocation location : locations) {
            for (PylosSquare square : location.getSquares()) {

                // blocking a square
                int spheresOther = square.getInSquare(this.OTHER);
                if (spheresOther == 3) {
                    PylosSphere sphere = board.getReserve(this);
                    game.moveSphere(sphere, location);
                    return true;
                }

                // make a square
                int spheresSelf = square.getInSquare(this);
                if (spheresSelf == 3) {
                    makeSquareLocation = location;
                }
            }
        }

        if (makeSquareLocation != null) {
            PylosSphere sphere = board.getReserve(this);
            game.moveSphere(sphere, makeSquareLocation);
            return true;
        }

        return false;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        PylosSphere sphere = lastLocation.getSphere();
        game.removeSphere(sphere);

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        game.pass();
    }
}
