# 💰 Expense Manager - Personal Finance Tracker (Android)

A modern, high-performance Android mobile application built with **Material 3** to help users track their daily income and expenses with smart analytics, intuitive navigation, and secure local storage.

---

## ✨ Key Features

### 🏠 Dynamic Dashboard (Home)
- **Financial Overview:** View real-time Total Balance, Income, and Expenses at a glance.
- **Smart Budget Progress Bar:** Automatically calculates spending percentage relative to monthly income.
    - 🟢 **Green:** Safe (Spending < 50% of Income).
    - 🟡 **Yellow:** Warning (50% - 80% of Income).
    - 🔴 **Red:** Critical (> 80% of Income).
- **Time-Travel Filter:** A custom Month-Year Picker to navigate through historical data effortlessly.
- **Context-Aware UI:** The app intelligently hides "Today" labels and the Budget Progress bar when viewing past months to focus on historical records.

### ➕ Transaction Management
- **CRUD Operations:** Seamlessly Add, Edit, or Delete financial records.
- **Categorization:** Clearly separate Income and Expenses for better tracking.
- **Image Attachments:** Capture receipts or select invoices from the gallery.

### 🎨 Premium UI/UX
- **Material 3 Integration:** Utilizing Google’s latest design language for a clean, professional look.
- **Smooth Animations:** Integrated Slide and Fade transitions for a fluid user experience.
- **Shimmer Loading:** Visual feedback during data fetching to ensure the app feels responsive.

---

## 🛠 Tech Stack

The app is built as a **Native Android** project using **Java**, leveraging modern architectural components:

| Component | Technology |
| :--- | :--- |
| **Database** | [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite) |
| **Architecture** | MVVM (Model-View-ViewModel) + LiveData |
| **UI Components** | Google Material Design 3 |
| **Image Handling** | Bitmap Compression & FileProvider API |
| **Concurrency** | Multithreading with Background Workers |


## 🏗 Installation & Setup

### System Requirements
- Android Studio Ladybug (or newer).
- JDK 17+.
- Android SDK 24 (Nougat) or higher.

### Quick Start
1. Clone the repository:
   ```bash
   git clone (https://github.com/ngohoaithanh/Expense-tracker.git)

2. Open the project in Android Studio.

3. Ensure the build.gradle file includes the necessary Material 3 dependencies:
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.room:room-runtime:2.6.1'

4. Build and Run on a physical device or emulator.

### 📌 Project Structure

Based on the actual project architecture:

```text
com.hoaithanh.expense_tracker
├── data/
│   └── local/
│       ├── dao/            # Data Access Objects (Room Queries)
│       ├── database/       # Room Database Configuration
│       ├── entity/         # Database Entities (Expense Tables)
│       └── repository/     # Data Source Orchestration
├── model/                  # POJO classes and Data Models
├── ui/                     # Feature-based UI Components
│   ├── add/                # Add Expense screen logic
│   ├── calendar/           # Custom Calendar & Date Picker logic
│   ├── detail/             # Transaction detail view
│   ├── gallery/            # Media/Image selection handling
│   ├── home/               # Main Dashboard and Overview
│   └── statics/            # Analytics and Financial statistics
└── utils/                  # Helper classes (Currency, Date, Image Compression)
```md

###  👨‍💻 Author

    NGO HOAI THANH - Lead Android Developer - [GitHub Profile](https://github.com/ngohoaithanh)