/*
 * Copyright 2017 KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("Extensions")

package com.kgurgul.cpuinfo.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.support.v4.app.Fragment

/**
 * All basic extensions
 *
 * @author kgurgul
 */
fun Float.round1(): Float = Math.round(this * 10.0) / 10.0f

fun Double.round1(): Double = Math.round(this * 10.0) / 10.0

fun Float.round2(): Float = Math.round(this * 100.0) / 100.0f

fun Double.round2(): Double = Math.round(this * 100.0) / 100.0

inline fun runOnApi(api: Int, f: () -> Unit, otherwise: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT == api) {
        f()
    } else {
        otherwise()
    }
}

inline fun runOnApiBelow(api: Int, f: () -> Unit, otherwise: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT < api) {
        f()
    } else {
        otherwise()
    }
}

inline fun runOnApiAbove(api: Int, f: () -> Unit, otherwise: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT > api) {
        f()
    } else {
        otherwise()
    }
}

/**
 * Returns not null context or throws exception. Use it only between [Fragment.onAttach] and
 * [Fragment.onDetach]
 */
fun Fragment.nonNullContext(): Context = context!!

/**
 * Returns not null activity or throws exception. Use it only between [Fragment.onAttach] and
 * [Fragment.onDetach]
 */
fun Fragment.nonNullActivity(): Activity = activity!!