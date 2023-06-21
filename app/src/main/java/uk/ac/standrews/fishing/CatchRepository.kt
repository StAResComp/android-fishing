package uk.ac.standrews.fishing

import kotlinx.coroutines.flow.Flow
import uk.ac.standrews.fishing.fishing.Catch
import uk.ac.standrews.fishing.fishing.FishingDao
import uk.ac.standrews.fishing.fishing.FullCatch
import uk.ac.standrews.fishing.fishing.LobsterCrabCatch
import uk.ac.standrews.fishing.fishing.NephropsCatch
import uk.ac.standrews.fishing.fishing.WrasseCatch

class CatchRepository(private val fishingDao: FishingDao) {

    val allFullCatches: Flow<List<FullCatch>> = fishingDao.getFullCatches()
    val numUnsubmittedCatches: Flow<Int> = fishingDao.getNumUnsubmittedCatches()

    suspend fun unsubmittedFullCatches(): List<FullCatch> {
       return fishingDao.getUnsubmittedFullCatches()
    }

    suspend fun insertCatch(aCatch: Catch): Long {
        return fishingDao.insertCatch(aCatch)
    }

    suspend fun insertNephropsCatch(nephropsCatch: NephropsCatch) {
        fishingDao.insertNephropsCatch(nephropsCatch)
    }

    suspend fun insertLobsterCrabCatch(lobsterCrabCatch: LobsterCrabCatch) {
        fishingDao.insertLobsterCrabCatch(lobsterCrabCatch)
    }

    suspend fun insertWrasseCatch(wrasseCatch: WrasseCatch) {
        fishingDao.insertWrasseCatch(wrasseCatch)
    }

    suspend fun getCatch(id: Int): Catch {
        return fishingDao.getCatch(id)
    }

    suspend fun updateCatch(aCatch: Catch) {
        fishingDao.updateCatch(aCatch)
    }
}
