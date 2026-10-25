package com.shiji.guard;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public abstract class Screen {

    protected final MainActivity act;
    protected final Context ctx;
    protected final ScrollView scroll;
    protected final LinearLayout root;

    protected Screen(MainActivity act) {
        this.act = act;
        this.ctx = act;
        scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        root = Ui.col(ctx);
        root.setPadding(Ui.dp(ctx, 24), Ui.dp(ctx, 8), Ui.dp(ctx, 24), Ui.dp(ctx, 30));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public View view() {
        return scroll;
    }

    public abstract void refresh();

    protected void clear() {
        root.removeAllViews();
    }

    protected void add(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null) {
            lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        root.addView(v, lp);
    }
}
