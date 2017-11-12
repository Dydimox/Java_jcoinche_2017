import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.io.IOException;
import java.util.Map;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

class Application {
    // The port that the server listens on.
    private static final int PORT = 9001;

    // Shared ressources _between game and players
    // End of shared ressources

    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) {
        System.out.println("The server is running.");
        try {
            ServerSocket listener = new ServerSocket(PORT);
            try {
                while (true) {
                    new Player(listener.accept()).start();
                }
            } catch (RuntimeException e) {
                System.out.println("RuntimeException handled :");
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException handled :");
                System.out.println(e.getMessage());
            } finally {
                try {
                    listener.close();
                } catch (IOException e) {
                    System.out.println("IOException handled :");
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("IOException handled :");
            System.out.println(e.getMessage());
        }
    }

    // Class player with its own thread
    private static class Player extends Thread {
        // Communication
        private Socket _socket;
        private BufferedReader _in;
        private PrintWriter _out;

        // Essentials stuff
        private boolean _isRunning = true;
        private interface inputMethod {
            void exec();
        }
        private Map<String, inputMethod> _inputToMethod = new HashMap<String, inputMethod>();

        Deck deck;

        // User interface
        private String [] _inputs = new String [6];
        private String [] _inputsDescriptions = new String [6];
        public enum Inputs {
            HIT(0), STAY(1), DOUBLE(2), EXIT(3), YES(4), NO(5), LENGTH(6);

            Inputs(int n){
                this._n = n;
            }

            public int getInt() {
                return this._n;
            }

            private int _n;
        }
        public enum Print {
            SND("SEND "), NL("NEW_LINE"), CLR("CLEAR"), WI("WAIT_INPUT"), SLEEP("SLEEP ");

            Print(String str) {
                this._str = str;
            }

            public String getString() {
                return _str;
            }

            private String _str;
        }
        // Dealer stuff
        private Vector<Card> _dealerCards = new Vector<Card>();
        private boolean _dealersBJ;

        // Player stuff
        private Vector<Card> _cards = new Vector<Card>();
        private int _bet;
        private int _nbToken = 200;
        private boolean _bJ;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interest_ing work is done in the run method.
         */
        public Player(Socket socket) {
            // Get socket
            this._socket = socket;

            // Initialize communication
            try {
                _in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
                _out = new PrintWriter(_socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Socket cannot be connected");
            }

            // Fill Method's map
            _inputToMethod.put("d", doubleBet);
            _inputToMethod.put("e", exit);
            _inputToMethod.put("h", hitCard);
            _inputToMethod.put("s", stay);

            // Fill user Interface
            _inputs[Inputs.DOUBLE.getInt()] = "d";
            _inputsDescriptions[Inputs.DOUBLE.getInt()] = "double your bet";
            _inputs[Inputs.EXIT.getInt()] = "e";
            _inputsDescriptions[Inputs.EXIT.getInt()] = "exit the game with your tokens";
            _inputs[Inputs.HIT.getInt()] = "h";
            _inputsDescriptions[Inputs.HIT.getInt()] = "hit a new card";
            _inputs[Inputs.STAY.getInt()] = "s";
            _inputsDescriptions[Inputs.STAY.getInt()] = "stay and wait the end of the turn";
            _inputs[Inputs.YES.getInt()] = "y";
            _inputsDescriptions[Inputs.YES.getInt()] = "yes";
            _inputs[Inputs.NO.getInt()] = "n";
            _inputsDescriptions[Inputs.NO.getInt()] = "no";

            run();
        }

        // Player run
        public void run() {
            try {
                while (_isRunning) {
                    sendInfo();
                    shuffleCards();
                    bet();
                    startTurn();
                    endTurn();
                }
            } finally {
                // close socket and quit game
                try {
                    _socket.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // Player functions
        private String askForInput(String askText, Inputs [] inputs, boolean onSameLine) {
            return askForInput(askText, inputs, onSameLine, true);
        }

        private String askForInput(String askText, Inputs [] inputs, boolean onSameLine, boolean sendInfo) {
            String _input;

            if (sendInfo)
                sendInfo();
            if (onSameLine) {
                String buffer = Print.SND.getString() + askText + " : ";
                for (int i = 0; i < inputs.length; i++)
                    buffer += _inputsDescriptions[inputs[i].getInt()] + " (" + _inputs[inputs[i].getInt()] + "), ";
                _out.println(buffer.substring(0, buffer.length() - 2));
            } else {
                _out.println(Print.SND.getString() + askText + " :");
                for (int i = 0; i < inputs.length; i++)
                    _out.println(Print.SND.getString() + "- \"" + _inputs[inputs[i].getInt()] + "\" -> " + _inputsDescriptions[inputs[i].getInt()]);
            }
            try {
                _out.println(Print.NL.getString());
                _out.println(Print.SND.getString() + "-------------------------------------------");
                _out.println(Print.WI.getString());
                _input = _in.readLine();
                _out.println(Print.NL.getString());
            } catch (IOException e) {
                System.out.println(e.getMessage());
                _out.println(Print.SND.getString() + "Read error");
                throw new RuntimeException("Read error");
            }
            for (int i = 0; i < inputs.length; i++)
                if (_input.equals(_inputs[inputs[i].getInt()]))
                    return _input;
            _out.println(Print.SND.getString() + "Bad input, try again");
            _out.println(Print.SLEEP + "1500");
            return askForInput(askText, inputs, onSameLine);
        }

        private void bet() {
            String _input;
            int n;

            sendInfo();
            _out.println(Print.SND.getString() + "You have " + _nbToken + " tokens, how many do you want to bet ?");
            try {
                _out.println("WAIT_INPUT");
                _input = _in.readLine();
            } catch (IOException e) {
                _out.println(Print.SND.getString() + "Read error");
                throw new RuntimeException("Read error");
            }
            for (int i = 0; i < _input.length(); i++)
                if (_input.charAt(i) < '0' || _input.charAt(i) > '9') {
                    sendInfo();
                    _out.println(Print.SND.getString() + "Bad parameter, try again.");
                    _out.println(Print.SLEEP + "1500");
                    bet();
                    return ;
                }
            n = Integer.parseInt(_input);
            if (n == 0) {
                _out.println(Print.SLEEP + "1500");
                _out.println("You must at least bet 1 token");
                bet();
                return ;
            }
            if (n > _nbToken) {
                sendInfo();
                _out.println(Print.SND.getString() + "You don't have enough token for that.");
                bet();
                return ;
            }
            _bet = n;
            _nbToken -= _bet;
        }

        private int calcScore(Vector<Card> _cards) {
            int as = 0;
            int score = 0;
            for (Card _card : _cards) {
                int value = _card.getRank();
                if (value == 1)
                    as++;
                else if (value >= 10)
                    score += 10;
                else
                    score += value;
            }
            while (as > 0) {
                if (score <= 10 + (as - 1))
                    score += 11;
                else
                    score += 1;
                as--;
            }
            return score;
        }

        private void drawCard(Vector<Card> _cards, String name) {
            sendInfo();
            Card card = deck.draw();
            _out.println(Print.SND.getString() + name + " draw a " + card.toString());
            _cards.addElement(card);
            _out.println(Print.SLEEP.getString() + "1500");
        }

        private void equality() {
            _nbToken += _bet;
            sendInfo();
            _out.println(Print.SND.getString() + "Equality");
            _out.println(Print.SND.getString() + "You loose nothing and win nothing");
        }

        private void endTurn() {
            drawCard(_dealerCards, "Dealer");
            if (_dealerCards.size() == 2 && calcScore(_dealerCards) == 21) {
                _out.println("Dealer get a Black Jack");
                _dealersBJ = true;
            }
            if (calcScore(_dealerCards) == calcScore(_cards) ||
                    (calcScore(_dealerCards) > 21 && calcScore(_cards) > 21)) {
                if (_dealersBJ == _bJ)
                    equality();
                else if (_dealersBJ == true)
                    loose();
                else
                    win();
            }
            else if (calcScore(_dealerCards) <= 21 &&
                    (calcScore(_cards) > 21 ||
                    calcScore(_dealerCards) > calcScore(_cards)))
                loose();
            else if (calcScore(_cards) <= 21 &&
                    (calcScore(_dealerCards) > 21 ||
                    calcScore(_dealerCards) < calcScore(_cards))) {
                if (calcScore(_dealerCards) < 17) {
                    endTurn();
                    return ;
                }
                win();
            }
            else
                throw new RuntimeException("Uncaught case");
            _out.println(Print.SLEEP + "3000");
            if (_nbToken == 0) {
                _out.println(Print.SND.getString() + "You loose all your tokens retry another day ;)");
                _isRunning = false;
                _out.println(Print.SLEEP + "2000");
                return ;
            }
            _out.println(Print.SND.getString() + "--------------------");
            _out.println(Print.SND.getString() + "You have " + _nbToken);
            String input = askForInput("Do you want to continue ?", generateYNInputs(), true, false);
            if (input.equals("n"))
                _inputToMethod.get("e").exec();
        }

        private Inputs [] generateYNInputs() {
            Inputs [] inputs = new Inputs [2];
            inputs[0] = Inputs.YES;
            inputs[1] = Inputs.NO;
            return inputs;
        }

        private Inputs [] generateActionInputs() {
            return generateActionInputs(true);
        }

        private Inputs [] generateActionInputs(boolean withDouble) {
            if (withDouble && _nbToken >= _bet) {
                Inputs[] inputs = new Inputs[3];
                inputs[0] = Inputs.HIT;
                inputs[1] = Inputs.STAY;
                inputs[2] = Inputs.DOUBLE;
                return inputs;
            }
            else {
                Inputs[] inputs = new Inputs[2];
                inputs[0] = Inputs.HIT;
                inputs[1] = Inputs.STAY;
                return inputs;
            }
        }

        private void loose() {
            sendInfo();
            _out.println(Print.SND.getString() + "Dealer win");
            _out.println(Print.SND.getString() + "You loose " + _bet + " tokens");
        }

        private void processInput(String _input) {
            _inputToMethod.get(_input).exec();
        }

        private void sendInfo() {
            _out.println(Print.CLR.getString());
            _out.println(Print.SND.getString() + "-------------INFO-------------");
            _out.println(Print.SND.getString() + "You have " + _nbToken + " tokens");
            _out.println(Print.SND.getString() + "Your cards are :");
            for (Card _card : _cards)
                _out.println(Print.SND.getString() + "- " + _card.toString());
            _out.println(Print.SND.getString() + "Your cards total is " + calcScore(_cards));
            _out.println(Print.NL.getString());
            _out.println(Print.SND.getString() + "The dealer's cards are :");
            for (Card _card : _dealerCards)
                _out.println(Print.SND.getString() + "- " + _card.toString());
            _out.println(Print.SND.getString() + "The dealer's cards total is " + calcScore(_dealerCards));
            _out.println(Print.SND.getString() + "------------------------------");
            _out.println(Print.NL.getString());
        }

        private void shuffleCards() {
            _cards.removeAllElements();
            _dealerCards.removeAllElements();
            _bJ = false;
            _dealersBJ = false;
            sendInfo();
            deck = new Deck(false);
            _out.println(Print.SND.getString() + "Deck is suffled");
            _out.println(Print.SLEEP.getString() + "1500");
        }

        private void startTurn() {
            sendInfo();
            _out.println(Print.NL.getString());
            drawCard(_cards, "You");
            drawCard(_cards, "You");
            if (calcScore(_cards) == 21) {
                _out.println("Great, you get a Black Jack");
                _bJ = true;
            }
            _out.println(Print.NL.getString());
            drawCard(_dealerCards, "The dealer");
            processInput(askForInput("What do you want to do ?", generateActionInputs(), false));
        }

        private void win() {
            _nbToken += _bet * 2;
            sendInfo();
            _out.println(Print.SND.getString() + "You win");
            _out.println(Print.SND.getString() + "You win " + _bet * 2 + " tokens");
        }

        // Player Inputs Methods
        private inputMethod doubleBet = new inputMethod() {
            public void exec() {
                sendInfo();
                _nbToken -= _bet;
                _bet *= 2;
                String input = askForInput("Do you want to take another card ?", generateYNInputs(), true);
                if (input.equals("y"))
                    drawCard(_cards, "You");
            }
        };

        private inputMethod exit = new inputMethod() {
            public void exec() {
                _out.println(Print.NL.getString());
                _out.println(Print.NL.getString());
                String input = askForInput("Are you sure ?", generateYNInputs(), true);
                if (input.equals("y")) {
                    _isRunning = false;
                    _out.println(Print.SND.getString() + "Congratulations you quit the game with " + _nbToken + " tokens !");
                }
            }
        };

        private inputMethod hitCard = new inputMethod() {
            public void exec() {
                sendInfo();
                drawCard(_cards, "You");
                if (calcScore(_cards) < 21)
                    processInput(askForInput("What do you want to do ?", generateActionInputs(false), false));
            }
        };

        private inputMethod stay = new inputMethod() {
            public void exec() {
                return;
            }
        };
    }
}
