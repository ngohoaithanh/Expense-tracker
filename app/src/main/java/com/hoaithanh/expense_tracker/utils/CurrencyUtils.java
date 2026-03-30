package com.hoaithanh.expense_tracker.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {
    public static String formatVND(double amount) {
        // Định dạng theo chuẩn tiếng Việt (vi) và quốc gia Việt Nam (VN)
        Locale vietnam = new Locale("vi", "VN");
        NumberFormat formatter = NumberFormat.getCurrencyInstance(vietnam);
        return formatter.format(amount);
    }
}
