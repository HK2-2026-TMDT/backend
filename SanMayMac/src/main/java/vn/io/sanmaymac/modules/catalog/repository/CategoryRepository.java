package vn.io.sanmaymac.modules.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.catalog.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
}
