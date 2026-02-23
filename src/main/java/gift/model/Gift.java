package gift.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Gift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Member sender;

    @ManyToOne
    private Member receiver;

    @ManyToOne
    private Option option;

    private int quantity;
    private String message;

    protected Gift() {
    }

    public Gift(final Member sender, final Member receiver, final Option option, final int quantity, final String message) {
        this(null, sender, receiver, option, quantity, message);
    }

    public Gift(final Long id, final Member sender, final Member receiver, final Option option, final int quantity, final String message) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.option = option;
        this.quantity = quantity;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Member getSender() {
        return sender;
    }

    public Member getReceiver() {
        return receiver;
    }

    public Option getOption() {
        return option;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMessage() {
        return message;
    }
}
