package com.hoaithanh.expense_tracker.ui.add;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.hoaithanh.expense_tracker.R;
import com.hoaithanh.expense_tracker.data.local.database.AppDatabase;
import com.hoaithanh.expense_tracker.data.local.entity.Category;
import com.hoaithanh.expense_tracker.data.local.entity.Expense;
import com.hoaithanh.expense_tracker.utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {
    private String currentPhotoPath;
    private ImageView ivPreview;
    private EditText etAmount, etTitle;
    private ChipGroup chipGroup;
    private AppDatabase database;
    private MaterialButtonToggleGroup toggleTransactionType; // KHAI BÁO BIẾN MỚI
    private int currentType = 0;

    // Danh sách danh mục cho Chi tiêu
    private String[] expenseCategories;
    private String[] incomeCategories;
    private List<String> expenseList = new ArrayList<>();
    private List<String> incomeList = new ArrayList<>();

    private String selectedCategory = "";

    private boolean isEditMode = false;
    private int editExpenseId = -1;
    private Expense originalExpense; // Lưu lại để lấy imagePath cũ nếu cần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        database = AppDatabase.getInstance(this);
        ivPreview = findViewById(R.id.ivPreview);
        etAmount = findViewById(R.id.etAmount);
        toggleTransactionType = findViewById(R.id.toggleTransactionType);
        etTitle = findViewById(R.id.etTitle);
        chipGroup = findViewById(R.id.chipGroupCategory);
        MaterialButton btnBack = findViewById(R.id.btnBack);
        loadCategoriesFromDb();
        btnBack.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            // Kiểm tra xem người dùng đã nhập dữ liệu gì chưa (Tư duy UX)
            if (hasUnsavedData()) {
                showExitConfirmation();
            } else {
                finish(); // Đóng Activity và quay lại màn hình trước
            }
        });

        // Kiểm tra xem là Sửa hay Thêm
        isEditMode = getIntent().getBooleanExtra("IS_EDIT", false);
        editExpenseId = getIntent().getIntExtra("EXPENSE_ID", -1);

        if (isEditMode) {
            setupEditUI();
            loadExistingData();
        }

        // 2. Lắng nghe đổi Thu/Chi
        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTypeIncome) {
                    currentType = 1;
                    // DÙNG LIST THAY VÌ ARRAY TĨNH
                    populateChips(incomeList.toArray(new String[0]), 1);
                    updateUIForIncome();
                } else {
                    currentType = 0;
                    // DÙNG LIST THAY VÌ ARRAY TĨNH
                    populateChips(expenseList.toArray(new String[0]), 0);
                    updateUIForExpense();
                }
            }
        });


        // Nhận tín hiệu từ Home: Có ép mở Camera không?
//        boolean autoCamera = getIntent().getBooleanExtra("AUTO_CAMERA", true);
//
//        if (autoCamera && savedInstanceState == null && currentPhotoPath == null) {
//            checkCameraPermission(); // Hàm này bên trong sẽ gọi dispatchTakePictureIntent()
//        } else {
//            // Nếu là nhập tay, hiện ảnh placeholder để UI không bị trống
//            ivPreview.setImageResource(R.drawable.bg_placeholder_expense2);
//        }

        // 1. Nhận tín hiệu từ Home (Mặc định là false để an toàn hơn khi Edit)
        boolean autoCamera = getIntent().getBooleanExtra("AUTO_CAMERA", false);

        // 2. LOGIC THÔNG MINH:
        // CHỈ mở Camera nếu: KHÔNG phải chế độ sửa VÀ Home yêu cầu mở Camera VÀ chưa có ảnh nào được nạp
        if (!isEditMode && autoCamera && savedInstanceState == null && currentPhotoPath == null) {
            checkCameraPermission();
        } else {
            // Nếu là chế độ NHẬP TAY mới (không phải Edit) thì mới hiện Placeholder
            if (!isEditMode && (currentPhotoPath == null)) {
                ivPreview.setImageResource(R.drawable.bg_placeholder_expense2);
            }
            // LƯU Ý: Nếu là isEditMode, ta để trống chỗ này vì ảnh sẽ được nạp
            // từ Glide bên trong hàm loadExistingData() mà bạn đã viết.
        }

        // Nút chọn từ Gallery (Thêm vào XML ở bước sau)
        findViewById(R.id.btnGallery).setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
        // Nút bỏ qua ảnh
        findViewById(R.id.btnSkipPhoto).setOnClickListener(v -> {
            currentPhotoPath = "default_placeholder"; // Đánh dấu dùng ảnh mặc định
            ivPreview.setImageResource(R.drawable.bg_placeholder_expense2); // Một ảnh gradient đẹp
            Toast.makeText(this, "Đã bỏ qua chụp ảnh", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnSave).setOnClickListener(v -> saveExpense());

        // 2. Format money
        etAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private final Locale vietnam = new Locale("vi", "VN");

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    etAmount.removeTextChangedListener(this);

                    // 1. Làm sạch chuỗi (chỉ giữ lại số)
                    String cleanString = s.toString().replaceAll("[^0-9]", "");

                    if (cleanString.isEmpty() || cleanString.equals("0")) {
                        current = "";
                        etAmount.setText("");
                    } else {
                        try {
                            double parsed = Double.parseDouble(cleanString);

                            // 2. Chỉ định dạng con số (Ví dụ: 1.000.000)
                            DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(vietnam);
                            decimalFormat.applyPattern("#,###");
                            String formattedNumber = decimalFormat.format(parsed);

                            // 3. Tự thêm hậu tố có khoảng trắng để dễ nhìn
                            current = formattedNumber + " ₫";
                            etAmount.setText(current);

                            // 4. QUAN TRỌNG: Đưa con trỏ về trước chữ " ₫" (lùi lại 2 ký tự)
                            int cursorPosition = current.length() - 2;
                            etAmount.setSelection(Math.max(0, cursorPosition));

                        } catch (NumberFormatException e) {
                            etAmount.setText("");
                        }
                    }

                    etAmount.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadExistingData() {
        // Truy vấn dữ liệu từ DB dựa trên editExpenseId đã lấy ở onCreate
        database.expenseDao().getExpenseById(editExpenseId).observe(this, expense -> {
            // Chỉ thực hiện nạp dữ liệu một lần duy nhất khi vào chế độ sửa
            if (expense != null && isEditMode && originalExpense == null) {
                originalExpense = expense; // Lưu lại đối tượng gốc

                // 1. Đổ số tiền (Tự động trigger TextWatcher để format tiền)
                etAmount.setText(String.valueOf((long) expense.amount));

                // 2. Đổ tiêu đề
                etTitle.setText(expense.title);

                // 3. Thiết lập loại Thu (1) hoặc Chi (0)
                currentType = expense.type;
                if (currentType == 1) {
                    toggleTransactionType.check(R.id.btnTypeIncome);
                } else {
                    toggleTransactionType.check(R.id.btnTypeExpense);
                }

                // 4. Xử lý ảnh cũ
                currentPhotoPath = expense.imagePath;
                if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                    if (currentPhotoPath.equals("default_placeholder")) {
                        ivPreview.setImageResource(R.drawable.bg_placeholder_expense2);
                    } else {
                        Glide.with(this)
                                .load(currentPhotoPath)
                                .centerCrop()
                                .into(ivPreview);
                    }
                }

                // 5. CHỌN ĐÚNG CHIP DANH MỤC
                // Vì danh sách Chip nạp từ DB có thể chậm hơn, ta dùng post để đợi UI vẽ xong
                chipGroup.post(() -> {
                    for (int i = 0; i < chipGroup.getChildCount(); i++) {
                        View view = chipGroup.getChildAt(i);
                        if (view instanceof Chip) {
                            Chip chip = (Chip) view;
                            if (chip.getText().toString().equals(expense.category)) {
                                chip.setChecked(true);
                                selectedCategory = expense.category;
                                // Cuộn đến vị trí Chip đó (Nếu ChipGroup nằm trong HorizontalScrollView)
                                chip.getParent().requestChildFocus(chip, chip);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    private void setupEditUI() {
        // 1. Đổi tiêu đề Toolbar (Nếu bạn dùng ActionBar mặc định)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chỉnh sửa ghi chú");
        }

        // 2. Đổi chữ trên nút Lưu thành "Cập nhật"
        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setText("Cập nhật thay đổi");
        btnSave.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check)); // Nếu bạn có icon check

        // 3. Đổi icon hoặc text cho nút Back nếu cần (Tùy chọn)
        MaterialButton btnBack = findViewById(R.id.btnBack);
        btnBack.setText("Hủy");
    }

    private void showDeleteCategoryDialog(String catName, int type) {
        // Tạo hiệu ứng rung nhẹ khi hiện Dialog (UX tốt hơn)
        chipGroup.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục?")
                .setMessage("Bạn có chắc muốn xóa '" + catName + "' không? Các chi tiêu cũ đã lưu danh mục này sẽ không bị ảnh hưởng.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // 1. Xóa khỏi List trong RAM để cập nhật UI nhanh
                    if (type == 1) {
                        incomeList.remove(catName);
                    } else {
                        expenseList.remove(catName);
                    }

                    // 2. Xóa khỏi Database vật lý
                    new Thread(() -> {
                        database.categoryDao().deleteCategoryByName(catName, type);

                        // 3. Vẽ lại bộ Chip ngay lập tức trên UI Thread
                        runOnUiThread(() -> {
                            if (type == 1) {
                                populateChips(incomeList.toArray(new String[0]), 1);
                            } else {
                                populateChips(expenseList.toArray(new String[0]), 0);
                            }
                            Toast.makeText(this, "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadCategoriesFromDb() {
        new Thread(() -> {
            expenseList.clear();
            incomeList.clear();
            // 1. Lấy danh mục Chi tiêu từ DB
            List<Category> dbExpenses = database.categoryDao().getCategoriesByType(0);
            if (dbExpenses.isEmpty()) {
                // Nếu DB trống, nạp mặc định từ strings.xml vào DB và List
                String[] defaults = getResources().getStringArray(R.array.expense_categories);
                for (String s : defaults) {
                    database.categoryDao().insert(new Category(s, 0));
                    expenseList.add(s);
                }
            } else {
                for (Category c : dbExpenses) expenseList.add(c.name);
            }

            // 2. Lấy danh mục Thu nhập từ DB
            List<Category> dbIncomes = database.categoryDao().getCategoriesByType(1);
            if (dbIncomes.isEmpty()) {
                String[] defaults = getResources().getStringArray(R.array.income_categories);
                for (String s : defaults) {
                    database.categoryDao().insert(new Category(s, 1));
                    incomeList.add(s);
                }
            } else {
                for (Category c : dbIncomes) incomeList.add(c.name);
            }

            // 3. Sau khi nạp xong vào List, hiển thị bộ Chip mặc định (Chi tiêu)
            runOnUiThread(() -> populateChips(expenseList.toArray(new String[0]), 0));
        }).start();
    }

    private void populateChips(String[] categories, int type) {
        chipGroup.removeAllViews(); // Xóa sạch để không bị lẫn Thu/Chi
        HashSet<String> seenNames = new HashSet<>();
        for (String cat : categories) {
            if (seenNames.contains(cat)) continue; // Nếu tên này đã vẽ rồi thì bỏ qua
            seenNames.add(cat);

            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Thiết lập màu sắc dựa trên Type
            if (type == 1) { // Thu nhập
                chip.setChipBackgroundColorResource(R.color.income_chip_bg);
                chip.setTextColor(getResources().getColor(R.color.income_text));
            } else { // Chi tiêu
                chip.setChipBackgroundColorResource(R.color.expense_chip_bg);
                chip.setTextColor(getResources().getColor(R.color.expense_text));
            }

            chip.setOnLongClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

                if (isDefaultCategory(cat)) {
                    Toast.makeText(this, "Danh mục mặc định không thể sửa/xóa", Toast.LENGTH_SHORT).show();
                    return true;
                }

                String[] options = {"Sửa tên", "Xóa danh mục"};
                new AlertDialog.Builder(this)
                        .setTitle(cat)
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                showEditCategoryDialog(cat, type);
                            } else {
                                showDeleteCategoryDialog(cat, type);
                            }
                        })
                        .show();
                return true;
            });

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategory = cat;
                }
            });
            chipGroup.addView(chip);
        }

        // Tự động chọn Chip đầu tiên để tránh lỗi chưa chọn danh mục
        if (chipGroup.getChildCount() > 0) {
            ((Chip) chipGroup.getChildAt(0)).setChecked(true);
            selectedCategory = categories[0];
        }

        createAddButtonChip(type);
    }

    private void showEditCategoryDialog(String oldName, int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sửa danh mục");

        final EditText input = new EditText(this);
        input.setText(oldName); // Hiện tên cũ để người dùng sửa
        builder.setView(input);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(oldName)) {
                // 1. Cập nhật trong List (RAM)
                if (type == 1) {
                    int index = incomeList.indexOf(oldName);
                    if (index != -1) incomeList.set(index, newName);
                } else {
                    int index = expenseList.indexOf(oldName);
                    if (index != -1) expenseList.set(index, newName);
                }

                // 2. Cập nhật trong Database
                new Thread(() -> {
                    database.categoryDao().updateCategory(oldName, newName, type);
                    int rowsAffected = database.expenseDao().updateExpenseCategory(oldName, newName);
                    // 3. Vẽ lại giao diện
                    runOnUiThread(() -> {
                        if (type == 1) populateChips(incomeList.toArray(new String[0]), 1);
                        else populateChips(expenseList.toArray(new String[0]), 0);
                    });
                }).start();
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private boolean isDefaultCategory(String catName) {
        // 1. Lấy lại mảng gốc từ strings.xml
        String[] defaultExpenses = getResources().getStringArray(R.array.expense_categories);
        String[] defaultIncomes = getResources().getStringArray(R.array.income_categories);

        // 2. Kiểm tra xem catName có nằm trong mảng Chi tiêu mặc định không
        for (String s : defaultExpenses) {
            if (s.equals(catName)) return true;
        }

        // 3. Kiểm tra xem catName có nằm trong mảng Thu nhập mặc định không
        for (String s : defaultIncomes) {
            if (s.equals(catName)) return true;
        }

        return false; // Nếu không nằm trong cả 2 thì đây là danh mục do người dùng tự thêm
    }

    private void addPlusButton(int type) {
        Chip addChip = new Chip(this);
        addChip.setText("+");
        addChip.setChipBackgroundColorResource(R.color.secondary_container);
        addChip.setOnClickListener(v -> {
            // Gọi Dialog thêm danh mục ở đây nếu cần
            Toast.makeText(this, "Tính năng thêm danh mục đang phát triển", Toast.LENGTH_SHORT).show();
        });
        chipGroup.addView(addChip);
    }

    // --- CÁC HÀM TINH CHỈNH UI (UX Bonus) ---
    private void updateUIForIncome() {
        // Ví dụ: Đổi màu số tiền thành xanh lá cho tiền vào
        etAmount.setTextColor(Color.GREEN);
        // Sau này chúng ta sẽ đổi danh sách Chip tại đây
    }

    private void updateUIForExpense() {
        // Đổi màu số tiền thành đỏ cho tiền ra
        etAmount.setTextColor(Color.RED);
        // Sau này chúng ta sẽ đổi danh sách Chip tại đây
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            // Tạo tên file duy nhất
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return file.getAbsolutePath(); // Trả về đường dẫn vật lý: /storage/emulated/0/...
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Kiểm tra xem có dữ liệu chưa lưu không
    private boolean hasUnsavedData() {
        String amount = etAmount.getText().toString().trim();
        // Nếu đã nhập tiền HOẶC đã có ảnh path thì coi là có dữ liệu
        return !amount.isEmpty() || currentPhotoPath != null;
    }

    // Hiển thị thông báo xác nhận thoát
    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy bỏ thay đổi?")
                .setMessage("Dữ liệu bạn vừa nhập sẽ không được lưu. Bạn có chắc muốn quay lại?")
                .setPositiveButton("Thoát", (dialog, which) -> finish())
                .setNegativeButton("Ở lại", null)
                .show();
    }


    private void createChip(String text, boolean isSelected) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);

        // Style Material 3: Bo tròn và màu sắc thân thiện
        chip.setChipCornerRadius(20f);
        chip.setChecked(isSelected);

        // Thêm vào ChipGroup
        chipGroup.addView(chip);
    }

    private void createAddButtonChip(int type) {
        Chip addChip = new Chip(this);
        addChip.setText("Thêm");
        addChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_add));
        addChip.setChipBackgroundColorResource(R.color.secondary_container);

        addChip.setOnClickListener(v -> {
            // Truyền currentType vào đây
            showAddCategoryDialog(type);
        });

        chipGroup.addView(addChip);
    }

    private void showAddCategoryDialog(int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm danh mục mới");
        final EditText input = new EditText(this);
        input.setHint("Ví dụ: 🐱 Thú cưng");
        builder.setView(input);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String newCatName = input.getText().toString().trim();
            if (!newCatName.isEmpty()) {
                if (type == 1) incomeList.add(newCatName);
                else expenseList.add(newCatName);

                new Thread(() -> {
                    // LƯU KÈM TYPE VÀO DATABASE
                    database.categoryDao().insert(new Category(newCatName, type));
                }).start();

                // Vẽ lại Chip
                if (type == 1) populateChips(incomeList.toArray(new String[0]), 1);
                else populateChips(expenseList.toArray(new String[0]), 0);
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Hàm hỗ trợ chèn chip vào vị trí cụ thể
    private void createChipAt(String text, int index) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(true); // Tự động chọn luôn danh mục vừa tạo
        chipGroup.addView(chip, index);
    }

    // 1. Thêm Launcher để chọn ảnh từ Gallery
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Lưu Uri vào biến đường dẫn
//                    currentPhotoPath = uri.toString();

                    String internalPath = saveImageToInternalStorage(uri);
                    if (internalPath != null) {
                        // 2. Cập nhật biến đường dẫn để tí nữa lưu Database
                        currentPhotoPath = internalPath;

                        // 3. Hiển thị ảnh lên giao diện cho người dùng xem
                        Glide.with(this)
                                .load(internalPath)
                                .centerCrop()
                                .into(ivPreview);
                        Toast.makeText(this, "Đã chọn ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Nếu chưa có quyền, hệ thống sẽ hiện hộp thoại xin quyền
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            // Nếu đã có quyền rồi, mở camera luôn
            dispatchTakePictureIntent();
        }
    }

    // Hàm này sẽ chạy khi người dùng bấm "Cho phép" hoặc "Từ chối" trên hộp thoại
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Người dùng đã cho phép!
                dispatchTakePictureIntent();
            } else {
                // Người dùng từ chối
                Toast.makeText(this, "Bạn cần cấp quyền Camera để chụp ảnh chi tiêu!", Toast.LENGTH_LONG).show();
                finish(); // Thoát màn hình vì không có ảnh thì không làm gì được
            }
        }
    }

    // Đăng ký nhận kết quả từ Camera
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    // CHỤP THÀNH CÔNG: Hiển thị ảnh lên ivPreview
                    Glide.with(this).load(currentPhotoPath).into(ivPreview);

                    // Senior Tip: Ép hệ thống quét file ảnh mới để tránh bị mất ảnh trong Gallery máy
                    MediaScannerConnection.scanFile(this, new String[]{currentPhotoPath}, null, null);
                } else {
                    // CHỤP THẤT BẠI HOẶC BẤM BACK: Thoát luôn để không bị lặp
                    Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = ImageUtils.createImageFile(this);
            currentPhotoPath = photoFile.getAbsolutePath();
            Uri photoURI = ImageUtils.getUriForFile(this, photoFile);
            takePictureLauncher.launch(photoURI);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) {
            etAmount.setError("Vui lòng nhập số tiền!");
            return;
        }

        // Xóa tất cả ký tự không phải là số (Regex: [^0-9])
        String cleanAmount = amountStr.replaceAll("[^0-9]", "");
        double amount = Double.parseDouble(cleanAmount);
        String title = etTitle.getText().toString();
        int transactionType = currentType;

        // Cách lấy text của Chip đang chọn cực gọn:
        int checkedChipId = chipGroup.getCheckedChipId();
        String category = "Khác"; // Giá trị mặc định

        if (checkedChipId != View.NO_ID) {
            Chip selectedChip = findViewById(checkedChipId);
            category = selectedChip.getText().toString();
        }else {
            // Nếu bạn muốn ép người dùng phải chọn danh mục
            Toast.makeText(this, "Hãy chọn một danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            // 1. Kiểm tra chế độ để quyết định hành động Database
            if (isEditMode && originalExpense != null) {
                // --- CHẾ ĐỘ CẬP NHẬT ---

                // BƯỚC 1: XÓA ẢNH CŨ RÁC (Nếu có)
                // Điều kiện 1: Đã chụp ảnh mới (currentPhotoPath khác ảnh cũ)
                if (!currentPhotoPath.equals(originalExpense.imagePath)) {

                    // Điều kiện 2: Ảnh cũ không phải là ảnh mặc định (placeholder)
                    // Cần so sánh chính xác để tránh xóa file placeholder hệ thống
                    if (originalExpense.imagePath != null && !originalExpense.imagePath.equals("default_placeholder")) {

                        // Tạo đối tượng File từ đường dẫn ảnh cũ
                        File oldFile = new File(originalExpense.imagePath);

                        // Kiểm tra xem file có thực sự tồn tại trên máy không
                        if (oldFile.exists()) {
                            oldFile.delete(); // Tiến hành xóa file vật lý
                            // Logcat để bạn debug (tùy chọn)
                            // Log.d("AddExpense", "Đã xóa ảnh cũ rác: " + originalExpense.imagePath);
                        }
                    }
                }

                // BƯỚC 2: GÁN GIÁ TRỊ MỚI VÀO ĐỐI TƯỢNG CŨ
                originalExpense.title = title;
                originalExpense.amount = amount;
                originalExpense.category = selectedCategory;
                originalExpense.imagePath = currentPhotoPath; // Đây là đường dẫn ảnh MỚI
                originalExpense.type = currentType;
                // Gán thời gian sửa (System.currentTimeMillis()) vào timestamp nếu bạn muốn
                // originalExpense.timestamp = System.currentTimeMillis();

                // BƯỚC 3: GỌI LỆNH UPDATE TRONG DAO
                database.expenseDao().update(originalExpense);

            } else {
                // --- CHẾ ĐỘ THÊM MỚI (Code cũ giữ nguyên) ---
                Expense newRecord = new Expense(
                        title,
                        amount,
                        System.currentTimeMillis(),
                        selectedCategory,
                        currentPhotoPath,
                        currentType
                );
                database.expenseDao().insert(newRecord);
            }

            // 2. Cập nhật giao diện trên UI Thread
            runOnUiThread(() -> {
                String message = isEditMode ? "Đã cập nhật thay đổi!" : "Đã lưu ghi chú mới!";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                finish();
                // Hiệu ứng đóng màn hình mượt mà
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
            });
        }).start();

    }
}
