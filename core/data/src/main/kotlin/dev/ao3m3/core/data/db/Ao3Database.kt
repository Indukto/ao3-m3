package dev.ao3m3.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.ao3m3.core.model.LibraryKind

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(TAG_SEPARATOR)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(TAG_SEPARATOR)

    @TypeConverter
    fun fromKind(value: LibraryKind): String = value.name

    @TypeConverter
    fun toKind(value: String): LibraryKind = LibraryKind.valueOf(value)

    private companion object {
        // Tags never contain newlines, so this round-trips safely.
        const val TAG_SEPARATOR = "\n"
    }
}

@Database(
    entities = [
        WorkEntity::class,
        ChapterEntity::class,
        ChapterTextEntity::class,
        PageEntity::class,
        PageItemEntity::class,
        LibraryEntity::class,
        ProgressEntity::class,
        PendingActionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class Ao3Database : RoomDatabase() {
    abstract fun dao(): Ao3Dao
}
