package com.moneytp.sothuchi.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.util.DataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Màn hình phân tích thu chi bằng biểu đồ.
 * Sử dụng MPAndroidChart:
 * - PieChart: cơ cấu chi tiêu / thu nhập theo danh mục
 * - BarChart: so sánh thu/chi theo tháng
 */
public class ChartActivity extends AppCompatActivity {

    private PieChart pieChi;
    private PieChart pieThu;
    private BarChart barMonthly;
    private TextView tvSummary;

    private DataRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Phân tích Thu Chi");
        }

        pieChi = findViewById(R.id.pie_chi);
        pieThu = findViewById(R.id.pie_thu);
        barMonthly = findViewById(R.id.bar_monthly);
        tvSummary = findViewById(R.id.tv_summary);

        setupPieChart(pieChi, "Cơ cấu CHI TIÊU");
        setupPieChart(pieThu, "Cơ cấu THU NHẬP");
        setupBarChart();

        repo = DataRepository.getInstance();
        repo.getChartLiveData().observe(this, this::renderCharts);
        repo.getBalanceLiveData().observe(this, summary -> {
            if (summary != null) {
                String text = String.format(
                        "Tổng thu: %s đ\nTổng chi: %s đ\nSố dư: %s đ",
                        format(summary.totalThu),
                        format(summary.totalChi),
                        format(summary.balance)
                );
                tvSummary.setText(text);
            }
        });
    }

    private void setupPieChart(PieChart chart, String centerText) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(8, 8, 8, 8);
        chart.setDragDecelerationFrictionCoef(0.95f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleRadius(50f);
        chart.setHoleRadius(42f);
        chart.setCenterText(centerText);
        chart.setCenterTextSize(13f);
        chart.setCenterTextColor(Color.parseColor("#334155"));
        chart.setEntryLabelColor(Color.DKGRAY);
        chart.setEntryLabelTextSize(11f);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);
    }

    private void setupBarChart() {
        barMonthly.getDescription().setEnabled(false);
        barMonthly.setDrawGridBackground(false);
        barMonthly.setDrawBarShadow(false);
        barMonthly.setFitBars(true);
        barMonthly.setPinchZoom(false);
        barMonthly.setScaleEnabled(false);
        barMonthly.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = barMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(11f);

        YAxis left = barMonthly.getAxisLeft();
        left.setDrawGridLines(true);
        left.setAxisMinimum(0f);
        left.setTextSize(11f);
        barMonthly.getAxisRight().setEnabled(false);

        Legend legend = barMonthly.getLegend();
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
    }

    private void renderCharts(DataRepository.ChartData data) {
        if (data == null) return;

        // Pie Chi
        renderPie(pieChi, data.chiByCategory, ColorTemplate.MATERIAL_COLORS);

        // Pie Thu
        renderPie(pieThu, data.thuByCategory, ColorTemplate.COLORFUL_COLORS);

        // Bar monthly
        renderBar(data.chiByMonth, data.thuByMonth);
    }

    private void renderPie(PieChart chart, Map<String, Long> map, int[] colors) {
        List<PieEntry> entries = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, Long> e : map.entrySet()) {
                if (e.getValue() > 0) {
                    entries.add(new PieEntry(e.getValue(), e.getKey()));
                }
            }
        }

        if (entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText("Chưa có dữ liệu");
            chart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2.5f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(chart));
        chart.setData(pieData);
        chart.highlightValues(null);
        chart.animateY(700);
        chart.invalidate();
    }

    private void renderBar(Map<String, Long> chiByMonth, Map<String, Long> thuByMonth) {
        // Lấy danh sách tháng unique, sort tăng dần
        List<String> months = new ArrayList<>();
        if (chiByMonth != null) months.addAll(chiByMonth.keySet());
        if (thuByMonth != null) {
            for (String m : thuByMonth.keySet()) {
                if (!months.contains(m)) months.add(m);
            }
        }
        Collections.sort(months);

        if (months.isEmpty()) {
            barMonthly.clear();
            barMonthly.setNoDataText("Chưa có dữ liệu theo tháng");
            barMonthly.invalidate();
            return;
        }

        List<BarEntry> chiEntries = new ArrayList<>();
        List<BarEntry> thuEntries = new ArrayList<>();

        for (int i = 0; i < months.size(); i++) {
            String m = months.get(i);
            long chi = chiByMonth != null && chiByMonth.containsKey(m) ? chiByMonth.get(m) : 0;
            long thu = thuByMonth != null && thuByMonth.containsKey(m) ? thuByMonth.get(m) : 0;
            chiEntries.add(new BarEntry(i, chi));
            thuEntries.add(new BarEntry(i, thu));
        }

        BarDataSet setChi = new BarDataSet(chiEntries, "Chi tiêu");
        setChi.setColor(Color.parseColor("#e11d48"));
        setChi.setValueTextSize(10f);

        BarDataSet setThu = new BarDataSet(thuEntries, "Thu nhập");
        setThu.setColor(Color.parseColor("#059669"));
        setThu.setValueTextSize(10f);

        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;

        BarData data = new BarData(setChi, setThu);
        data.setBarWidth(barWidth);

        barMonthly.setData(data);
        barMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        barMonthly.getXAxis().setLabelCount(months.size());
        barMonthly.groupBars(0f, groupSpace, barSpace);
        barMonthly.animateY(800);
        barMonthly.invalidate();
    }

    private String format(long n) {
        return String.format("%,d", n).replace(',', '.');
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
