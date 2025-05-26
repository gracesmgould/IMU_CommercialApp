package com.example.uibasics;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity implements RecordFragment.OnRecordControlListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new RecordFragment(), "Record");
        adapter.addFragment(new PlotFragment(), "Plot");

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(adapter.getTitle(position))
        ).attach();
    }

    @Override
    public void onStartRecording() {
        ViewPagerAdapter adapter = (ViewPagerAdapter) ((ViewPager2) findViewById(R.id.viewPager)).getAdapter();
        if (adapter != null) {
            PlotFragment plotFragment = (PlotFragment) adapter.getFragment(1); // Second fragment
            if (plotFragment != null) {
                plotFragment.startSimulatingData();
            }
        }
    }

    @Override
    public void onStopRecording() {
        ViewPagerAdapter adapter = (ViewPagerAdapter) ((ViewPager2) findViewById(R.id.viewPager)).getAdapter();
        if (adapter != null) {
            PlotFragment plotFragment = (PlotFragment) adapter.getFragment(1);
            if (plotFragment != null) {
                plotFragment.stopSimulatingData();
            }
        }
    }
}


