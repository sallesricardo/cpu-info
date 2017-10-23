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

package com.kgurgul.cpuinfo.features.applications

import android.arch.lifecycle.ViewModel
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.databinding.ObservableBoolean
import android.os.Build
import com.kgurgul.cpuinfo.common.Prefs
import com.kgurgul.cpuinfo.common.list.AdapterArrayList
import com.kgurgul.cpuinfo.utils.SingleLiveEvent
import com.kgurgul.cpuinfo.utils.runOnApiBelow
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import javax.inject.Inject

/**
 * ViewModel for [ApplicationsFragment]
 *
 * @author kgurgul
 */
class ApplicationsViewModel @Inject constructor(
        private val prefs: Prefs,
        private val packageManager: PackageManager) : ViewModel() {

    private val SORTING_APPS_KEY = "SORTING_APPS_KEY"

    val isLoading = ObservableBoolean(false)
    val applicationList = AdapterArrayList<ExtendedAppInfo>()
    val shouldStartStorageService = SingleLiveEvent<Void>()

    private val ref = asReference()
    private var isSortingAsc = prefs.get(SORTING_APPS_KEY, true)
    private var refreshingDisposable: Disposable? = null

    init {
        refreshApplicationsList()
        EventBus.getDefault().register(this)
    }

    @Synchronized
    fun refreshApplicationsList() {
        if (refreshingDisposable == null || !isLoading.get()) {
            refreshingDisposable = getApplicationsListSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe({ _ -> isLoading.set(true) })
                    .doFinally({ isLoading.set(false) })
                    .subscribe({ appList ->
                        applicationList.replace(appList)
                        runOnApiBelow(Build.VERSION_CODES.O, {
                            shouldStartStorageService.call()
                        }, {})
                    })
        }
    }

    /**
     * Get all user applications
     */
    private fun getApplicationsListSingle(): Single<List<ExtendedAppInfo>> {
        return Single.fromCallable({
            val extendedAppList = ArrayList<ExtendedAppInfo>()
            try {
                var appsList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                appsList = if (isSortingAsc) {
                    appsList.sortedBy {
                        it.loadLabel(packageManager).toString().toUpperCase()
                    }
                } else {
                    appsList.sortedByDescending {
                        it.loadLabel(packageManager).toString().toUpperCase()
                    }
                }
                appsList.map {
                    extendedAppList.add(
                            ExtendedAppInfo(it.loadLabel(packageManager).toString(),
                                    it.packageName, it.nativeLibraryDir))
                }
            } catch (e: Exception) {
                // Do nothing
            }
            extendedAppList
        })
    }

    /**
     * Change apps list sorting type from ascending to descending or or vice versa
     */
    fun changeAppsSorting() {
        async(UI) {
            val result = bg { getAppSortedList() }
            val sortedAppList = result.await()
            ref().applicationList.replace(sortedAppList)
        }
    }

    /**
     * @return sorted list of the apps from [applicationList]
     */
    private fun getAppSortedList(): List<ExtendedAppInfo> {
        val appListCopy = ArrayList<ExtendedAppInfo>(applicationList)
        isSortingAsc = !isSortingAsc
        prefs.insert(SORTING_APPS_KEY, isSortingAsc)
        if (isSortingAsc) {
            appListCopy.sortBy { it.name.toUpperCase() }
        } else {
            appListCopy.sortByDescending { it.name.toUpperCase() }
        }
        return appListCopy
    }

    /**
     * Update package size whit specific package name using coroutine
     */
    @Subscribe
    fun onUpdatePackageSizeEvent(event: StorageUsageService.UpdatePackageSizeEvent) {
        async(UI) {
            val result = bg { getUpdatedApp(event) }
            val newAppPair = result.await()
            if (newAppPair.first != -1) {
                ref().applicationList[newAppPair.first] = newAppPair.second!!
            }
        }
    }

    /**
     * @return updated [ExtendedAppInfo] instance with corresponding index
     */
    private fun getUpdatedApp(event: StorageUsageService.UpdatePackageSizeEvent)
            : Pair<Int, ExtendedAppInfo?> {
        val app = applicationList.find { it.packageName == event.packageName }
        if (app != null) {
            val index = applicationList.indexOf(app)
            app.appSize = event.size
            return Pair(index, app)
        }
        return Pair(-1, null)
    }

    override fun onCleared() {
        super.onCleared()
        refreshingDisposable?.dispose()
        EventBus.getDefault().unregister(this)
    }
}