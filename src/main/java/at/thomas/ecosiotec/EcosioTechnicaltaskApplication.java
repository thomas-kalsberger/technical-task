package at.thomas.ecosiotec;

import at.thomas.ecosiotec.crawler.LinkCrawler;
import org.apache.commons.validator.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class EcosioTechnicaltaskApplication implements CommandLineRunner {

	// holds Ecosio url
	private static final String DEMO_URL = "https://www.ecosio.com";

	// injected link crawler implementation
	private final LinkCrawler linkCrawler;

	/**
	 * Create new instance
	 *
	 * @param webCrawler	injected implementation
	 */
	@Autowired
	public EcosioTechnicaltaskApplication(LinkCrawler webCrawler) {
		this.linkCrawler = webCrawler;
	}

	/**
	 * Program entry point
	 *
	 * @param args	commandline arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(EcosioTechnicaltaskApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// run crawler on ecosio site and print result
		UrlValidator urlValidator = new UrlValidator();
		if (urlValidator.isValid(DEMO_URL)) {
			List<String> links = this.linkCrawler.crawl(DEMO_URL);
			links.stream().sorted().forEach(System.out::println);
		}
		else System.out.println("invalid url: " + DEMO_URL);
	}
}
