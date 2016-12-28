package cn.ictgu.taskmanager;

import lombok.extern.log4j.Log4j;

/**
 * 自定义的锁
 * Created by Silence on 2016/12/20.
 */
@Log4j
class LockObject {

  private int m_threadCount = 0;
  private final Object m_waitOnObject = new Object();

  void waitCurrentThread() throws Exception {
    synchronized (m_waitOnObject) {
      log.debug(Thread.currentThread().getName() + "：休眠当前线程");
      this.m_waitOnObject.wait();
    }
  }

  void notifyOtherThread() throws Exception {
    synchronized (m_waitOnObject) {
      log.debug(Thread.currentThread().getName() + "：唤醒所有等待线程");
      this.m_waitOnObject.notifyAll();
    }
  }

  void addThread() {
    synchronized (this) {
      m_threadCount = m_threadCount + 1;
    }
  }

  void releaseThread() {
    synchronized (this) {
      m_threadCount = m_threadCount - 1;
    }
  }

  /**
   * 降低线程数量，如果是最后一个线程，则不能休眠
   */
  boolean releaseThreadButNotLast() {
    synchronized (this) {
      if (this.m_threadCount == 1) {
        return false;
      } else {
        m_threadCount = m_threadCount - 1;
        return true;
      }
    }
  }

  int count() {
    synchronized (this) {
      return m_threadCount;
    }
  }
}
