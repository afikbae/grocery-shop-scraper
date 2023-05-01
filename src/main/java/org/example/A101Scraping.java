package org.example;

import org.example.data.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class A101Scraping {
    public static void main(String[] args) throws IOException
    {
        Set<Product> scrapedProducts = Scrape();
        System.out.println(scrapedProducts);

        try {
            String fileName = "A101Products.txt";
            FileWriter writer = new FileWriter("src/main/java/org/example/" + fileName);
            for (Product p : scrapedProducts)
            {
                writer.write(p.toString() + "\n");
            }
            writer.close();
            System.out.println("Scraped data has been written to file " + fileName);
        } catch (IOException e) {
            System.out.println("An error occurred while writing the file");
            e.printStackTrace();
        }
    }

    public static Set<Product> Scrape ()
    {
        Document doc = null;
        String url = "https://www.a101.com.tr";
        String primaryEndPoint = "/online-alisveris";

        try {
            doc = Jsoup
                    .connect(url + primaryEndPoint)
                    .get();
        } catch (IOException e) {
            System.out.println("An error occured while trying to connect " + url + primaryEndPoint);
            e.printStackTrace();
        }


        //System.out.println(doc);

        Elements categories = doc.select(
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


        Set<Product> products = new HashSet<>();

        for (Element category : categories) {
            String categoryName = category.attr("href");
            String categoryLink = url + categoryName;
            Document categoryDoc = null;
            try {
                categoryDoc = Jsoup
                        .connect(categoryLink)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                        .header("Accept-Language", "*")
                        .get();
            } catch (IOException e) {
                System.out.println("An error occured while trying to connect " + categoryLink);
                e.printStackTrace();
            }

            Set<String> pagesDiscovered = new HashSet<>();
            Set<String> pagesToScrape = new HashSet<>();

            pagesToScrape.add(categoryLink);

            System.out.println("Started looking for another pages from " + categoryLink);

            findPages(pagesToScrape, categoryDoc, url, categoryName);

            System.out.println("Product Scraping Started for " + categoryName);

            int productCount = 0;

            for (String page : pagesToScrape)
            {
                if (pagesDiscovered.add(page))
                {
                    Document pageDoc = null;
                    try {
                        pageDoc = Jsoup
                                .connect(page)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                                .header("Accept-Language", "*")
                                .get();
                    } catch (IOException e) {
                        System.out.println("An error occured while trying to connect " + page);
                        e.printStackTrace();
                    }

                    Elements productsNamePrice = pageDoc.select("a.name-price");

                    for (Element productNamePrice : productsNamePrice) {
                        String name = productNamePrice.selectFirst("h3.name").text();
                        String price = productNamePrice.selectFirst("span.current").text();
                        Product product = new Product(name, price, categoryName);
                        products.add(product);
                        productCount++;
                    }
                }
            }

            System.out.println("Successfully scraped " + productCount + " products.");
        }

        return products;
    }

    public static void findPages (Set<String> pagesToScrape, Document doc, String url, String categoryName)
    {

        Elements pageLinks = doc.select("a.page-link");

        for (Element pageLink : pageLinks)
        {
            String page = pageLink.selectFirst("a.page-link").attr("href");
            if (page.startsWith("/market"))
            {
                page = "";
            }
            if (pagesToScrape.add(url + categoryName + page))
            {
                System.out.printf("found new page : " + url + categoryName + page + "\n");
                Document pageDoc = null;
                try {
                    pageDoc = Jsoup
                            .connect(url + categoryName + page)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                            .header("Accept-Language", "*")
                            .get();
                } catch (IOException e) {
                    System.out.println("An error occured while trying to connect " + url + categoryName + page);
                    e.printStackTrace();
                }
                findPages(pagesToScrape, pageDoc,url, categoryName);
            }
        }
    }
}