import com.zhufucdev.practiso.helper.DateTimeAdapter
import kotlin.test.Test

class DateTimeAdapterTest {
    @Test
    fun canParseCurrentTimestamp() {
        DateTimeAdapter.decode("2025-05-12 07:48:49")
    }
}