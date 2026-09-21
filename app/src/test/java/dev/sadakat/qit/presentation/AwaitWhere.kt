package dev.sadakat.qit.presentation

import app.cash.turbine.ReceiveTurbine

/**
 * Suspends until the next state matching [predicate] arrives, skipping intermediate states.
 * More robust than `expectMostRecentItem` (which does not suspend) against StateFlow conflation
 * and scheduler races.
 */
suspend fun <T : Any> ReceiveTurbine<T>.awaitWhere(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
