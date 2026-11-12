package ir.hamrahproject.app

import android.content.Context
import android.os.Bundle
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    Section("assistant", "دستیار", Icons.Default.AutoAwesome),
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
    val docsDir = remember { File(context.filesDir, "project_docs").apply { mkdirs() } }
    val documents = remember { mutableStateListOf<File>().apply { addAll(docsDir.listFiles()?.toList() ?: emptyList()) } }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "document"
            } ?: "document"
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "document" }
            val target = File(docsDir, "${System.currentTimeMillis()}_$safeName")
            context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            documents.add(target)
        }
    }

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
            } else if (selected == "assistant") {
                AssistantScreen(context)
            } else if (selected == "project") {
                ProjectScreen(projectName, onEdit = { showProjectEdit = true })
            } else if (selected == "settings") {
                SettingsScreen(onNavigate = { selected = it })
            } else if (selected == "reports") {
                ReportsScreen(records, ::listFor)
            } else if (selected == "files") {
                DocumentsScreen(documents, onImport = { documentPicker.launch(arrayOf("application/pdf", "image/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv")) }, onDelete = { file -> file.delete(); documents.remove(file) })
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
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF102A43))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("همراه هوشمند پروژه", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                Text("پروژه‌ات را منظم، قابل‌پیگیری و هدفمند پیش ببر", color = androidx.compose.ui.graphics.Color(0xFFD8E6F3))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سطح ۱  •  شروع‌کننده", color = androidx.compose.ui.graphics.Color(0xFFFFD166))
                    Spacer(Modifier.weight(1f))
                    Text("⭐  ۰ امتیاز", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
        Text("داشبورد پروژه", style = MaterialTheme.typography.headlineSmall)
        Text("نمای کلی اطلاعات ثبت‌شده در $projectName")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("دستیار هوش مصنوعی", style = MaterialTheme.typography.titleMedium)
                Text("برای پاسخ‌گویی هوشمند باید اتصال امن سرویس AI پیکربندی شود. در این نسخه هنوز اتصال فعال نیست.", style = MaterialTheme.typography.bodySmall)
                Text("وضعیت اتصال: پیکربندی نشده", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        }
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


@Composable
private fun DocumentsScreen(files: List<File>, onImport: () -> Unit, onDelete: (File) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("اسناد و نقشه‌ها", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Button(onClick = onImport) { Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(6.dp)); Text("انتخاب فایل") }
        }
        Text("فایل انتخاب‌شده از حافظه گوشی، در فضای خصوصی برنامه کپی می‌شود.")
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("هنوز سندی اضافه نشده است")
                    Text("PDF، تصویر، Word، Excel و CSV", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(files) { _, file ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name.substringAfter('_', file.name), style = MaterialTheme.typography.titleSmall)
                                Text("${(file.length() / 1024).coerceAtLeast(1)} کیلوبایت", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDelete(file) }) { Icon(Icons.Default.Delete, "حذف") }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AssistantScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences("hamrah", 0) }
    var endpoint by remember { mutableStateOf(prefs.getString("aiEndpoint", "") ?: "") }
    var message by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("سلام! بگو چه پروژه‌ای می‌خواهی مدیریت کنی؛ مثلاً ساخت مخزن آب ۲۰۰۰ مترمکعبی.") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("برای فعال شدن پاسخ هوشمند، نشانی سرویس ابری باید ثبت شود.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("دستیار هوشمند پروژه", style = MaterialTheme.typography.headlineSmall)
        Text("گفت‌وگوی تخصصی برای تعریف پروژه، فعالیت‌ها، زمان‌بندی و هزینه‌ها")
        OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانی سرویس ابری") }, placeholder = { Text("https://...workers.dev") }, singleLine = true)
        Button(onClick = {
            prefs.edit().putString("aiEndpoint", endpoint.trim().trimEnd('/')).apply()
            status = if (endpoint.startsWith("https://")) "نشانی ذخیره شد." else "نشانی معتبر HTTPS وارد کنید."
        }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره نشانی اتصال") }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("پاسخ دستیار", style = MaterialTheme.typography.titleMedium)
            Text(answer)
        } }
        OutlinedTextField(message, { message = it }, modifier = Modifier.fillMaxWidth(), label = { Text("پیام شما") }, minLines = 2)
        Button(enabled = !busy && message.isNotBlank(), onClick = {
            val base = prefs.getString("aiEndpoint", "") ?: ""
            if (!base.startsWith("https://")) status = "ابتدا نشانی سرویس ابری را ثبت کنید."
            else {
                busy = true
                val userMessage = message.trim()
                message = ""
                Thread {
                    var result = ""
                    var error = ""
                    try {
                        val connection = (java.net.URL("$base/chat").openConnection() as java.net.HttpURLConnection).apply {
                            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 45000; doOutput = true
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        }
                        val payload = JSONObject().put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", userMessage)))
                        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                        val code = connection.responseCode
                        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                        val parsed = JSONObject(stream.bufferedReader().use { it.readText() })
                        if (code in 200..299) result = parsed.optString("answer") else error = parsed.optString("error", "خطا در دریافت پاسخ")
                        connection.disconnect()
                    } catch (_: Exception) { error = "اتصال برقرار نشد؛ اینترنت و نشانی سرویس را بررسی کنید." }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (result.isNotBlank()) answer = result
                        status = if (error.isBlank()) "پاسخ دریافت شد." else error
                        busy = false
                    }
                }.start()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "در حال دریافت پاسخ..." else "ارسال پیام") }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
