package gift.application;

import gift.model.Gift;
import gift.model.GiftDelivery;
import gift.model.GiftRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class GiftService {
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final GiftRepository giftRepository;
    private final GiftDelivery giftDelivery;

    public GiftService(
        final OptionRepository optionRepository,
        final MemberRepository memberRepository,
        final GiftRepository giftRepository,
        final GiftDelivery giftDelivery
    ) {
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.giftRepository = giftRepository;
        this.giftDelivery = giftDelivery;
    }

    @Transactional(readOnly = true)
    public Gift retrieveById(final Long id) {
        return giftRepository.findById(id).orElseThrow();
    }

    public Gift give(final GiveGiftRequest request, final Long memberId) {
        final Member sender = memberRepository.findById(memberId).orElseThrow();
        final Member receiver = memberRepository.findById(request.getReceiverId()).orElseThrow();
        final Option option = optionRepository.findById(request.getOptionId()).orElseThrow();
        option.decrease(request.getQuantity());
        final Gift gift = new Gift(sender, receiver, option, request.getQuantity(), request.getMessage());
        giftRepository.save(gift);
        giftDelivery.deliver(gift);
        return gift;
    }
}
