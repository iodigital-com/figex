package com.iodigital.figex.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DateFormat
import java.util.Date
import kotlin.time.Duration.Companion.seconds


internal var verboseLogs = false
internal var debugLogs = false
internal var showStatus = true
private val tagLength = 16
private val statusLength = 64
private var status = "Starting FigEx..."
private var statusSuffix = "..."
private var statusPrefix = "\uD83E\uDD84 "
private var logScope = CoroutineScope(Dispatchers.IO)
private var logMutex = Mutex()
private var statusAnimationJob: Job? = null

internal fun startStatusAnimation(scope: CoroutineScope) {
    if (!showStatus) return

    statusAnimationJob?.cancel("New animation")
    statusAnimationJob = statusAnimationJob ?: scope.launch {
        var counter = 0
        while (true) {
            delay(1.seconds)
            counter = (counter + 1) % 5
            statusSuffix = "..." + ".".repeat(counter)
            status(status)
        }
    }
}

internal fun status(message: String, finalStatus: Boolean = false) {
    if (!showStatus) return

    if (finalStatus) {
        statusAnimationJob?.cancel()
        statusAnimationJob = null
        statusSuffix = ""
        statusPrefix = if (message == "") "" else statusPrefix
    }

    status = "$message"
    logScope.launch {
        logMutex.withLock {
            print("\r$statusPrefix$status$statusSuffix".take(statusLength).padEnd(statusLength))
        }
    }
}

internal fun info(tag: String, message: String) {
    log(level = "\uD83D\uDC9A", tag = tag, message = message)
}

internal fun warning(tag: String, message: String) {
    log(level = "\uD83D\uDC9B", tag = tag, message = message)
}

internal fun debug(tag: String, message: String) {
    if (debugLogs || verboseLogs) {
        log(level = "\uD83E\uDE75", tag = tag, message = message)
    }
}

internal fun verbose(tag: String, message: String) {
    if (verboseLogs) {
        log(level = "\uD83E\uDD0D", tag = tag, message = message)
    }
}

internal fun critical(tag: String, message: String, throwable: Throwable? = null) = runBlocking {
    log(level = "❤\uFE0F", tag = tag, message = message, throwable = throwable).join()
}

private fun log(level: String, tag: String, message: String, throwable: Throwable? = null) =
    logScope.launch {
        logMutex.withLock {
            val date = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
            val paddedTag = tag.take(tagLength).padEnd(tagLength, ' ')
            val status =
                (if (showStatus) "$statusPrefix$status$statusSuffix" else "").take(statusLength)
                    .padEnd(statusLength)
            val line = "\r" + listOf(
                level,
                date,
                paddedTag,
                message
            ).joinToString(" | ")
            System.out.flush()
            print(line.padEnd(status.length + 1, ' ') + "\n$status")
            System.out.flush()
            throwable?.let {
                print("\r\n\n")
                it.printStackTrace()
            }
        }
    }