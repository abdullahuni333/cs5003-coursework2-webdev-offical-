package com.myapp;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named("buildBean")
@SessionScoped
public class BuildBean implements Serializable {

    private String cpu;
    private String gpu;
    private String ram;

    public double getTotalPrice() {
        double total = 0;

        if ("Ryzen 5 5600X".equals(cpu)) total += 180;
        if ("Core i5-12400F".equals(cpu)) total += 170;

        if ("RTX 4060".equals(gpu)) total += 290;
        if ("RX 7600".equals(gpu)) total += 260;

        if ("16GB DDR4".equals(ram)) total += 45;
        if ("32GB DDR4".equals(ram)) total += 80;

        return total;
    }

    // getters and setters

    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }

    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }

    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
}