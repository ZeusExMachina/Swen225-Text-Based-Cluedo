import java.util.*;

public class Player {
	private final Map<String, Card> hand;
	private final String name;
	private final Random rand;
	public Game g;
	private boolean canAccuse = true;
	private boolean canSuggest = true;
	private Stack<Location> prevLocations;
	private Set<Location> locationsVisited;

	public Player(String name) {
		this.hand = new HashMap<>();
		this.name = name;
		this.rand = new Random();
		this.prevLocations = new Stack<Location>();
		this.locationsVisited = new HashSet<Location>();
	}

	public String getName() { return name; }
	
	public boolean canAccuse() { return canAccuse; }

	public void giveCard(Card card) { hand.put(card.getName(),card); }

	public boolean playTurn(Game g) {
		boolean receivedValidInput = false;
		this.g = g;
		Scanner scan = g.getScanner();
		printCards();
		while(!receivedValidInput){
			System.out.println("Would you like to see the board? (Y/N): ");
			String userInput = scan.nextLine().toUpperCase();
			if (userInput.equals("Y")) {
				receivedValidInput = true;
				g.drawBoard();
			} else if (userInput.equals("N")) {
				receivedValidInput = true;
			} else {
				System.out.println("Invalid input, please try again.");
			}
		}
		receivedValidInput = false;
		while (!receivedValidInput && g.getPlayerRoom(this).playerCanRoll()) {
			System.out.println("Would you like to roll? (Y/N): ");
			String userInput = scan.nextLine().toUpperCase();
			if (userInput.equals("Y")) {
				receivedValidInput = true;
				move();
			} else if (userInput.equals("N")) {
				receivedValidInput = true;
			} else {
				System.out.println("Invalid input, please try again.");
			}
		}
		if(!g.getPlayerRoom(this).playerCanRoll()){
			System.out.println("Sorry, you cannot roll to move out of this Room, the exits are blocked!");
		}
		
		suggest(receivedValidInput,scan);

		receivedValidInput = false;
		while (!receivedValidInput) {
			System.out.print("Would you like to accuse? (Y/N): ");
			String userInput = scan.nextLine().toUpperCase();
			if (userInput.equals("Y")) {
				return (accuse(g));
			} else if (userInput.equals("N")) {
				receivedValidInput = true;
			} else {
				System.out.println("Invalid input, please try again.");
			}
		}
		return false;
	}

	/**
	 * Roll two dice and get their sum. Two numbers are generated
	 * to try to mimic real dice.
	 *
	 * @return the total of the two dice
	 */
	private Integer rollDice() {
		int first = rand.nextInt(6) + 1, second = rand.nextInt(6) + 1;
		System.out.println("You rolled a " + first + " and a " + second);
		return first + second;
	}

	private void move(){
		int counter = rollDice();
		Scanner scan = g.getScanner();
		Location prevLocation;
		Location newLocation;
		locationsVisited.clear();
		prevLocations.clear();
		while(counter > 0) {
			// Ask where to move
			System.out.println("Moves left: " + counter + " Enter a single move with W, A, S, or D and press enter: ");
			String direction = scan.nextLine().toUpperCase();
			if (direction.equals("W") || direction.equals("A") || direction.equals("S") || direction.equals("D")) {
				// Before anything, record the location we're moving from
				prevLocation = g.getPlayerLocation(this);
				// Then first, check whether or not the move is valid
				int moveAttemptResult = g.movePlayer(this, direction, locationsVisited, prevLocations);
				if(moveAttemptResult == 0){
					// Successful move
					newLocation = g.getPlayerLocation(this);
					if (!prevLocations.isEmpty() && newLocation.equals(prevLocations.peek())) {
						locationsVisited.remove(prevLocations.pop());
						counter++;
					} else {
						locationsVisited.add(prevLocation);
						prevLocations.push(prevLocation);
						if(g.checkPlayerInRoom(this)){
							counter = 0;
							// TODO: place Piece into random non-doorway unused Location in the room
						} else{
							counter--;
						}
					}
					g.drawBoard();
				} else if (moveAttemptResult < 0) {
					System.out.println("Cannot move in that direction, please try again.");
				} else if (moveAttemptResult > 0) {
					System.out.println("Already moved there this turn, please try again.");
				}
			} else {
				System.out.println("Invalid input, please try again.");
			}
		}
	}

	private Card isCard(String card, Card.CardType ct) {
		Card returnCard = g.getCard(card);
		if(returnCard != null) {
			if(returnCard.getType().equals(ct)) {
				return returnCard;
			}
			return null;
		}
		return null;
	}

	public void suggest(boolean receivedValidInput, Scanner scan) {
		if (g.checkPlayerInRoom(this)) {
			receivedValidInput = false;
			canSuggest = true;
			while (!receivedValidInput && canSuggest) {
				System.out.print("Would you like to suggest? (Y/N): ");
				String userInput = scan.nextLine().toUpperCase();
				if (userInput.equals("N")) {
					receivedValidInput = true;
				} else if (userInput.equals("Y")) {
					canSuggest = false;
					Card charCard;
					System.out.print("Character: ");
					charCard = isCard(scan.nextLine(), Card.CardType.CHARACTER);
					if (charCard == null) {
						while (charCard == null) {
							System.out.println("Not a valid Character card, try again: ");
							charCard = isCard(scan.nextLine(), Card.CardType.CHARACTER);
						}
					}

					Card weapCard;
					System.out.print("Weapon: ");
					weapCard = isCard(scan.nextLine(), Card.CardType.WEAPON);
					if (weapCard == null) {
						while (weapCard == null) {
							System.out.println("Not a valid Weapon card, try again: ");
							weapCard = isCard(scan.nextLine(), Card.CardType.WEAPON);
						}
					}

					Card roomCard = g.getCard(g.getPlayerRoom(this).getName());


					CardTuple cardTup = new CardTuple(charCard, weapCard, roomCard);
					g.moveViaSuggestion(cardTup);

					Card refuteCard = g.refutationProcess(this, cardTup);
					if (refuteCard == null) {
						System.out.println("Your suggestion " + cardTup.toString() + " was not refuted!");
					} else {
						System.out.println("Your suggestion " + cardTup.toString() + " was refuted by the card " + refuteCard.getName());
					}
				} else {
					System.out.println("Invalid input, please try again.");
				}
			}
		}
	}

	public Card askForCard(Game game) {
		Scanner scan = g.getScanner();
		System.out.print("Card: ");
		Card card = game.getCard(scan.nextLine());
		while (card == null) {
			System.out.println("Invalid card. Try again.\nCard:");
			card = game.getCard(scan.nextLine());
		}
		return card;
	}

	public Card refute(CardTuple tup) {
		Set<Card> refuteOptions = new HashSet<Card>();
		Card refuteCard = null;

		for(Card c : hand.values()) {
			if(tup.characterCard().equals(c)) {
				refuteOptions.add(c);
			}
			if(tup.weaponCard().equals(c)) {
				refuteOptions.add(c);;
			}
			if(tup.roomCard().equals(c)) {
				refuteOptions.add(c);
			}


		}

		if(!refuteOptions.isEmpty()) {
			Scanner scan = new Scanner(System.in);
			System.out.println("Refutable Cards:");
			for(Card c : refuteOptions) {
				System.out.println(c.getName());
			}
			System.out.println("What card would you like to refute with?: ");

			while(refuteCard == null) {
				String choice = scan.nextLine().toUpperCase();


				if(choice.equalsIgnoreCase(tup.weaponCard().getName())) {
					for(Card c : refuteOptions) {
						if(c.getName().equalsIgnoreCase(choice)) {
							refuteCard = tup.weaponCard();
							break;
						}
					}
				}

				if(choice.equalsIgnoreCase(tup.characterCard().getName())) {
					for(Card c : refuteOptions) {
						if(c.getName().equalsIgnoreCase(choice)) {
							refuteCard = tup.characterCard();
							break;
						}
					}
				}

				if(choice.equalsIgnoreCase(tup.roomCard().getName())) {
					for(Card c : refuteOptions) {
						if(c.getName().equalsIgnoreCase(choice)) {
							refuteCard = tup.roomCard();
							break;
						}
					}
				}

				else { System.out.println("Not a valid option please choose from your refutable cards: ");}
			}

		}

		else {System.out.println("No cards to refute");}

		return refuteCard;
	}

	public void printCards() {
		for(Card c : hand.values()) {
			System.out.println(c.getName());
		}
	}

	public boolean accuse(Game game) {
		if (!canAccuse) {
			System.out.println("You have already made an accusation before, and cannot make another.");
			return false;
		} else {
			System.out.println("Make a suggestion - type in 3 cards:");
			CardTuple accusation = getThreeCards(game);
			canAccuse = false;
			return game.checkAccusation(accusation);
		}
	}

	public CardTuple getThreeCards(Game game) {
		Scanner scan = new Scanner(System.in);
		Card charCard;
		System.out.print("Character: ");
		charCard = isCard(scan.nextLine(),Card.CardType.CHARACTER);
		if(charCard == null) {
			while(charCard == null) {
				System.out.println("Not a valid Character card try again: ");
				charCard = isCard(scan.nextLine(),Card.CardType.CHARACTER);
			}
		}

		Card weapCard;
		System.out.print("Weapon: ");
		weapCard = isCard(scan.nextLine(),Card.CardType.WEAPON);
		if(weapCard == null) {
			while(weapCard == null) {
				System.out.println("Not a valid Weapon card try again: ");
				weapCard = isCard(scan.nextLine(),Card.CardType.WEAPON);
			}
		}

		Card roomCard;
		System.out.print("Room: ");
		roomCard = isCard(scan.nextLine(),Card.CardType.ROOM);
		if(roomCard == null) {
			while(roomCard == null) {
				System.out.println("Not a valid room card try again: ");
				roomCard = isCard(scan.nextLine(),Card.CardType.ROOM);
			}
		}

		roomCard = g.getCard(g.getPlayerRoom(this).getName());
		return new CardTuple(charCard,weapCard,roomCard);

	}
	
	public String toString() { return "name: " + name + ", in hand: " + hand.toString(); }
}
