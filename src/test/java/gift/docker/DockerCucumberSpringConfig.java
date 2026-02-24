package gift.docker;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("docker")
public class DockerCucumberSpringConfig {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @io.cucumber.java.Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
        clearDatabase();
    }

    private void clearDatabase() {
        List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'",
                String.class
        );
        jdbcTemplate.execute("SET session_replication_role = 'replica'");
        for (String tableName : tableNames) {
            jdbcTemplate.execute("TRUNCATE TABLE \"" + tableName + "\" RESTART IDENTITY CASCADE");
        }
        jdbcTemplate.execute("SET session_replication_role = 'origin'");
    }
}
