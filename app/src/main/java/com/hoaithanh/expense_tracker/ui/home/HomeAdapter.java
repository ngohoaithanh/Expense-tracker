package com.hoaithanh.expense_tracker.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
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

            dateHolder.tvDate.setText(item.getDateHeader());

            NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            long totalAmount = item.getTotalAmount();

            dateHolder.tvTotal.setText(vnFormat.format(totalAmount));

            // Đổi màu: Nếu tổng thu > chi (số dương) -> Màu xanh, ngược lại -> Màu đỏ
            if (totalAmount > 0) {
                dateHolder.tvTotal.setTextColor(Color.parseColor("#4CAF50")); // Xanh
            } else if (totalAmount < 0) {
                dateHolder.tvTotal.setTextColor(Color.parseColor("#F44336")); // Đỏ
            } else {
                dateHolder.tvTotal.setTextColor(Color.GRAY); // Bằng 0 thì màu xám
            }
        } else if (holder instanceof ExpenseViewHolder) {
            Expense expense = item.getExpense();
            ExpenseViewHolder expHolder = (ExpenseViewHolder) holder;

            expHolder.tvTitle.setText(expense.title.isEmpty() ? "No Title" : expense.title);
            expHolder.chipCategory.setText(expense.category);

            NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            if (expense.type == 1) { // Thu nhập (Income)
                expHolder.tvAmount.setText("+ " + vnFormat.format(expense.amount));
                expHolder.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Màu Xanh
            } else { // Chi tiêu (Expense)
                expHolder.tvAmount.setText("- " + vnFormat.format(expense.amount));
                expHolder.tvAmount.setTextColor(Color.parseColor("#F44336")); // Màu Đỏ
            }

            // 1. Xác định ảnh mặc định dựa trên Loại (Type)
//            int defaultImageId = (expense.type == 1) ? R.drawable.income : R.drawable.bg_placeholder_expense;

            int defaultImageId = (expense.type == 1) ?
                    R.drawable.item_income_visual :  // Nền xanh + Túi tiền PNG
                    R.drawable.item_expense_visual;  // Nền xám + Camera
            if (expense.imagePath == null || expense.imagePath.isEmpty()) {
                // NẾU KHÔNG CÓ ẢNH: Load thẳng ảnh mặc định, không cần placeholder/error phức tạp
                Glide.with(expHolder.itemView.getContext())
                        .load(defaultImageId) // Load trực tiếp Resource ID
                        .centerCrop()
                        .into(expHolder.ivThumbnail);
            } else {
                // NẾU CÓ ẢNH: Load từ bộ nhớ máy
                Glide.with(expHolder.itemView.getContext())
                        .load(expense.imagePath)
                        .centerCrop()
                        .placeholder(defaultImageId)
                        .error(defaultImageId)
                        .into(expHolder.ivThumbnail);
            }

            expHolder.itemView.setOnClickListener(v -> {
                if (position != RecyclerView.NO_POSITION) {
                    // Giả sử bạn muốn log ra xem đã bấm trúng chưa
                    Log.d("CLICK_ITEM", "Bạn vừa bấm vào mục vị trí: " + position);

                    // Sau này bạn có thể mở màn hình "Chi tiết chi tiêu" ở đây
                }
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                Intent intent = new Intent(v.getContext(), DetailActivity.class);
                intent.putExtra("EXPENSE_ID", expense.id);
                v.getContext().startActivity(intent);

                if (v.getContext() instanceof Activity) {
                    Activity activity = (Activity) v.getContext();

                    // Dùng hiệu ứng Mờ dần (Fade) - Đảm bảo KHÔNG BÁO ĐỎ
                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                    // HOẶC nếu muốn hiệu ứng trượt sang ngang (Slide) kiểu iPhone:
                    // activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
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
