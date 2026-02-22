package gift;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleaner {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void clear() {
		entityManager.flush();
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
		for (final String tableName : getTableNames()) {
			entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
			entityManager.createNativeQuery(
				"ALTER TABLE " + tableName + " ALTER COLUMN ID RESTART WITH 1"
			).executeUpdate();
		}
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
	}

	private List<String> getTableNames() {
		return entityManager.getMetamodel().getEntities().stream()
			.map(this::getTableName)
			.toList();
	}

	private String getTableName(EntityType<?> entity) {
		Class<?> javaType = entity.getJavaType();
		Table table = javaType.getAnnotation(Table.class);
		if (table != null && !table.name().isEmpty()) {
			return table.name();
		}
		Entity entityAnnotation = javaType.getAnnotation(Entity.class);
		if (entityAnnotation != null && !entityAnnotation.name().isEmpty()) {
			return entityAnnotation.name();
		}
		return entity.getName();
	}
}
