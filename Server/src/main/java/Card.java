import java.util.HashMap;
import java.util.Map;

public class Card {
    public final int suit;
    public final int rank;

    final Map<Integer, String> Suit = new HashMap<Integer, String>();
    final Map<Integer, String> Rank = new HashMap<Integer, String>();
    public Card(int suit, int rank) {
        // Maps initialization
        Suit.put(0, "Clubs");
        Suit.put(1, "Spades");
        Suit.put(2, "Diamonds");
        Suit.put(3, "Hearts");

        Rank.put(0, "Ace");
        Rank.put(1, "Two");
        Rank.put(2, "Three");
        Rank.put(3, "Four");
        Rank.put(4, "Five");
        Rank.put(5, "Six");
        Rank.put(6, "Seven");
        Rank.put(7, "Height");
        Rank.put(8, "Nine");
        Rank.put(9, "Ten");
        Rank.put(10, "Jack");
        Rank.put(11, "Queen");
        Rank.put(12, "King");

        if (suit > 4 || suit < 0 || rank > 13 || rank < 0)
            throw new RuntimeException("Rank or Suit out of range");
        this.suit = suit;
        this.rank = rank;
    }

    public String toString() {
        return Rank.get(rank) + " of " + Suit.get(suit);
    }

    public int getRank() {
        return rank + 1;
    }
}
