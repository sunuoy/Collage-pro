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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.scale
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
                    color = MaterialTheme.colorScheme.background
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
                    onSyncClick = { viewModel.showSyncDialog = true }
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

        // Interactive Cloud Sync Dialog
        if (viewModel.showSyncDialog) {
            CloudSyncDialog(viewModel = viewModel)
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
                    .background(MaterialTheme.colorScheme.background)
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
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Collage Pro Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("cloud_sync_btn")
                ) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Filled.Refresh else Icons.Filled.CloudDone,
                        contentDescription = "Cloud Status",
                        tint = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF0F823E)
                    )
                }
            }
        },
        bottomBar = {
            // Elegant studio trademark footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "© Collage Pro Studio • Local & Cloud Backup Ready",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Brand Title Header
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
            val backgroundColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .drawBehind {
                        // Drawing custom design studio background
                        val colorsList = listOf(surfaceVariantColor, backgroundColor)
                        drawRect(
                            brush = Brush.verticalGradient(colorsList),
                            size = size
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.06f),
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
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GridView,
                                contentDescription = "Studio Icon",
                                tint = MaterialTheme.colorScheme.onPrimary,
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
                                color = MaterialTheme.colorScheme.onBackground
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Cloud Sync Bar
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Filled.Refresh else Icons.Filled.Cloud,
                                contentDescription = "Cloud Status",
                                tint = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF0F823E),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${projects.size} items",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved projects yet",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Choose a layout above to assemble your first photography collage!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$size Grid",
                color = MaterialTheme.colorScheme.onSurface,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Synced indicator
                    Icon(
                        imageVector = if (project.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = "Sync State Indicator",
                        tint = if (project.isSynced) Color(0xFF0F823E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Layout: ${project.gridLayoutSize}-cell grid • Apply: ${project.filterName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "Saved: $dateString",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLoad) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Load layout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    .background(MaterialTheme.colorScheme.background)
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
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        BasicTextField(
                            value = viewModel.activeProjectName,
                            onValueChange = { viewModel.activeProjectName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
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
                            color = MaterialTheme.colorScheme.primary,
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
                            tint = MaterialTheme.colorScheme.onBackground
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
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

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
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "COLOUR",
                            color = if (viewModel.isColorOutput) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (viewModel.isColorOutput) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.isColorOutput = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "MONO B&W",
                            color = if (!viewModel.isColorOutput) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (!viewModel.isColorOutput) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.isColorOutput = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                // EXPORT & SHARE ACTIONS SEPARATED
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // DOWNLOAD SECTION
                    Text(
                        text = "LOCAL EXPORT (SAVE TO DEVICE)",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportAndShare("JPG", context, shareAfterExport = false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_jpg_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save JPG", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save JPG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.exportAndShare("PDF", context, shareAfterExport = false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Save PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Drafts,
                    contentDescription = "Draft Icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.generateDisplayFileName(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(4.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
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

            // Always-Visible Watermark Quick Status & Toggle Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (viewModel.showWatermark) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Watermark Visibility",
                        tint = if (viewModel.showWatermark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Collage Watermark: ${if (viewModel.showWatermark) "Visible" else "Hidden"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.showWatermark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\"${viewModel.watermarkText}\"",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                
                Switch(
                    checked = viewModel.showWatermark,
                    onCheckedChange = { viewModel.showWatermark = it },
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("main_watermark_toggle")
                )
            }

            // Guide instruction banner
            Text(
                text = "Long press cells to Swap easily • Tap cells to replace content",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Cell control settings if a slot was manually selected
            viewModel.selectedSlotIndex?.let { index ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Card", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adjust Slot #${index + 1} Media",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            // Reset Button
                            TextButton(
                                onClick = {
                                    viewModel.setZoomInSlot(index, 1f)
                                    viewModel.setPanXInSlot(index, 0f)
                                    viewModel.setPanYInSlot(index, 0f)
                                    viewModel.updateRotationInSlot(index, 0f)
                                }
                            ) {
                                Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.selectedSlotIndex = null }) {
                                Icon(Icons.Filled.Close, "Dismiss Action Bar", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    val zoomVal = viewModel.activeZooms.getOrElse(index) { 1f }
                    val panXVal = viewModel.activePanX.getOrElse(index) { 0f }
                    val panYVal = viewModel.activePanY.getOrElse(index) { 0f }
                    val rotVal = viewModel.activeRotations.getOrElse(index) { 0f }

                    // Slider 1: ZOOM SCALE
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ZoomIn, "Zoom scale", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Zoom & Scale", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(text = String.format("%.2fx", zoomVal), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = zoomVal,
                            onValueChange = { viewModel.setZoomInSlot(index, it) },
                            valueRange = 1f..4f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6750A4),
                                activeTrackColor = Color(0xFF6750A4)
                            ),
                            modifier = Modifier.height(30.dp).testTag("adjust_zoom_slider_$index")
                        )
                    }

                    // Slider 2: PAN HORIZONTAL (CROP)
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Horizontal Crop Offset (Pan X)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                            Text(text = String.format("%d%%", (panXVal * 100).toInt()), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6750A4))
                        }
                        Slider(
                            value = panXVal,
                            onValueChange = { viewModel.setPanXInSlot(index, it) },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(30.dp).testTag("adjust_panx_slider_$index")
                        )
                    }

                    // Slider 3: PAN VERTICAL (CROP)
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Vertical Crop Offset (Pan Y)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format("%d%%", (panYVal * 100).toInt()), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = panYVal,
                            onValueChange = { viewModel.setPanYInSlot(index, it) },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(30.dp).testTag("adjust_pany_slider_$index")
                        )
                    }

                    // Slider 4: SMOOTH ROTATION (0 to 360 degrees)
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.RotateRight, "Rotate index", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rotation Degrees", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(text = String.format("%.0f°", rotVal), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = rotVal,
                            onValueChange = { viewModel.updateRotationInSlot(index, it) },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(30.dp).testTag("adjust_rot_slider_$index")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                viewModel.setImageInSlot(index, viewModel.presetOptions[index % viewModel.presetOptions.size])
                                viewModel.setZoomInSlot(index, 1f)
                                viewModel.setPanXInSlot(index, 0f)
                                viewModel.setPanYInSlot(index, 0f)
                                viewModel.updateRotationInSlot(index, 0f)
                                viewModel.selectedSlotIndex = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Text("Reset Placeholder", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.selectedSlotIndex = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) {
                            Text("Done Adjusting", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STUDIO CONTROL TABS Navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
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
                    .background(MaterialTheme.colorScheme.background)
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
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
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
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        BorderStroke(
                            width = if (isSelected || isDragging) 3.dp else if (viewModel.showBorders) 1.dp else 0.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isDragging -> MaterialTheme.colorScheme.primary
                                else -> if (viewModel.showBorders) MaterialTheme.colorScheme.outline else Color.Transparent
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
                                val zoom = viewModel.activeZooms.getOrElse(index) { 1f }
                                scaleX = zoom
                                scaleY = zoom
                                val panXVal = viewModel.activePanX.getOrElse(index) { 0f }
                                val panYVal = viewModel.activePanY.getOrElse(index) { 0f }
                                translationX = panXVal * this.size.width
                                translationY = panYVal * this.size.height
                            }
                    )
                }

                // Small cell index tag label
                if (viewModel.showCellIndices) {
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
        if (viewModel.showWatermark && viewModel.watermarkText.isNotBlank()) {
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

data class TemplateOption(val index: Int, val name: String, val description: String)

fun getTemplateOptionsForSize(size: Int): List<TemplateOption> {
    return when (size) {
        2 -> listOf(
            TemplateOption(0, "Vertical Split", "50:50 vertical split"),
            TemplateOption(1, "Horizontal Split", "50:50 horizontal split"),
            TemplateOption(2, "Golden Vertical", "Asymmetric 1:2 vertical focal"),
            TemplateOption(3, "Golden Horizontal", "Asymmetric 1:2 horizontal focal")
        )
        4 -> listOf(
            TemplateOption(0, "Equal Grid", "Perfect 2x2 symmetry"),
            TemplateOption(1, "Spotlight Focus", "Dominant left vertical band"),
            TemplateOption(2, "Cinematic Strip", "Top wide cinema focus"),
            TemplateOption(3, "Vertical Pillars", "Four equal tall strips"),
            TemplateOption(4, "Pinwheel Spiral", "Rotational alignment")
        )
        6 -> listOf(
            TemplateOption(0, "Standard Blocks", "2 rows of 3 equal columns"),
            TemplateOption(1, "Banner Spotlight", "Top banner with bottom tiles"),
            TemplateOption(2, "Vertical Hero", "Left showcase with sidebar tiles"),
            TemplateOption(3, "Book Showcase", "3 rows of 2 equal columns")
        )
        9 -> listOf(
            TemplateOption(0, "Classic Matrix", "3x3 photography grid"),
            TemplateOption(1, "Center Spotlight", "Featured central block"),
            TemplateOption(2, "Column Pillars", "Prominent wide left band"),
            TemplateOption(3, "Epic Panorama", "Top giant focal band")
        )
        else -> listOf(
            TemplateOption(0, "Default Grid", "Standard layout")
        )
    }
}

@Composable
fun TemplateMiniPreview(size: Int, template: Int, isSelected: Boolean) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = Modifier
            .size(70.dp, 50.dp)
            .background(if (isSelected) primaryColor.copy(alpha = 0.12f) else surfaceColor)
            .border(1.dp, if (isSelected) primaryColor else outlineColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(3.dp)
    ) {
        val slots = remember(size, template) { getCollageSlots(size, template) }
        slots.forEach { slot ->
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction = 1f)
                    .align(Alignment.TopStart)
                    .offset(
                        x = (slot.leftFraction * 64f).dp,
                        y = (slot.topFraction * 44f).dp
                    )
                    .size(
                        width = (slot.widthFraction * 64f).dp,
                        height = (slot.heightFraction * 44f).dp
                    )
                    .padding(1.dp)
                    .background(
                        if (isSelected) primaryColor.copy(alpha = 0.6f) else outlineColor.copy(alpha = 0.4f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun GridsTabContent(viewModel: MainViewModel) {
    val currentSize = viewModel.gridLayoutSize
    val templates = remember(currentSize) { getTemplateOptionsForSize(currentSize) }

    Column {
        Text(
            text = "CHOOSE TEMPLATE STYLE",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            templates.forEach { option ->
                TemplateVariantCard(
                    name = option.name,
                    description = option.description,
                    size = currentSize,
                    template = option.index,
                    isSelected = viewModel.templateIndex == option.index,
                    onClick = { viewModel.templateIndex = option.index }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GRID LAYOUTS",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(2, 4, 6, 9).forEach { size ->
                OutlinedButton(
                    onClick = { viewModel.startNewProject(size) },
                    border = BorderStroke(
                        width = if (viewModel.gridLayoutSize == size) 2.dp else 1.dp,
                        color = if (viewModel.gridLayoutSize == size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (viewModel.gridLayoutSize == size) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (viewModel.gridLayoutSize == size) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_layout_$size")
                ) {
                    Text("$size Cells")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "DISPLAY PREFERENCES",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Borders", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Show divider separators between photos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = viewModel.showBorders,
                onCheckedChange = { viewModel.showBorders = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("show_borders_switch")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Cell Numbers", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Display index tags in preview editor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = viewModel.showCellIndices,
                onCheckedChange = { viewModel.showCellIndices = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("show_cell_indices_switch")
            )
        }
    }
}

@Composable
fun TemplateVariantCard(
    name: String,
    description: String,
    size: Int,
    template: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .height(130.dp)
            .clickable(onClick = onClick)
            .testTag("template_card_${size}_$template"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TemplateMiniPreview(size = size, template = template, isSelected = isSelected)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
            }
        }
    }
}


@Composable
fun FiltersTabContent(viewModel: MainViewModel) {
    val filters = listOf("Classic", "Cinema", "Warm", "Cool", "Sepia", "Monochrome", "Vintage")
    Column {
        Text(text = "LIVE RETRO FILTER PRESETS", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = filter,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "PROFESSIONAL WATERMARK", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
                Switch(
                    checked = viewModel.showWatermark,
                    onCheckedChange = { viewModel.showWatermark = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("show_watermark_switch")
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.watermarkText,
            onValueChange = { viewModel.watermarkText = it },
            enabled = viewModel.showWatermark,
            label = { Text("Watermark branding text", color = if (viewModel.showWatermark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("watermark_text_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Opacity slider
        Text(
            text = "Opacity: ${(viewModel.watermarkOpacity * 100).toInt()}%",
            color = if (viewModel.showWatermark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            fontSize = 12.sp
        )
        Slider(
            value = viewModel.watermarkOpacity,
            onValueChange = { viewModel.watermarkOpacity = it },
            enabled = viewModel.showWatermark,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                disabledThumbColor = MaterialTheme.colorScheme.outline,
                disabledActiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.testTag("watermark_opacity_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Badge selectors
        Text(
            text = "Select Branding Hue:",
            color = if (viewModel.showWatermark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("White", "Black", "Gold", "Accent").forEach { color ->
                val isSelected = viewModel.watermarkColorName == color
                val background = when {
                    !viewModel.showWatermark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    !viewModel.showWatermark -> MaterialTheme.colorScheme.outline
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = color,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(background)
                        .clickable(enabled = viewModel.showWatermark) { viewModel.watermarkColorName = color }
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
        Text(text = "BATCH MULTI-CELL ACTIONS", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(text = "Apply rapid changes to streamline your creation process instantly:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
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
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = vector, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
            when (template) {
                0 -> { // Vertical halves
                    list.add(CollageSlot(0f, 0f, 0.5f, 1.0f))
                    list.add(CollageSlot(0.5f, 0f, 0.5f, 1.0f))
                }
                1 -> { // Horizontal halves
                    list.add(CollageSlot(0f, 0f, 1.0f, 0.5f))
                    list.add(CollageSlot(0f, 0.5f, 1.0f, 0.5f))
                }
                2 -> { // Left 1/3, Right 2/3
                    list.add(CollageSlot(0f, 0f, 1/3f, 1.0f))
                    list.add(CollageSlot(1/3f, 0f, 2/3f, 1.0f))
                }
                3 -> { // Top 1/3, Bottom 2/3
                    list.add(CollageSlot(0f, 0f, 1.0f, 1/3f))
                    list.add(CollageSlot(0f, 1/3f, 1.0f, 2/3f))
                }
                else -> {
                    list.add(CollageSlot(0f, 0f, 0.5f, 1.0f))
                    list.add(CollageSlot(0.5f, 0f, 0.5f, 1.0f))
                }
            }
        }
        4 -> {
            when (template) {
                0 -> { // 2x2 standard
                    list.add(CollageSlot(0f, 0f, 0.5f, 0.5f))
                    list.add(CollageSlot(0.5f, 0f, 0.5f, 0.5f))
                    list.add(CollageSlot(0f, 0.5f, 0.5f, 0.5f))
                    list.add(CollageSlot(0.5f, 0.5f, 0.5f, 0.5f))
                }
                1 -> { // Asymmetric focus
                    list.add(CollageSlot(0f, 0f, 0.6f, 1.0f))
                    list.add(CollageSlot(0.6f, 0f, 0.4f, 1/3f))
                    list.add(CollageSlot(0.6f, 1/3f, 0.4f, 1/3f))
                    list.add(CollageSlot(0.6f, 2/3f, 0.4f, 1/3f))
                }
                2 -> { // Cinematic Strip (Top Hero, 3 bottom)
                    list.add(CollageSlot(0f, 0f, 1.0f, 0.6f))
                    list.add(CollageSlot(0f, 0.6f, 1/3f, 0.4f))
                    list.add(CollageSlot(1/3f, 0.6f, 1/3f, 0.4f))
                    list.add(CollageSlot(2/3f, 0.6f, 1/3f, 0.4f))
                }
                3 -> { // 4 Vertical Pillars
                    list.add(CollageSlot(0f, 0f, 0.25f, 1.0f))
                    list.add(CollageSlot(0.25f, 0f, 0.25f, 1.0f))
                    list.add(CollageSlot(0.5f, 0f, 0.25f, 1.0f))
                    list.add(CollageSlot(0.75f, 0f, 0.25f, 1.0f))
                }
                4 -> { // Spiral
                    list.add(CollageSlot(0f, 0f, 0.5f, 0.5f))
                    list.add(CollageSlot(0.5f, 0f, 0.5f, 0.4f))
                    list.add(CollageSlot(0.6f, 0.4f, 0.4f, 0.6f))
                    list.add(CollageSlot(0f, 0.5f, 0.6f, 0.5f))
                }
                else -> {
                    list.add(CollageSlot(0f, 0f, 0.5f, 0.5f))
                    list.add(CollageSlot(0.5f, 0f, 0.5f, 0.5f))
                    list.add(CollageSlot(0f, 0.5f, 0.5f, 0.5f))
                    list.add(CollageSlot(0.5f, 0.5f, 0.5f, 0.5f))
                }
            }
        }
        6 -> {
            when (template) {
                0 -> { // 2 rows, 3 columns
                    for (row in 0..1) {
                        for (col in 0..2) {
                            list.add(CollageSlot(col / 3f, row / 2f, 1/3f, 1/2f))
                        }
                    }
                }
                1 -> { // spotlight 1 top, 5 bottom
                    list.add(CollageSlot(0f, 0f, 1.0f, 0.55f))
                    for (col in 0..4) {
                        list.add(CollageSlot(col / 5f, 0.55f, 0.2f, 0.45f))
                    }
                }
                2 -> { // Vertical Hero Left
                    list.add(CollageSlot(0f, 0f, 0.55f, 1.0f))
                    for (row in 0..4) {
                        list.add(CollageSlot(0.55f, row * 0.2f, 0.45f, 0.2f))
                    }
                }
                3 -> { // 3 rows, 2 columns
                    for (row in 0..2) {
                        for (col in 0..1) {
                            list.add(CollageSlot(col / 2f, row / 3f, 0.5f, 1/3f))
                        }
                    }
                }
                else -> {
                    for (row in 0..1) {
                        for (col in 0..2) {
                            list.add(CollageSlot(col / 3f, row / 2f, 1/3f, 1/2f))
                        }
                    }
                }
            }
        }
        9 -> {
            when (template) {
                0 -> { // 3 rows, 3 col
                    for (row in 0..2) {
                        for (col in 0..2) {
                            list.add(CollageSlot(col / 3f, row / 3f, 1/3f, 1/3f))
                        }
                    }
                }
                1 -> { // center spotlight, 8 surround
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
                2 -> { // Column showcase
                    for (row in 0..2) {
                        list.add(CollageSlot(0f, row / 3f, 0.5f, 1/3f))
                    }
                    for (row in 0..2) {
                        list.add(CollageSlot(0.5f, row / 3f, 0.25f, 1/3f))
                    }
                    for (row in 0..2) {
                        list.add(CollageSlot(0.75f, row / 3f, 0.25f, 1/3f))
                    }
                }
                3 -> { // Giant top hero, 8 below
                    list.add(CollageSlot(0f, 0f, 1.0f, 0.5f))
                    for (col in 0..3) {
                        list.add(CollageSlot(col * 0.25f, 0.5f, 0.25f, 0.25f))
                    }
                    for (col in 0..3) {
                        list.add(CollageSlot(col * 0.25f, 0.75f, 0.25f, 0.25f))
                    }
                }
                else -> {
                    for (row in 0..2) {
                        for (col in 0..2) {
                            list.add(CollageSlot(col / 3f, row / 3f, 1/3f, 1/3f))
                        }
                    }
                }
            }
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
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("settings_back_btn")) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back to dashboard",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings & Local Storage",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Info, "Info Icon", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Picture Collage Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Version v1.1.0 • Major Layout Update",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "A professional design suite enabling you to customize layouts, linear color filter spectrums, and fine opacity watermarking. Saves all exports directly to external local storage safely.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // GitHub updates card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("github_updates_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "GITHUB INTERACTIVE UPDATES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check online repository status structure to align with security, stability, or visual updates.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.isCheckingUpdates) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Querying GitHub releases catalog...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        viewModel.updateMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, "Checked", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { viewModel.checkForGithubUpdates() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("git_check_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SAVING & QUALITY PREFERENCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Compression
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Uncompressed High-Quality JPG", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Renders collage outputs at 95% ratio (default 75%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = viewModel.highQualityExport,
                            onCheckedChange = { viewModel.highQualityExport = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("hq_export_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

                    // Grid startup
                    Text(text = "Default Startup Templates Size", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Determine preferred number of starting canvas blocks:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

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
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            if (localFiles.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_files_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Empty File Cabinet",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No saved collages found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Your exported high-resolution PDF or JPEG canvas layouts will be listed permanently in this local storage manager.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPdf) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPdf) Icons.Filled.Share /* Placeholder standard icon */ else Icons.Filled.Image,
                                        contentDescription = "File Type Type",
                                        tint = if (isPdf) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$formattedSize • Storage Location",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        Icon(Icons.Filled.Share, "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                        Icon(Icons.Filled.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
                                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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

@Composable
fun CloudSyncDialog(viewModel: MainViewModel) {
    val syncProgressText by viewModel.syncProgressText.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var tempCode by remember { mutableStateOf(viewModel.syncCode) }
    
    AlertDialog(
        onDismissRequest = { if (!isSyncing) viewModel.showSyncDialog = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cross-Device Cloud Sync")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Synchronize your collage projects dynamically across all your devices using a shared secure Profile Key.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = tempCode,
                    onValueChange = { 
                        if (!isSyncing) {
                            tempCode = it.trim().uppercase(Locale.US)
                        }
                    },
                    label = { Text("Profile Sync Code") },
                    placeholder = { Text("e.g. MY-COLLAGE-SYNC") },
                    singleLine = true,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth().testTag("sync_code_input")
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tip: Enter the exact same Sync Code on another device to merge and sync your creations seamlessly!",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                if (isSyncing || syncProgressText != "Ready to synchronize") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = syncProgressText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateSyncCode(tempCode)
                    viewModel.syncProjectsToCloud()
                },
                enabled = !isSyncing && tempCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("sync_now_btn")
            ) {
                Text("Sync Now")
            }
        },
        dismissButton = {
            if (!isSyncing) {
                TextButton(
                    onClick = { viewModel.showSyncDialog = false },
                    modifier = Modifier.testTag("sync_cancel_btn")
                ) {
                    Text("Close")
                }
            }
        }
    )
}
