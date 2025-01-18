package at.thomas.ecosiotec.crawler;

import java.util.List;

public interface LinkCrawler {

    /**
     * Search '<a href></a>' tags on specified website.
     *
     * @param url   the urls of the site
     * @return  a list holding the links
     *
     * @throws Exception
     */
    List<String> crawl(final String url) throws Exception;

}
