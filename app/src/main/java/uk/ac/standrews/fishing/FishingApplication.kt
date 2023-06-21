package uk.ac.standrews.fishing

import android.app.Application
import android.provider.Settings.Secure
import uk.ac.standrews.fishing.db.AppDatabase
import uk.ac.standrews.fishing.db.CatchRepository

/**
 * Extends [android.app.Application] to handle tracking independently of any activity
 *
 * @constructor creates an instance with tracking location off
 */
class FishingApplication : Application() {

    private val database by lazy { AppDatabase.getAppDataBase(this) }
    val repository by lazy { CatchRepository(database.fishingDao()) }

    fun getId(): String {
        return Secure.getString(this.contentResolver, Secure.ANDROID_ID)
    }
}
