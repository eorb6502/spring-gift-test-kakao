package gift;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("cucumber")
public class PostgresDatabaseCleaner implements DatabaseCleanerStrategy {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void clear() {
		entityManager.flush();
		entityManager.createNativeQuery("SET session_replication_role = 'replica'").executeUpdate();
		for (final String tableName : getTableNames()) {
			entityManager.createNativeQuery(
				"TRUNCATE TABLE \"" + tableName + "\" RESTART IDENTITY CASCADE"
			).executeUpdate();
		}
		entityManager.createNativeQuery("SET session_replication_role = 'origin'").executeUpdate();
	}

	@SuppressWarnings("unchecked")
	private List<String> getTableNames() {
		return entityManager.createNativeQuery(
			"SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
		).getResultList();
	}
}
