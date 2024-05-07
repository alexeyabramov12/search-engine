package searchengine.service.indexing.sitemap;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import searchengine.model.site.Site;

import java.io.IOException;

public interface SiteMapService {

    boolean exists(String path, Site site);

    void createSiteMap(Document doc, int statusCode, String path, Site site);

    void updateSite(Site site);

    boolean createOrUpdatePage(Connection.Response response, String path, Site site) throws IOException;

}
