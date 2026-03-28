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
import com.hoaithanh.expense_tracker.model.ListItem;
import com.hoaithanh.expense_tracker.ui.detail.DetailActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ListItem> items = new ArrayList<>();

    public void setExpenses(List<ListItem> list) {
        this.items = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_DATE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_header, parent, false);
            return new DateViewHolder(view);
        } else {
            // Giữ nguyên layout item_expense_recent của bạn
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_recent, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);

        if (holder instanceof DateViewHolder) {
            DateViewHolder dateHolder = (DateViewHolder) holder;

            // Hiển thị ngày
            dateHolder.tvDate.setText(item.getDateHeader());

            // Định dạng và hiển thị tổng tiền
            NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            dateHolder.tvTotal.setText(vnFormat.format(item.getTotalAmount()));
        } else if (holder instanceof ExpenseViewHolder) {
            Expense expense = item.getExpense();
            ExpenseViewHolder expHolder = (ExpenseViewHolder) holder;

            expHolder.tvTitle.setText(expense.title.isEmpty() ? "No Title" : expense.title);
            expHolder.chipCategory.setText(expense.category);

            NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            expHolder.tvAmount.setText(vnFormat.format(expense.amount));

            Glide.with(expHolder.itemView.getContext())
                    .load(expense.imagePath)
                    .centerCrop()
                    .placeholder(R.drawable.ic_camera)
                    .error(R.drawable.bg_placeholder_expense)
                    .into(expHolder.ivThumbnail);

            expHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), DetailActivity.class);
                intent.putExtra("EXPENSE_ID", expense.id);
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // --- 2 ViewHolders riêng biệt ---
    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTotal;

        DateViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
            tvTotal = itemView.findViewById(R.id.tvDayTotal); // Ánh xạ TextView tổng tiền
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle, tvAmount;
        Chip chipCategory;
        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            chipCategory = itemView.findViewById(R.id.chipCategory);
        }
    }
}
