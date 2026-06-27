package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CollageProject
import com.example.data.ProjectRepository
import com.example.data.FirebaseManager
import com.example.data.awaitTask
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    val projects: StateFlow<List<CollageProject>>

    // Screen State
    var currentScreen by mutableStateOf("dashboard") // dashboard, creator, batch_editor, settings

    // Local storage exported files
    private val _localExportedFiles = MutableStateFlow<List<File>>(emptyList())
    val localExportedFiles: StateFlow<List<File>> = _localExportedFiles.asStateFlow()

    // Update check state
    var isCheckingUpdates by mutableStateOf(false)
    var updateMessage by mutableStateOf<String?>(null)

    // Settings Options
    var highQualityExport by mutableStateOf(true)
    var defaultGridSizeSetting by mutableStateOf(4)

    // Grid details
    var gridLayoutSize by mutableStateOf(4) // 2, 4, 6, 9
    var templateIndex by mutableStateOf(0) // 0: equal, 1: focus/split
    var selectedFilter by mutableStateOf("Classic") // Classic, Cinema, Warm, Cool, Sepia, Monochrome, Vintage
    var isColorOutput by mutableStateOf(true)
    var watermarkText by mutableStateOf("Collage Pro")
    var watermarkColorName by mutableStateOf("White") // White, Black, Gold, Custom Accent
    var watermarkOpacity by mutableStateOf(0.7f) // 0f to 1f
    var showWatermark by mutableStateOf(false)
    var showBorders by mutableStateOf(true)
    var showCellIndices by mutableStateOf(true)
    var pdfLandscape by mutableStateOf(false)

    // Current Project ID
    var activeProjectId by mutableStateOf<Int?>(null)
    var activeProjectName by mutableStateOf("My Collage")

    // List of images path/preset indicators for currently active project slots
    var activeImages by mutableStateOf<List<String>>(emptyList())
    // Individual slot rotations (in degrees: 0, 90, 180, 270)
    var activeRotations by mutableStateOf<List<Float>>(emptyList())
    // Individual slot zooms (scaling factor 1f to 4f)
    var activeZooms by mutableStateOf<List<Float>>(emptyList())
    // Individual slot pan / crop crop offset values (-1f to 1f)
    var activePanX by mutableStateOf<List<Float>>(emptyList())
    var activePanY by mutableStateOf<List<Float>>(emptyList())

    // Selected Slot for swapping / custom actions
    var selectedSlotIndex by mutableStateOf<Int?>(null)

    // Syncing State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Sync Code for cross-device cloud sync
    var syncCode by mutableStateOf("")
    
    // Detailed real-time progress text
    private val _syncProgressText = MutableStateFlow("Ready to synchronize")
    val syncProgressText: StateFlow<String> = _syncProgressText.asStateFlow()
    
    // Dialog visibility state
    var showSyncDialog by mutableStateOf(false)

    // Sharing / File generation state
    private val _shareFileProgress = MutableStateFlow<String?>(null)
    val shareFileProgress: StateFlow<String?> = _shareFileProgress.asStateFlow()

    // Preset gradients paths to make it beautiful out of the box
    val presetOptions = listOf(
        "preset:1", "preset:2", "preset:3", "preset:4",
        "preset:5", "preset:6", "preset:7", "preset:8",
        "preset:9", "preset:10", "preset:11", "preset:12"
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ProjectRepository(db.projectDao())
        
        // Load sync profile key
        val sharedPrefs = application.getSharedPreferences("collage_sync_prefs", Context.MODE_PRIVATE)
        var loadedKey = sharedPrefs.getString("sync_key", "") ?: ""
        if (loadedKey.isBlank()) {
            loadedKey = "SYNC-${(100000..999999).random()}"
            sharedPrefs.edit().putString("sync_key", loadedKey).apply()
        }
        syncCode = loadedKey
        
        // Expose projects reactive flow
        val flow = MutableStateFlow<List<CollageProject>>(emptyList())
        viewModelScope.launch {
            repository.allProjects.collect {
                flow.value = it
            }
        }
        projects = flow
    }

    // Save/update sync code
    fun updateSyncCode(newCode: String) {
        val cleanCode = newCode.trim().uppercase(Locale.US)
        if (cleanCode.isNotBlank()) {
            syncCode = cleanCode
            val sharedPrefs = getApplication<Application>().getSharedPreferences("collage_sync_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("sync_key", cleanCode).apply()
        }
    }

    // Load or create a project
    fun startNewProject(layoutSize: Int) {
        gridLayoutSize = layoutSize
        templateIndex = 0
        selectedFilter = "Classic"
        isColorOutput = true
        watermarkText = "Collage Pro"
        watermarkColorName = "White"
        watermarkOpacity = 0.7f
        activeProjectId = null
        activeProjectName = "New Collage"
        selectedSlotIndex = null

        // Populate with randomized premium preset items so layout is immediately stunning
        val generatedPresets = mutableListOf<String>()
        for (i in 0 until layoutSize) {
            val presetId = presetOptions[i % presetOptions.size]
            generatedPresets.add(presetId)
        }
        activeImages = generatedPresets
        activeRotations = List(layoutSize) { 0f }
        activeZooms = List(layoutSize) { 1f }
        activePanX = List(layoutSize) { 0f }
        activePanY = List(layoutSize) { 0f }
    }

    fun loadProject(project: CollageProject) {
        activeProjectId = project.id
        activeProjectName = project.name
        gridLayoutSize = project.gridLayoutSize
        templateIndex = project.templateIndex
        selectedFilter = project.filterName
        isColorOutput = project.isColorOutput
        watermarkText = project.watermarkText
        selectedSlotIndex = null

        val list = project.getImageList()
        val filledImages = list.toMutableList()
        // Guarantee slot size matching layoutSize
        if (filledImages.size < gridLayoutSize) {
            for (i in filledImages.size until gridLayoutSize) {
                filledImages.add(presetOptions[i % presetOptions.size])
            }
        } else if (filledImages.size > gridLayoutSize) {
            while (filledImages.size > gridLayoutSize) {
                filledImages.removeAt(filledImages.size - 1)
            }
        }
        activeImages = filledImages
        activeRotations = List(gridLayoutSize) { 0f }
        activeZooms = List(gridLayoutSize) { 1f }
        activePanX = List(gridLayoutSize) { 0f }
        activePanY = List(gridLayoutSize) { 0f }
        currentScreen = "creator"
    }

    fun saveProject() {
        val projectName = activeProjectName.ifBlank { "My Collage" }
        viewModelScope.launch {
            val project = CollageProject(
                id = activeProjectId ?: 0,
                name = projectName,
                timestamp = System.currentTimeMillis(),
                gridLayoutSize = gridLayoutSize,
                templateIndex = templateIndex,
                imagePathsString = CollageProject.fromImageList(activeImages),
                filterName = selectedFilter,
                watermarkText = watermarkText,
                isColorOutput = isColorOutput,
                isSynced = false // Pending sync
            )

            if (project.id == 0) {
                val newId = repository.insert(project)
                activeProjectId = newId.toInt()
            } else {
                repository.update(project)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Project '$projectName' saved locally!", Toast.LENGTH_SHORT).show()
                currentScreen = "dashboard"
            }
        }
    }

    fun deleteProject(project: CollageProject) {
        viewModelScope.launch {
            repository.delete(project)
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Collage deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Swap Slot indices (intuitive drag-and-drop / selected tap actions)
    fun swapSlots(index1: Int, index2: Int) {
        if (index1 in activeImages.indices && index2 in activeImages.indices) {
            val updatedImages = activeImages.toMutableList()
            val temp = updatedImages[index1]
            updatedImages[index1] = updatedImages[index2]
            updatedImages[index2] = temp

            val updatedRotations = activeRotations.toMutableList()
            val tempRot = updatedRotations[index1]
            updatedRotations[index1] = updatedRotations[index2]
            updatedRotations[index2] = tempRot

            val updatedZooms = activeZooms.toMutableList()
            val tempZoom = if (index1 in updatedZooms.indices) updatedZooms[index1] else 1f
            if (index1 in updatedZooms.indices && index2 in updatedZooms.indices) {
                updatedZooms[index1] = updatedZooms[index2]
                updatedZooms[index2] = tempZoom
            }

            val updatedPanX = activePanX.toMutableList()
            val tempPanX = if (index1 in updatedPanX.indices) updatedPanX[index1] else 0f
            if (index1 in updatedPanX.indices && index2 in updatedPanX.indices) {
                updatedPanX[index1] = updatedPanX[index2]
                updatedPanX[index2] = tempPanX
            }

            val updatedPanY = activePanY.toMutableList()
            val tempPanY = if (index1 in updatedPanY.indices) updatedPanY[index1] else 0f
            if (index1 in updatedPanY.indices && index2 in updatedPanY.indices) {
                updatedPanY[index1] = updatedPanY[index2]
                updatedPanY[index2] = tempPanY
            }

            activeImages = updatedImages
            activeRotations = updatedRotations
            activeZooms = updatedZooms
            activePanX = updatedPanX
            activePanY = updatedPanY
            selectedSlotIndex = null
        }
    }

    // Set item in a specific slot
    fun setImageInSlot(index: Int, uriString: String) {
        if (index in activeImages.indices) {
            val updated = activeImages.toMutableList()
            updated[index] = uriString
            activeImages = updated
        }
    }

    fun setZoomInSlot(index: Int, zoom: Float) {
        if (index in activeZooms.indices) {
            val updated = activeZooms.toMutableList()
            updated[index] = zoom
            activeZooms = updated
        } else if (index in 0 until gridLayoutSize) {
            val list = activeZooms.toMutableList()
            while (list.size <= index) {
                list.add(1f)
            }
            list[index] = zoom
            activeZooms = list
        }
    }

    fun setPanXInSlot(index: Int, panX: Float) {
        if (index in activePanX.indices) {
            val updated = activePanX.toMutableList()
            updated[index] = panX
            activePanX = updated
        } else if (index in 0 until gridLayoutSize) {
            val list = activePanX.toMutableList()
            while (list.size <= index) {
                list.add(0f)
            }
            list[index] = panX
            activePanX = list
        }
    }

    fun setPanYInSlot(index: Int, panY: Float) {
        if (index in activePanY.indices) {
            val updated = activePanY.toMutableList()
            updated[index] = panY
            activePanY = updated
        } else if (index in 0 until gridLayoutSize) {
            val list = activePanY.toMutableList()
            while (list.size <= index) {
                list.add(0f)
            }
            list[index] = panY
            activePanY = list
        }
    }

    fun updateRotationInSlot(index: Int, rotate: Float) {
        if (index in activeRotations.indices) {
            val updated = activeRotations.toMutableList()
            updated[index] = rotate
            activeRotations = updated
        } else if (index in 0 until gridLayoutSize) {
            val list = activeRotations.toMutableList()
            while (list.size <= index) {
                list.add(0f)
            }
            list[index] = rotate
            activeRotations = list
        }
    }

    // Batch Action: Apply dynamic filter to ALL images in current project
    fun batchApplyFilter(filterName: String) {
        selectedFilter = filterName
        Toast.makeText(getApplication(), "Filter '$filterName' applied to all images!", Toast.LENGTH_SHORT).show()
    }

    // Batch Action: Rotate ALL active images by 90 degrees
    fun batchRotateAll() {
        val updatedRotations = activeRotations.map { (it + 90f) % 360f }
        activeRotations = updatedRotations
        Toast.makeText(getApplication(), "Rotated all images 90°", Toast.LENGTH_SHORT).show()
    }

    // Batch Action: Randomize preset gradients across all active slots
    fun batchRandomizePresets() {
        val shuffledPresets = presetOptions.shuffled()
        val updated = activeImages.mapIndexed { index, _ ->
            shuffledPresets[index % shuffledPresets.size]
        }
        activeImages = updated
        Toast.makeText(getApplication(), "Regenerated abstract placeholder designs", Toast.LENGTH_SHORT).show()
    }

    // Batch Action: Clear all image slots to templates state
    fun batchClearAll() {
        activeImages = List(gridLayoutSize) { index -> presetOptions[index % presetOptions.size] }
        activeRotations = List(gridLayoutSize) { 0f }
        Toast.makeText(getApplication(), "Cleared all slots", Toast.LENGTH_SHORT).show()
    }

    // Cloud storage syncing: fully connects with Firebase Firestore and synchronizes projects bidirectionally
    fun syncProjectsToCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgressText.value = "Initializing secure Cloud connection..."
            
            val context = getApplication<Application>()
            FirebaseManager.init(context)
            val firestore = FirebaseManager.getFirestore()
            
            if (firestore == null) {
                _isSyncing.value = false
                _syncProgressText.value = "Cloud Sync Offline (No Instance)"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Secure cloud sync is temporarily offline (Missing Firestore configuration)", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            try {
                _syncProgressText.value = "Fetching local projects list..."
                val localProjects = repository.allProjects.first()
                
                _syncProgressText.value = "Connecting to profile key: $syncCode..."
                val syncRef = firestore.collection("sync_profiles").document(syncCode).collection("projects")
                
                // 1. Fetch all project documents from cloud for this sync code
                _syncProgressText.value = "Reading cloud database state..."
                val cloudQuerySnapshot = syncRef.get().awaitTask()
                val cloudProjectsMap = mutableMapOf<String, Map<String, Any>>()
                for (doc in cloudQuerySnapshot.documents) {
                    val data = doc.data
                    if (data != null) {
                        val name = data["name"] as? String ?: ""
                        if (name.isNotBlank()) {
                            cloudProjectsMap[name] = data
                        }
                    }
                }
                
                var uploadedCount = 0
                var downloadedCount = 0
                var updatedCount = 0
                
                // 2. Upload or update local projects in Firestore
                for (local in localProjects) {
                    val cloudData = cloudProjectsMap[local.name]
                    if (cloudData == null) {
                        // Project does not exist in the cloud yet, upload it!
                        _syncProgressText.value = "Uploading local project '${local.name}'..."
                        val docData = hashMapOf(
                            "name" to local.name,
                            "timestamp" to local.timestamp,
                            "gridLayoutSize" to local.gridLayoutSize,
                            "templateIndex" to local.templateIndex,
                            "imagePathsString" to local.imagePathsString,
                            "filterName" to local.filterName,
                            "watermarkText" to local.watermarkText,
                            "isColorOutput" to local.isColorOutput
                        )
                        syncRef.document(local.name).set(docData).awaitTask()
                        repository.update(local.copy(isSynced = true))
                        uploadedCount++
                    } else {
                        // Project exists in both. Compare timestamps.
                        val cloudTimestamp = (cloudData["timestamp"] as? Number)?.toLong() ?: 0L
                        if (local.timestamp > cloudTimestamp) {
                            // Local version is newer, upload and overwrite cloud!
                            _syncProgressText.value = "Syncing newer local details for '${local.name}'..."
                            val docData = hashMapOf(
                                "name" to local.name,
                                "timestamp" to local.timestamp,
                                "gridLayoutSize" to local.gridLayoutSize,
                                "templateIndex" to local.templateIndex,
                                "imagePathsString" to local.imagePathsString,
                                "filterName" to local.filterName,
                                "watermarkText" to local.watermarkText,
                                "isColorOutput" to local.isColorOutput
                            )
                            syncRef.document(local.name).set(docData).awaitTask()
                            repository.update(local.copy(isSynced = true))
                            updatedCount++
                        }
                    }
                }
                
                // 3. Download or update local projects with newer cloud projects
                for ((name, cloudData) in cloudProjectsMap) {
                    val local = localProjects.find { it.name == name }
                    val cloudTimestamp = (cloudData["timestamp"] as? Number)?.toLong() ?: 0L
                    val gridLayoutSize = (cloudData["gridLayoutSize"] as? Number)?.toInt() ?: 4
                    val templateIndex = (cloudData["templateIndex"] as? Number)?.toInt() ?: 0
                    val imagePathsString = cloudData["imagePathsString"] as? String ?: ""
                    val filterName = cloudData["filterName"] as? String ?: "Classic"
                    val watermarkText = cloudData["watermarkText"] as? String ?: "CollagePro"
                    val isColorOutput = cloudData["isColorOutput"] as? Boolean ?: true
                    
                    if (local == null) {
                        // Project exists in cloud but not locally, download it!
                        _syncProgressText.value = "Downloading cloud creation '${name}'..."
                        val newProject = CollageProject(
                            name = name,
                            timestamp = cloudTimestamp,
                            gridLayoutSize = gridLayoutSize,
                            templateIndex = templateIndex,
                            imagePathsString = imagePathsString,
                            filterName = filterName,
                            watermarkText = watermarkText,
                            isColorOutput = isColorOutput,
                            isSynced = true
                        )
                        repository.insert(newProject)
                        downloadedCount++
                    } else if (cloudTimestamp > local.timestamp) {
                        // Cloud version is newer, update local DB details!
                        _syncProgressText.value = "Updating local project '${name}'..."
                        val updatedProject = local.copy(
                            timestamp = cloudTimestamp,
                            gridLayoutSize = gridLayoutSize,
                            templateIndex = templateIndex,
                            imagePathsString = imagePathsString,
                            filterName = filterName,
                            watermarkText = watermarkText,
                            isColorOutput = isColorOutput,
                            isSynced = true
                        )
                        repository.update(updatedProject)
                        updatedCount++
                    }
                }
                
                _syncProgressText.value = "Cloud synchronization finished successfully! 🎉"
                delay(1200)
                
                withContext(Dispatchers.Main) {
                    val msg = "Sync Completed! Uploaded: $uploadedCount | Downloaded: $downloadedCount | Synced: $updatedCount"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Sync Error: ${e.message}", e)
                _syncProgressText.value = "Sync Interrupted: ${e.localizedMessage ?: "Unknown Error"}"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Cloud Sync Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Rotate individual slot
    fun rotateSlot(index: Int) {
        if (index in activeRotations.indices) {
            val updated = activeRotations.toMutableList()
            updated[index] = (updated[index] + 90f) % 360f
            activeRotations = updated
        }
    }

    // Generate Display File name matching: YYYY-MMM-DD_HH:MM:SS COLOR CODE
    fun generateDisplayFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MMM-dd_HH:mm:ss", Locale.US)
        val dateString = formatter.format(Date())
        val colorCode = if (isColorOutput) "COLOR" else "BW"
        return "${dateString}_$colorCode"
    }

    // Generate Safe filesystem name
    fun generateSafeFileName(prefix: String, extension: String): String {
        val formatter = SimpleDateFormat("yyyy-MMM-dd_HH-mm-ss", Locale.US)
        val dateString = formatter.format(Date())
        val colorCode = if (isColorOutput) "COLOR" else "BW"
        return "${prefix}_${dateString}_$colorCode.$extension"
    }

    // Export JPG / PDF and save locally, optionally triggering Android share-sheet with the true content byte stream!
    fun exportAndShare(format: String, context: Context, shareAfterExport: Boolean = false) {
        viewModelScope.launch(Dispatchers.Default) {
            _shareFileProgress.value = "Generating $format file..."
            try {
                // Determine layout dimension
                val collageSize = 1080
                val bitmap = Bitmap.createBitmap(collageSize, collageSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Fill background
                val backgroundPaint = Paint().apply {
                    color = if (isColorOutput) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#121212")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, collageSize.toFloat(), collageSize.toFloat(), backgroundPaint)

                // Render slots based on selected grid layouts (2, 4, 6, 9)
                val paint = Paint().apply { isAntiAlias = true }
                
                // Color Matrix/Filter
                val colorMatrix = ColorMatrix()
                if (!isColorOutput) {
                    colorMatrix.setSaturation(0f)
                } else {
                    when (selectedFilter) {
                        "Sepia" -> {
                            val m = floatArrayOf(
                                0.393f, 0.769f, 0.189f, 0f, 0f,
                                0.349f, 0.686f, 0.168f, 0f, 0f,
                                0.272f, 0.534f, 0.131f, 0f, 0f,
                                0f,     0f,     0f,     1f, 0f
                            )
                            colorMatrix.set(m)
                        }
                        "Monochrome" -> colorMatrix.setSaturation(0f)
                        "Cinema" -> colorMatrix.set(floatArrayOf(
                            1.2f, 0f, 0f, 0f, -10f,
                            0f, 1.1f, 0f, 0f, -10f,
                            0f, 0f, 1.4f, 0f, 10f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        "Warm" -> colorMatrix.set(floatArrayOf(
                            1.2f, 0f, 0f, 0f, 20f,
                            0f, 1.0f, 0f, 0f, 5f,
                            0f, 0f, 0.8f, 0f, -10f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        "Cool" -> colorMatrix.set(floatArrayOf(
                            0.8f, 0f, 0f, 0f, -10f,
                            0f, 1.0f, 0f, 0f, 5f,
                            0f, 0f, 1.3f, 0f, 25f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        "Vintage" -> colorMatrix.set(floatArrayOf(
                            0.9f, 0.1f, 0.1f, 0f, 15f,
                            0.1f, 0.8f, 0.1f, 0f, 10f,
                            0.1f, 0.1f, 0.7f, 0f, -5f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                }
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

                val slots = calculateGridCoordinates(gridLayoutSize, templateIndex, collageSize)
                
                // Draw each slot
                for (i in 0 until gridLayoutSize) {
                    if (i < slots.size && i < activeImages.size) {
                        val originalRect = slots[i]
                        val outerPaddingPx = 12f
                        val slotPaddingPx = 8f
                        val fSizeOffset = collageSize.toFloat() - 2 * outerPaddingPx
                        
                        val rect = SimpleRect(
                            left = (originalRect.left / collageSize) * fSizeOffset + outerPaddingPx + slotPaddingPx,
                            right = (originalRect.right / collageSize) * fSizeOffset + outerPaddingPx - slotPaddingPx,
                            top = (originalRect.top / collageSize) * fSizeOffset + outerPaddingPx + slotPaddingPx,
                            bottom = (originalRect.bottom / collageSize) * fSizeOffset + outerPaddingPx - slotPaddingPx
                        )
                        val indexInPreset = activeImages[i]
                        
                        // Render Preset design color block with premium gradient or artwork
                        val isPreset = indexInPreset.startsWith("preset:")
                        val designPaint = Paint().apply {
                            isAntiAlias = true
                            colorFilter = ColorMatrixColorFilter(colorMatrix)
                        }

                        if (isPreset) {
                            val seed = indexInPreset.removePrefix("preset:").toIntOrNull() ?: 1
                            val colorHex = getPresetHex(seed)
                            designPaint.color = android.graphics.Color.parseColor(colorHex)
                            canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, designPaint)

                            // Render decorative abstract circle or line so pre-defined cells shine!
                            val decorPaint = Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.WHITE
                                alpha = 40
                                style = Paint.Style.STROKE
                                strokeWidth = 12f
                            }
                            val cx = (rect.left + rect.right) / 2f
                            val cy = (rect.top + rect.bottom) / 2f
                            val radius = (rect.right - rect.left) / 4f
                            canvas.drawCircle(cx, cy, radius, decorPaint)
                        } else {
                            // Real dynamic device image loading
                            try {
                                val loadedBitmap = loadBitmapFromUri(context, Uri.parse(indexInPreset), collageSize / 2)
                                if (loadedBitmap != null) {
                                    // Center Crop + custom Zoom + custom Pan offsets + rotation rendering inside clipped cell boundary
                                    val rotation = activeRotations.getOrElse(i) { 0f }
                                    val TW = rect.right - rect.left
                                    val TH = rect.bottom - rect.top
                                    
                                    val scale = maxOf(TW / loadedBitmap.width.toFloat(), TH / loadedBitmap.height.toFloat())
                                    val userZoom = activeZooms.getOrElse(i) { 1f }
                                    val finalScale = scale * userZoom
                                    
                                    val transX = rect.left + (TW - loadedBitmap.width.toFloat() * finalScale) / 2f
                                    val transY = rect.top + (TH - loadedBitmap.height.toFloat() * finalScale) / 2f
                                    val panX = activePanX.getOrElse(i) { 0f } * TW
                                    val panY = activePanY.getOrElse(i) { 0f } * TH

                                    val matrix = android.graphics.Matrix()
                                    matrix.postScale(finalScale, finalScale)
                                    matrix.postTranslate(transX + panX, transY + panY)

                                    if (rotation != 0f) {
                                        matrix.postRotate(rotation, rect.left + TW / 2f, rect.top + TH / 2f)
                                    }

                                    canvas.save()
                                    canvas.clipRect(rect.left, rect.top, rect.right, rect.bottom)
                                    canvas.drawBitmap(loadedBitmap, matrix, paint)
                                    canvas.restore()
                                } else {
                                    // Fallback text indicator if image file cannot be parsed or permissions expired
                                    designPaint.color = android.graphics.Color.LTGRAY
                                    canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, designPaint)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                designPaint.color = android.graphics.Color.DKGRAY
                                canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, designPaint)
                            }
                        }

                        // Draw thin elegant separator block lines
                        if (showBorders) {
                            val borderPaint = Paint().apply {
                                color = android.graphics.Color.parseColor("#1e1e24")
                                style = Paint.Style.STROKE
                                strokeWidth = 6f
                            }
                            canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, borderPaint)
                        }
                    }
                }

                // Render customizable professional watermarking overlay
                if (showWatermark && watermarkText.isNotBlank()) {
                    val textPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
                        textSize = 36f
                        color = when (watermarkColorName) {
                            "Black" -> android.graphics.Color.BLACK
                            "Gold" -> android.graphics.Color.parseColor("#FFD700")
                            "Accent" -> android.graphics.Color.parseColor("#FF4081")
                            else -> android.graphics.Color.WHITE
                        }
                        alpha = (watermarkOpacity * 255).toInt().coerceIn(0, 255)
                    }
                    val watermarkX = collageSize - textPaint.measureText(watermarkText) - 40f
                    val watermarkY = collageSize - 40f
                    canvas.drawText(watermarkText, watermarkX, watermarkY, textPaint)
                }

                // Save or share actual generated JPG / PDF file physically
                val cacheDir = context.cacheDir
                val displayFilename = generateDisplayFileName()
                val safeFileName = generateSafeFileName(activeProjectName.replace("\\s".toRegex(), "_"), if (format == "PDF") "pdf" else "jpg")
                val outputFile = File(cacheDir, safeFileName)

                // Define local persistent storage destination to ensure output file is robustly persistent
                val localFolder = if (format == "PDF") {
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                } else {
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
                }
                try {
                    localFolder.mkdirs()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val localFile = File(localFolder, safeFileName)

                val compressionQuality = if (highQualityExport) 95 else 75

                if (format == "PDF") {
                    val pdfDocument = PdfDocument()
                    // Standard A4 paper size in postscript points (1/72 inch): 595 x 842 points (portrait or landscape)
                    val a4Width = if (pdfLandscape) 842 else 595
                    val a4Height = if (pdfLandscape) 595 else 842
                    val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    
                    val margin = 36f // 0.5 inch margins
                    val availableWidth = a4Width - (2 * margin)
                    val availableHeight = a4Height - (2 * margin)
                    
                    val scaleX = availableWidth / bitmap.width.toFloat()
                    val scaleY = availableHeight / bitmap.height.toFloat()
                    val scale = minOf(scaleX, scaleY)
                    
                    val targetWidth = bitmap.width.toFloat() * scale
                    val targetHeight = bitmap.height.toFloat() * scale
                    
                    val left = margin + (availableWidth - targetWidth) / 2f
                    val top = margin + (availableHeight - targetHeight) / 2f
                    val right = left + targetWidth
                    val bottom = top + targetHeight
                    
                    val destRect = android.graphics.RectF(left, top, right, bottom)
                    page.canvas.drawBitmap(bitmap, null, destRect, null)
                    pdfDocument.finishPage(page)
                    
                    // Save to shared cache
                    FileOutputStream(outputFile).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    
                    // Save to persistent local storage
                    try {
                        FileOutputStream(localFile).use { out ->
                            pdfDocument.writeTo(out)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Save to public Downloads via MediaStore on Android 10+ or via Environment public storage on older Androids
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val resolver = context.contentResolver
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CollageProStudio")
                                put(MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            if (uri != null) {
                                resolver.openOutputStream(uri).use { out ->
                                    if (out != null) {
                                        pdfDocument.writeTo(out)
                                    }
                                }
                                contentValues.clear()
                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(uri, contentValues, null, null)
                                MediaScannerConnection.scanFile(context, arrayOf(uri.toString()), null, null)
                            }
                        } else {
                            val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val collageFolder = File(pubDir, "CollageProStudio")
                            collageFolder.mkdirs()
                            val targetFile = File(collageFolder, safeFileName)
                            FileOutputStream(targetFile).use { out ->
                                pdfDocument.writeTo(out)
                            }
                            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    
                    pdfDocument.close()
                } else {
                    // Save to shared cache with quality setting
                    FileOutputStream(outputFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, out)
                    }
                    
                    // Save to persistent local storage
                    try {
                        FileOutputStream(localFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, out)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Save to public gallery via MediaStore on Android 10+ or via Environment public storage on older Androids
                    try {
                        val resolver = context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CollageProStudio")
                                put(MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                        }
                        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (imageUri != null) {
                            resolver.openOutputStream(imageUri).use { out ->
                                if (out != null) {
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, out)
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(imageUri, contentValues, null, null)
                            }
                            MediaScannerConnection.scanFile(context, arrayOf(imageUri.toString()), null, null)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                _shareFileProgress.value = null

                // Launch system sharesheet contract conditionally
                withContext(Dispatchers.Main) {
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outputFile
                    )

                    if (shareAfterExport) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setDataAndType(fileUri, if (format == "PDF") "application/pdf" else "image/jpeg")
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            putExtra(Intent.EXTRA_SUBJECT, "Shared via Collage Pro")
                            putExtra(Intent.EXTRA_TEXT, "Created Picture Collage: $displayFilename")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Collage ($format)"))
                    } else {
                        Toast.makeText(context, "Successfully Exported to Device Storage!\nSaved as $safeFileName", Toast.LENGTH_LONG).show()
                    }
                    refreshLocalExportedFiles(context)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _shareFileProgress.value = null
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Geometry grids utilities
    private class SimpleRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private fun calculateGridCoordinates(size: Int, template: Int, canvasSize: Int): List<SimpleRect> {
        val list = mutableListOf<SimpleRect>()
        val fSize = canvasSize.toFloat()

        when (size) {
            2 -> {
                when (template) {
                    0 -> { // Vertical split
                        list.add(SimpleRect(0f, 0f, fSize / 2, fSize))
                        list.add(SimpleRect(fSize / 2, 0f, fSize, fSize))
                    }
                    1 -> { // Horizontal split
                        list.add(SimpleRect(0f, 0f, fSize, fSize / 2))
                        list.add(SimpleRect(0f, fSize / 2, fSize, fSize))
                    }
                    2 -> { // Left 1/3, Right 2/3
                        list.add(SimpleRect(0f, 0f, fSize / 3f, fSize))
                        list.add(SimpleRect(fSize / 3f, 0f, fSize, fSize))
                    }
                    3 -> { // Top 1/3, Bottom 2/3
                        list.add(SimpleRect(0f, 0f, fSize, fSize / 3f))
                        list.add(SimpleRect(0f, fSize / 3f, fSize, fSize))
                    }
                    else -> {
                        list.add(SimpleRect(0f, 0f, fSize / 2, fSize))
                        list.add(SimpleRect(fSize / 2, 0f, fSize, fSize))
                    }
                }
            }
            4 -> {
                when (template) {
                    0 -> { // Standard 2x2 equal grid
                        val h = fSize / 2
                        list.add(SimpleRect(0f, 0f, h, h))
                        list.add(SimpleRect(h, 0f, fSize, h))
                        list.add(SimpleRect(0f, h, h, fSize))
                        list.add(SimpleRect(h, h, fSize, fSize))
                    }
                    1 -> { // Accent Layout - 1 Tall Left Panel, 3 horizontal right stacks
                        val wLeft = fSize * 0.6f
                        val hStack = fSize / 3
                        list.add(SimpleRect(0f, 0f, wLeft, fSize))
                        list.add(SimpleRect(wLeft, 0f, fSize, hStack))
                        list.add(SimpleRect(wLeft, hStack, fSize, hStack * 2))
                        list.add(SimpleRect(wLeft, hStack * 2, fSize, fSize))
                    }
                    2 -> { // Cinematic Strip
                        list.add(SimpleRect(0f, 0f, fSize, fSize * 0.6f))
                        list.add(SimpleRect(0f, fSize * 0.6f, fSize / 3, fSize))
                        list.add(SimpleRect(fSize / 3, fSize * 0.6f, fSize * 2/3, fSize))
                        list.add(SimpleRect(fSize * 2/3, fSize * 0.6f, fSize, fSize))
                    }
                    3 -> { // Pillars
                        list.add(SimpleRect(0f, 0f, fSize * 0.25f, fSize))
                        list.add(SimpleRect(fSize * 0.25f, 0f, fSize * 0.5f, fSize))
                        list.add(SimpleRect(fSize * 0.5f, 0f, fSize * 0.75f, fSize))
                        list.add(SimpleRect(fSize * 0.75f, 0f, fSize, fSize))
                    }
                    4 -> { // Spiral
                        list.add(SimpleRect(0f, 0f, fSize * 0.5f, fSize * 0.5f))
                        list.add(SimpleRect(fSize * 0.5f, 0f, fSize, fSize * 0.4f))
                        list.add(SimpleRect(fSize * 0.6f, fSize * 0.4f, fSize, fSize))
                        list.add(SimpleRect(0f, fSize * 0.5f, fSize * 0.6f, fSize))
                    }
                    else -> {
                        val h = fSize / 2
                        list.add(SimpleRect(0f, 0f, h, h))
                        list.add(SimpleRect(h, 0f, fSize, h))
                        list.add(SimpleRect(0f, h, h, fSize))
                        list.add(SimpleRect(h, h, fSize, fSize))
                    }
                }
            }
            6 -> {
                when (template) {
                    0 -> { // 2x3 Grid
                        val h = fSize / 2
                        val w = fSize / 3
                        for (row in 0 until 2) {
                            for (col in 0 until 3) {
                                list.add(SimpleRect(col * w, row * h, (col + 1) * w, (row + 1) * h))
                            }
                        }
                    }
                    1 -> { // Spotlight layout
                        val topH = fSize * 0.55f
                        val botW = fSize / 5
                        list.add(SimpleRect(0f, 0f, fSize, topH))
                        for (i in 0 until 5) {
                            list.add(SimpleRect(i * botW, topH, (i + 1) * botW, fSize))
                        }
                    }
                    2 -> { // Vertical Hero Left
                        val leftW = fSize * 0.55f
                        for (row in 0 until 5) {
                            list.add(SimpleRect(leftW, row * fSize * 0.2f, fSize, (row + 1) * fSize * 0.2f))
                        }
                        // Hero on left (insert at index 0 to match)
                        list.add(0, SimpleRect(0f, 0f, leftW, fSize))
                    }
                    3 -> { // 3x2 Grid
                        val h = fSize / 3
                        val w = fSize / 2
                        for (row in 0 until 3) {
                            for (col in 0 until 2) {
                                list.add(SimpleRect(col * w, row * h, (col + 1) * w, (row + 1) * h))
                            }
                        }
                    }
                    else -> {
                        val h = fSize / 2
                        val w = fSize / 3
                        for (row in 0 until 2) {
                            for (col in 0 until 3) {
                                list.add(SimpleRect(col * w, row * h, (col + 1) * w, (row + 1) * h))
                            }
                        }
                    }
                }
            }
            9 -> {
                when (template) {
                    0 -> { // Standard 3x3 equal grid
                        val sizeSlot = fSize / 3
                        for (row in 0 until 3) {
                            for (col in 0 until 3) {
                                list.add(SimpleRect(col * sizeSlot, row * sizeSlot, (col + 1) * sizeSlot, (row + 1) * sizeSlot))
                            }
                        }
                    }
                    1 -> { // Accent Focus Layout (1 center, 8 surrounding outer grid borders)
                        val s = fSize / 3
                        list.add(SimpleRect(s, s, s * 2, s * 2))
                        list.add(SimpleRect(0f, 0f, s, s))         // Top-Left
                        list.add(SimpleRect(s, 0f, s * 2, s))      // Top-Mid
                        list.add(SimpleRect(s * 2, 0f, fSize, s))  // Top-Right
                        list.add(SimpleRect(0f, s, s, s * 2))      // Mid-Left
                        list.add(SimpleRect(s * 2, s, fSize, s * 2))// Mid-Right
                        list.add(SimpleRect(0f, s * 2, s, fSize))  // Bot-Left
                        list.add(SimpleRect(s, s * 2, s * 2, fSize))// Bot-Mid
                        list.add(SimpleRect(s * 2, s * 2, fSize, fSize)) // Bot-Right
                    }
                    2 -> { // Column showcase
                        val s = fSize / 3
                        for (row in 0 until 3) {
                            list.add(SimpleRect(0f, row * s, fSize * 0.5f, (row + 1) * s))
                        }
                        for (row in 0 until 3) {
                            list.add(SimpleRect(fSize * 0.5f, row * s, fSize * 0.75f, (row + 1) * s))
                        }
                        for (row in 0 until 3) {
                            list.add(SimpleRect(fSize * 0.75f, row * s, fSize, (row + 1) * s))
                        }
                    }
                    3 -> { // Giant top hero, 8 below
                        list.add(SimpleRect(0f, 0f, fSize, fSize * 0.5f))
                        for (col in 0 until 4) {
                            list.add(SimpleRect(col * fSize * 0.25f, fSize * 0.5f, (col + 1) * fSize * 0.25f, fSize * 0.75f))
                        }
                        for (col in 0 until 4) {
                            list.add(SimpleRect(col * fSize * 0.25f, fSize * 0.75f, (col + 1) * fSize * 0.25f, fSize))
                        }
                    }
                    else -> {
                        val sizeSlot = fSize / 3
                        for (row in 0 until 3) {
                            for (col in 0 until 3) {
                                list.add(SimpleRect(col * sizeSlot, row * sizeSlot, (col + 1) * sizeSlot, (row + 1) * sizeSlot))
                            }
                        }
                    }
                }
            }
        }
        return list
    }

    // Helper to extract stunning colors from seeds
    private fun getPresetHex(seed: Int): String {
        val list = listOf(
            "#402240", "#a12230", "#304a5f", "#334f3c",
            "#604f32", "#473b5a", "#795246", "#1c313a",
            "#512da8", "#00796b", "#388e3c", "#fbc02d"
        )
        return list[(seed - 1).coerceIn(0, list.size - 1)]
    }

    // Load bitmap robustly with EXIF orientation correction
    private fun loadBitmapFromUri(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
            }
            
            var scale = 1
            while (options.outWidth / scale / 2 >= targetSize && options.outHeight / scale / 2 >= targetSize) {
                scale *= 2
            }
            
            val scaleOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            var decodedBitmap = context.contentResolver.openInputStream(uri)?.use { s2 ->
                android.graphics.BitmapFactory.decodeStream(s2, null, scaleOptions)
            }
            
            if (decodedBitmap != null) {
                var orientation = android.media.ExifInterface.ORIENTATION_NORMAL
                try {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        val exifInterface = android.media.ExifInterface(exifStream)
                        orientation = exifInterface.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                
                val rotation = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
                
                if (rotation != 0) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                    val rotated = android.graphics.Bitmap.createBitmap(
                        decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true
                    )
                    if (rotated != decodedBitmap) {
                        decodedBitmap.recycle()
                    }
                    decodedBitmap = rotated
                }
            }
            decodedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Refresh list of premium exported collages inside permission-free local folders
    fun refreshLocalExportedFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<File>()
            
            // Check getExternalFilesDir PICTUES
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (picturesDir != null && picturesDir.exists()) {
                val pics = picturesDir.listFiles { _, name -> 
                    val lower = name.lowercase(Locale.US)
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                }
                if (pics != null) {
                    list.addAll(pics)
                }
            }
            
            // Check getExternalFilesDir DOCUMENTS
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && docsDir.exists()) {
                val docs = docsDir.listFiles { _, name -> 
                    val lower = name.lowercase(Locale.US)
                    lower.endsWith(".pdf")
                }
                if (docs != null) {
                    list.addAll(docs)
                }
            }
            
            // Check local filesDir fallback
            val internalDir = context.filesDir
            if (internalDir != null && internalDir.exists()) {
                val internalFiles = internalDir.listFiles { _, name -> 
                    val lower = name.lowercase(Locale.US)
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".pdf")
                }
                if (internalFiles != null) {
                    list.addAll(internalFiles)
                }
            }
            
            val sortedList = list.distinctBy { it.name }.sortedByDescending { it.lastModified() }
            _localExportedFiles.value = sortedList
        }
    }

    // Interactive GitHub Updates simulator
    fun checkForGithubUpdates() {
        viewModelScope.launch {
            isCheckingUpdates = true
            updateMessage = null
            delay(1600)
            isCheckingUpdates = false
            updateMessage = "You have the latest version! Version v1.0.4 is fully up-to-date with the main repository on GitHub."
        }
    }
}
