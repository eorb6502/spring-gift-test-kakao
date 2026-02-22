package gift.application;

import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public Option create(final CreateOptionRequest request) {
        final Product product = productRepository.findById(request.getProductId()).orElseThrow();
        return optionRepository.save(new Option(request.getName(), request.getQuantity(), product));
    }

    public List<Option> retrieve() {
        return optionRepository.findAll();
    }

    public Option retrieveById(final Long id) {
        return optionRepository.findById(id).orElseThrow();
    }
}
