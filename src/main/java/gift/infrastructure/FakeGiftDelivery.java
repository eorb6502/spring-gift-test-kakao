package gift.infrastructure;

import gift.model.Gift;
import gift.model.GiftDelivery;
import gift.model.Option;
import gift.model.Product;
import org.springframework.stereotype.Component;

@Component
class FakeGiftDelivery implements GiftDelivery {

    @Override
    public void deliver(final Gift gift) {
        final Option option = gift.getOption();
        final Product product = option.getProduct();
        System.out.println(gift.getSender().getName() + product.getName() + option.getName() + option.getQuantity());
    }
}
