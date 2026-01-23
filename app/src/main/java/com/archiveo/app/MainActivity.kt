package com.archiveo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArchiveoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArchiveoApp()
                }
            }
        }
    }
}

private data class OutlineNode(
    val heading: String,
    val points: List<String>,
    val children: List<OutlineNode> = emptyList(),
)

private data class MindmapNode(
    val text: String,
    val children: List<MindmapNode> = emptyList(),
)

private data class SourceItem(
    val type: String,
    val title: String,
    val body: String,
    val meta: String? = null,
)

private data class Article(
    val id: String,
    val title: String,
    val category: String,
    val keywords: List<String>,
    val createdAt: LocalDateTime,
    val outline: List<OutlineNode>,
    val mindmap: MindmapNode,
    val sources: List<SourceItem>,
)

private enum class InputType(val label: String) {
    Link("链接"),
    Text("文本"),
    Image("图片"),
}

private data class NewEntryInput(
    val type: InputType,
    val content: String,
    val language: String,
    val summaryLength: String,
    val ocrEnabled: Boolean,
    val imageCount: Int,
)

private class ArchiveoState(
    val articles: SnapshotStateList<Article>,
) {
    fun addArticle(input: NewEntryInput): Article {
        val article = Article(
            id = UUID.randomUUID().toString().take(8),
            title = buildTitle(input),
            category = "未分类",
            keywords = buildKeywords(input),
            createdAt = LocalDateTime.now(),
            outline = buildOutline(input),
            mindmap = buildMindmap(input),
            sources = buildSources(input),
        )
        articles.add(0, article)
        return article
    }

    fun findById(id: String): Article? = articles.firstOrNull { it.id == id }
}

@Composable
private fun rememberArchiveoState(): ArchiveoState {
    val articles = remember { mutableStateListOf<Article>().apply { addAll(seedArticles()) } }
    return remember { ArchiveoState(articles) }
}

@Composable
private fun ArchiveoApp() {
    val navController = rememberNavController()
    val appState = rememberArchiveoState()
    val items = listOf(
        BottomNavItem("home", "首页", Icons.Default.Article),
        BottomNavItem("new", "新建", Icons.Default.Add),
        BottomNavItem("search", "搜索", Icons.Default.Search),
        BottomNavItem("settings", "设置", Icons.Default.Settings),
    )

    Scaffold(
        topBar = { ArchiveoTopBar() },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry = navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry.value?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    articles = appState.articles,
                    onArticleSelected = { articleId ->
                        navController.navigate("detail/$articleId")
                    },
                )
            }
            composable("new") {
                NewEntryScreen(
                    onGenerate = { input -> appState.addArticle(input) },
                    onArticleGenerated = { articleId ->
                        navController.navigate("detail/$articleId")
                    },
                )
            }
            composable("search") {
                SearchScreen(
                    articles = appState.articles,
                    onArticleSelected = { articleId ->
                        navController.navigate("detail/$articleId")
                    },
                )
            }
            composable("settings") { SettingsScreen() }
            composable(
                route = "detail/{articleId}",
                arguments = listOf(navArgument("articleId") { defaultValue = "" }),
            ) { entry ->
                val articleId = entry.arguments?.getString("articleId").orEmpty()
                val detail = appState.findById(articleId) ?: appState.articles.first()
                ArticleDetailScreen(
                    detail = detail,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveoTopBar() {
    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(),
    )
    TopAppBar(
        title = { Text("Archiveo · 知库") },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun HomeScreen(articles: List<Article>, onArticleSelected: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "文章列表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(articles) { article ->
            ArticleCardItem(article, onClick = { onArticleSelected(article.id) })
        }
    }
}

@Composable
private fun ArticleCardItem(article: Article, onClick: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagChip(article.category, background = MaterialTheme.colorScheme.primaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = formatTime(article.createdAt), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                article.keywords.take(4).forEach { keyword ->
                    TagChip(keyword)
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, background: Color = MaterialTheme.colorScheme.surface) {
    Box(
        modifier = Modifier
            .background(background, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NewEntryScreen(
    onGenerate: (NewEntryInput) -> Article,
    onArticleGenerated: (String) -> Unit,
) {
    val inputTypes = InputType.values().toList()
    var selectedType by remember { mutableStateOf(InputType.Text) }
    var textInput by remember { mutableStateOf("") }
    var linkInput by remember { mutableStateOf("") }
    val images = remember { mutableStateListOf<String>() }

    val languageOptions = listOf("自动", "中文", "英文")
    val summaryOptions = listOf("短", "中", "长")
    var languageIndex by remember { mutableStateOf(0) }
    var summaryIndex by remember { mutableStateOf(1) }
    var ocrEnabled by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var stepIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    fun currentContent(): String = when (selectedType) {
        InputType.Text -> textInput
        InputType.Link -> linkInput
        InputType.Image -> images.joinToString("\n")
    }

    val placeholder = when (selectedType) {
        InputType.Text -> "在此粘贴文本或笔记段落"
        InputType.Link -> "每行一个链接，例如 https://example.com"
        InputType.Image -> "已选择图片：${images.size} 张"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "新建输入",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            inputTypes.forEach { option ->
                val selectedState = selectedType == option
                Button(
                    onClick = { selectedType = option },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedState) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(option.label)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("输入内容", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedType == InputType.Image) {
                    ImagePickerCard(images)
                } else {
                    OutlinedTextField(
                        value = currentContent(),
                        onValueChange = { value ->
                            if (selectedType == InputType.Text) {
                                textInput = value
                            } else {
                                linkInput = value
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text(placeholder) },
                        isError = errorMessage != null,
                    )
                }
                if (selectedType == InputType.Image) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("高级选项", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptionSelectorRow(
                        label = "语言识别",
                        value = languageOptions[languageIndex],
                        onClick = { languageIndex = (languageIndex + 1) % languageOptions.size },
                    )
                    OptionSelectorRow(
                        label = "摘要长度",
                        value = summaryOptions[summaryIndex],
                        onClick = { summaryIndex = (summaryIndex + 1) % summaryOptions.size },
                    )
                    OptionSelectorRow(
                        label = "图片 OCR",
                        value = if (ocrEnabled) "开启" else "关闭",
                        onClick = { ocrEnabled = !ocrEnabled },
                        enabled = selectedType == InputType.Image,
                    )
                }
            }
        }

        Button(
            onClick = {
                val content = currentContent().trim()
                val hasInput = when (selectedType) {
                    InputType.Image -> images.isNotEmpty()
                    else -> content.isNotBlank()
                }
                if (!hasInput) {
                    errorMessage = "请先输入内容"
                    return@Button
                }
                errorMessage = null
                if (isGenerating) return@Button
                scope.launch {
                    isGenerating = true
                    val steps = listOf("提取", "理解", "组织", "生成")
                    steps.forEachIndexed { index, _ ->
                        stepIndex = index
                        delay(320)
                    }
                    val article = onGenerate(
                        NewEntryInput(
                            type = selectedType,
                            content = content,
                            language = languageOptions[languageIndex],
                            summaryLength = summaryOptions[summaryIndex],
                            ocrEnabled = ocrEnabled,
                            imageCount = images.size,
                        ),
                    )
                    isGenerating = false
                    stepIndex = null
                    onArticleGenerated(article.id)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isGenerating,
        ) {
            Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isGenerating) "生成中..." else "生成文章")
        }

        ProgressStepRow(stepIndex = stepIndex)
    }
}

@Composable
private fun ImagePickerCard(images: SnapshotStateList<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("图片输入", fontWeight = FontWeight.SemiBold)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(images) { image ->
                TagChip(image, background = MaterialTheme.colorScheme.secondaryContainer)
            }
        }
        OutlinedButton(
            onClick = { images.add("图片 ${images.size + 1}") },
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("模拟添加图片")
        }
    }
}

@Composable
private fun OptionSelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onClick, enabled = enabled) {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProgressStepRow(stepIndex: Int?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Text("生成进度", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            val steps = listOf("提取", "理解", "组织", "生成")
            steps.forEachIndexed { index, step ->
                val active = stepIndex != null && index <= stepIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (active) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(step.first().toString(), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(step, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(articles: List<Article>, onArticleSelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = if (query.isBlank()) {
        articles
    } else {
        articles.filter { article ->
            article.title.contains(query, ignoreCase = true) ||
                article.category.contains(query, ignoreCase = true) ||
                article.keywords.any { it.contains(query, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "搜索",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关键词 / 全文", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入搜索词") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text("筛选条件", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip("标签")
                    TagChip("分类")
                    TagChip("时间")
                }
            }
        }
        Text("搜索结果 · ${results.size}", fontWeight = FontWeight.SemiBold)
        results.forEach { article ->
            ArticleCardItem(article, onClick = { onArticleSelected(article.id) })
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SettingsCard(
            title = "导入 / 导出",
            description = "导出 JSON / Markdown / ZIP，支持时间段选择",
        )
        SettingsCard(
            title = "模型与隐私",
            description = "配置模型密钥、隐私提示与日志开关",
        )
        SettingsCard(
            title = "存储与同步",
            description = "本地存储状态、备份与恢复",
        )
    }
}

@Composable
private fun SettingsCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ArticleDetailScreen(detail: Article, onBack: () -> Unit) {
    val tabs = listOf("大纲", "思维导图", "源内容")
    var selectedTab by remember { mutableStateOf(tabs.first()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "返回",
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onBack() },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TagChip(detail.category, background = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatTime(detail.createdAt), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        detail.keywords.take(6).forEach { keyword ->
                            TagChip(keyword)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    Button(
                        onClick = { selectedTab = tab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(tab)
                    }
                }
            }
        }
        when (selectedTab) {
            "大纲" -> {
                items(detail.outline) { node ->
                    OutlineCard(node, depth = 0)
                }
            }
            "思维导图" -> {
                item { MindmapCard(detail.mindmap) }
            }
            else -> {
                items(detail.sources) { source ->
                    SourceCard(source)
                }
            }
        }
    }
}

@Composable
private fun OutlineCard(node: OutlineNode, depth: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(node.heading, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            node.points.forEach { point ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("-", fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(point, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    node.children.forEach { child ->
        OutlineCard(child, depth = depth + 1)
    }
}

@Composable
private fun MindmapCard(node: MindmapNode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("思维导图", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            MindmapNodeView(node, depth = 0)
        }
    }
}

@Composable
private fun MindmapNodeView(node: MindmapNode, depth: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width((depth * 12).dp))
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(node.text, style = MaterialTheme.typography.labelSmall)
        }
    }
    node.children.forEach { child ->
        Spacer(modifier = Modifier.height(6.dp))
        MindmapNodeView(child, depth + 1)
    }
}

@Composable
private fun SourceCard(source: SourceItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${source.type} · ${source.title}", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(source.body, style = MaterialTheme.typography.bodySmall)
            source.meta?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ArchiveoTheme(content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}

private fun seedArticles(): List<Article> {
    val now = LocalDateTime.now()
    return listOf(
        Article(
            id = "a1",
            title = "AI 总结驱动的知识归档方法",
            category = "知识管理",
            keywords = listOf("摘要", "结构化", "检索", "归档"),
            createdAt = now.minusHours(2),
            outline = listOf(
                OutlineNode(
                    heading = "问题与目标",
                    points = listOf("碎片化输入难以长期复用", "需要可检索的结构化内容"),
                ),
                OutlineNode(
                    heading = "核心流程",
                    points = listOf("输入 → 理解 → 组织 → 生成文章"),
                    children = listOf(
                        OutlineNode(
                            heading = "输入层",
                            points = listOf("链接、文本、图片", "保留原始来源"),
                        ),
                        OutlineNode(
                            heading = "输出层",
                            points = listOf("标题、关键词、大纲、思维导图"),
                        ),
                    ),
                ),
                OutlineNode(
                    heading = "落地建议",
                    points = listOf("先做文本输入 MVP", "逐步完善导入导出"),
                ),
            ),
            mindmap = MindmapNode(
                text = "知识归档",
                children = listOf(
                    MindmapNode(
                        text = "输入",
                        children = listOf(
                            MindmapNode(text = "链接"),
                            MindmapNode(text = "文本"),
                            MindmapNode(text = "图片"),
                        ),
                    ),
                    MindmapNode(
                        text = "AI 处理",
                        children = listOf(
                            MindmapNode(text = "提炼主题"),
                            MindmapNode(text = "组织结构"),
                            MindmapNode(text = "生成摘要"),
                        ),
                    ),
                    MindmapNode(
                        text = "归档输出",
                        children = listOf(
                            MindmapNode(text = "文章"),
                            MindmapNode(text = "标签/分类"),
                        ),
                    ),
                ),
            ),
            sources = listOf(
                SourceItem(
                    type = "链接",
                    title = "原始网页",
                    body = "https://example.com/knowledge-archive",
                    meta = "抓取时间：今天 10:20",
                ),
                SourceItem(
                    type = "文本",
                    title = "粘贴文本",
                    body = "整理知识的关键在于保留来源，形成可复用的文章结构。",
                ),
                SourceItem(
                    type = "图片",
                    title = "会议白板照片",
                    body = "已生成图片描述：白板包含三步流程与重点标注。",
                    meta = "OCR：未开启",
                ),
            ),
        ),
        Article(
            id = "a2",
            title = "产品复盘：如何减少信息噪音",
            category = "产品思考",
            keywords = listOf("复盘", "信号", "流程", "决策"),
            createdAt = now.minusDays(1),
            outline = listOf(
                OutlineNode(
                    heading = "复盘背景",
                    points = listOf("信息过载导致决策延迟", "团队需要共识"),
                ),
                OutlineNode(
                    heading = "三步流程",
                    points = listOf("收集事实", "过滤噪音", "沉淀原则"),
                ),
                OutlineNode(
                    heading = "实践建议",
                    points = listOf("每周固定复盘", "成果进入知识库"),
                ),
            ),
            mindmap = MindmapNode(
                text = "信息噪音",
                children = listOf(
                    MindmapNode(text = "问题"),
                    MindmapNode(text = "过滤策略"),
                    MindmapNode(text = "复盘输出"),
                ),
            ),
            sources = listOf(
                SourceItem(
                    type = "文本",
                    title = "复盘记录",
                    body = "本周新增需求较多，需要聚焦核心指标。",
                ),
                SourceItem(
                    type = "链接",
                    title = "参考资料",
                    body = "https://example.com/product-review",
                    meta = "抓取时间：昨天 18:20",
                ),
            ),
        ),
        Article(
            id = "a3",
            title = "读书笔记：系统化学习的三步",
            category = "学习",
            keywords = listOf("大纲", "知识库", "输出", "复盘"),
            createdAt = now.minusDays(2),
            outline = listOf(
                OutlineNode(
                    heading = "建立体系",
                    points = listOf("定义学习目标", "拆解知识模块"),
                ),
                OutlineNode(
                    heading = "高效输入",
                    points = listOf("做笔记", "及时归档"),
                ),
                OutlineNode(
                    heading = "输出复盘",
                    points = listOf("总结文章", "形成长期记忆"),
                ),
            ),
            mindmap = MindmapNode(
                text = "系统化学习",
                children = listOf(
                    MindmapNode(text = "体系"),
                    MindmapNode(text = "输入"),
                    MindmapNode(text = "输出"),
                ),
            ),
            sources = listOf(
                SourceItem(
                    type = "文本",
                    title = "书籍摘要",
                    body = "系统化学习强调从目标出发，持续输出。",
                ),
            ),
        ),
    )
}

private fun buildTitle(input: NewEntryInput): String {
    val trimmed = input.content.trim()
    return when (input.type) {
        InputType.Text -> trimmed.lineSequence().firstOrNull()?.take(26)?.ifBlank { "文本摘要" }
            ?: "文本摘要"
        InputType.Link -> "链接归档"
        InputType.Image -> "图片归档"
    }
}

private fun buildKeywords(input: NewEntryInput): List<String> {
    val base = listOf("摘要", "结构化", "归档")
    return when (input.type) {
        InputType.Text -> base + listOf("文本", input.summaryLength)
        InputType.Link -> base + listOf("链接", "来源")
        InputType.Image -> base + listOf("图片", if (input.ocrEnabled) "OCR" else "描述")
    }
}

private fun buildOutline(input: NewEntryInput): List<OutlineNode> {
    return when (input.type) {
        InputType.Text -> {
            val highlights = extractHighlights(input.content)
            listOf(
                OutlineNode(
                    heading = "核心要点",
                    points = if (highlights.isNotEmpty()) highlights else listOf("已根据文本生成摘要"),
                ),
                OutlineNode(
                    heading = "结构建议",
                    points = listOf("保留原文语境", "按主题组织段落"),
                ),
            )
        }
        InputType.Link -> listOf(
            OutlineNode(
                heading = "抓取与清洗",
                points = listOf("提取正文", "保留标题与发布时间"),
            ),
            OutlineNode(
                heading = "内容理解",
                points = listOf("聚合要点", "生成摘要与关键词"),
            ),
        )
        InputType.Image -> listOf(
            OutlineNode(
                heading = "图片处理",
                points = listOf("生成图片描述", "保留原图引用"),
            ),
            OutlineNode(
                heading = "文字提取",
                points = listOf(if (input.ocrEnabled) "已执行 OCR" else "可选 OCR 提取"),
            ),
        )
    }
}

private fun buildMindmap(input: NewEntryInput): MindmapNode {
    return MindmapNode(
        text = buildTitle(input),
        children = listOf(
            MindmapNode(text = "输入"),
            MindmapNode(text = "摘要"),
            MindmapNode(text = "归档"),
        ),
    )
}

private fun buildSources(input: NewEntryInput): List<SourceItem> {
    val meta = "语言：${input.language} · 摘要长度：${input.summaryLength}"
    return when (input.type) {
        InputType.Text -> listOf(
            SourceItem(
                type = "文本",
                title = "原始文本",
                body = input.content.take(180),
                meta = meta,
            ),
        )
        InputType.Link -> listOf(
            SourceItem(
                type = "链接",
                title = "输入链接",
                body = input.content,
                meta = meta,
            ),
        )
        InputType.Image -> listOf(
            SourceItem(
                type = "图片",
                title = "图片输入",
                body = "共 ${input.imageCount} 张图片",
                meta = if (input.ocrEnabled) "$meta · OCR 已开启" else "$meta · OCR 未开启",
            ),
        )
    }
}

private fun extractHighlights(content: String): List<String> {
    return content
        .replace("\r", "")
        .split("\n")
        .flatMap { line -> line.split("。", "！", "？") }
        .map { it.trim() }
        .filter { it.length >= 6 }
        .distinct()
        .take(3)
}

private fun formatTime(time: LocalDateTime): String {
    val today = LocalDate.now()
    val date = time.toLocalDate()
    val timeText = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (date) {
        today -> "今天 · $timeText"
        today.minusDays(1) -> "昨天 · $timeText"
        else -> time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd · HH:mm"))
    }
}
