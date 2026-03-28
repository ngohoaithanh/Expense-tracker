package com.hoaithanh.expense_tracker.ui.gallery;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;

public class GalleryActivity extends AppCompatActivity {
    private RecyclerView rvGallery;
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarGallery);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvGallery = findViewById(R.id.rvGallery);
        adapter = new GalleryAdapter();

        // PHẦN QUAN TRỌNG: Thiết lập Masonry Layout (2 cột)
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        // Ngăn chặn các item bị nhảy khi scroll
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);

        rvGallery.setLayoutManager(layoutManager);
        rvGallery.setAdapter(adapter);

        // Lấy dữ liệu từ Room
        AppDatabase.getInstance(this).expenseDao().getAllExpenses().observe(this, expenses -> {
            if (expenses != null) adapter.setExpenses(expenses);
        });
    }
}
