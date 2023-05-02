package org.example;

import org.example.data.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MigrosScrapingOld {
    public static void main(String[] args) throws  IOException
    {
        Set<Product> scrapedProducts = Scrape();
        System.out.println(scrapedProducts);

        try {
            FileWriter writer = new FileWriter("src/main/java/org/example/MigrosProducts.txt");
            for (Product p : scrapedProducts)
            {
                writer.write(p.toString() + "\n");
            }
            writer.close();
            System.out.println("Scraped data has been written to file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing the file.");
            e.printStackTrace();
        }
    }

    public static Set<Product> Scrape ()
    {
        System.setProperty("webdriver.chrome.driver", "src/main/java/org/example/chromedriver.exe");

        // Initialize a new ChromeDriver instance
        WebDriver driver = new ChromeDriver();

        // Navigate to the target URL
        String url = "https://www.migros.com.tr";
        driver.get(url);
        driver.manage().window().maximize();


        while (true) {
            try {
                WebElement popOverDiv = driver.findElement(By.cssSelector("div.popover"));
                WebElement faIcon = popOverDiv.findElement(By.cssSelector("fa-icon"));
                faIcon.click();
                break;
            } catch (Exception e) {
                System.out.println("Unable to close popover, trying again");
            }
        }
        WebElement menuButton = driver.findElement(By.cssSelector("div.categories-icon"));

        Actions action = new Actions(driver);
        action.moveToElement(menuButton).perform();

        // Wait for the page to load and JavaScript to execute
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get the page's HTML source
        String pageSource = driver.getPageSource();

        // Close the browser and WebDriver instance

        Document doc = Jsoup.parse(pageSource);

        //System.out.println(doc);

        Set<Product> products = new HashSet<>();

        Elements categories = doc.select("a.categories:gt(2):lt(12)");

        //System.out.println(categories);

        for (Element category : categories) {

            String categoryName = category.attr("href");

            System.out.println(categoryName);

            driver.get(url + categoryName);

            // Wait for the page to load and JavaScript to execute
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Get the page's HTML source
            List<String> categoryPageSources = new ArrayList<>();
            categoryPageSources.add(driver.getPageSource());

            WebElement nextPageButton = null;

            int page = -1;

            do {
                page++;

                nextPageButton = driver.findElement(By.id("pagination-button-next"));

                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");



                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                nextPageButton.click();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Document categoryDoc = Jsoup.parse(driver.getPageSource());

                Elements productsNamePrice = categoryDoc.select("mat-card.mat-mdc-card");

                for (Element productNamePrice : productsNamePrice) {
                    String name = productNamePrice.selectFirst("a.mat-caption").text();
                    String price = productNamePrice.selectFirst("span.amount").text();
                    Product product = new Product(name, price, categoryName);
                    products.add(product);
                }

                System.out.println(url + categoryName + "__" + page);

//                try {
//                    String filename = "src/main/java/org/example/Migros/htmls/" + (url + categoryName).replaceFirst("https?://", "").replace('.','_').replace('/','_') + "__" +page + ".html";
//                    FileWriter writer = new FileWriter(filename);
//                    writer.write(driver.getPageSource());
//                    writer.close();
//                    System.out.println("Page source saved to file " + filename);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

            } while (nextPageButton.getAttribute("disabled") == null);

//            for (String categoryPageSource : categoryPageSources)
//            {
//                Document categoryDoc = Jsoup.parse(categoryPageSource);
//
//                Elements productsNamePrice = categoryDoc.select("mat-card.mat-mdc-card");
//
//                for (Element productNamePrice : productsNamePrice) {
//                    String name = productNamePrice.selectFirst("a.mat-caption").text();
//                    String price = productNamePrice.selectFirst("span.amount").text();
//                    Product product = new Product(name, price, categoryName);
//                    products.add(product);
//                }
//            }
        }

        driver.quit();

        return products;
    }
}
