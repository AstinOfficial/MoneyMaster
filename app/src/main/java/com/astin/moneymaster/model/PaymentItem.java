package com.astin.moneymaster.model;

public class PaymentItem {
    private String name;
    private String budget;
    private String budget_balance;

    // 🔧 Required empty constructor for Firebase
    public PaymentItem() {
    }

    public PaymentItem(String name, String budget) {
        this.name = name;
        this.budget = budget;
        this.budget_balance=budget;
    }


    public String getBudget_balance() {
        return budget_balance;
    }

    public void setBudget_balance(String budget_balance) {
        this.budget_balance = budget_balance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }
}
