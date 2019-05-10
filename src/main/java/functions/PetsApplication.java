package functions;

import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class PetsApplication {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Bean
	public Function<String, String> pets() {
		return s -> {
			List<String> pets = jdbcTemplate.queryForList(
				"select p.name from pets p where p.type_id in (select t.id from types t where t.name = ?)",
				String.class, s);
			return pets.toString();
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(PetsApplication.class, args);
	}

}
