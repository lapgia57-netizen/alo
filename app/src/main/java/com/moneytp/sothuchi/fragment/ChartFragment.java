package com.moneytp.sothuchi.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

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
import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.util.DataRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Biểu đồ — dùng ChartData pre-computed từ DataRepository (không parse lại list).
 * Material light theme, animation mượt.
 */
public class ChartFragment extends Fragment {

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

        pieChartExpense = view.findViewById(R.id.pieChartExpense);
        pieChartIncome = view.findViewById(R.id.pieChartIncome);
        barChartMonthly = view.findViewById(R.id.barChartMonthly);

        setupPieChart(pieChartExpense);
        setupPieChart(pieChartIncome);
        setupBarChart(barChartMonthly);

        DataRepository.getInstance().getChartLiveData()
                .observe(getViewLifecycleOwner(), new Observer<DataRepository.ChartData>() {
                    @Override
                    public void onChanged(DataRepository.ChartData data) {
                        if (data == null) return;
                        updatePieChart(pieChartExpense, data.chiByCategory, "Chi tiêu");
                        updatePieChart(pieChartIncome, data.thuByCategory, "Thu nhập");
                        updateBarChart(data.thuByMonth, data.chiByMonth);
                    }
                });
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
        chart.setExtraBottomOffset(8f);

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

    private void updatePieChart(PieChart chart, Map<String, Long> categorySum, String centerText) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (categorySum != null) {
            for (Map.Entry<String, Long> e : categorySum.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
                }
            }
        }

        if (entries.isEmpty()) {
            chart.clear();
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
        chart.animateY(500);
        chart.invalidate();
    }

    private void updateBarChart(Map<String, Long> thuByMonth, Map<String, Long> chiByMonth) {
        TreeMap<String, float[]> monthly = new TreeMap<>();

        if (thuByMonth != null) {
            for (Map.Entry<String, Long> e : thuByMonth.entrySet()) {
                float[] vals = monthly.getOrDefault(e.getKey(), new float[]{0f, 0f});
                vals[0] = e.getValue() != null ? e.getValue() : 0f;
                monthly.put(e.getKey(), vals);
            }
        }
        if (chiByMonth != null) {
            for (Map.Entry<String, Long> e : chiByMonth.entrySet()) {
                float[] vals = monthly.getOrDefault(e.getKey(), new float[]{0f, 0f});
                vals[1] = e.getValue() != null ? e.getValue() : 0f;
                monthly.put(e.getKey(), vals);
            }
        }

        if (monthly.isEmpty()) {
            barChartMonthly.clear();
            barChartMonthly.invalidate();
            return;
        }

        List<String> labels = new ArrayList<>();
        ArrayList<BarEntry> thuEntries = new ArrayList<>();
        ArrayList<BarEntry> chiEntries = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, float[]> e : monthly.entrySet()) {
            labels.add(e.getKey());
            thuEntries.add(new BarEntry(i, e.getValue()[0]));
            chiEntries.add(new BarEntry(i, e.getValue()[1]));
            i++;
        }

        BarDataSet thuSet = new BarDataSet(thuEntries, "Thu");
        thuSet.setColor(Color.parseColor("#059669"));
        thuSet.setValueTextSize(9f);

        BarDataSet chiSet = new BarDataSet(chiEntries, "Chi");
        chiSet.setColor(Color.parseColor("#E11D48"));
        chiSet.setValueTextSize(9f);

        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;

        BarData data = new BarData(thuSet, chiSet);
        data.setBarWidth(barWidth);

        barChartMonthly.setData(data);
        barChartMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartMonthly.getXAxis().setLabelCount(labels.size());
        barChartMonthly.getXAxis().setAxisMinimum(0);
        barChartMonthly.getXAxis().setAxisMaximum(labels.size());
        barChartMonthly.groupBars(0, groupSpace, barSpace);
        barChartMonthly.animateY(500);
        barChartMonthly.invalidate();
    }
}
