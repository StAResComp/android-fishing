package uk.ac.standrews.fishing.fishing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Database Access Object for working with fishing activity data
 */
@Dao
interface FishingDao {

    @Insert
    suspend fun insertCatch(aCatch: Catch): Long

    @Insert
    suspend fun insertNephropsCatch(nephropsCatch: NephropsCatch): Long

    @Insert
    suspend fun insertLobsterCrabCatch(lobsterCrabCatch: LobsterCrabCatch): Long

    @Insert
    suspend fun insertWrasseCatch(wrasseCatch: WrasseCatch): Long

    @Query("SELECT * FROM catch ORDER BY timestamp ASC")
    fun getCatches(): Flow<List<Catch>>

    @Query("""
        SELECT
            c.id,
            c.string_num as stringNum,
            c.lat,
            c.lon,
            c.timestamp,
            c.uploaded,
            n.id as nephropsId,
            n.num_small_cases as numSmallCases,
            n.num_medium_cases as numMediumCases,
            n.num_large_cases as numLargeCases,
            n.wt_returned as wtReturned,
            l.id as lobsterCrabId,
            l.num_lobster_retained as numlobsterRetained,
            l.num_lobster_returned as numlobsterReturned,
            l.num_brown_retained as numBrownRetained,
            l.num_brown_returned as numBrownReturned,
            l.num_velvet_retained as numVelvetRetained,
            l.num_velvet_returned as numVelvetReturned,
            w.id as wrasseId,
            w.num_wrasse_retained as numWrasseRetained,
            w.num_wrasse_returned as numWrasseReturned
        FROM
            catch c
        LEFT OUTER JOIN
            nephrops_catch n ON c.id = n.catch_id
        LEFT OUTER JOIN
            lobster_crab_catch l ON c.id = l.catch_id
        LEFT OUTER JOIN
            wrasse_catch w ON c.id = w.catch_id
        ORDER BY
            c.timestamp
    """)
    fun getFullCatches(): Flow<List<FullCatch>>
}
