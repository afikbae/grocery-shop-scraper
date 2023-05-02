package org.example;

import org.example.data.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class A101Scraper
{
    public static void main(String[] args) {
        Set<Product> scrapedProducts = (new A101Scraper()).Scrape();
        System.out.println(scrapedProducts);

        if (!scrapedProducts.isEmpty())
        {
            try {
                String fileName = "src/main/java/org/example/MigrosProducts.txt";
                FileWriter writer = new FileWriter(fileName);
                for (Product p : scrapedProducts) {
                    writer.write(p.toString() + "\n");
                }
                writer.close();
                System.out.println("Scraped data has been written to file " + fileName);
            } catch (IOException e) {
                System.out.println("An error occurred while writing the file.");
                e.printStackTrace();
            }
        }
    }

    private final String url = "https://www.a101.com.tr";
    private final String primaryEndPoint = "/online-alisveris";

    public A101Scraper ()
    {

    }

    public Set<Product> Scrape ()
    {
        Set<Product> products = new HashSet<>();

        Document doc = getDocumentFrom(url + primaryEndPoint);
        Elements categories = selectRelatedCategories(doc);

        for (Element category : categories)
        {
            String categoryEndpoint = category.attr("href");
            String categoryUrl = url + categoryEndpoint;
            Document categoryDoc = getDocumentFrom(categoryUrl);

            Set<String> pagesDiscovered = new HashSet<>();
            Set<String> pagesToScrape = new HashSet<>();

            pagesToScrape.add(categoryUrl);

            System.out.println("Started looking for another pages from " + categoryUrl);

            findPages(pagesToScrape, categoryDoc, categoryUrl);

            System.out.println("Product Scraping Started for " + categoryUrl);

            for (String page : pagesToScrape)
            {
                if (!pagesDiscovered.add(page))
                {
                    break;
                }
                Document pageDoc = getDocumentFrom(page);

                scrapeProducts(products, pageDoc, categoryEndpoint.replace("market", "").replace("/", ""));
            }
        }

        return products;
    }

    private Elements selectRelatedCategories (Document doc)
    {
        return doc.select(
                "[href^=\"/market/\"]:" +
                        "not([href$=\"atistirmalik/\"]," +
                        "[href$=\"ambalaj-malzemeleri/\"]," +
                        "[href$=\"temel-gida/\"]," +
                        "[href$=\"/market/\"]," +
                        "[href$=\"/market/\"]," +
                        "[href$=\"saglikli-yasam-urunleri/\"]," +
                        "[href$=\"icecek/\"]," +
                        "[href$=\"kahvaltilik-sut-urunleri/\"]," +
                        "[href$=\"ev-bakim-temizlik/\"])");
    }

    private Document getDocumentFrom (String url)
    {
        try {
            return Jsoup
                    .connect(url)
                    .get();
        } catch (IOException e) {
            System.out.println("An error occured while trying to connect " + url);
            throw new RuntimeException(e);
        }
    }

    private void findPages (Set<String> pagesToScrape, Document doc, String categoryUrl)
    {

        Elements pageLinks = doc.select("a.page-link");

        for (Element pageLink : pageLinks)
        {
            String page = pageLink.selectFirst("a.page-link").attr("href");
            if (page.startsWith("/market"))
            {
                page = "";
            }
            String newUrl = categoryUrl + page;
            if (pagesToScrape.add(newUrl))
            {
                System.out.printf("found new page : " + newUrl + "\n");
                Document pageDoc = getDocumentFrom( newUrl);
                findPages(pagesToScrape, pageDoc, categoryUrl);
            }
        }
    }

    private void scrapeProducts (Set<Product> products, Document pageDoc, String categoryName)
    {
        Elements productsNamePrice = pageDoc.select("a.name-price");

        for (Element productNamePrice : productsNamePrice) {
            String name = productNamePrice.selectFirst("h3.name").text();
            String price = productNamePrice.selectFirst("span.current").text();
            Product product = new Product(name, price, categoryName);
            products.add(product);
            System.out.println(product);
        }
    }
}
