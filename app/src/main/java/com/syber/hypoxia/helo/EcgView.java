package com.syber.hypoxia.helo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by liangtg on 16-7-19.
 */
public class EcgView extends View {
    private int position = 0;
    private float[] simu = {0, 1, 0, 0, 1.5f, 2, 1.5f, 1, 0, 0, 0, 0, -2.5f, 10, -1, 0, 0, 1, 0, 0};
    private Paint linePaint;
    private Paint dashPaint;
    private Paint pathPaint;
    private Path dashPath = new Path();
    private boolean start = false;
    private ArrayList<Float> data = new ArrayList<>();
    private Random random = new Random();

    public EcgView(Context context) {
        super(context);
        init();
    }

    public EcgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setMinimumWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics()));
        setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics()));
        linePaint = new Paint();
        linePaint.setColor(0xFFD97748);
        linePaint.setStrokeWidth(5);
        dashPaint = new Paint();
        dashPaint.setStrokeWidth(3);
        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setPathEffect(new DashPathEffect(new float[]{5, 3, 5, 3}, 0));
        dashPaint.setColor(0xFFD97748);
        pathPaint = new Paint();
        pathPaint.setStrokeWidth(3);
        pathPaint.setColor(0xFF000000);
        pathPaint.setStyle(Paint.Style.STROKE);
    }

    public void start(boolean start) {
        this.start = start;
        if (!start) {
            data.clear();
        }
    }

    public void addPoint(float value) {
        if (!start) return;
        data.add(0, value);
        if (data.size() > 65) data.remove(data.size() - 1);
    }

    @Override
    public void computeScroll() {
        if (start) {
            position++;
            if (position == simu.length) position = 0;
            if (data.size() > 65) data.remove(data.size() - 1);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawLines(canvas);
        if (!data.isEmpty()) {
            dashPath.reset();
            float width = getWidth() / 65f;
            dashPath.moveTo(0, matchData(0));
            for (int i = 1; i < data.size(); i++) {
                dashPath.lineTo(width * i, matchData(i));
            }
        }
        canvas.drawPath(dashPath, pathPaint);
    }

    private float matchData(int index) {
        return (1200 - data.get(index)) / 1200f * getHeight();
    }

    private void drawLines(Canvas canvas) {
        int width = getWidth() / 13;
        int height = getHeight() / 9;
        for (int i = 0; i < 14; i++) {
            canvas.drawLine(width * i, 0, width * i, getHeight(), linePaint);
            for (int j = 0; j < 4; j++) {
                dashPath.reset();
                int dashX = width * i + width / 5 * (j + 1);
                dashPath.moveTo(dashX, 0);
                dashPath.lineTo(dashX, getHeight());
                canvas.drawPath(dashPath, dashPaint);
            }
        }
        for (int i = 0; i < 10; i++) {
            canvas.drawLine(0, height * i, getWidth(), height * i, linePaint);
            for (int j = 0; j < 4; j++) {
                int dashY = height * i + height / 5 * (j + 1);
                dashPath.reset();
                dashPath.moveTo(0, dashY);
                dashPath.lineTo(getWidth(), dashY);
                canvas.drawPath(dashPath, dashPaint);
            }
        }
    }

}
