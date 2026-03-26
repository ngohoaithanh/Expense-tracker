package com.hoaithanh.expense_tracker.ui.home;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.ui.detail.DetailActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {
    private List<Expense> expenseList = new ArrayList<>();

    public void setExpenses(List<Expense> list) {
        this.expenseList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_recent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvTitle.setText(expense.title);
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvAmount.setText(vnFormat.format(expense.amount));
        holder.chipCategory.setText(expense.category);

        // Senior Tip: Sử dụng Glide để load ảnh mượt mà, tránh OutOfMemory
        Glide.with(holder.itemView.getContext())
                .load(expense.imagePath)
                .centerCrop()
                .placeholder(R.drawable.ic_camera) // Hiện tạm icon camera trong lúc load
                .into(holder.ivThumbnail);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            // Gửi ID đi với khóa là "EXPENSE_ID"
            intent.putExtra("EXPENSE_ID", expense.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle, tvAmount;
        Chip chipCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            chipCategory = itemView.findViewById(R.id.chipCategory);
        }
    }
}
