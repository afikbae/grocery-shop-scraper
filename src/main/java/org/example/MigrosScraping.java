package org.example;

import org.example.data.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MigrosScraping {
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

    public static Set<Product> Scrape () throws IOException
    {
        System.setProperty("webdriver.chrome.driver", "src/main/java/org/example/chromedriver.exe");

        // Initialize a new ChromeDriver instance
        WebDriver driver = new ChromeDriver();

        // Navigate to the target URL
        String url = "https://www.migros.com.tr";
        driver.get(url);
        driver.manage().window().maximize();

        // find the close button element
        // Find the div element with class "pop-over"
        WebElement popOverDiv = driver.findElement(By.cssSelector("div.popover"));

        // Find the fa-icon element within the div element
        WebElement faIcon = popOverDiv.findElement(By.cssSelector("fa-icon"));

        // Perform some action on the fa-icon element, for example click it
        faIcon.click();

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
        driver.quit();

        Document doc = Jsoup.parse(pageSource);

        //System.out.println(doc);

        Set<Product> products = new HashSet<>();

        Elements categories = doc.select("a.categories");

        //System.out.println(categories);

        for (Element category : categories) {

            String categoryName = category.attr("href");

            WebDriver driverCategory = new ChromeDriver();

            driverCategory.get(url + categoryName);

            // Wait for the page to load and JavaScript to execute
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Get the page's HTML source
            String pageSourceCategory = driverCategory.getPageSource();

            // Close the browser and WebDriver instance
            driverCategory.quit();

            Document categoryDoc = Jsoup.parse(pageSourceCategory);

            Elements productsNamePrice = categoryDoc.select("mat-card.mat-mdc-card");

            for (Element productNamePrice : productsNamePrice) {
                String name = productNamePrice.selectFirst("a.mat-caption").text();
                String price = productNamePrice.selectFirst("span.amount").text();
                Product product = new Product(name, price, categoryName);
                products.add(product);
            }
        }

        return products;
    }
}
