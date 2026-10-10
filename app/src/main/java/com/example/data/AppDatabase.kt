package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromActivityContext(value: ActivityContext): String = value.name

    @TypeConverter
    fun toActivityContext(value: String): ActivityContext =
        try { ActivityContext.valueOf(value) } catch (e: Exception) { ActivityContext.RESTING }

    @TypeConverter
    fun fromTimeOfDay(value: TimeOfDay): String = value.name

    @TypeConverter
    fun toTimeOfDay(value: String): TimeOfDay =
        try { TimeOfDay.valueOf(value) } catch (e: Exception) { TimeOfDay.MORNING }

    @TypeConverter
    fun fromMeasurementArm(value: MeasurementArm): String = value.name

    @TypeConverter
    fun toMeasurementArm(value: MeasurementArm?): String = value?.name ?: MeasurementArm.LEFT.name

    @TypeConverter
    fun toArm(value: String): MeasurementArm =
        try { MeasurementArm.valueOf(value) } catch (e: Exception) { MeasurementArm.LEFT }
}

@Database(
    entities = [BloodPressureReading::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodPressureDao(): BloodPressureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vitals_drift.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
