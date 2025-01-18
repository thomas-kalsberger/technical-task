package at.thomas.ecosiotec.crawler;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class LinkCrawlerImpl implements LinkCrawler {

    // number of threads that will be executed in parallel
    private static final int PARTITION_SIZE = 10;

    // thread pool
    private final ExecutorService threadPool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    // holds collected links
    private final Set<String> result = new HashSet<>();

    @Override
    public List<String> crawl(String url) throws Exception {
        // get the top-level links
        List<String> topLevelLinks = this.getTopLevelLinks(url);
        // split into equals size partitions
        List<List<String>> partitions = partitionLinks(topLevelLinks, PARTITION_SIZE);
        // process partitions
        for(List<String> partition : partitions) {
            // each partition is converted into an list of tasks
            List<SearchLinksTask> tasks = toLinkCollectionTasks(partition);
            try {
                // execute tasks in parallel and collect intermediate results
                List<Future<List<String>>> futures = threadPool.invokeAll(tasks);
                for(Future<List<String>> future : futures) {
                    if(future.isDone()) {
                        result.addAll(future.get());
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // terminate threadpool and return result
        this.threadPool.shutdown();
        this.threadPool.awaitTermination(5, TimeUnit.SECONDS);
        return new ArrayList<>(result);
    }

    /*
     * Convert urls to list of tasks, which will be executed in threadpool.
     */
    private List<SearchLinksTask> toLinkCollectionTasks(List<String> partition) {
        List<SearchLinksTask> tasks = new ArrayList<>();
        for(String link : partition) {
            tasks.add(new SearchLinksTask(link));
        }
        return tasks;
    }

    /*
     * Partition links into arrays of same size
     */
    private List<List<String>> partitionLinks(final List<String> links, final int partitionSize) {
        List<List<String>> partitions = new ArrayList<>();
        int startIdx = 0;
        int endIdx = 0;
        for(int i = 0; i <= links.size()/partitionSize; i++) {
            startIdx = i*partitionSize;
            endIdx = startIdx+partitionSize;
            if(endIdx > links.size())
                endIdx = links.size();
            partitions.add(new ArrayList<>(links.subList(startIdx, endIdx)));
        }
        return partitions;
    }

    /*
     * Collect top-level links from site
     */
    private List<String> getTopLevelLinks(final String url) throws Exception {
        return new SearchLinksTask(url).call();
    }
}
