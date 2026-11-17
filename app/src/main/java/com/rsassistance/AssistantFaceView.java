package com.rsassistance;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Lightweight, custom-drawn RS avatar. Emotion changes are visual states;
 * this view does not claim to infer the user's emotions.
 */
public class AssistantFaceView extends View {
    public enum Emotion { IDLE, LISTENING, SPEAKING, HAPPY, THINKING, CONCERNED, SURPRISED, SLEEPING }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Emotion emotion = Emotion.IDLE;
    private float breath = 0f;

    public AssistantFaceView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2600);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(value -> {
            breath = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setEmotion(Emotion next) {
        if (next != null) {
            emotion = next;
            invalidate();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.34f;
        float pulse = 1f + breath * 0.018f;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx, cy, radius * 1.65f,
                new int[]{0x5530F0B0, 0x2230DDA0, 0x0030DDA0},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.65f * pulse, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xAA48E6B0);
        canvas.drawCircle(cx, cy, radius * pulse, paint);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0x5548E6B0);
        canvas.drawCircle(cx, cy, radius * 1.12f * pulse, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
                new int[]{0xFF173F39, 0xFF0B2424, 0xFF071719},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 0.91f, paint);
        paint.setShader(null);

        // Soft forehead highlight.
        paint.setColor(0x223FFFF0);
        canvas.drawCircle(cx, cy - radius * 0.43f, radius * 0.58f, paint);

        float eyeY = cy - radius * 0.12f;
        float eyeGap = radius * 0.38f;
        float eyeWidth = radius * 0.19f;
        float eyeHeight = radius * 0.11f;

        paint.setColor(0xFFB8FFE9);
        if (emotion == Emotion.SLEEPING) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            canvas.drawArc(cx - eyeGap - eyeWidth, eyeY - eyeHeight,
                    cx - eyeGap + eyeWidth, eyeY + eyeHeight, 200, 140, false, paint);
            canvas.drawArc(cx + eyeGap - eyeWidth, eyeY - eyeHeight,
                    cx + eyeGap + eyeWidth, eyeY + eyeHeight, 200, 140, false, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            float height = emotion == Emotion.SURPRISED ? eyeHeight * 1.55f : eyeHeight;
            if (emotion == Emotion.HAPPY) height *= 0.58f;
            canvas.drawOval(cx - eyeGap - eyeWidth, eyeY - height,
                    cx - eyeGap + eyeWidth, eyeY + height, paint);
            canvas.drawOval(cx + eyeGap - eyeWidth, eyeY - height,
                    cx + eyeGap + eyeWidth, eyeY + height, paint);
            paint.setColor(0xFF1A6E5A);
            canvas.drawCircle(cx - eyeGap, eyeY, dp(2.5f), paint);
            canvas.drawCircle(cx + eyeGap, eyeY, dp(2.5f), paint);
        }

        float mouthY = cy + radius * 0.36f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xFF7DFFD1);
        Path mouth = new Path();
        if (emotion == Emotion.SPEAKING || emotion == Emotion.SURPRISED) {
            canvas.drawOval(cx - radius * 0.12f, mouthY - radius * 0.07f,
                    cx + radius * 0.12f, mouthY + radius * 0.12f, paint);
        } else {
            float curve = emotion == Emotion.HAPPY ? radius * 0.18f
                    : emotion == Emotion.CONCERNED ? -radius * 0.12f : radius * 0.025f;
            mouth.moveTo(cx - radius * 0.23f, mouthY);
            mouth.quadTo(cx, mouthY + curve, cx + radius * 0.23f, mouthY);
            canvas.drawPath(mouth, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF6BDBB6);
        paint.setTextSize(dp(11));
        paint.setTextAlign(Paint.Align.CENTER);
        String label = emotion.name().toLowerCase().replace('_', ' ');
        canvas.drawText(label, cx, cy + radius * 1.38f, paint);
    }
}
