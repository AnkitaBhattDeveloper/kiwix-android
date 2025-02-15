/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.kiwix.kiwixmobile.webserver

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.kiwix.kiwixmobile.core.utils.DEFAULT_PORT
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.ServerUtils.INVALID_IP
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getIp
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getIpAddress
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.IpAddressCallbacks
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */
class WebServerHelper @Inject constructor(
  private val kiwixServerFactory: KiwixServer.Factory,
  private val ipAddressCallbacks: IpAddressCallbacks
) {
  private var kiwixServer: KiwixServer? = null
  private var isServerStarted = false

  fun startServerHelper(selectedBooksPath: ArrayList<String>): Boolean {
    val ip = getIpAddress()
    return if (ip.isNullOrEmpty()) {
      false
    } else if (startAndroidWebServer(selectedBooksPath)) {
      true
    } else {
      isServerStarted
    }
  }

  fun stopAndroidWebServer() {
    if (isServerStarted) {
      kiwixServer?.stopServer()
      updateServerState(false)
    }
  }

  private fun startAndroidWebServer(selectedBooksPath: ArrayList<String>): Boolean {
    if (!isServerStarted) {
      ServerUtils.port = DEFAULT_PORT
      kiwixServer = kiwixServerFactory.createKiwixServer(selectedBooksPath).also {
        updateServerState(it.startServer(ServerUtils.port))
        Log.d(TAG, "Server status$isServerStarted")
      }
    }
    return isServerStarted
  }

  private fun updateServerState(isStarted: Boolean) {
    isServerStarted = isStarted
    ServerUtils.isServerStarted = isStarted
  }

  // Keeps checking if hotspot has been turned using the ip address with an interval of 1 sec
  // If no ip is found after 15 seconds, dismisses the progress dialog
  @Suppress("MagicNumber")
  fun pollForValidIpAddress() {
    Flowable.interval(1, TimeUnit.SECONDS)
      .map { getIp() }
      .filter { s: String? -> s != INVALID_IP }
      .timeout(15, TimeUnit.SECONDS)
      .take(1)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        { s: String? ->
          ipAddressCallbacks.onIpAddressValid()
          Log.d(TAG, "onSuccess:  $s")
        }
      ) { e: Throwable? ->
        Log.d(TAG, "Unable to turn on server", e)
        ipAddressCallbacks.onIpAddressInvalid()
      }
  }

  companion object {
    private const val TAG = "WebServerHelper"
  }
}
