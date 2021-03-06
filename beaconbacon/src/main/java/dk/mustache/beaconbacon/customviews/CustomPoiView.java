package dk.mustache.beaconbacon.customviews;

/* CLASS NAME GOES HERE

Copyright (c) 2017 Mustache ApS

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.mustache.beaconbacon.R;
import dk.mustache.beaconbacon.data.BeaconBaconManager;

import static dk.mustache.beaconbacon.utils.Converter.dpToPx;

/**
 * Treat this as a Black Box
 */
public class CustomPoiView {
    private Context context;

    public Bitmap iconBitmap;

    private Bitmap infoBoxText;
    private Bitmap infoBoxArrow;
    public TextView infoWindowText;

    public String title;

    public float radius;

    public float cx = -1;
    public float cy = -1;

    public float centerX;
    public float centerY;

    private Path path = new Path();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<String> area;
    private List<Float> areaFloats = new ArrayList<>();

    public float currentX;
    public float currentY;
    public float scaleFactor = 1.0f;

    public float areaHeight;
    public float areaWidth;


    //region Constructors
    public CustomPoiView(Context context, Bitmap iconBitmap, float x, float y, float radius, String title, boolean isFindTheBook) {
        this.context = context;

        this.iconBitmap = isFindTheBook ? createFindTheBookBitmap(iconBitmap) : iconBitmap;

        this.cx = x;
        this.cy = y;

        this.radius = isFindTheBook ? (int) dpToPx(20) : (int) dpToPx(15);
        this.title = title;

        generateInfoWindow();
    }

    public CustomPoiView(Context context, String area, int color, String title) {
        this.context = context;
        this.title = title;

        generateFloatValuesForArea(area);
        generatePathAndPaintForArea(color);

        cx = centerX;
        cy = centerY;

        generateInfoWindow();

        cx = -1;
        cy = -1;

    }
    //endregion


    //region Draw, Contains and Title

    /**
     * This method draws the updated CustomPoiView on the Canvas provided.
     *
     * @param canvas to draw on
     */
    public void draw(Canvas canvas) {
        if (iconBitmap != null) { //Draw icon
            canvas.drawBitmap(iconBitmap, cx - radius, cy - radius, null);

            if (iconBitmap != null && infoWindowText != null && infoWindowText.getVisibility() == View.VISIBLE) {
                int x = (int) ((cx) - infoWindowText.getWidth() / 2);
                int y = (int) ((cy) - infoWindowText.getHeight() - radius);
                int x2 = (int) ((cx) - infoBoxArrow.getWidth() / 2);
                int y2 = (int) ((cy) - infoBoxArrow.getHeight() / 2 - radius);
                drawInfoBox(canvas, x, y, x2, y2);
            }

        } else { //Draw area
            path.reset();
            path.moveTo(areaFloats.get(0) + cx, areaFloats.get(1) + cy);
            for (int i = 2; i < areaFloats.size(); i++) {
                path.lineTo(areaFloats.get(i) + cx, areaFloats.get(i + 1) + cy);
                i++; //Jump double for x/y values
            }
            path.close();

            canvas.drawPath(path, paint);

            if (infoWindowText != null && infoWindowText.getVisibility() == View.VISIBLE) {
                    int x = (int) ((centerX) - infoWindowText.getWidth()*scaleFactor / 2 / 2);
                    int y = (int) ((centerY) - infoWindowText.getHeight()*scaleFactor / 2 - areaHeight/2);
                    int x2 = (int) ((centerX) - infoBoxArrow.getWidth()*scaleFactor / 2 / 2);
                    int y2 = (int) ((centerY) - infoBoxArrow.getHeight()*scaleFactor / 2 / 2 - areaHeight/2);
                drawAreaInfoBox(canvas, x, y, x2, y2);
            }
        }
    }

    private void drawInfoBox(Canvas canvas, int x, int y, int x2, int y2) {
        Bitmap infoBoxBitmap = Bitmap.createScaledBitmap(infoBoxText, infoBoxText.getWidth(), infoBoxText.getHeight(), false);
        Bitmap infoBoxArrowBitmap = Bitmap.createScaledBitmap(infoBoxArrow, infoBoxArrow.getWidth(), infoBoxArrow.getHeight(), false);

        canvas.drawBitmap(infoBoxBitmap, x, y - infoBoxArrow.getHeight() / 3 + 2, null);
        canvas.drawBitmap(infoBoxArrowBitmap, x2, y2, null);
    }

    private void drawAreaInfoBox(Canvas canvas, int x, int y, int x2, int y2) {
        Bitmap infoBoxBitmap = Bitmap.createScaledBitmap(infoBoxText, (int)(infoBoxText.getWidth()*scaleFactor)/2, (int)(infoBoxText.getHeight()*scaleFactor)/2, false);
        Bitmap infoBoxArrowBitmap = Bitmap.createScaledBitmap(infoBoxArrow, (int)(infoBoxArrow.getWidth()*scaleFactor)/2, (int)(infoBoxArrow.getHeight()*scaleFactor)/2, false);

        canvas.drawBitmap(infoBoxBitmap, x, y - infoBoxArrow.getHeight()/2*scaleFactor/3 + 3*scaleFactor, null);
        canvas.drawBitmap(infoBoxArrowBitmap, x2, y2, null);
    }

    /**
     * This method calculates whether the given x,y value resides within this CustomPoiView.
     *
     * @param x the clicked x value
     * @param y the clicked y value
     * @return True if this CustomPoiView contains the Point, false otherwise
     */
    public boolean contains(float x, float y) {
        return Math.hypot(cx - x, cy - y) < radius;
    }

    public boolean areaContains(float x, float y, float radius) {
        // // TODO: Check Inside Rect instead of Radius
        return Math.hypot(centerX - x, centerY - y) < radius;
    }

    @Override
    public String toString() {
        return title;
    }
    //endregion


    //region Area
    private void generatePathAndPaintForArea(int color) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        paint.setColor(color);
        paint.setAlpha(75);

        path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
    }

    private void generateFloatValuesForArea(String area) {
        if (area != null) this.area = Arrays.asList(area.split(","));

        for (String element : this.area) {
            areaFloats.add(Float.valueOf(element)/2);
        }

        float lowX = 0.0f;
        float highX = 0.0f;
        float lowY = 0.0f;
        float highY = 0.0f;

        for (int i = 0; i < areaFloats.size(); i++) {
            float x = areaFloats.get(i);
            float y = areaFloats.get(i + 1);

            if (lowX == 0.0f && lowY == 0.0f) {
                // Default (set both low and high of first coordinate set)
                lowX = highX = x;
                lowY = highY = y;
            } else {
                lowX = Math.min(lowX, x);
                highX = Math.max(highX, x);
                lowY = Math.min(lowY, y);
                highY = Math.max(highY, y);
            }
            //Jump double for x/y values
            i++;
        }
        areaWidth = highX - lowX;
        areaHeight = highY - lowY;

        centerX = lowX + (areaWidth / 2);
        centerY = lowY + (areaHeight / 2);

        radius = Math.max(areaWidth/2, areaHeight/2);

    }
    //endregion


    //region POI
    private void generateInfoWindow() {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.layout_info_window, null);
        View viewArrow = inflater.inflate(R.layout.layout_info_arrow, null);

        infoWindowText = view.findViewById(R.id.info_window_text);
        infoWindowText.setText(title);
        if (BeaconBaconManager.getInstance().getConfigurationObject() != null) infoWindowText.setTypeface(BeaconBaconManager.getInstance().getConfigurationObject().getTypeface());

        ImageView infoArrow = viewArrow.findViewById(R.id.info_window_arrow);
        infoArrow.setVisibility(View.VISIBLE);

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        viewArrow.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        viewArrow.layout(0, 0, viewArrow.getMeasuredWidth(), viewArrow.getMeasuredHeight());
        viewArrow.setDrawingCacheEnabled(true);
        viewArrow.buildDrawingCache();

        infoBoxText = Bitmap.createBitmap(view.getDrawingCache());
        infoBoxArrow = Bitmap.createBitmap(viewArrow.getDrawingCache());

        view.setDrawingCacheEnabled(false);
        viewArrow.setDrawingCacheEnabled(false);

        infoWindowText.setVisibility(View.GONE);
    }

    private Bitmap createFindTheBookBitmap(Bitmap bitmap) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.layout_find_the_book_bitmap, null);

        ImageView icon = view.findViewById(R.id.find_the_book_icon);
        icon.setImageBitmap(bitmap);

        ImageView background = view.findViewById(R.id.find_the_book_background);

        int tintColor;
        try {
            tintColor = context.getResources().getColor(BeaconBaconManager.getInstance().getConfigurationObject().getTintColor());
        } catch (Exception e) {
            tintColor =  Color.DKGRAY;
        }

        background.setColorFilter(tintColor);

        this.radius = dpToPx(15);

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        Bitmap b = null;
        if (view.getDrawingCache() != null) b = Bitmap.createScaledBitmap(view.getDrawingCache(), view.getWidth(), view.getHeight(), false);

        view.setDrawingCacheEnabled(false);

        return b;
    }
    //endregion
}
