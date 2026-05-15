import com.zhufucdev.practiso.DmetaSmallZh
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.PractisoAnswer
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.helper.toDatabase
import com.zhufucdev.practiso.platform.getFeiInferenceSession
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.FeiDbState
import com.zhufucdev.practiso.service.FeiService
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.service.ImportState
import com.zhufucdev.practiso.service.MissingModelResponse
import com.zhufucdev.practiso.service.PendingDownloadResponse
import com.zhufucdev.practiso.service.RecommendationService
import com.zhufucdev.practiso.service.RecommendationServiceConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.use
import opacity.client.ArchiveMetadata
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val database = getPlatform().createDbDriver("test.db").toDatabase()

class RecommendationSystemTest {
    @Test
    fun recallShouldBeAcceptable() = runTest(timeout = Duration.INFINITE) {
        val feiService = FeiService(
            defaultModel = DmetaSmallZh,
            db = database,
            inferenceSession = getFeiInferenceSession(),
            context = Dispatchers.Default
        )
        val communityService = CommunityService(identity = MockIdentity())
        val archives =
            listOf(
                communityService.getArchiveMetadata("ViASCFCuotiqaR72SI63m" /* software engineering */)
                    .first()
                    ?.let { communityService.downloadAndImportIfNotPresent(it) },
                communityService.getArchiveMetadata("DhZGL7Z5PUCWT8DGmKE9w") /* digital image processing */
                    .first()
                    ?.let { communityService.downloadAndImportIfNotPresent(it) },
                communityService.getArchiveMetadata("_j2BWJGTcAxD4HcY_5lr8")
                    .first()
                    ?.let { communityService.downloadAndImportIfNotPresent(it) },
            )

        database.sessionQueries.getAllSessions().executeAsList()
            .forEach {
                database.sessionQueries.removeSession(it.id)
            }

        val file = getPlatform().createTemporaryFile("recommendation-system-evaluation", ".csv")

        file.let { getPlatform().filesystem.openReadWrite(it, mustCreate = true) }
            .appendingSink()
            .buffer()
            .use { output ->
                output.writeUtf8("category,errorRate,TP,FN,FP,FN,recall,accuracy\n")
                for (err in 1..9) {
                    archives.forEach { archive ->
                        archive?.dimensions?.forEach { dim ->
                            repeat(10) {
                                val confusionMatrix = getConfusionMatrix(
                                    getRecommendationService(feiService),
                                    dim.name,
                                    errorPercent = err.toFloat() / 10
                                )
                                val recall = confusionMatrix.getRecall()
                                val accuracy = confusionMatrix.getAccuracy()
                                println("(recall, acc) of ${dim.name}: ($recall, $accuracy)")
                                output.writeUtf8("${dim.name},${err.toFloat() / 10},${confusionMatrix.truePositives},${confusionMatrix.trueNegatives},${confusionMatrix.falsePositives},${confusionMatrix.falseNegatives},${recall},${accuracy}\n")
                            }
                        }
                    }
                }
            }
        println("csv table written to $file")
    }
}

@OptIn(FlowPreview::class)
suspend fun getRecommendationService(feiService: FeiService): RecommendationService {
    feiService.getUpgradeState()
        .debounce {
            if (it is FeiDbState.Ready) {
                2.seconds
            } else {
                100.milliseconds
            }
        }
        .transformWhile {
            emit(it)
            it !is FeiDbState.Ready
        }
        .collect { state ->
            when (state) {
                FeiDbState.Collecting -> {}
                is FeiDbState.DownloadingInference -> {
                    println("Downloading model ${state.progress * 100}%")
                }

                is FeiDbState.Error -> {
                    throw state.error
                }

                is FeiDbState.InProgress -> {
                    println("Inferring ${state.done} / ${state.total}")
                }

                is FeiDbState.MissingModel -> {
                    state.proceed?.send(MissingModelResponse.ProceedAnyway)
                }

                is FeiDbState.PendingDownload -> {
                    state.response.send(PendingDownloadResponse.Immediate)
                }

                is FeiDbState.Ready -> {
                    println("FEI service ready!")
                }
            }
        }

    return RecommendationService(
        fei = feiService,
        db = database,
        config = RecommendationServiceConfiguration(searchK = 100, idealSimilarity = 0.7f)
    )
}

suspend fun CommunityService.downloadAndImportIfNotPresent(meta: ArchiveMetadata): ArchiveMetadata {
    val dimNames = meta.dimensions.map { it.name }.toSet()
    if (
        database.dimensionQueries.getDimensionsByName(dimNames)
            .executeAsList().size == meta.dimensions.size
    ) {
        println("skipping ${meta.name} because it's cached")
        return meta
    }
    val handle = meta.toHandle()
    val source = handle.getAsSource()
    ImportService(database)
        .import(source)
        .collect {
            when (it) {
                is ImportState.Confirmation -> it.ok.send(Unit)
                is ImportState.Error -> throw it.error
                is ImportState.Idle -> {}
                is ImportState.Importing -> {}
                is ImportState.Unarchiving -> {}
            }
        }
    return meta
}

data class ConfusionMatrix(
    val truePositives: Int,
    val trueNegatives: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
) {
    fun getRecall(): Double {
        return truePositives.toDouble() / (truePositives + falseNegatives)
    }

    fun getAccuracy(): Double {
        return (truePositives + trueNegatives).toDouble() / (truePositives + trueNegatives + falsePositives + falseNegatives)
    }
}

suspend fun getConfusionMatrix(
    sys: RecommendationService,
    dimensionName: String,
    errorPercent: Float = 0.5f,
): ConfusionMatrix {
    val dim = database.dimensionQueries.getDimensionByName(dimensionName).executeAsOneOrNull()
        ?: throw IllegalArgumentException("dimension name")
    val dimQuizzes = database.quizQueries.getQuizFrames(
        database.quizQueries.getQuizByDimensions(setOf(dim.id))
    )
        .first()
    val quizzes = dimQuizzes
        .shuffled()
        .let { it.take((it.size * errorPercent).toInt()) }

    val sessionId =
        database.transactionWithResult {
            database.sessionQueries.insertSesion("recall test of $dimensionName")
            database.quizQueries.lastInsertRowId().executeAsOne()
        }
    quizzes.forEach {
        database.sessionQueries.assoicateQuizWithSession(it.quiz.id, sessionId)
    }
    val takeId = database.transactionWithResult {
        database.sessionQueries.insertTake(sessionId)
        database.quizQueries.lastInsertRowId().executeAsOne()
    }
    coroutineScope {
        quizzes.mapIndexed { index, it ->
            async {
                val optionsFrame =
                    it.frames.first { pf -> pf.frame is Frame.Options }.frame as Frame.Options
                PractisoAnswer.Option(
                    optionId = optionsFrame.frames.firstOrNull { option -> !option.isKey }?.frame?.id
                        ?: optionsFrame.frames.first().frame.id,
                    frameId = optionsFrame.id,
                    quizId = it.quiz.id
                )
                    .commit(database, takeId, priority = index)
            }
        }
            .awaitAll()
    }
    val recommendedQuizIds = sys.getSmartRecommendations().first()
        .fold(mutableSetOf<Long>()) { acc, curr -> acc.addAll(curr.selection.quizIds); acc }
    database.sessionQueries.removeSession(sessionId)

    val allQuizzesIds = database.quizQueries.getAllQuiz().executeAsList().map { it.id }.toSet()
    val dimQuizIds = dimQuizzes.map { it.quiz.id }.toSet()
    val sampledQuizIds = quizzes.map { it.quiz.id }.toSet()
    val expectedRecommendations = dimQuizIds - sampledQuizIds
    val notRecommended = allQuizzesIds - recommendedQuizIds

    val truePositive = recommendedQuizIds.count { rq -> rq in expectedRecommendations }
    val trueNegative =
        (allQuizzesIds - recommendedQuizIds).count { rq -> rq !in expectedRecommendations }

    val falsePositive = recommendedQuizIds.count { it !in expectedRecommendations }
    val falseNegative = notRecommended.count { it in expectedRecommendations }

    assertTrue {
        truePositive + falsePositive == recommendedQuizIds.size
    }
    assertTrue {
        trueNegative + falseNegative == notRecommended.size
    }
    assertTrue {
        truePositive + falseNegative == expectedRecommendations.size
    }

    return ConfusionMatrix(
        truePositive,
        trueNegative,
        falsePositive,
        falseNegative
    )
}
