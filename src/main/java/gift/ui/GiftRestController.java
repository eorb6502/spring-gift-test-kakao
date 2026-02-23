package gift.ui;

import gift.application.GiftService;
import gift.application.GiveGiftRequest;
import gift.model.Gift;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gifts")
public class GiftRestController {
    private final GiftService giftService;

    public GiftRestController(final GiftService giftService) {
        this.giftService = giftService;
    }

    @GetMapping("/{id}")
    public Gift retrieveById(@PathVariable Long id) {
        return giftService.retrieveById(id);
    }

    @PostMapping
    public Gift give(@RequestBody GiveGiftRequest request, @RequestHeader("Member-Id") Long memberId) {
        return giftService.give(request, memberId);
    }
}