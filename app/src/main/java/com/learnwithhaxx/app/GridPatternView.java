package com.learnwithhaxx.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridPatternView extends View {

    private Paint gridPaint;
    private int gridSpacingDp = 28; // size of each cell

    public GridPatternView(Context context) {
        super(context);
        init();
    }

    public GridPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(0x0A39D353); // #39D353 at ~4% opacity
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float spacing = gridSpacingDp * getResources().getDisplayMetrics().density;
        
        // Draw vertical lines
        for (float x = 0; x < getWidth(); x += spacing) {
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }
        // Draw horizontal lines
        for (float y = 0; y < getHeight(); y += spacing) {
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }
}
