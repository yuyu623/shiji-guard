package com.shiji.guard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 今日 24 小时使用条。 */
public class DayStrip extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Repo.Sess> sessions = new ArrayList<>();
    private long dayStart;

    public DayStrip(Context c) {
        super(c);
    }

    public void setData(long dayStart, List<Repo.Sess> sessions) {
        this.dayStart = dayStart;
        this.sessions = sessions == null ? new ArrayList<>() : sessions;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 20));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float barH = Ui.dp(getContext(), 10);
        float top = (h - barH) / 2f;
        float r = barH / 2f;
        paint.setColor(Ui.TRACK);
        canvas.drawRoundRect(new RectF(0, top, w, top + barH), r, r, paint);

        Path clip = new Path();
        clip.addRoundRect(new RectF(0, top, w, top + barH), r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        paint.setColor(Ui.ACCENT);
        paint.setAlpha(225);
        for (Repo.Sess s : sessions) {
            if (s.end <= dayStart) continue;
            float x1 = (s.start - dayStart) / (float) TimeUtil.DAY * w;
            float x2 = (s.end - dayStart) / (float) TimeUtil.DAY * w;
            if (x2 < 0 || x1 > w) continue;
            x1 = Math.max(0, x1);
            x2 = Math.min(w, x2);
            canvas.drawRect(x1, top, Math.max(x1 + 2f, x2), top + barH, paint);
        }
        canvas.restore();

        long now = System.currentTimeMillis();
        if (now >= dayStart && now < dayStart + TimeUtil.DAY) {
            float x = (now - dayStart) / (float) TimeUtil.DAY * w;
            paint.setColor(Ui.NOW);
            paint.setStrokeWidth(Ui.dp(getContext(), 1));
            canvas.drawLine(x, top - Ui.dp(getContext(), 3), x, top + barH + Ui.dp(getContext(), 3), paint);
        }
    }
}
