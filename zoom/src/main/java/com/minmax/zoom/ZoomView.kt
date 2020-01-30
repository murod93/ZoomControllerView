package com.minmax.zoom

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.graphics.*
import android.util.Log
import kotlin.math.max


/**
 * Created by bo on 7/2/18.
 */

class ZoomView : View {

    private val TAG:String = this.javaClass.simpleName

    private var mTextSize: Int = 30
    private var mThumbRadius: Int = 0
    private var mTrackBgThickness: Int = 0
    private var mTrackFgThickness: Int = 0
    private var mThumbFgPaint: Paint? = null
    private var mTrackBgPaint: Paint? = null
    private var mTrackFgPaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mTrackRect: RectF? = null
    private var mListener: OnProgressChangeListener? = null
    private var mProgress = 0.0f
    private var mArrowHeight = 30
    private var mPaddingBetweenTextAndArrow = 10
    private var mMinZoomValue = 1
    private var mMaxZoomValue = 3

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs, defStyleAttr)
    }

    fun setThumbColor(color: Int) {
        mThumbFgPaint!!.color = color
        invalidate()
    }

    fun setTrackFgColor(color: Int) {
        mTrackFgPaint!!.color = color
        invalidate()
    }

    fun setTrackBgColor(color: Int) {
        mTrackBgPaint!!.color = color
        invalidate()
    }

    fun setThumbRadiusPx(radiusPx: Int) {
        mThumbRadius = radiusPx
        invalidate()
    }

    fun setTrackFgThicknessPx(heightPx: Int) {
        mTrackFgThickness = heightPx
        invalidate()
    }

    fun setTrackBgThicknessPx(heightPx: Int) {
        mTrackBgThickness = heightPx
        invalidate()
    }

    fun setTextColor(color: Int){
        mTextPaint!!.color = color
        invalidate()
    }

    fun setTextSize(size: Float){
        mTextPaint!!.textSize = size
    }

    fun setMaxValue(max:Int){
        this.mMaxZoomValue = max
    }

    fun setMinValue(min:Int){
        this.mMinZoomValue = min
    }

    @JvmOverloads
    fun setProgress(progress: Float, notifyListener: Boolean = false) {
        onProgressChanged(progress, notifyListener)
    }

    fun setOnSliderProgressChangeListener(listener: OnProgressChangeListener) {
        mListener = listener
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        // to support non-touchable environment
        isFocusable = true

        val colorDefaultBg = resolveAttrColor("colorControlNormal", COLOR_BG)
        val colorDefaultFg = resolveAttrColor("colorControlActivated", COLOR_FG)

        mThumbFgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mThumbFgPaint!!.style = Paint.Style.FILL
        mThumbFgPaint!!.color = colorDefaultFg

        mTrackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTrackBgPaint!!.style = Paint.Style.FILL
        mTrackBgPaint!!.color = colorDefaultBg

        mTrackFgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTrackFgPaint!!.style = Paint.Style.FILL
        mTrackFgPaint!!.color = colorDefaultFg

        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint!!.style = Paint.Style.FILL
        mTextPaint!!.color = COLOR_TEXT

        mTrackRect = RectF()

        val dm = resources.displayMetrics
        mThumbRadius =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, THUMB_RADIUS_FG.toFloat(), dm)
                .toInt()
        mTrackBgThickness =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TRACK_HEIGHT_BG.toFloat(), dm)
                .toInt()
        mTrackFgThickness =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TRACK_HEIGHT_FG.toFloat(), dm)
                .toInt()

        if (attrs != null) {
            val arr =
                context.obtainStyledAttributes(attrs, R.styleable.ZoomController, defStyleAttr, 0)
            val thumbColor =
                arr.getColor(R.styleable.ZoomController_zoom_thumb_color, mThumbFgPaint!!.color)
            mThumbFgPaint!!.color = thumbColor

            val trackColor =
                arr.getColor(R.styleable.ZoomController_zoom_track_fg_color, mTrackFgPaint!!.color)
            mTrackFgPaint!!.color = trackColor

            val trackBgColor =
                arr.getColor(R.styleable.ZoomController_zoom_track_bg_color, mTrackBgPaint!!.color)

            val textColor =
                arr.getColor(R.styleable.ZoomController_zoom_text_color, mTextPaint!!.color)

            mTrackBgPaint!!.color = trackBgColor

            mTextPaint!!.color = textColor

            mThumbRadius = arr.getDimensionPixelSize(
                R.styleable.ZoomController_zoom_pointer_radius,
                mThumbRadius
            )
            mTrackFgThickness = arr.getDimensionPixelSize(
                R.styleable.ZoomController_zoom_track_fg_thickness,
                mTrackFgThickness
            )
            mTrackBgThickness = arr.getDimensionPixelSize(
                R.styleable.ZoomController_zoom_track_bg_thickness,
                mTrackBgThickness
            )

            mTextSize = arr.getDimensionPixelSize(
                R.styleable.ZoomController_zoom_text_size,
                mTextSize
            )

            arr.recycle()
        }
    }

    private fun resolveAttrColor(attrName: String, defaultColor: Int): Int {
        val packageName = context.packageName
        val attrRes = resources.getIdentifier(attrName, "attr", packageName)
        if (attrRes <= 0) {
            return defaultColor
        }
        val value = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attrRes, value, true)
        return resources.getColor(value.resourceId)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)

        val contentWidth = paddingLeft + mThumbRadius * 2 + paddingRight
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        if (widthMode != MeasureSpec.EXACTLY) {
            width = max(contentWidth, suggestedMinimumWidth)
        }
        setMeasuredDimension(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val height = height - paddingTop - paddingBottom - 2 * mThumbRadius - mTextSize - mArrowHeight - mPaddingBetweenTextAndArrow
                onProgressChanged(1 - y / height, true)
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mProgress < 1f) {
                onProgressChanged(mProgress + 0.02f, true)
                return true
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (mProgress > 0f) {
                onProgressChanged(mProgress - 0.02f, true)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onProgressChanged(progress: Float, notifyChange: Boolean) {
        mProgress = progress
        if (mProgress < 0) {
            mProgress = 0f
        } else if (mProgress > 1f) {
            mProgress = 1f
        }
        invalidate()
        if (notifyChange && mListener != null) {
            mListener!!.onProgress(mProgress)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBgCircles(canvas, mThumbRadius)

        drawBottomArrow(canvas)

        drawTopArrow(canvas)

        val trackPadding = if (mTrackBgThickness > mTrackFgThickness) mTrackBgThickness - mTrackFgThickness shr 1 else 0

        drawPointerCircle(canvas, trackPadding)

        drawText(canvas)
    }

    /**
     * this function is used to draw zoom level
     * @param canvas
     * @param cX
     * @param cY
     */
    private fun drawText(canvas: Canvas){
        val numberString = (mMaxZoomValue*mProgress)+mMinZoomValue
        drawCenter(canvas, String.format("%.1fx", numberString))
    }

    private val r = Rect()

    private fun drawCenter(
        canvas: Canvas,
        text: String) {
        canvas.getClipBounds(r)
        val cWidth = r.width()
        mTextPaint!!.textAlign = Paint.Align.LEFT
        mTextPaint!!.textSize = 30f
        mTextPaint!!.getTextBounds(text, 0, text.length, r)
        val x = cWidth / 2f - r.width() / 2f - r.left
        val y = 50f//cHeight / 2f + r.height() / 2f - r.bottom
        canvas.drawText(text, x, y, mTextPaint!!)
    }


    /**
     * This function is used to draw pointer circle
     * @param canvas
     * @param trackPadding
     */
    private fun drawPointerCircle(
        canvas: Canvas,
        trackPadding: Int) {
        // draw bg thumb
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom - 2 * mThumbRadius - 2*mArrowHeight - 2 * trackPadding - mTextSize - mPaddingBetweenTextAndArrow

        val leftOffset = width - mThumbRadius * 2 shr 1

        mThumbFgPaint!!.style = Paint.Style.STROKE
        mThumbFgPaint!!.strokeWidth = 5f
        //This method draws circle
        canvas.drawCircle(
            (paddingLeft + leftOffset + mThumbRadius).toFloat(),
            paddingTop.toFloat() + mTextSize + mArrowHeight + mThumbRadius.toFloat() + mPaddingBetweenTextAndArrow + (1 - mProgress) * height + trackPadding.toFloat(),
            mThumbRadius.toFloat(),
            mThumbFgPaint!!
        )
    }

    /**
     * This function draws small circles along line
     * @param canvas
     * @param pointerRadius
     */
    private fun drawBgCircles(canvas: Canvas,
                              pointerRadius: Int){

        // drawing background with circles
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom - 2 * pointerRadius - 2*mArrowHeight - mTextSize - mPaddingBetweenTextAndArrow

        val numOfCircles = 15
        val smCircleRadius = 2

        val leftOffset = width - mThumbRadius * 2 shr 1

        mThumbFgPaint!!.style = Paint.Style.STROKE
        mThumbFgPaint!!.strokeWidth = 5f

        for (i in 0..numOfCircles){
            //This method draws circle
            canvas.drawCircle(
                (paddingLeft + leftOffset + pointerRadius).toFloat(),
                paddingTop.toFloat() + mTextSize + mArrowHeight + pointerRadius + (i * height/numOfCircles),
                smCircleRadius.toFloat()+(i/5),
                mThumbFgPaint!!
            )
        }
    }

    /**
     * This function draws bottom arrow
     * @param canvas
     */
    private fun drawBottomArrow(
        canvas: Canvas
    ){
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - mTextSize - mPaddingBetweenTextAndArrow

        mThumbFgPaint!!.style = Paint.Style.STROKE
        mThumbFgPaint!!.strokeWidth = 5f

        // startX, startY, stopX, stopY
        canvas.drawLine((width/2).toFloat(),
            height.toFloat()+mPaddingBetweenTextAndArrow,
            (paddingLeft + mThumbRadius).toFloat(),
            (height+24+mPaddingBetweenTextAndArrow).toFloat(),
            mThumbFgPaint!!)

        canvas.drawLine((paddingLeft + mThumbRadius).toFloat(),
            (height+24 + mPaddingBetweenTextAndArrow).toFloat(),
            (width - paddingRight + mThumbRadius).toFloat(),
            height.toFloat()+mPaddingBetweenTextAndArrow,
            mThumbFgPaint!!)
    }

    /**
     * This function draws top arrow
     * @param canvas
     */
    private fun drawTopArrow(
        canvas: Canvas
    ){
        val width = width - paddingLeft - paddingRight

        mThumbFgPaint!!.style = Paint.Style.STROKE
        mThumbFgPaint!!.strokeWidth = 5f

        // startX, startY, stopX, stopY
        canvas.drawLine((width/2).toFloat(),
            (mTextSize+paddingTop+30+mPaddingBetweenTextAndArrow).toFloat(),
            (paddingLeft + mThumbRadius).toFloat(),
            (mTextSize+paddingTop+mPaddingBetweenTextAndArrow).toFloat(),
            mThumbFgPaint!!)

        canvas.drawLine((paddingLeft + mThumbRadius).toFloat(),
            (mTextSize + paddingTop+mPaddingBetweenTextAndArrow).toFloat(),
            (width - paddingRight + mThumbRadius).toFloat(),
            (mTextSize + paddingTop + 30+mPaddingBetweenTextAndArrow).toFloat(),
            mThumbFgPaint!!)
    }

    /**
     * This function draws vertical slider line
     * @param canvas
     * @param thumbRadius
     * @param trackThickness
     * @param trackPadding
     * @param trackPaint
     * @param progress
     */
    private fun drawTrack(
        canvas: Canvas,
        thumbRadius: Int,
        trackThickness: Int,
        trackPadding: Int,
        trackPaint: Paint?,
        progress: Float
    ) {
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom - 2 * thumbRadius - mPaddingBetweenTextAndArrow

        val trackLeft = paddingLeft + (width - trackThickness shr 1)
        val trackRight = trackLeft + trackThickness
        val trackRadius = trackThickness * 0.5f

        val trackTop: Int

        trackTop =
            (paddingTop.toFloat() + thumbRadius.toFloat() + (1 - progress) * height).toInt() + trackPadding

        mTrackRect!!.set(
            trackLeft.toFloat(),
            trackTop.toFloat(),
            trackRight.toFloat(),
            (paddingTop + height + mThumbRadius).toFloat()
        )

        canvas.drawRoundRect(mTrackRect!!, trackRadius, trackRadius, trackPaint!!)
    }

    /**
     * This function draws background line for slider
     * @param canvas
     * @param thumbRadius
     * @param trackThickness
     * @param trackPadding
     * @param trackPaint
     * @param progress
     */
    private fun drawBgTrackLine(
        canvas: Canvas,
        thumbRadius: Int,
        trackThickness: Int,
        trackPadding: Int,
        trackPaint: Paint?,
        progress: Float
    ) {
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom - 2 * thumbRadius

        val trackLeft = paddingLeft + (width - trackThickness shr 1)
        val trackTop =
            (paddingTop.toFloat() + thumbRadius.toFloat() + (1 - progress) * height).toInt() + trackPadding
        val trackRight = trackLeft + trackThickness
        val trackBottom = getHeight() - paddingBottom - thumbRadius - trackPadding
        val trackRadius = trackThickness * 0.5f
        mTrackRect!!.set(
            trackLeft.toFloat(),
            trackTop.toFloat(),
            trackRight.toFloat(),
            trackBottom.toFloat()
        )

        canvas.drawRoundRect(mTrackRect!!, trackRadius, trackRadius, trackPaint!!)
    }

    /**
     * This method draws vertical slider
     * @param canvas
     */
    private fun onDrawSLider(canvas: Canvas){
        //this method draws background of slider
        drawBgTrackLine(canvas, mThumbRadius, mTrackBgThickness, 0, mTrackBgPaint, 1f)

        val trackPadding = if (mTrackBgThickness > mTrackFgThickness) mTrackBgThickness - mTrackFgThickness shr 1 else 0
        //this method draws pointer
        drawPointerCircle(canvas, trackPadding)

        //this method draws slider
        drawTrack(canvas, mThumbRadius, mTrackFgThickness, trackPadding, mTrackFgPaint, mProgress)
    }

    interface OnProgressChangeListener {
        fun onProgress(progress: Float)
    }

    companion object {

        private val THUMB_RADIUS_FG = 6

        private const val TRACK_HEIGHT_BG = 4
        private const val TRACK_HEIGHT_FG = 2

        private val COLOR_BG = Color.parseColor("#dddfeb")
        private val COLOR_FG = Color.parseColor("#7da1ae")
        private val COLOR_TEXT = Color.parseColor("#ffffff")
    }
}
