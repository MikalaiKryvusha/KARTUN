package com.kriniks.kartun

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * The ONE thread every engine call of the process runs on (architecture invariant 4, research 03 §B lesson 7), named so
 * the thread list shows it. It lives as long as the process and is never shut down: the engine links libomp statically,
 * and a static libomp shuts itself down when the last thread that used it exits (`__kmp_internal_end_thread`, LLVM
 * `kmp_runtime.cpp`); the next thread then re-initialises it from a half-torn state and its affinity assert aborts the
 * app - bugs/01: a screen closed with Back stopped its own engine thread, the next screen's new thread (`pool-7-thread-1`)
 * loaded a model, `OMP: Error #13: Assertion failure at kmp_affinity.cpp(4755)`. [NOT-TESTED]
 */
object EngineThread {
    private const val NAME = "kartun-engine"

    val executor = Executors.newSingleThreadExecutor { r -> Thread(r, NAME) }

    /** The same thread as a coroutine dispatcher, for `withContext(EngineThread.dispatcher)`. */
    val dispatcher = executor.asCoroutineDispatcher()
}
