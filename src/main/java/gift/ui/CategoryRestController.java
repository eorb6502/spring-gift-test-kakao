package gift.ui;

import gift.application.CategoryService;
import gift.application.CreateCategoryRequest;
import gift.model.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/categories")
@RestController
public class CategoryRestController {
    private final CategoryService categoryService;

    public CategoryRestController(final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public Category create(@RequestBody final CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping
    public List<Category> retrieve() {
        return categoryService.retrieve();
    }
}
