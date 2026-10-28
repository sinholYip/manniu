package com.ranke.health.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "weights", indices = [Index("time")])
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: String,
    val kilograms: Double,
    val source: String
)

@Entity(tableName = "meals", indices = [Index("time")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: String,
    val name: String,
    val kilocalories: Double,
    val source: String,
    val period: String,
    val carbohydratesGrams: Double,
    val proteinGrams: Double,
    val fatGrams: Double
)

@Entity(tableName = "workout_days", indices = [Index("date")])
data class WorkoutDayEntity(
    @PrimaryKey val date: String,
    val mode: String,
    val cycleWeek: Int?,
    val isDeload: Boolean,
    val isCompleted: Boolean
)

@Entity(
    tableName = "workout_exercises",
    primaryKeys = ["dayDate", "position"],
    indices = [Index("dayDate")]
)
data class WorkoutExerciseEntity(
    val dayDate: String,
    val position: Int,
    val exerciseId: Long,
    val name: String,
    val sets: Int,
    val reps: String,
    val weight: String
)

@Entity(tableName = "exercise_prs", indices = [Index("date"), Index("exerciseName")])
data class ExercisePrEntity(
    @PrimaryKey val id: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val date: String
)

@Dao
interface HealthDao {
    @Query("SELECT * FROM weights ORDER BY time")
    fun weights(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeight(item: WeightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeights(items: List<WeightEntity>)

    @Query("SELECT * FROM meals ORDER BY time")
    fun meals(): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMeal(item: MealEntity)

    @Query("UPDATE meals SET name = :name, kilocalories = :kilocalories, period = :period, carbohydratesGrams = :carbs, proteinGrams = :protein, fatGrams = :fat WHERE time = :time")
    fun updateMeal(
        time: String,
        name: String,
        kilocalories: Double,
        period: String,
        carbs: Double,
        protein: Double,
        fat: Double
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMeals(items: List<MealEntity>)

    @Query("SELECT * FROM workout_days ORDER BY date")
    fun workoutDays(): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_exercises ORDER BY dayDate, position")
    fun workoutExercises(): List<WorkoutExerciseEntity>

    @Query("DELETE FROM workout_days")
    fun clearWorkoutDays()

    @Query("DELETE FROM workout_exercises")
    fun clearWorkoutExercises()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkoutDays(items: List<WorkoutDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Query("SELECT * FROM exercise_prs ORDER BY date DESC, id DESC")
    fun exercisePrs(): List<ExercisePrEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExercisePr(item: ExercisePrEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExercisePrs(items: List<ExercisePrEntity>)
}

@Database(
    entities = [
        WeightEntity::class,
        MealEntity::class,
        WorkoutDayEntity::class,
        WorkoutExerciseEntity::class,
        ExercisePrEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "manniu-health.db"
            ).build().also { instance = it }
        }
    }
}
