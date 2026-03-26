package com.hoaithanh.expense_tracker.ui.gallery;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.ui.detail.DetailActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private List<Expense> expenseList = new ArrayList<>();

    public void setExpenses(List<Expense> list) {
        this.expenseList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        // Hiển thị số tiền đè lên ảnh
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvAmount.setText(vnFormat.format(expense.amount));

        // Senior Tip: Với Staggered Grid, Glide cần xử lý fitCenter để không mất layout
        Glide.with(holder.itemView.getContext())
                .load(expense.imagePath)
                .placeholder(R.drawable.ic_camera)
                .into(holder.ivPhoto);

        // Xử lý sự kiện click để xem chi tiết (Bước sau)
        holder.itemView.setOnClickListener(v -> {
            // Intent sang DetailActivity
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            // Gửi ID đi với khóa là "EXPENSE_ID"
            intent.putExtra("EXPENSE_ID", expense.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return expenseList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivGridPhoto);
            tvAmount = itemView.findViewById(R.id.tvGridAmount);
        }
    }
}
