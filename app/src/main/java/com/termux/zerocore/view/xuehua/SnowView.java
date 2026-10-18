package com.termux.zerocore.view.xuehua;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SnowView extends View {
    private static final int NUM_SNOWFLAKES = 150;
    private static final int DELAY = 5;


    private boolean run = true;
    private SnowFlake[] snowflakes;


    public SnowView(Context context) {
        super(context);
    }

    public SnowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SnowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void resize(int width, int height) {
        // A zero/negative size means the view is not measured yet; leave snowflakes null so
        // onDraw() can retry once a real size is known.
        if (!run || width <= 0 || height <= 0) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        snowflakes = new SnowFlake[NUM_SNOWFLAKES];
        for (int i = 0; i < NUM_SNOWFLAKES; i++) {
            snowflakes[i] = SnowFlake.create(width, height, paint);
        }
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (run) {
            if (w != oldw || h != oldh) {
                resize(w, h);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!run) {
            return;
        }
        // onDraw() may run before onSizeChanged() has allocated the array, so initialise it
        // lazily instead of iterating over null and crashing with a NullPointerException.
        if (snowflakes == null) {
            resize(getWidth(), getHeight());
        }
        if (snowflakes != null) {
            for (SnowFlake snowFlake : snowflakes) {
                if (snowFlake != null) {
                    snowFlake.draw(canvas);
                }
            }
        }
        android.os.Handler handler = getHandler();
        if (handler != null) {
            handler.postDelayed(runnable, DELAY);
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };
}
