package uk.gov.companieshouse.officerssearch.subdelta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static final String NAMESPACE = "officers-search-consumer";

    public static void main(String[] args) {
        // TEST CHANGE
        SpringApplication.run(Application.class, args);
    }
}
