package com.syber.hypoxia;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.mikephil.charting_rename.animation.ChartAnimator;
import com.github.mikephil.charting_rename.buffer.ScatterBuffer;
import com.github.mikephil.charting_rename.charts.Chart;
import com.github.mikephil.charting_rename.charts.CombinedChart;
import com.github.mikephil.charting_rename.charts.ScatterChart;
import com.github.mikephil.charting_rename.components.XAxis;
import com.github.mikephil.charting_rename.components.YAxis;
import com.github.mikephil.charting_rename.data.CandleData;
import com.github.mikephil.charting_rename.data.CandleDataSet;
import com.github.mikephil.charting_rename.data.CandleEntry;
import com.github.mikephil.charting_rename.data.CombinedData;
import com.github.mikephil.charting_rename.data.Entry;
import com.github.mikephil.charting_rename.data.LineData;
import com.github.mikephil.charting_rename.data.LineDataSet;
import com.github.mikephil.charting_rename.data.ScatterData;
import com.github.mikephil.charting_rename.data.ScatterDataSet;
import com.github.mikephil.charting_rename.highlight.Highlight;
import com.github.mikephil.charting_rename.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting_rename.interfaces.datasets.ICandleDataSet;
import com.github.mikephil.charting_rename.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting_rename.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting_rename.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting_rename.renderer.BarChartRenderer;
import com.github.mikephil.charting_rename.renderer.BubbleChartRenderer;
import com.github.mikephil.charting_rename.renderer.CandleStickChartRenderer;
import com.github.mikephil.charting_rename.renderer.CombinedChartRenderer;
import com.github.mikephil.charting_rename.renderer.DataRenderer;
import com.github.mikephil.charting_rename.renderer.LineChartRenderer;
import com.github.mikephil.charting_rename.renderer.ScatterChartRenderer;
import com.github.mikephil.charting_rename.utils.ColorTemplate;
import com.github.mikephil.charting_rename.utils.Transformer;
import com.github.mikephil.charting_rename.utils.ViewPortHandler;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.syber.base.BaseFragment;
import com.syber.hypoxia.data.BPChartResponse;
import com.syber.hypoxia.data.IRequester;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/**
 * Created by liangtg on 16-5-10.
 */
public class BloodPressureChartFragment extends BaseFragment implements RadioGroup.OnCheckedChangeListener, View.OnClickListener, OnChartValueSelectedListener {
    private int lastHight = -1;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private CombinedChart barChart;
    private TextView selectedDate, hightlightSys, highlightDia, highlightPul, highlightDate;
    private TextView lastPeriod, nextPeriod;
    private ProgressBar progressBar;
    private View abnormal;

    private ChartDataProvider dayProvider, weekProvider, monthProvider;
    private ChartDataProvider curProvider;
    private Handler handler = new Handler();
    private Runnable highlightRunnable = new Runnable() {
        @Override
        public void run() {
            if (getView() == null || getActivity().isFinishing() || barChart.isEmpty()) return;
            barChart.highlightValue(lastHight, 0);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createProvider();
    }

    private void createProvider() {
        long now = System.currentTimeMillis();
        dayProvider = new ChartDataProvider(createCalendar(now),
                createCalendar(now),
                1,
                Calendar.DAY_OF_YEAR,
                "%tF",
                R.string.last_day,
                R.string.next_day);
        Calendar startDate = createCalendar(now), endDate = createCalendar(now);
        while (startDate.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) startDate.add(Calendar.DAY_OF_YEAR, -1);
        endDate.setTimeInMillis(startDate.getTimeInMillis());
        endDate.add(Calendar.DAY_OF_WEEK, 6);
        weekProvider = new ChartDataProvider(startDate, endDate, 7, Calendar.DAY_OF_YEAR, "%tY-%3$d周", R.string.last_week, R.string.next_week);
        startDate = createCalendar(now);
        endDate = createCalendar(now);
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        monthProvider = new ChartDataProvider(startDate, endDate, 1, Calendar.MONTH, "%tY-%tm", R.string.last_month, R.string.next_month);
        curProvider = dayProvider;
    }

    private Calendar createCalendar(long time) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTimeInMillis(time);
        return calendar;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bloodpressure_history, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        highlightDate = get(R.id.highlight_date);
        abnormal = get(R.id.abnormal);
        lastPeriod = get(R.id.last_period);
        nextPeriod = get(R.id.next_period);
        lastPeriod.setOnClickListener(this);
        nextPeriod.setOnClickListener(this);
        hightlightSys = get(R.id.highlight_sys);
        highlightDia = get(R.id.highlight_dia);
        highlightPul = get(R.id.highlight_pul);
        selectedDate = get(R.id.selected_date);
        progressBar = get(R.id.progress);
        barChart = get(R.id.chart);
        barChart.setOnChartValueSelectedListener(this);
        get(R.id.bp_detail).setOnClickListener(this);
        get(R.id.add_bp).setOnClickListener(this);
        get(R.id.refresh).setOnClickListener(this);

        RadioGroup group = get(R.id.date_group);
        group.setOnCheckedChangeListener(this);
        initChart();
        curProvider.updateChart();
    }

    private void initChart() {
        barChart.setNoDataText("");
        barChart.getPaint(Chart.PAINT_INFO).setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                16,
                getResources().getDisplayMetrics()));
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setDescription("");
        YAxis yAxis = barChart.getAxisLeft();
        int color = 0xFFFFFFFF;
        yAxis.setAxisLineColor(color);
        yAxis.setDrawGridLines(true);
        yAxis.enableGridDashedLine(5, 5, 2);
        yAxis.setTextColor(color);
        yAxis.setGridColor(color);
        yAxis.setDrawZeroLine(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.enableGridDashedLine(5, 5, 5);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(color);
        xAxis.setAxisLineColor(color);
        xAxis.setYOffset(15);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (R.id.day == checkedId) {
            curProvider = dayProvider;
        } else if (R.id.week == checkedId) {
            curProvider = weekProvider;
        } else if (R.id.month == checkedId) {
            curProvider = monthProvider;
        }
        curProvider.updateChart();
    }

    @Override
    public void onResume() {
        super.onResume();
        curProvider.refresh();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.add_bp == id) {
            gotoActivity(AddBPActivity.class);
        } else if (R.id.refresh == id) {
            curProvider.refresh();
        } else if (R.id.bp_detail == id) {
            getFragmentManager().beginTransaction().add(R.id.fragment_container, new BloodPressureHistoryFragment(), "bp_history").addToBackStack(
                    "bp_history").commit();
        } else if (R.id.last_period == id) {
            curProvider.lastPeriod();
        } else if (R.id.next_period == id) {
            curProvider.nextPeriod();
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        lastHight = h.getXIndex();
        BPChartResponse.ChartItem item = curProvider.dataResponse.chart.get(lastHight);
        highlightDate.setText(item.key);
        int sys = dayProvider == curProvider ? item.systolicMax : (int) item.systolicAvg;
        hightlightSys.setText(String.valueOf(sys));
        int dia = dayProvider == curProvider ? item.diastolicMax : (int) item.diastolicAvg;
        highlightDia.setText(String.valueOf(dia));
        int pul = dayProvider == curProvider ? item.heartRateMax : (int) item.heartRateAvg;
        highlightPul.setText(String.valueOf(pul));
        abnormal.setVisibility((sys < 130 && dia < 85) ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onNothingSelected() {
        if (lastHight < 0) {
            if (!barChart.isEmpty()) lastHight = 0;
        } else {
            if (barChart.isEmpty()) {
                lastHight = -1;
            } else if (lastHight >= barChart.getXValCount()) {
                lastHight = barChart.getXValCount() - 1;
            }
        }
        if (lastHight >= 0) handler.post(highlightRunnable);
    }


    class ChartDataProvider {
        BPChartResponse dataResponse;
        boolean working = false;
        private Calendar startDate;
        private Calendar endDate;
        private int period;
        private int periodField;
        private String format;
        private int lastText;
        private int nextText;
        private CombinedData barData;
        private Bus bus = new Bus();

        public ChartDataProvider(Calendar startDate, Calendar endDate, int period, int periodField, String format, int lastText, int nextText) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.period = period;
            this.periodField = periodField;
            this.format = format;
            this.lastText = lastText;
            this.nextText = nextText;
            bus.register(this);
        }

        private void nextPeriod() {
            if (working) return;
            dataResponse = null;
            barData = null;
            startDate.add(periodField, period);
            endDate.add(periodField, period);
            if (Calendar.MONTH == periodField) {
                endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            }
            updateChart();
        }

        private void lastPeriod() {
            if (working) return;
            dataResponse = null;
            barData = null;
            startDate.add(periodField, period * -1);
            endDate.add(periodField, period * -1);
            if (Calendar.MONTH == periodField) {
                endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            }
            updateChart();
        }

        @Subscribe
        public void withData(BPChartResponse event) {
            if (getView() == null || getActivity().isFinishing()) return;
            working = false;
            if (event.isSuccess()) {
                dataResponse = event;
                if (curProvider == this) updateChart();
            } else if (curProvider == this) {
                progressBar.setVisibility(View.GONE);
                showToast("数据获取失败");
//                fillData();
//                createData();
            }
        }

        private void fillData() {
            BPChartResponse data = new BPChartResponse();
            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                BPChartResponse.ChartItem item = new BPChartResponse.ChartItem();
                item.systolicMax = random.nextInt(10) + 130;
                item.systolicAvg = random.nextInt(10) + 120;
                item.systolicMin = random.nextInt(10) + 110;
                item.diastolicMax = random.nextInt(10) + 80;
                item.diastolicAvg = random.nextInt(10) + 70;
                item.diastolicMin = random.nextInt(10) + 60;
                item.heartRateMax = random.nextInt(10) + 100;
                item.heartRateAvg = random.nextInt(10) + 90;
                item.heartRateMin = random.nextInt(10) + 80;
                item.key = "12:11";
                data.chart.add(item);
            }
            BPChartResponse.ChartTotal total = new BPChartResponse.ChartTotal();
            total.totalltimes = random.nextInt(20) + 40;
            data.total.add(total);
            dataResponse = data;
        }

        public void refresh() {
            if (working) {
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            barData = null;
            working = true;
            IRequester.getInstance().bloodChartData(bus, sdf.format(startDate.getTime()), sdf.format(endDate.getTime()));
        }

        void createData() {
            ArrayList<String> xVals = new ArrayList<>();
            ArrayList<Entry> rateYVals = new ArrayList<>();
            ArrayList<Entry> sysYVals = new ArrayList<>();
            ArrayList<Entry> diaYVals = new ArrayList<>();
            for (int i = 0; i < dataResponse.chart.size(); i++) {
                BPChartResponse.ChartItem item = dataResponse.chart.get(i);
                xVals.add(item.key);
                if (dayProvider == curProvider) {
                    rateYVals.add(new Entry(item.heartRateMax, i));
                    sysYVals.add(new Entry(item.systolicMax, i));
                    diaYVals.add(new Entry(item.diastolicMax, i));
                } else {
                    rateYVals.add(new Entry(item.heartRateAvg, i));
                    sysYVals.add(new Entry(item.systolicMax, i));
                    diaYVals.add(new Entry(item.diastolicMax, i));
                }
            }
            LineData lineData = new LineData(xVals);
            LineDataSet rateSet = new LineDataSet(rateYVals, "");
//            rateSet.setColor(0x80CF4C4C);
//            rateSet.setCircleColor(0xFFCF4C4C);
//            rateSet.setDrawCircleHole(false);
//            rateSet.setDrawFilled(false);
//            rateSet.setDrawHorizontalHighlightIndicator(false);
//            rateSet.setDrawVerticalHighlightIndicator(false);
//            lineData.addDataSet(rateSet);
            rateSet = new LineDataSet(sysYVals, "");
            rateSet.setDrawHorizontalHighlightIndicator(false);
            rateSet.setDrawVerticalHighlightIndicator(false);
            rateSet.setColor(0x80FFF8AB);
            rateSet.setCircleColor(0xFFFFF8AB);
            rateSet.setDrawCircleHole(false);
            lineData.addDataSet(rateSet);
            rateSet = new LineDataSet(diaYVals, "");
            rateSet.setDrawHorizontalHighlightIndicator(false);
            rateSet.setDrawVerticalHighlightIndicator(false);
            rateSet.setColor(0x80FFFFFF);
            rateSet.setCircleColor(0xFFFFFFFF);
            rateSet.setDrawCircleHole(false);
            lineData.addDataSet(rateSet);
            lineData.setDrawValues(false);
            barData = new CombinedData(xVals);
            barData.setData(lineData);
            resetData();
        }


        void reateData1() {
            ArrayList<String> xVals = new ArrayList<>();
            ArrayList<Entry> rateYVals = new ArrayList<>();
            ArrayList<CandleEntry> yVals = new ArrayList<>();
            ArrayList<CandleEntry> yValsLow = new ArrayList<>();
            for (int i = 0; i < dataResponse.chart.size(); i++) {
                BPChartResponse.ChartItem item = dataResponse.chart.get(i);
                int sys, dia;
                xVals.add(item.key);
                if (dayProvider == this) {
                    rateYVals.add(new Entry(item.heartRateMax, i));
                    sys = item.systolicMax;
                    dia = item.diastolicMax;
                    yVals.add(new CandleEntry(i, sys, dia, sys, dia));
                } else {
                    rateYVals.add(new Entry(item.heartRateAvg, i));
                    yVals.add(new CandleEntry(i, item.systolicMax, item.systolicMin, item.systolicMax, item.systolicMin));
                    yValsLow.add(new CandleEntry(i, item.diastolicMax, item.diastolicMin, item.diastolicMax, item.diastolicMin));
                }
            }
            CandleDataSet dataSet = new CandleDataSet(yVals, "收缩压");
            dataSet.setColor(0x80FFFFFF);
            dataSet.setDrawHighlightIndicators(false);
            barData = new CombinedData(xVals);
            CandleData candleData = new CandleData(xVals, dataSet);
            if (dayProvider != this) {
                dataSet = new CandleDataSet(yValsLow, "舒张压");
                dataSet.setColor(0x80FFFFFF);
                dataSet.setDrawHighlightIndicators(false);
                candleData.addDataSet(dataSet);
            }
            barData.setData(candleData);
            ScatterDataSet scatterDataSet = new ScatterDataSet(rateYVals, "心率");
            scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            scatterDataSet.setScatterShapeSize(10);
            scatterDataSet.setColor(0xFFFF0000);
            scatterDataSet.setDrawHighlightIndicators(false);
            ScatterData scatterData = new ScatterData(xVals, scatterDataSet);
            barData.setData(scatterData);
            barData.setDrawValues(false);
            resetData();
        }

        private void resetData() {
            barChart.clear();
            barChart.resetTracking();
            if (barData.getYValCount() > 0) {
                barChart.setData(barData);
                barChart.highlightValue(new Highlight(barData.getXValCount() - 1, 0), true);
            } else {
                barChart.setNoDataText("您还没有测量过血压");
            }
            BPCombineRender renderer = new BPCombineRender();
            barChart.setRenderer(renderer);
            renderer.initBuffers();
            barChart.invalidate();
            barChart.animateY(500);
            progressBar.setVisibility(View.GONE);
        }

        public void updateChart() {
            progressBar.setVisibility(View.VISIBLE);
            selectedDate.setText(String.format(format, startDate.getTime(), endDate.getTime(), startDate.get(Calendar.WEEK_OF_YEAR)));
            lastPeriod.setText(lastText);
            nextPeriod.setText(nextText);
            if (working) {
                barChart.clear();
                return;
            }
            if (null == dataResponse) {
                barChart.clear();
                refresh();
            } else if (null == barData) {
                createData();
            } else {
                resetData();
            }
        }

    }

    private class BPCombineRender extends CombinedChartRenderer {

        public BPCombineRender() {
            super(barChart, barChart.getAnimator(), barChart.getViewPortHandler());
        }

        @Override
        protected void createRenderers(CombinedChart chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {

            mRenderers = new ArrayList<DataRenderer>();

            CombinedChart.DrawOrder[] orders = chart.getDrawOrder();

            for (CombinedChart.DrawOrder order : orders) {

                switch (order) {
                    case BAR:
                        if (chart.getBarData() != null) mRenderers.add(new BarChartRenderer(chart, animator, viewPortHandler));
                        break;
                    case BUBBLE:
                        if (chart.getBubbleData() != null) mRenderers.add(new BubbleChartRenderer(chart, animator, viewPortHandler));
                        break;
                    case LINE:
                        if (chart.getLineData() != null) mRenderers.add(new BPLineRender(chart, animator, viewPortHandler));
                        break;
                    case CANDLE:
                        if (chart.getCandleData() != null) mRenderers.add(new BPRender());
                        break;
                    case SCATTER:
                        if (chart.getScatterData() != null) mRenderers.add(new BPScatterRender());
                        break;
                }
            }
        }
    }

    private class BPLineRender extends LineChartRenderer {
        private int highHalfWidth;
        private Drawable highLight;

        public BPLineRender(LineDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
            super(chart, animator, viewPortHandler);
            highLight = getResources().getDrawable(R.drawable.high_light_red, getActivity().getTheme());
            highHalfWidth = highLight.getIntrinsicWidth() / 2;
        }

        @Override
        public void drawExtras(Canvas c) {
            super.drawExtras(c);
            Highlight[] indices = barChart.getHighlighted();
            if (null == indices) return;

            for (int i = 0; i < indices.length; i++) {

                ILineDataSet set = mChart.getLineData().getDataSetByIndex(indices[i].getDataSetIndex());

                if (set == null || !set.isHighlightEnabled()) continue;

                int xIndex = indices[i].getXIndex(); // get the
                // x-position

                if (xIndex > mChart.getXChartMax() * mAnimator.getPhaseX()) continue;

                final float yVal = set.getYValForXIndex(xIndex);
                if (Float.isNaN(yVal)) continue;

                float y = yVal * mAnimator.getPhaseY(); // get
                // the
                // y-position

                float[] pts = new float[]{xIndex, y};

                mChart.getTransformer(set.getAxisDependency()).pointValuesToPixel(pts);

                highLight.setBounds((int) (pts[0] - highHalfWidth),
                        0,
                        (int) (pts[0] + highHalfWidth),
                        (int) (mViewPortHandler.contentBottom() + highLight.getIntrinsicHeight() / 2));
                highLight.draw(c);
            }

        }
    }


    private class BPRender extends CandleStickChartRenderer {
        int[] setDrawables = {R.drawable.high_pressure, R.drawable.low_pressure};
        private float[] mBodyBuffers = new float[4];
        private Drawable[] drawables = new Drawable[setDrawables.length];


        public BPRender() {
            super(barChart, barChart.getAnimator(), barChart.getViewPortHandler());
            for (int i = 0; i < setDrawables.length; i++) {
                drawables[i] = ResourcesCompat.getDrawable(getResources(), setDrawables[i], getActivity().getTheme());
            }
        }

        @Override
        protected void drawDataSet(Canvas c, ICandleDataSet dataSet) {

            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

            float phaseX = mAnimator.getPhaseX();
            float phaseY = mAnimator.getPhaseY();
            float barSpace = dataSet.getBarSpace();
            boolean showCandleBar = dataSet.getShowCandleBar();

            int minx = Math.max(mMinX, 0);
            int maxx = Math.min(mMaxX + 1, dataSet.getEntryCount());

            mRenderPaint.setStrokeWidth(dataSet.getShadowWidth());

            // draw the body
            for (int j = minx,
                 count = (int) Math.ceil((maxx - minx) * phaseX + (float) minx); j < count; j++) {

                // get the entry
                CandleEntry e = dataSet.getEntryForIndex(j);

                final int xIndex = e.getXIndex();

                if (xIndex < minx || xIndex >= maxx) continue;

                final float open = e.getOpen();
                final float close = e.getClose();
                final float high = e.getHigh();
                final float low = e.getLow();

                if (showCandleBar) {
                    // calculate the body
                    mBodyBuffers[0] = xIndex - 0.5f + barSpace;
                    mBodyBuffers[1] = close * phaseY;
                    mBodyBuffers[2] = (xIndex + 0.5f - barSpace);
                    mBodyBuffers[3] = open * phaseY;
                    trans.pointValuesToPixel(mBodyBuffers);
                    int index = mChart.getData().getIndexOfDataSet(dataSet);
                    int intrinsicWidth = drawables[index].getIntrinsicWidth();
                    float width = mBodyBuffers[2] - mBodyBuffers[0];
                    if (width > intrinsicWidth) {
                        mBodyBuffers[0] += (width - intrinsicWidth) / 2;
                        mBodyBuffers[2] -= (width - intrinsicWidth) / 2;
                    }
                    // draw body differently for increasing and decreasing entry
                    if (open > close) { // decreasing

                        if (dataSet.getDecreasingColor() == ColorTemplate.COLOR_NONE) {
                            mRenderPaint.setColor(dataSet.getColor(j));
                        } else {
                            mRenderPaint.setColor(dataSet.getDecreasingColor());
                        }
                        mRenderPaint.setStyle(dataSet.getDecreasingPaintStyle());
                        c.drawRect(mBodyBuffers[0], mBodyBuffers[3], mBodyBuffers[2], mBodyBuffers[1], mRenderPaint);
                        int left = (int) (mBodyBuffers[0] + (mBodyBuffers[2] - mBodyBuffers[0]) / 2 - intrinsicWidth / 2);
                        int top = (int) (mBodyBuffers[3] - drawables[index].getIntrinsicHeight() / 2);
                        drawables[index].setBounds(left, top, left + intrinsicWidth, top + drawables[index].getIntrinsicHeight());
                        drawables[index].draw(c);
                        top = (int) (mBodyBuffers[1] - drawables[index].getIntrinsicHeight() / 2);
                        if (2 == mChart.getData().getDataSetCount()) index = 1;
                        drawables[index].setBounds(left, top, left + intrinsicWidth, top + drawables[index].getIntrinsicHeight());
                        drawables[index].draw(c);
                    } else if (open < close) {
                        if (dataSet.getIncreasingColor() == ColorTemplate.COLOR_NONE) {
                            mRenderPaint.setColor(dataSet.getColor(j));
                        } else {
                            mRenderPaint.setColor(dataSet.getIncreasingColor());
                        }
                        mRenderPaint.setStyle(dataSet.getIncreasingPaintStyle());
                        c.drawRect(mBodyBuffers[0], mBodyBuffers[1], mBodyBuffers[2], mBodyBuffers[3], mRenderPaint);
                    } else { // equal values
                        if (dataSet.getNeutralColor() == ColorTemplate.COLOR_NONE) {
                            mRenderPaint.setColor(dataSet.getColor(j));
                        } else {
                            mRenderPaint.setColor(dataSet.getNeutralColor());
                        }
                        c.drawLine(mBodyBuffers[0], mBodyBuffers[1], mBodyBuffers[2], mBodyBuffers[3], mRenderPaint);
                    }
                }
            }
        }
    }

    private class BPScatterRender extends ScatterChartRenderer {
        int width, height;
        private Drawable drawable;

        public BPScatterRender() {
            super(barChart, barChart.getAnimator(), barChart.getViewPortHandler());
            drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.heart, getActivity().getTheme());
            width = drawable.getIntrinsicWidth();
            height = drawable.getIntrinsicHeight();
        }

        @Override
        protected void drawDataSet(Canvas c, IScatterDataSet dataSet) {
            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
            float phaseX = mAnimator.getPhaseX();
            float phaseY = mAnimator.getPhaseY();
            ScatterBuffer buffer = mScatterBuffers[0];
            buffer.setPhases(phaseX, phaseY);
            buffer.feed(dataSet);
            trans.pointValuesToPixel(buffer.buffer);
            int left, top;
            for (int i = 0; i < buffer.size(); i += 2) {
                if (!mViewPortHandler.isInBoundsRight(buffer.buffer[i])) break;
                if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[i]) || !mViewPortHandler.isInBoundsY(buffer.buffer[i + 1])) continue;
                left = (int) (buffer.buffer[i] - width / 2);
                top = (int) (buffer.buffer[i + 1] - height / 2);
                drawable.setBounds(left, top, left + width, top + height);
                drawable.draw(c);
            }
        }
    }


}
