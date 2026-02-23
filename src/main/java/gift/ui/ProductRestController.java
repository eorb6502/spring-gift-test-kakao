package gift.ui;

import gift.application.CreateProductRequest;
import gift.application.ProductService;
import gift.model.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/products")
@RestController
public class ProductRestController {
    private final ProductService productService;

    public ProductRestController(final ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product create(@RequestBody final CreateProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public List<Product> retrieve() {
        return productService.retrieve();
    }
}
