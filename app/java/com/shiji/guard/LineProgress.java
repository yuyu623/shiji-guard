package com.shiji.guard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class LineProgress extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private int color = Ui.ACCENT;

    public LineProgress(Context c) {
        super(c);
    }

    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    public void setColor(int c) {
        color = c;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, Ui.dp(getContext(), 3));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float r = h / 2f;
        paint.setColor(Ui.TRACK);
        canvas.drawRoundRect(new RectF(0, 0, getWidth(), h), r, r, paint);
        if (progress > 0) {
            paint.setColor(color);
            canvas.drawRoundRect(new RectF(0, 0, Math.max(h, getWidth() * progress), h), r, r, paint);
        }
    }
}
