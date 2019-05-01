package org.phoenixframework

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class TimeoutTimerTest {

  @Mock lateinit var mockWorkItem: DispatchWorkItem
  @Mock lateinit var mockDispatchQueue: DispatchQueue

  private var callbackCallCount: Int = 0
  private lateinit var timeoutTimer: TimeoutTimer

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)

    callbackCallCount = 0
    whenever(mockDispatchQueue.queue(any(), any(), any())).thenReturn(mockWorkItem)

    timeoutTimer = TimeoutTimer(
        dispatchQueue = mockDispatchQueue,
        callback = { callbackCallCount += 1 },
        timerCalculation = { tries ->
          if(tries > 3 )  10000 else listOf(1000L, 2000L, 5000L)[tries -1]
        })
  }

  @Test
  fun `scheduleTimeout executes with backoff`() {
    argumentCaptor<() -> Unit> {
      timeoutTimer.scheduleTimeout()
      verify(mockDispatchQueue).queue(eq(1000L), eq(TimeUnit.MILLISECONDS), capture())
      lastValue.invoke()
      assertThat(callbackCallCount).isEqualTo(1)

      timeoutTimer.scheduleTimeout()
      verify(mockDispatchQueue).queue(eq(2000L), eq(TimeUnit.MILLISECONDS), capture())
      lastValue.invoke()
      assertThat(callbackCallCount).isEqualTo(2)

      timeoutTimer.scheduleTimeout()
      verify(mockDispatchQueue).queue(eq(5000L), eq(TimeUnit.MILLISECONDS), capture())
      lastValue.invoke()
      assertThat(callbackCallCount).isEqualTo(3)

      timeoutTimer.reset()
      timeoutTimer.scheduleTimeout()
      verify(mockDispatchQueue, times(2)).queue(eq(1000L), eq(TimeUnit.MILLISECONDS), capture())
      lastValue.invoke()
      assertThat(callbackCallCount).isEqualTo(4)
    }
  }
}