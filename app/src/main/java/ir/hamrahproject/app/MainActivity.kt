package ir.hamrahproject.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private data class Section(val key: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private data class Entry(val title: String, val detail: String)

private val sections = listOf(
    Section("home", "خانه", Icons.Default.Home),
    Section("project", "پروژه", Icons.Default.Business),
    Section("tasks", "فعالیت‌ها", Icons.Default.CheckCircle),
    Section("schedule", "زمان‌بندی", Icons.Default.CalendarMonth),
    Section("progress", "پیشرفت", Icons.Default.TrendingUp),
    Section("costs", "هزینه‌ها", Icons.Default.Payments),
    Section("files", "اسناد و نقشه‌ها", Icons.Default.Folder),
    Section("reports", "گزارش‌ها", Icons.Default.Assessment),
    Section("settings", "تنظیمات", Icons.Default.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF176B5B))) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HamrahApp(applicationContext)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HamrahApp(context: Context) {
    var selected by remember { mutableStateOf("home") }
    var projectName by remember { mutableStateOf(context.getSharedPreferences("hamrah", 0).getString("projectName", "پروژه جدید") ?: "پروژه جدید") }
    val records = remember { mutableStateMapOf<String, MutableList<Entry>>() }
    var showAdd by remember { mutableStateOf(false) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var inputTitle by remember { mutableStateOf("") }
    var inputDetail by remember { mutableStateOf("") }
    var showProjectEdit by remember { mutableStateOf(false) }

    fun load(key: String): MutableList<Entry> {
        val arr = JSONArray(context.getSharedPreferences("hamrah", 0).getString(key, "[]") ?: "[]")
        val result = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            result.add(Entry(o.optString("title"), o.optString("detail")))
        }
        return result
    }
    fun save(key: String) {
        val arr = JSONArray()
        (records[key] ?: emptyList()).forEach { e -> arr.put(JSONObject().put("title", e.title).put("detail", e.detail)) }
        context.getSharedPreferences("hamrah", 0).edit().putString(key, arr.toString()).apply()
    }
    fun listFor(key: String): MutableList<Entry> {
        if (!records.containsKey(key)) records[key] = load(key)
        return records[key]!!
    }

    val current = sections.first { it.key == selected }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("همراه پروژه"); Text(projectName, style = MaterialTheme.typography.labelSmall) } },
                actions = { if (selected == "project") IconButton(onClick = { showProjectEdit = true }) { Icon(Icons.Default.Edit, "ویرایش نام") } }
            )
        },
        bottomBar = {
            NavigationBar {
                val bottom = sections.take(5)
                bottom.forEach { s ->
                    NavigationBarItem(selected == s.key, onClick = { selected = s.key }, icon = { Icon(s.icon, s.title) }, label = { Text(s.title) })
                }
            }
        },
        floatingActionButton = {
            if (selected in listOf("tasks", "schedule", "progress", "costs", "files")) {
                FloatingActionButton(onClick = {
                    editIndex = -1; inputTitle = ""; inputDetail = ""; showAdd = true
                }) { Icon(Icons.Default.Add, "افزودن") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (selected == "home") {
                HomeScreen(projectName, records, ::listFor, onSelect = { selected = it })
            } else if (selected == "project") {
                ProjectScreen(projectName, onEdit = { showProjectEdit = true })
            } else if (selected == "settings") {
                SettingsScreen(onNavigate = { selected = it })
            } else if (selected == "reports") {
                ReportsScreen(records, ::listFor)
            } else {
                val key = selected
                val list = listFor(key)
                RecordsScreen(current.title, list,
                    onEdit = { index, entry ->
                        editIndex = index; inputTitle = entry.title; inputDetail = entry.detail; showAdd = true
                    },
                    onDelete = { index -> list.removeAt(index); save(key) },
                    onAdd = { editIndex = -1; inputTitle = ""; inputDetail = ""; showAdd = true }
                )
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(if (editIndex < 0) "افزودن مورد" else "ویرایش مورد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(inputTitle, { inputTitle = it }, label = { Text("عنوان") }, singleLine = true)
                    OutlinedTextField(inputDetail, { inputDetail = it }, label = { Text("توضیحات / مقدار / تاریخ") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputTitle.isNotBlank()) {
                        val list = listFor(selected)
                        val entry = Entry(inputTitle.trim(), inputDetail.trim())
                        if (editIndex >= 0 && editIndex < list.size) list[editIndex] = entry else list.add(entry)
                        save(selected)
                    }
                    showAdd = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("انصراف") } }
        )
    }
    if (showProjectEdit) {
        AlertDialog(
            onDismissRequest = { showProjectEdit = false },
            title = { Text("نام پروژه") },
            text = { OutlinedTextField(projectName, { projectName = it }, label = { Text("عنوان پروژه") }) },
            confirmButton = { TextButton(onClick = {
                context.getSharedPreferences("hamrah", 0).edit().putString("projectName", projectName).apply()
                showProjectEdit = false
            }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showProjectEdit = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun HomeScreen(projectName: String, records: Map<String, MutableList<Entry>>, listFor: (String) -> MutableList<Entry>, onSelect: (String) -> Unit) {
    val totalTasks = listFor("tasks").size
    val totalCosts = listFor("costs").size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(4.dp))
        Text("داشبورد پروژه", style = MaterialTheme.typography.headlineSmall)
        Text("نمای کلی اطلاعات ثبت‌شده در $projectName")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("فعالیت‌ها", totalTasks.toString(), Modifier.weight(1f))
            SummaryCard("اقلام هزینه", totalCosts.toString(), Modifier.weight(1f))
            SummaryCard("اسناد", listFor("files").size.toString(), Modifier.weight(1f))
        }
        Text("دسترسی سریع", style = MaterialTheme.typography.titleMedium)
        sections.drop(1).forEach { s ->
            ElevatedCard(onClick = { onSelect(s.key) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(s.icon, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(s.title, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronLeft, null)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProjectScreen(name: String, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("اطلاعات پایه پروژه", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نام پروژه", style = MaterialTheme.typography.labelMedium)
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text("برای شروع، نام پروژه را وارد کنید و سپس فعالیت‌ها، زمان‌بندی و هزینه‌ها را ثبت کنید.")
                Button(onClick = onEdit) { Text("ویرایش نام پروژه") }
            }
        }
    }
}

@Composable
private fun RecordsScreen(title: String, entries: List<Entry>, onEdit: (Int, Entry) -> Unit, onDelete: (Int) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("افزودن") }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("هنوز موردی ثبت نشده است")
                    Text("برای ثبت اولین مورد، دکمه افزودن را بزنید", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                itemsIndexed(entries) { index, entry ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            if (entry.detail.isNotBlank()) Text(entry.detail, style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEdit(index, entry) }) { Icon(Icons.Default.Edit, null); Text("ویرایش") }
                                TextButton(onClick = { onDelete(index) }) { Icon(Icons.Default.Delete, null); Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(records: Map<String, MutableList<Entry>>, listFor: (String) -> MutableList<Entry>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("گزارش وضعیت", style = MaterialTheme.typography.headlineSmall)
        Text("این گزارش بر اساس اطلاعاتی است که در برنامه ثبت کرده‌اید.")
        sections.filter { it.key in listOf("tasks", "schedule", "progress", "costs", "files") }.forEach { s ->
            val count = listFor(s.key).size
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.title, Modifier.weight(1f))
                    Text("$count مورد", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text("برای گزارش دقیق‌تر، ابتدا اطلاعات هر بخش را ثبت کنید.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات و بخش‌ها", style = MaterialTheme.typography.headlineSmall)
        Text("برای ورود به هر بخش، آن را انتخاب کنید.")
        sections.drop(1).forEach { s ->
            OutlinedButton(onClick = { onNavigate(s.key) }, modifier = Modifier.fillMaxWidth()) {
                Icon(s.icon, null); Spacer(Modifier.width(8.dp)); Text(s.title)
            }
        }
        Text("نسخه آزمایشی ۰٫۲ - اطلاعات روی همین گوشی ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall)
    }
}
