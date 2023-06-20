package uk.ac.standrews.fishing.fishing

import androidx.room.*
import java.util.*

/**
 * Describes the catch for a specified string
 *
 * @property id numeric id, autoincremented by the database
 * @property stringId the fisher's identifier for the string
 * @property lat the latitude of the catch location
 * @property lon the longitude of the catch location
 * @property timestamp when the catch was made
 * @property uploaded when the data was uploaded to the server
 */
@Entity(
    tableName = "catch"
)
data class Catch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "string_num") var stringId: String,
    var lat: Double,
    var lon: Double,
    var timestamp: Date,
    var uploaded: Date? = null
)

/**
 * Describes the detail of a nephrops catch
 *
 * @property id numeric id, autoincremented by the database
 * @property catchId the id of the corresponding Catch
 * @property numSmall the number graded as "small"
 * @property numMedium the number graded as "medium"
 * @property numLarge the number graded as "large"
 * @property numCases the number of cases
 * @property wtReturned the weight returned
 */
@Entity(
    tableName = "nephrops_catch",
    foreignKeys = [
        ForeignKey(
            entity = Catch::class,
            parentColumns = ["id"],
            childColumns = ["catch_id"]
        )
    ],
)
data class NephropsCatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "catch_id", index = true) var catchId: Int,
    @ColumnInfo(name = "num_small_cases") var numSmallCases: Double = 0.0,
    @ColumnInfo(name = "num_medium_cases") var numMediumCases: Double = 0.0,
    @ColumnInfo(name = "num_large_cases") var numLargeCases: Double = 0.0,
    @ColumnInfo(name = "wt_returned") var wtReturned: Double = 0.0,
)

/**
 * Describes the detail of a lobster/crab catch
 *
 * @property id numeric id, autoincremented by the database
 * @property catchId the id of the corresponding Catch
 * @property numLobsterRetained the number of lobsters retained
 * @property numLobsterReturned the number of lobsters returned
 * @property numBrownRetained the number of brown crabs retained
 * @property numBrownReturned the number of brown crabs returned
 * @property numVelvetRetained the number of velvet crabs retained
 * @property numVelvetReturned the number of velvet crabs returned
 */
@Entity(
    tableName = "lobster_crab_catch",
    foreignKeys = [
        ForeignKey(
            entity = Catch::class,
            parentColumns = ["id"],
            childColumns = ["catch_id"]
        )
    ],
)
data class LobsterCrabCatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "catch_id", index = true) var catchId: Int,
    @ColumnInfo(name = "num_lobster_retained") var numLobsterRetained: Int = 0,
    @ColumnInfo(name = "num_lobster_returned") var numLobsterReturned: Int = 0,
    @ColumnInfo(name = "num_brown_retained") var numBrownRetained: Int = 0,
    @ColumnInfo(name = "num_brown_returned") var numBrownReturned: Int = 0,
    @ColumnInfo(name = "num_velvet_retained") var numVelvetRetained: Int = 0,
    @ColumnInfo(name = "num_velvet_returned") var numVelvetReturned: Int = 0
)

/**
 * Describes the detail of a lobster/crab catch
 *
 * @property id numeric id, autoincremented by the database
 * @property catchId the id of the corresponding Catch
 * @property numWrasseRetained the number of lobsters retained
 * @property numWrasseReturned the number of lobsters returned
 */
@Entity(
    tableName = "wrasse_catch",
    foreignKeys = [
        ForeignKey(
            entity = Catch::class,
            parentColumns = ["id"],
            childColumns = ["catch_id"]
        )
    ],
)
data class WrasseCatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "catch_id", index = true) var catchId: Int,
    @ColumnInfo(name = "num_wrasse_retained") var numWrasseRetained: Int = 0,
    @ColumnInfo(name = "num_wrasse_returned") var numWrasseReturned: Int = 0,
)

/**
 * Describes the full details of a catch for a specified string
 *
 * @property id id of the Catch record
 * @property stringNum the string number
 * @property lat the latitude of the catch location
 * @property lon the longitude of the catch location
 * @property timestamp when the catch was made
 * @property uploaded when the data was uploaded to the server
 * @property numSmallCases the number of "small" nephrops cases
 * @property numMediumCases the number of "medium" nephrops cases
 * @property numLargeCases the number of "large" nephrops cases
 * @property wtReturned the weight of nephrops returned
 * @property numlobsterRetained the number of lobsters retained
 * @property numlobsterReturned the number of lobsters returned
 * @property numBrownRetained the number of brown crabs retained
 * @property numBrownReturned the number of brown crabs returned
 * @property numVelvetRetained the number of velvet crabs retained
 * @property numVelvetReturned the number of velvet crabs returned
 * @property numWrasseRetained the number of velvet crabs retained
 * @property numWrasseReturned the number of velvet crabs returned
 */
data class FullCatch (
    val id: Int = 0,
    val stringNum: String,
    val lat: Double,
    val lon: Double,
    val timestamp: Date,
    val uploaded: Date? = null,
    val nephropsId: Int?,
    val numSmallCases: Double?,
    val numMediumCases: Double?,
    val numLargeCases: Double?,
    val wtReturned: Double?,
    val lobsterCrabId: Int?,
    val numlobsterRetained: Int?,
    val numlobsterReturned: Int?,
    val numBrownRetained: Int?,
    val numBrownReturned: Int?,
    val numVelvetRetained: Int?,
    val numVelvetReturned: Int?,
    val wrasseId: Int?,
    val numWrasseRetained: Int?,
    val numWrasseReturned: Int?
)
