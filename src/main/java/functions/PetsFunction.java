package functions;

import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PetsFunction {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Bean
	public Function<String, String> pets() {
		return s -> {
			List<String> pets = jdbcTemplate.queryForList(
				"select p.name from pets p" +
				" where p.type_id in (select t.id from types t where t.name = ?)",
				String.class, s);
			return pets.toString();
		};
	}
}
