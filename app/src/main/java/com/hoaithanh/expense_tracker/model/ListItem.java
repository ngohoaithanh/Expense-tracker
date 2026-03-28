package com.hoaithanh.expense_tracker.model;

import com.hoaithanh.expense_tracker.data.local.entity.Expense;

public class ListItem {
    public static final int TYPE_DATE = 0;
    public static final int TYPE_EXPENSE = 1;

    private int type;
    private String dateHeader;
    private long totalAmount; // THÊM BIẾN NÀY để lưu tổng tiền của ngày
    private Expense expense;

    // Cập nhật Constructor cho Header (Ngày) để nhận thêm số tiền tổng
    public ListItem(String dateHeader, long totalAmount) {
        this.type = TYPE_DATE;
        this.dateHeader = dateHeader;
        this.totalAmount = totalAmount;
    }

    public ListItem(Expense expense) {
        this.type = TYPE_EXPENSE;
        this.expense = expense;
    }

    public int getType() { return type; }
    public String getDateHeader() { return dateHeader; }
    public long getTotalAmount() { return totalAmount; } // Getter cho tổng tiền
    public Expense getExpense() { return expense; }
}