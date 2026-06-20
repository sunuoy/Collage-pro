package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collage_projects")
data class CollageProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val gridLayoutSize: Int = 4, // 2, 4, 6, 9
    val templateIndex: Int = 0, // Variant within layout
    val imagePathsString: String = "", // Comma-separated Uris/colors or asset indicators
    val filterName: String = "Classic", // Classic, Cinema, Warm, Cool, Sepia, Monochrome, Vintage
    val watermarkText: String = "CollagePro",
    val isColorOutput: Boolean = true,
    val isSynced: Boolean = false
) {
    fun getImageList(): List<String> {
        if (imagePathsString.isBlank()) return emptyList()
        return imagePathsString.split(":::")
    }

    companion object {
        fun fromImageList(list: List<String>): String {
            return list.joinToString(":::")
        }
    }
}
