package com.astin.moneymaster.model;

public class HistoryEntry {
    private String id;
    private String categoryName;
    private double amountPaid;
    private String dateTime;
    private String itemName; // ← newly added field

    // Required empty constructor for Firebase
    public HistoryEntry() {}

    public HistoryEntry(String id, String categoryName, double amountPaid, String dateTime, String itemName) {
        this.id = id;
        this.categoryName = categoryName;
        this.amountPaid = amountPaid;
        this.dateTime = dateTime;
        this.itemName = itemName;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
}
