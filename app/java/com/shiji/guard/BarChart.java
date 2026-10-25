package com.shiji.guard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

public class BarChart extends View {

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long[] values = new long[0];
    private String[] labels = new String[0];
    private int highlight = -1;

    public BarChart(Context c) {
        super(c);
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(long[] values, String[] labels, int highlight) {
        this.values = values == null ? new long[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        this.highlight = highlight;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 168));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = values.length;
        if (n == 0) return;
        Context c = getContext();
        float labelH = Ui.dp(c, 24);
        float base = getHeight() - labelH;
        float chartH = base - Ui.dp(c, 6);
        long max = 1;
        for (long v : values) max = Math.max(max, v);

        float slot = getWidth() / (float) n;
        float barW = Math.min(slot * 0.5f, Ui.dp(c, 10));

        bar.setColor(Ui.TRACK);
        bar.setStrokeWidth(1);
        canvas.drawLine(0, base, getWidth(), base, bar);

        for (int i = 0; i < n; i++) {
            float hgt = (values[i] / (float) max) * chartH;
            if (values[i] > 0 && hgt < Ui.dp(c, 3)) hgt = Ui.dp(c, 3);
            float left = slot * i + (slot - barW) / 2f;
            float top = base - hgt;
            if (i == highlight) {
                bar.setColor(Ui.ACCENT);
            } else if (values[i] > 0) {
                bar.setColor(Ui.BAR);
            } else {
                bar.setColor(Ui.BAR_EMPTY);
            }
            canvas.drawRoundRect(new RectF(left, top, left + barW, base), barW / 2f, barW / 2f, bar);

            boolean show = n <= 7 || i == 0 || i == n - 1 || (i % 5 == 0);
            if (show) {
                text.setColor(i == highlight ? Ui.ACCENT : Ui.FAINT);
                text.setTextSize(Ui.dp(c, 9.5f));
                canvas.drawText(labels[i], slot * i + slot / 2f, getHeight() - Ui.dp(c, 6), text);
            }
        }
    }
}
