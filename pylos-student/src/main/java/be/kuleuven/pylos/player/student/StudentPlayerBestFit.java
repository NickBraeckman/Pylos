package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;


public class StudentPlayerBestFit extends PylosPlayer {
    PylosLocation lastLocation = null;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        //vind alle useable locations
        List<PylosLocation> usableLocations = new ArrayList<>();
        for (PylosLocation location : board.getLocations()) {
            if (location.isUsable()) {
                usableLocations.add(location);
            }
        }

        //belet enemy van óf probeer zelf een square te maken
        lastLocation = makeSquareLocation(board, game, usableLocations);
        if (lastLocation != null){
            PylosSphere toMove = board.getReserve(this);
            game.moveSphere(toMove, lastLocation);
            return;
        }

        List<PylosLocation> topLocations = new ArrayList<>();
        // move to higher level if possible
        for (PylosSquare square : board.getAllSquares()) {
            // if square is filled and has empty location above
            if (square.isSquare() && square.getTopLocation().isUsable()) {
                //if this move creates square opportunity for enemy, don't do it.
                for(PylosSquare topsquare : square.getTopLocation().getSquares()){
                    if(topsquare.getInSquare(this.OTHER) != 3){
                        topLocations.add(square.getTopLocation());
                    }
                }
            }
        }
        // als er WEL vrije locaties zijn op een hoger niveau, dan
        if (!topLocations.isEmpty()) {
            // kijk of je zelf een square kan maken/enemy beletten om dit te doen
            lastLocation = makeSquareLocation(board, game, topLocations);
            //is enkel != NULL als er ergens een locatie is die te maken heeft met een square
            if (lastLocation != null) {
                PylosSphere sphere = getMoveableSphere(board, game, lastLocation);
                game.moveSphere(sphere, lastLocation);
                return;
            } else {
                //toplocations is reeds gefiltered op beletten van squares op hoger niveau, hoeft dus hier niet
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
            //zorg ervoor dat de zet die je selecteert geen square creëert op een hoger niveau voor de tegenspeler
            //      -> tenzij het een gedwongen zet is.
            List<PylosLocation> betterRandom = usableLocations.stream().filter( location -> {
                for(PylosSquare plausibleSquare : location.getSquares()){
                    if(plausibleSquare.getInSquare() == 3) {
                        for(PylosSquare square : plausibleSquare.getTopLocation().getSquares()){
                            if(square.getInSquare(this.OTHER) == 3) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }).collect(Collectors.toList());
            if(!betterRandom.isEmpty()){
                usableLocations = betterRandom;
            }

            /*if(usableLocations.size() > 1) {
                Iterator<PylosLocation> listit = usableLocations.listIterator();
                while (listit.hasNext()) {
                    PylosLocation location = listit.next();
                    //enkel toplocatie checken voor squares die gevuld worden rond de te checken locatie
                    for (PylosSquare plausibleSquare : location.getSquares()) {
                        //deze boolean breakt als er gevonden is dat deze locatie beter niet genomen wordt.
                        boolean hasBeenCancelled = false;
                        if (plausibleSquare.getInSquare() == 3) {
                            // check de squares rond de locatie die vrijkomt door de overwogen zet
                            for (PylosSquare square : plausibleSquare.getTopLocation().getSquares()) {
                                if (square.getInSquare(this.OTHER) == 3) {
                                    if (usableLocations.size() > 1) {
                                        listit.remove();
                                    }
                                    hasBeenCancelled = true;
                                    break;
                                }
                            }
                            if (hasBeenCancelled) {
                                break;
                            }
                        }
                    }
                }
            }*/
            // selecteer at random een van de resterende zetten.
            lastLocation = usableLocations.get(this.getRandom().nextInt(usableLocations.size()));
        }

        PylosSphere toMove = board.getReserve(this);
        game.moveSphere(toMove, lastLocation);

    }

    public PylosLocation getUsableLocation(PylosBoard board, PylosGameIF game){
        for (PylosSquare square : board.getAllSquares()){
            if (square.getInSquare(this.OTHER) == 2 && square.getInSquare(this) == 0){
                for(PylosLocation location :square.getLocations()){
                    if (location.isUsable()){
                        return location;
                    }
                }
            }
        }
        return null;
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
        if (moveableSphere == null){
            moveableSphere = board.getReserve(this);
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

        //belet om spheres terug te nemen die tegenstander een square geven
        List<PylosSphere> result = moveableSpheres.stream().filter(sphere -> {
            for(PylosSquare square: sphere.getLocation().getSquares()){
                if (square.getInSquare(this.OTHER) == 3) return true;
            }
            return false;
        }).collect(Collectors.toList());

        // probeer ervoor te zorgen dat je de eigen mogelijkheid om squares te maken niet fnuikt
        List<PylosSphere> secondaryResult = result.stream().filter(sphere -> {
            for(PylosSquare square: sphere.getLocation().getSquares()){
                if (square.getInSquare(this) == 3) return true;
            }
            return false;
        }).collect(Collectors.toList());

        if(secondaryResult.isEmpty()){
            return result;
        } else {
            return secondaryResult;
        }
        /*Iterator<PylosSphere> iterator = moveableSpheres.iterator();
        while (iterator.hasNext()){
            PylosSphere sphere = iterator.next();
            for (PylosSquare square : sphere.getLocation().getSquares()){

                // geef andere speler geen mogelijkheid om een square te maken
                if (square.getInSquare(this.OTHER) == 3){
                    iterator.remove();
                    continue;
                }

                // probeer ervoor te zorgen dat je de eigen mogelijkheid om squares te maken niet fnuikt
                if (square.getInSquare(this) == 3) {
                    if(moveableSpheres.size() > 1){
                        iterator.remove();
                        continue;
                    }
                }

            }
        }*/
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
            return;
        }

        //selecteer de te verwijderen sphere
        int index = this.getRandom().nextInt(moveableSpheres.size());
        game.removeSphere(moveableSpheres.get(index));
        moveableSpheres.remove(index);
    }
}

// SLECHTE ZETTEN

// 1)
// zijn er nog andere situaties waar je liever niet een sphere terugneemt? geen idee...
    // --> als de andere er 2 in een square heeft EN
        // beide andere locations vrij zijn
        // hij bovendien nog ergens anders een sphere heeft om er nadien bovenop te zetten
        // waarom? anders kan hij met gedwongen zetten de gewonnen sphere terugverdienen
    //SLECHT: Op dat moment kan je namelijk dit gewoon negeren als speler 1 en terug je vierkant maken
    //          dit levert een nulwinst op voor beiden, dus speler 1 is nog altijd 2 ballen ahead.