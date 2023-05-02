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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class MigrosScraper
{
    public static void main(String[] args) {
        String chromeDriverPath = "src/main/java/org/example/chromedriver.exe";
        Set<Product> scrapedProducts = (new MigrosScraper(chromeDriverPath)).Scrape();
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

    private final String chromeDriverPath;
    private WebDriver driver;
    private WebDriverWait wait;

    private final String url = "https://www.migros.com.tr";

    public MigrosScraper (String chromeDriverPath)
    {
        this.chromeDriverPath = chromeDriverPath;
    }

    public Set<Product> Scrape ()
    {
        Set<Product> products = new HashSet<>();

        // initializing new chrome driver instance
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        driver = new ChromeDriver();

        // navigating to migros url
        driver.get(url);

        // maximizing window because some elements cannot be seen without maximizing
        driver.manage().window().maximize();

        // waiting page loading
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.popover")));

        closePopover();
        clickMenuButton();

        // get the main page's HTML source
        String mainPageSource = driver.getPageSource();
        Document mainPage = Jsoup.parse(mainPageSource, "UTF-8");

        // selecting the categories which interests bahceden app price prediction (3-12)
        Elements categories = mainPage.select("a.categories:gt(2):lt(12)");

        for (Element category : categories)
        {
            String categoryEndpoint = category.attr("href");
            driver.get(url + categoryEndpoint);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.filter__subcategories")));
            String categoryPageSource = driver.getPageSource();

            crawlInSubCategories(products, categoryPageSource);
        }

        driver.quit();
        return products;
    }

    private void closePopover ()
    {
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
    }

    private void clickMenuButton ()
    {
        WebElement menuButton = driver.findElement(By.cssSelector("div.categories-icon"));

        Actions action = new Actions(driver);
        action.moveToElement(menuButton).perform();
    }

    private void crawlInSubCategories (Set<Product> products, String categoryPageSource)
    {
        Elements categories = findSubCategories(categoryPageSource);

        for (Element subCategory : categories)
        {
            String subCategoryEndpoint = subCategory.attr("href");
            driver.get(url + subCategoryEndpoint);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("mat-card.mat-mdc-card")));
            String subCategoryPageSource = driver.getPageSource();
            Elements subCategories = findSubCategories(subCategoryPageSource);
            if (subCategories.size() <= 1)
            {
                crawlPagesAndScrape(products, subCategoryEndpoint);
            }
            else
            {
                crawlInSubCategories(products, subCategoryPageSource);
            }
        }
    }

    private Elements findSubCategories (String categoryPageSource)
    {
        Document categoryPage = Jsoup.parse(categoryPageSource, "UTF-8");
        return categoryPage
                .selectFirst("div.filter__subcategories")
                .select("div > div > a");
    }

    private void crawlPagesAndScrape (Set<Product> products, String subCategoryEndpoint)
    {
        Document subCategoryPage;
        WebElement nextPageButton = null;

        do
        {
            subCategoryPage = Jsoup.parse(driver.getPageSource());
            scrapeProducts(products, subCategoryPage, subCategoryEndpoint);

            try {
                nextPageButton = driver.findElement(By.id("pagination-button-next"));
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
                nextPageButton.click();
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("mat-card.mat-mdc-card")));
            } catch (Exception ignored) {

            }
        }
        while (nextPageButton != null && nextPageButton.getAttribute("disabled") == null);

        subCategoryPage = Jsoup.parse(driver.getPageSource());
        scrapeProducts(products, subCategoryPage, subCategoryEndpoint);
    }

    private void scrapeProducts (Set<Product> products, Document categoryDoc, String subCategoryEndpoint)
    {
        Elements productsNamePrice = categoryDoc.select("mat-card.mat-mdc-card");

        for (Element productNamePrice : productsNamePrice) {
            String name = productNamePrice.selectFirst("a.mat-caption").text();
            String price = productNamePrice.selectFirst("span.amount").text();
            String categoryName = subCategoryEndpoint.substring(1, subCategoryEndpoint.indexOf("-c-"));
            Product product = new Product(name, price, categoryName);
            products.add(product);
            System.out.println(product);
        }
    }
}
