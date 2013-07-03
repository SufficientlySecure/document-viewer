/**
 * Copyright (C) 2009 Mark Wyszomierski.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.emdev.ui.widget;

import org.ebookdroid.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a simple TextView-like control which allows us to set a max number
 * of lines to wrap an input string, then ellipsizes the last line if there's
 * not enough room to handle the entire input string.
 *
 * This widget can be in a collapsed or expanded mode. In collapsed mode, the
 * maxLines parameter is honored, and ellipsizing will occur if there's not
 * enough space for the input string. In expanded mode, the widget takes up
 * as much height as necessary for the input string. You can toggle the mode
 * to expand/collapse the widget in-place, via a click-handler for example.
 *
 * This widget does not handle right to left text, spannables, it's pretty
 * basic.
 *
 * The basis for this widget is taken from the android custom widget doc:
 * http://developer.android.com/guide/samples/ApiDemos/src/com/example/android/apis/view/LabelView.html
 *
 * @author Mark Wyszomierski
 */
public class TextViewMultilineEllipse extends View {

    private TextPaint mTextPaint;

    private ColorStateList mTextColor;
    private int mCurTextColor;

    private CharSequence mText;
    private int mAscent;
    private String mStrEllipsis;
    private String mStrEllipsisMore;
    private int mMaxLines;
    private boolean mDrawEllipsizeMoreString;
    private int mColorEllipsizeMore;
    private boolean mRightAlignEllipsizeMoreString;
    private boolean mExpanded;
    private LineBreaker mBreakerExpanded;
    private LineBreaker mBreakerCollapsed;

    public TextViewMultilineEllipse(Context context) {
        super(context);

        init();
    }

    public TextViewMultilineEllipse(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.org_emdev_ui_widget_TextViewMultilineEllipse);

        CharSequence s = a.getString(R.styleable.org_emdev_ui_widget_TextViewMultilineEllipse_text);
        if (s != null) {
            setText(s.toString());
        }

        // Retrieve the color(s) to be used for this view and apply them.
        // Note, if you only care about supporting a single color, that you
        // can instead call a.getColor() and pass that to setTextColor().
        setTextColor(a.getColorStateList(R.styleable.org_emdev_ui_widget_TextViewMultilineEllipse_textColor));

        int textSize = a.getDimensionPixelOffset(R.styleable.org_emdev_ui_widget_TextViewMultilineEllipse_textSize, 0);
        if (textSize > 0) {
            setTextSize(textSize);
        }

        mMaxLines = a.getInt(R.styleable.org_emdev_ui_widget_TextViewMultilineEllipse_maxLines, -1);

        a.recycle();
    }

    private void init() {
        mExpanded = false;
        mDrawEllipsizeMoreString = true;
        mRightAlignEllipsizeMoreString = false;
        mMaxLines = -1;
        mStrEllipsis = "...";
        mStrEllipsisMore = "";
        mColorEllipsizeMore = 0xFF0000FF;

        mBreakerExpanded = new LineBreaker();
        mBreakerCollapsed = new LineBreaker();

        // Default font size and color.
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(13);
        mTextPaint.setColor(0xFF000000);
        mTextPaint.setTextAlign(Align.LEFT);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mTextColor != null && mTextColor.isStateful()) {
            updateTextColors();
        }
    }

    /**
     * Sets the text to display in this widget.
     *
     * @param text
     *            The text to display.
     */
    public void setText(CharSequence text) {
        mText = text;
        requestLayout();
        invalidate();
    }

    /**
     * Sets the text size for this widget.
     *
     * @param size
     *            Font size.
     */
    public void setTextSize(int size) {
        setRawTextSize(size);
    }

    /**
     * Set the default text size to a given unit and value. See {@link TypedValue} for the possible dimension units.
     *
     * @param unit
     *            The desired dimension unit.
     * @param size
     *            The desired size in the given units.
     */
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();

        setRawTextSize(TypedValue.applyDimension(unit, size, r.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);

            requestLayout();
            invalidate();
        }
    }

    /**
     * Sets the text color for all the states (normal, selected,
     * focused) to be this color.
     */
    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    /**
     * Sets the text color.
     */
    public void setTextColor(ColorStateList colors) {
        final ColorStateList defList = Resources.getSystem().getColorStateList(android.R.color.primary_text_dark);
        mTextColor = colors != null ? colors : defList;
        updateTextColors();
    }

    private void updateTextColors() {
        boolean inval = false;
        int color = mTextColor.getColorForState(getDrawableState(), 0);
        if (color != mCurTextColor) {
            mCurTextColor = color;
            inval = true;
        }
        if (inval) {
            invalidate();
        }
    }

    /**
     * The string to append when ellipsizing. Must be shorter than the available
     * width for a single line!
     *
     * @param ellipsis
     *            The ellipsis string to use, like "...", or "-----".
     */
    public void setEllipsis(String ellipsis) {
        mStrEllipsis = ellipsis;
    }

    /**
     * Optional extra ellipsize string. This
     *
     * @param ellipsisMore
     */
    public void setEllipsisMore(String ellipsisMore) {
        mStrEllipsisMore = ellipsisMore;
    }

    /**
     * The maximum number of lines to allow, height-wise.
     *
     * @param maxLines
     */
    public void setMaxLines(int maxLines) {
        mMaxLines = maxLines;
    }

    /**
     * Turn drawing of the optional ellipsizeMore string on or off.
     *
     * @param drawEllipsizeMoreString
     *            Yes or no.
     */
    public void setDrawEllipsizeMoreString(boolean drawEllipsizeMoreString) {
        mDrawEllipsizeMoreString = drawEllipsizeMoreString;
    }

    /**
     * Font color to use for the optional ellipsizeMore string.
     *
     * @param color
     *            ARGB color.
     */
    public void setColorEllpsizeMore(int color) {
        mColorEllipsizeMore = color;
    }

    /**
     * When drawing the ellipsizeMore string, either draw it wherever ellipsizing on the last
     * line occurs, or always right align it. On by default.
     *
     * @param rightAlignEllipsizeMoreString
     *            Yes or no.
     */
    public void setRightAlignEllipsizeMoreString(boolean rightAlignEllipsizeMoreString) {
        mRightAlignEllipsizeMoreString = rightAlignEllipsizeMoreString;
    }

    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be.
            result = specSize;

            // Format the text using this exact width, and the current mode.
            breakWidth(specSize);
        } else {
            if (specMode == MeasureSpec.AT_MOST) {
                // Use the AT_MOST size - if we had very short text, we may need even less
                // than the AT_MOST value, so return the minimum.
                result = breakWidth(specSize);
                result = Math.min(result, specSize);
            } else {
                // We're not given any width - so in this case we assume we have an unlimited
                // width?
                breakWidth(specSize);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be, so nothing to do.
            result = specSize;
        } else {
            // The lines should already be broken up. Calculate our max desired height
            // for our current mode.
            int numLines;
            if (mExpanded) {
                numLines = mBreakerExpanded.getLines().size();
            } else {
                numLines = mBreakerCollapsed.getLines().size();
            }
            result = numLines * (int) (-mAscent + mTextPaint.descent()) + getPaddingTop() + getPaddingBottom();

            // Respect AT_MOST value if that was what is called for by measureSpec.
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Render the text
     *
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        List<int[]> lines;
        LineBreaker breaker;
        if (mExpanded) {
            breaker = mBreakerExpanded;
            lines = mBreakerExpanded.getLines();
        } else {
            breaker = mBreakerCollapsed;
            lines = mBreakerCollapsed.getLines();
        }

        mTextPaint.setColor(mCurTextColor);

        float x = getPaddingLeft();
        float y = getPaddingTop() + (-mAscent);
        for (int i = 0; i < lines.size(); i++) {
            // Draw the current line.
            int[] pair = lines.get(i);
            canvas.drawText(mText, pair[0], pair[1] + 1, x, y, mTextPaint);

            // Draw the ellipsis if necessary.
            if (i == lines.size() - 1) {
                if (breaker.getRequiredEllipsis()) {
                    canvas.drawText(mStrEllipsis, x + breaker.getLengthLastEllipsizedLine(), y, mTextPaint);
                    if (mDrawEllipsizeMoreString) {
                        int lastColor = mTextPaint.getColor();
                        mTextPaint.setColor(mColorEllipsizeMore);
                        if (mRightAlignEllipsizeMoreString) {
                            // Seems to not be right...
                            canvas.drawText(mStrEllipsisMore, canvas.getWidth()
                                    - (breaker.getLengthEllipsisMore() + getPaddingRight() + getPaddingLeft()), y,
                                    mTextPaint);
                        } else {
                            canvas.drawText(mStrEllipsisMore, x + breaker.getLengthLastEllipsizedLinePlusEllipsis(), y,
                                    mTextPaint);
                        }
                        mTextPaint.setColor(lastColor);
                    }
                }
            }

            y += (-mAscent + mTextPaint.descent());
            if (y > canvas.getHeight()) {
                break;
            }
        }
    }

    public boolean getIsExpanded() {
        return mExpanded;
    }

    public void expand() {
        mExpanded = true;
        requestLayout();
        invalidate();
    }

    public void collapse() {
        mExpanded = false;
        requestLayout();
        invalidate();
    }

    private int breakWidth(int availableWidth) {
        int widthUsed = 0;
        if (mExpanded) {
            widthUsed = mBreakerExpanded.breakText(mText, availableWidth - getPaddingLeft() - getPaddingRight(),
                    mTextPaint);
        } else {
            widthUsed = mBreakerCollapsed.breakText(mText, mStrEllipsis, mStrEllipsisMore, mMaxLines, availableWidth
                    - getPaddingLeft() - getPaddingRight(), mTextPaint);
        }

        return widthUsed + getPaddingLeft() + getPaddingRight();
    }

    /**
     * Used internally to break a string into a list of integer pairs. The pairs are
     * start and end locations for lines given the current available layout width.
     */
    private static class LineBreaker {

        /** Was the input text long enough to need an ellipsis? */
        private boolean mRequiredEllipsis;

        /** Beginning and end indices for the input string. */
        private ArrayList<int[]> mLines;

        /** The width in pixels of the last line, used to draw the ellipsis if necessary. */
        private float mLengthLastLine;

        /**
         * The width of the ellipsis string, so we know where to draw the ellipsisMore string
         * if necessary.
         */
        private float mLengthEllipsis;

        /** The width of the ellipsizeMore string, same use as above. */
        private float mLengthEllipsisMore;

        public LineBreaker() {
            mRequiredEllipsis = false;
            mLines = new ArrayList<int[]>(100);
        }

        /**
         * Used for breaking text in 'expanded' mode, which needs no ellipse.
         * Uses as many lines as is necessary to accomodate the entire input
         * string.
         *
         * @param input
         *            String to be broken.
         * @param maxWidth
         *            Available layout width.
         * @param tp
         *            Current paint object with styles applied to it.
         */
        public int breakText(CharSequence input, int maxWidth, TextPaint tp) {
            return breakText(input, null, null, -1, maxWidth, tp);
        }

        /**
         * Used for breaking text, honors ellipsizing. The string will be broken into lines using
         * the available width. The last line will subtract the physical width of the ellipsis
         * string from maxWidth to reserve room for the ellipsis. If the ellpsisMore string is set,
         * then space will also be reserved for its length as well.
         *
         * @param input
         *            String to be broken.
         * @param ellipsis
         *            Ellipsis string, like "..."
         * @param ellipsisMore
         *            Optional space reservation after the ellipsis, like " Read More!"
         * @param maxLines
         *            Max number of lines to allow before ellipsizing.
         * @param maxWidth
         *            Available layout width.
         * @param tp
         *            Current paint object with styles applied to it.
         */
        public int breakText(CharSequence input, String ellipsis, String ellipsisMore, int maxLines, int maxWidth,
                TextPaint tp) {
            mLines.clear();
            mRequiredEllipsis = false;
            mLengthLastLine = 0.0f;
            mLengthEllipsis = 0.0f;
            mLengthEllipsisMore = 0.0f;

            // If maxWidth is -1, interpret that as meaning to render the string on a single
            // line. Skip everything.
            if (maxWidth == -1) {
                mLines.add(new int[] { 0, input.length() });
                return (int) (tp.measureText(input, 0, input.length()) + 0.5f);
            }

            // Measure the ellipsis string, and the ellipsisMore string if valid.
            if (ellipsis != null) {
                mLengthEllipsis = tp.measureText(ellipsis);
            }
            if (ellipsisMore != null) {
                mLengthEllipsisMore = tp.measureText(ellipsisMore);
            }

            // Start breaking.
            int posStartThisLine = -1;
            float lengthThisLine = 0.0f;
            boolean breakWords = true;
            int pos = 0;
            float[] widths = new float[input.length()];
            tp.getTextWidths(input, 0, input.length(), widths);
            while (pos < input.length()) {

                if (posStartThisLine == -1) {
                    posStartThisLine = pos;
                }

                if (mLines.size() == maxLines) {
                    mRequiredEllipsis = true;
                    break;
                }

                float widthOfChar = widths[pos];
                boolean newLineRequired = false;

                // Check for a new line character or if we've run over max width.
                if (input.charAt(pos) == '\n') {
                    newLineRequired = true;

                    // We want the current line to go up to the character right before the
                    // new line char, and we want the next line to start at the char after
                    // this new line char.
                    mLines.add(new int[] { posStartThisLine, pos - 1 });
                } else if (lengthThisLine + widthOfChar >= maxWidth) {
                    newLineRequired = true;
                    // We need to backup if we are in the middle of a word.
                    if (input.charAt(pos) == ' ' || breakWords == false) {
                        // Backup one character, because it doesn't fit on this line.
                        pos--;

                        // So this line includes up to the character before the space.
                        mLines.add(new int[] { posStartThisLine, pos });
                    } else {
                        // Backup until we are at a space.
                        while (pos >= 0 && input.charAt(pos) != ' ') {
                            pos--;
                        }

                        // This line includes up to the space.
                        mLines.add(new int[] { posStartThisLine, pos });
                    }
                }

                if (newLineRequired) {
                    // The next cycle should reset the position if it sees it's -1 (to whatever i is).
                    posStartThisLine = -1;

                    // Reset line length for next iteration.
                    lengthThisLine = 0.0f;

                    // When we get to the last line, subtract the width of the ellipsis.
                    if (mLines.size() == maxLines - 1) {
                        maxWidth -= (mLengthEllipsis + mLengthEllipsisMore);
                        // We also don't need to break on a full word, it'll look a little
                        // cleaner if all breaks on the final lines break in the middle of
                        // the last word.
                        breakWords = false;
                    }
                } else {
                    lengthThisLine += widthOfChar;

                    // If we're on the last character of the input string, add on whatever we have leftover.
                    if (pos == input.length() - 1) {
                        mLines.add(new int[] { posStartThisLine, pos });
                    }
                }

                pos++;
            }

            // If we ellipsized, then add the ellipsis string to the end.
            if (mRequiredEllipsis) {
                int[] pairLast = mLines.get(mLines.size() - 1);
                final CharSequence subSequence = input.subSequence(pairLast[0], pairLast[1] + 1);
                mLengthLastLine = tp.measureText(subSequence, 0, subSequence.length());
            }

            // If we required only one line, return its length, otherwise we used
            // whatever the maxWidth supplied was.
            if (mLines.size() == 0) {
                return 0;
            } else if (mLines.size() == 1) {
                return (int) (tp.measureText(input, 0, input.length()) + 0.5f);
            } else {
                return maxWidth;
            }
        }

        public boolean getRequiredEllipsis() {
            return mRequiredEllipsis;
        }

        public List<int[]> getLines() {
            return mLines;
        }

        public float getLengthLastEllipsizedLine() {
            return mLengthLastLine;
        }

        public float getLengthLastEllipsizedLinePlusEllipsis() {
            return mLengthLastLine + mLengthEllipsis;
        }

        public float getLengthEllipsisMore() {
            return mLengthEllipsisMore;
        }
    }
}
