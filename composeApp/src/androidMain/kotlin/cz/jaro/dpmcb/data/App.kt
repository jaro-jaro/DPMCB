package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.data.database.SpojeDataSource
import cz.jaro.dpmcb.data.database.SpojeQueries
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SeqOfConn
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.AndroidDiagramManager
import cz.jaro.dpmcb.ui.map.DiagramManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.dsl.bind
import org.koin.dsl.module

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val ctx = this
        val androidModule = module(true) {
            single { this@App } bind Context::class
            single { Firebase.initialize(get<Context>())!! }
            single<SpojeDataSource> {
                Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "db-dpmcb")
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .let(::dataSource)
            }
            single { get<Context>().getSharedPreferences("prefs-dpmcb", MODE_PRIVATE) }
            single { SharedPreferencesSettings(get()) } bind ObservableSettings::class
            single { UserOnlineManager(ctx) }
            single { DetailsOpener(ctx) }
            single<DiagramManager> { AndroidDiagramManager(ctx) }
        }
        initKoin(androidModule)
    }

    private fun dataSource(database: AppDatabase): SpojeDataSource {
        val dao = database.dataSource()
        return object : SpojeDataSource, SpojeQueries by dao {
            override suspend fun clearAllTables() = database.clearAllTables()
            override val needsToDownloadData = true

            override fun insertConnStops(connStops: List<ConnStop>) =
                listOf(suspend { dao.insertConnStops2(connStops) })
            override fun insertStops(stops: List<Stop>) =
                listOf(suspend { dao.insertStops2(stops) })
            override fun insertTimeCodes(timeCodes: List<TimeCode>) =
                listOf(suspend { dao.insertTimeCodes2(timeCodes) })
            override fun insertLines(lines: List<Line>) =
                listOf(suspend { dao.insertLines2(lines) })
            override fun insertConns(conns: List<Conn>) =
                listOf(suspend { dao.insertConns2(conns) })
            override fun insertSeqOfConns(seqsOfBuses: List<SeqOfConn>) =
                listOf(suspend { dao.insertSeqOfConns2(seqsOfBuses) })
            override fun insertSeqGroups(seqGroups: List<SeqGroup>) =
                listOf(suspend { dao.insertSeqGroups2(seqGroups) })
        }
    }
}