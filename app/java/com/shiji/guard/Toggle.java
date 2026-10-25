package com.shiji.guard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 极简自绘开关，避免各厂商 Switch 样式差异。 */
public class Toggle extends View {

    public interface Listener {
        void onChanged(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean checked;
    private Listener listener;

    public Toggle(Context c) {
        super(c);
        setClickable(true);
        setFocusable(true);
    }

    public void setChecked(boolean value) {
        if (checked == value) return;
        checked = value;
        invalidate();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Ui.dp(getContext(), 46), Ui.dp(getContext(), 26));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float r = h / 2f;
        paint.setColor(checked ? Ui.ACCENT : Ui.TRACK);
        canvas.drawRoundRect(new RectF(0, 0, w, h), r, r, paint);
        float radius = h / 2f - Ui.dp(getContext(), 3);
        float cx = checked ? w - r : r;
        paint.setColor(checked ? Ui.BG : Ui.DIM);
        canvas.drawCircle(cx, r, radius, paint);
    }

    @Override
    public boolean performClick() {
        checked = !checked;
        invalidate();
        if (listener != null) listener.onChanged(checked);
        return true;
    }
}
