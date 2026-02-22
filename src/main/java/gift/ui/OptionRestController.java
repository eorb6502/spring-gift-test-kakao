package gift.ui;

import gift.application.CreateOptionRequest;
import gift.application.OptionService;
import gift.model.Option;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/options")
@RestController
public class OptionRestController {
    private final OptionService optionService;

    public OptionRestController(final OptionService optionService) {
        this.optionService = optionService;
    }

    @PostMapping
    public Option create(@RequestBody final CreateOptionRequest request) {
        return optionService.create(request);
    }

    @GetMapping("/{id}")
    public Option retrieveById(@PathVariable Long id) {
        return optionService.retrieveById(id);
    }
}
