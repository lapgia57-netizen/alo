package com.moneytp.sothuchi.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
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
import com.google.firebase.database.ValueEventListener;
import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Biểu đồ dữ liệu - màu sáng, dễ đọc.
 * - Pie chart chi tiêu theo danh mục
 * - Pie chart thu nhập theo danh mục
 * - Bar chart thu/chi theo tháng
 */
public class ChartFragment extends Fragment {

    private FirebaseHelper firebaseHelper;
    private ValueEventListener chiListener, thuListener;
    private List<Transaction> allChi = new ArrayList<>();
    private List<Transaction> allThu = new ArrayList<>();

    private PieChart pieChartExpense;
    private PieChart pieChartIncome;
    private BarChart barChartMonthly;

    private final int[] CHART_COLORS = {
            Color.parseColor("#6366F1"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#84CC16")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseHelper = new FirebaseHelper();

        pieChartExpense = view.findViewById(R.id.pieChartExpense);
        pieChartIncome = view.findViewById(R.id.pieChartIncome);
        barChartMonthly = view.findViewById(R.id.barChartMonthly);

        setupPieChart(pieChartExpense);
        setupPieChart(pieChartIncome);
        setupBarChart(barChartMonthly);

        loadData();
    }

    private void setupPieChart(PieChart chart) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(8, 8, 8, 8);
        chart.setDragDecelerationFrictionCoef(0.9f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        chart.setHoleRadius(48f);
        chart.setTransparentCircleRadius(54f);
        chart.setDrawCenterText(true);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setEntryLabelColor(Color.parseColor("#0F172A"));
        chart.setEntryLabelTextSize(11f);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#64748B"));
        legend.setWordWrapEnabled(true);
    }

    private void setupBarChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setFitBars(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#64748B"));
        xAxis.setTextSize(10f);

        YAxis left = chart.getAxisLeft();
        left.setDrawGridLines(true);
        left.setGridColor(Color.parseColor("#E2E8F0"));
        left.setTextColor(Color.parseColor("#64748B"));
        left.setTextSize(10f);
        left.setAxisMinimum(0f);

        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#64748B"));
    }

    private void loadData() {
        chiListener = firebaseHelper.listenTransactions(FirebaseHelper.TYPE_CHI,
                new FirebaseHelper.DataCallback() {
                    @Override
                    public void onData(List<Transaction> list) {
                        allChi = list != null ? list : new ArrayList<>();
                        updateCharts();
                    }

                    @Override
                    public void onError(String message) { }
                });

        thuListener = firebaseHelper.listenTransactions(FirebaseHelper.TYPE_THU,
                new FirebaseHelper.DataCallback() {
                    @Override
                    public void onData(List<Transaction> list) {
                        allThu = list != null ? list : new ArrayList<>();
                        updateCharts();
                    }

                    @Override
                    public void onError(String message) { }
                });
    }

    private void updateCharts() {
        if (!isAdded()) return;
        updatePieChart(pieChartExpense, allChi, "Chi tiêu");
        updatePieChart(pieChartIncome, allThu, "Thu nhập");
        updateBarChart();
    }

    private void updatePieChart(PieChart chart, List<Transaction> data, String centerText) {
        Map<String, Float> categorySum = new HashMap<>();
        for (Transaction t : data) {
            String cat = t.getDanh_muc() != null && !t.getDanh_muc().isEmpty()
                    ? t.getDanh_muc() : "Khác";
            categorySum.put(cat, categorySum.getOrDefault(cat, 0f) + t.getSo_tien());
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> e : categorySum.entrySet()) {
            if (e.getValue() > 0) {
                entries.add(new PieEntry(e.getValue(), e.getKey()));
            }
        }

        if (entries.isEmpty()) {
            chart.setData(null);
            chart.setCenterText("Chưa có dữ liệu");
            chart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(CHART_COLORS);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(chart));
        pieData.setValueTextSize(11f);
        pieData.setValueTextColor(Color.WHITE);

        chart.setData(pieData);
        chart.setCenterText(centerText);
        chart.setCenterTextSize(13f);
        chart.setCenterTextColor(Color.parseColor("#0F172A"));
        chart.highlightValues(null);
        chart.invalidate();
        chart.animateY(600);
    }

    private void updateBarChart() {
        // Group theo tháng (YYYY-MM)
        TreeMap<String, float[]> monthly = new TreeMap<>(); // [0]=thu, [1]=chi

        for (Transaction t : allThu) {
            String month = extractMonth(t.getNgay());
            float[] vals = monthly.getOrDefault(month, new float[]{0f, 0f});
            vals[0] += t.getSo_tien();
            monthly.put(month, vals);
        }
        for (Transaction t : allChi) {
            String month = extractMonth(t.getNgay());
            float[] vals = monthly.getOrDefault(month, new float[]{0f, 0f});
            vals[1] += t.getSo_tien();
            monthly.put(month, vals);
        }

        if (monthly.isEmpty()) {
            barChartMonthly.setData(null);
            barChartMonthly.invalidate();
            return;
        }

        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, float[]> e : monthly.entrySet()) {
            labels.add(e.getKey());
            incomeEntries.add(new BarEntry(i, e.getValue()[0]));
            expenseEntries.add(new BarEntry(i, e.getValue()[1]));
            i++;
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeSet.setColor(Color.parseColor("#10B981"));
        incomeSet.setValueTextSize(9f);
        incomeSet.setValueTextColor(Color.parseColor("#059669"));

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseSet.setColor(Color.parseColor("#F43F5E"));
        expenseSet.setValueTextSize(9f);
        expenseSet.setValueTextColor(Color.parseColor("#E11D48"));

        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(barWidth);

        barChartMonthly.setData(barData);
        barChartMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartMonthly.getXAxis().setLabelCount(labels.size());
        barChartMonthly.groupBars(0f, groupSpace, barSpace);
        barChartMonthly.getXAxis().setAxisMinimum(0f);
        barChartMonthly.getXAxis().setAxisMaximum(0f + barData.getGroupWidth(groupSpace, barSpace) * labels.size());
        barChartMonthly.invalidate();
        barChartMonthly.animateY(600);
    }

    private String extractMonth(String ngay) {
        if (ngay != null && ngay.length() >= 7) {
            return ngay.substring(0, 7); // YYYY-MM
        }
        return "N/A";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chiListener != null) {
            firebaseHelper.removeListener(FirebaseHelper.TYPE_CHI, chiListener);
        }
        if (thuListener != null) {
            firebaseHelper.removeListener(FirebaseHelper.TYPE_THU, thuListener);
        }
    }
}
