package at.thomas.ecosiotec.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchLinksTask implements Callable<List<String>> {

    // constants
    private static final String ECOSIO_DOMAIN = "https://ecosio";
    private static final String LINK_PATTERN  = "<a href=\"(.*?)\"";

    // predicates for checking link validity
    private static final Predicate<? super Element> validLinkElementPredicate = link -> {
        String ref = link.attr("href");
        return ref.contains(ECOSIO_DOMAIN) && !ref.contains(".pdf") && !ref.contains(".png")
                && !ref.contains(".jpeg") && !ref.contains(".jpg")
                && !ref.contains("download");
    };

    private static final Predicate<String> validLinkPredicate =
            ref -> ref.contains(ECOSIO_DOMAIN) && !ref.contains(".pdf") && !ref.contains(".png")
                    && !ref.contains(".jpeg") && !ref.contains(".jpg")
                    && !ref.contains("download");


    // holds the url which will be processed
    private final String url;

    /**
     * Create new instance.
     *
     * @param url   the url, which will be processed in this task
     */
    public SearchLinksTask(String url) {
        this.url = url;
    }

    @Override
    public List<String> call()  {
        return this.collectWithParser();
    }

    /*
     * Collect links with Jsoup parser
     */
    private List<String> collectWithParser() {
        try {
            // read page into parser
            Connection.Response response = Jsoup.connect(url).timeout(10 * 1000).execute();
            if(response.statusCode() == 200) {
                Document document = response.parse();
                return document.select("a[href]")
                        .stream()
                        .filter(validLinkElementPredicate)
                        .map(link -> link.attr("href"))
                        .distinct()
                        .toList();
            }
            else {
                // log invalid status and return empty list
                System.out.println(response.statusCode() + " " + response.statusMessage());
                return Collections.emptyList();
            }
        } catch(Exception ex) {
            return Collections.emptyList();
        }
    }

    /*
     * Possible solution using regular-expressions.
     *
     * Regular expressions are not good for parsing HTML (see Chomsky normal form)
     */
    private List<String> collectWithRegex() {
        Set<String> links = new HashSet<>();    // holds links
        URL siteUrl;
        try {
            // open site and read into buffer
            siteUrl = new URL(this.url);
            URLConnection con = siteUrl.openConnection();
            InputStream in = con.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            // create regular expression for link extraction and apply on buffer
            Pattern p = Pattern.compile(LINK_PATTERN);
            Matcher m = p.matcher(sb.toString());
            while(m.find()) {
                links.add(this.extractLink(m.group()));
            }
            // return result (skip images etc. )
            return links.stream()
                    .filter(validLinkPredicate)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Extract link from string
     */
    private String extractLink(final String link) {
        if(link != null) {
            int startIdx = link.indexOf("\"");
            int endIdx = link.indexOf("\"", ++startIdx);
            return link.substring(startIdx, endIdx);
        }
        else {
            // log error and throw exception
            throw new RuntimeException("link can not be null");
        }
    }
}
