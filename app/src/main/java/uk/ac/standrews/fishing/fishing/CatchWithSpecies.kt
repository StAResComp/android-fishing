package uk.ac.standrews.fishing.fishing

import androidx.room.*

class CatchWithSpecies {
    @Embedded
    lateinit var aCatch: Catch
    @Relation(parentColumn = "species_id", entityColumn = "id")
    lateinit var species: List<Species>
}
