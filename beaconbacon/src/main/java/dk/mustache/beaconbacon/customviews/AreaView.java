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
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import dk.mustache.beaconbacon.R;

public class AreaView extends FrameLayout {

    public AreaView(Context context) {
        super(context);
    }

    public AreaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AreaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCircleColor(int color) {
        View view = inflate(getContext(), R.layout.layout_area_icon, null);
        ImageView circle = view.findViewById(R.id.area_circle);

        setColorToCirlce(circle, color);

        addView(view);
    }

    private void setColorToCirlce(ImageView circle, int color) {
        GradientDrawable circleImageDrawable = new GradientDrawable();
        circleImageDrawable.setShape(GradientDrawable.OVAL);
        circleImageDrawable.setColor(color);
        circleImageDrawable.setAlpha(75);

        circle.setImageDrawable(circleImageDrawable);
    }
}
