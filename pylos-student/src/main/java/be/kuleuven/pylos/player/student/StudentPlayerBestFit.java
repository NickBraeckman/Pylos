package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
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
            //zorg ervoor dat de zet die je selecteert geen gevulde square creëert
            //      -> tenzij het een gedwongen zet is.
            List<PylosLocation> betterRandom = usableLocations.stream().filter( location -> {
                for(PylosSquare plausibleSquare : location.getSquares()){
                    if(plausibleSquare.getInSquare() == 3) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            if(!betterRandom.isEmpty()){
                usableLocations = betterRandom;
            } else {
                //zorg ervoor dat de square die je creëert geen mogelijkheid geeft tot squaring voor tegenstander op dat hoger niveau
                //      -> tenzij het een gedwongen zet is
                List<PylosLocation> slightlyBetterRandom = usableLocations.stream().filter( location -> {
                    for(PylosSquare plausibleSquare : location.getSquares()){
                        if(plausibleSquare.getInSquare() == 3) {
                            for(PylosSquare squareUpTop : plausibleSquare.getTopLocation().getSquares()){
                                if(squareUpTop.getInSquare(this.OTHER) == 3){
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
                if(!slightlyBetterRandom.isEmpty()){
                    usableLocations = slightlyBetterRandom;
                }
            }

            // selecteer at random een van de resterende zetten.
            lastLocation = usableLocations.get(this.getRandom().nextInt(usableLocations.size()));
        }

        PylosSphere toMove = board.getReserve(this);
        game.moveSphere(toMove, lastLocation);

    }

    public PylosLocation getUsableLocation(PylosBoard board, PylosGameIF game){
        List<PylosLocation> possibleLocations = new ArrayList<>();
        for (PylosSquare square : board.getAllSquares()){
            if (square.getInSquare(this.OTHER) == 2 && square.getInSquare(this) == 0){
                for(PylosLocation location :square.getLocations()){
                    if (location.isUsable()){
                        possibleLocations.add(location);
                    }
                }
            }
        }
        List<PylosLocation> betterLocations = possibleLocations.stream().filter( location -> {
            for(PylosSquare toCheck : location.getSquares()){
                if(toCheck.getInSquare() == 3){
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
        if(!betterLocations.isEmpty()){
            possibleLocations = betterLocations;
        }

        //return als mogelijk, anders null
        if(possibleLocations.isEmpty()){
            return null;
        }
        return possibleLocations.get(0);
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
        System.out.println("square was made");

        Set<PylosSphere> spheres = new HashSet<>();
        //zoek de net gemaakte square && voeg alle removeable spheres hieraan toe.
        for(PylosSquare square : lastLocation.getSquares()){
            if(square.getInSquare(this) == 4){
                for(PylosLocation loc : square.getLocations()){
                    if(!loc.hasAbove()){
                        spheres.add(loc.getSphere());
                    }
                }
            }
        }

        //zorg ervoor dat de andere geen vierkant kan maken door een bepaalde sphere te removen
        List<PylosSphere> result = spheres.stream().filter(sphere -> {
            for(PylosSquare square: sphere.getLocation().getSquares()){
                if (square.getInSquare(this.OTHER) == 3) return false;
            }
            return true;
        }).collect(Collectors.toList());

        if(!result.isEmpty()){
            //als Z == 0, kies willekeurig een en verwijder
            if(result.get(0).getLocation().Z == 0){
                int index = this.getRandom().nextInt(result.size());
                game.removeSphere(result.get(index));
                return;
            } else {
                //eigenlijk moet hier rekening gehouden worden met optimale keuze:
                //verwijderen hier een zodanig dat je als 2e een sphere kan verwijderen die 'omhoog zetten voor de tegenspeler'
                //onmogelijk maakt

                //TODO: fix this
                int index = this.getRandom().nextInt(result.size());
                game.removeSphere(result.get(index));
                return;
            }
        } else {
            //je kan geen enkele sphere terugnemen uit de net-gemaakte square zonder de tegenstander een vierkant te geven

            //probeer alsnog ergens anders een verwijderbare sphere te vinden
            //zoek alle moveable spheres
            List<PylosSphere> moveableSpheres = new ArrayList<>();
            for (PylosSphere sphere : board.getSpheres(this)) {
                if (!sphere.isReserve() && !sphere.getLocation().hasAbove()) {
                    moveableSpheres.add(sphere);
                }
            }
            //verwijder alle spheres uit deze lijst die in het originele vierkant zitten
            moveableSpheres = moveableSpheres.stream().filter( element -> {
                return !spheres.contains(element);
            }).collect(Collectors.toList());

            if(!moveableSpheres.isEmpty()){
                //er zijn andere opties om te bekijken.
                result = moveableSpheres;
            } else {
                //er is geen andere optie dan een sphere te verwijderen uit de nieuw-gemaakte square
                result = new ArrayList<>(spheres);
            }
        }

        //construëer een map die alle verwijderbare spheres van this.OTHER per niveau groepeert.
        //map.get(1) returnt de lijst met alle removeable spheres van this.OTHER op niveau Z==1
        HashMap<Integer, List<PylosSphere>> enemySpheres = new HashMap<>();
        for(int i=0; i<4; i++){
            enemySpheres.put(i, new ArrayList<PylosSphere>());
        }
        //vul deze map nu correct op
        for (PylosSphere sphere : board.getSpheres(this.OTHER)) {
            if (!sphere.isReserve() && !sphere.getLocation().hasAbove()) {
                enemySpheres.get(sphere.getLocation().Z).add(sphere);
            }
        }

        // probeer een sphere weg te pakken zodanig dat de andere niet naar een hoger niveau kan verplaatsen
        //      -> bekijk alle spheres die verwijderbaar zijn
        //          DAN -> als deze tot een square behoort en hun toplocatie vrij is, is dit bij prioriteit te verwijderen
        for(PylosSphere sphere : result){
            for(PylosSquare square : sphere.getLocation().getSquares()){
                if(square.getTopLocation().isUsable()){
                    for(int i=0; i<square.getTopLocation().Z; i++){
                        if(!enemySpheres.get(i).isEmpty()){
                            game.removeSphere(sphere);
                            return;
                        }
                    }
                }
            }
        }

        // probeer GEEN sphere weg te nemen die een locatie opent op een hoger niveau ASA de this.OTHER een bal heeft om
        // naar die locatie te verplaatsen.
        // MAAR eigenlijk zou hij jou moeten blokkeren op dat moment.
        //      -> als hij dat dan doet creëert this.OTHER OOK een square waarop jij een lager gelegen sphere kan naartoe verplaatsen.
        // conclusie: het is beter om de eerste stelling te volgen
        List<PylosSphere> temp = result.stream().filter( sphere -> {
            if(sphere.getLocation().Z > 0){
                for(int i=0; i<sphere.getLocation().Z; i++){
                    if(!enemySpheres.get(i).isEmpty()){
                        return false;
                    }
                }
            }
            return true;
        }).collect(Collectors.toList());
        if(!temp.isEmpty()){
            result = temp;
        }

        //eventueel andere dingen bekijken
        System.out.println("size van de removeable lijst: " + result.size());
        int index = this.getRandom().nextInt(result.size());
        game.removeSphere(result.get(index));
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
                if (square.getInSquare(this.OTHER) == 3) return false;
            }
            return true;
        }).collect(Collectors.toList());

        // probeer ervoor te zorgen dat je de eigen mogelijkheid om squares te maken niet fnuikt
        List<PylosSphere> secondaryResult = result.stream().filter(sphere -> {
            for(PylosSquare square: sphere.getLocation().getSquares()){
                if (square.getInSquare(this) == 3 && square.getInSquare(this.OTHER) == 0) return false;
            }
            return true;
        }).collect(Collectors.toList());

        // probeer een sphere weg te pakken zodanig dat de andere niet naar een hoger niveau kan verplaatsen
        //      -> bekijk alle spheres die verwijderbaar zijn
        //          DAN -> als deze tot een square behoort en hun toplocatie vrij is, is dit bij prioriteit te verwijderen

        // probeer GEEN sphere weg te nemen die een locatie opent op een hoger niveau ASA de this.OTHER een bal heeft om
        // naar die locatie te verplaatsen.
        // MAAR eigenlijk zou hij jou moeten blokkeren op dat moment.
        //      -> als hij dat dan doet creëert this.OTHER OOK een square waarop jij een lager gelegen sphere kan naartoe verplaatsen.
        // conclusie: het is beter om de eerste stelling te volgen

        if(secondaryResult.isEmpty()){
            return result;
        } else {
            return secondaryResult;
        }
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