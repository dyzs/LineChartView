package cn.dyzs.view.linechartview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * @author dyzs Created on 2018/2/11.
 */
public class LineChartView extends View {
    private Context mCtx;
    private ArrayList<ViewItem> mListData;
    private ArrayList<Point> mListPoints;

    private Paint mLineXYPaint;// 表格 x,y 轴画笔
    private Paint mTextPaint;// 文本画笔
    private Paint mSelectionPointPaint;// 选中项画笔
    private Paint mDottedPaint;// 虚线画笔
    private Paint mChartLinePaint;// 绘制折线的画笔
    private Paint mGradientPathPaint;// 渐变背景画笔
    private Paint mXYAxisPointsPaint;// xy 轴点的颜色画笔

    private int mSelection = 0;// 选中项
    private float mViewWidth, mViewHeight;// view 实际宽高
    private float mXAxisStart = 0f;// x 轴的起始位置
    private float mXAxisTerminal = 10f;// x 轴的终点位置
    private float mYAxisStart = 10f;// y 轴的起始位置

    private float mYAxisTerminal = 10f;// y 轴的终点位置
    private float mXAxisLength = 100f;// x 轴总长度
    private float mYAxisLength = 100f;// y 轴总长度
    private float mActualPointsLength = 90f;// x 轴上的实际显示区域总长度
    private float mPointSpacingWidth = 10f;// x 轴上的刻度点的平均长度
    private int mXAxisDisplayNumber = 4; // x 轴同时显示的个数, 实际值 + 1
    private int mYAxisDisplayNumber = 3;// y 轴显示的个数
    private int mYAxisPeakValue = 90;// 初始状态的 y 轴最大值

    private Path mChartLinePath;// 折线路径
    private Path mReplacePath;// path measure 的路径, 用来替换定义的 path 路径
    private PathMeasure mLinePathMeasure;
    private float mPathSegment;
    private ValueAnimator mPathMeasureAnimator;
    private boolean isPlayLine = false, mDisplayYAxis = true;

    private Path mGradientPath;// 用来显示折线图的渐变颜色的路径
    private LinearGradient mChartGradient;// 折线图的渐变参数
    private RectF mChartGradientRectFArea;// 用来计算渐变动画时的设置渐变背景的动画区域
    // {背景颜色, 折线图颜色, 选中点颜色, xy 轴颜色, xy 轴刻度点的颜色, 文本颜色}
    private int mBgColor, mChartLineColor, mSelectionColor, mXYAxisColor, mXYAxisPointColor, mTextColor;

    private float mChartLineSize;
    // {用来判断 touch time & up time 的时间差}
    private long mTouchDownTime;
    private long mPerLineDuration;// 两点成线的动画持续时间

    private static final int[] SYS_ATTRS = new int[] {
        android.R.attr.background
    };

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mCtx = context;

        TypedArray ta = context.obtainStyledAttributes(attrs, SYS_ATTRS);
        mBgColor = ta.getColor(0, ContextCompat.getColor(context, R.color.white));
        ta.recycle();
        setBackgroundColor(mBgColor);

        ta = context.obtainStyledAttributes(attrs, R.styleable.LineChartViewDYZS);
        mChartLineSize = ta.getDimension(R.styleable.LineChartViewDYZS_lcvLineSize, 5f);
        mChartLineColor = ta.getColor(R.styleable.LineChartViewDYZS_lcvLineColor, ContextCompat.getColor(context, R.color.oxygen_green));
        mSelectionColor = ta.getColor(R.styleable.LineChartViewDYZS_lcvSelectionColor, ContextCompat.getColor(context, R.color.oxygen_green));
        mXYAxisColor = ta.getColor(R.styleable.LineChartViewDYZS_lcvXYAxisColor, ContextCompat.getColor(context, R.color.alice_white));
        mXYAxisPointColor = ta.getColor(R.styleable.LineChartViewDYZS_lcvXYAxisPointColor, ContextCompat.getColor(context, R.color.alice_white));
        mTextColor = ta.getColor(R.styleable.LineChartViewDYZS_lcvTextColor, ContextCompat.getColor(context, R.color.oxygen_grey));
        mDisplayYAxis = ta.getBoolean(R.styleable.LineChartViewDYZS_lcvYAxisDisplay, true);
        mPerLineDuration = ta.getInteger(R.styleable.LineChartViewDYZS_lcvPerLineDuration, 300);
        mXAxisDisplayNumber = ta.getInteger(R.styleable.LineChartViewDYZS_lcvXAxisDisplayNumber, 4);
        ta.recycle();
        mPerLineDuration = mPerLineDuration < 100 ? 100 : mPerLineDuration;

        initialize();

        playLineAnimation();
    }

    private void initialize() {
        mListData = new ArrayList<>();
        mListPoints = new ArrayList<>();

        initParams();

        initPaintAndPath();
    }

    private void initParams() {
        mXAxisStart = 10f;

    }

    private void initPaintAndPath() {
        mLineXYPaint = new Paint();
        mLineXYPaint.setAntiAlias(true);
        mLineXYPaint.setColor(mXYAxisColor);
        mLineXYPaint.setStrokeWidth(5f);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStrokeWidth(4f);
        mTextPaint.setTextSize(dp2Px(15));// 初始化 15sp

        mDottedPaint = new Paint();
        mDottedPaint.setAntiAlias(true);
        mDottedPaint.setStyle(Paint.Style.STROKE);
        mDottedPaint.setStrokeWidth(1f);
        mDottedPaint.setColor(ContextCompat.getColor(getContext(), R.color.oxygen_grey));
        mDottedPaint.setStrokeCap(Paint.Cap.ROUND);

        mSelectionPointPaint = new Paint();
        mSelectionPointPaint.setAntiAlias(true);
        mSelectionPointPaint.setColor(mSelectionColor);
        mSelectionPointPaint.setStyle(Paint.Style.FILL);

        mChartLinePath = new Path();
        mLinePathMeasure = new PathMeasure();
        mChartLinePaint = new Paint();
        mChartLinePaint.setAntiAlias(true);
        mChartLinePaint.setColor(mChartLineColor);
        mChartLinePaint.setStrokeWidth(mChartLineSize);
        mChartLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mChartLinePaint.setStyle(Paint.Style.STROKE);
        mChartLinePaint.setShadowLayer(2, 0, 0, Color.WHITE);

        mReplacePath = new Path();
        mGradientPath = new Path();
        mGradientPathPaint = new Paint();
        mGradientPathPaint.setAntiAlias(true);
        mGradientPathPaint.setStyle(Paint.Style.FILL);
        mChartGradientRectFArea = new RectF();

        mXYAxisPointsPaint = new Paint();
        mXYAxisPointsPaint.setAntiAlias(true);
        mXYAxisPointsPaint.setStyle(Paint.Style.FILL);
        mXYAxisPointsPaint.setColor(mXYAxisPointColor);
    }

    /**
     * calc initialize value
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthPixels = getResources().getDisplayMetrics().widthPixels;
        mViewHeight = MeasureSpec.getSize(heightMeasureSpec) * 1.0f;

        // 临时计算 x 轴参数
        mXAxisStart = widthPixels * 0.1f;
        mXAxisTerminal = widthPixels - widthPixels * 0.05f;
        mXAxisLength = mXAxisTerminal - mXAxisStart;

        // 计算 points, points 点的存在范围应该在 line 中，所以开始、结束同时向内缩小 0.05f
        mActualPointsLength = mXAxisLength - mXAxisStart;
        mPointSpacingWidth = mActualPointsLength / mXAxisDisplayNumber;

        // 计算 y 轴参数
        mYAxisStart = mViewHeight * 0.1f;
        mYAxisTerminal = mViewHeight * 0.85f;
        mYAxisLength = mYAxisTerminal - mYAxisStart;

        // 计算点数据的距离
        if (mListData != null && mListData.size() > 0) {
            mListPoints.clear();
            Point point;
            for (int i = 0; i < mListData.size(); i ++) {
                point = new Point();
                float pX = mPointSpacingWidth * i + mXAxisStart + mXAxisStart / 2;
                point.x = (int) pX;
                float pY = mYAxisLength / mYAxisPeakValue * (mYAxisPeakValue * 1f - mListData.get(i).getPointValue());
                point.y = (int) (pY + mYAxisStart);
                mListPoints.add(point);
            }
            mViewWidth = (int) (mListPoints.get(mListPoints.size() - 1).x + mXAxisStart);
        }
        mViewWidth = mViewWidth > widthPixels ? mViewWidth : widthPixels;

        // 重新计算 X 轴的 terminal 最大值
        mXAxisTerminal = mViewWidth - widthPixels * 0.05f;

        // 设置自己的宽高
        setMeasuredDimension((int) mViewWidth, (int) mViewHeight);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /* 绘制 x 轴和 x 轴刻度, 同时绘制相对于 x 轴刻度的表格虚线 */
        drawXAxisAndPoints(canvas);

        /* 绘制 y 轴和 y 轴刻度, 同时绘制相对于 y 轴刻度的表格虚线 */
        drawYAxisAndMarks(canvas);

        /* 绘制折线, 折线渐变, 播放动画*/
        drawChartLineAndPoint(canvas);

        /* final step, 绘制折线点对应的价格 */
        drawPointTextAndRect(canvas);
    }


    private void drawChartLineAndPoint(Canvas canvas) {
        if (mListPoints == null || mListPoints.size() <= 0) {return;}
        for (int i = 0; i < mListPoints.size(); i ++) {
            if (mSelection == i) {// 绘制选中的折线上的点
                canvas.drawCircle(mListPoints.get(i).x, mListPoints.get(i).y, mChartLineSize * 2f, mSelectionPointPaint);
            }
        }

        if (mListPoints != null && mListPoints.size() > 0) {
            // 设置折线路径
            mChartLinePath.reset();
            mChartLinePath.moveTo(mListPoints.get(0).x, mListPoints.get(0).y);

            // 设置渐变路径
            mGradientPath.reset();
            mGradientPath.moveTo(mListPoints.get(0).x, mYAxisTerminal);// 起始点
            for (int i = 0; i < mListPoints.size(); i ++) {
                mChartLinePath.lineTo(mListPoints.get(i).x, mListPoints.get(i).y);
                mGradientPath.lineTo(mListPoints.get(i).x, mListPoints.get(i).y);
            }
            // 封闭 path 渐变, 连接折线图的最后一个点到 x 轴上的点
            mGradientPath.lineTo(mListPoints.get(mListPoints.size() - 1).x, mYAxisTerminal);
            mGradientPath.close();

            // 初始化渐变, 设置为垂直渐变
            mChartGradient = new LinearGradient(
                    (mListPoints.get(mListPoints.size() - 1).x - mListPoints.get(0).x) / 2,
                    mYAxisStart - mYAxisStart * 5f,
                    (mListPoints.get(mListPoints.size() - 1).x - mListPoints.get(0).x) / 2,
                    mYAxisTerminal,
                    new int[]{mChartLineColor, Color.TRANSPARENT},
                    new float[]{0f, 1f}, // 颜色数组对应的 position, 若为 null 表示颜色渐变平均分布
                    Shader.TileMode.CLAMP
            );
            mGradientPathPaint.setShader(mChartGradient);

            if (isPlayLine) {
                // 播放动画, 设置 path measure 片段
                mReplacePath.reset();
                mLinePathMeasure.setPath(mChartLinePath, false);
                mLinePathMeasure.getSegment(0, mPathSegment * mLinePathMeasure.getLength(), mReplacePath, true);
                canvas.drawPath(mReplacePath, mChartLinePaint);

                // 播放渐变背景
                canvas.save();
                canvas.clipPath(mGradientPath, Region.Op.INTERSECT);// 裁剪取交集
                mChartGradientRectFArea = new RectF(
                        mListPoints.get(0).x,
                        mYAxisStart,
                        mPathSegment * mListPoints.get(mListPoints.size() - 1).x,
                        mYAxisTerminal);
                canvas.drawRect(mChartGradientRectFArea, mGradientPathPaint);
                canvas.restore();
            } else {
                canvas.drawPath(mChartLinePath, mChartLinePaint);
                canvas.drawPath(mGradientPath, mGradientPathPaint);
            }
        }
    }

    private void drawYAxisAndMarks(Canvas canvas) {
        if (mDisplayYAxis) {
            canvas.drawLine(mXAxisStart, mYAxisStart, mXAxisStart, mYAxisTerminal, mLineXYPaint);
        }
        // 计算每个 dotted line 的高度
        float perDottedLineHeight = (mYAxisTerminal - mYAxisStart) / 3;
        for (int i = 0; i <= mYAxisDisplayNumber; i++) {
            float y = mYAxisTerminal - perDottedLineHeight * i;
            if (i > 0) {
                drawDottedLine(canvas, mXAxisStart, y, mXAxisTerminal, y);
            }

            if (mDisplayYAxis) {
                drawYAxisText(canvas, i, mXAxisStart, y);

                canvas.drawCircle(mXAxisStart, y, 5f, mXYAxisPointsPaint);
            }
        }
    }

    /**
     * 绘制 x 轴, x 轴上的虚线, x 轴上的 mark 点
     * @param canvas
     */
    private void drawXAxisAndPoints(Canvas canvas) {
        canvas.drawLine(mXAxisStart, mYAxisTerminal, mXAxisTerminal, mYAxisTerminal, mLineXYPaint);
        for (int i = 0; i <= getXAxisDisplayCount(); i++) {
            float fx = mPointSpacingWidth * i + mXAxisStart * 1.5f;
            float fy = mYAxisTerminal;
            drawDottedLine(canvas, fx, mYAxisStart, fx, fy);

            canvas.drawCircle(fx, fy, 5f, mXYAxisPointsPaint);

            String text = mListData.get(i).getAxisText();
            float textTotalWidth = mTextPaint.measureText(text);
            fx -= textTotalWidth / 2;
            fy += FontMatrixUtils.calcTextHalfHeightPoint(mTextPaint) + textTotalWidth * 0.3f;
            canvas.drawText(text, fx, fy, mTextPaint);
        }
    }

    private int getXAxisDisplayCount() {
        return mListPoints.size() - 1;
        // return mXAxisDisplayNumber > mListPoints.size() ? mXAxisDisplayNumber : mListPoints.size();
    }

    private void drawDottedLine(Canvas canvas, float startX, float startY, float stopX, float stopY) {
        mDottedPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 5));
        Path mPath = new Path();
        mPath.reset();
        mPath.moveTo(startX, startY);
        mPath.lineTo(stopX, stopY);
        canvas.drawPath(mPath, mDottedPaint);
    }

    private void drawYAxisText (Canvas canvas, int i, float x, float y) {
        int text = mYAxisPeakValue / mYAxisDisplayNumber * i;
        float textWidth = mTextPaint.measureText(text + "");
        float fx = x - textWidth - mXAxisStart * 0.1f;
        float fy = y + FontMatrixUtils.calcTextHalfHeightPoint(mTextPaint) / 2;
        canvas.drawText(text + "", fx, fy, mTextPaint);
    }

    /**
     * 绘制折线点对应的价格
     */
    private void drawPointTextAndRect(Canvas canvas) {
        if (mListPoints == null || mListPoints.size() <= 0) {return;}

        mTextPaint.setTextSize(dp2Px(10));
        float currencySymbolWidth = mTextPaint.measureText("$");
        mTextPaint.setTextSize(dp2Px(15));
        String text;
        float textTotalWidth;
        float textHeight = FontMatrixUtils.calcTextHalfHeightPoint(mTextPaint);
        for (int i = 0; i < mListPoints.size(); i ++) {
            if (i != mSelection)continue;
            float pY = mListPoints.get(i).y;// 中心点为 line 向上的一半
            text = mListData.get(i).getPointText();
            textTotalWidth = currencySymbolWidth + mTextPaint.measureText(text);
            // draw currency symbol text
            mTextPaint.setTextSize(dp2Px(10));
            canvas.drawText(
                    "$",
                    mListPoints.get(i).x - textTotalWidth / 2,
                    pY - textHeight * 0.3f,
                    mTextPaint);

            mTextPaint.setTextSize(dp2Px(15));
            canvas.drawText(
                    text,
                    mListPoints.get(i).x - (textTotalWidth - currencySymbolWidth) / 2 + dp2Px(3),
                    pY - textHeight * 0.3f,
                    mTextPaint);
        }
    }

    /**
     * 开启 path measure 绘制 path 路径
     */
    public void playLineAnimation() {
        isPlayLine = true;
        mPathMeasureAnimator = new ValueAnimator().ofFloat(0, 1);
        mPathMeasureAnimator.setDuration(mPerLineDuration * mListPoints.size());
        mPathMeasureAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPathSegment = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mPathMeasureAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isPlayLine = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mPathMeasureAnimator.start();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isPlayLine)return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
        }
        return true;
    }

    private void handleActionUp(MotionEvent event) {
        long upTime = System.currentTimeMillis();
        if (upTime - mTouchDownTime < 100) {
            float upX = event.getX();
            float upY = event.getY();
            float l, t, r , b;
            RectF rectF;
            float touchLimit = mPointSpacingWidth / 2;
            for (int i = 0; i < mListPoints.size(); i ++) {
                l = mListPoints.get(i).x - touchLimit;
                t = mListPoints.get(i).y - touchLimit;
                r = mListPoints.get(i).x + touchLimit;
                b = mListPoints.get(i).y + touchLimit;
                rectF = new RectF(l, t, r, b);
                if (rectF.contains(upX, upY)) {
                    mSelection = i;
                    invalidate();
                    if (mListener != null) {
                        mListener.onPointClick(mSelection);
                    }
                }
            }
        }
    }

    private LineChartViewListener mListener;
    public void setOnLineChartViewListener (LineChartViewListener listener) {
        this.mListener = listener;
    }

    public interface LineChartViewListener {
        void onPointClick(int selection);
    }

    private float dp2Px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mCtx.getResources().getDisplayMetrics());
    }

    public static class ViewItem {
        private String xAxisText = "";
        @Deprecated
        private String pointText = "";// 已过时参数
        private float pointValue = 0f;// value 的值, 用来做具体计算
        @Deprecated
        private String xAxisDate = "";// 当前 item 的真实日期, 这个参数...貌似并没有什么卵用
        public String getAxisText () {
            return xAxisText;
        }

        public void setXAxisText (String text) {
            this.xAxisText = text;
        }

        @Deprecated
        public String getPointText() {
            return pointText;
        }

        @Deprecated
        public void setPointText(String price) {
            this.pointText = price;
        }

        public void setXAxisDate (String date) {
            this.xAxisDate = date;
        }

        public String getXAxisDate () {
            return xAxisDate;
        }

        public void setPointValue(float value) {
            this.pointValue = value;
        }

        public float getPointValue () {
            return pointValue;
        }
    }

    public void setData(ArrayList<ViewItem> listData) {
        this.mListData = listData;
        ArrayList<Float> temp = new ArrayList<>();
        for (int i = 0; i < mListData.size(); i++) {
            temp.add(mListData.get(i).getPointValue());
            mSelection = mListData.size() - 1;
        }
        if (temp.size() > 0) {
            float max = Collections.max(temp);
            resetPeakValue(max);
        }
        requestLayout();
    }

    /**
     * 先做一遍值比较, 再进行四舍五入, 然后再进行倍数计算
     * 重置 peak value, 计算最小公倍数, 符合则结束递归
     * @param listMax
     */
    private void resetPeakValue (float listMax) {
        mYAxisPeakValue = Math.round(Math.max(listMax, mYAxisPeakValue));
        if (mYAxisPeakValue % 30 != 0) {
            mYAxisPeakValue += 1;
            resetPeakValue(mYAxisPeakValue);
        }
    }

    public ArrayList<ViewItem> testLoadData(int items) {
        ArrayList<ViewItem> list = new ArrayList<>();
        for (int i = 0; i < items; i++) {
            ViewItem viewItem = new ViewItem();
            Random random = new Random();
            // int p = random.nextInt(mYAxisPeakValue + 50);
            float p = random.nextFloat() * 100f + 50f;
            viewItem.setPointText(p + "");
            viewItem.setPointValue(p);
            viewItem.setXAxisText(i + "月");
            list.add(viewItem);
        }
        return list;
    }
}
