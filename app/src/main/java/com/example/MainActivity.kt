package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CollageProject
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

// Layout fractional coordinate model
data class CollageSlot(
    val leftFraction: Float,
    val topFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float
)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_main_surface"),
                    color = Color(0xFFFEF7FF) // Geometric Balance surface
                ) {
                    AppContent(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val shareProgress by viewModel.shareFileProgress.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewModel.currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    projects = projects,
                    isSyncing = isSyncing,
                    onSyncClick = { viewModel.syncProjectsToCloud() }
                )
                "creator" -> CreatorScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.currentScreen = "dashboard" }
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.currentScreen = "dashboard" }
                )
            }
        }

        // Sharing Overlay Indicator
        if (shareProgress != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = shareProgress ?: "",
                            color = Color(0xFF1D1B20),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    projects: List<CollageProject>,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.currentScreen = "settings" },
                    modifier = Modifier.testTag("hamburger_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open Settings & Local Files",
                        tint = Color(0xFF1D1B20)
                    )
                }
                Text(
                    text = "Collage Pro Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1D1B20)
                )
                IconButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("cloud_sync_btn")
                ) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Filled.Refresh else Icons.Filled.CloudDone,
                        contentDescription = "Cloud Status",
                        tint = if (isSyncing) Color(0xFF6750A4) else Color(0xFF0F823E)
                    )
                }
            }
        },
        bottomBar = {
            // Elegant studio trademark footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "© Collage Pro Studio • Local & Cloud Backup Ready",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = Color(0xFFFEF7FF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Brand Title Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .drawBehind {
                        // Drawing custom design studio background
                        val colorsList = listOf(Color(0xFFF3EDF7), Color(0xFFFEF7FF))
                        drawRect(
                            brush = Brush.verticalGradient(colorsList),
                            size = size
                        )
                        drawCircle(
                            color = Color(0xFF6750A4).copy(alpha = 0.06f),
                            radius = size.width * 0.4f,
                            center = Offset(size.width * 0.9f, size.height * 0.1f)
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6750A4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GridView,
                                contentDescription = "Studio Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Picture Collage Pro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF1D1B20)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = "Cloud Synced",
                                    tint = Color(0xFF0F823E),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Cloud Synced",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F823E)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Customize professional grids, filters & watermarks",
                        fontSize = 13.sp,
                        color = Color(0xFF49454F),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Cloud Sync Bar
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3EDF7))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Filled.Refresh else Icons.Filled.Cloud,
                                contentDescription = "Cloud Status",
                                tint = if (isSyncing) Color(0xFF6750A4) else Color(0xFF0F823E),
                                modifier = Modifier
                                    .size(18.dp)
                                    .run {
                                        if (isSyncing) graphicsLayer(rotationZ = System.currentTimeMillis() % 360f) else this
                                    }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSyncing) "Syncing with cloud storage..." else "${projects.size} projects backed up",
                                fontSize = 12.sp,
                                color = Color(0xFF1D1B20)
                            )
                        }
                        IconButton(
                            onClick = onSyncClick,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("sync_cloud_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Force Cloud Synchronization",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Quick Create Grid Selection
            Text(
                text = "START A NEW CREATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            // Responsive equal-padding rows of collage size templates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GridCreatorButton(size = 2, icon = Icons.Filled.GridGoldenratio, viewModel, Modifier.weight(1f))
                GridCreatorButton(size = 4, icon = Icons.Filled.GridView, viewModel, Modifier.weight(1f))
                GridCreatorButton(size = 6, icon = Icons.Filled.SquareFoot, viewModel, Modifier.weight(1f))
                GridCreatorButton(size = 9, icon = Icons.Filled.Apps, viewModel, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Project List section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR SAVED PROJECTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF49454F)
                )
                Text(
                    text = "${projects.size} items",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (projects.isEmpty()) {
                // Highly visual empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF3EDF7))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Empty",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved projects yet",
                            color = Color(0xFF1D1B20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Choose a layout above to assemble your first photography collage!",
                            color = Color(0xFF49454F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                projects.forEach { project ->
                    ProjectItemRow(
                        project = project,
                        onLoad = { viewModel.loadProject(project) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun GridCreatorButton(
    size: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable {
                viewModel.startNewProject(size)
                viewModel.currentScreen = "creator"
            }
            .testTag("create_grid_$size"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$size Grids creation",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$size Grid",
                color = Color(0xFF1D1B20),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ProjectItemRow(
    project: CollageProject,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(project.timestamp) {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(project.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        color = Color(0xFF1D1B20),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Synced indicator
                    Icon(
                        imageVector = if (project.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = "Sync State Indicator",
                        tint = if (project.isSynced) Color(0xFF0F823E) else Color(0xFF8A8590),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Layout: ${project.gridLayoutSize}-cell grid • Apply: ${project.filterName}",
                    color = Color(0xFF49454F),
                    fontSize = 12.sp
                )
                Text(
                    text = "Saved: $dateString",
                    color = Color(0xFF8A8590),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLoad) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Load layout",
                        tint = Color(0xFF6750A4)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF8A8590)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatorScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("grids") } // grids, filters, watermark, batch
    val focusManager = LocalFocusManager.current

    // Media Picker initialization
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectedSlotIndex?.let { index ->
                viewModel.setImageInSlot(index, it.toString())
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Exit to dashboard",
                            tint = Color(0xFF1D1B20)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        BasicTextField(
                            value = viewModel.activeProjectName,
                            onValueChange = { viewModel.activeProjectName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF1D1B20),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .width(160.dp)
                                .testTag("project_name_input")
                        )
                        Text(
                            text = "Auto-save ready",
                            color = Color(0xFF6750A4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row {
                    // Quick Save local Button
                    IconButton(onClick = { viewModel.saveProject() }) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save project locally",
                            tint = Color(0xFF1D1B20)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Elegant export bars containing JPG / PDF exports & color switch options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = Color(0xFFCAC4D0), thickness = 1.dp)

                // Color preference toggle (Colour Palette vs Black & White output)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Global Output Style:",
                        color = Color(0xFF1D1B20),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFFF3EDF7))
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "COLOUR",
                            color = if (viewModel.isColorOutput) Color.White else Color(0xFF49454F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (viewModel.isColorOutput) Color(0xFF6750A4) else Color.Transparent)
                                .clickable { viewModel.isColorOutput = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "MONO B&W",
                            color = if (!viewModel.isColorOutput) Color.White else Color(0xFF49454F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (!viewModel.isColorOutput) Color(0xFF49454F) else Color.Transparent)
                                .clickable { viewModel.isColorOutput = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFCAC4D0), thickness = 1.dp)

                // EXPORT ACTIONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportAndShare("JPG", context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_jpg_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JPG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.exportAndShare("PDF", context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_btn")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFFFEF7FF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful display showing exact output naming format
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3EDF7))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Drafts,
                    contentDescription = "Draft Icon",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.generateDisplayFileName(),
                    color = Color(0xFF49454F),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Interactive Canvas Workspace
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(4.dp, Color.White, RoundedCornerShape(24.dp))
                    .border(5.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .testTag("collage_canvas_container"),
                contentAlignment = Alignment.Center
            ) {
                CollageWorkspaceCanvas(
                    viewModel = viewModel,
                    onImageClick = { index ->
                        viewModel.selectedSlotIndex = index
                        photoPickerLauncher.launch("image/*")
                    }
                )
            }

            // Guide instruction banner
            Text(
                text = "Long press cells to Swap easily • Tap cells to replace content",
                fontSize = 11.sp,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Cell control settings if a slot was manually selected
            viewModel.selectedSlotIndex?.let { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3EDF7))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Cell #${index + 1} Selected", color = Color(0xFF1D1B20), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { viewModel.rotateSlot(index) }) {
                            Icon(Icons.Filled.RotateRight, "Rotate Slot", tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            viewModel.setImageInSlot(index, viewModel.presetOptions[index % viewModel.presetOptions.size])
                            viewModel.selectedSlotIndex = null
                        }) {
                            Icon(Icons.Filled.ClearAll, "Reset Placeholder", tint = Color(0xFFBA1A1A), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STUDIO CONTROL TABS Navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
            ) {
                TabButton(text = "Templates", isActive = activeTab == "grids", onClick = { activeTab = "grids" }, modifier = Modifier.weight(1f))
                TabButton(text = "Filters", isActive = activeTab == "filters", onClick = { activeTab = "filters" }, modifier = Modifier.weight(1f))
                TabButton(text = "Watermark", isActive = activeTab == "watermark", onClick = { activeTab = "watermark" }, modifier = Modifier.weight(1f))
                TabButton(text = "Batch Stream", isActive = activeTab == "batch", onClick = { activeTab = "batch" }, modifier = Modifier.weight(1f))
            }

            // TAB EXPAND PANEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .padding(20.dp)
            ) {
                when (activeTab) {
                    "grids" -> GridsTabContent(viewModel)
                    "filters" -> FiltersTabContent(viewModel)
                    "watermark" -> WatermarkTabContent(viewModel)
                    "batch" -> BatchTabContent(viewModel)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) Color(0xFF6750A4) else Color.Transparent
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color(0xFF6750A4) else Color(0xFF49454F)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollageWorkspaceCanvas(
    viewModel: MainViewModel,
    onImageClick: (Int) -> Unit
) {
    val template = viewModel.templateIndex
    val size = viewModel.gridLayoutSize
    val slots = remember(size, template) { getCollageSlots(size, template) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight

        slots.forEachIndexed { index, slot ->
            val isSelected = viewModel.selectedSlotIndex == index
            val isDragging = draggingIndex == index

            Box(
                modifier = Modifier
                    .offset(
                        x = (slot.leftFraction * wPx / (LocalContext.current.resources.displayMetrics.density)).dp,
                        y = (slot.topFraction * hPx / (LocalContext.current.resources.displayMetrics.density)).dp
                    )
                    .size(
                        width = (slot.widthFraction * wPx / (LocalContext.current.resources.displayMetrics.density)).dp,
                        height = (slot.heightFraction * hPx / (LocalContext.current.resources.displayMetrics.density)).dp
                    )
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF3EDF7))
                    .border(
                        BorderStroke(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = when {
                                isSelected -> Color(0xFF6750A4)
                                isDragging -> Color(0xFF6750A4)
                                else -> Color(0xFFCAC4D0)
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .combinedClickable(
                        onLongClick = {
                            // High interactive Swap Mode
                            if (draggingIndex == null) {
                                draggingIndex = index
                                viewModel.selectedSlotIndex = index
                            } else {
                                viewModel.swapSlots(draggingIndex!!, index)
                                draggingIndex = null
                            }
                        },
                        onClick = {
                            if (draggingIndex != null) {
                                viewModel.swapSlots(draggingIndex!!, index)
                                draggingIndex = null
                            } else {
                                viewModel.selectedSlotIndex = index
                                onImageClick(index)
                            }
                        }
                    )
                    .testTag("canvas_slot_$index"),
                contentAlignment = Alignment.Center
            ) {
                val imagePath = viewModel.activeImages.getOrElse(index) { "" }
                val isPreset = imagePath.startsWith("preset:")

                // Live Color Matrix filters mapping
                val filterMatrix = remember(viewModel.selectedFilter, viewModel.isColorOutput) {
                    getLiveFilterMatrix(viewModel.selectedFilter, viewModel.isColorOutput)
                }

                if (isPreset) {
                    // Render premium styled cell backgrounds immediately
                    val seed = imagePath.removePrefix("preset:").toIntOrNull() ?: 1
                    val colorHex = getPresetHexHex(seed)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(colorHex)),
                                        Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .graphicsLayer {
                                rotationZ = viewModel.activeRotations.getOrElse(index) { 0f }
                            }
                    ) {
                        // Subtle abstract geometric vector background for premium design
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                                .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = "Upload placeholder",
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp)
                        )
                    }
                } else {
                    // Actual dynamic loaded Uri
                    AsyncImage(
                        model = Uri.parse(imagePath),
                        contentDescription = "Active Custom Selected Media Slot",
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(filterMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = viewModel.activeRotations.getOrElse(index) { 0f }
                            }
                    )
                }

                // Small cell index tag label
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${index + 1}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // If this is the active anchor cell during the swap loop
                if (draggingIndex == index) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF00B4D8).copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SWAP ACTIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Live professional customizable watermark overlay inside the preview canvas!
        if (viewModel.watermarkText.isNotBlank()) {
            val watermarkColor = when (viewModel.watermarkColorName) {
                "Black" -> Color.Black
                "Gold" -> Color(0xFFD4AF37)
                "Accent" -> Color(0xFFFF4081)
                else -> Color.White
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = viewModel.watermarkText,
                    color = watermarkColor.copy(alpha = viewModel.watermarkOpacity),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

// Render colors safely from seeds
private fun getPresetHexHex(seed: Int): String {
    val list = listOf(
        "#402240", "#a12230", "#304a5f", "#334f3c",
        "#604f32", "#473b5a", "#795246", "#1c313a",
        "#512da8", "#00796b", "#388e3c", "#fbc02d"
    )
    return list[(seed - 1).coerceIn(0, list.size - 1)]
}

@Composable
fun GridsTabContent(viewModel: MainViewModel) {
    Column {
        Text(text = "CHOOSE TEMPLATE STYLE", fontSize = 12.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            TemplateVariantCard(
                name = "Equal Split",
                description = "Classic balances",
                isSelected = viewModel.templateIndex == 0,
                onClick = { viewModel.templateIndex = 0 }
            )
            TemplateVariantCard(
                name = "Asymmetric Spotlight",
                description = "Highlight central focal point",
                isSelected = viewModel.templateIndex == 1,
                onClick = { viewModel.templateIndex = 1 }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "GRID LAYOUTS", fontSize = 11.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(2, 4, 6, 9).forEach { size ->
                OutlinedButton(
                    onClick = { viewModel.startNewProject(size) },
                    border = BorderStroke(
                        width = if (viewModel.gridLayoutSize == size) 2.dp else 1.dp,
                        color = if (viewModel.gridLayoutSize == size) Color(0xFF6750A4) else Color(0xFFCAC4D0)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (viewModel.gridLayoutSize == size) Color(0xFFEADDFF) else Color.Transparent,
                        contentColor = if (viewModel.gridLayoutSize == size) Color(0xFF21005D) else Color(0xFF49454F)
                    ),
                    modifier = Modifier.testTag("switch_layout_$size")
                ) {
                    Text("$size Cells")
                }
            }
        }
    }
}

@Composable
fun TemplateVariantCard(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = description, color = Color(0xFF49454F), fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun FiltersTabContent(viewModel: MainViewModel) {
    val filters = listOf("Classic", "Cinema", "Warm", "Cool", "Sepia", "Monochrome", "Vintage")
    Column {
        Text(text = "LIVE RETRO FILTER PRESETS", fontSize = 12.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            filters.forEach { filter ->
                val isSelected = viewModel.selectedFilter == filter
                Card(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { viewModel.selectedFilter = filter }
                        .testTag("filter_card_$filter"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drawing dynamic mini graphical representations of filter styling
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6750A4), Color(0xFFEADDFF))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = filter,
                            color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatermarkTabContent(viewModel: MainViewModel) {
    Column {
        Text(text = "PROFESSIONAL WATERMARK", fontSize = 12.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.watermarkText,
            onValueChange = { viewModel.watermarkText = it },
            label = { Text("Watermark branding text", color = Color(0xFF49454F)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1D1B20),
                unfocusedTextColor = Color(0xFF1D1B20),
                focusedBorderColor = Color(0xFF6750A4),
                unfocusedBorderColor = Color(0xFFCAC4D0),
                focusedLabelColor = Color(0xFF6750A4),
                unfocusedLabelColor = Color(0xFF49454F)
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("watermark_text_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Opacity slider
        Text(text = "Opacity: ${(viewModel.watermarkOpacity * 100).toInt()}%", color = Color(0xFF1D1B20), fontSize = 12.sp)
        Slider(
            value = viewModel.watermarkOpacity,
            onValueChange = { viewModel.watermarkOpacity = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6750A4),
                activeTrackColor = Color(0xFF6750A4),
                inactiveTrackColor = Color(0xFFCAC4D0)
            ),
            modifier = Modifier.testTag("watermark_opacity_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Badge selectors
        Text(text = "Select Branding Hue:", color = Color(0xFF49454F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("White", "Black", "Gold", "Accent").forEach { color ->
                val isSelected = viewModel.watermarkColorName == color
                Text(
                    text = color,
                    color = if (isSelected) Color.White else Color(0xFF49454F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF6750A4) else Color(0xFFF3EDF7))
                        .clickable { viewModel.watermarkColorName = color }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("watermark_color_$color")
                )
            }
        }
    }
}

@Composable
fun BatchTabContent(viewModel: MainViewModel) {
    Column {
        Text(text = "BATCH MULTI-CELL ACTIONS", fontSize = 12.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
        Text(text = "Apply rapid changes to streamline your creation process instantly:", fontSize = 11.sp, color = Color(0xFF49454F), modifier = Modifier.padding(vertical = 4.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BatchActionButton(
                label = "Rotate All 90°",
                vector = Icons.Filled.RotateRight,
                onClick = { viewModel.batchRotateAll() },
                modifier = Modifier.weight(1f).testTag("batch_rotate_all")
            )
            BatchActionButton(
                label = "Abstract Art",
                vector = Icons.Filled.AutoAwesome,
                onClick = { viewModel.batchRandomizePresets() },
                modifier = Modifier.weight(1f).testTag("batch_randomize")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BatchActionButton(
                label = "Apply Sepia All",
                vector = Icons.Filled.FilterFrames,
                onClick = { viewModel.batchApplyFilter("Sepia") },
                modifier = Modifier.weight(1f).testTag("batch_filter_sepia")
            )
            BatchActionButton(
                label = "Clear All Cells",
                vector = Icons.Filled.Refresh,
                onClick = { viewModel.batchClearAll() },
                modifier = Modifier.weight(1f).testTag("batch_clear_all")
            )
        }
    }
}

@Composable
fun BatchActionButton(
    label: String,
    vector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1D1B20)
        ),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = vector, contentDescription = label, tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
        }
    }
}

private fun getLiveFilterMatrix(filterName: String, isColor: Boolean): ColorMatrix {
    if (!isColor) {
        return ColorMatrix().apply { setToSaturation(0f) }
    }
    return when (filterName) {
        "Sepia" -> {
            ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            ))
        }
        "Monochrome" -> ColorMatrix().apply { setToSaturation(0f) }
        "Cinema" -> ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, -10f,
            0f, 1.1f, 0f, 0f, -10f,
            0f, 0f, 1.4f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
        "Warm" -> ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 20f,
            0f, 1.0f, 0f, 0f, 5f,
            0f, 0f, 0.8f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ))
        "Cool" -> ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, -10f,
            0f, 1.0f, 0f, 0f, 5f,
            0f, 0f, 1.3f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        ))
        "Vintage" -> ColorMatrix(floatArrayOf(
            0.9f, 0.1f, 0.1f, 0f, 15f,
            0.1f, 0.8f, 0.1f, 0f, 10f,
            0.1f, 0.1f, 0.7f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        ))
        else -> ColorMatrix()
    }
}

private fun getCollageSlots(size: Int, template: Int): List<CollageSlot> {
    val list = mutableListOf<CollageSlot>()
    when (size) {
        2 -> {
            if (template == 0) { // Vertical halves
                list.add(CollageSlot(0f, 0f, 0.5f, 1.0f))
                list.add(CollageSlot(0.5f, 0f, 0.5f, 1.0f))
            } else { // Horizontal halves
                list.add(CollageSlot(0f, 0f, 1.0f, 0.5f))
                list.add(CollageSlot(0f, 0.5f, 1.0f, 0.5f))
            }
        }
        4 -> {
            if (template == 0) { // 2x2 standard
                list.add(CollageSlot(0f, 0f, 0.5f, 0.5f))
                list.add(CollageSlot(0.5f, 0f, 0.5f, 0.5f))
                list.add(CollageSlot(0f, 0.5f, 0.5f, 0.5f))
                list.add(CollageSlot(0.5f, 0.5f, 0.5f, 0.5f))
            } else { // Asymmetric focus
                list.add(CollageSlot(0f, 0f, 0.6f, 1.0f))
                list.add(CollageSlot(0.6f, 0f, 0.4f, 1/3f))
                list.add(CollageSlot(0.6f, 1/3f, 0.4f, 1/3f))
                list.add(CollageSlot(0.6f, 2/3f, 0.4f, 1/3f))
            }
        }
        6 -> {
            if (template == 0) { // 2 rows, 3 columns
                for (row in 0..1) {
                    for (col in 0..2) {
                        list.add(CollageSlot(col / 3f, row / 2f, 1/3f, 1/2f))
                    }
                }
            } else { // spotlight 1 top, 5 bottom
                list.add(CollageSlot(0f, 0f, 1.0f, 0.55f))
                for (col in 0..4) {
                    list.add(CollageSlot(col / 5f, 0.55f, 0.2f, 0.45f))
                }
            }
        }
        9 -> {
            if (template == 0) { // 3 rows, 3 col
                for (row in 0..2) {
                    for (col in 0..2) {
                        list.add(CollageSlot(col / 3f, row / 3f, 1/3f, 1/3f))
                    }
                }
            } else { // center spotlight, 8 surround
                val s = 1/3f
                list.add(CollageSlot(s, s, s, s)) // center at index 0
                list.add(CollageSlot(0f, 0f, s, s))
                list.add(CollageSlot(s, 0f, s, s))
                list.add(CollageSlot(s * 2, 0f, s, s))
                list.add(CollageSlot(0f, s, s, s))
                list.add(CollageSlot(s * 2, s, s, s))
                list.add(CollageSlot(0f, s * 2, s, s))
                list.add(CollageSlot(s, s * 2, s, s))
                list.add(CollageSlot(s * 2, s * 2, s, s))
            }
        }
        else -> {
            list.add(CollageSlot(0f, 0f, 1.0f, 1.0f))
        }
    }
    return list
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val localFiles by viewModel.localExportedFiles.collectAsStateWithLifecycle()

    // Refresh files on screen entering
    LaunchedEffect(Unit) {
        viewModel.refreshLocalExportedFiles(context)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("settings_back_btn")) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back to dashboard",
                        tint = Color(0xFF1D1B20)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings & Local Storage",
                    color = Color(0xFF1D1B20),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        containerColor = Color(0xFFFEF7FF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // About App Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("about_app_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF6750A4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Info, "Info Icon", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Picture Collage Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Version v1.0.4 • Official Stable Build",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "A professional design suite enabling you to customize layouts, linear color filter spectrums, and fine opacity watermarking. Saves all exports directly to external local storage safely.",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 16.sp
                    )
                }
            }

            // GitHub updates card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("github_updates_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "GITHUB INTERACTIVE UPDATES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check online repository status structure to align with security, stability, or visual updates.",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.isCheckingUpdates) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Querying GitHub releases catalog...", fontSize = 12.sp, color = Color(0xFF49454F))
                        }
                    } else {
                        viewModel.updateMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEADDFF))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, "Checked", tint = Color(0xFF21005D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { viewModel.checkForGithubUpdates() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("git_check_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check GitHub for Updates", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quality Options Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("quality_options_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SAVING & QUALITY PREFERENCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Compression
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Uncompressed High-Quality JPG", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                            Text(text = "Renders collage outputs at 95% ratio (default 75%)", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                        Switch(
                            checked = viewModel.highQualityExport,
                            onCheckedChange = { viewModel.highQualityExport = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4)
                            ),
                            modifier = Modifier.testTag("hq_export_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFCAC4D0), thickness = 0.5.dp)

                    // Grid startup
                    Text(text = "Default Startup Templates Size", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    Text(text = "Determine preferred number of starting canvas blocks:", fontSize = 11.sp, color = Color(0xFF49454F), modifier = Modifier.padding(bottom = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 4, 6, 9).forEach { num ->
                            val isSelected = viewModel.defaultGridSizeSetting == num
                            OutlinedButton(
                                onClick = {
                                    viewModel.defaultGridSizeSetting = num
                                    viewModel.gridLayoutSize = num
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color(0xFFEADDFF) else Color.Transparent,
                                    contentColor = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F)
                                ),
                                modifier = Modifier.weight(1f).testTag("default_grid_$num")
                            ) {
                                Text(text = "$num Blocks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Saved Files Section
            Text(
                text = "EXPORTED COLLAGES FILE EXPLORER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6750A4),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            if (localFiles.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_files_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Empty File Cabinet",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No saved collages found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F)
                        )
                        Text(
                            text = "Your exported high-resolution PDF or JPEG canvas layouts will be listed permanently in this local storage manager.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("saved_files_list")
                ) {
                    localFiles.forEach { file ->
                        val isPdf = file.name.endsWith(".pdf", ignoreCase = true)
                        val formattedSize = formatSizeInKB(file.length())

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("local_file_item_${file.name}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPdf) Color(0xFFF9DEDC) else Color(0xFFE8DEF8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPdf) Icons.Filled.Share /* Placeholder standard icon */ else Icons.Filled.Image,
                                        contentDescription = "File Type Type",
                                        tint = if (isPdf) Color(0xFF8C1D18) else Color(0xFF6750A4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$formattedSize • Storage Location",
                                        fontSize = 10.sp,
                                        color = Color(0xFF49454F)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Open file
                                    IconButton(
                                        onClick = {
                                            val fileUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                setDataAndType(fileUri, if (isPdf) "application/pdf" else "image/jpeg")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No app found to preview this style file.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("open_file_${file.name}")
                                    ) {
                                        Icon(Icons.Filled.Share, "Preview", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                                    }

                                    // Share (System Intents)
                                    IconButton(
                                        onClick = {
                                            val fileUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                setDataAndType(fileUri, if (isPdf) "application/pdf" else "image/jpeg")
                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Exported Collage"))
                                        },
                                        modifier = Modifier.size(32.dp).testTag("share_file_${file.name}")
                                    ) {
                                        Icon(Icons.Filled.Share, "Share", tint = Color(0xFF49454F), modifier = Modifier.size(16.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = {
                                            try {
                                                if (file.exists()) {
                                                    file.delete()
                                                    Toast.makeText(context, "Purged from storage cache!", Toast.LENGTH_SHORT).show()
                                                    viewModel.refreshLocalExportedFiles(context)
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Purge error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("delete_file_${file.name}")
                                    ) {
                                        Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFBA1A1A), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Inline format utility to avoid double declarations
private fun formatSizeInKB(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024f
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024f
    return String.format(Locale.US, "%.1f MB", mb)
}
