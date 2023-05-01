package org.example.data;

import java.util.Objects;

public class Product {
    private String name;
    private String price;
    private String category;
    public Product(String name, String price, String category) {
        this.name = name;
        this.price = price;
        this.category = category;
    }


    @Override
    public String toString() {
        return String.format("{\n" +
                "   \'category\':\'%s\', \n" +
                "   \'name\':\'%s\', \n" +
                "   \'price\':\'%s\'\n" +
                "}", category, name, price);
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object obj) {
        return this.name.equals(((Product)obj).getName()) && this.price.equals(((Product)obj).getPrice());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.price);
    }
}