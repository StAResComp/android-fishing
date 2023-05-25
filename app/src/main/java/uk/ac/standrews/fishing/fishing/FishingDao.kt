package uk.ac.standrews.fishing.fishing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.util.Date

/**
 * Database Access Object for working with fishing activity data
 */
@Dao
interface FishingDao {

    @Insert
    fun insertSpecies(species: Array<Species>)

    @Insert
    fun insertSpecies(species: Species)

    @Insert
    fun insertLanded(aCatch: Catch): Long

    @Query("SELECT * FROM species ORDER BY id ASC")
    fun getSpecies(): Array<Species>

    @Query("SELECT name FROM species ORDER BY id ASC")
    fun getSpeciesNames(): Array<String>

    @Transaction
    @Query("SELECT l.* FROM species s INNER JOIN catch l ON s.id = l.species_id WHERE l.timestamp >= :startedAt AND l.timestamp < :finishedAt ORDER BY s.id ASC")
    fun getLandedsForPeriod(startedAt: Date, finishedAt: Date): Array<CatchWithSpecies>

    @Query("UPDATE catch SET weight = :weight, timestamp = :timestamp WHERE id = :id")
    fun updateLanded(id: Int, weight: Double, timestamp: Date)

    @Transaction
    @Query("SELECT * FROM catch WHERE uploaded IS NULL AND timestamp >= :startedAt AND timestamp < :finishedAt")
    fun getUnuploadedLandedsForPeriod(startedAt: Date, finishedAt: Date): List<CatchWithSpecies>

    @Query("UPDATE catch SET uploaded = :timestamp WHERE id IN (:ids)")
    fun markLandedsUploaded(ids: List<Int>, timestamp: Date)
}
