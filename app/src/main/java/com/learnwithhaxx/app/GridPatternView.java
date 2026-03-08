package com.learnwithhaxx.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridPatternView extends View {

    private Paint gridPaint;
    private int gridSpacingDp = 24; // slightly smaller grid cells

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
        // Bright green lines with low opacity (approx 15% opacity)
        gridPaint.setColor(0x2639D353); 
        gridPaint.setStrokeWidth(2f); // thicker lines for "check" feel
        gridPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float spacing = gridSpacingDp * getResources().getDisplayMetrics().density;
        
        // Draw vertical lines
        for (float x = 0; x <= getWidth(); x += spacing) {
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }
        // Draw horizontal lines
        for (float y = 0; y <= getHeight(); y += spacing) {
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }
}
