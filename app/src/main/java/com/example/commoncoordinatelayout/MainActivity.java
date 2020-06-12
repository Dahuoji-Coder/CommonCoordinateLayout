package com.example.commoncoordinatelayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CommonCoordinateLayout coordinateLayout = findViewById(R.id.coordinateLayout);
        final LinearLayout coordinateHelpLayout = findViewById(R.id.coordinateHelpLayout);
        final LinearLayout buttonsLayout = findViewById(R.id.buttonsLayout);
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Adapter adapter = new Adapter(this);
        recyclerView.setAdapter(adapter);

        coordinateLayout.setHelpLayout(coordinateHelpLayout);
        coordinateLayout.setRecyclerView(recyclerView);
        coordinateLayout.setStatusBarHeight(0);

        buttonsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int coordinateLayoutHeight = coordinateLayout.getMeasuredHeight();
                        int recyclerViewHeight = coordinateLayoutHeight - buttonsLayout.getMeasuredHeight();

                        //设置上滑最大高度
                        coordinateLayout.setMaxScroll(coordinateHelpLayout.getMeasuredHeight() - buttonsLayout.getMeasuredHeight());
                        //设置上滑最小高度
                        coordinateLayout.setMinScroll(0);
                        coordinateLayout.scrollTo(0, 0);
                        //设置RecyclerView高度
                        ViewGroup.LayoutParams layoutParams = recyclerView.getLayoutParams();
                        layoutParams.height = recyclerViewHeight;
                        recyclerView.setLayoutParams(layoutParams);

                        buttonsLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

    }
}
