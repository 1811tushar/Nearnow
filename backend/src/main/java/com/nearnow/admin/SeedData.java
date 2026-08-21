package com.nearnow.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Full, programmatically-generated Java-port of seed_data.dart —
 * parsed directly from the verified source file, not hand-transcribed.
 * 12 categories, 96 products, counts verified against the original.
 */
public class SeedData {

    public record CategorySeed(String name, String imageUrl, int sortOrder) {}

    public record ProductSeed(String name, BigDecimal price, BigDecimal salePrice,
                               String unit, int stock, double rating, boolean isFeatured, String imageUrl) {}

    public static final List<CategorySeed> SEED_CATEGORIES = List.of(
            new CategorySeed("Fruits & Vegetables", "https://picsum.photos/seed/vegetables/300/300", 1),
            new CategorySeed("Dairy & Breakfast", "https://picsum.photos/seed/milk/300/300", 2),
            new CategorySeed("Snacks", "https://picsum.photos/seed/chips/300/300", 3),
            new CategorySeed("Beverages", "https://picsum.photos/seed/juice/300/300", 4),
            new CategorySeed("Bakery", "https://picsum.photos/seed/bakery/300/300", 5),
            new CategorySeed("Atta, Rice & Dals", "https://picsum.photos/seed/rice/300/300", 6),
            new CategorySeed("Masala & Dry Fruits", "https://picsum.photos/seed/spices/300/300", 7),
            new CategorySeed("Frozen Food", "https://picsum.photos/seed/frozenfood/300/300", 8),
            new CategorySeed("Cleaning Essentials", "https://picsum.photos/seed/cleaning/300/300", 9),
            new CategorySeed("Personal Care", "https://picsum.photos/seed/cosmetics/300/300", 10),
            new CategorySeed("Baby Care", "https://picsum.photos/seed/babyproducts/300/300", 11),
            new CategorySeed("Pet Care", "https://picsum.photos/seed/petfood/300/300", 12)
    );

    public static final Map<String, List<ProductSeed>> SEED_PRODUCTS_BY_CATEGORY = Map.ofEntries(
            Map.entry("Fruits & Vegetables", List.of(
                    new ProductSeed("Fresh Banana", new BigDecimal("40.0"), new BigDecimal("0.0"), "6 pcs", 40, 3.8, true, "https://picsum.photos/seed/banana/400/400"),
                    new ProductSeed("Royal Gala Apple", new BigDecimal("180.0"), new BigDecimal("160.0"), "4 pcs", 47, 4.0, false, "https://picsum.photos/seed/apple/400/400"),
                    new ProductSeed("Alphonso Mango", new BigDecimal("350.0"), new BigDecimal("300.0"), "1 kg", 54, 4.3, false, "https://picsum.photos/seed/mango/400/400"),
                    new ProductSeed("Tomato", new BigDecimal("30.0"), new BigDecimal("0.0"), "500 g", 61, 4.5, false, "https://picsum.photos/seed/tomato/400/400"),
                    new ProductSeed("Onion", new BigDecimal("35.0"), new BigDecimal("0.0"), "1 kg", 68, 4.8, false, "https://picsum.photos/seed/onion/400/400"),
                    new ProductSeed("Potato", new BigDecimal("28.0"), new BigDecimal("0.0"), "1 kg", 75, 3.8, false, "https://picsum.photos/seed/potato/400/400"),
                    new ProductSeed("Spinach Bunch", new BigDecimal("20.0"), new BigDecimal("0.0"), "250 g", 82, 4.0, false, "https://picsum.photos/seed/spinach/400/400"),
                    new ProductSeed("Cucumber", new BigDecimal("25.0"), new BigDecimal("20.0"), "500 g", 89, 4.3, false, "https://picsum.photos/seed/cucumber/400/400")
            )),
            Map.entry("Dairy & Breakfast", List.of(
                    new ProductSeed("Toned Milk", new BigDecimal("32.0"), new BigDecimal("0.0"), "500 ml", 40, 3.8, true, "https://picsum.photos/seed/milk/400/400"),
                    new ProductSeed("Farm Fresh Eggs", new BigDecimal("72.0"), new BigDecimal("65.0"), "6 pcs", 47, 4.0, false, "https://picsum.photos/seed/eggs/400/400"),
                    new ProductSeed("Butter", new BigDecimal("54.0"), new BigDecimal("0.0"), "100 g", 54, 4.3, false, "https://picsum.photos/seed/butter/400/400"),
                    new ProductSeed("Paneer", new BigDecimal("90.0"), new BigDecimal("80.0"), "200 g", 61, 4.5, false, "https://picsum.photos/seed/paneer/400/400"),
                    new ProductSeed("Curd", new BigDecimal("45.0"), new BigDecimal("0.0"), "400 g", 68, 4.8, false, "https://picsum.photos/seed/yogurt/400/400"),
                    new ProductSeed("Cheese Slices", new BigDecimal("120.0"), new BigDecimal("110.0"), "10 slices", 75, 3.8, false, "https://picsum.photos/seed/cheese/400/400"),
                    new ProductSeed("Corn Flakes", new BigDecimal("175.0"), new BigDecimal("150.0"), "475 g", 82, 4.0, false, "https://picsum.photos/seed/cornflakes/400/400"),
                    new ProductSeed("Bread", new BigDecimal("45.0"), new BigDecimal("0.0"), "400 g", 89, 4.3, false, "https://picsum.photos/seed/bread/400/400")
            )),
            Map.entry("Snacks", List.of(
                    new ProductSeed("Potato Chips", new BigDecimal("20.0"), new BigDecimal("0.0"), "52 g", 40, 3.8, true, "https://picsum.photos/seed/chips/400/400"),
                    new ProductSeed("Salted Namkeen", new BigDecimal("55.0"), new BigDecimal("45.0"), "200 g", 47, 4.0, false, "https://picsum.photos/seed/namkeen/400/400"),
                    new ProductSeed("Chocolate Cookies", new BigDecimal("40.0"), new BigDecimal("35.0"), "150 g", 54, 4.3, false, "https://picsum.photos/seed/cookies/400/400"),
                    new ProductSeed("Peanut Butter", new BigDecimal("210.0"), new BigDecimal("190.0"), "340 g", 61, 4.5, false, "https://picsum.photos/seed/peanutbutter/400/400"),
                    new ProductSeed("Popcorn", new BigDecimal("60.0"), new BigDecimal("0.0"), "80 g", 68, 4.8, false, "https://picsum.photos/seed/popcorn/400/400"),
                    new ProductSeed("Chocolate Bar", new BigDecimal("90.0"), new BigDecimal("80.0"), "100 g", 75, 3.8, false, "https://picsum.photos/seed/chocolate/400/400"),
                    new ProductSeed("Instant Noodles", new BigDecimal("56.0"), new BigDecimal("50.0"), "280 g", 82, 4.0, false, "https://picsum.photos/seed/noodles/400/400"),
                    new ProductSeed("Muesli Bar", new BigDecimal("30.0"), new BigDecimal("0.0"), "40 g", 89, 4.3, false, "https://picsum.photos/seed/granolabar/400/400")
            )),
            Map.entry("Beverages", List.of(
                    new ProductSeed("Orange Juice", new BigDecimal("110.0"), new BigDecimal("99.0"), "1 L", 40, 3.8, true, "https://picsum.photos/seed/orangejuice/400/400"),
                    new ProductSeed("Cola", new BigDecimal("40.0"), new BigDecimal("0.0"), "750 ml", 47, 4.0, false, "https://picsum.photos/seed/cola/400/400"),
                    new ProductSeed("Green Tea", new BigDecimal("150.0"), new BigDecimal("130.0"), "25 bags", 54, 4.3, false, "https://picsum.photos/seed/greentea/400/400"),
                    new ProductSeed("Instant Coffee", new BigDecimal("260.0"), new BigDecimal("230.0"), "100 g", 61, 4.5, false, "https://picsum.photos/seed/coffee/400/400"),
                    new ProductSeed("Mineral Water", new BigDecimal("20.0"), new BigDecimal("0.0"), "1 L", 68, 4.8, false, "https://picsum.photos/seed/water/400/400"),
                    new ProductSeed("Energy Drink", new BigDecimal("110.0"), new BigDecimal("0.0"), "250 ml", 75, 3.8, false, "https://picsum.photos/seed/energydrink/400/400"),
                    new ProductSeed("Mango Shake Mix", new BigDecimal("175.0"), new BigDecimal("160.0"), "500 g", 82, 4.0, false, "https://picsum.photos/seed/mangoshake/400/400"),
                    new ProductSeed("Lemonade", new BigDecimal("45.0"), new BigDecimal("0.0"), "500 ml", 89, 4.3, false, "https://picsum.photos/seed/lemonade/400/400")
            )),
            Map.entry("Bakery", List.of(
                    new ProductSeed("Croissant", new BigDecimal("65.0"), new BigDecimal("0.0"), "2 pcs", 40, 3.8, true, "https://picsum.photos/seed/croissant/400/400"),
                    new ProductSeed("Chocolate Cake", new BigDecimal("320.0"), new BigDecimal("280.0"), "500 g", 47, 4.0, false, "https://picsum.photos/seed/cake/400/400"),
                    new ProductSeed("Burger Buns", new BigDecimal("48.0"), new BigDecimal("0.0"), "6 pcs", 54, 4.3, false, "https://picsum.photos/seed/buns/400/400"),
                    new ProductSeed("Doughnut", new BigDecimal("90.0"), new BigDecimal("75.0"), "4 pcs", 61, 4.5, false, "https://picsum.photos/seed/doughnut/400/400"),
                    new ProductSeed("Pav Bread", new BigDecimal("35.0"), new BigDecimal("0.0"), "8 pcs", 68, 4.8, false, "https://picsum.photos/seed/pav/400/400"),
                    new ProductSeed("Muffins", new BigDecimal("120.0"), new BigDecimal("100.0"), "4 pcs", 75, 3.8, false, "https://picsum.photos/seed/muffin/400/400"),
                    new ProductSeed("Rusk", new BigDecimal("55.0"), new BigDecimal("0.0"), "200 g", 82, 4.0, false, "https://picsum.photos/seed/rusk/400/400"),
                    new ProductSeed("Garlic Bread", new BigDecimal("85.0"), new BigDecimal("75.0"), "250 g", 89, 4.3, false, "https://picsum.photos/seed/garlicbread/400/400")
            )),
            Map.entry("Atta, Rice & Dals", List.of(
                    new ProductSeed("Wheat Atta", new BigDecimal("260.0"), new BigDecimal("240.0"), "5 kg", 40, 3.8, true, "https://picsum.photos/seed/wheatflour/400/400"),
                    new ProductSeed("Basmati Rice", new BigDecimal("320.0"), new BigDecimal("290.0"), "5 kg", 47, 4.0, false, "https://picsum.photos/seed/rice/400/400"),
                    new ProductSeed("Toor Dal", new BigDecimal("165.0"), new BigDecimal("150.0"), "1 kg", 54, 4.3, false, "https://picsum.photos/seed/lentils/400/400"),
                    new ProductSeed("Moong Dal", new BigDecimal("145.0"), new BigDecimal("130.0"), "1 kg", 61, 4.5, false, "https://picsum.photos/seed/moongdal/400/400"),
                    new ProductSeed("Chana Dal", new BigDecimal("110.0"), new BigDecimal("0.0"), "1 kg", 68, 4.8, false, "https://picsum.photos/seed/chickpeas/400/400"),
                    new ProductSeed("Rajma", new BigDecimal("130.0"), new BigDecimal("115.0"), "1 kg", 75, 3.8, false, "https://picsum.photos/seed/kidneybeans/400/400"),
                    new ProductSeed("Poha", new BigDecimal("55.0"), new BigDecimal("0.0"), "500 g", 82, 4.0, false, "https://picsum.photos/seed/poha/400/400"),
                    new ProductSeed("Sooji", new BigDecimal("48.0"), new BigDecimal("0.0"), "500 g", 89, 4.3, false, "https://picsum.photos/seed/semolina/400/400")
            )),
            Map.entry("Masala & Dry Fruits", List.of(
                    new ProductSeed("Turmeric Powder", new BigDecimal("45.0"), new BigDecimal("0.0"), "200 g", 40, 3.8, true, "https://picsum.photos/seed/turmeric/400/400"),
                    new ProductSeed("Red Chilli Powder", new BigDecimal("60.0"), new BigDecimal("55.0"), "200 g", 47, 4.0, false, "https://picsum.photos/seed/chilipowder/400/400"),
                    new ProductSeed("Garam Masala", new BigDecimal("85.0"), new BigDecimal("75.0"), "100 g", 54, 4.3, false, "https://picsum.photos/seed/spices/400/400"),
                    new ProductSeed("Almonds", new BigDecimal("480.0"), new BigDecimal("430.0"), "500 g", 61, 4.5, false, "https://picsum.photos/seed/almonds/400/400"),
                    new ProductSeed("Cashews", new BigDecimal("560.0"), new BigDecimal("500.0"), "500 g", 68, 4.8, false, "https://picsum.photos/seed/cashewnuts/400/400"),
                    new ProductSeed("Raisins", new BigDecimal("120.0"), new BigDecimal("105.0"), "250 g", 75, 3.8, false, "https://picsum.photos/seed/raisins/400/400"),
                    new ProductSeed("Cumin Seeds", new BigDecimal("95.0"), new BigDecimal("0.0"), "100 g", 82, 4.0, false, "https://picsum.photos/seed/cumin/400/400"),
                    new ProductSeed("Black Pepper", new BigDecimal("130.0"), new BigDecimal("115.0"), "100 g", 89, 4.3, false, "https://picsum.photos/seed/pepper/400/400")
            )),
            Map.entry("Frozen Food", List.of(
                    new ProductSeed("Frozen Peas", new BigDecimal("65.0"), new BigDecimal("0.0"), "500 g", 40, 3.8, true, "https://picsum.photos/seed/peas/400/400"),
                    new ProductSeed("Veg Nuggets", new BigDecimal("145.0"), new BigDecimal("130.0"), "425 g", 47, 4.0, false, "https://picsum.photos/seed/nuggets/400/400"),
                    new ProductSeed("Frozen Paratha", new BigDecimal("95.0"), new BigDecimal("85.0"), "5 pcs", 54, 4.3, false, "https://picsum.photos/seed/paratha/400/400"),
                    new ProductSeed("French Fries", new BigDecimal("110.0"), new BigDecimal("99.0"), "425 g", 61, 4.5, false, "https://picsum.photos/seed/frenchfries/400/400"),
                    new ProductSeed("Ice Cream Tub", new BigDecimal("250.0"), new BigDecimal("220.0"), "700 ml", 68, 4.8, false, "https://picsum.photos/seed/icecream/400/400"),
                    new ProductSeed("Momos", new BigDecimal("130.0"), new BigDecimal("115.0"), "250 g", 75, 3.8, false, "https://picsum.photos/seed/dumplings/400/400"),
                    new ProductSeed("Veg Spring Roll", new BigDecimal("140.0"), new BigDecimal("0.0"), "300 g", 82, 4.0, false, "https://picsum.photos/seed/springroll/400/400"),
                    new ProductSeed("Frozen Corn", new BigDecimal("70.0"), new BigDecimal("0.0"), "500 g", 89, 4.3, false, "https://picsum.photos/seed/corn/400/400")
            )),
            Map.entry("Cleaning Essentials", List.of(
                    new ProductSeed("Dish Wash Liquid", new BigDecimal("99.0"), new BigDecimal("89.0"), "500 ml", 40, 3.8, true, "https://picsum.photos/seed/dishsoap/400/400"),
                    new ProductSeed("Floor Cleaner", new BigDecimal("175.0"), new BigDecimal("150.0"), "1 L", 47, 4.0, false, "https://picsum.photos/seed/floorcleaner/400/400"),
                    new ProductSeed("Laundry Detergent", new BigDecimal("210.0"), new BigDecimal("190.0"), "1 kg", 54, 4.3, false, "https://picsum.photos/seed/detergent/400/400"),
                    new ProductSeed("Toilet Cleaner", new BigDecimal("95.0"), new BigDecimal("0.0"), "500 ml", 61, 4.5, false, "https://picsum.photos/seed/toiletcleaner/400/400"),
                    new ProductSeed("Glass Cleaner", new BigDecimal("110.0"), new BigDecimal("99.0"), "500 ml", 68, 4.8, false, "https://picsum.photos/seed/glasscleaner/400/400"),
                    new ProductSeed("Garbage Bags", new BigDecimal("85.0"), new BigDecimal("0.0"), "30 pcs", 75, 3.8, false, "https://picsum.photos/seed/garbagebags/400/400"),
                    new ProductSeed("Scrub Pad", new BigDecimal("30.0"), new BigDecimal("0.0"), "3 pcs", 82, 4.0, false, "https://picsum.photos/seed/scrubpad/400/400"),
                    new ProductSeed("Air Freshener", new BigDecimal("160.0"), new BigDecimal("140.0"), "300 ml", 89, 4.3, false, "https://picsum.photos/seed/airfreshener/400/400")
            )),
            Map.entry("Personal Care", List.of(
                    new ProductSeed("Shampoo", new BigDecimal("210.0"), new BigDecimal("185.0"), "340 ml", 40, 3.8, true, "https://picsum.photos/seed/shampoo/400/400"),
                    new ProductSeed("Body Wash", new BigDecimal("199.0"), new BigDecimal("175.0"), "250 ml", 47, 4.0, false, "https://picsum.photos/seed/bodywash/400/400"),
                    new ProductSeed("Toothpaste", new BigDecimal("95.0"), new BigDecimal("0.0"), "150 g", 54, 4.3, false, "https://picsum.photos/seed/toothpaste/400/400"),
                    new ProductSeed("Hand Sanitizer", new BigDecimal("60.0"), new BigDecimal("50.0"), "100 ml", 61, 4.5, false, "https://picsum.photos/seed/sanitizer/400/400"),
                    new ProductSeed("Face Wash", new BigDecimal("175.0"), new BigDecimal("155.0"), "100 g", 68, 4.8, false, "https://picsum.photos/seed/facewash/400/400"),
                    new ProductSeed("Deodorant", new BigDecimal("220.0"), new BigDecimal("195.0"), "150 ml", 75, 3.8, false, "https://picsum.photos/seed/deodorant/400/400"),
                    new ProductSeed("Razor", new BigDecimal("99.0"), new BigDecimal("0.0"), "1 pc", 82, 4.0, false, "https://picsum.photos/seed/razor/400/400"),
                    new ProductSeed("Moisturizer", new BigDecimal("250.0"), new BigDecimal("220.0"), "200 ml", 89, 4.3, false, "https://picsum.photos/seed/moisturizer/400/400")
            )),
            Map.entry("Baby Care", List.of(
                    new ProductSeed("Baby Diapers", new BigDecimal("450.0"), new BigDecimal("399.0"), "36 pcs", 40, 3.8, true, "https://picsum.photos/seed/diapers/400/400"),
                    new ProductSeed("Baby Wipes", new BigDecimal("120.0"), new BigDecimal("105.0"), "80 pcs", 47, 4.0, false, "https://picsum.photos/seed/babywipes/400/400"),
                    new ProductSeed("Baby Lotion", new BigDecimal("180.0"), new BigDecimal("160.0"), "200 ml", 54, 4.3, false, "https://picsum.photos/seed/babylotion/400/400"),
                    new ProductSeed("Baby Shampoo", new BigDecimal("165.0"), new BigDecimal("145.0"), "200 ml", 61, 4.5, false, "https://picsum.photos/seed/babyshampoo/400/400"),
                    new ProductSeed("Baby Powder", new BigDecimal("140.0"), new BigDecimal("0.0"), "200 g", 68, 4.8, false, "https://picsum.photos/seed/babypowder/400/400"),
                    new ProductSeed("Baby Food Cereal", new BigDecimal("220.0"), new BigDecimal("195.0"), "300 g", 75, 3.8, false, "https://picsum.photos/seed/babyfood/400/400"),
                    new ProductSeed("Feeding Bottle", new BigDecimal("250.0"), new BigDecimal("0.0"), "1 pc", 82, 4.0, false, "https://picsum.photos/seed/babybottle/400/400"),
                    new ProductSeed("Baby Oil", new BigDecimal("130.0"), new BigDecimal("115.0"), "200 ml", 89, 4.3, false, "https://picsum.photos/seed/babyoil/400/400")
            )),
            Map.entry("Pet Care", List.of(
                    new ProductSeed("Dog Food", new BigDecimal("650.0"), new BigDecimal("590.0"), "1.2 kg", 40, 3.8, true, "https://picsum.photos/seed/dogfood/400/400"),
                    new ProductSeed("Cat Food", new BigDecimal("480.0"), new BigDecimal("430.0"), "1 kg", 47, 4.0, false, "https://picsum.photos/seed/catfood/400/400"),
                    new ProductSeed("Pet Shampoo", new BigDecimal("220.0"), new BigDecimal("195.0"), "200 ml", 54, 4.3, false, "https://picsum.photos/seed/petshampoo/400/400"),
                    new ProductSeed("Dog Treats", new BigDecimal("150.0"), new BigDecimal("130.0"), "150 g", 61, 4.5, false, "https://picsum.photos/seed/dogtreats/400/400"),
                    new ProductSeed("Cat Litter", new BigDecimal("350.0"), new BigDecimal("310.0"), "5 L", 68, 4.8, false, "https://picsum.photos/seed/catlitter/400/400"),
                    new ProductSeed("Pet Toy", new BigDecimal("199.0"), new BigDecimal("0.0"), "1 pc", 75, 3.8, false, "https://picsum.photos/seed/pettoy/400/400"),
                    new ProductSeed("Bird Seed Mix", new BigDecimal("90.0"), new BigDecimal("0.0"), "500 g", 82, 4.0, false, "https://picsum.photos/seed/birdseed/400/400"),
                    new ProductSeed("Fish Food Flakes", new BigDecimal("110.0"), new BigDecimal("95.0"), "100 g", 89, 4.3, false, "https://picsum.photos/seed/fishfood/400/400")
            ))
    );
}