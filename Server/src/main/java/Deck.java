import java.util.Vector;

public class Deck {
    Vector<Card> deck = new Vector<Card>();

    public Deck(boolean is32) {
        fillDeck(is32);
    }

    private void fillDeck(boolean is32) {
        try {
            if (is32) {
                for (int i = 0; i < 4; i++) {
                    deck.add(((int) (Math.random() * deck.size())), new Card(i, 0));
                    for (int j = 6; j < 13; j++) {
                        deck.add(((int) (Math.random() * deck.size())), new Card(i, j));
                    }
                }
            }
            else {
                for (int i = 0; i < 4; i++)
                    for (int j = 0; j < 13; j++)
                        deck.add(((int) (Math.random() * deck.size())), new Card(i, j));
            }
        }
        catch (RuntimeException e){
            System.out.println(e.getMessage());
        }
    }

    public Card draw() {
        int rand = (int)(Math.random() * deck.size());
        Card card = deck.get(rand);
        deck.remove(rand);
        return card;
    }

}
