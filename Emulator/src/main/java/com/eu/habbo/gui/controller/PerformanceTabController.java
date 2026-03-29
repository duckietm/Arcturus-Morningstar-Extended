package com.eu.habbo.gui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.lang.management.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PerformanceTabController {

    private static final int MAX_DATA_POINTS = 60;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private XYChart.Series<String, Number> heapUsedSeries;
    private XYChart.Series<String, Number> heapMaxSeries;
    private XYChart.Series<String, Number> threadsSeries;
    private XYChart.Series<String, Number> cpuSeries;

    private Label heapUsedLabel;
    private Label heapMaxLabel;
    private Label nonHeapLabel;
    private Label gcCountLabel;
    private Label gcTimeLabel;
    private Label threadCountLabel;
    private Label peakThreadsLabel;
    private Label cpuLoadLabel;

    private Timeline refreshTimeline;

    public Tab createTab() {
        Tab tab = new Tab("Performance");
        tab.setClosable(false);

        Label title = new Label("Performance Monitor");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        // Stats grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(8);
        statsGrid.setPadding(new Insets(5, 0, 0, 0));

        heapUsedLabel = createValueLabel("0 MB");
        heapMaxLabel = createValueLabel("0 MB");
        nonHeapLabel = createValueLabel("0 MB");
        gcCountLabel = createValueLabel("0");
        gcTimeLabel = createValueLabel("0 ms");
        threadCountLabel = createValueLabel("0");
        peakThreadsLabel = createValueLabel("0");
        cpuLoadLabel = createValueLabel("N/A");

        int row = 0;
        statsGrid.add(createHeaderLabel("Heap Used"), 0, row);
        statsGrid.add(heapUsedLabel, 1, row);
        statsGrid.add(createHeaderLabel("Heap Max"), 2, row);
        statsGrid.add(heapMaxLabel, 3, row++);
        statsGrid.add(createHeaderLabel("Non-Heap"), 0, row);
        statsGrid.add(nonHeapLabel, 1, row);
        statsGrid.add(createHeaderLabel("CPU Load"), 2, row);
        statsGrid.add(cpuLoadLabel, 3, row++);
        statsGrid.add(createHeaderLabel("GC Count"), 0, row);
        statsGrid.add(gcCountLabel, 1, row);
        statsGrid.add(createHeaderLabel("GC Time"), 2, row);
        statsGrid.add(gcTimeLabel, 3, row++);
        statsGrid.add(createHeaderLabel("Threads"), 0, row);
        statsGrid.add(threadCountLabel, 1, row);
        statsGrid.add(createHeaderLabel("Peak Threads"), 2, row);
        statsGrid.add(peakThreadsLabel, 3, row++);

        // Charts
        HBox chartsTop = new HBox(10, createHeapChart(), createCpuChart());
        HBox.setHgrow(chartsTop.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(chartsTop.getChildren().get(1), Priority.ALWAYS);

        HBox chartsBottom = new HBox(10, createThreadsChart());
        HBox.setHgrow(chartsBottom.getChildren().get(0), Priority.ALWAYS);

        VBox content = new VBox(8, title, statsGrid, new Separator(), chartsTop, chartsBottom);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(chartsTop, Priority.ALWAYS);
        VBox.setVgrow(chartsBottom, Priority.ALWAYS);

        tab.setContent(content);

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshPerformance()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        tab.setOnClosed(e -> refreshTimeline.stop());

        return tab;
    }

    private LineChart<String, Number> createHeapChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("MB");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Heap Memory");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(200);
        chart.getStyleClass().add("dark-chart");
        heapUsedSeries = new XYChart.Series<>();
        heapUsedSeries.setName("Used");
        heapMaxSeries = new XYChart.Series<>();
        heapMaxSeries.setName("Max");
        chart.getData().addAll(heapUsedSeries, heapMaxSeries);
        return chart;
    }

    private LineChart<String, Number> createCpuChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("%");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("CPU Load");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(200);
        chart.getStyleClass().add("dark-chart");
        cpuSeries = new XYChart.Series<>();
        chart.getData().add(cpuSeries);
        return chart;
    }

    private LineChart<String, Number> createThreadsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Active Threads");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(180);
        chart.getStyleClass().add("dark-chart");
        threadsSeries = new XYChart.Series<>();
        chart.getData().add(threadsSeries);
        return chart;
    }

    private void refreshPerformance() {
        try {
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            MemoryUsage heap = memBean.getHeapMemoryUsage();
            MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

            long heapUsedMB = heap.getUsed() / (1024 * 1024);
            long heapMaxMB = heap.getMax() / (1024 * 1024);
            long nonHeapMB = nonHeap.getUsed() / (1024 * 1024);

            long totalGcCount = 0;
            long totalGcTime = 0;
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gc : gcBeans) {
                totalGcCount += gc.getCollectionCount();
                totalGcTime += gc.getCollectionTime();
            }

            int threads = threadBean.getThreadCount();
            int peakThreads = threadBean.getPeakThreadCount();

            double cpuLoad = -1;
            try {
                com.sun.management.OperatingSystemMXBean osBean =
                        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                cpuLoad = osBean.getProcessCpuLoad() * 100;
            } catch (Exception ignored) {
            }

            String time = LocalTime.now().format(TIME_FMT);
            final long fGcCount = totalGcCount;
            final long fGcTime = totalGcTime;
            final double fCpu = cpuLoad;

            Platform.runLater(() -> {
                heapUsedLabel.setText(heapUsedMB + " MB");
                heapMaxLabel.setText(heapMaxMB + " MB");
                nonHeapLabel.setText(nonHeapMB + " MB");
                gcCountLabel.setText(String.valueOf(fGcCount));
                gcTimeLabel.setText(fGcTime + " ms");
                threadCountLabel.setText(String.valueOf(threads));
                peakThreadsLabel.setText(String.valueOf(peakThreads));
                cpuLoadLabel.setText(fCpu >= 0 ? String.format("%.1f%%", fCpu) : "N/A");

                addData(heapUsedSeries, time, heapUsedMB);
                addData(heapMaxSeries, time, heapMaxMB);
                addData(threadsSeries, time, threads);
                if (fCpu >= 0) addData(cpuSeries, time, fCpu);
            });
        } catch (Exception ignored) {
        }
    }

    private void addData(XYChart.Series<String, Number> series, String time, Number value) {
        series.getData().add(new XYChart.Data<>(time, value));
        if (series.getData().size() > MAX_DATA_POINTS) {
            series.getData().remove(0);
        }
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setMinWidth(110);
        return label;
    }

    private Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        return label;
    }
}
