package com.erenkalkan.financial_risk_analysis.util;

public class PieChartHelper {
    private double value;
    private String color;
    private String path;

    public PieChartHelper(double value, String color, String path) {
        this.value = value;
        this.color = color;
        this.path = path;
    }

    // Getters and setters
    public double getValue() { return value; }
    public String getColor() { return color; }
    public String getPath() { return path; }
}
